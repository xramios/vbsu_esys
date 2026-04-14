#!/usr/bin/env python3
"""
Seeding orchestration module.

Coordinates all seeding operations in the correct dependency order,
manages state across seeders, and handles the complete seeding lifecycle.
"""

from typing import Optional

from seeder.core.database import DatabaseManager
from seeder.models.data_models import SeedingState
from seeder.services.department_seeder import DepartmentSeeder
from seeder.services.course_seeder import CourseSeeder
from seeder.services.room_seeder import RoomSeeder
from seeder.services.user_seeder import UserSeeder
from seeder.services.academic_seeder import AcademicSeeder
from seeder.services.enrollment_seeder import EnrollmentSeeder
from seeder.services.drop_request_seeder import DropRequestSeeder
from seeder.services.semester_seeder import SemesterSeeder


class SeedingOrchestrator:
    """Orchestrates the complete database seeding process."""

    def __init__(self, db_manager: DatabaseManager) -> None:
        """Initialize orchestrator with database manager.

        Args:
            db_manager: Database manager instance
        """
        self.db_manager = db_manager
        self.state = SeedingState()

        # Initialize all seeders with shared state
        self.department_seeder = DepartmentSeeder(db_manager, self.state)
        self.course_seeder = CourseSeeder(db_manager, self.state)
        self.room_seeder = RoomSeeder(db_manager, self.state)
        self.user_seeder = UserSeeder(db_manager, self.state)
        self.academic_seeder = AcademicSeeder(db_manager, self.state)
        self.enrollment_seeder = EnrollmentSeeder(db_manager, self.state)
        self.drop_request_seeder = DropRequestSeeder(db_manager, self.state)
        self.semester_seeder = SemesterSeeder(db_manager, self.state)

    def seed_all(self, clear_existing: bool = True) -> bool:
        """Seed all tables with comprehensive data in correct dependency order.

        Args:
            clear_existing: Whether to clear existing data before seeding

        Returns:
            True if seeding successful, False otherwise
        """
        if not self.db_manager.connect():
            return False

        try:
            if clear_existing:
                self._clear_tables()

            # Execute seeders in dependency order
            self.department_seeder.seed()
            self.course_seeder.seed()
            self.room_seeder.seed()
            self.user_seeder.seed_users()
            self.academic_seeder.seed_curriculum()
            self.user_seeder.seed_students()
            self.user_seeder.seed_faculty()
            self.user_seeder.seed_registrars()
            self.academic_seeder.seed_subjects()
            self.academic_seeder.seed_sections()
            self.enrollment_seeder.seed_enrollment_periods()
            self.academic_seeder.seed_prerequisites()
            self.semester_seeder.seed()
            self.enrollment_seeder.seed_offerings()
            self.enrollment_seeder.seed_schedules()
            self.enrollment_seeder.seed_enrollments()
            self.enrollment_seeder.seed_student_semester_progress()
            drop_requests_created = self.drop_request_seeder.seed_drop_requests()

            self._print_summary(drop_requests_created)
            return True

        except Exception as e:
            print(f"Error during seeding: {e}")
            return False
        finally:
            self.db_manager.disconnect()

    def _clear_tables(self) -> None:
        """Clear all tables in correct order to respect foreign key constraints."""
        from seeder.config.settings import TABLES_TO_CLEAR

        print("Clearing existing data...")
        cursor = self.db_manager.connection.cursor()

        for table in TABLES_TO_CLEAR:
            try:
                if self.db_manager.db_type == "derby":
                    self._drop_derby_table(cursor, table)
                else:
                    self._clear_mysql_table(cursor, table)
            except Exception as e:
                print(f"Error clearing {table}: {e}")

        self.db_manager.commit()
        cursor.close()

    def _drop_derby_table(self, cursor: any, table: str) -> None:
        """Drop a Derby table if it exists, ignoring foreign key errors.

        Args:
            cursor: Database cursor
            table: Table name to drop
        """
        try:
            cursor.execute(f"DROP TABLE APP.{table}")
            print(f"Dropped table: {table}")
        except Exception as e:
            if "does not exist" in str(e) or "not found" in str(e).lower():
                print(f"Table {table} does not exist, skipping")
            else:
                try:
                    cursor.execute(f"DELETE FROM APP.{table}")
                    print(f"Cleared table: {table}")
                except Exception as del_err:
                    print(f"Error clearing {table}: {del_err}")

    def _clear_mysql_table(self, cursor: any, table: str) -> None:
        """Clear a MySQL table.

        Args:
            cursor: Database cursor
            table: Table name to clear
        """
        cursor.execute(f"DELETE FROM {table}")
        print(f"Cleared table: {table}")

    def _print_summary(self, drop_requests_created: int = 0) -> None:
        """Print summary of created records."""
        print("\n=== Database Seeding Complete ===")
        print(f"Departments: {len(self.state.departments)}")
        print(f"Courses: {len(self.state.courses)}")
        print(f"Rooms: {len(self.state.rooms)}")
        print(f"Users: {len(self.state.users)}")
        print(f"Students: {len(self.state.students)}")
        print(f"Faculty: {len(self.state.faculty)}")
        print(f"Registrars: {len(self.state.registrars)}")
        print(f"Subjects: {len(self.state.subjects)}")
        print(f"Sections: {len(self.state.sections)}")
        print(f"Curriculum entries: {len(self.state.curriculums)}")
        print(f"Enrollment periods: {len(self.state.enrollment_periods)}")
        print(f"Offerings: {len(self.state.offerings)}")
        print(f"Semesters: {len(self.state.semesters)}")
        print(f"Semester subjects: {len(self.state.semester_subjects)}")
        print(f"Student enrolled subjects: {len(self.state.student_enrolled_subjects)}")
        print(f"Student semester progress: {len(self.state.student_semester_progress)}")
        print(f"Drop requests: {drop_requests_created}")
