#!/usr/bin/env python3
"""Seeder for faculty drop request records."""

from __future__ import annotations

from dataclasses import dataclass
import random
from typing import TYPE_CHECKING

from faker import Faker

from seeder.config.constants import SEEDING_COUNTS
from seeder.services.base_seeder import BaseSeeder

if TYPE_CHECKING:
    from seeder.core.database import DatabaseManager
    from seeder.models.data_models import SeedingState


@dataclass(frozen=True, slots=True)
class DropRequestCandidate:
    """Potential faculty drop request built from enrollment data."""

    enrollment_id: int
    faculty_id: int
    student_id: str
    offering_id: int
    subject_code: str
    subject_name: str
    section_code: str
    school_year: str
    semester: str


class DropRequestSeeder(BaseSeeder):
    """Seeds pending faculty drop requests for registrar review."""

    DROP_REQUESTS_CREATE_SQL = """
        CREATE TABLE APP.faculty_student_drop_requests (
            id BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
            faculty_id BIGINT NOT NULL,
            student_id VARCHAR(32) NOT NULL,
            offering_id BIGINT NOT NULL,
            reason CLOB,
            status VARCHAR(20) CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')) DEFAULT 'PENDING',
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            UNIQUE (student_id, offering_id)
        )
    """

    REASON_TEMPLATES: tuple[str, ...] = (
        "Attendance and participation concern in {subject_code} {section_code}.",
        "Schedule conflict reported for {subject_code} {section_code} during {semester_label}.",
        "Faculty review requested for {subject_code} {section_code} due to performance issues.",
        "Requested drop for {subject_code} {section_code} after repeated absences.",
        "Advising concern for {subject_code} {section_code} in {semester_label}.",
    )

    def __init__(self, db_manager: "DatabaseManager", state: "SeedingState") -> None:
        super().__init__(db_manager, state)
        self._fake = Faker("en_PH")
        self._placeholder = "?" if db_manager.db_type == "derby" else "%s"

    def seed_drop_requests(self, count: int | None = None) -> int:
        """Seed pending drop requests from existing enrollment detail rows."""
        target_count = count if count is not None else SEEDING_COUNTS["drop_requests"]
        if target_count <= 0:
            return 0

        print(f"Seeding drop requests... target={target_count}")

        if self.db_manager.db_type == "derby":
            self.create_table_if_not_exists("faculty_student_drop_requests", self.DROP_REQUESTS_CREATE_SQL)

        if not self._table_exists("faculty_student_drop_requests"):
            print("faculty_student_drop_requests table not available. Skipping drop request seeding.")
            return 0

        candidates = self._load_candidates()
        if not candidates:
            print("No eligible enrollment rows found. Skipping drop request seeding.")
            return 0

        existing_pairs = self._load_existing_pairs()
        eligible_candidates = [
            candidate
            for candidate in candidates
            if (candidate.student_id, candidate.offering_id) not in existing_pairs
        ]
        if not eligible_candidates:
            print("No eligible drop request candidates remain after duplicate filtering.")
            return 0

        sample_size = min(target_count, len(eligible_candidates))
        selected_candidates = random.sample(eligible_candidates, sample_size)

        created = 0
        cursor = self.db_manager.connection.cursor()
        try:
            for candidate in selected_candidates:
                request_created_at = self._fake.date_time_between(start_date="-120d", end_date="now")
                created_at_value = self.format_timestamp(request_created_at)
                reason = self._build_reason(candidate)

                if self.db_manager.db_type == "derby":
                    query = """
                        INSERT INTO APP.faculty_student_drop_requests
                        (faculty_id, student_id, offering_id, reason, status, created_at, updated_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?)
                    """
                    cursor.execute(
                        query,
                        (
                            candidate.faculty_id,
                            candidate.student_id,
                            candidate.offering_id,
                            reason,
                            "PENDING",
                            created_at_value,
                            created_at_value,
                        ),
                    )
                else:
                    query = """
                        INSERT INTO faculty_student_drop_requests
                        (faculty_id, student_id, offering_id, reason, status, created_at, updated_at)
                        VALUES (%s, %s, %s, %s, %s, %s, %s)
                    """
                    cursor.execute(
                        query,
                        (
                            candidate.faculty_id,
                            candidate.student_id,
                            candidate.offering_id,
                            reason,
                            "PENDING",
                            request_created_at,
                            request_created_at,
                        ),
                    )

                created += 1

            self.db_manager.commit()
        finally:
            cursor.close()

        print(f"Created {created} faculty drop requests")
        return created

    def _load_candidates(self) -> list[DropRequestCandidate]:
        cursor = self.db_manager.connection.cursor()
        try:
            sql = f"""
                SELECT
                    e.id AS enrollment_id,
                    e.student_id,
                    ed.offering_id,
                    MIN(s.faculty_id) AS faculty_id,
                    sub.subject_code,
                    sub.subject_name,
                    sec.section_code,
                    ep.school_year,
                    ep.semester
                FROM enrollments_details ed
                INNER JOIN enrollments e ON e.id = ed.enrollment_id
                INNER JOIN offerings o ON o.id = ed.offering_id
                INNER JOIN subjects sub ON sub.id = o.subject_id
                INNER JOIN sections sec ON sec.id = o.section_id
                INNER JOIN enrollment_period ep ON ep.id = o.enrollment_period_id
                INNER JOIN schedules s ON s.offering_id = o.id
                WHERE ed.status = {self._placeholder}
                  AND e.status IN ('DRAFT', 'SUBMITTED', 'APPROVED', 'ENROLLED')
                GROUP BY e.id, e.student_id, ed.offering_id, sub.subject_code, sub.subject_name, sec.section_code, ep.school_year, ep.semester
                ORDER BY e.id DESC, ed.offering_id ASC
            """
            cursor.execute(sql, ("SELECTED",))

            candidates: list[DropRequestCandidate] = []
            for row in cursor.fetchall():
                faculty_id = row[3]
                student_id = row[1]
                offering_id = row[2]
                if faculty_id is None or student_id is None or offering_id is None:
                    continue

                candidates.append(
                    DropRequestCandidate(
                        enrollment_id=int(row[0]),
                        student_id=str(student_id).strip(),
                        offering_id=int(offering_id),
                        faculty_id=int(faculty_id),
                        subject_code=str(row[4]),
                        subject_name=str(row[5]),
                        section_code=str(row[6]),
                        school_year=str(row[7]),
                        semester=str(row[8]),
                    )
                )

            return candidates
        finally:
            cursor.close()

    def _load_existing_pairs(self) -> set[tuple[str, int]]:
        cursor = self.db_manager.connection.cursor()
        try:
            cursor.execute(self._existing_pairs_query())
            pairs: set[tuple[str, int]] = set()
            for row in cursor.fetchall():
                if row[0] is None or row[1] is None:
                    continue
                pairs.add((str(row[0]).strip(), int(row[1])))
            return pairs
        finally:
            cursor.close()

    def _existing_pairs_query(self) -> str:
        table = self._table("faculty_student_drop_requests")
        return f"SELECT student_id, offering_id FROM {table}"

    def _build_reason(self, candidate: DropRequestCandidate) -> str:
        semester_label = f"{candidate.school_year} | {candidate.semester}"
        template = random.choice(self.REASON_TEMPLATES)
        return template.format(
            subject_code=candidate.subject_code,
            subject_name=candidate.subject_name,
            section_code=candidate.section_code,
            semester_label=semester_label,
        )

    def _table_exists(self, table_name: str) -> bool:
        cursor = self.db_manager.connection.cursor()
        try:
            if self.db_manager.db_type == "derby":
                query = (
                    "SELECT COUNT(*) FROM SYS.SYSTABLES t "
                    "JOIN SYS.SYSSCHEMAS s ON t.SCHEMAID = s.SCHEMAID "
                    "WHERE s.SCHEMANAME = ? AND t.TABLENAME = ?"
                )
                cursor.execute(query, ("APP", table_name.upper()))
            else:
                query = (
                    "SELECT COUNT(*) FROM information_schema.tables "
                    "WHERE table_schema = %s AND table_name = %s"
                )
                cursor.execute(query, (self.db_manager.database, table_name))

            row = cursor.fetchone()
            return bool(row and int(row[0]) > 0)
        finally:
            cursor.close()

    def _table(self, table_name: str) -> str:
        return f"APP.{table_name}" if self.db_manager.db_type == "derby" else table_name