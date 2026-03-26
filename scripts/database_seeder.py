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
import jaydebeapi
import jpype

fake = Faker('en_PH')

class UniversityDatabaseSeeder:
    """Comprehensive database seeder for university enrollment system.
    
    Generates realistic fake data for all university tables including departments,
    courses, students, faculty, subjects, enrollments, and more.
    
    Attributes:
        db_type (str): Database type ('mysql' or 'derby')
        host (str): Database host address
        database (str): Database name
        user (str): Database username
        password (str): Database password
        connection: Database connection object (MySQL or Derby)
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
    def __init__(self, db_type='mysql', host=None, database=None, user=None, password=None):
        self.db_type = db_type.lower()
        if self.db_type == 'derby':
            self.host = host or DERBY_CONFIG['host']
            self.database = database or DERBY_CONFIG['database']
            self.user = user or DERBY_CONFIG['user']
            self.password = password or DERBY_CONFIG['password']
        else:
            self.host = host or DATABASE_CONFIG['host']
            self.database = database or DATABASE_CONFIG['database']
            self.user = user or DATABASE_CONFIG['user']
            self.password = password or DATABASE_CONFIG['password']
        self.connection = None
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
            if self.db_type == 'mysql':
                self.connection = mysql.connector.connect(
                    host=self.host,
                    database=self.database,
                    user=self.user,
                    password=self.password
                )
                if self.connection.is_connected():
                    print(f"Connected to MySQL database '{self.database}'")
                    return True
            elif self.db_type == 'derby':
                derby_jar_path = "derbyclient-10.17.1.0.jar"
                derby_shared_jar_path = "derbyshared-10.17.1.0.jar"
                
                if not jpype.isJVMStarted():
                    jpype.startJVM(classpath=[derby_jar_path, derby_shared_jar_path])
                
                connection_string = f"jdbc:derby://localhost:1527/{self.database};create=true"
                driver_class = 'org.apache.derby.client.ClientAutoloadedDriver'
                
                try:
                    credentials = [self.user] if not self.password else [self.user, self.password]
                    self.connection = jaydebeapi.connect(
                        driver_class,
                        connection_string,
                        credentials
                    )
                    print(f"Connected to Derby database '{self.database}' using network server")
                    return True
                except Exception as network_error:
                    print(f"Network server connection failed: {network_error}")
                    raise network_error
            else:
                print(f"Unsupported database type: {self.db_type}")
                return False
                
        except Exception as e:
            print(f"Error connecting to {self.db_type.upper()}: {e}")
            return False
    
    def disconnect(self):
        """Close database connection.
        
        Cleans up the database connection and prints confirmation message.
        """
        if self.connection:
            try:
                if self.db_type == 'mysql' and self.connection.is_connected():
                    self.connection.close()
                    print("MySQL connection closed")
                elif self.db_type == 'derby':
                    self.connection.close()
                    print("Derby connection closed")
            except Exception as e:
                print(f"Error closing connection: {e}")
    
    def _get_table_prefix(self):
        """Get table prefix based on database type."""
        return 'APP.' if self.db_type == 'derby' else ''
    
    def _get_param_placeholder(self):
        """Get parameter placeholder based on database type."""
        return '?' if self.db_type == 'derby' else '%s'
    
    def _format_datetime(self, dt):
        """Format datetime for database compatibility."""
        return dt.strftime('%Y-%m-%d') if self.db_type == 'derby' else dt
    
    def _format_timestamp(self, ts):
        """Format timestamp for database compatibility."""
        return ts.strftime('%Y-%m-%d %H:%M:%S') if self.db_type == 'derby' else ts
    
    def _create_table_if_not_exists(self, table_name, create_sql):
        """Create table if it doesn't exist, handling database-specific syntax."""
        if self.db_type != 'derby':
            return
        
        cursor = self.connection.cursor()
        try:
            prefixed_name = f"{self._get_table_prefix()}{table_name}"
            cursor.execute(create_sql.replace('TABLE_NAME', prefixed_name))
            print(f"Created {table_name} table")
        except Exception as e:
            print(f"{table_name.title()} table creation error (may already exist): {e}")
        finally:
            cursor.close()
    
    def _execute_insert(self, table_name, columns, values, return_id=True):
        """Execute insert query with database-specific parameter handling."""
        cursor = self.connection.cursor()
        try:
            table_prefix = self._get_table_prefix()
            param_placeholder = self._get_param_placeholder()
            
            column_list = ', '.join(columns)
            param_list = ', '.join([param_placeholder] * len(values))
            
            query = f"INSERT INTO {table_prefix}{table_name} ({column_list}) VALUES ({param_list})"
            cursor.execute(query, values)
            
            if return_id:
                return self.get_last_insert_id(cursor, table_name)
            return None
        finally:
            cursor.close()
    
    def get_last_insert_id(self, cursor, table_name):
        """Get the last inserted ID based on database type and table.
        
        Args:
            cursor: Database cursor object
            table_name: Name of the table to query
            
        Returns:
            int: The last inserted ID
        """
        if self.db_type == 'mysql':
            return cursor.lastrowid
        else:  # Derby
            try:
                cursor.execute(f"SELECT MAX(id) FROM {table_name}")
                result = cursor.fetchone()
                if result and result[0] is not None:
                    return result[0]
                else:
                    return 1
            except:
                return 1
    
    def clear_tables(self):
        """Clear all tables in correct order to respect foreign key constraints.
        
        Deletes all data from tables in dependency order to avoid foreign key
        constraint violations. Prints confirmation for each cleared table.
        """
        print("Clearing existing data...")
        cursor = self.connection.cursor()
        
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
                if self.db_type == 'derby':
                    try:
                        cursor.execute(f"DROP TABLE APP.{table}")
                        print(f"Dropped table: {table}")
                    except Exception as drop_error:
                        if "does not exist" in str(drop_error):
                            print(f"Table {table} does not exist, skipping")
                        else:
                            print(f"Error dropping {table}: {drop_error}")
                else:
                    cursor.execute(f"DELETE FROM {table}")
                    print(f"Cleared table: {table}")
            except Exception as e:
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
        
        # Create table for Derby
        departments_sql = """
            CREATE TABLE TABLE_NAME (
                id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                department_name VARCHAR(255) NOT NULL,
                description CLOB
            )
        """
        self._create_table_if_not_exists('departments', departments_sql)
        
        cursor = self.connection.cursor()
        try:
            for name, description in tqdm(DEPARTMENT_DATA[:count], desc="Creating departments", unit="dept"):
                last_id = self._execute_insert('departments', 
                                            ['department_name', 'description'], 
                                            [name, description])
                
                self.departments.append({
                    'id': last_id,
                    'name': name,
                    'description': description
                })
            
            self.connection.commit()
        finally:
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
        
        # Create table for Derby
        courses_sql = """
            CREATE TABLE TABLE_NAME (
                id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                course_name VARCHAR(255) NOT NULL,
                description CLOB,
                department_id INTEGER,
                FOREIGN KEY (department_id) REFERENCES TABLE_NAME(id)
            )
        """
        self._create_table_if_not_exists('courses', courses_sql)
        
        cursor = self.connection.cursor()
        try:
            for name, description, dept_id in tqdm(COURSE_DATA[:count], desc="Creating courses", unit="course"):
                if dept_id < len(self.departments):
                    last_id = self._execute_insert('courses',
                                                ['course_name', 'description', 'department_id'],
                                                [name, description, self.departments[dept_id]['id']])
                    
                    self.courses.append({
                        'id': last_id,
                        'name': name,
                        'department_id': self.departments[dept_id]['id']
                    })
            
            self.connection.commit()
        finally:
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
        
        # Create table for Derby
        rooms_sql = """
            CREATE TABLE TABLE_NAME (
                id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                room VARCHAR(50) NOT NULL,
                capacity INTEGER
            )
        """
        self._create_table_if_not_exists('rooms', rooms_sql)
        
        cursor = self.connection.cursor()
        try:
            for i in tqdm(range(count), desc="Creating rooms", unit="room"):
                room_type, base_capacity = random.choice(ROOM_TYPES)
                building = random.choice(BUILDING_NAMES)
                floor = random.randint(*BUILDING_FLOORS)
                room_number = f"{building[0]}{floor:02d}{i+1:03d}"
                
                capacity = max(MIN_ROOM_CAPACITY, base_capacity + random.randint(*CAPACITY_VARIATION))
                
                last_id = self._execute_insert('rooms', ['room', 'capacity'], [room_number, capacity])
                
                self.rooms.append({
                    'id': last_id,
                    'room': room_number,
                    'capacity': capacity
                })
            
            self.connection.commit()
        finally:
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
        
        # Create table for Derby
        users_sql = """
            CREATE TABLE TABLE_NAME (
                id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                email VARCHAR(255) NOT NULL UNIQUE,
                password VARCHAR(255) NOT NULL,
                role VARCHAR(50) NOT NULL
            )
        """
        self._create_table_if_not_exists('users', users_sql)
        
        cursor = self.connection.cursor()
        try:
            # Helper function to create users
            def create_users(count, role, user_type, desc):
                for _ in tqdm(range(count), desc=f"Creating {desc} users", unit="user"):
                    email = fake.unique.email()
                    password = bcrypt.hashpw(DEFAULT_PASSWORD.encode('utf-8'), bcrypt.gensalt(rounds=BCRYPT_ROUNDS)).decode('utf-8')
                    
                    last_id = self._execute_insert('users', ['email', 'password', 'role'], [email, password, role])
                    
                    self.users.append({
                        'id': last_id,
                        'email': email,
                        'role': role,
                        'type': user_type
                    })
            
            # Create different user types
            create_users(student_count, USER_ROLES['STUDENT'], 'student', 'student')
            create_users(faculty_count, USER_ROLES['FACULTY'], 'faculty', 'faculty')
            create_users(registrar_count, USER_ROLES['REGISTRAR'], 'registrar', 'registrar')
            
            self.connection.commit()
        finally:
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
        
        # Create table for Derby
        students_sql = """
            CREATE TABLE TABLE_NAME (
                id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                student_id VARCHAR(50) NOT NULL UNIQUE,
                user_id INTEGER NOT NULL,
                first_name VARCHAR(255) NOT NULL,
                last_name VARCHAR(255) NOT NULL,
                middle_name VARCHAR(255),
                birthdate DATE,
                student_status VARCHAR(50),
                course_id INTEGER,
                year_level INTEGER,
                FOREIGN KEY (user_id) REFERENCES TABLE_NAME(id),
                FOREIGN KEY (course_id) REFERENCES TABLE_NAME(id)
            )
        """
        self._create_table_if_not_exists('students', students_sql)
        
        cursor = self.connection.cursor()
        try:
            student_users = [u for u in self.users if u['type'] == 'student']
            
            for user in tqdm(student_users, desc="Creating student records", unit="student"):
                year = random.randint(*STUDENT_DEMOGRAPHICS['year_range'])
                student_number = f"{year}-{random.randint(*STUDENT_DEMOGRAPHICS['student_number_range'])}"
                while student_number in [s['student_id'] for s in self.students]:
                    student_number = f"{year}-{random.randint(10000, 99999)}"
                
                first_name = fake.first_name()
                last_name = fake.last_name()
                middle_name = fake.first_name() if random.random() > STUDENT_DEMOGRAPHICS['middle_name_probability'] else None
                
                age = random.randint(*STUDENT_AGE_RANGE)
                birthdate = datetime.now() - timedelta(days=age*365)
                birthdate_str = self._format_datetime(birthdate)
                
                student_status = (STUDENT_STATUS_CONFIG['irregular_status'] 
                                if random.random() < STUDENT_STATUS_CONFIG['irregular_probability']
                                else STUDENT_STATUS_CONFIG['regular_status'])
                
                course = random.choice(self.courses)
                year_level = min(random.randint(*STUDENT_YEAR_LEVEL_RANGE), 
                                BACHELOR_MAX_YEAR if course['name'].startswith('Bachelor') else 5)
                
                columns = ['student_id', 'user_id', 'first_name', 'last_name', 'middle_name',
                          'birthdate', 'student_status', 'course_id', 'year_level']
                values = [student_number, user['id'], first_name, last_name, middle_name,
                         birthdate_str, student_status, course['id'], year_level]
                
                last_id = self._execute_insert('students', columns, values)
                
                self.students.append({
                    'student_id': student_number,
                    'user_id': user['id'],
                    'first_name': first_name,
                    'last_name': last_name,
                    'course_id': course['id'],
                    'year_level': year_level
                })
            
            self.connection.commit()
        finally:
            cursor.close()
        
        print(f"Created {len(self.students)} students")
    
    def seed_faculty(self):
        """Seed faculty table with instructor information.
        
        Creates faculty records linking user accounts to departments with
        realistic names and department assignments.
        """
        print("Seeding faculty...")
        
        # Create table for Derby
        faculty_sql = """
            CREATE TABLE TABLE_NAME (
                id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                user_id INTEGER NOT NULL,
                first_name VARCHAR(255) NOT NULL,
                last_name VARCHAR(255) NOT NULL,
                department_id INTEGER,
                FOREIGN KEY (user_id) REFERENCES TABLE_NAME(id),
                FOREIGN KEY (department_id) REFERENCES TABLE_NAME(id)
            )
        """
        self._create_table_if_not_exists('faculty', faculty_sql)
        
        cursor = self.connection.cursor()
        try:
            faculty_users = [u for u in self.users if u['type'] == 'faculty']
            
            for user in tqdm(faculty_users, desc="Creating faculty records", unit="faculty"):
                first_name = fake.first_name()
                last_name = fake.last_name()
                department = random.choice(self.departments)
                
                last_id = self._execute_insert('faculty',
                                            ['user_id', 'first_name', 'last_name', 'department_id'],
                                            [user['id'], first_name, last_name, department['id']])
                
                self.faculty.append({
                    'id': last_id,
                    'user_id': user['id'],
                    'first_name': first_name,
                    'last_name': last_name,
                    'department_id': department['id']
                })
            
            self.connection.commit()
        finally:
            cursor.close()
        
        print(f"Created {len(self.faculty)} faculty members")
    
    def seed_curriculum(self):
        """Seed curriculum table with semester-based curriculum entries.
        
        Creates curriculum records for each course spanning 4 years
        with 2 semesters per year (8 total semesters per course).
        """
        print("Seeding curriculum...")
        cursor = self.connection.cursor()
        
        if self.db_type == 'derby':
            try:
                cursor.execute(f"""
                    CREATE TABLE APP.curriculum (
                        id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                        semester VARCHAR(255),
                        cur_year DATE,
                        course INTEGER,
                        FOREIGN KEY (course) REFERENCES APP.courses(id)
                    )
                """)
                print("Created curriculum table")
            except Exception as e:
                print(f"Curriculum table creation error (may already exist): {e}")
        
        for course in self.courses:
            for year in range(1, 5):
                for semester in [1, 2]:
                    curriculum_year = datetime.now() - timedelta(days=random.randint(365, 1825))
                    curriculum_year_str = curriculum_year.strftime('%Y-%m-%d') if self.db_type == 'derby' else curriculum_year
                    
                    if self.db_type == 'derby':
                        query = """
                        INSERT INTO APP.curriculum (semester, cur_year, course)
                        VALUES (?, ?, ?)
                        """
                        cursor.execute(query, (f"Semester {semester}", curriculum_year_str, course['id']))
                    else:
                        query = """
                        INSERT INTO curriculum (semester, cur_year, course)
                        VALUES (%s, %s, %s)
                        """
                        cursor.execute(query, (f"Semester {semester}", curriculum_year, course['id']))
                    
                    last_id = self.get_last_insert_id(cursor, 'curriculum')
                    
                    self.curriculums.append({
                        'id': last_id,
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
        
        if self.db_type == 'derby':
            try:
                cursor.execute(f"""
                    CREATE TABLE APP.subjects (
                        id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                        subject_name VARCHAR(255) NOT NULL,
                        subject_code VARCHAR(50) NOT NULL,
                        units INTEGER,
                        description CLOB,
                        curriculum_id INTEGER,
                        department_id INTEGER,
                        FOREIGN KEY (curriculum_id) REFERENCES APP.curriculum(id),
                        FOREIGN KEY (department_id) REFERENCES APP.departments(id)
                    )
                """)
                print("Created subjects table")
            except Exception as e:
                print(f"Subjects table creation error (may already exist): {e}")
        
        for i in tqdm(range(count), desc="Creating subjects", unit="subject"):
            template = random.choice(SUBJECT_TEMPLATES)
            subject_name = f"{template[0]} {random.randint(1, 4)}"
            subject_code = f"{template[1]}{random.randint(100, 999)}"
            units = template[2]
            description = template[3]
            
            curriculum = random.choice(self.curriculums) if self.curriculums else None
            department = random.choice(self.departments)
            
            if self.db_type == 'derby':
                query = """
                INSERT INTO APP.subjects (subject_name, subject_code, units, description, curriculum_id, department_id)
                VALUES (?, ?, ?, ?, ?, ?)
                """
                cursor.execute(query, (subject_name, subject_code, units, description, 
                                     curriculum['id'] if curriculum else None, department['id']))
            else:
                query = """
                INSERT INTO subjects (subject_name, subject_code, units, description, curriculum_id, department_id)
                VALUES (%s, %s, %s, %s, %s, %s)
                """
                cursor.execute(query, (subject_name, subject_code, units, description, 
                                     curriculum['id'] if curriculum else None, department['id']))
            
            last_id = self.get_last_insert_id(cursor, 'subjects')
            
            self.subjects.append({
                'id': last_id,
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
        
        if self.db_type == 'derby':
            try:
                cursor.execute(f"""
                    CREATE TABLE APP.sections (
                        id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                        section_name VARCHAR(255) NOT NULL,
                        section_code VARCHAR(50) NOT NULL,
                        subject_id INTEGER,
                        capacity INTEGER,
                        FOREIGN KEY (subject_id) REFERENCES APP.subjects(id)
                    )
                """)
                print("Created sections table")
            except Exception as e:
                print(f"Sections table creation error (may already exist): {e}")
        
        for subject in tqdm(self.subjects, desc="Creating sections", unit="subject"):
            num_sections = random.randint(2, 4)
            
            for i in range(num_sections):
                section_name = f"{subject['code']}-{chr(65 + i)}"
                section_code = f"SEC{subject['id']}-{i+1}"
                capacity = random.randint(25, 50)
                
                if self.db_type == 'derby':
                    query = """
                    INSERT INTO APP.sections (section_name, section_code, subject_id, capacity)
                    VALUES (?, ?, ?, ?)
                    """
                    cursor.execute(query, (section_name, section_code, subject['id'], capacity))
                else:
                    query = """
                    INSERT INTO sections (section_name, section_code, subject_id, capacity)
                    VALUES (%s, %s, %s, %s)
                    """
                    cursor.execute(query, (section_name, section_code, subject['id'], capacity))
                
                last_id = self.get_last_insert_id(cursor, 'sections')
                
                self.sections.append({
                    'id': last_id,
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
        
        if self.db_type == 'derby':
            try:
                cursor.execute(f"""
                    CREATE TABLE APP.prerequisites (
                        id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                        pre_subject_id INTEGER,
                        subject_id INTEGER,
                        FOREIGN KEY (pre_subject_id) REFERENCES APP.subjects(id),
                        FOREIGN KEY (subject_id) REFERENCES APP.subjects(id)
                    )
                """)
                print("Created prerequisites table")
            except Exception as e:
                print(f"Prerequisites table creation error (may already exist): {e}")
        
        for i, subject in enumerate(self.subjects[:50]):
            if random.random() > 0.7:
                num_prereqs = random.randint(1, 2)
                available_prereqs = [s for s in self.subjects if s['id'] != subject['id']]
                
                for _ in range(num_prereqs):
                    if available_prereqs:
                        prereq = random.choice(available_prereqs)
                        available_prereqs.remove(prereq)
                        
                        if self.db_type == 'derby':
                            query = """
                            INSERT INTO APP.prerequisites (pre_subject_id, subject_id)
                            VALUES (?, ?)
                            """
                            cursor.execute(query, (prereq['id'], subject['id']))
                        else:
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
        
        if self.db_type == 'derby':
            try:
                cursor.execute(f"""
                    CREATE TABLE APP.enrollment_period (
                        id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                        school_year VARCHAR(50),
                        semester INTEGER,
                        start_date DATE,
                        end_date DATE
                    )
                """)
                print("Created enrollment_period table")
            except Exception as e:
                print(f"Enrollment_period table creation error (may already exist): {e}")
        
        for i in range(count):
            year = 2021 + i
            for semester in [1, 2]:
                if semester == 1:
                    start_date = datetime(year - 1, 10, 1)
                    end_date = datetime(year - 1, 11, 30)
                else:
                    start_date = datetime(year, 3, 1)
                    end_date = datetime(year, 4, 30)
                
                start_date_str = start_date.strftime('%Y-%m-%d') if self.db_type == 'derby' else start_date
                end_date_str = end_date.strftime('%Y-%m-%d') if self.db_type == 'derby' else end_date
                
                if self.db_type == 'derby':
                    query = """
                    INSERT INTO APP.enrollment_period (school_year, semester, start_date, end_date)
                    VALUES (?, ?, ?, ?)
                    """
                    cursor.execute(query, (f"{year-1}-{year}", semester, start_date_str, end_date_str))
                else:
                    query = """
                    INSERT INTO enrollment_period (school_year, semester, start_date, end_date)
                    VALUES (%s, %s, %s, %s)
                    """
                    cursor.execute(query, (f"{year-1}-{year}", semester, start_date, end_date))
                
                last_id = self.get_last_insert_id(cursor, 'enrollment_period')
                
                self.enrollment_periods.append({
                    'id': last_id,
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
        
        if self.db_type == 'derby':
            try:
                cursor.execute(f"""
                    CREATE TABLE APP.schedules (
                        id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                        section_id INTEGER,
                        room_id INTEGER,
                        faculty_id INTEGER,
                        day VARCHAR(10),
                        start_time VARCHAR(10),
                        end_time VARCHAR(10),
                        FOREIGN KEY (section_id) REFERENCES APP.sections(id),
                        FOREIGN KEY (room_id) REFERENCES APP.rooms(id),
                        FOREIGN KEY (faculty_id) REFERENCES APP.faculty(id)
                    )
                """)
                print("Created schedules table")
            except Exception as e:
                print(f"Schedules table creation error (may already exist): {e}")
        
        for section in self.sections:
            num_schedules = random.randint(1, 2)
            
            for _ in range(num_schedules):
                day = random.choice(DAYS_OF_WEEK)
                
                start_hour = random.choice([7, 8, 9, 10, 13, 14, 15, 16])
                start_minute = random.choice([0, 30])
                end_hour = start_hour + random.choice([1, 2, 3])
                end_minute = start_minute
                
                start_time = f"{start_hour:02d}:{start_minute:02d}"
                end_time = f"{end_hour:02d}:{end_minute:02d}"
                
                room = random.choice(self.rooms)
                faculty = random.choice(self.faculty)
                
                if self.db_type == 'derby':
                    query = """
                    INSERT INTO APP.schedules (section_id, room_id, faculty_id, day, start_time, end_time)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """
                    cursor.execute(query, (section['id'], room['id'], faculty['id'], day, start_time, end_time))
                else:
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
        
        if self.db_type == 'derby':
            try:
                cursor.execute(f"""
                    CREATE TABLE APP.enrollments (
                        id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                        student_id VARCHAR(50),
                        school_year VARCHAR(50),
                        semester INTEGER,
                        status VARCHAR(50),
                        max_units DECIMAL(5,2),
                        total_units DECIMAL(5,2),
                        submitted_at TIMESTAMP
                    )
                """)
                print("Created enrollments table")
            except Exception as e:
                print(f"Enrollments table creation error (may already exist): {e}")
            
            try:
                cursor.execute(f"""
                    CREATE TABLE APP.enrollments_details (
                        id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                        enrollment_id INTEGER,
                        section_id INTEGER,
                        subject_id INTEGER,
                        units DECIMAL(3,1),
                        status VARCHAR(50),
                        FOREIGN KEY (enrollment_id) REFERENCES APP.enrollments(id),
                        FOREIGN KEY (section_id) REFERENCES APP.sections(id),
                        FOREIGN KEY (subject_id) REFERENCES APP.subjects(id)
                    )
                """)
                print("Created enrollments_details table")
            except Exception as e:
                print(f"Enrollments_details table creation error (may already exist): {e}")
            
            try:
                cursor.execute(f"""
                    CREATE TABLE APP.student_enrolled_subjects (
                        student_id VARCHAR(50),
                        subject_id INTEGER,
                        PRIMARY KEY (student_id, subject_id),
                        FOREIGN KEY (subject_id) REFERENCES APP.subjects(id)
                    )
                """)
                print("Created student_enrolled_subjects table")
            except Exception as e:
                print(f"Student_enrolled_subjects table creation error (may already exist): {e}")
        
        statuses = ENROLLMENT_STATUSES
        
        for student in self.students:
            num_enrollments = random.randint(1, 4)
            
            for _ in range(num_enrollments):
                school_year = random.choice([f"{y}-{y+1}" for y in range(2021, 2025)])
                semester = random.randint(1, 2)
                status = random.choice(statuses)
                
                max_units = random.uniform(15, 24)
                total_units = random.uniform(12, max_units)
                
                submitted_at = fake.date_time_between(start_date='-2y', end_date='now')
                submitted_at_str = submitted_at.strftime('%Y-%m-%d %H:%M:%S') if self.db_type == 'derby' else submitted_at
                
                if self.db_type == 'derby':
                    query = """
                    INSERT INTO APP.enrollments (student_id, school_year, semester, status, max_units, total_units, submitted_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """
                    cursor.execute(query, (student['student_id'], school_year, semester, status, 
                                         max_units, total_units, submitted_at_str))
                else:
                    query = """
                    INSERT INTO enrollments (student_id, school_year, semester, status, max_units, total_units, submitted_at)
                    VALUES (%s, %s, %s, %s, %s, %s, %s)
                    """
                    cursor.execute(query, (student['student_id'], school_year, semester, status, 
                                         max_units, total_units, submitted_at))
                
                if self.db_type == 'mysql':
                    enrollment_id = cursor.lastrowid
                else:
                    enrollment_id = self.get_last_insert_id(cursor, 'enrollments')
                
                if status in ['APPROVED', 'ENROLLED']:
                    num_subjects = random.randint(3, 7)
                    available_sections = random.sample(self.sections, min(num_subjects, len(self.sections)))
                    
                    for section in available_sections:
                        detail_status = random.choice(ENROLLMENT_DETAIL_STATUSES)
                        units = next((s['units'] for s in self.subjects if s['id'] == section['subject_id']), 3)
                        
                        if self.db_type == 'derby':
                            query = """
                            INSERT INTO APP.enrollments_details (enrollment_id, section_id, subject_id, units, status)
                            VALUES (?, ?, ?, ?, ?)
                            """
                            cursor.execute(query, (enrollment_id, section['id'], section['subject_id'], units, detail_status))
                        else:
                            query = """
                            INSERT INTO enrollments_details (enrollment_id, section_id, subject_id, units, status)
                            VALUES (%s, %s, %s, %s, %s)
                            """
                            cursor.execute(query, (enrollment_id, section['id'], section['subject_id'], units, detail_status))
                        
                        if self.db_type == 'derby':
                            cursor.execute("""
                                SELECT COUNT(*) FROM APP.student_enrolled_subjects 
                                WHERE student_id = ? AND subject_id = ?
                            """, (student['student_id'], section['subject_id']))
                            if cursor.fetchone()[0] == 0:
                                query = """
                                INSERT INTO APP.student_enrolled_subjects (student_id, subject_id)
                                VALUES (?, ?)
                                """
                                cursor.execute(query, (student['student_id'], section['subject_id']))
                        else:
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
        --db-type: Database type ('mysql' or 'derby', default: 'mysql')
        --host: Database host (default from config)
        --database: Database name (default from config)
        --user: Database user (default from config)
        --password: Database password (default from config)
        --no-clear: Skip clearing existing data
    """
    parser = argparse.ArgumentParser(description='University Database Seeder')
    parser.add_argument('--db-type', default='mysql', choices=['mysql', 'derby'], 
                       help='Database type (mysql or derby)')
    parser.add_argument('--host', default=DATABASE_CONFIG['host'], help='Database host')
    parser.add_argument('--database', default=DATABASE_CONFIG['database'], help='Database name')
    parser.add_argument('--user', default=DATABASE_CONFIG['user'], help='Database user')
    parser.add_argument('--password', default=DATABASE_CONFIG['password'], help='Database password')
    parser.add_argument('--no-clear', action='store_true', help='Do not clear existing data')
    
    args = parser.parse_args()
    
    seeder = UniversityDatabaseSeeder(
        db_type=args.db_type,
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
