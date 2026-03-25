#!/usr/bin/env python3
"""
Comprehensive Database Seeder for University Enrollment System
Generates realistic fake data for all tables in the enrollment system
"""

import random
from datetime import datetime, timedelta
from faker import Faker
import mysql.connector
from mysql.connector import Error
import argparse
import sys
import bcrypt
from tqdm import tqdm
from config import *

# Initialize Faker with Philippine locale
fake = Faker('en_PH')

class UniversityDatabaseSeeder:
    """Comprehensive database seeder for university enrollment system.
    
    Generates realistic fake data for all university tables including departments,
    courses, students, faculty, subjects, enrollments, and more.
    
    Attributes:
        host (str): Database host address
        database (str): Database name
        user (str): Database username
        password (str): Database password
        connection: MySQL database connection object
        departments (list): Storage for generated department records
        courses (list): Storage for generated course records
        rooms (list): Storage for generated room records
        users (list): Storage for generated user records
        faculty (list): Storage for generated faculty records
        students (list): Storage for generated student records
        subjects (list): Storage for generated subject records
        sections (list): Storage for generated section records
        curriculums (list): Storage for generated curriculum records
        enrollment_periods (list): Storage for generated enrollment period records
    """
    def __init__(self, host=None, database=None, user=None, password=None):
        # Use config defaults if not provided
        self.host = host or DATABASE_CONFIG['host']
        self.database = database or DATABASE_CONFIG['database']
        self.user = user or DATABASE_CONFIG['user']
        self.password = password or DATABASE_CONFIG['password']
        self.connection = None
        # Storage for generated IDs to maintain referential integrity
        self.departments = []
        self.courses = []
        self.rooms = []
        self.users = []
        self.faculty = []
        self.students = []
        self.subjects = []
        self.sections = []
        self.curriculums = []
        self.enrollment_periods = []
        
    def connect(self):
        """Establish database connection.
        
        Returns:
            bool: True if connection successful, False otherwise
        """
        try:
            self.connection = mysql.connector.connect(
                host=self.host,
                database=self.database,
                user=self.user,
                password=self.password
            )
            if self.connection.is_connected():
                print(f"Connected to MySQL database '{self.database}'")
                return True
        except Error as e:
            print(f"Error connecting to MySQL: {e}")
            return False
    
    def disconnect(self):
        """Close database connection.
        
        Cleans up the database connection and prints confirmation message.
        """
        if self.connection and self.connection.is_connected():
            self.connection.close()
            print("MySQL connection closed")
    
    def clear_tables(self):
        """Clear all tables in correct order to respect foreign key constraints.
        
        Deletes all data from tables in dependency order to avoid foreign key
        constraint violations. Prints confirmation for each cleared table.
        """
        print("Clearing existing data...")
        cursor = self.connection.cursor()
        
        # Order matters due to foreign key constraints
        tables_to_clear = [
            'student_enrolled_subjects',
            'enrollments_details',
            'enrollments',
            'schedules',
            'prerequisites',
            'sections',
            'subjects',
            'curriculum',
            'faculty',
            'students',
            'users',
            'rooms',
            'courses',
            'departments',
            'enrollment_period'
        ]
        
        for table in tables_to_clear:
            try:
                cursor.execute(f"DELETE FROM {table}")
                print(f"Cleared table: {table}")
            except Error as e:
                print(f"Error clearing {table}: {e}")
        
        self.connection.commit()
        cursor.close()
    
    def seed_departments(self, count=None):
        """Seed departments table with realistic academic departments.
        
        Args:
            count (int, optional): Number of departments to create. 
                Defaults to SEEDING_COUNTS['departments'].
                
        Creates college departments with names and descriptions based on
        DEPARTMENT_DATA configuration.
        """
        count = count or SEEDING_COUNTS['departments']
        print(f"Seeding {count} departments...")
        cursor = self.connection.cursor()
        
        for i, (name, description) in enumerate(tqdm(DEPARTMENT_DATA[:count], desc="Creating departments", unit="dept")):
            query = """
            INSERT INTO departments (department_name, description)
            VALUES (%s, %s)
            """
            cursor.execute(query, (name, description))
            self.departments.append({
                'id': cursor.lastrowid,
                'name': name,
                'description': description
            })
        
        self.connection.commit()
        cursor.close()
        print(f"Created {len(self.departments)} departments")
    
    def seed_courses(self, count=None):
        """Seed courses table with academic programs.
        
        Args:
            count (int, optional): Number of courses to create.
                Defaults to SEEDING_COUNTS['courses'].
                
        Creates degree programs linked to departments using COURSE_DATA
        configuration. Each course includes name, description, and department.
        """
        count = count or SEEDING_COUNTS['courses']
        print(f"Seeding {count} courses...")
        cursor = self.connection.cursor()
        
        for i, (name, description, dept_id) in enumerate(tqdm(COURSE_DATA[:count], desc="Creating courses", unit="course")):
            if dept_id < len(self.departments):
                query = """
                INSERT INTO courses (course_name, description, department_id)
                VALUES (%s, %s, %s)
                """
                cursor.execute(query, (name, description, self.departments[dept_id]['id']))
                self.courses.append({
                    'id': cursor.lastrowid,
                    'name': name,
                    'department_id': self.departments[dept_id]['id']
                })
        
        self.connection.commit()
        cursor.close()
        print(f"Created {len(self.courses)} courses")
    
    def seed_rooms(self, count=None):
        """Seed rooms table with various room types and capacities.
        
        Args:
            count (int, optional): Number of rooms to create.
                Defaults to SEEDING_COUNTS['rooms'].
                
        Generates rooms with different types (lecture halls, labs, etc.) and
        realistic capacity variations based on ROOM_TYPES configuration.
        """
        count = count or SEEDING_COUNTS['rooms']
        print(f"Seeding {count} rooms...")
        cursor = self.connection.cursor()
        
        for i in tqdm(range(count), desc="Creating rooms", unit="room"):
            room_type, base_capacity = random.choice(ROOM_TYPES)
            building = random.choice(BUILDING_NAMES)
            floor = random.randint(1, 5)
            room_number = f"{building[0]}{floor:02d}{i+1:03d}"
            
            # Add some variation to capacity
            capacity = base_capacity + random.randint(-10, 20)
            capacity = max(15, capacity)  # Minimum capacity
            
            query = """
            INSERT INTO rooms (room, capacity)
            VALUES (%s, %s)
            """
            cursor.execute(query, (room_number, capacity))
            self.rooms.append({
                'id': cursor.lastrowid,
                'room': room_number,
                'capacity': capacity
            })
        
        self.connection.commit()
        cursor.close()
        print(f"Created {len(self.rooms)} rooms")
    
    def seed_users(self, student_count=None, faculty_count=None, registrar_count=None):
        """Seed users table with students, faculty, and registrar accounts.
        
        Args:
            student_count (int, optional): Number of student users to create.
                Defaults to SEEDING_COUNTS['students'].
            faculty_count (int, optional): Number of faculty users to create.
                Defaults to SEEDING_COUNTS['faculty'].
            registrar_count (int, optional): Number of registrar users to create.
                Defaults to SEEDING_COUNTS['registrars'].
                
        Creates user accounts with fake emails and simulated password hashes
        for different user roles in the university system.
        """
        student_count = student_count or SEEDING_COUNTS['students']
        faculty_count = faculty_count or SEEDING_COUNTS['faculty']
        registrar_count = registrar_count or SEEDING_COUNTS['registrars']
        
        print(f"Seeding {student_count} students, {faculty_count} faculty, and {registrar_count} registrars...")
        cursor = self.connection.cursor()
        
        # Create student users
        for i in tqdm(range(student_count), desc="Creating student users", unit="user"):
            email = fake.unique.email()
            # Simple password hash simulation
            password = bcrypt.hashpw("12345678".encode('utf-8'), bcrypt.gensalt(rounds=4)).decode('utf-8')
            role = USER_ROLES['STUDENT']
            
            query = """
            INSERT INTO users (email, password, role)
            VALUES (%s, %s, %s)
            """
            cursor.execute(query, (email, password, role))
            self.users.append({
                'id': cursor.lastrowid,
                'email': email,
                'role': role,
                'type': 'student'
            })
        
        # Create faculty users
        for i in tqdm(range(faculty_count), desc="Creating faculty users", unit="user"):
            email = fake.unique.email()
            # Simple password hash simulation
            password = bcrypt.hashpw("12345678".encode('utf-8'), bcrypt.gensalt(rounds=4)).decode('utf-8')
            role = USER_ROLES['FACULTY']
            
            query = """
            INSERT INTO users (email, password, role)
            VALUES (%s, %s, %s)
            """
            cursor.execute(query, (email, password, role))
            self.users.append({
                'id': cursor.lastrowid,
                'email': email,
                'role': role,
                'type': 'faculty'
            })
        
        # Create registrar users
        for i in tqdm(range(registrar_count), desc="Creating registrar users", unit="user"):
            email = fake.unique.email()
            # Simple password hash simulation
            password = bcrypt.hashpw("12345678".encode('utf-8'), bcrypt.gensalt(rounds=4)).decode('utf-8')
            role = USER_ROLES['REGISTRAR']
            
            query = """
            INSERT INTO users (email, password, role)
            VALUES (%s, %s, %s)
            """
            cursor.execute(query, (email, password, role))
            self.users.append({
                'id': cursor.lastrowid,
                'email': email,
                'role': role,
                'type': 'registrar'
            })
        
        self.connection.commit()
        cursor.close()
        print(f"Created {len(self.users)} users")
    
    def seed_students(self):
        """Seed students table with realistic student data.
        
        Creates student records with:
        - Unique student IDs with year-based format
        - Personal information (names, birthdates)
        - Realistic age distribution (18-25)
        - Student status (REGULAR/IRREGULAR with 12% irregular probability)
        - Course assignments and year levels
        
        Uses STUDENT_DEMOGRAPHICS and STUDENT_STATUS_CONFIG for realistic data.
        """
        print("Seeding students...")
        cursor = self.connection.cursor()
        
        student_users = [u for u in self.users if u['type'] == 'student']
        
        for user in tqdm(student_users, desc="Creating student records", unit="student"):
            # Generate student ID
            year = random.randint(*STUDENT_DEMOGRAPHICS['year_range'])
            student_number = f"{year}-{random.randint(*STUDENT_DEMOGRAPHICS['student_number_range'])}"
            # Check if student number already exists
            while student_number in [s['student_id'] for s in self.students]:
                student_number = f"{year}-{random.randint(10000, 99999)}"
            
            first_name = fake.first_name()
            last_name = fake.last_name()
            middle_name = fake.first_name() if random.random() > STUDENT_DEMOGRAPHICS['middle_name_probability'] else None
            
            # Generate realistic birthdate
            age = random.randint(*STUDENT_DEMOGRAPHICS['age_range'])
            birthdate = datetime.now() - timedelta(days=age*365)
            
            # Make IRREGULAR students uncommon
            if random.random() < STUDENT_STATUS_CONFIG['irregular_probability']:
                student_status = STUDENT_STATUS_CONFIG['irregular_status']
            else:
                student_status = STUDENT_STATUS_CONFIG['regular_status']
            course = random.choice(self.courses)
            year_level = min(random.randint(1, 5), 4 if course['name'].startswith('Bachelor') else 5)
            
            query = """
            INSERT INTO students (student_id, user_id, first_name, last_name, middle_name, 
                                 birthdate, student_status, course_id, year_level)
            VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s)
            """
            cursor.execute(query, (student_number, user['id'], first_name, last_name, 
                                 middle_name, birthdate, student_status, course['id'], year_level))
            
            self.students.append({
                'student_id': student_number,
                'user_id': user['id'],
                'first_name': first_name,
                'last_name': last_name,
                'course_id': course['id'],
                'year_level': year_level
            })
        
        self.connection.commit()
        cursor.close()
        print(f"Created {len(self.students)} students")
    
    def seed_faculty(self):
        """Seed faculty table with instructor information.
        
        Creates faculty records linking user accounts to departments with
        realistic names and department assignments.
        """
        print("Seeding faculty...")
        cursor = self.connection.cursor()
        
        faculty_users = [u for u in self.users if u['type'] == 'faculty']
        
        for user in tqdm(faculty_users, desc="Creating faculty records", unit="faculty"):
            first_name = fake.first_name()
            last_name = fake.last_name()
            department = random.choice(self.departments)
            
            query = """
            INSERT INTO faculty (user_id, first_name, last_name, department_id)
            VALUES (%s, %s, %s, %s)
            """
            cursor.execute(query, (user['id'], first_name, last_name, department['id']))
            
            self.faculty.append({
                'id': cursor.lastrowid,
                'user_id': user['id'],
                'first_name': first_name,
                'last_name': last_name,
                'department_id': department['id']
            })
        
        self.connection.commit()
        cursor.close()
        print(f"Created {len(self.faculty)} faculty members")
    
    def seed_curriculum(self):
        """Seed curriculum table with semester-based curriculum entries.
        
        Creates curriculum records for each course spanning 4 years
        with 2 semesters per year (8 total semesters per course).
        """
        print("Seeding curriculum...")
        cursor = self.connection.cursor()
        
        for course in self.courses:
            # Create curriculum for each semester (1-8)
            for year in range(1, 5):
                for semester in [1, 2]:
                    curriculum_year = datetime.now() - timedelta(days=random.randint(365, 1825))
                    
                    query = """
                    INSERT INTO curriculum (semester, cur_year, course)
                    VALUES (%s, %s, %s)
                    """
                    cursor.execute(query, (f"Semester {semester}", curriculum_year, course['id']))
                    
                    self.curriculums.append({
                        'id': cursor.lastrowid,
                        'semester': f"Semester {semester}",
                        'course_id': course['id']
                    })
        
        self.connection.commit()
        cursor.close()
        print(f"Created {len(self.curriculums)} curriculum entries")
    
    def seed_subjects(self, count=None):
        """Seed subjects table with academic courses.
        
        Args:
            count (int, optional): Number of subjects to create.
                Defaults to SEEDING_COUNTS['subjects'].
                
        Creates subject records using SUBJECT_TEMPLATES configuration with
        realistic names, codes, units, and descriptions. Links to curriculum
        and departments.
        """
        count = count or SEEDING_COUNTS['subjects']
        print(f"Seeding {count} subjects...")
        cursor = self.connection.cursor()
        
        for i in tqdm(range(count), desc="Creating subjects", unit="subject"):
            template = random.choice(SUBJECT_TEMPLATES)
            subject_name = f"{template[0]} {random.randint(1, 4)}"
            subject_code = f"{template[1]}{random.randint(100, 999)}"
            units = template[2]
            description = template[3]
            
            curriculum = random.choice(self.curriculums) if self.curriculums else None
            department = random.choice(self.departments)
            
            query = """
            INSERT INTO subjects (subject_name, subject_code, units, description, curriculum_id, department_id)
            VALUES (%s, %s, %s, %s, %s, %s)
            """
            cursor.execute(query, (subject_name, subject_code, units, description, 
                                 curriculum['id'] if curriculum else None, department['id']))
            
            self.subjects.append({
                'id': cursor.lastrowid,
                'name': subject_name,
                'code': subject_code,
                'units': units,
                'department_id': department['id']
            })
        
        self.connection.commit()
        cursor.close()
        print(f"Created {len(self.subjects)} subjects")
    
    def seed_sections(self):
        """Seed sections table with class sections for each subject.
        
        Creates 2-4 sections per subject with unique section names,
        codes, and capacities. Sections are used for class scheduling
        and enrollment management.
        """
        print("Seeding sections...")
        cursor = self.connection.cursor()
        
        for subject in tqdm(self.subjects, desc="Creating sections", unit="subject"):
            num_sections = random.randint(2, 4)
            
            for i in range(num_sections):
                section_name = f"{subject['code']}-{chr(65 + i)}"  # A, B, C, D
                section_code = f"SEC{subject['id']}-{i+1}"
                capacity = random.randint(25, 50)
                
                query = """
                INSERT INTO sections (section_name, section_code, subject_id, capacity)
                VALUES (%s, %s, %s, %s)
                """
                cursor.execute(query, (section_name, section_code, subject['id'], capacity))
                
                self.sections.append({
                    'id': cursor.lastrowid,
                    'name': section_name,
                    'code': section_code,
                    'subject_id': subject['id'],
                    'capacity': capacity
                })
        
        self.connection.commit()
        cursor.close()
        print(f"Created {len(self.sections)} sections")
    
    def seed_prerequisites(self):
        """Seed prerequisites table with course prerequisite relationships.
        
        Creates prerequisite relationships for ~30% of subjects,
        with 1-2 prerequisites per subject. Ensures no circular
        dependencies by avoiding self-references.
        """
        print("Seeding prerequisites...")
        cursor = self.connection.cursor()
        
        for i, subject in enumerate(self.subjects[:50]):  # Limit to first 50 subjects
            if random.random() > 0.7:  # 30% chance of having prerequisites
                num_prereqs = random.randint(1, 2)
                available_prereqs = [s for s in self.subjects if s['id'] != subject['id']]
                
                for _ in range(num_prereqs):
                    if available_prereqs:
                        prereq = random.choice(available_prereqs)
                        available_prereqs.remove(prereq)
                        
                        query = """
                        INSERT INTO prerequisites (pre_subject_id, subject_id)
                        VALUES (%s, %s)
                        """
                        cursor.execute(query, (prereq['id'], subject['id']))
        
        self.connection.commit()
        cursor.close()
        print("Created prerequisites")
    
    def seed_enrollment_periods(self, count=None):
        """Seed enrollment_period table with academic periods.
        
        Args:
            count (int, optional): Number of enrollment periods to create.
                Defaults to SEEDING_COUNTS['enrollment_periods'].
                
        Creates enrollment periods for different academic years and semesters
        with start and end dates for enrollment periods.
        """
        count = count or SEEDING_COUNTS['enrollment_periods']
        print(f"Seeding {count} enrollment periods...")
        cursor = self.connection.cursor()
        
        for i in range(count):
            year = 2021 + i
            for semester in [1, 2]:
                # Generate realistic enrollment period dates
                if semester == 1:
                    start_date = datetime(year - 1, 10, 1)  # October previous year
                    end_date = datetime(year - 1, 11, 30)   # November previous year
                else:
                    start_date = datetime(year, 3, 1)  # March
                    end_date = datetime(year, 4, 30)   # April
                
                query = """
                INSERT INTO enrollment_period (school_year, semester, start_date, end_date)
                VALUES (%s, %s, %s, %s)
                """
                cursor.execute(query, (f"{year-1}-{year}", semester, start_date, end_date))
                
                self.enrollment_periods.append({
                    'id': cursor.lastrowid,
                    'school_year': f"{year-1}-{year}",
                    'semester': semester,
                    'start_date': start_date,
                    'end_date': end_date
                })
        
        self.connection.commit()
        cursor.close()
        print(f"Created {len(self.enrollment_periods)} enrollment periods")
    
    def seed_schedules(self):
        """Seed schedules table with class schedules.
        
        Creates schedules for sections with:
        - Random days of the week
        - Realistic time slots
        - Room assignments
        - Faculty assignments
        """
        print("Seeding schedules...")
        cursor = self.connection.cursor()
        
        for section in self.sections:
            # Create 1-2 schedules per section
            num_schedules = random.randint(1, 2)
            
            for _ in range(num_schedules):
                day = random.choice(DAYS_OF_WEEK)
                
                # Generate realistic time slots
                start_hour = random.choice([7, 8, 9, 10, 13, 14, 15, 16])
                start_minute = random.choice([0, 30])
                end_hour = start_hour + random.choice([1, 2, 3])
                end_minute = start_minute
                
                start_time = f"{start_hour:02d}:{start_minute:02d}"
                end_time = f"{end_hour:02d}:{end_minute:02d}"
                
                room = random.choice(self.rooms)
                faculty = random.choice(self.faculty)
                
                query = """
                INSERT INTO schedules (section_id, room_id, faculty_id, day, start_time, end_time)
                VALUES (%s, %s, %s, %s, %s, %s)
                """
                cursor.execute(query, (section['id'], room['id'], faculty['id'], day, start_time, end_time))
        
        self.connection.commit()
        cursor.close()
        print("Created schedules")
    
    def seed_enrollments(self):
        """Seed enrollments and enrollment details tables.
        
        Creates enrollment records for students with:
        - Multiple enrollment records per student (1-4)
        - Various enrollment statuses
        - Realistic unit loads and submission dates
        - Detailed subject selections for approved/enrolled enrollments
        
        Also populates student_enrolled_subjects for final enrollments.
        """
        print("Seeding enrollments...")
        cursor = self.connection.cursor()
        
        statuses = ENROLLMENT_STATUSES
        
        for student in self.students:
            # Create 1-4 enrollments per student
            num_enrollments = random.randint(1, 4)
            
            for _ in range(num_enrollments):
                school_year = random.choice([f"{y}-{y+1}" for y in range(2021, 2025)])
                semester = random.randint(1, 2)
                status = random.choice(statuses)
                
                max_units = random.uniform(15, 24)
                total_units = random.uniform(12, max_units)
                
                submitted_at = fake.date_time_between(start_date='-2y', end_date='now')
                
                query = """
                INSERT INTO enrollments (student_id, school_year, semester, status, max_units, total_units, submitted_at)
                VALUES (%s, %s, %s, %s, %s, %s, %s)
                """
                cursor.execute(query, (student['student_id'], school_year, semester, status, 
                                     max_units, total_units, submitted_at))
                
                enrollment_id = cursor.lastrowid
                
                # Add enrollment details
                if status in ['APPROVED', 'ENROLLED']:
                    num_subjects = random.randint(3, 7)
                    available_sections = random.sample(self.sections, min(num_subjects, len(self.sections)))
                    
                    for section in available_sections:
                        detail_status = random.choice(ENROLLMENT_DETAIL_STATUSES)
                        units = next((s['units'] for s in self.subjects if s['id'] == section['subject_id']), 3)
                        
                        query = """
                        INSERT INTO enrollments_details (enrollment_id, section_id, subject_id, units, status)
                        VALUES (%s, %s, %s, %s, %s)
                        """
                        cursor.execute(query, (enrollment_id, section['id'], section['subject_id'], units, detail_status))
                        
                        # Add to student_enrolled_subjects if enrolled
                        if status == 'ENROLLED' and detail_status == 'SELECTED':
                            query = """
                            INSERT IGNORE INTO student_enrolled_subjects (student_id, subject_id)
                            VALUES (%s, %s)
                            """
                            cursor.execute(query, (student['student_id'], section['subject_id']))
        
        self.connection.commit()
        cursor.close()
        print("Created enrollments and enrollment details")
    
    def seed_all(self, clear_existing=True):
        """Seed all tables with comprehensive data in correct dependency order.
        
        Args:
            clear_existing (bool): Whether to clear existing data before seeding.
                Defaults to True.
                
        Returns:
            bool: True if seeding successful, False otherwise.
                
        Executes all seeding methods in the correct order to respect
        foreign key constraints. Prints summary statistics of created records.
        """
        if not self.connect():
            return False
        
        try:
            if clear_existing:
                self.clear_tables()
            
            # Seed in order respecting foreign key constraints
            self.seed_departments()
            self.seed_courses()
            self.seed_rooms()
            self.seed_users(student_count=SEEDING_COUNTS['students'], 
                       faculty_count=SEEDING_COUNTS['faculty'], 
                       registrar_count=SEEDING_COUNTS['registrars'])
            self.seed_students()
            self.seed_faculty()
            self.seed_curriculum()
            self.seed_subjects(count=SEEDING_COUNTS['subjects'])
            self.seed_sections()
            self.seed_enrollment_periods(count=SEEDING_COUNTS['enrollment_periods'])
            self.seed_schedules()
            self.seed_prerequisites()
            self.seed_enrollments()
            
            print("\n=== Database Seeding Complete ===")
            print(f"Departments: {len(self.departments)}")
            print(f"Courses: {len(self.courses)}")
            print(f"Rooms: {len(self.rooms)}")
            print(f"Users: {len(self.users)}")
            print(f"Students: {len(self.students)}")
            print(f"Faculty: {len(self.faculty)}")
            print(f"Subjects: {len(self.subjects)}")
            print(f"Sections: {len(self.sections)}")
            print(f"Curriculum entries: {len(self.curriculums)}")
            print(f"Enrollment periods: {len(self.enrollment_periods)}")
            
            return True
            
        except Exception as e:
            print(f"Error during seeding: {e}")
            return False
        finally:
            self.disconnect()

