#!/usr/bin/env python3
"""Seeder for initializing baseline student semester progress rows."""

from __future__ import annotations

from dataclasses import dataclass
from typing import Any

from seeder.core.database import DatabaseManager


@dataclass(frozen=True, slots=True)
class StudentSemesterProgressSeedSummary:
    """Summary of baseline semester progress rows created."""

    rows_created: int
    rows_skipped: int


class RealStudentSemesterProgressSeeder:
    """Seeds one baseline semester progress row per student."""

    def __init__(self, db_manager: DatabaseManager) -> None:
        self.db_manager = db_manager
        self._placeholder = "?" if db_manager.db_type == "derby" else "%s"
        self._table_prefix = "APP." if db_manager.db_type == "derby" else ""

    def seed(self) -> StudentSemesterProgressSeedSummary:
        """Seed first-year first-semester NOT_STARTED progress for each student."""
        if not self.db_manager.connect():
            raise RuntimeError("Failed to connect to database")

        cursor = self.db_manager.create_cursor()
        try:
            existing_pairs = self._load_existing_progress_pairs(cursor)
            students = self._load_students(cursor)

            rows_created = 0
            rows_skipped = 0
            for student_id, curriculum_id, course_id in students:
                effective_curriculum_id = curriculum_id
                if effective_curriculum_id is None:
                    effective_curriculum_id = self._resolve_latest_curriculum_id(cursor, course_id)

                if effective_curriculum_id is None:
                    raise RuntimeError(
                        f"Missing curriculum for student {student_id}. Cannot seed semester progress."
                    )

                first_semester_id = self._resolve_first_semester_id(cursor, effective_curriculum_id)
                if first_semester_id is None:
                    raise RuntimeError(
                        f"Missing first semester for curriculum {effective_curriculum_id}."
                    )

                key = (student_id, first_semester_id)
                if key in existing_pairs:
                    rows_skipped += 1
                    continue

                self.db_manager.execute_insert(
                    "student_semester_progress",
                    [
                        "student_id",
                        "curriculum_id",
                        "semester_id",
                        "status",
                        "started_at",
                        "completed_at",
                    ],
                    [
                        student_id,
                        effective_curriculum_id,
                        first_semester_id,
                        "NOT_STARTED",
                        None,
                        None,
                    ],
                    return_id=False,
                    cursor=cursor,
                )
                existing_pairs.add(key)
                rows_created += 1

            self.db_manager.commit()
            return StudentSemesterProgressSeedSummary(
                rows_created=rows_created,
                rows_skipped=rows_skipped,
            )
        except Exception:
            rollback = getattr(self.db_manager.connection, "rollback", None)
            if callable(rollback):
                rollback()
            raise
        finally:
            cursor.close()
            self.db_manager.disconnect()

    def _load_students(self, cursor: Any) -> list[tuple[str, int | None, int | None]]:
        table = self._table("students")
        cursor.execute(f"SELECT student_id, curriculum_id, course_id FROM {table}")

        students: list[tuple[str, int | None, int | None]] = []
        for row in cursor.fetchall():
            if row[0] is None:
                continue

            student_id = str(row[0]).strip()
            curriculum_id = int(row[1]) if row[1] is not None else None
            course_id = int(row[2]) if row[2] is not None else None
            students.append((student_id, curriculum_id, course_id))

        return students

    def _load_existing_progress_pairs(self, cursor: Any) -> set[tuple[str, int]]:
        table = self._table("student_semester_progress")
        cursor.execute(f"SELECT student_id, semester_id FROM {table}")

        pairs: set[tuple[str, int]] = set()
        for row in cursor.fetchall():
            if row[0] is None or row[1] is None:
                continue
            pairs.add((str(row[0]).strip(), int(row[1])))

        return pairs

    def _resolve_latest_curriculum_id(self, cursor: Any, course_id: int | None) -> int | None:
        if course_id is None:
            return None

        table = self._table("curriculum")
        limit_clause = "FETCH FIRST 1 ROWS ONLY" if self.db_manager.db_type == "derby" else "LIMIT 1"
        query = (
            f"SELECT id FROM {table} "
            f"WHERE course = {self._placeholder} "
            f"ORDER BY cur_year DESC, created_at DESC {limit_clause}"
        )
        cursor.execute(query, (course_id,))
        row = cursor.fetchone()
        if row is None:
            return None
        return int(row[0])

    def _resolve_first_semester_id(self, cursor: Any, curriculum_id: int) -> int | None:
        table = self._table("semester")
        has_year_level = self._semester_has_year_level(cursor)
        limit_clause = "FETCH FIRST 1 ROWS ONLY" if self.db_manager.db_type == "derby" else "LIMIT 1"

        semester_rank = (
            "CASE "
            "WHEN UPPER(TRIM(semester)) LIKE '%1ST%' OR UPPER(TRIM(semester)) LIKE '%FIRST%' OR UPPER(TRIM(semester)) = 'SEMESTER 1' THEN 1 "
            "WHEN UPPER(TRIM(semester)) LIKE '%2ND%' OR UPPER(TRIM(semester)) LIKE '%SECOND%' OR UPPER(TRIM(semester)) = 'SEMESTER 2' THEN 2 "
            "WHEN UPPER(TRIM(semester)) LIKE '%3RD%' OR UPPER(TRIM(semester)) LIKE '%THIRD%' OR UPPER(TRIM(semester)) = 'SEMESTER 3' THEN 3 "
            "WHEN UPPER(TRIM(semester)) LIKE '%SUMMER%' THEN 9 "
            "ELSE 99 END"
        )

        if has_year_level:
            query = (
                f"SELECT id FROM {table} "
                f"WHERE curriculum_id = {self._placeholder} "
                f"ORDER BY year_level ASC, {semester_rank}, created_at ASC, id ASC "
                f"{limit_clause}"
            )
        else:
            query = (
                f"SELECT id FROM {table} "
                f"WHERE curriculum_id = {self._placeholder} "
                f"ORDER BY {semester_rank}, created_at ASC, id ASC "
                f"{limit_clause}"
            )

        cursor.execute(query, (curriculum_id,))
        row = cursor.fetchone()
        if row is None:
            return None
        return int(row[0])

    def _semester_has_year_level(self, cursor: Any) -> bool:
        columns = self._get_table_columns(cursor, "semester")
        return "year_level" in columns

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

    def _table(self, table_name: str) -> str:
        return f"{self._table_prefix}{table_name}"
