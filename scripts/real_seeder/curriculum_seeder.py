#!/usr/bin/env python3
"""Curriculum-only seeder using real data from CSV files."""

from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime
from pathlib import Path
from typing import Any

from seeder.core.database import DatabaseManager

from real_seeder.curriculum_parser import CurriculumCsvRow, parse_curriculum_csv


@dataclass(frozen=True, slots=True)
class CurriculumSeedSummary:
    """Summary of rows created or updated during curriculum import."""

    curriculum_id: int
    course_id: int
    subjects_created: int
    subjects_updated: int
    semesters_created: int
    semester_subject_links_created: int
    prerequisites_created: int
    prerequisite_codes_skipped: int


@dataclass(frozen=True, slots=True)
class CourseContext:
    """Resolved course context used by curriculum seeding."""

    course_id: int
    course_name: str
    department_id: int | None


class RealCurriculumSeeder:
    """Imports curriculum, subjects, semester links, and prerequisites from CSV."""

    def __init__(self, db_manager: DatabaseManager, subject_name_max_length: int = 32) -> None:
        self.db_manager = db_manager
        self.subject_name_max_length = subject_name_max_length
        self._placeholder = "?" if db_manager.db_type == "derby" else "%s"
        self._table_prefix = "APP." if db_manager.db_type == "derby" else ""

    def seed_from_csv(
        self,
        csv_path: Path,
        curriculum_name: str,
        curriculum_year: int,
        course_id: int | None,
        course_name: str,
        create_course_if_missing: bool = False,
    ) -> CurriculumSeedSummary:
        """Run curriculum seeding using a curriculum CSV file."""
        rows = parse_curriculum_csv(csv_path)

        if not self.db_manager.connect():
            raise RuntimeError("Failed to connect to database")

        cursor = self.db_manager.create_cursor()
        try:
            course_context = self._resolve_course_context(
                cursor,
                requested_course_id=course_id,
                requested_course_name=course_name,
                create_if_missing=create_course_if_missing,
            )

            curriculum_id = self._upsert_curriculum(
                cursor,
                curriculum_name=curriculum_name,
                curriculum_year=curriculum_year,
                course_id=course_context.course_id,
            )

            subject_map, subjects_created, subjects_updated = self._upsert_subjects(
                cursor,
                rows,
                department_id=course_context.department_id,
                source_label=curriculum_name,
            )

            semesters_created, semester_subject_links_created = self._upsert_semester_structure(
                cursor,
                rows,
                curriculum_id=curriculum_id,
                subject_id_by_code=subject_map,
            )

            prerequisites_created, prerequisite_codes_skipped = self._upsert_prerequisites(
                cursor,
                rows,
                subject_id_by_code=subject_map,
            )

            self.db_manager.commit()

            return CurriculumSeedSummary(
                curriculum_id=curriculum_id,
                course_id=course_context.course_id,
                subjects_created=subjects_created,
                subjects_updated=subjects_updated,
                semesters_created=semesters_created,
                semester_subject_links_created=semester_subject_links_created,
                prerequisites_created=prerequisites_created,
                prerequisite_codes_skipped=prerequisite_codes_skipped,
            )
        except Exception:
            rollback = getattr(self.db_manager.connection, "rollback", None)
            if callable(rollback):
                rollback()
            raise
        finally:
            cursor.close()
            self.db_manager.disconnect()

    def _resolve_course_context(
        self,
        cursor: Any,
        requested_course_id: int | None,
        requested_course_name: str,
        create_if_missing: bool,
    ) -> CourseContext:
        if requested_course_id is not None:
            course = self._fetch_course_by_id(cursor, requested_course_id)
            if course is None:
                raise ValueError(f"Course ID not found: {requested_course_id}")
            return course

        course = self._fetch_course_by_name(cursor, requested_course_name)
        if course is not None:
            return course

        if not create_if_missing:
            raise ValueError(
                "Course not found. Use --course-id, adjust --course-name, or pass --create-course-if-missing."
            )

        department_id = self._resolve_default_it_department_id(cursor)
        description = "Imported for real curriculum seeding"
        inserted_id = self.db_manager.execute_insert(
            "courses",
            ["course_name", "description", "department_id"],
            [requested_course_name, description, department_id],
            return_id=True,
            cursor=cursor,
        )

        if inserted_id is None:
            raise RuntimeError("Failed to create course record")

        return CourseContext(
            course_id=inserted_id,
            course_name=requested_course_name,
            department_id=department_id,
        )

    def _fetch_course_by_id(self, cursor: Any, course_id: int) -> CourseContext | None:
        table = self._table("courses")
        query = (
            f"SELECT id, course_name, department_id FROM {table} "
            f"WHERE id = {self._placeholder}"
        )
        cursor.execute(query, (course_id,))
        row = cursor.fetchone()
        if row is None:
            return None

        return CourseContext(
            course_id=int(row[0]),
            course_name=str(row[1]),
            department_id=int(row[2]) if row[2] is not None else None,
        )

    def _fetch_course_by_name(self, cursor: Any, course_name: str) -> CourseContext | None:
        table = self._table("courses")
        query = (
            f"SELECT id, course_name, department_id FROM {table} "
            f"WHERE LOWER(course_name) = LOWER({self._placeholder})"
        )
        cursor.execute(query, (course_name,))
        row = cursor.fetchone()
        if row is None:
            return None

        return CourseContext(
            course_id=int(row[0]),
            course_name=str(row[1]),
            department_id=int(row[2]) if row[2] is not None else None,
        )

    def _resolve_default_it_department_id(self, cursor: Any) -> int | None:
        table = self._table("departments")
        query = (
            f"SELECT id FROM {table} "
            f"WHERE LOWER(department_code) = LOWER({self._placeholder})"
        )
        cursor.execute(query, ("CIT",))
        row = cursor.fetchone()
        if row is not None:
            return int(row[0])

        query = (
            f"SELECT id FROM {table} "
            f"WHERE LOWER(department_name) LIKE LOWER({self._placeholder})"
        )
        cursor.execute(query, ("%information technology%",))
        row = cursor.fetchone()
        if row is not None:
            return int(row[0])

        return None

    def _upsert_curriculum(
        self,
        cursor: Any,
        curriculum_name: str,
        curriculum_year: int,
        course_id: int,
    ) -> int:
        name_column = self._resolve_curriculum_name_column(cursor)
        table = self._table("curriculum")
        query = (
            f"SELECT id FROM {table} "
            f"WHERE {name_column} = {self._placeholder}"
        )
        cursor.execute(query, (curriculum_name,))
        existing = cursor.fetchone()

        cur_year = datetime(curriculum_year, 1, 1)
        formatted_year = self.db_manager.adapter.format_datetime(cur_year)

        if existing is not None:
            curriculum_id = int(existing[0])
            update_query = (
                f"UPDATE {table} SET {name_column} = {self._placeholder}, "
                f"cur_year = {self._placeholder}, course = {self._placeholder} "
                f"WHERE id = {self._placeholder}"
            )
            cursor.execute(update_query, (curriculum_name, formatted_year, course_id, curriculum_id))
            return curriculum_id

        inserted_id = self.db_manager.execute_insert(
            "curriculum",
            [name_column, "cur_year", "course"],
            [curriculum_name, formatted_year, course_id],
            return_id=True,
            cursor=cursor,
        )

        if inserted_id is None:
            raise RuntimeError("Failed to create curriculum")

        return inserted_id

    def _resolve_curriculum_name_column(self, cursor: Any) -> str:
        columns = self._get_table_columns(cursor, "curriculum")
        if "name" in columns:
            return "name"
        if "semester" in columns:
            return "semester"
        raise RuntimeError("Could not determine curriculum name column (expected name or semester)")

    def _upsert_subjects(
        self,
        cursor: Any,
        rows: list[CurriculumCsvRow],
        department_id: int | None,
        source_label: str,
    ) -> tuple[dict[str, int], int, int]:
        unique_subjects: dict[str, CurriculumCsvRow] = {}
        for row in rows:
            unique_subjects.setdefault(row.subject_code, row)

        subject_table = self._table("subjects")
        subject_columns = self._get_table_columns(cursor, "subjects")
        has_estimated_time = "estimated_time" in subject_columns
        has_schedule_pattern = "schedule_pattern" in subject_columns

        query = f"SELECT id, subject_code FROM {subject_table}"
        cursor.execute(query)

        existing_by_code: dict[str, int] = {}
        for db_row in cursor.fetchall():
            existing_by_code[str(db_row[1]).strip()] = int(db_row[0])

        subjects_created = 0
        subjects_updated = 0
        subject_id_by_code: dict[str, int] = {}

        for subject_code, row in unique_subjects.items():
            truncated_name = row.subject_name[: self.subject_name_max_length]
            description = f"{source_label} curriculum subject: {row.subject_name}"
            estimated_time = 90
            schedule_pattern = self._resolve_schedule_pattern(subject_code, row.subject_name)

            existing_id = existing_by_code.get(subject_code)
            if existing_id is not None:
                set_clauses = [
                    f"subject_name = {self._placeholder}",
                    f"units = {self._placeholder}",
                ]
                params: list[Any] = [truncated_name, row.units]

                if has_estimated_time:
                    set_clauses.append(f"estimated_time = {self._placeholder}")
                    params.append(estimated_time)

                if has_schedule_pattern:
                    set_clauses.append(f"schedule_pattern = {self._placeholder}")
                    params.append(schedule_pattern)

                set_clauses.extend([
                    f"description = {self._placeholder}",
                    f"department_id = {self._placeholder}",
                ])
                params.extend([description, department_id, existing_id])

                update_query = (
                    f"UPDATE {subject_table} SET {', '.join(set_clauses)} "
                    f"WHERE id = {self._placeholder}"
                )
                cursor.execute(update_query, tuple(params))
                subject_id_by_code[subject_code] = existing_id
                subjects_updated += 1
                continue

            insert_columns = ["subject_name", "subject_code", "units"]
            insert_values: list[Any] = [truncated_name, subject_code, row.units]

            if has_estimated_time:
                insert_columns.append("estimated_time")
                insert_values.append(estimated_time)

            if has_schedule_pattern:
                insert_columns.append("schedule_pattern")
                insert_values.append(schedule_pattern)

            insert_columns.extend(["description", "department_id"])
            insert_values.extend([description, department_id])

            inserted_id = self.db_manager.execute_insert(
                "subjects",
                insert_columns,
                insert_values,
                return_id=True,
                cursor=cursor,
            )
            if inserted_id is None:
                raise RuntimeError(f"Failed to create subject: {subject_code}")

            subject_id_by_code[subject_code] = inserted_id
            subjects_created += 1

        return subject_id_by_code, subjects_created, subjects_updated

    def _resolve_schedule_pattern(self, subject_code: str, subject_name: str) -> str:
        descriptor = f"{subject_code} {subject_name}".upper()

        if descriptor.startswith("STC") or "NSTP" in descriptor or "CIVIC" in descriptor:
            return "NSTP_BLOCK"

        if descriptor.startswith("PPF") or "PATHFIT" in descriptor or "PHYSICAL" in descriptor:
            return "PE_PAIRED"

        if descriptor.startswith("ZGE"):
            return "GE_PAIRED"

        if descriptor.startswith("CCP") or descriptor.startswith("CDS") or descriptor.startswith("CFD"):
            return "LECTURE_LAB"

        if "LAB" in descriptor or "PRACTICUM" in descriptor:
            return "LECTURE_LAB"

        return "LECTURE_ONLY"

    def _upsert_semester_structure(
        self,
        cursor: Any,
        rows: list[CurriculumCsvRow],
        curriculum_id: int,
        subject_id_by_code: dict[str, int],
    ) -> tuple[int, int]:
        semester_table = self._table("semester")
        semester_subjects_table = self._table("semester_subjects")

        has_year_level = self._semester_has_year_level(cursor)

        unique_semesters: list[tuple[int, str]] = sorted(
            {(row.year_level, row.semester_label) for row in rows},
            key=self._semester_sort_key,
        )

        semester_id_by_key: dict[tuple[int, str], int] = {}
        semesters_created = 0
        for year_level, semester_label in unique_semesters:
            semester_id = self._find_semester_id(
                cursor,
                curriculum_id=curriculum_id,
                semester_label=semester_label,
                year_level=year_level,
                has_year_level=has_year_level,
            )
            if semester_id is None:
                if has_year_level:
                    semester_id = self.db_manager.execute_insert(
                        "semester",
                        ["curriculum_id", "semester", "year_level"],
                        [curriculum_id, semester_label, year_level],
                        return_id=True,
                        cursor=cursor,
                    )
                else:
                    semester_id = self.db_manager.execute_insert(
                        "semester",
                        ["curriculum_id", "semester"],
                        [curriculum_id, semester_label],
                        return_id=True,
                        cursor=cursor,
                    )
                if semester_id is None:
                    raise RuntimeError(
                        f"Failed to create semester for year {year_level} {semester_label}"
                    )
                semesters_created += 1

            semester_id_by_key[(year_level, semester_label)] = semester_id

        cursor.execute(f"SELECT semester_id, subject_id FROM {semester_subjects_table}")
        existing_links = {(int(row[0]), int(row[1])) for row in cursor.fetchall()}

        links_created = 0
        for row in rows:
            subject_id = subject_id_by_code.get(row.subject_code)
            if subject_id is None:
                continue

            semester_id = semester_id_by_key[(row.year_level, row.semester_label)]
            key = (semester_id, subject_id)
            if key in existing_links:
                continue

            self.db_manager.execute_insert(
                "semester_subjects",
                ["semester_id", "subject_id"],
                [semester_id, subject_id],
                return_id=False,
                cursor=cursor,
            )
            existing_links.add(key)
            links_created += 1

        return semesters_created, links_created

    def _semester_has_year_level(self, cursor: Any) -> bool:
        columns = self._get_table_columns(cursor, "semester")
        return "year_level" in columns

    def _find_semester_id(
        self,
        cursor: Any,
        curriculum_id: int,
        semester_label: str,
        year_level: int,
        has_year_level: bool,
    ) -> int | None:
        table = self._table("semester")
        if has_year_level:
            query = (
                f"SELECT id FROM {table} "
                f"WHERE curriculum_id = {self._placeholder} "
                f"AND semester = {self._placeholder} "
                f"AND year_level = {self._placeholder}"
            )
            cursor.execute(query, (curriculum_id, semester_label, year_level))
        else:
            query = (
                f"SELECT id FROM {table} "
                f"WHERE curriculum_id = {self._placeholder} "
                f"AND semester = {self._placeholder}"
            )
            cursor.execute(query, (curriculum_id, semester_label))

        row = cursor.fetchone()
        if row is None:
            return None
        return int(row[0])

    def _upsert_prerequisites(
        self,
        cursor: Any,
        rows: list[CurriculumCsvRow],
        subject_id_by_code: dict[str, int],
    ) -> tuple[int, int]:
        table = self._table("prerequisites")
        cursor.execute(f"SELECT pre_subject_id, subject_id FROM {table}")
        existing_pairs = {(int(row[0]), int(row[1])) for row in cursor.fetchall()}

        created = 0
        skipped = 0
        for row in rows:
            subject_id = subject_id_by_code.get(row.subject_code)
            if subject_id is None:
                continue

            for prerequisite_code in row.prerequisite_codes:
                prerequisite_id = subject_id_by_code.get(prerequisite_code)
                if prerequisite_id is None:
                    skipped += 1
                    continue

                key = (prerequisite_id, subject_id)
                if key in existing_pairs:
                    continue

                self.db_manager.execute_insert(
                    "prerequisites",
                    ["pre_subject_id", "subject_id"],
                    [prerequisite_id, subject_id],
                    return_id=False,
                    cursor=cursor,
                )
                existing_pairs.add(key)
                created += 1

        return created, skipped

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

    def _semester_sort_key(self, item: tuple[int, str]) -> tuple[int, int, str]:
        year_level, semester_label = item
        semester_order = {
            "semester 1": 1,
            "semester 2": 2,
            "summer": 3,
        }
        normalized = semester_label.strip().lower()
        return year_level, semester_order.get(normalized, 99), normalized

    def _table(self, table_name: str) -> str:
        return f"{self._table_prefix}{table_name}"