def main():
    """Main function to run the database seeder.
    
    Parses command line arguments for database connection parameters,
    initializes the seeder, and runs the seeding process.
    
    Command line arguments:
        --host: Database host (default from config)
        --database: Database name (default from config)
        --user: Database user (default from config)
        --password: Database password (default from config)
        --no-clear: Skip clearing existing data
    """
    parser = argparse.ArgumentParser(description='University Database Seeder')
    parser.add_argument('--host', default=DATABASE_CONFIG['host'], help='Database host')
    parser.add_argument('--database', default=DATABASE_CONFIG['database'], help='Database name')
    parser.add_argument('--user', default=DATABASE_CONFIG['user'], help='Database user')
    parser.add_argument('--password', default=DATABASE_CONFIG['password'], help='Database password')
    parser.add_argument('--no-clear', action='store_true', help='Do not clear existing data')
    
    args = parser.parse_args()
    
    seeder = UniversityDatabaseSeeder(
        host=args.host,
        database=args.database,
        user=args.user,
        password=args.password
    )
    
    success = seeder.seed_all(clear_existing=not args.no_clear)
    
    if success:
        print("Database seeding completed successfully!")
        sys.exit(0)
    else:
        print("Database seeding failed!")
        sys.exit(1)

if __name__ == "__main__":
    main()
