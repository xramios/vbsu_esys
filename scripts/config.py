#!/usr/bin/env python3
"""
Configuration file for University Database Seeder and Visualization
Contains all hardcoded values and settings in one centralized location
"""

# Database Configuration
DATABASE_CONFIG = {
    'host': 'localhost',
    'database': 'university_db',
    'user': 'root',
    'password': ''
}

# Derby Database Configuration
DERBY_CONFIG = {
    'host': 'localhost',
    'database': 'sample',
    'user': 'APP',
    'password': 'derby',
    'port': 1527,
    'driver': 'org.apache.derby.client.ClientAutoloadedDriver'
}

# Department Data
DEPARTMENT_DATA = [
    ('College of Engineering', 'Offers various engineering programs including Civil, Electrical, Mechanical, and Computer Engineering'),
    ('College of Business Administration', 'Provides business education with majors in Management, Accounting, Marketing, and Finance'),
    ('College of Arts and Sciences', 'Liberal arts college offering programs in Humanities, Social Sciences, and Natural Sciences'),
    ('College of Education', 'Teacher education institution producing future educators and school administrators'),
    ('College of Nursing', 'Healthcare education provider offering Bachelor of Science in Nursing'),
    ('College of Information Technology', 'Technology-focused college offering Computer Science, Information Technology, and Data Science programs'),
    ('College of Architecture', 'Architecture and design education provider'),
    ('College of Law', 'Legal education institution offering Juris Doctor program')
]

# Course Data (course_name, description, department_index)
COURSE_DATA = [
    ('Bachelor of Science in Computer Science', '4-year degree program focusing on software development, algorithms, and computing theory', 5),
    ('Bachelor of Science in Information Technology', 'Program focusing on IT infrastructure, network management, and systems administration', 5),
    ('Bachelor of Science in Civil Engineering', '5-year program covering structural design, construction management, and transportation engineering', 0),
    ('Bachelor of Science in Electrical Engineering', '5-year program focusing on power systems, electronics, and telecommunications', 0),
    ('Bachelor of Science in Mechanical Engineering', '5-year program covering thermodynamics, machine design, and manufacturing processes', 0),
    ('Bachelor of Science in Accountancy', '4-year program preparing students for CPA licensure and accounting careers', 1),
    ('Bachelor of Science in Business Administration', '4-year program with majors in Management, Marketing, and Finance', 1),
    ('Bachelor of Arts in English', '4-year liberal arts program focusing on literature and language studies', 2),
    ('Bachelor of Science in Psychology', '4-year program studying human behavior and mental processes', 2),
    ('Bachelor of Secondary Education', '4-year teacher education program', 3),
    ('Bachelor of Science in Nursing', '4-year professional nursing program', 4),
    ('Bachelor of Science in Architecture', '5-year professional architecture program', 6),
    ('Bachelor of Arts in Communication', '4-year program in mass communication and media studies', 2),
    ('Bachelor of Science in Mathematics', '4-year program in pure and applied mathematics', 2),
    ('Juris Doctor', '4-year professional law degree program', 7)
]

# Room Types and Capacities
ROOM_TYPES = [
    ('Lecture Hall', 150),
    ('Laboratory', 40),
    ('Computer Lab', 35),
    ('Discussion Room', 25),
    ('Conference Room', 20),
    ('Auditorium', 300),
    ('Classroom', 50),
    ('Seminar Room', 30)
]

BUILDING_NAMES = ['Engineering', 'Business', 'Arts', 'Science', 'Main']

# Subject Templates (name, code_prefix, units, description)
SUBJECT_TEMPLATES = [
    ('Calculus', 'CALC', 3, 'Mathematical analysis of functions, limits, derivatives, and integrals'),
    ('Physics', 'PHYS', 4, 'Study of matter, energy, and their interactions'),
    ('Chemistry', 'CHEM', 3, 'Study of matter, its properties, composition, and reactions'),
    ('Programming', 'PROG', 3, 'Introduction to computer programming concepts and practices'),
    ('Data Structures', 'DS', 3, 'Study of data organization and manipulation algorithms'),
    ('Database Systems', 'DB', 3, 'Design and implementation of database management systems'),
    ('Software Engineering', 'SE', 3, 'Principles and practices of software development'),
    ('Web Development', 'WEB', 3, 'Design and development of web applications'),
    ('Accounting Principles', 'ACC', 3, 'Fundamental concepts and principles of accounting'),
    ('Business Finance', 'FIN', 3, 'Financial management and analysis in business'),
    ('Marketing Management', 'MKT', 3, 'Principles and strategies in marketing'),
    ('Organizational Behavior', 'ORG', 3, 'Study of human behavior in organizations'),
    ('Educational Psychology', 'EDPSY', 3, 'Psychological principles in education'),
    ('Teaching Methods', 'TCH', 3, 'Methodologies and strategies in teaching'),
    ('Nursing Fundamentals', 'NURS', 4, 'Basic principles and practices in nursing'),
    ('Anatomy and Physiology', 'ANAT', 4, 'Study of human body structure and function'),
    ('Engineering Mathematics', 'ENG MATH', 3, 'Advanced mathematics for engineering applications'),
    ('Thermodynamics', 'THERMO', 3, 'Study of heat, work, and energy'),
    ('Strength of Materials', 'SOM', 3, 'Analysis of material properties under stress'),
    ('Circuit Analysis', 'CIRCUIT', 3, 'Analysis of electrical circuits and components'),
    ('Digital Logic', 'DIGITAL', 3, 'Study of digital systems and logic design'),
    ('Machine Design', 'MACH', 3, 'Principles of mechanical machine design'),
    ('Structural Analysis', 'STRUCT', 3, 'Analysis of structures and loads'),
    ('Business Law', 'BLAW', 3, 'Legal aspects of business operations'),
    ('Cost Accounting', 'COST', 3, 'Accounting for product and service costs'),
    ('Human Resource Management', 'HRM', 3, 'Management of human resources in organizations'),
    ('Operations Management', 'OPS', 3, 'Management of production and service operations'),
    ('Literature', 'LIT', 3, 'Study of literary works and criticism'),
    ('Philosophy', 'PHIL', 3, 'Study of fundamental questions about existence and knowledge'),
    ('Statistics', 'STAT', 3, 'Mathematical analysis of data and probability'),
    ('Research Methods', 'RES', 3, 'Methodologies for conducting research'),
]

