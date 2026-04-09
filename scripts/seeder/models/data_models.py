#!/usr/bin/env python3
"""Data models for the University Database Seeder."""

from dataclasses import dataclass, field
from typing import Optional, List
from datetime import datetime


@dataclass
class Department:
    """Represents a university department."""

    id: int
    department_name: str
    description: str
    created_at: Optional[datetime] = None
    updated_at: Optional[datetime] = None

    @property
    def name(self) -> str:
        """Backward-compatible alias for department_name."""
        return self.department_name

    @name.setter
    def name(self, value: str) -> None:
        self.department_name = value


@dataclass
class Course:
    """Represents an academic course/program."""

    id: int
    course_name: str
    department_id: int
    description: Optional[str] = None
    created_at: Optional[datetime] = None
    updated_at: Optional[datetime] = None

    @property
    def name(self) -> str:
        """Backward-compatible alias for course_name."""
        return self.course_name

    @name.setter
    def name(self, value: str) -> None:
        self.course_name = value


@dataclass
class Room:
    """Represents a physical classroom or facility."""

    id: int
    building: str
    room_type: str
    status: str
    room: str
    capacity: int
    created_at: Optional[datetime] = None
    updated_at: Optional[datetime] = None


@dataclass
class User:
    """Represents a system user account."""

    id: int
    email: str
    role: str
    user_type: str
    password: Optional[str] = None
    created_at: Optional[datetime] = None
    updated_at: Optional[datetime] = None


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
    created_at: Optional[datetime] = None
    updated_at: Optional[datetime] = None


@dataclass
class Faculty:
    """Represents a faculty member record."""

    id: int
    user_id: int
    first_name: str
    last_name: str
    department_id: int
    created_at: Optional[datetime] = None
    updated_at: Optional[datetime] = None


@dataclass
class Registrar:
    """Represents a registrar staff member record."""

    id: int
    user_id: int
    employee_id: str
    first_name: str
    last_name: str
    contact_number: Optional[str] = None
    created_at: Optional[datetime] = None
    updated_at: Optional[datetime] = None


@dataclass
class Curriculum:
    """Represents a curriculum entry linking course and academic year."""

    id: int
    name: str
    course: int
    cur_year: Optional[datetime] = None
    created_at: Optional[datetime] = None
    updated_at: Optional[datetime] = None

    @property
    def course_id(self) -> int:
        """Backward-compatible alias for course."""
        return self.course

    @course_id.setter
    def course_id(self, value: int) -> None:
        self.course = value


@dataclass
class Subject:
    """Represents an academic subject/course."""

    id: int
    subject_name: str
    subject_code: str
    units: float
    department_id: int
    description: Optional[str] = None
    created_at: Optional[datetime] = None
    updated_at: Optional[datetime] = None

    @property
    def name(self) -> str:
        """Backward-compatible alias for subject_name."""
        return self.subject_name

    @name.setter
    def name(self, value: str) -> None:
        self.subject_name = value

    @property
    def code(self) -> str:
        """Backward-compatible alias for subject_code."""
        return self.subject_code

    @code.setter
    def code(self, value: str) -> None:
        self.subject_code = value


@dataclass
class Section:
    """Represents a class section for a subject."""

    id: int
    section_name: str
    section_code: str
    subject_id: int
    capacity: int
    created_at: Optional[datetime] = None
    updated_at: Optional[datetime] = None

    @property
    def name(self) -> str:
        """Backward-compatible alias for section_name."""
        return self.section_name

    @name.setter
    def name(self, value: str) -> None:
        self.section_name = value

    @property
    def code(self) -> str:
        """Backward-compatible alias for section_code."""
        return self.section_code

    @code.setter
    def code(self, value: str) -> None:
        self.section_code = value


@dataclass
class EnrollmentPeriod:
    """Represents an enrollment period for a semester."""

    id: int
    school_year: str
    semester: str
    start_date: datetime
    end_date: datetime
    created_at: Optional[datetime] = None
    updated_at: Optional[datetime] = None

    @property
    def semester_number(self) -> int:
        """Map semester labels to a numeric representation used by seed logic."""
        normalized = self.semester.strip().lower()
        if normalized in {"first", "1", "semester 1"}:
            return 1
        if normalized in {"second", "2", "semester 2"}:
            return 2
        if normalized in {"summer", "3", "semester 3"}:
            return 3
        return 1


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
    created_at: Optional[datetime] = None
    updated_at: Optional[datetime] = None


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
    created_at: Optional[datetime] = None
    updated_at: Optional[datetime] = None


@dataclass
class EnrollmentDetail:
    """Represents an enrollment detail (selected subject)."""

    enrollment_id: int
    section_id: int
    subject_id: int
    units: float
    status: str
    id: Optional[int] = None
    created_at: Optional[datetime] = None
    updated_at: Optional[datetime] = None


@dataclass
class Semester:
    """Represents a semester linked to a curriculum."""

    id: int
    curriculum_id: int
    semester: str
    created_at: Optional[datetime] = None
    updated_at: Optional[datetime] = None


@dataclass
class SemesterSubject:
    """Represents a many-to-many link between semesters and subjects."""

    id: int
    semester_id: int
    subject_id: int
    year_level: int
    created_at: Optional[datetime] = None
    updated_at: Optional[datetime] = None


@dataclass
class StudentEnrolledSubject:
    """Represents a student's subject enrollment with status."""

    student_id: str
    semester_subject_id: int
    status: str = "ENROLLED"
    created_at: Optional[datetime] = None
    updated_at: Optional[datetime] = None


@dataclass
class StudentSemesterProgress:
    """Represents a student's progress summary for a curriculum semester."""

    student_id: str
    curriculum_id: int
    semester_id: int
    status: str = "NOT_STARTED"
    id: Optional[int] = None
    started_at: Optional[datetime] = None
    completed_at: Optional[datetime] = None
    created_at: Optional[datetime] = None
    updated_at: Optional[datetime] = None


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
    student_semester_progress: List[StudentSemesterProgress] = field(default_factory=list)
