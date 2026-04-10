#!/usr/bin/env python3
"""Faculty and registrar seeder for the real-data bundle workflow."""

from __future__ import annotations

from dataclasses import dataclass
from datetime import date
import random
from typing import Any

import bcrypt
from faker import Faker

from seeder.core.database import DatabaseManager


@dataclass(frozen=True, slots=True)
class StaffSeedSummary:
    """Summary of faculty and registrar seeding actions."""

    faculty_created: int
    registrar_created: int
    department_id: int
    registrar_email: str


class RealStaffSeeder:
    """Seeds faculty and registrar accounts for bundle runs."""

    ENGINEERING_DEPARTMENT_NAME = "College of Engineering"
    ENGINEERING_DEPARTMENT_CODE = "COE"
    REGISTRAR_EMAIL = "registrar@vbsu.edu.ph"
    REGISTRAR_PASSWORD = "12345678"

    def __init__(self, db_manager: DatabaseManager, bcrypt_rounds: int = 12) -> None:
        self.db_manager = db_manager
        self.bcrypt_rounds = bcrypt_rounds
        self._placeholder = "?" if db_manager.db_type == "derby" else "%s"
        self._table_prefix = "APP." if db_manager.db_type == "derby" else ""
        self._fake = Faker("en_PH")

    def seed(self, faculty_count: int = 10) -> StaffSeedSummary:
        """Seed faculty and registrar rows with matching user credentials."""
        if faculty_count < 0:
            raise ValueError("faculty_count must be zero or greater")

        if not self.db_manager.connect():
            raise RuntimeError("Failed to connect to database")

        cursor = self.db_manager.connection.cursor()
        try:
            department_id = self._resolve_or_create_engineering_department(cursor)
            used_emails = self._load_existing_emails(cursor)

            faculty_created = self._seed_faculty(
                cursor,
                department_id=department_id,
                faculty_count=faculty_count,
                used_emails=used_emails,
            )
            registrar_created = self._seed_registrar(cursor, used_emails)

            self.db_manager.commit()
            return StaffSeedSummary(
                faculty_created=faculty_created,
                registrar_created=registrar_created,
                department_id=department_id,
                registrar_email=self.REGISTRAR_EMAIL,
            )
        except Exception:
            rollback = getattr(self.db_manager.connection, "rollback", None)
            if callable(rollback):
                rollback()
            raise
        finally:
            cursor.close()
            self.db_manager.disconnect()

    def _seed_faculty(
        self,
        cursor: Any,
        department_id: int,
        faculty_count: int,
        used_emails: set[str],
    ) -> int:
        faculty_created = 0
        for _ in range(faculty_count):
            first_name = self._fake.first_name()
            last_name = self._fake.last_name()
            middle_name = self._fake.first_name() if random.random() < 0.85 else None
            birthdate = self._fake.date_of_birth(minimum_age=24, maximum_age=65)
            contact_number = self._build_contact_number()
            email = self._build_numbered_faculty_email(
                preferred_number=faculty_created + 1,
                used_emails=used_emails,
            )

            plain_password = self._build_faculty_password(last_name, birthdate)
            hashed_password = self._hash_password(plain_password)

            user_id = self.db_manager.execute_insert(
                "users",
                ["email", "password", "role"],
                [email, hashed_password, "FACULTY"],
                return_id=True,
                cursor=cursor,
            )
            if user_id is None:
                raise RuntimeError(f"Failed to create faculty user for {email}")

            self.db_manager.execute_insert(
                "faculty",
                [
                    "user_id",
                    "first_name",
                    "last_name",
                    "middle_name",
                    "contact_number",
                    "birthdate",
                    "department_id",
                ],
                [
                    user_id,
                    first_name,
                    last_name,
                    middle_name,
                    contact_number,
                    self._format_date(birthdate),
                    department_id,
                ],
                return_id=False,
                cursor=cursor,
            )
            faculty_created += 1

        return faculty_created

    def _seed_registrar(self, cursor: Any, used_emails: set[str]) -> int:
        registrar_email = self.REGISTRAR_EMAIL.lower()
        hashed_password = self._hash_password(self.REGISTRAR_PASSWORD)

        user_id = self.db_manager.execute_insert(
            "users",
            ["email", "password", "role"],
            [registrar_email, hashed_password, "REGISTRAR"],
            return_id=True,
            cursor=cursor,
        )
        if user_id is None:
            raise RuntimeError(f"Failed to create registrar user for {registrar_email}")

        used_emails.add(registrar_email)

        self.db_manager.execute_insert(
            "registrar",
            ["user_id", "employee_id", "first_name", "last_name", "contact_number"],
            [user_id, "REG-0001", "System", "Registrar", self._build_contact_number()],
            return_id=False,
            cursor=cursor,
        )
        return 1

    def _resolve_or_create_engineering_department(self, cursor: Any) -> int:
        table = self._table("departments")
        query = (
            f"SELECT id FROM {table} "
            f"WHERE LOWER(department_name) = LOWER({self._placeholder})"
        )
        cursor.execute(query, (self.ENGINEERING_DEPARTMENT_NAME,))
        row = cursor.fetchone()
        if row is not None:
            return int(row[0])

        inserted_id = self.db_manager.execute_insert(
            "departments",
            ["department_name", "department_code", "description"],
            [
                self.ENGINEERING_DEPARTMENT_NAME,
                self.ENGINEERING_DEPARTMENT_CODE,
                "Auto-created by real seeder for faculty assignment",
            ],
            return_id=True,
            cursor=cursor,
        )
        if inserted_id is None:
            raise RuntimeError("Failed to create College of Engineering department")
        return inserted_id

    def _load_existing_emails(self, cursor: Any) -> set[str]:
        query = f"SELECT email FROM {self._table('users')}"
        cursor.execute(query)
        emails: set[str] = set()
        for row in cursor.fetchall():
            if row[0] is not None:
                emails.add(str(row[0]).strip().lower())
        return emails

    def _build_numbered_faculty_email(
        self,
        preferred_number: int,
        used_emails: set[str],
    ) -> str:
        sequence = max(1, preferred_number)
        while True:
            candidate = f"faculty{sequence}@vbsu.edu.ph"
            candidate_key = candidate.lower()
            if candidate_key not in used_emails:
                used_emails.add(candidate_key)
                return candidate
            sequence += 1

    def _build_faculty_password(self, last_name: str, birthdate: date) -> str:
        """Match FacultyForm password format: LastName_YYYY-MM-DD."""
        return f"{last_name}_{birthdate.isoformat()}"

    def _build_contact_number(self) -> str:
        return (
            f"09{random.randint(10, 99)}-"
            f"{random.randint(100, 999)}-"
            f"{random.randint(1000, 9999)}"
        )

    def _hash_password(self, plain_password: str) -> str:
        return bcrypt.hashpw(
            plain_password.encode("utf-8"),
            bcrypt.gensalt(rounds=self.bcrypt_rounds),
        ).decode("utf-8")

    def _format_date(self, value: date) -> str | date:
        if self.db_manager.db_type == "derby":
            return value.strftime("%Y-%m-%d")
        return value

    def _table(self, table_name: str) -> str:
        return f"{self._table_prefix}{table_name}"
