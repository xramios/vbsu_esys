#!/usr/bin/env python3
"""
Enrollment seeder module.

Generates enrollment periods, schedules, and enrollment records.
"""

import random
from datetime import datetime, timedelta
from typing import TYPE_CHECKING
from tqdm import tqdm

from seeder.services.base_seeder import BaseSeeder
from seeder.config.constants import (
    SEEDING_COUNTS,
    DAYS_OF_WEEK,
    START_HOURS,
    START_MINUTES,
    DURATION_HOURS,
    ENROLLMENT_STATUSES,
    ENROLLMENT_DETAIL_STATUSES,
    ENROLLMENTS_PER_STUDENT,
)
from seeder.utils.faker_instance import fake
from seeder.models.data_models import EnrollmentPeriod, Room, Schedule, Section

if TYPE_CHECKING:
    from seeder.core.database import DatabaseManager
    from seeder.models.data_models import SeedingState


class EnrollmentSeeder(BaseSeeder):
    """Seeder for enrollment periods, schedules, and enrollments."""

    ENROLLMENT_PERIOD_CREATE_SQL = """
        CREATE TABLE APP.enrollment_period (
            id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
            school_year VARCHAR(50),
            semester INTEGER,
            start_date DATE,
            end_date DATE,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )
    """

    SCHEDULES_CREATE_SQL = """
        CREATE TABLE APP.schedules (
            id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
            section_id INTEGER,
            room_id INTEGER,
            faculty_id INTEGER,
            day VARCHAR(10),
            start_time TIME,
            end_time TIME,
            enrollment_period_id INTEGER,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (section_id) REFERENCES APP.sections(id),
            FOREIGN KEY (room_id) REFERENCES APP.rooms(id),
            FOREIGN KEY (faculty_id) REFERENCES APP.faculty(id),
            FOREIGN KEY (enrollment_period_id) REFERENCES APP.enrollment_period(id)
        )
    """

    ENROLLMENTS_CREATE_SQL = """
        CREATE TABLE APP.enrollments (
            id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
            student_id VARCHAR(50),
            enrollment_period_id INTEGER,
            status VARCHAR(50),
            max_units DECIMAL(5,2),
            total_units DECIMAL(5,2),
            submitted_at TIMESTAMP,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (student_id) REFERENCES APP.students(student_id),
            FOREIGN KEY (enrollment_period_id) REFERENCES APP.enrollment_period(id)
        )
    """

    ENROLLMENT_DETAILS_CREATE_SQL = """
        CREATE TABLE APP.enrollments_details (
            id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
            enrollment_id INTEGER,
            section_id INTEGER,
            subject_id INTEGER,
            units DECIMAL(3,1),
            status VARCHAR(50),
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (enrollment_id) REFERENCES APP.enrollments(id),
            FOREIGN KEY (section_id) REFERENCES APP.sections(id),
            FOREIGN KEY (subject_id) REFERENCES APP.subjects(id)
        )
    """

    STUDENT_ENROLLED_SUBJECTS_CREATE_SQL = """
        CREATE TABLE APP.student_enrolled_subjects (
            student_id VARCHAR(50),
            semester_subject_id INTEGER,
            status VARCHAR(20) DEFAULT 'ENROLLED',
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            PRIMARY KEY (student_id, semester_subject_id),
            FOREIGN KEY (student_id) REFERENCES APP.students(student_id)
        )
    """

    def __init__(self, db_manager: "DatabaseManager", state: "SeedingState") -> None:
        """Initialize enrollment seeder.

        Args:
            db_manager: Database manager instance
            state: Shared seeding state
        """
        super().__init__(db_manager, state)

    def seed_enrollment_periods(self, count: int = None) -> None:
        """Seed enrollment_period table with academic periods.

        Args:
            count: Number of enrollment periods to create
        """
        count = count or SEEDING_COUNTS["enrollment_periods"]
        print(f"Seeding {count} enrollment periods...")

        if self.db_manager.db_type == "derby":
            self.create_table_if_not_exists("enrollment_period", self.ENROLLMENT_PERIOD_CREATE_SQL)

        cursor = self.db_manager.connection.cursor()
        try:
            for i in range(count):
                year = 2021 + i
                for semester in [1, 2]:
                    if semester == 1:
                        start_date = datetime(year - 1, 10, 1)
                        end_date = datetime(year - 1, 11, 30)
                    else:
                        start_date = datetime(year, 3, 1)
                        end_date = datetime(year, 4, 30)

                    start_date_str = self.format_datetime(start_date)
                    end_date_str = self.format_datetime(end_date)

                    if self.db_manager.db_type == "derby":
                        query = """
                            INSERT INTO APP.enrollment_period
                            (school_year, semester, start_date, end_date)
                            VALUES (?, ?, ?, ?)
                        """
                        cursor.execute(
                            query,
                            (f"{year-1}-{year}", semester, start_date_str, end_date_str),
                        )
                    else:
                        query = """
                            INSERT INTO enrollment_period
                            (school_year, semester, start_date, end_date)
                            VALUES (%s, %s, %s, %s)
                        """
                        cursor.execute(
                            query, (f"{year-1}-{year}", semester, start_date, end_date)
                        )

                    last_id = self.adapter.get_last_insert_id(cursor, "enrollment_period")

                    self.state.enrollment_periods.append(
                        EnrollmentPeriod(
                            id=last_id,
                            school_year=f"{year-1}-{year}",
                            semester=semester,
                            start_date=start_date,
                            end_date=end_date,
                        )
                    )

            self.db_manager.commit()
        finally:
            cursor.close()

        print(f"Created {len(self.state.enrollment_periods)} enrollment periods")

    def seed_schedules(self) -> None:
        """Seed schedules table with class schedules."""
        print("Seeding schedules...")

        if self.db_manager.db_type == "derby":
            self.create_table_if_not_exists("schedules", self.SCHEDULES_CREATE_SQL)

        cursor = self.db_manager.connection.cursor()
        try:
            for section in self.state.sections:
                num_schedules = random.randint(1, 2)
                room = self._select_room_for_section(section)

                for _ in range(num_schedules):
                    day = random.choice(DAYS_OF_WEEK)

                    start_hour = random.choice(START_HOURS)
                    start_minute = random.choice(START_MINUTES)
                    end_hour = start_hour + random.choice(DURATION_HOURS)
                    end_minute = start_minute

                    start_time = f"{start_hour:02d}:{start_minute:02d}:00"
                    end_time = f"{end_hour:02d}:{end_minute:02d}:00"
                    faculty = random.choice(self.state.faculty)

                    enrollment_period_id = random.choice(self.state.enrollment_periods).id

                    if self.db_manager.db_type == "derby":
                        query = """
                            INSERT INTO APP.schedules
                            (section_id, room_id, faculty_id, day, start_time, end_time, enrollment_period_id)
                            VALUES (?, ?, ?, ?, ?, ?, ?)
                        """
                        cursor.execute(
                            query,
                            (section.id, room.id, faculty.id, day, start_time, end_time, enrollment_period_id),
                        )
                    else:
                        query = """
                            INSERT INTO schedules
                            (section_id, room_id, faculty_id, day, start_time, end_time, enrollment_period_id)
                            VALUES (?, ?, ?, ?, ?, ?, ?)
                        """
                        cursor.execute(
                            query,
                            (section.id, room.id, faculty.id, day, start_time, end_time, enrollment_period_id),
                        )

            self.db_manager.commit()
        finally:
            cursor.close()

        print("Created schedules")

    def _select_room_for_section(self, section: Section) -> Room:
        """Select a room that can fit the section capacity.

        Args:
            section: Section being scheduled

        Returns:
            A room with enough capacity, or the largest room available as a fallback.
        """
        if not self.state.rooms:
            raise ValueError("Cannot seed schedules without any rooms")

        eligible_rooms = [room for room in self.state.rooms if room.capacity >= section.capacity]
        if eligible_rooms:
            return random.choice(eligible_rooms)

        return max(self.state.rooms, key=lambda room: room.capacity)

    def seed_enrollments(self) -> None:
        """Seed enrollments and enrollment details tables."""
        print("Seeding enrollments...")

        if self.db_manager.db_type == "derby":
            self.create_table_if_not_exists("enrollments", self.ENROLLMENTS_CREATE_SQL)
            self.create_table_if_not_exists(
                "enrollments_details", self.ENROLLMENT_DETAILS_CREATE_SQL
            )
            self.create_table_if_not_exists(
                "student_enrolled_subjects", self.STUDENT_ENROLLED_SUBJECTS_CREATE_SQL
            )

        cursor = self.db_manager.connection.cursor()
        try:
            statuses = ENROLLMENT_STATUSES
            section_occupancy = {section.id: 0 for section in self.state.sections}

            for student in self.state.students:
                num_enrollments = random.randint(*ENROLLMENTS_PER_STUDENT)

                for _ in range(num_enrollments):
                    enrollment_period = random.choice(self.state.enrollment_periods)
                    semester = enrollment_period.semester
                    status = random.choice(statuses)

                    max_units = random.uniform(15, 24)
                    total_units = random.uniform(12, max_units)

                    submitted_at = fake.date_time_between(start_date="-2y", end_date="now")
                    submitted_at_str = self.format_timestamp(submitted_at)

                    if self.db_manager.db_type == "derby":
                        query = """
                            INSERT INTO APP.enrollments
                            (student_id, enrollment_period_id, status, max_units, total_units, submitted_at)
                            VALUES (?, ?, ?, ?, ?, ?)
                        """
                        cursor.execute(
                            query,
                            (
                                student.student_id,
                                enrollment_period.id,
                                status,
                                max_units,
                                total_units,
                                submitted_at_str,
                            ),
                        )
                    else:
                        query = """
                            INSERT INTO enrollments
                            (student_id, enrollment_period_id, status, max_units, total_units, submitted_at)
                            VALUES (%s, %s, %s, %s, %s, %s)
                        """
                        cursor.execute(
                            query,
                            (
                                student.student_id,
                                enrollment_period.id,
                                status,
                                max_units,
                                total_units,
                                submitted_at,
                            ),
                        )

                    if self.db_manager.db_type == "mysql":
                        enrollment_id = cursor.lastrowid
                    else:
                        enrollment_id = self.adapter.get_last_insert_id(cursor, "enrollments")

                    if status in ["APPROVED", "ENROLLED"]:
                        self._create_enrollment_details(
                            cursor,
                            enrollment_id,
                            student.student_id,
                            semester,
                            section_occupancy,
                        )

            self.db_manager.commit()
        finally:
            cursor.close()

        print("Created enrollments and enrollment details")

    def seed(self) -> None:
        """Execute all enrollment-related seeding operations.

        Orchestrates enrollment periods, schedules, and enrollments seeding.
        """
        self.seed_enrollment_periods()
        self.seed_schedules()
        self.seed_enrollments()

    def _create_enrollment_details(
        self,
        cursor: any,
        enrollment_id: int,
        student_id: str,
        semester: int = 1,
        section_occupancy: dict[int, int] | None = None,
    ) -> None:
        """Create enrollment details for an enrollment.

        Args:
            cursor: Database cursor
            enrollment_id: Enrollment ID
            student_id: Student ID
            semester: Semester number (1 or 2) for finding correct semester_subject
            section_occupancy: Running count of selected seats per section
        """
        if section_occupancy is None:
            section_occupancy = {}

        num_subjects = random.randint(3, 7)
        used_section_ids: set[int] = set()

        # Get student's course_id for finding correct semester_subjects
        student = next((s for s in self.state.students if s.student_id == student_id), None)
        student_course_id = student.course_id if student else None
        student_year_level = student.year_level if student else 1

        for _ in range(min(num_subjects, len(self.state.sections))):
            detail_status = random.choice(ENROLLMENT_DETAIL_STATUSES)
            section = self._select_section_for_detail(
                used_section_ids,
                section_occupancy,
                require_capacity=detail_status == "SELECTED",
            )

            if section is None and detail_status == "SELECTED":
                detail_status = "DROPPED"
                section = self._select_section_for_detail(
                    used_section_ids,
                    section_occupancy,
                    require_capacity=False,
                )

            if section is None:
                break

            units = next(
                (s.units for s in self.state.subjects if s.id == section.subject_id), 3
            )

            if self.db_manager.db_type == "derby":
                query = """
                    INSERT INTO APP.enrollments_details
                    (enrollment_id, section_id, subject_id, units, status)
                    VALUES (?, ?, ?, ?, ?)
                """
                cursor.execute(
                    query, (enrollment_id, section.id, section.subject_id, units, detail_status)
                )
            else:
                query = """
                    INSERT INTO enrollments_details
                    (enrollment_id, section_id, subject_id, units, status)
                    VALUES (%s, %s, %s, %s, %s)
                """
                cursor.execute(
                    query, (enrollment_id, section.id, section.subject_id, units, detail_status)
                )

            used_section_ids.add(section.id)

            if detail_status == "SELECTED":
                section_occupancy[section.id] = section_occupancy.get(section.id, 0) + 1

            # Find the correct semester_subject_id for this subject and semester
            semester_subject_id = self._find_semester_subject_id(
                section.subject_id, semester, student_course_id, student_year_level
            )
            if semester_subject_id:
                self._insert_student_enrolled_subject(cursor, student_id, semester_subject_id)

    def _select_section_for_detail(
        self,
        used_section_ids: set[int],
        section_occupancy: dict[int, int],
        require_capacity: bool,
    ) -> Section | None:
        """Pick an unused section for an enrollment detail.

        Args:
            used_section_ids: Section IDs already chosen for the current enrollment
            section_occupancy: Running selected-seat counts per section
            require_capacity: Whether the chosen section must still have selected seats left

        Returns:
            A section that satisfies the current constraints, or None if no section remains.
        """
        candidate_sections = [
            section for section in self.state.sections if section.id not in used_section_ids
        ]

        if require_capacity:
            candidate_sections = [
                section
                for section in candidate_sections
                if section_occupancy.get(section.id, 0) < section.capacity
            ]

        if not candidate_sections:
            return None

        return random.choice(candidate_sections)

    def _find_semester_subject_id(
        self, subject_id: int, semester_num: int, course_id: int = None, year_level: int = None
    ) -> int | None:
        """Find the semester_subject_id for a given subject and semester context.

        Args:
            subject_id: The subject ID
            semester_num: Semester number (1 or 2)
            course_id: Optional course ID to narrow down by curriculum
            year_level: Optional student year level for filtering

        Returns:
            The semester_subject ID if found, None otherwise
        """
        if not self.state.semester_subjects:
            return None

        # Build a list of semester IDs matching the semester number
        semester_name = f"Semester {semester_num}"
        matching_semester_ids = {
            s.id for s in self.state.semesters if s.semester == semester_name
        }

        # If course_id provided, narrow down to semesters from that course's curriculums
        if course_id:
            course_curriculum_ids = {c.id for c in self.state.curriculums if c.course_id == course_id}
            matching_semester_ids = {
                s.id for s in self.state.semesters
                if s.semester == semester_name and s.curriculum_id in course_curriculum_ids
            }

        # Find semester_subjects matching the criteria
        candidates = [
            ss for ss in self.state.semester_subjects
            if ss.subject_id == subject_id and ss.semester_id in matching_semester_ids
        ]

        # Filter by year level if provided (allow subjects at or below student's year)
        if year_level:
            candidates = [ss for ss in candidates if ss.year_level <= year_level]

        if candidates:
            return random.choice(candidates).id
        return None

    def _insert_student_enrolled_subject(
        self, cursor: any, student_id: str, semester_subject_id: int
    ) -> None:
        """Insert student enrolled subject with duplicate handling.

        Args:
            cursor: Database cursor
            student_id: Student ID
            semester_subject_id: Semester Subject ID (from semester_subjects table)
        """
        if self.db_manager.db_type == "derby":
            # Derby doesn't support INSERT IGNORE, check existence first
            cursor.execute(
                """
                    SELECT COUNT(*) FROM APP.student_enrolled_subjects
                    WHERE student_id = ? AND semester_subject_id = ?
                """,
                (student_id, semester_subject_id),
            )
            if cursor.fetchone()[0] == 0:
                query = """
                    INSERT INTO APP.student_enrolled_subjects (student_id, semester_subject_id, status)
                    VALUES (?, ?, 'ENROLLED')
                """
                cursor.execute(query, (student_id, semester_subject_id))
        else:
            query = """
                INSERT IGNORE INTO student_enrolled_subjects (student_id, semester_subject_id, status)
                VALUES (%s, %s, 'ENROLLED')
            """
            cursor.execute(query, (student_id, semester_subject_id))