# Schedule Configuration
DAYS_OF_WEEK = ['MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT']

# Enrollment Statuses
ENROLLMENT_STATUSES = ['DRAFT', 'SUBMITTED', 'APPROVED', 'ENROLLED', 'CANCELLED']

ENROLLMENT_DETAIL_STATUSES = ['SELECTED', 'DROPPED']

# Student Status Configuration
STUDENT_STATUS_CONFIG = {
    'irregular_probability': 0.12,  # 12% chance of being IRREGULAR
    'regular_status': 'REGULAR',
    'irregular_status': 'IRREGULAR'
}

# User Roles
USER_ROLES = {
    'STUDENT': 'STUDENT',
    'FACULTY': 'FACULTY', 
    'REGISTRAR': 'REGISTRAR'
}

# Seeding Counts
SEEDING_COUNTS = {
    'departments': 8,
    'courses': 15,
    'rooms': 100,
    'students': 4391,
    'faculty': 50,
    'registrars': 5,
    'subjects': 100,
    'enrollment_periods': 4
}

# Student Demographics
STUDENT_DEMOGRAPHICS = {
    'age_range': (18, 25),
    'year_range': (2019, 2026),
    'student_number_range': (10000, 99999),
    'middle_name_probability': 0.90
}

# Gender Distribution for Realistic Patterns (course_keyword, female_probability)
GENDER_DISTRIBUTION = {
    'Nursing': 0.85,      # 85% female
    'Education': 0.75,    # 75% female
    'Psychology': 0.70,   # 70% female
    'Communication': 0.65, # 65% female
    'English': 0.60,      # 60% female
    'Accountancy': 0.55,  # 55% female
    'Business Administration': 0.52, # 52% female
    'Architecture': 0.40, # 40% female
    'Computer Science': 0.35, # 35% female
    'Information Technology': 0.30, # 30% female
    'Civil Engineering': 0.25, # 25% female
    'Electrical Engineering': 0.20, # 20% female
    'Mechanical Engineering': 0.15, # 15% female
    'Mathematics': 0.45,   # 45% female
    'Law': 0.55,          # 55% female
}

# Visualization Configuration
VISUALIZATION_CONFIG = {
    'output_directory': '/home/nytri/Projects/paul_esys/scripts/artifacts/',
    'dpi': 300,
    'figure_format': 'png',
    'style': 'seaborn-v0_8',
    'color_palette': 'husl'
}

# Visualization File Names
VISUALIZATION_FILES = {
    'regular_vs_irregular': 'regular_vs_irregular.png',
    'students_per_course_department': 'students_per_course_department.png',
    'gender_distribution': 'gender_distribution.png',
    'enrollment_analysis': 'enrollment_analysis.png',
    'year_level_distribution': 'year_level_distribution.png',
    'age_demographics': 'age_demographics.png'
}

# Plot Colors
PLOT_COLORS = {
    'regular': '#2ecc71',
    'irregular': '#e74c3c',
    'female': '#ff69b4',
    'male': '#4169e1',
    'year_levels': ['#ff6b6b', '#4ecdc4', '#45b7d1', '#96ceb4', '#feca57']
}

# Constants
DEFAULT_PASSWORD = "12345678"
BCRYPT_ROUNDS = 4
MIN_ROOM_CAPACITY = 40
CAPACITY_VARIATION = (-10, 20)
BUILDING_FLOORS = (1, 5)
STUDENT_AGE_RANGE = (17, 25)
STUDENT_YEAR_LEVEL_RANGE = (1, 5)
BACHELOR_MAX_YEAR = 5
SECTIONS_PER_SUBJECT = (3, 6)
SECTION_CAPACITY_RANGE = (25, 50)
PREREQUISITE_PROBABILITY = 0.7
PREREQUISITES_PER_SUBJECT = (1, 2)
SUBJECTS_WITH_PREREQUISITES = 50
SCHEDULES_PER_SECTION = (2, 4)
ENROLLMENTS_PER_STUDENT = (2, 6)
SUBJECTS_PER_ENROLLMENT = (4, 8)
UNITS_RANGE = (16, 24)
MIN_UNITS = 16
START_HOURS = [7, 8, 9, 10, 13, 14, 15, 16]
START_MINUTES = [0, 30]
DURATION_HOURS = [1, 2, 3]