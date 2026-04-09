#!/usr/bin/env python3
"""
Semester seeder module.

Generates semesters, semester_subjects, and student_subjects.
"""

import random
from typing import TYPE_CHECKING
from tqdm import tqdm

from seeder.services.base_seeder import BaseSeeder
from seeder.models.data_models import Semester, SemesterSubject, StudentEnrolledSubject

if TYPE_CHECKING:
    from seeder.core.database import DatabaseManager
    from seeder.models.data_models import SeedingState


class SemesterSeeder(BaseSeeder):
    """Seeder for semesters, semester_subjects, and student_subjects."""

    SEMESTER_CREATE_SQL = """
        CREATE TABLE APP.semester (
            id BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
            curriculum_id BIGINT NOT NULL,
            semester VARCHAR(24) NOT NULL,
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
            year_level INT NOT NULL,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (semester_id) REFERENCES APP.semester(id),
            FOREIGN KEY (subject_id) REFERENCES APP.subjects(id)
        )
    """

    STUDENT_ENROLLED_SUBJECTS_CREATE_SQL = """
        CREATE TABLE APP.student_enrolled_subjects (
            student_id VARCHAR(32) NOT NULL,
            semester_subject_id BIGINT NOT NULL,
            status VARCHAR(20) CHECK (status IN ('ENROLLED', 'COMPLETED', 'DROPPED')) NOT NULL DEFAULT 'ENROLLED',
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            PRIMARY KEY (student_id, semester_subject_id),
            FOREIGN KEY (student_id) REFERENCES APP.students(student_id),
            FOREIGN KEY (semester_subject_id) REFERENCES APP.semester_subjects(id)
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
        self.seed_student_subjects()

    def seed_semesters(self) -> None:
        """Seed semester table linked to curriculum."""
        print("Seeding semesters...")

        if self.db_manager.db_type == "derby":
            self.create_table_if_not_exists("semester", self.SEMESTER_CREATE_SQL)

        cursor = self.db_manager.connection.cursor()
        try:
            for curriculum in tqdm(self.state.curriculums, desc="Creating semesters", unit="curriculum"):
                # Create 2 semesters per curriculum (1st and 2nd semester)
                for sem_num in [1, 2]:
                    semester_name = f"Semester {sem_num}"

                    if self.db_manager.db_type == "derby":
                        query = """
                            INSERT INTO APP.semester (curriculum_id, semester)
                            VALUES (?, ?)
                        """
                        cursor.execute(query, (curriculum.id, semester_name))
                    else:
                        query = """
                            INSERT INTO semester (curriculum_id, semester)
                            VALUES (%s, %s)
                        """
                        cursor.execute(query, (curriculum.id, semester_name))

                    last_id = self.adapter.get_last_insert_id(cursor, "semester")

                    self.state.semesters.append(
                        Semester(
                            id=last_id,
                            curriculum_id=curriculum.id,
                            semester=semester_name,
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
                    selected_subjects = random.sample(subjects, min(15, len(subjects)))
                else:
                    # Fallback: assign random subjects if there are no department subjects.
                    selected_subjects = random.sample(self.state.subjects, min(10, len(self.state.subjects)))

                # Assign subjects to this semester with year levels (1-4)
                for subject in selected_subjects:
                    year_level = random.randint(1, 4)

                    if self.db_manager.db_type == "derby":
                        query = """
                            INSERT INTO APP.semester_subjects (semester_id, subject_id, year_level)
                            VALUES (?, ?, ?)
                        """
                        cursor.execute(query, (semester.id, subject.id, year_level))
                    else:
                        query = """
                            INSERT INTO semester_subjects (semester_id, subject_id, year_level)
                            VALUES (%s, %s, %s)
                        """
                        cursor.execute(query, (semester.id, subject.id, year_level))

                    last_id = self.adapter.get_last_insert_id(cursor, "semester_subjects")

                    self.state.semester_subjects.append(
                        SemesterSubject(
                            id=last_id,
                            semester_id=semester.id,
                            subject_id=subject.id,
                            year_level=year_level,
                        )
                    )

            self.db_manager.commit()
        finally:
            cursor.close()

        print(f"Created {len(self.state.semester_subjects)} semester subject links")

    def seed_student_subjects(self) -> None:
        """Seed student_subjects table with student enrollments."""
        print("Seeding student subjects...")

        if self.db_manager.db_type == "derby":
            self.create_table_if_not_exists("student_enrolled_subjects", self.STUDENT_ENROLLED_SUBJECTS_CREATE_SQL)

        cursor = self.db_manager.connection.cursor()
        try:
            statuses = ["ENROLLED", "COMPLETED", "DROPPED"]
            status_weights = [0.5, 0.4, 0.1]  # 50% enrolled, 40% completed, 10% dropped

            for student in tqdm(self.state.students, desc="Creating student subjects", unit="student"):
                # Find semester subjects for this student's course
                student_course_curriculums = [
                    c for c in self.state.curriculums if c.course_id == student.course_id
                ]

                student_semesters = [
                    s for s in self.state.semesters
                    if s.curriculum_id in [c.id for c in student_course_curriculums]
                ]

                student_semester_subjects = [
                    ss for ss in self.state.semester_subjects
                    if ss.semester_id in [s.id for s in student_semesters]
                    and ss.year_level <= student.year_level
                ]

                if not student_semester_subjects:
                    # Fallback: use any semester subjects
                    student_semester_subjects = random.sample(
                        self.state.semester_subjects,
                        min(5, len(self.state.semester_subjects))
                    )

                max_subjects = min(8, len(student_semester_subjects))
                if max_subjects == 0:
                    continue

                # Prefer 4-8 subjects, but gracefully handle sparse curriculum assignments.
                min_subjects = 4 if max_subjects >= 4 else 1
                num_subjects = random.randint(min_subjects, max_subjects)
                selected_semester_subjects = random.sample(student_semester_subjects, num_subjects)

                for semester_subject in selected_semester_subjects:
                    status = random.choices(statuses, weights=status_weights)[0]

                    if self.db_manager.db_type == "derby":
                        # Check for duplicates since Derby doesn't support INSERT IGNORE
                        cursor.execute(
                            """
                                SELECT COUNT(*) FROM APP.student_enrolled_subjects
                                WHERE student_id = ? AND semester_subject_id = ?
                            """,
                            (student.student_id, semester_subject.id),
                        )
                        if cursor.fetchone()[0] > 0:
                            continue

                        query = """
                            INSERT INTO APP.student_enrolled_subjects (student_id, semester_subject_id, status)
                            VALUES (?, ?, ?)
                        """
                        cursor.execute(query, (student.student_id, semester_subject.id, status))
                    else:
                        query = """
                            INSERT IGNORE INTO student_enrolled_subjects (student_id, semester_subject_id, status)
                            VALUES (%s, %s, %s)
                        """
                        cursor.execute(query, (student.student_id, semester_subject.id, status))

                    self.state.student_enrolled_subjects.append(
                        StudentEnrolledSubject(
                            student_id=student.student_id,
                            semester_subject_id=semester_subject.id,
                            status=status,
                        )
                    )

            self.db_manager.commit()
        finally:
            cursor.close()

        print(f"Created {len(self.state.student_enrolled_subjects)} student subject records")
