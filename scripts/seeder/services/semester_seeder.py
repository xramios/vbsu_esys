#!/usr/bin/env python3
"""
Semester seeder module.

Generates semesters and semester_subjects.
"""

import random
import re
from typing import TYPE_CHECKING
from tqdm import tqdm

from seeder.services.base_seeder import BaseSeeder
from seeder.config.constants import BACHELOR_MAX_YEAR
from seeder.models.data_models import Semester, SemesterSubject

if TYPE_CHECKING:
    from seeder.core.database import DatabaseManager
    from seeder.models.data_models import SeedingState


class SemesterSeeder(BaseSeeder):
    """Seeder for semesters and semester_subjects."""

    SEMESTER_CREATE_SQL = """
        CREATE TABLE APP.semester (
            id BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
            curriculum_id BIGINT NOT NULL,
            semester VARCHAR(24) NOT NULL,
            year_level INT NOT NULL,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (curriculum_id) REFERENCES APP.curriculum(id)
        )
    """

    SEMESTER_SUBJECTS_CREATE_SQL = """
        CREATE TABLE APP.semester_subjects (
            id BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
            semester_id BIGINT NOT NULL,
            subject_id BIGINT NOT NULL,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            UNIQUE (semester_id, subject_id),
            UNIQUE (id, subject_id),
            FOREIGN KEY (semester_id) REFERENCES APP.semester(id),
            FOREIGN KEY (subject_id) REFERENCES APP.subjects(id)
        )
    """

    def __init__(self, db_manager: "DatabaseManager", state: "SeedingState") -> None:
        """Initialize semester seeder.

        Args:
            db_manager: Database manager instance
            state: Shared seeding state
        """
        super().__init__(db_manager, state)

    def seed(self) -> None:
        """Execute all semester-related seeding operations."""
        self.seed_semesters()
        self.seed_semester_subjects()

    def seed_semesters(self) -> None:
        """Seed semester table linked to curriculum."""
        print("Seeding semesters...")

        if self.db_manager.db_type == "derby":
            self.create_table_if_not_exists("semester", self.SEMESTER_CREATE_SQL)

        cursor = self.db_manager.connection.cursor()
        try:
            courses_by_id = {course.id: course for course in self.state.courses}

            for curriculum in tqdm(self.state.curriculums, desc="Creating semesters", unit="curriculum"):
                course = courses_by_id.get(curriculum.course_id)
                max_year_level = self._resolve_max_year_level(
                    course.description if course else None
                )

                for year_level in range(1, max_year_level + 1):
                    for sem_num in [1, 2]:
                        semester_name = f"Semester {sem_num}"

                        if self.db_manager.db_type == "derby":
                            query = """
                                INSERT INTO APP.semester (curriculum_id, semester, year_level)
                                VALUES (?, ?, ?)
                            """
                            cursor.execute(query, (curriculum.id, semester_name, year_level))
                        else:
                            query = """
                                INSERT INTO semester (curriculum_id, semester, year_level)
                                VALUES (%s, %s, %s)
                            """
                            cursor.execute(query, (curriculum.id, semester_name, year_level))

                        last_id = self.adapter.get_last_insert_id(cursor, "semester")

                        self.state.semesters.append(
                            Semester(
                                id=last_id,
                                curriculum_id=curriculum.id,
                                semester=semester_name,
                                year_level=year_level,
                            )
                        )

            self.db_manager.commit()
        finally:
            cursor.close()

        print(f"Created {len(self.state.semesters)} semesters")

    def seed_semester_subjects(self) -> None:
        """Seed semester_subjects table linking semesters to subjects."""
        print("Seeding semester subjects...")

        if self.db_manager.db_type == "derby":
            self.create_table_if_not_exists("semester_subjects", self.SEMESTER_SUBJECTS_CREATE_SQL)

        cursor = self.db_manager.connection.cursor()
        try:
            courses_by_id = {course.id: course for course in self.state.courses}
            department_subjects = {}
            for subject in self.state.subjects:
                if subject.department_id not in department_subjects:
                    department_subjects[subject.department_id] = []
                department_subjects[subject.department_id].append(subject)

            for semester in tqdm(self.state.semesters, desc="Creating semester subjects", unit="semester"):
                # Find curriculum for this semester
                curriculum = next(
                    (c for c in self.state.curriculums if c.id == semester.curriculum_id),
                    None
                )

                if not curriculum:
                    continue

                course = courses_by_id.get(curriculum.course_id)
                subjects = department_subjects.get(course.department_id, []) if course else []

                if subjects:
                    max_subjects = min(8, len(subjects))
                    if max_subjects == 0:
                        continue
                    min_subjects = min(5, max_subjects)
                    selected_subjects = random.sample(
                        subjects,
                        random.randint(min_subjects, max_subjects),
                    )
                else:
                    max_subjects = min(6, len(self.state.subjects))
                    if max_subjects == 0:
                        continue
                    min_subjects = min(3, max_subjects)
                    selected_subjects = random.sample(
                        self.state.subjects,
                        random.randint(min_subjects, max_subjects),
                    )

                for subject in selected_subjects:
                    if self.db_manager.db_type == "derby":
                        query = """
                            INSERT INTO APP.semester_subjects (semester_id, subject_id)
                            VALUES (?, ?)
                        """
                        cursor.execute(query, (semester.id, subject.id))
                    else:
                        query = """
                            INSERT INTO semester_subjects (semester_id, subject_id)
                            VALUES (%s, %s)
                        """
                        cursor.execute(query, (semester.id, subject.id))

                    last_id = self.adapter.get_last_insert_id(cursor, "semester_subjects")

                    self.state.semester_subjects.append(
                        SemesterSubject(
                            id=last_id,
                            semester_id=semester.id,
                            subject_id=subject.id,
                        )
                    )

            self.db_manager.commit()
        finally:
            cursor.close()

        print(f"Created {len(self.state.semester_subjects)} semester subject links")

    @staticmethod
    def _resolve_max_year_level(course_description: str | None) -> int:
        """Infer curriculum max year level from course description text."""
        if not course_description:
            return 4

        match = re.search(r"(\d+)\s*-\s*year", course_description, flags=re.IGNORECASE)
        if not match:
            return 4

        parsed_value = int(match.group(1))
        if parsed_value < 1:
            return 1
        if parsed_value > BACHELOR_MAX_YEAR:
            return BACHELOR_MAX_YEAR
        return parsed_value
