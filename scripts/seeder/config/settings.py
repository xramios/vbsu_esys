#!/usr/bin/env python3
"""
Database and runtime settings for the University Database Seeder.

This module contains configuration dictionaries that may be overridden
via environment variables or command-line arguments.
"""

from typing import Dict, Any

# =============================================================================
# MySQL Database Configuration
# =============================================================================

DATABASE_CONFIG: Dict[str, str] = {
    "host": "localhost",
    "database": "university_db",
    "user": "root",
    "password": "",
}

# =============================================================================
# Derby Database Configuration
# =============================================================================

DERBY_CONFIG: Dict[str, Any] = {
    "host": "localhost",
    "database": "sample",
    "user": "APP",
    "password": "derby",
    "port": 1527,
    "driver": "org.apache.derby.client.ClientAutoloadedDriver",
}

# =============================================================================
# Derby Driver Configuration
# =============================================================================

DERBY_JAR_PATH: str = "derbyclient-10.17.1.0.jar"

DERBY_SHARED_JAR_PATH: str = "derbyshared-10.17.1.0.jar"

# =============================================================================
# Table Clear Order (respects foreign key constraints)
# =============================================================================

TABLES_TO_CLEAR: list[str] = [
    "student_semester_progress",
    "student_enrolled_subjects",
    "enrollments_details",
    "schedules",
    "offerings",
    "enrollments",
    "semester_subjects",
    "prerequisites",
    "sections",
    "subjects",
    "semester",
    "curriculum",
    "faculty",
    "registrar",
    "students",
    "users",
    "rooms",
    "courses",
    "departments",
    "enrollment_period",
]
