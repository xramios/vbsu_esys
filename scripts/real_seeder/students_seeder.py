#!/usr/bin/env python3
"""Students seeder using real data from CSV files."""

from __future__ import annotations

from dataclasses import dataclass
from datetime import date
from pathlib import Path
from typing import Any

import bcrypt

from seeder.core.database import DatabaseManager

from real_seeder.students_parser import StudentCsvRow, parse_students_csv


@dataclass(frozen=True, slots=True)
class StudentsSeedSummary:
    """Summary of student seeding changes."""

    course_id: int
    curriculum_id: int
    users_created: int
    users_updated: int
    students_created: int
    students_updated: int


class RealStudentsSeeder:
    """Imports student accounts and student records from CSV."""

    def __init__(self, db_manager: DatabaseManager, bcrypt_rounds: int = 12) -> None:
        self.db_manager = db_manager
        self.bcrypt_rounds = bcrypt_rounds
        self._placeholder = "?" if db_manager.db_type == "derby" else "%s"
        self._table_prefix = "APP." if db_manager.db_type == "derby" else ""

    def seed_from_csv(
        self,
        csv_path: Path,
        course_hint: str = "BSIT",
        curriculum_name: str = "NITEN2023",
    ) -> StudentsSeedSummary:
        """Seed users and students tables from CSV."""
        rows = parse_students_csv(csv_path)

        if not self.db_manager.connect():
            raise RuntimeError("Failed to connect to database")

        cursor = self.db_manager.create_cursor()
        try:
            course_id = self._resolve_course_id(cursor, course_hint)
            curriculum_id = self._resolve_curriculum_id(cursor, curriculum_name, course_id)
            self._ensure_students_curriculum_schema(cursor)

            users_by_email = self._load_users_by_email(cursor)
            students_by_id = self._load_students_by_id(cursor)

            users_created = 0
            users_updated = 0
            students_created = 0
            students_updated = 0

            for row in rows:
                email = self._build_email(row.last_name, row.first_name)
                raw_password = self._build_plain_password(row.last_name, row.birthdate)
                hashed_password = self._hash_password(raw_password)

                existing_student_user_id = students_by_id.get(row.student_id)
                existing_email_user_id = users_by_email.get(email)

                if existing_student_user_id is not None:
                    user_id = existing_student_user_id
                    self._update_user(cursor, user_id, email, hashed_password)
                    users_updated += 1
                elif existing_email_user_id is not None:
                    user_id = existing_email_user_id
                    self._update_user(cursor, user_id, email, hashed_password)
                    users_updated += 1
                else:
                    user_id = self._create_user(cursor, email, hashed_password)
                    users_created += 1

                users_by_email[email] = user_id

                if row.student_id in students_by_id:
                    self._update_student(
                        cursor,
                        row,
                        user_id=user_id,
                        course_id=course_id,
                        curriculum_id=curriculum_id,
                    )
                    students_updated += 1
                else:
                    self._create_student(
                        cursor,
                        row,
                        user_id=user_id,
                        course_id=course_id,
                        curriculum_id=curriculum_id,
                    )
                    students_created += 1

                students_by_id[row.student_id] = user_id

            self.db_manager.commit()

            return StudentsSeedSummary(
                course_id=course_id,
                curriculum_id=curriculum_id,
                users_created=users_created,
                users_updated=users_updated,
                students_created=students_created,
                students_updated=students_updated,
            )
        except Exception:
            rollback = getattr(self.db_manager.connection, "rollback", None)
            if callable(rollback):
                rollback()
            raise
        finally:
            cursor.close()
            self.db_manager.disconnect()

    def _resolve_course_id(self, cursor: Any, course_hint: str) -> int:
        table = self._table("courses")
        cursor.execute(f"SELECT id, course_name FROM {table}")
        candidates = [(int(row[0]), str(row[1])) for row in cursor.fetchall()]
        if not candidates:
            raise ValueError("No courses found. Seed course data first.")

        normalized_hint = self._normalize_key(course_hint)
        best_match: tuple[int, int] | None = None
        for course_id, course_name in candidates:
            aliases = self._course_aliases(course_name)
            if normalized_hint in aliases:
                score = 0
            elif normalized_hint in self._normalize_key(course_name):
                score = 1
            else:
                continue

            if best_match is None or score < best_match[0]:
                best_match = (score, course_id)

        if best_match is None:
            raise ValueError(f"Course not found for hint: {course_hint}")

        return best_match[1]

    def _resolve_curriculum_id(self, cursor: Any, curriculum_name: str, course_id: int) -> int:
        name_column = self._resolve_curriculum_name_column(cursor)
        table = self._table("curriculum")

        query = (
            f"SELECT id FROM {table} "
            f"WHERE {name_column} = {self._placeholder} AND course = {self._placeholder}"
        )
        cursor.execute(query, (curriculum_name, course_id))
        row = cursor.fetchone()
        if row is not None:
            return int(row[0])

        query = f"SELECT id FROM {table} WHERE {name_column} = {self._placeholder}"
        cursor.execute(query, (curriculum_name,))
        row = cursor.fetchone()
        if row is not None:
            return int(row[0])

        raise ValueError(
            f"Curriculum not found: {curriculum_name}. Seed curriculum data first."
        )

    def _resolve_curriculum_name_column(self, cursor: Any) -> str:
        columns = self._get_table_columns(cursor, "curriculum")
        if "name" in columns:
            return "name"
        if "semester" in columns:
            return "semester"
        raise RuntimeError("Could not determine curriculum name column (expected name or semester)")

    def _ensure_students_curriculum_schema(self, cursor: Any) -> None:
        columns = self._get_table_columns(cursor, "students")
        if "curriculum_id" in columns:
            return

        table = self._table("students")
        try:
            cursor.execute(f"ALTER TABLE {table} ADD COLUMN curriculum_id BIGINT")
        except Exception as error:
            if not self._is_ignorable_schema_error(error):
                raise

    def _load_users_by_email(self, cursor: Any) -> dict[str, int]:
        table = self._table("users")
        cursor.execute(f"SELECT id, email FROM {table}")
        mapping: dict[str, int] = {}
        for row in cursor.fetchall():
            if row[1] is None:
                continue
            mapping[str(row[1]).strip().lower()] = int(row[0])
        return mapping

    def _load_students_by_id(self, cursor: Any) -> dict[str, int]:
        table = self._table("students")
        cursor.execute(f"SELECT student_id, user_id FROM {table}")
        mapping: dict[str, int] = {}
        for row in cursor.fetchall():
            if row[0] is None or row[1] is None:
                continue
            mapping[str(row[0]).strip()] = int(row[1])
        return mapping

    def _create_user(self, cursor: Any, email: str, hashed_password: str) -> int:
        inserted_id = self.db_manager.execute_insert(
            "users",
            ["email", "password", "role"],
            [email, hashed_password, "STUDENT"],
            return_id=True,
            cursor=cursor,
        )
        if inserted_id is None:
            raise RuntimeError(f"Failed to create user for {email}")
        return inserted_id

    def _update_user(self, cursor: Any, user_id: int, email: str, hashed_password: str) -> None:
        table = self._table("users")
        query = (
            f"UPDATE {table} SET email = {self._placeholder}, "
            f"password = {self._placeholder}, role = {self._placeholder} "
            f"WHERE id = {self._placeholder}"
        )
        cursor.execute(query, (email, hashed_password, "STUDENT", user_id))

    def _create_student(
        self,
        cursor: Any,
        row: StudentCsvRow,
        user_id: int,
        course_id: int,
        curriculum_id: int,
    ) -> None:
        self.db_manager.execute_insert(
            "students",
            [
                "student_id",
                "user_id",
                "first_name",
                "last_name",
                "middle_name",
                "birthdate",
                "student_status",
                "course_id",
                "curriculum_id",
                "year_level",
            ],
            [
                row.student_id,
                user_id,
                row.first_name,
                row.last_name,
                row.middle_name,
                self._format_date(row.birthdate),
                row.status,
                course_id,
                curriculum_id,
                row.year_level,
            ],
            return_id=False,
            cursor=cursor,
        )

    def _update_student(
        self,
        cursor: Any,
        row: StudentCsvRow,
        user_id: int,
        course_id: int,
        curriculum_id: int,
    ) -> None:
        table = self._table("students")
        query = (
            f"UPDATE {table} "
            f"SET user_id = {self._placeholder}, "
            f"first_name = {self._placeholder}, "
            f"last_name = {self._placeholder}, "
            f"middle_name = {self._placeholder}, "
            f"birthdate = {self._placeholder}, "
            f"student_status = {self._placeholder}, "
            f"course_id = {self._placeholder}, "
            f"curriculum_id = {self._placeholder}, "
            f"year_level = {self._placeholder} "
            f"WHERE student_id = {self._placeholder}"
        )
        cursor.execute(
            query,
            (
                user_id,
                row.first_name,
                row.last_name,
                row.middle_name,
                self._format_date(row.birthdate),
                row.status,
                course_id,
                curriculum_id,
                row.year_level,
                row.student_id,
            ),
        )

    def _format_date(self, value: date) -> str | date:
        if self.db_manager.db_type == "derby":
            return value.strftime("%Y-%m-%d")
        return value

    def _hash_password(self, plain_password: str) -> str:
        return bcrypt.hashpw(
            plain_password.encode("utf-8"),
            bcrypt.gensalt(rounds=self.bcrypt_rounds),
        ).decode("utf-8")

    def _build_email(self, last_name: str, first_name: str) -> str:
        normalized_last = self._normalize_identifier(last_name)
        normalized_first = self._normalize_identifier(first_name)
        return f"{normalized_last}.{normalized_first}@vbsu.edu.ph"

    def _build_plain_password(self, last_name: str, birthdate: date) -> str:
        normalized_last = self._normalize_identifier(last_name)
        return f"{normalized_last}_{birthdate.year}"

    def _normalize_identifier(self, value: str) -> str:
        return "".join(ch for ch in value.strip().lower() if ch.isalnum())

    def _normalize_key(self, value: str) -> str:
        return "".join(ch for ch in value.strip().lower() if ch.isalnum())

    def _course_aliases(self, course_name: str) -> set[str]:
        aliases = {self._normalize_key(course_name)}
        lowered = course_name.strip().lower()

        prefix = "bachelor of science in "
        if lowered.startswith(prefix):
            major = lowered[len(prefix):]
            aliases.add(self._normalize_key(major))

            initials = "".join(token[0] for token in major.split() if token)
            if initials:
                aliases.add(self._normalize_key(initials))
                aliases.add(self._normalize_key(f"bs{initials}"))

        if "information technology" in lowered:
            aliases.add("bsit")
            aliases.add("it")

        return aliases

    def _get_table_columns(self, cursor: Any, table_name: str) -> set[str]:
        table = self._table(table_name)
        if self.db_manager.db_type == "derby":
            query = f"SELECT * FROM {table} FETCH FIRST ROW ONLY"
        else:
            query = f"SELECT * FROM {table} LIMIT 1"

        cursor.execute(query)
        if not cursor.description:
            return set()

        return {str(col[0]).lower() for col in cursor.description}

    def _is_ignorable_schema_error(self, error: Exception) -> bool:
        message = str(error).lower()
        return any(
            marker in message
            for marker in (
                "already exists",
                "duplicate column",
                "duplicate key",
                "x0y32",
                "42s21",
                "errno: 1060",
                "errno: 1061",
            )
        )

    def _table(self, table_name: str) -> str:
        return f"{self._table_prefix}{table_name}"
