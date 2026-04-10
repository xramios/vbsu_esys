#!/usr/bin/env python3
"""CLI for seeding real curriculum data."""

from __future__ import annotations

import argparse
import sys
from pathlib import Path
from typing import Any

from seeder.config.settings import DATABASE_CONFIG, DERBY_CONFIG
from seeder.core.database import DatabaseManager

from real_seeder.bundle_seeder import BsitNiten2023BundleSeeder
from real_seeder.curriculum_seeder import RealCurriculumSeeder
from real_seeder.students_seeder import RealStudentsSeeder


DEFAULT_CURRICULUM_CSV = (
    Path(__file__).resolve().parents[2]
    / "docs"
    / "ENROLLMENT SYSTEM CCP FINALS - Curriculum NITEN2023.csv"
)


def create_parser() -> argparse.ArgumentParser:
    """Create command-line parser for the real curriculum seeder."""
    parser = argparse.ArgumentParser(description="Seed real data from CSV")
    parser.add_argument(
        "--target",
        choices=["bundle", "curriculum", "students"],
        default="bundle",
        help="Seeding target",
    )
    parser.add_argument(
        "--csv",
        type=Path,
        default=DEFAULT_CURRICULUM_CSV,
        help="Path to curriculum CSV file",
    )
    parser.add_argument(
        "--db-type",
        choices=["mysql", "derby"],
        default="derby",
        help="Database type to use",
    )
    parser.add_argument("--host", help="Database host")
    parser.add_argument("--database", help="Database name")
    parser.add_argument("--user", help="Database user")
    parser.add_argument("--password", help="Database password")

    parser.add_argument(
        "--course-id",
        type=int,
        help="Existing course ID that the curriculum belongs to",
    )
    parser.add_argument(
        "--course-name",
        default="Bachelor of Science in Information Technology",
        help="Course name used when --course-id is not provided",
    )
    parser.add_argument(
        "--create-course-if-missing",
        action="store_true",
        help="Create course record when course name does not exist",
    )

    parser.add_argument(
        "--curriculum-name",
        default="NITEN2023",
        help="Curriculum code/name to store in curriculum table",
    )
    parser.add_argument(
        "--curriculum-year",
        type=int,
        default=2023,
        help="Curriculum year stored in curriculum.cur_year",
    )
    parser.add_argument(
        "--subject-name-max-length",
        type=int,
        default=32,
        help="Maximum subject_name characters for safe inserts",
    )

    parser.add_argument(
        "--students-csv",
        type=Path,
        default=(Path(__file__).resolve().parents[2] / "docs" / "students.csv"),
        help="Path to students CSV file",
    )
    parser.add_argument(
        "--students-course",
        default="BSIT",
        help="Course hint used to resolve course_id for student imports",
    )
    parser.add_argument(
        "--students-curriculum",
        default="NITEN2023",
        help="Curriculum name for student imports",
    )
    parser.add_argument(
        "--bcrypt-rounds",
        type=int,
        default=12,
        help="BCrypt rounds used to hash derived student passwords",
    )
    parser.add_argument(
        "--faculty-count",
        type=int,
        default=10,
        help="Number of Faker-generated faculty accounts for bundle target",
    )
    return parser


def _resolve_db_value(args_value: str | None, defaults: dict[str, Any], key: str) -> str:
    if args_value is not None:
        return args_value
    value = defaults.get(key)
    return "" if value is None else str(value)


def main(argv: list[str] | None = None) -> int:
    """Execute CLI command."""
    parser = create_parser()
    args = parser.parse_args(argv)

    config_defaults = DERBY_CONFIG if args.db_type == "derby" else DATABASE_CONFIG

    db_manager = DatabaseManager(
        db_type=args.db_type,
        host=_resolve_db_value(args.host, config_defaults, "host"),
        database=_resolve_db_value(args.database, config_defaults, "database"),
        user=_resolve_db_value(args.user, config_defaults, "user"),
        password=_resolve_db_value(args.password, config_defaults, "password"),
    )

    if args.target == "bundle":
        seeder = BsitNiten2023BundleSeeder(
            db_manager,
            bcrypt_rounds=args.bcrypt_rounds,
        )
        try:
            summary = seeder.run(
                curriculum_csv_path=args.csv,
                students_csv_path=args.students_csv,
                course_name=args.course_name,
                curriculum_name=args.curriculum_name,
                curriculum_year=args.curriculum_year,
                students_course_hint=args.students_course,
                faculty_count=args.faculty_count,
            )
        except Exception as exc:
            print(f"Bundle seeding failed: {exc}")
            return 1

        print("Bundle seeding completed successfully.")
        print(f"Reset actions: {', '.join(summary.cleared_tables)}")
        print(f"Course ID: {summary.curriculum.course_id}")
        print(f"Curriculum ID: {summary.curriculum.curriculum_id}")
        print(f"Subjects created: {summary.curriculum.subjects_created}")
        print(f"Semesters created: {summary.curriculum.semesters_created}")
        print(
            "Semester-subject links created: "
            f"{summary.curriculum.semester_subject_links_created}"
        )
        print(f"Prerequisites created: {summary.curriculum.prerequisites_created}")
        print(f"Users created: {summary.students.users_created}")
        print(f"Students created: {summary.students.students_created}")
        print(f"Faculty created: {summary.staff.faculty_created}")
        print(f"Registrar created: {summary.staff.registrar_created}")
        print(f"Registrar email: {summary.staff.registrar_email}")
        print("Registrar password: 12345678")
        return 0

    if args.target == "students":
        seeder = RealStudentsSeeder(db_manager, bcrypt_rounds=args.bcrypt_rounds)
        try:
            summary = seeder.seed_from_csv(
                csv_path=args.students_csv,
                course_hint=args.students_course,
                curriculum_name=args.students_curriculum,
            )
        except Exception as exc:
            print(f"Students seeding failed: {exc}")
            return 1

        print("Students seeding completed successfully.")
        print(f"Course ID: {summary.course_id}")
        print(f"Curriculum ID: {summary.curriculum_id}")
        print(f"Users created: {summary.users_created}")
        print(f"Users updated: {summary.users_updated}")
        print(f"Students created: {summary.students_created}")
        print(f"Students updated: {summary.students_updated}")
        return 0

    seeder = RealCurriculumSeeder(
        db_manager,
        subject_name_max_length=args.subject_name_max_length,
    )

    try:
        summary = seeder.seed_from_csv(
            csv_path=args.csv,
            curriculum_name=args.curriculum_name,
            curriculum_year=args.curriculum_year,
            course_id=args.course_id,
            course_name=args.course_name,
            create_course_if_missing=args.create_course_if_missing,
        )
    except Exception as exc:
        print(f"Curriculum seeding failed: {exc}")
        return 1

    print("Curriculum seeding completed successfully.")
    print(f"Course ID: {summary.course_id}")
    print(f"Curriculum ID: {summary.curriculum_id}")
    print(f"Subjects created: {summary.subjects_created}")
    print(f"Subjects updated: {summary.subjects_updated}")
    print(f"Semesters created: {summary.semesters_created}")
    print(f"Semester-subject links created: {summary.semester_subject_links_created}")
    print(f"Prerequisites created: {summary.prerequisites_created}")
    print(f"Prerequisite codes skipped (missing subjects): {summary.prerequisite_codes_skipped}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
