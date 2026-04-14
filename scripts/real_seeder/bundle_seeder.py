#!/usr/bin/env python3
"""One-command reset-and-seed workflow for BSIT + NITEN2023 + students."""

from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
import re
from typing import Any

from seeder.core.database import DatabaseManager

from real_seeder.curriculum_seeder import CurriculumSeedSummary, RealCurriculumSeeder
from real_seeder.room_seeder import RealRoomSeeder, RoomSeedSummary
from real_seeder.staff_seeder import RealStaffSeeder, StaffSeedSummary
from real_seeder.student_semester_progress_seeder import (
    RealStudentSemesterProgressSeeder,
    StudentSemesterProgressSeedSummary,
)
from real_seeder.students_seeder import RealStudentsSeeder, StudentsSeedSummary


@dataclass(frozen=True, slots=True)
class BundleSeedSummary:
    """Summary for bundled reset-and-seed operations."""

    cleared_tables: tuple[str, ...]
    curriculum: CurriculumSeedSummary
    students: StudentsSeedSummary
    semester_progress: StudentSemesterProgressSeedSummary
    staff: StaffSeedSummary
    rooms: RoomSeedSummary


class BsitNiten2023BundleSeeder:
    """Clears related tables and seeds only BSIT, NITEN2023, and students."""

    REQUIRED_TABLES: tuple[str, ...] = (
        "courses",
        "admins",
        "curriculum",
        "departments",
        "faculty_student_drop_requests",
        "prerequisites",
        "semester",
        "semester_subjects",
        "student_semester_progress",
        "students",
        "subjects",
        "users",
    )

    TABLES_TO_CLEAR: tuple[str, ...] = (
        "student_semester_progress",
        "student_enrolled_subjects",
        "faculty_student_drop_requests",
        "enrollments_details",
        "schedules",
        "offerings",
        "enrollments",
        "semester_subjects",
        "prerequisites",
        "semester",
        "students",
        "faculty",
        "registrar",
        "admins",
        "password_reset_tokens",
        "users",
        "subjects",
        "curriculum",
        "courses",
        "rooms",
    )

    DERBY_TABLES_TO_DROP: tuple[str, ...] = (
        "student_semester_progress",
        "student_enrolled_subjects",
        "faculty_student_drop_requests",
        "enrollments_details",
        "schedules",
        "offerings",
        "enrollments",
        "semester_subjects",
        "prerequisites",
        "sections",
        "subjects",
        "semester",
        "faculty",
        "registrar",
        "students",
        "admins",
        "password_reset_tokens",
        "curriculum",
        "users",
        "rooms",
        "courses",
        "departments",
        "enrollment_period",
    )

    def __init__(self, db_manager: DatabaseManager, bcrypt_rounds: int = 12) -> None:
        self.db_manager = db_manager
        self.bcrypt_rounds = bcrypt_rounds
        self._table_prefix = "APP." if db_manager.db_type == "derby" else ""

    def run(
        self,
        curriculum_csv_path: Path,
        students_csv_path: Path,
        course_name: str = "Bachelor of Science in Information Technology",
        curriculum_name: str = "NITEN2023",
        curriculum_year: int = 2023,
        students_course_hint: str = "BSIT",
        faculty_count: int = 10,
    ) -> BundleSeedSummary:
        """Execute reset + curriculum/students/faculty/registrar seeding."""
        if self.db_manager.db_type == "derby":
            cleared_tables = self._rebuild_derby_schema()
        else:
            self._ensure_schema_ready()
            cleared_tables = self._clear_target_tables()

        curriculum_summary = RealCurriculumSeeder(self.db_manager).seed_from_csv(
            csv_path=curriculum_csv_path,
            curriculum_name=curriculum_name,
            curriculum_year=curriculum_year,
            course_id=None,
            course_name=course_name,
            create_course_if_missing=True,
        )

        students_summary = RealStudentsSeeder(
            self.db_manager,
            bcrypt_rounds=self.bcrypt_rounds,
        ).seed_from_csv(
            csv_path=students_csv_path,
            course_hint=students_course_hint,
            curriculum_name=curriculum_name,
        )

        semester_progress_summary = RealStudentSemesterProgressSeeder(self.db_manager).seed()

        staff_summary = RealStaffSeeder(
            self.db_manager,
            bcrypt_rounds=self.bcrypt_rounds,
        ).seed(faculty_count=faculty_count)

        rooms_summary = RealRoomSeeder(self.db_manager).seed()

        return BundleSeedSummary(
            cleared_tables=cleared_tables,
            curriculum=curriculum_summary,
            students=students_summary,
            semester_progress=semester_progress_summary,
            staff=staff_summary,
            rooms=rooms_summary,
        )

    def _rebuild_derby_schema(self) -> tuple[str, ...]:
        if not self.db_manager.connect():
            raise RuntimeError("Failed to connect to database")

        cursor = self.db_manager.connection.cursor()
        reset_actions: list[str] = []
        try:
            print("Starting Derby bundle reset")
            self._drop_derby_foreign_keys(cursor)
            for table in self.DERBY_TABLES_TO_DROP:
                try:
                    print(f"Dropping Derby table: {table}")
                    cursor.execute(f"DROP TABLE {self._table(table)}")
                    reset_actions.append(f"dropped:{table}")
                except Exception as error:
                    print(f"Failed to drop Derby table {table}: {error}")
                    if not self._is_ignorable_drop_error(error):
                        raise

            schema_path = self._resolve_schema_file_path()
            print(f"Recreating Derby schema from {schema_path.name}")
            for statement in self._load_schema_statements(schema_path):
                try:
                    cursor.execute(statement)
                except Exception as error:
                    print(f"Failed to execute Derby schema statement: {error}")
                    if not self._is_ignorable_schema_error(error):
                        raise

            reset_actions.append(f"reran:{schema_path.name}")
            self.db_manager.commit()
            print("Derby bundle reset completed")
            return tuple(reset_actions)
        except Exception:
            rollback = getattr(self.db_manager.connection, "rollback", None)
            if callable(rollback):
                rollback()
            raise
        finally:
            cursor.close()
            self.db_manager.disconnect()

    def _drop_derby_foreign_keys(self, cursor: Any) -> None:
        cursor.execute(
            "SELECT t.TABLENAME, c.CONSTRAINTNAME "
            "FROM SYS.SYSCONSTRAINTS c "
            "JOIN SYS.SYSTABLES t ON c.TABLEID = t.TABLEID "
            "JOIN SYS.SYSSCHEMAS s ON t.SCHEMAID = s.SCHEMAID "
            "WHERE s.SCHEMANAME = ? AND c.TYPE = 'F' "
            "ORDER BY t.TABLENAME, c.CONSTRAINTNAME",
            ("APP",),
        )
        foreign_keys = cursor.fetchall()

        for table_name, constraint_name in foreign_keys:
            table_name_text = str(table_name)
            constraint_name_text = str(constraint_name)
            try:
                print(
                    "Dropping Derby foreign key constraint: "
                    f"{table_name_text}.{constraint_name_text}"
                )
                cursor.execute(
                    f"ALTER TABLE {self._table(table_name_text.lower())} "
                    f"DROP CONSTRAINT {constraint_name_text}"
                )
            except Exception as error:
                print(
                    "Failed to drop Derby foreign key constraint "
                    f"{table_name_text}.{constraint_name_text}: {error}"
                )
                if not self._is_ignorable_drop_error(error):
                    raise

    def _ensure_schema_ready(self) -> None:
        if not self.db_manager.connect():
            raise RuntimeError("Failed to connect to database")

        cursor = self.db_manager.connection.cursor()
        try:
            missing_tables = self._find_missing_required_tables(cursor)
            if not missing_tables:
                return

            schema_path = self._resolve_schema_file_path()
            for statement in self._load_schema_statements(schema_path):
                try:
                    cursor.execute(statement)
                except Exception as error:
                    if not self._is_ignorable_schema_error(error):
                        raise

            self.db_manager.commit()
        except Exception:
            rollback = getattr(self.db_manager.connection, "rollback", None)
            if callable(rollback):
                rollback()
            raise
        finally:
            cursor.close()
            self.db_manager.disconnect()

    def _find_missing_required_tables(self, cursor: Any) -> list[str]:
        missing: list[str] = []
        for table_name in self.REQUIRED_TABLES:
            if not self._table_exists(cursor, table_name):
                missing.append(table_name)
        return missing

    def _table_exists(self, cursor: Any, table_name: str) -> bool:
        if self.db_manager.db_type == "derby":
            cursor.execute(
                "SELECT COUNT(*) FROM SYS.SYSTABLES t "
                "JOIN SYS.SYSSCHEMAS s ON t.SCHEMAID = s.SCHEMAID "
                "WHERE s.SCHEMANAME = ? AND t.TABLETYPE = 'T' AND t.TABLENAME = ?",
                ("APP", table_name.upper()),
            )
            row = cursor.fetchone()
            return bool(row and int(row[0]) > 0)

        cursor.execute(
            "SELECT COUNT(*) FROM information_schema.tables "
            "WHERE table_schema = %s AND table_name = %s",
            (self.db_manager.database, table_name),
        )
        row = cursor.fetchone()
        return bool(row and int(row[0]) > 0)

    def _resolve_schema_file_path(self) -> Path:
        project_root = Path(__file__).resolve().parents[2]
        schema_file = "derby.sql" if self.db_manager.db_type == "derby" else "import.sql"
        schema_path = project_root / "src" / "main" / "resources" / "db" / schema_file
        if not schema_path.exists():
            raise FileNotFoundError(f"Schema file not found: {schema_path}")
        return schema_path

    def _load_schema_statements(self, schema_path: Path) -> list[str]:
        sql_content = schema_path.read_text(encoding="utf-8")
        sql_without_block_comments = re.sub(r"/\*.*?\*/", "", sql_content, flags=re.DOTALL)

        cleaned_lines: list[str] = []
        for line in sql_without_block_comments.splitlines():
            stripped = line.strip()
            if stripped.startswith("--") or stripped.startswith("#"):
                continue
            cleaned_lines.append(line)

        normalized_sql = "\n".join(cleaned_lines)
        statements = [statement.strip() for statement in normalized_sql.split(";")]
        return [statement for statement in statements if statement]

    def _clear_target_tables(self) -> tuple[str, ...]:
        if not self.db_manager.connect():
            raise RuntimeError("Failed to connect to database")

        cursor = self.db_manager.connection.cursor()
        cleared: list[str] = []
        try:
            for table in self.TABLES_TO_CLEAR:
                try:
                    cursor.execute(f"DELETE FROM {self._table(table)}")
                    cleared.append(table)
                except Exception as error:
                    if not self._is_ignorable_clear_error(error):
                        raise

            self.db_manager.commit()
            return tuple(cleared)
        except Exception:
            rollback = getattr(self.db_manager.connection, "rollback", None)
            if callable(rollback):
                rollback()
            raise
        finally:
            cursor.close()
            self.db_manager.disconnect()

    def _is_ignorable_clear_error(self, error: Exception) -> bool:
        message = str(error).lower()
        return any(
            marker in message
            for marker in (
                "does not exist",
                "not found",
                "42x05",
                "42s02",
            )
        )

    def _is_ignorable_drop_error(self, error: Exception) -> bool:
        message = str(error).lower()
        return any(
            marker in message
            for marker in (
                "does not exist",
                "not found",
                "42y55",
                "42s02",
            )
        )

    def _is_ignorable_schema_error(self, error: Exception) -> bool:
        message = str(error).lower()
        return any(
            marker in message
            for marker in (
                "already exists",
                "duplicate",
                "already an object",
                "x0y32",
                "x0y68",
                "x0y56",
                "42s01",
                "42y55",
                "errno: 1050",
                "errno: 1061",
            )
        )

    def _table(self, table_name: str) -> str:
        return f"{self._table_prefix}{table_name}"
