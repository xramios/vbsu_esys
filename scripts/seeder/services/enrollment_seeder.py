#!/usr/bin/env python3
"""
Enrollment seeder module.

Generates enrollment periods, offerings, schedules, and enrollment records.
"""

import random
from datetime import datetime, timedelta
from typing import Any, TYPE_CHECKING

from tqdm import tqdm

from seeder.config.constants import (
    DAYS_OF_WEEK,
    DURATION_HOURS,
    ENROLLMENT_DETAIL_STATUSES,
    ENROLLMENT_STATUSES,
    ENROLLMENTS_PER_STUDENT,
    SCHEDULES_PER_SECTION,
    SEEDING_COUNTS,
    START_HOURS,
    START_MINUTES,
)
from seeder.models.data_models import (
    EnrollmentPeriod,
    Offering,
    Room,
    Schedule,
    Section,
    StudentEnrolledSubject,
    StudentSemesterProgress,
)
from seeder.services.base_seeder import BaseSeeder
from seeder.utils.faker_instance import fake

if TYPE_CHECKING:
    from seeder.core.database import DatabaseManager
    from seeder.models.data_models import SeedingState


class EnrollmentSeeder(BaseSeeder):
    """Seeder for enrollment periods, offerings, schedules, and enrollments."""

    ENROLLMENT_PERIOD_CREATE_SQL = """
        CREATE TABLE APP.enrollment_period (
            id BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
            school_year VARCHAR(24) NOT NULL,
            semester VARCHAR(24) NOT NULL,
            start_date TIMESTAMP NOT NULL,
            end_date TIMESTAMP NOT NULL,
            description CLOB,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )
    """

    OFFERINGS_CREATE_SQL = """
        CREATE TABLE APP.offerings (
            id BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
            subject_id BIGINT NOT NULL,
            section_id BIGINT NOT NULL,
            enrollment_period_id BIGINT NOT NULL,
            semester_subject_id BIGINT,
            capacity INT,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            UNIQUE (subject_id, section_id, enrollment_period_id),
            FOREIGN KEY (subject_id) REFERENCES APP.subjects(id),
            FOREIGN KEY (section_id) REFERENCES APP.sections(id),
            FOREIGN KEY (enrollment_period_id) REFERENCES APP.enrollment_period(id),
            FOREIGN KEY (semester_subject_id, subject_id) REFERENCES APP.semester_subjects(id, subject_id)
        )
    """

    SCHEDULES_CREATE_SQL = """
        CREATE TABLE APP.schedules (
            id BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
            offering_id BIGINT NOT NULL,
            room_id BIGINT,
            faculty_id BIGINT,
            day VARCHAR(3) CHECK (day IN ('MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT', 'SUN')),
            start_time TIME,
            end_time TIME,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            UNIQUE (offering_id, day, start_time),
            FOREIGN KEY (offering_id) REFERENCES APP.offerings(id),
            FOREIGN KEY (room_id) REFERENCES APP.rooms(id),
            FOREIGN KEY (faculty_id) REFERENCES APP.faculty(id)
        )
    """

    ENROLLMENTS_CREATE_SQL = """
        CREATE TABLE APP.enrollments (
            id BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
            student_id VARCHAR(32) NOT NULL,
            enrollment_period_id BIGINT NOT NULL,
            status VARCHAR(20) NOT NULL CHECK (status IN ('DRAFT', 'SUBMITTED', 'APPROVED', 'ENROLLED', 'CANCELLED')),
            max_units FLOAT,
            total_units FLOAT,
            submitted_at TIMESTAMP,
            approved_by BIGINT,
            approved_at TIMESTAMP,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            UNIQUE (student_id, enrollment_period_id),
            FOREIGN KEY (student_id) REFERENCES APP.students(student_id),
            FOREIGN KEY (enrollment_period_id) REFERENCES APP.enrollment_period(id),
            FOREIGN KEY (approved_by) REFERENCES APP.users(id)
        )
    """

    ENROLLMENT_DETAILS_CREATE_SQL = """
        CREATE TABLE APP.enrollments_details (
            id BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
            enrollment_id BIGINT NOT NULL,
            offering_id BIGINT NOT NULL,
            units FLOAT,
            status VARCHAR(20) CHECK (status IN ('SELECTED', 'DROPPED')),
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            UNIQUE (enrollment_id, offering_id),
            FOREIGN KEY (enrollment_id) REFERENCES APP.enrollments(id),
            FOREIGN KEY (offering_id) REFERENCES APP.offerings(id)
        )
    """

    STUDENT_ENROLLED_SUBJECTS_CREATE_SQL = """
        CREATE TABLE APP.student_enrolled_subjects (
            student_id VARCHAR(32) NOT NULL,
            enrollment_id BIGINT NOT NULL,
            offering_id BIGINT NOT NULL,
            semester_subject_id BIGINT NOT NULL,
            status VARCHAR(20) CHECK (status IN ('ENROLLED', 'COMPLETED', 'DROPPED')) NOT NULL DEFAULT 'ENROLLED',
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            UNIQUE (student_id, offering_id),
            FOREIGN KEY (student_id) REFERENCES APP.students(student_id),
            FOREIGN KEY (enrollment_id) REFERENCES APP.enrollments(id),
            FOREIGN KEY (offering_id) REFERENCES APP.offerings(id),
            FOREIGN KEY (enrollment_id, offering_id) REFERENCES APP.enrollments_details(enrollment_id, offering_id),
            FOREIGN KEY (semester_subject_id) REFERENCES APP.semester_subjects(id)
        )
    """

    STUDENT_SEMESTER_PROGRESS_CREATE_SQL = """
        CREATE TABLE APP.student_semester_progress (
            id BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
            student_id VARCHAR(32) NOT NULL,
            curriculum_id BIGINT NOT NULL,
            semester_id BIGINT NOT NULL,
            status VARCHAR(20) CHECK (status IN ('NOT_STARTED', 'IN_PROGRESS', 'COMPLETED')) NOT NULL DEFAULT 'NOT_STARTED',
            started_at TIMESTAMP,
            completed_at TIMESTAMP,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (student_id) REFERENCES APP.students(student_id),
            FOREIGN KEY (curriculum_id) REFERENCES APP.curriculum(id),
            FOREIGN KEY (semester_id) REFERENCES APP.semester(id)
        )
    """

    def __init__(self, db_manager: "DatabaseManager", state: "SeedingState") -> None:
        super().__init__(db_manager, state)

    def seed_enrollment_periods(self, count: int = None) -> None:
        """Seed enrollment_period table with academic periods."""
        count = count or SEEDING_COUNTS["enrollment_periods"]
        print(f"Seeding {count} enrollment periods...")

        if self.db_manager.db_type == "derby":
            self.create_table_if_not_exists("enrollment_period", self.ENROLLMENT_PERIOD_CREATE_SQL)

        cursor = self.db_manager.connection.cursor()
        try:
            for i in range(count):
                year = 2021 + i
                for semester_label, semester_num in [("First", 1), ("Second", 2)]:
                    if semester_num == 1:
                        start_date = datetime(year - 1, 10, 1)
                        end_date = datetime(year - 1, 11, 30)
                    else:
                        start_date = datetime(year, 3, 1)
                        end_date = datetime(year, 4, 30)

                    description = (
                        f"{semester_label} semester enrollment period for school year {year-1}-{year}"
                    )

                    start_date_str = self.format_timestamp(start_date)
                    end_date_str = self.format_timestamp(end_date)

                    if self.db_manager.db_type == "derby":
                        query = """
                            INSERT INTO APP.enrollment_period
                            (school_year, semester, start_date, end_date, description)
                            VALUES (?, ?, ?, ?, ?)
                        """
                        cursor.execute(
                            query,
                            (
                                f"{year-1}-{year}",
                                semester_label,
                                start_date_str,
                                end_date_str,
                                description,
                            ),
                        )
                    else:
                        query = """
                            INSERT INTO enrollment_period
                            (school_year, semester, start_date, end_date, description)
                            VALUES (%s, %s, %s, %s, %s)
                        """
                        cursor.execute(
                            query,
                            (f"{year-1}-{year}", semester_label, start_date, end_date, description),
                        )

                    last_id = self.adapter.get_last_insert_id(cursor, "enrollment_period")
                    self.state.enrollment_periods.append(
                        EnrollmentPeriod(
                            id=last_id,
                            school_year=f"{year-1}-{year}",
                            semester=semester_label,
                            start_date=start_date,
                            end_date=end_date,
                            description=description,
                        )
                    )

            self.db_manager.commit()
        finally:
            cursor.close()

        print(f"Created {len(self.state.enrollment_periods)} enrollment periods")

    def seed_offerings(self) -> None:
        """Seed offerings as the canonical enrollable class instances."""
        print("Seeding offerings...")

        if self.db_manager.db_type == "derby":
            self.create_table_if_not_exists("offerings", self.OFFERINGS_CREATE_SQL)

        if not self.state.enrollment_periods or not self.state.semester_subjects:
            print("Missing enrollment periods or semester subjects. Skipping offerings seeding.")
            return

        section_pool = [
            section
            for section in self.state.sections
            if section.status in {"OPEN", "WAITLIST"}
        ]
        if not section_pool:
            section_pool = list(self.state.sections)
        if not section_pool:
            print("No sections available. Skipping offerings seeding.")
            return

        semester_by_id = {semester.id: semester for semester in self.state.semesters}
        semester_subjects_by_semester_name: dict[str, list] = {}
        for semester_subject in self.state.semester_subjects:
            semester = semester_by_id.get(semester_subject.semester_id)
            if semester is None:
                continue
            semester_subjects_by_semester_name.setdefault(semester.semester, []).append(
                semester_subject
            )

        self.state.offerings.clear()
        existing_offering_keys: set[tuple[int, int, int]] = set()

        cursor = self.db_manager.connection.cursor()
        try:
            for period in self.state.enrollment_periods:
                semester_name = f"Semester {period.semester_number}"
                semester_subjects = semester_subjects_by_semester_name.get(semester_name, [])
                if not semester_subjects:
                    continue

                target_count = min(
                    len(section_pool),
                    max(16, int(len(section_pool) * 0.6)),
                )
                target_sections = random.sample(section_pool, target_count)

                for section in target_sections:
                    semester_subject = random.choice(semester_subjects)
                    subject_id = semester_subject.subject_id
                    semester_subject_id = semester_subject.id
                    offering_key = (subject_id, section.id, period.id)
                    if offering_key in existing_offering_keys:
                        continue

                    existing_offering_keys.add(offering_key)

                    if self.db_manager.db_type == "derby":
                        query = """
                            INSERT INTO APP.offerings
                            (subject_id, section_id, enrollment_period_id, semester_subject_id, capacity)
                            VALUES (?, ?, ?, ?, ?)
                        """
                        cursor.execute(
                            query,
                            (
                                subject_id,
                                section.id,
                                period.id,
                                semester_subject_id,
                                section.capacity,
                            ),
                        )
                    else:
                        query = """
                            INSERT INTO offerings
                            (subject_id, section_id, enrollment_period_id, semester_subject_id, capacity)
                            VALUES (%s, %s, %s, %s, %s)
                        """
                        cursor.execute(
                            query,
                            (
                                subject_id,
                                section.id,
                                period.id,
                                semester_subject_id,
                                section.capacity,
                            ),
                        )

                    offering_id = self.adapter.get_last_insert_id(cursor, "offerings")
                    self.state.offerings.append(
                        Offering(
                            id=offering_id,
                            subject_id=subject_id,
                            section_id=section.id,
                            enrollment_period_id=period.id,
                            semester_subject_id=semester_subject_id,
                            capacity=section.capacity,
                        )
                    )

            self.db_manager.commit()
        finally:
            cursor.close()

        print(f"Created {len(self.state.offerings)} offerings")

    def seed_schedules(self) -> None:
        """Seed schedules directly against offering IDs."""
        print("Seeding schedules...")

        if self.db_manager.db_type == "derby":
            self.create_table_if_not_exists("schedules", self.SCHEDULES_CREATE_SQL)

        if not self.state.offerings:
            self.seed_offerings()
        if not self.state.offerings:
            print("No offerings available. Skipping schedules seeding.")
            return

        if not self.state.rooms or not self.state.faculty:
            print("Missing rooms or faculty. Skipping schedules seeding.")
            return

        cursor = self.db_manager.connection.cursor()
        try:
            self.state.schedules.clear()
            section_by_id = {section.id: section for section in self.state.sections}

            for offering in self.state.offerings:
                num_schedules = random.randint(*SCHEDULES_PER_SECTION)
                section = section_by_id.get(offering.section_id)
                room = (
                    self._select_room_for_section(section)
                    if section is not None
                    else random.choice(self.state.rooms)
                )
                used_time_slots: set[tuple[str, str]] = set()

                max_attempts = max(num_schedules * 8, 8)
                attempts = 0

                while len(used_time_slots) < num_schedules and attempts < max_attempts:
                    attempts += 1
                    day = random.choice(DAYS_OF_WEEK)

                    start_hour = random.choice(START_HOURS)
                    start_minute = random.choice(START_MINUTES)
                    end_hour = start_hour + random.choice(DURATION_HOURS)
                    end_minute = start_minute

                    start_time = f"{start_hour:02d}:{start_minute:02d}:00"
                    end_time = f"{end_hour:02d}:{end_minute:02d}:00"
                    faculty = random.choice(self.state.faculty)

                    schedule_slot_key = (day, start_time)
                    if schedule_slot_key in used_time_slots:
                        continue
                    used_time_slots.add(schedule_slot_key)

                    if self.db_manager.db_type == "derby":
                        query = """
                            INSERT INTO APP.schedules
                            (offering_id, room_id, faculty_id, day, start_time, end_time)
                            VALUES (?, ?, ?, ?, ?, ?)
                        """
                        cursor.execute(
                            query,
                            (offering.id, room.id, faculty.id, day, start_time, end_time),
                        )
                    else:
                        query = """
                            INSERT INTO schedules
                            (offering_id, room_id, faculty_id, day, start_time, end_time)
                            VALUES (%s, %s, %s, %s, %s, %s)
                        """
                        cursor.execute(
                            query,
                            (offering.id, room.id, faculty.id, day, start_time, end_time),
                        )

                    schedule_id = self.adapter.get_last_insert_id(cursor, "schedules")
                    self.state.schedules.append(
                        Schedule(
                            id=schedule_id,
                            offering_id=offering.id,
                            room_id=room.id,
                            faculty_id=faculty.id,
                            day=day,
                            start_time=start_time,
                            end_time=end_time,
                        )
                    )

            self.db_manager.commit()
        finally:
            cursor.close()

        print("Created schedules")

    def _select_room_for_section(self, section: Section) -> Room:
        """Select a room that can fit the section capacity."""
        if not self.state.rooms:
            raise ValueError("Cannot seed schedules without any rooms")

        eligible_rooms = [
            room
            for room in self.state.rooms
            if room.status == "AVAILABLE" and room.capacity >= section.capacity
        ]
        if eligible_rooms:
            return random.choice(eligible_rooms)

        return max(
            [room for room in self.state.rooms if room.status == "AVAILABLE"] or self.state.rooms,
            key=lambda room: room.capacity,
        )

    def seed_enrollments(self) -> None:
        """Seed enrollments and related detail tables."""
        print("Seeding enrollments...")

        if self.db_manager.db_type == "derby":
            self.create_table_if_not_exists("enrollments", self.ENROLLMENTS_CREATE_SQL)
            self.create_table_if_not_exists("enrollments_details", self.ENROLLMENT_DETAILS_CREATE_SQL)
            self.create_table_if_not_exists(
                "student_enrolled_subjects",
                self.STUDENT_ENROLLED_SUBJECTS_CREATE_SQL,
            )

        if not self.state.offerings:
            self.seed_offerings()
        if not self.state.offerings:
            raise ValueError("Cannot seed enrollments without offerings")

        offering_period_ids = {offering.enrollment_period_id for offering in self.state.offerings}
        available_periods = [
            period
            for period in self.state.enrollment_periods
            if period.id in offering_period_ids
        ]
        if not available_periods:
            raise ValueError("Cannot seed enrollments without enrollment periods that have offerings")

        cursor = self.db_manager.connection.cursor()
        try:
            statuses = ENROLLMENT_STATUSES
            registrar_user_ids = [
                registrar.user_id
                for registrar in self.state.registrars
                if registrar.user_id is not None
            ]
            offering_occupancy = {offering.id: 0 for offering in self.state.offerings}
            self.state.student_enrolled_subjects.clear()

            for student in self.state.students:
                requested_enrollments = random.randint(*ENROLLMENTS_PER_STUDENT)
                selected_periods = random.sample(
                    available_periods,
                    min(requested_enrollments, len(available_periods)),
                )

                for enrollment_period in selected_periods:
                    semester_num = enrollment_period.semester_number
                    status = random.choice(statuses)

                    max_units = random.uniform(15, 24)
                    total_units = random.uniform(12, max_units)

                    submitted_at = fake.date_time_between(start_date="-2y", end_date="now")
                    approved_by = None
                    approved_at = None
                    if status in {"APPROVED", "ENROLLED"} and registrar_user_ids:
                        approved_by = random.choice(registrar_user_ids)
                        approved_at = fake.date_time_between(start_date=submitted_at, end_date="now")

                    submitted_at_str = self.format_timestamp(submitted_at)
                    approved_at_str = self.format_timestamp(approved_at) if approved_at else None

                    if self.db_manager.db_type == "derby":
                        query = """
                            INSERT INTO APP.enrollments
                            (student_id, enrollment_period_id, status, max_units, total_units, submitted_at, approved_by, approved_at)
                            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
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
                                approved_by,
                                approved_at_str,
                            ),
                        )
                    else:
                        query = """
                            INSERT INTO enrollments
                            (student_id, enrollment_period_id, status, max_units, total_units, submitted_at, approved_by, approved_at)
                            VALUES (%s, %s, %s, %s, %s, %s, %s, %s)
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
                                approved_by,
                                approved_at,
                            ),
                        )

                    if self.db_manager.db_type == "mysql":
                        enrollment_id = cursor.lastrowid
                    else:
                        enrollment_id = self.adapter.get_last_insert_id(cursor, "enrollments")

                    if status in {"APPROVED", "ENROLLED"}:
                        self._create_enrollment_details(
                            cursor,
                            enrollment_id,
                            student.student_id,
                            enrollment_period.id,
                            status,
                            semester_num,
                            offering_occupancy,
                        )

            self.db_manager.commit()
        finally:
            cursor.close()

        print("Created enrollments and enrollment details")

    def seed(self) -> None:
        """Execute all enrollment-related seeding operations."""
        self.seed_enrollment_periods()
        self.seed_offerings()
        self.seed_schedules()
        self.seed_enrollments()
        self.seed_student_semester_progress()

    def seed_student_semester_progress(self) -> None:
        """Seed student_semester_progress based on subject enrollment activity."""
        print("Seeding student semester progress...")

        if self.db_manager.db_type == "derby":
            self.create_table_if_not_exists(
                "student_semester_progress",
                self.STUDENT_SEMESTER_PROGRESS_CREATE_SQL,
            )

        self.state.student_semester_progress.clear()

        course_curriculums: dict[int, list] = {}
        for curriculum in self.state.curriculums:
            course_curriculums.setdefault(curriculum.course_id, []).append(curriculum)

        semester_subjects_by_id = {
            semester_subject.id: semester_subject
            for semester_subject in self.state.semester_subjects
        }
        required_subject_ids_by_semester: dict[int, set[int]] = {}
        for semester_subject in self.state.semester_subjects:
            required_subject_ids_by_semester.setdefault(semester_subject.semester_id, set()).add(
                semester_subject.id
            )

        student_subject_status_by_semester: dict[tuple[str, int], dict[int, str]] = {}
        for enrolled_subject in self.state.student_enrolled_subjects:
            semester_subject = semester_subjects_by_id.get(enrolled_subject.semester_subject_id)
            if semester_subject is None:
                continue

            key = (enrolled_subject.student_id, semester_subject.semester_id)
            statuses_by_subject = student_subject_status_by_semester.setdefault(key, {})
            existing_status = statuses_by_subject.get(semester_subject.id)
            statuses_by_subject[semester_subject.id] = self._prefer_subject_status(
                existing_status,
                enrolled_subject.status,
            )

        semesters_by_curriculum: dict[int, list] = {}
        for semester in self.state.semesters:
            semesters_by_curriculum.setdefault(semester.curriculum_id, []).append(semester)

        cursor = self.db_manager.connection.cursor()
        try:
            if self.db_manager.db_type == "derby":
                cursor.execute("SELECT id, student_id, semester_id FROM APP.student_semester_progress")
            else:
                cursor.execute("SELECT id, student_id, semester_id FROM student_semester_progress")

            existing_progress_ids = {
                (row[1], row[2]): row[0]
                for row in cursor.fetchall()
            }

            for student in tqdm(self.state.students, desc="Creating semester progress", unit="student"):
                student_curriculum = self._select_student_curriculum(student, course_curriculums)
                if student_curriculum is None:
                    continue

                matching_semesters = semesters_by_curriculum.get(student_curriculum.id, [])

                for semester in matching_semesters:
                    required_subject_ids = required_subject_ids_by_semester.get(semester.id, set())
                    subject_statuses = student_subject_status_by_semester.get(
                        (student.student_id, semester.id),
                        {},
                    )

                    status, started_at, completed_at = self._derive_semester_progress(
                        required_subject_ids,
                        subject_statuses,
                    )

                    started_at_value = self.format_timestamp(started_at) if started_at else None
                    completed_at_value = self.format_timestamp(completed_at) if completed_at else None
                    progress_key = (student.student_id, semester.id)
                    progress_id = existing_progress_ids.get(progress_key)

                    if progress_id is None:
                        if self.db_manager.db_type == "derby":
                            query = """
                                INSERT INTO APP.student_semester_progress
                                (student_id, curriculum_id, semester_id, status, started_at, completed_at)
                                VALUES (?, ?, ?, ?, ?, ?)
                            """
                            cursor.execute(
                                query,
                                (
                                    student.student_id,
                                    student_curriculum.id,
                                    semester.id,
                                    status,
                                    started_at_value,
                                    completed_at_value,
                                ),
                            )
                        else:
                            query = """
                                INSERT INTO student_semester_progress
                                (student_id, curriculum_id, semester_id, status, started_at, completed_at)
                                VALUES (%s, %s, %s, %s, %s, %s)
                            """
                            cursor.execute(
                                query,
                                (
                                    student.student_id,
                                    student_curriculum.id,
                                    semester.id,
                                    status,
                                    started_at,
                                    completed_at,
                                ),
                            )

                        progress_id = self.adapter.get_last_insert_id(
                            cursor,
                            "student_semester_progress",
                        )
                        existing_progress_ids[progress_key] = progress_id
                    else:
                        if self.db_manager.db_type == "derby":
                            query = """
                                UPDATE APP.student_semester_progress
                                SET curriculum_id = ?, status = ?, started_at = ?, completed_at = ?,
                                    updated_at = CURRENT_TIMESTAMP
                                WHERE id = ?
                            """
                            cursor.execute(
                                query,
                                (
                                    student_curriculum.id,
                                    status,
                                    started_at_value,
                                    completed_at_value,
                                    progress_id,
                                ),
                            )
                        else:
                            query = """
                                UPDATE student_semester_progress
                                SET curriculum_id = %s, status = %s, started_at = %s, completed_at = %s,
                                    updated_at = CURRENT_TIMESTAMP
                                WHERE id = %s
                            """
                            cursor.execute(
                                query,
                                (
                                    student_curriculum.id,
                                    status,
                                    started_at,
                                    completed_at,
                                    progress_id,
                                ),
                            )

                    self.state.student_semester_progress.append(
                        StudentSemesterProgress(
                            id=progress_id,
                            student_id=student.student_id,
                            curriculum_id=student_curriculum.id,
                            semester_id=semester.id,
                            status=status,
                            started_at=started_at,
                            completed_at=completed_at,
                        )
                    )

            self.db_manager.commit()
        finally:
            cursor.close()

        print(
            "Created "
            f"{len(self.state.student_semester_progress)} student semester progress records"
        )

    @staticmethod
    def _prefer_subject_status(existing_status: str | None, new_status: str) -> str:
        """Pick the strongest status when the same subject appears multiple times."""
        priority = {"COMPLETED": 3, "ENROLLED": 2, "DROPPED": 1}
        if existing_status is None:
            return new_status
        if priority.get(new_status, 0) >= priority.get(existing_status, 0):
            return new_status
        return existing_status

    @staticmethod
    def _derive_semester_progress(
        required_subject_ids: set[int],
        subject_statuses: dict[int, str],
    ) -> tuple[str, datetime | None, datetime | None]:
        """Compute semester progress status and activity timestamps."""
        attempted_required_subject_ids = {
            subject_id for subject_id in subject_statuses if subject_id in required_subject_ids
        }
        if not attempted_required_subject_ids:
            return "NOT_STARTED", None, None

        completed_required_subject_ids = {
            subject_id
            for subject_id, status in subject_statuses.items()
            if subject_id in required_subject_ids and status == "COMPLETED"
        }

        if required_subject_ids and required_subject_ids.issubset(completed_required_subject_ids):
            started_at = fake.date_time_between(start_date="-4y", end_date="-1y")
            completed_at = fake.date_time_between(start_date=started_at, end_date="now")
            if completed_at < started_at:
                completed_at = started_at + timedelta(days=1)
            return "COMPLETED", started_at, completed_at

        started_at = fake.date_time_between(start_date="-2y", end_date="now")
        return "IN_PROGRESS", started_at, None

    def _select_student_curriculum(self, student: Any, course_curriculums: dict[int, list]) -> Any:
        """Select the best curriculum for a student from their course curriculums."""
        matching_curriculums = course_curriculums.get(student.course_id, [])
        if not matching_curriculums:
            return None

        admission_year = self._extract_admission_year(student.student_id)
        if admission_year is None:
            return max(
                matching_curriculums,
                key=lambda curriculum: getattr(curriculum.cur_year, "year", 0),
            )

        return min(
            matching_curriculums,
            key=lambda curriculum: abs(
                getattr(curriculum.cur_year, "year", admission_year) - admission_year
            ),
        )

    @staticmethod
    def _extract_admission_year(student_id: str) -> int | None:
        """Extract admission year from student IDs like YYYY-#####."""
        if not student_id or "-" not in student_id:
            return None
        year_part = student_id.split("-", maxsplit=1)[0]
        if not year_part.isdigit():
            return None
        return int(year_part)

    def _create_enrollment_details(
        self,
        cursor: Any,
        enrollment_id: int,
        student_id: str,
        enrollment_period_id: int,
        enrollment_status: str,
        semester: int = 1,
        offering_occupancy: dict[int, int] | None = None,
    ) -> None:
        """Create enrollment details for an enrollment."""
        if offering_occupancy is None:
            offering_occupancy = {}

        num_subjects = random.randint(3, 7)
        used_offering_ids: set[int] = set()

        student = next((s for s in self.state.students if s.student_id == student_id), None)
        student_course_id = student.course_id if student else None
        student_year_level = student.year_level if student else 1

        matching_semester_ids = self._find_matching_semester_ids(
            semester,
            student_course_id,
            student_year_level,
        )
        if not matching_semester_ids:
            return

        semester_subject_ids_by_subject: dict[int, list[int]] = {}
        for semester_subject in self.state.semester_subjects:
            if semester_subject.semester_id in matching_semester_ids:
                semester_subject_ids_by_subject.setdefault(
                    semester_subject.subject_id,
                    [],
                ).append(semester_subject.id)

        if not semester_subject_ids_by_subject:
            return

        allowed_subject_ids = set(semester_subject_ids_by_subject.keys())

        for _ in range(num_subjects):
            detail_status = random.choice(ENROLLMENT_DETAIL_STATUSES)
            offering = self._select_offering_for_detail(
                enrollment_period_id,
                allowed_subject_ids,
                used_offering_ids,
                offering_occupancy,
                require_capacity=detail_status == "SELECTED",
            )

            if offering is None and detail_status == "SELECTED":
                detail_status = "DROPPED"
                offering = self._select_offering_for_detail(
                    enrollment_period_id,
                    allowed_subject_ids,
                    used_offering_ids,
                    offering_occupancy,
                    require_capacity=False,
                )

            if offering is None:
                break

            subject_id = offering.subject_id
            semester_subject_id = random.choice(semester_subject_ids_by_subject[subject_id])
            if offering.semester_subject_id in semester_subject_ids_by_subject[subject_id]:
                semester_subject_id = offering.semester_subject_id

            units = next((s.units for s in self.state.subjects if s.id == subject_id), 3)

            if self.db_manager.db_type == "derby":
                query = """
                    INSERT INTO APP.enrollments_details
                    (enrollment_id, offering_id, units, status)
                    VALUES (?, ?, ?, ?)
                """
                cursor.execute(
                    query,
                    (enrollment_id, offering.id, units, detail_status),
                )
            else:
                query = """
                    INSERT INTO enrollments_details
                    (enrollment_id, offering_id, units, status)
                    VALUES (%s, %s, %s, %s)
                """
                cursor.execute(
                    query,
                    (enrollment_id, offering.id, units, detail_status),
                )

            used_offering_ids.add(offering.id)

            if detail_status == "SELECTED":
                offering_occupancy[offering.id] = offering_occupancy.get(offering.id, 0) + 1

            student_subject_status = self._derive_student_subject_status(
                enrollment_status,
                detail_status,
            )
            self._insert_student_enrolled_subject(
                cursor,
                student_id,
                enrollment_id,
                offering.id,
                semester_subject_id,
                student_subject_status,
            )

    @staticmethod
    def _derive_student_subject_status(enrollment_status: str, detail_status: str) -> str:
        """Map enrollment and detail state to student subject status."""
        if detail_status == "DROPPED":
            return "DROPPED"

        if enrollment_status == "ENROLLED":
            return random.choices(["ENROLLED", "COMPLETED"], weights=[0.65, 0.35], k=1)[0]

        return "ENROLLED"

    def _find_matching_semester_ids(
        self,
        semester_num: int,
        course_id: int | None,
        year_level: int | None,
    ) -> set[int]:
        """Find semester IDs that match semester number and student academic context."""
        semester_name = f"Semester {semester_num}"
        matching_semesters = [
            semester
            for semester in self.state.semesters
            if semester.semester == semester_name
            and (year_level is None or semester.year_level <= year_level)
        ]

        if course_id is not None:
            course_curriculum_ids = {
                curriculum.id
                for curriculum in self.state.curriculums
                if curriculum.course_id == course_id
            }
            matching_semesters = [
                semester
                for semester in matching_semesters
                if semester.curriculum_id in course_curriculum_ids
            ]

        return {semester.id for semester in matching_semesters}

    def _select_offering_for_detail(
        self,
        enrollment_period_id: int,
        allowed_subject_ids: set[int],
        used_offering_ids: set[int],
        offering_occupancy: dict[int, int],
        require_capacity: bool,
    ) -> Offering | None:
        """Pick an offering that matches period, subject eligibility, and capacity constraints."""
        candidates = [
            offering
            for offering in self.state.offerings
            if offering.enrollment_period_id == enrollment_period_id
            and offering.subject_id in allowed_subject_ids
            and offering.id not in used_offering_ids
        ]

        if require_capacity:
            candidates = [
                offering
                for offering in candidates
                if offering.capacity is None
                or offering_occupancy.get(offering.id, 0) < offering.capacity
            ]

        if not candidates:
            return None

        return random.choice(candidates)

    def _insert_student_enrolled_subject(
        self,
        cursor: Any,
        student_id: str,
        enrollment_id: int,
        offering_id: int,
        semester_subject_id: int,
        status: str,
    ) -> None:
        """Insert student enrolled subject with duplicate handling."""
        inserted = False
        merged_status = status

        if self.db_manager.db_type == "derby":
            cursor.execute(
                """
                    SELECT status FROM APP.student_enrolled_subjects
                    WHERE student_id = ? AND offering_id = ?
                """,
                (student_id, offering_id),
            )
            existing_row = cursor.fetchone()

            if existing_row is None:
                query = """
                    INSERT INTO APP.student_enrolled_subjects
                    (student_id, enrollment_id, offering_id, semester_subject_id, status)
                    VALUES (?, ?, ?, ?, ?)
                """
                cursor.execute(
                    query,
                    (student_id, enrollment_id, offering_id, semester_subject_id, status),
                )
                inserted = True
            else:
                merged_status = self._prefer_subject_status(existing_row[0], status)
                query = """
                    UPDATE APP.student_enrolled_subjects
                    SET enrollment_id = ?, semester_subject_id = ?, status = ?, updated_at = CURRENT_TIMESTAMP
                    WHERE student_id = ? AND offering_id = ?
                """
                cursor.execute(
                    query,
                    (enrollment_id, semester_subject_id, merged_status, student_id, offering_id),
                )
        else:
            cursor.execute(
                """
                    SELECT status FROM student_enrolled_subjects
                    WHERE student_id = %s AND offering_id = %s
                """,
                (student_id, offering_id),
            )
            existing_row = cursor.fetchone()

            if existing_row is None:
                query = """
                    INSERT INTO student_enrolled_subjects
                    (student_id, enrollment_id, offering_id, semester_subject_id, status)
                    VALUES (%s, %s, %s, %s, %s)
                """
                cursor.execute(
                    query,
                    (student_id, enrollment_id, offering_id, semester_subject_id, status),
                )
                inserted = True
            else:
                merged_status = self._prefer_subject_status(existing_row[0], status)
                query = """
                    UPDATE student_enrolled_subjects
                    SET enrollment_id = %s, semester_subject_id = %s, status = %s, updated_at = CURRENT_TIMESTAMP
                    WHERE student_id = %s AND offering_id = %s
                """
                cursor.execute(
                    query,
                    (enrollment_id, semester_subject_id, merged_status, student_id, offering_id),
                )

        state_entry = next(
            (
                subject
                for subject in self.state.student_enrolled_subjects
                if subject.student_id == student_id
                and subject.offering_id == offering_id
            ),
            None,
        )

        if inserted:
            self.state.student_enrolled_subjects.append(
                StudentEnrolledSubject(
                    student_id=student_id,
                    enrollment_id=enrollment_id,
                    offering_id=offering_id,
                    semester_subject_id=semester_subject_id,
                    status=merged_status,
                )
            )
        elif state_entry is not None:
            state_entry.enrollment_id = enrollment_id
            state_entry.semester_subject_id = semester_subject_id
            state_entry.status = merged_status
