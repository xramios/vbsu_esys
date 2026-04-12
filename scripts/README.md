# University Database Seeder

A comprehensive Python script to seed a university enrollment system database with realistic fake data using the Faker library.

## Features

- Generates realistic fake data for all tables in the enrollment system
- Maintains referential integrity between tables
- Configurable data generation with realistic constraints
- Support for different user roles (Student, Faculty, Registrar)
- Realistic scheduling and enrollment data
- Philippine-localized data generation
- **Multi-database support: MySQL and Derby/Java DB**

## Database Schema Coverage

The seeder generates data for all tables:

- `departments` - Academic departments
- `courses` - Degree programs
- `rooms` - Classroom and laboratory rooms
- `users` - System users with different roles
- `students` - Student profiles and information
- `faculty` - Faculty member profiles
- `curriculum` - Academic curriculum entries
- `subjects` - Course subjects with codes, units, estimated_time (minutes), and schedule_pattern
- `sections` - Class sections for subjects
- `enrollment_period` - Enrollment periods
- `schedules` - Class schedules with time slots
- `prerequisites` - Subject prerequisite relationships
- `enrollments` - Student enrollment records
- `enrollments_details` - Detailed enrollment information
- `student_enrolled_subjects` - Student-subject relationships

## Installation

1. Install the required dependencies:

```bash
pip install -r requirements.txt
```

1. Make sure you have a database server running with the database schema already created:

- **MySQL**: MySQL 5.7+ or MySQL 8.0+
- **Derby**: Java DB/Derby server running on port 1527 (default)

## Usage

### Basic Usage

```bash
python database_seeder.py
```

This will connect to `localhost` database `university_db` as `root` user and seed all tables with realistic data.

### Derby/Java DB Usage

```bash
python database_seeder.py --db-type derby
```

This will connect to Derby database using the default configuration (localhost:1527, user=app, password=app).

### Custom Database Connection

```bash
python database_seeder.py --host your-host --database your-db --user your-user --password your-password
```

### Options

- `--db-type`: Database type ('mysql' or 'derby', default: 'mysql')
- `--host`: Database host (default: localhost)
- `--database`: Database name (default: university_db)
- `--user`: Database user (default: root for mysql, app for derby)
- `--password`: Database password (default: empty for mysql, app for derby)
- `--no-clear`: Skip clearing existing data before seeding

### Example Commands

```bash
# MySQL with custom credentials
python database_seeder.py --host 192.168.1.100 --database enrollment_system --user admin --password secret123

# Derby with default configuration
python database_seeder.py --db-type derby

# Derby with custom configuration
python database_seeder.py --db-type derby --host localhost --database my_derby_db --user my_user --password my_pass

# Add data without clearing existing data
python database_seeder.py --no-clear

# Derby without clearing existing data
python database_seeder.py --db-type derby --no-clear
```

## Generated Data Statistics

The seeder generates approximately:

- **8 Academic Departments** (Engineering, Business, Arts, etc.)
- **15 Degree Programs** (Various bachelor's and professional degrees)
- **30 Rooms** (Lecture halls, labs, classrooms)
- **555 Users** (500 students, 50 faculty, 5 registrars)
- **500 Students** with realistic profiles
- **50 Faculty Members** across departments
- **100 Subjects** with proper codes and units
- **300+ Sections** (2-4 sections per subject)
- **Comprehensive Schedules** with time slots and room assignments
- **Realistic Enrollment Records** with varying statuses

## Data Realism Features

- **Student IDs**: Format like "2023-12345"
- **Email Addresses**: Unique and realistic
- **Names**: Philippine-localized names
- **Birthdates**: Realistic age ranges (18-25 for students)
- **Course Codes**: Professional format (e.g., "CALC101", "PROG201")
- **Room Numbers**: Building and floor based (e.g., "E101001")
- **Schedules**: Realistic time slots (7 AM - 7 PM)
- **Enrollment Statuses**: Complete workflow from draft to enrolled

## Database Requirements

The seeder supports multiple database types:

### MySQL

- MySQL 5.7+ or MySQL 8.0+
- Database schema must already be created (using the provided SQL file)
- User must have INSERT, DELETE, and SELECT privileges on the database

### Derby/Java DB

- Derby database server running (network server mode)
- Default port: 1527
- Database will be created if it doesn't exist (create=true in connection string)
- Java 8+ required for JDBC driver
- Additional Python dependencies: JPype1, JayDeBeApi

## Error Handling

The script includes comprehensive error handling:

- Database connection errors
- Foreign key constraint violations
- Data generation errors
- Transaction rollback on failures

## Customization

You can modify the data generation by editing the `seed_all()` method parameters:

```python
# Example: Generate more students and fewer subjects
self.seed_users(student_count=1000, faculty_count=75, registrar_count=10)
self.seed_subjects(count=150)
```

## Security Notes

- Passwords are simulated hashes (in production, use proper password hashing)
- The script requires database credentials - ensure secure handling
- Consider using environment variables for sensitive data in production

## Troubleshooting

### Connection Issues

**MySQL:**

- Verify MySQL server is running
- Check database credentials and permissions
- Ensure database exists and schema is created

**Derby:**

- Ensure Derby network server is running on port 1527
- Verify Java is installed and accessible
- Check that Derby client JAR is in classpath
- Ensure firewall allows connections to port 1527

### Foreign Key Errors

- Ensure database schema is properly created with all constraints
- Use `--no-clear` flag if you want to preserve existing data

### Memory Issues

- For very large datasets, consider batching inserts
- Monitor system memory usage during generation

### Derby-Specific Issues

- JVM startup failures: Check Java installation
- Classpath issues: Ensure Derby JARs are accessible
- Connection timeouts: Verify Derby server is running

## License

This script is part of the University Enrollment System project.
