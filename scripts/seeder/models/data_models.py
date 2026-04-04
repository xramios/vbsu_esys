#!/usr/bin/env python3
"""Data models for the University Database Seeder."""

from dataclasses import dataclass, field
from typing import Optional, List
from datetime import datetime


@dataclass
class Department:
    """Represents a university department."""

    id: int
    name: str
    description: str


@dataclass
class Course:
    """Represents an academic course/program."""

    id: int
    name: str
    department_id: int
    description: Optional[str] = None


@dataclass
class Room:
    """Represents a physical classroom or facility."""

    id: int
    room: str
    capacity: int


@dataclass
class User:
    """Represents a system user account."""

    id: int
    email: str
    role: str
    user_type: str
    password: Optional[str] = None


@dataclass
class Student:
    """Represents a student record."""

    student_id: str
    user_id: int
    first_name: str
    last_name: str
    course_id: int
    year_level: int
    middle_name: Optional[str] = None
    birthdate: Optional[datetime] = None
    student_status: str = "REGULAR"


@dataclass
class Faculty:
    """Represents a faculty member record."""

    id: int
    user_id: int
    first_name: str
    last_name: str
    department_id: int


@dataclass
class Registrar:
    """Represents a registrar staff member record."""

    id: int
    user_id: int
    employee_id: str
    first_name: str
    last_name: str
    contact_number: Optional[str] = None


@dataclass
class Curriculum:
    """Represents a curriculum entry linking course and semester."""

    id: int
    semester: str
    course_id: int
    cur_year: Optional[datetime] = None


@dataclass
class Subject:
    """Represents an academic subject/course."""

    id: int
    name: str
    code: str
    units: int
    department_id: int
    description: Optional[str] = None
    curriculum_id: Optional[int] = None


@dataclass
class Section:
    """Represents a class section for a subject."""

    id: int
    name: str
    code: str
    subject_id: int
    capacity: int


@dataclass
class EnrollmentPeriod:
    """Represents an enrollment period for a semester."""

    id: int
    school_year: str
    semester: int
    start_date: datetime
    end_date: datetime


@dataclass
class Schedule:
    """Represents a class schedule entry."""

    section_id: int
    room_id: int
    faculty_id: int
    day: str
    start_time: str
    end_time: str
    enrollment_period_id: int
    id: Optional[int] = None


@dataclass
class Enrollment:
    """Represents a student enrollment record."""

    student_id: str
    enrollment_period_id: int
    status: str
    max_units: float
    total_units: float
    submitted_at: datetime
    id: Optional[int] = None


@dataclass
class EnrollmentDetail:
    """Represents an enrollment detail (selected subject)."""

    enrollment_id: int
    section_id: int
    subject_id: int
    units: float
    status: str
    id: Optional[int] = None


@dataclass
class Semester:
    """Represents a semester linked to a curriculum."""

    id: int
    curriculum_id: int
    semester: str


@dataclass
class SemesterSubject:
    """Represents a many-to-many link between semesters and subjects."""

    id: int
    semester_id: int
    subject_id: int
    year_level: int


@dataclass
class StudentEnrolledSubject:
    """Represents a student's subject enrollment with status."""

    student_id: str
    semester_subject_id: int
    status: str = "ENROLLED"


@dataclass
class SeedingState:
    """Maintains state across seeding operations."""

    departments: List[Department] = field(default_factory=list)
    courses: List[Course] = field(default_factory=list)
    rooms: List[Room] = field(default_factory=list)
    users: List[User] = field(default_factory=list)
    faculty: List[Faculty] = field(default_factory=list)
    registrars: List[Registrar] = field(default_factory=list)
    students: List[Student] = field(default_factory=list)
    subjects: List[Subject] = field(default_factory=list)
    sections: List[Section] = field(default_factory=list)
    curriculums: List[Curriculum] = field(default_factory=list)
    enrollment_periods: List[EnrollmentPeriod] = field(default_factory=list)
    semesters: List[Semester] = field(default_factory=list)
    semester_subjects: List[SemesterSubject] = field(default_factory=list)
    student_enrolled_subjects: List[StudentEnrolledSubject] = field(default_factory=list)
