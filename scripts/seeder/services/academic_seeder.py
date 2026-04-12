#!/usr/bin/env python3
"""
Academic seeder module.

Generates curriculum, subjects, sections, and prerequisites.
"""

import random
import re
from datetime import datetime
from typing import TYPE_CHECKING
from tqdm import tqdm

from seeder.services.base_seeder import BaseSeeder
from seeder.config.constants import (
    SEEDING_COUNTS,
    SUBJECT_TEMPLATES,
    SECTIONS_PER_SUBJECT,
    SECTION_CAPACITY_RANGE,
    SECTION_STATUSES,
    PREREQUISITE_PROBABILITY,
    PREREQUISITES_PER_SUBJECT,
    SUBJECTS_WITH_PREREQUISITES,
)
from seeder.models.data_models import Curriculum, Subject, Section

if TYPE_CHECKING:
    from seeder.core.database import DatabaseManager
    from seeder.models.data_models import SeedingState


class AcademicSeeder(BaseSeeder):
    """Seeder for curriculum, subjects, sections, and prerequisites."""

    CURRICULUM_CREATE_SQL = """
        CREATE TABLE APP.curriculum (
            id BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
            name VARCHAR(64) UNIQUE,
            cur_year DATE,
            course BIGINT,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (course) REFERENCES APP.courses(id)
        )
    """

    SUBJECTS_CREATE_SQL = """
        CREATE TABLE APP.subjects (
            id BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
            subject_name VARCHAR(32),
            subject_code VARCHAR(32),
            units FLOAT,
            estimated_time INT DEFAULT 90,
            schedule_pattern VARCHAR(32) DEFAULT 'LECTURE_ONLY' CHECK (schedule_pattern IN ('LECTURE_ONLY', 'LECTURE_LAB', 'GE_PAIRED', 'PE_PAIRED', 'NSTP_BLOCK')),
            description CLOB,
            department_id BIGINT,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (department_id) REFERENCES APP.departments(id)
        )
    """

    SECTIONS_CREATE_SQL = """
        CREATE TABLE APP.sections (
            id BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
            section_name VARCHAR(48),
            section_code VARCHAR(48),
            capacity INT NOT NULL,
            status VARCHAR(20) CHECK (status IN ('OPEN', 'CLOSED', 'WAITLIST', 'DISSOLVED')) DEFAULT 'OPEN',
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )
    """

    PREREQUISITES_CREATE_SQL = """
        CREATE TABLE APP.prerequisites (
            id BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
            pre_subject_id BIGINT,
            subject_id BIGINT,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (pre_subject_id) REFERENCES APP.subjects(id),
            FOREIGN KEY (subject_id) REFERENCES APP.subjects(id)
        )
    """

    def __init__(self, db_manager: "DatabaseManager", state: "SeedingState") -> None:
        """Initialize academic seeder.

        Args:
            db_manager: Database manager instance
            state: Shared seeding state
        """
        super().__init__(db_manager, state)

    def seed(self) -> None:
        """Seed academic data."""
        self.seed_curriculum()
        self.seed_subjects()
        self.seed_sections()
        self.seed_prerequisites()

    @staticmethod
    def _abbreviate(text: str, stop_words: set[str], single_word_length: int = 2) -> str:
        """Build an uppercase abbreviation from significant words."""
        words = [re.sub(r"[^A-Za-z]", "", token) for token in text.split()]
        words = [word for word in words if word]

        filtered_words = [word for word in words if word.lower() not in stop_words]
        source_words = filtered_words or words

        if not source_words:
            return "XX"

        if len(source_words) == 1:
            return source_words[0][:single_word_length].upper()

        return "".join(word[0].upper() for word in source_words)

    def _build_course_code(self, course_name: str) -> str:
        """Build a short code from the course name (e.g., Information Technology -> IT)."""
        normalized = course_name.lower()
        program_name = course_name

        if " in " in normalized:
            split_index = normalized.rfind(" in ") + len(" in ")
            program_name = course_name[split_index:]

        return self._abbreviate(
            program_name,
            stop_words={"bachelor", "master", "of", "science", "arts", "the", "and", "in"},
        )

    def _build_department_code(self, department_name: str) -> str:
        """Build a short code from the department name (e.g., Engineering -> EN)."""
        normalized = department_name.lower()
        unit_name = department_name

        if " of " in normalized:
            split_index = normalized.rfind(" of ") + len(" of ")
            unit_name = department_name[split_index:]

        return self._abbreviate(
            unit_name,
            stop_words={"college", "school", "department", "of", "the", "and"},
        )

    def _build_curriculum_name(self, course_name: str, department_name: str, year: int) -> str:
        """Build curriculum name in COURSEDEPTYEAR format (e.g., ITEN2023)."""
        course_code = self._build_course_code(course_name)
        department_code = self._build_department_code(department_name)
        return f"{course_code}{department_code}{year}"

    def _resolve_schedule_pattern(self, subject_code: str, subject_name: str) -> str:
        """Resolve a deterministic schedule pattern from subject code/name."""
        descriptor = f"{subject_code} {subject_name}".upper()

        if descriptor.startswith("STC") or "NSTP" in descriptor or "CIVIC" in descriptor:
            return "NSTP_BLOCK"

        if descriptor.startswith("PPF") or "PATHFIT" in descriptor or "PHYSICAL" in descriptor:
            return "PE_PAIRED"

        if descriptor.startswith("ZGE"):
            return "GE_PAIRED"

        if descriptor.startswith("CCP") or descriptor.startswith("CDS") or descriptor.startswith("CFD"):
            return "LECTURE_LAB"

        if descriptor.startswith("PROG") or descriptor.startswith("DS") or descriptor.startswith("DB"):
            return "LECTURE_LAB"

        return "LECTURE_ONLY"

    def seed_curriculum(self) -> None:
        """Seed curriculum table with course/year curriculum entries."""
        print("Seeding curriculum...")

        if self.db_manager.db_type == "derby":
            self.create_table_if_not_exists("curriculum", self.CURRICULUM_CREATE_SQL)

        current_year = datetime.now().year
        curriculum_years = range(current_year - 3, current_year + 1)
        departments_by_id = {department.id: department for department in self.state.departments}

        cursor = self.db_manager.connection.cursor()
        try:
            for course in self.state.courses:
                department = departments_by_id.get(course.department_id)
                department_name = department.name if department else ""

                for year in curriculum_years:
                    curriculum_year = datetime(year, 1, 1)
                    curriculum_name = self._build_curriculum_name(
                        course.name,
                        department_name,
                        year,
                    )

                    if self.db_manager.db_type == "derby":
                        query = """
                            INSERT INTO APP.curriculum (name, cur_year, course)
                            VALUES (?, ?, ?)
                        """
                        cursor.execute(
                            query,
                            (curriculum_name, self.format_datetime(curriculum_year), course.id),
                        )
                    else:
                        query = """
                            INSERT INTO curriculum (name, cur_year, course)
                            VALUES (%s, %s, %s)
                        """
                        cursor.execute(query, (curriculum_name, curriculum_year, course.id))

                    last_id = self.adapter.get_last_insert_id(cursor, "curriculum")

                    self.state.curriculums.append(
                        Curriculum(
                            id=last_id,
                            name=curriculum_name,
                            course=course.id,
                            cur_year=curriculum_year,
                        )
                    )

            self.db_manager.commit()
        finally:
            cursor.close()

        print(f"Created {len(self.state.curriculums)} curriculum entries")

    def seed_subjects(self, count: int = None) -> None:
        """Seed subjects table with academic courses.

        Args:
            count: Number of subjects to create (default from SEEDING_COUNTS)
        """
        count = count or SEEDING_COUNTS["subjects"]
        print(f"Seeding {count} subjects...")

        if self.db_manager.db_type == "derby":
            self.create_table_if_not_exists("subjects", self.SUBJECTS_CREATE_SQL)

        cursor = self.db_manager.connection.cursor()
        try:
            for i in tqdm(range(count), desc="Creating subjects", unit="subject"):
                template = random.choice(SUBJECT_TEMPLATES)
                subject_name = f"{template[0]} {random.randint(1, 4)}"
                subject_code = f"{template[1]}{random.randint(100, 999)}"
                units = template[2]
                estimated_time = 90
                schedule_pattern = self._resolve_schedule_pattern(subject_code, subject_name)
                description = template[3]

                department = random.choice(self.state.departments)

                if self.db_manager.db_type == "derby":
                    query = """
                        INSERT INTO APP.subjects
                        (subject_name, subject_code, units, estimated_time, schedule_pattern, description, department_id)
                        VALUES (?, ?, ?, ?, ?, ?, ?)
                    """
                    cursor.execute(
                        query,
                        (subject_name, subject_code, units, estimated_time, schedule_pattern, description, department.id),
                    )
                else:
                    query = """
                        INSERT INTO subjects
                        (subject_name, subject_code, units, estimated_time, schedule_pattern, description, department_id)
                        VALUES (%s, %s, %s, %s, %s, %s, %s)
                    """
                    cursor.execute(
                        query,
                        (subject_name, subject_code, units, estimated_time, schedule_pattern, description, department.id),
                    )

                last_id = self.adapter.get_last_insert_id(cursor, "subjects")

                self.state.subjects.append(
                    Subject(
                        id=last_id,
                        subject_name=subject_name,
                        subject_code=subject_code,
                        units=units,
                        department_id=department.id,
                        estimated_time=estimated_time,
                        schedule_pattern=schedule_pattern,
                        description=description,
                    )
                )

            self.db_manager.commit()
        finally:
            cursor.close()

        print(f"Created {len(self.state.subjects)} subjects")

    def seed_sections(self) -> None:
        """Seed sections table with reusable class sections."""
        print("Seeding sections...")

        if self.db_manager.db_type == "derby":
            self.create_table_if_not_exists("sections", self.SECTIONS_CREATE_SQL)

        cursor = self.db_manager.connection.cursor()
        try:
            section_count = random.randint(*SECTIONS_PER_SUBJECT) * max(len(self.state.courses), 1)
            for section_index in tqdm(range(section_count), desc="Creating sections", unit="section"):
                section_name = f"Block {section_index + 1}"
                section_code = f"SEC-{section_index + 1:03d}"
                capacity = random.randint(*SECTION_CAPACITY_RANGE)
                status = random.choices(SECTION_STATUSES, weights=[0.82, 0.08, 0.07, 0.03], k=1)[0]

                if self.db_manager.db_type == "derby":
                    query = """
                        INSERT INTO APP.sections (section_name, section_code, capacity, status)
                        VALUES (?, ?, ?, ?)
                    """
                    cursor.execute(query, (section_name, section_code, capacity, status))
                else:
                    query = """
                        INSERT INTO sections (section_name, section_code, capacity, status)
                        VALUES (%s, %s, %s, %s)
                    """
                    cursor.execute(query, (section_name, section_code, capacity, status))

                last_id = self.adapter.get_last_insert_id(cursor, "sections")

                self.state.sections.append(
                    Section(
                        id=last_id,
                        section_name=section_name,
                        section_code=section_code,
                        capacity=capacity,
                        status=status,
                    )
                )

            self.db_manager.commit()
        finally:
            cursor.close()

        print(f"Created {len(self.state.sections)} sections")

    def seed_prerequisites(self) -> None:
        """Seed prerequisites table with course prerequisite relationships."""
        print("Seeding prerequisites...")

        if self.db_manager.db_type == "derby":
            self.create_table_if_not_exists("prerequisites", self.PREREQUISITES_CREATE_SQL)

        cursor = self.db_manager.connection.cursor()
        try:
            for i, subject in enumerate(self.state.subjects[:SUBJECTS_WITH_PREREQUISITES]):
                if random.random() > PREREQUISITE_PROBABILITY:
                    continue

                num_prereqs = random.randint(*PREREQUISITES_PER_SUBJECT)
                available_prereqs = [s for s in self.state.subjects if s.id != subject.id]

                for _ in range(num_prereqs):
                    if not available_prereqs:
                        break

                    prereq = random.choice(available_prereqs)
                    available_prereqs.remove(prereq)

                    if self.db_manager.db_type == "derby":
                        query = """
                            INSERT INTO APP.prerequisites (pre_subject_id, subject_id)
                            VALUES (?, ?)
                        """
                        cursor.execute(query, (prereq.id, subject.id))
                    else:
                        query = """
                            INSERT INTO prerequisites (pre_subject_id, subject_id)
                            VALUES (%s, %s)
                        """
                        cursor.execute(query, (prereq.id, subject.id))

            self.db_manager.commit()
        finally:
            cursor.close()

        print("Created prerequisites")
