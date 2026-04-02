/**
 * MySQL Database Schema for Enrollment System
 * 
 * Defines the complete database structure for a university enrollment management
 * system, including student records, course catalogs, scheduling, and enrollment
 * tracking with full referential integrity.
 * 
 * @database MySQL 8.0+
 * @schema enrollment_system
 */

/**
 * Defines enrollment periods for each academic term.
 * Stores the school year, semester (e.g., First, Second, Summer),
 * and the date range when enrollment is open for students.
 * Used to control when students can enroll in classes.
 */
CREATE TABLE IF NOT EXISTS enrollment_period
(
    id          bigint PRIMARY KEY AUTO_INCREMENT,
    school_year varchar(24) not null,
    semester    varchar(24) not null,
    start_date  datetime    not null,
    end_date    datetime    not null,
    updated_at  timestamp default current_timestamp(),
    created_at  timestamp default current_timestamp()
);

/**
 * Stores user credentials and roles for authentication.
 * Links to students, faculty, or registrars via role-based access.
 * Passwords should be stored as hashed values (bcrypt recommended).
 */
CREATE TABLE IF NOT EXISTS users
(
    id       bigint PRIMARY KEY AUTO_INCREMENT,
    email    varchar(255),
    password char(60),
    role     ENUM ('STUDENT', 'REGISTRAR', 'FACULTY')
);

/**
 * Contains student personal and academic information.
 * Links to users table for authentication and courses table for program enrollment.
 * Tracks student status (REGULAR/IRREGULAR) and current year level.
 */
CREATE TABLE IF NOT EXISTS students
(
    student_id     varchar(32) PRIMARY KEY,
    user_id        bigint,
    first_name     varchar(128) NOT NULL,
    last_name      varchar(128) NOT NULL,
    middle_name    varchar(48)  NULL,
    birthdate      date         NOT NULL,
    student_status ENUM ('REGULAR', 'IRREGULAR') DEFAULT 'REGULAR',
    course_id      bigint,
    year_level     int                           default 1,
    created_at     timestamp                     default current_timestamp()
);

/**
 * Catalog of all subjects/courses offered by the university.
 * Contains subject details including name, code, units, and description.
 * Linked to curriculum and department for academic organization.
 */
CREATE TABLE IF NOT EXISTS subjects
(
    id            bigint PRIMARY KEY AUTO_INCREMENT,
    subject_name  varchar(32),
    subject_code  varchar(32),
    units         float,
    description   text,
    curriculum_id bigint,
    department_id bigint,
    updated_at    timestamp default current_timestamp(),
    created_at    timestamp default current_timestamp()
);

/**
 * Represents class sections for subjects.
 * Each section belongs to a specific subject and has a capacity limit.
 * Sections are scheduled in the schedules table with time and room assignments.
 */
CREATE TABLE IF NOT EXISTS sections
(
    id           bigint PRIMARY KEY AUTO_INCREMENT,
    section_name varchar(48),
    section_code varchar(48),
    subject_id   bigint,
    capacity     int NOT NULL,
    updated_at   timestamp    default current_timestamp(),
    created_at   timestamp    default current_timestamp()
);

/**
 * Defines academic curricula for each course program.
 * A curriculum represents a specific year's academic requirements for a course.
 * Used to organize which subjects are required for each program year.
 */
CREATE TABLE IF NOT EXISTS curriculum
(
    id       bigint PRIMARY KEY AUTO_INCREMENT,
    cur_year date,
    course bigint
);

/**
 * Organizes curriculum into academic semesters.
 * Each semester belongs to a specific curriculum and contains a set of subjects.
 * Links curriculum structure to the subjects offered each term.
 */
CREATE TABLE IF NOT EXISTS semester (
    id bigint PRIMARY KEY AUTO_INCREMENT,
    curriculum_id bigint NOT NULL,
    semester varchar(24) NOT NULL,
    created_at timestamp default current_timestamp(),
    updated_at timestamp default current_timestamp(),
    FOREIGN KEY (curriculum_id) REFERENCES curriculum(id)
);

/**
 * Junction table linking semesters to their required subjects.
 * Specifies which subjects are offered in each semester and the recommended year level.
 * Used to track the academic progression of students through their program.
 */
CREATE TABLE IF NOT EXISTS semester_subjects (
    semester_id bigint NOT NULL,
    subject_id bigint NOT NULL,
    year_level int NOT NULL,
    created_at timestamp default current_timestamp(),
    updated_at timestamp default current_timestamp()
);

/**
 * Physical classroom and facility inventory.
 * Stores room identifiers and seating capacity for scheduling purposes.
 * Used by the scheduling system to assign sections to available rooms.
 */
CREATE TABLE IF NOT EXISTS rooms
(
    id       bigint PRIMARY KEY AUTO_INCREMENT,
    room     varchar(32) NOT NULL,
    capacity int         NOT NULL
);

/**
 * Contains faculty/instructor information and assignments.
 * Links to users table for authentication and departments for organizational structure.
 * Faculty are assigned to teach sections via the schedules table.
 */
CREATE TABLE IF NOT EXISTS faculty
(
    id            bigint PRIMARY KEY AUTO_INCREMENT,
    user_id       bigint,
    first_name    varchar(128),
    last_name     varchar(128),
    department_id bigint,
    updated_at    timestamp default current_timestamp(),
    created_at    timestamp default current_timestamp()
);

/**
 * Class schedule assignments linking sections to rooms, faculty, and time slots.
 * Defines when and where each section meets (day of week, start/end times).
 * Prevents scheduling conflicts for rooms, faculty, and students.
 */
CREATE TABLE IF NOT EXISTS schedules
(
    id          bigint PRIMARY KEY AUTO_INCREMENT,
    section_id  bigint,
    room_id     bigint,
    faculty_id  bigint,
    day         ENUM ('MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT', 'SUN'),
    start_time  time,
    end_time    time,
    school_year varchar(9),
    semester    tinyint,
    updated_at  timestamp default current_timestamp(),
    created_at  timestamp default current_timestamp()
);

/**
 * Enrollment records tracking a student's enrollment for a specific term.
 * Records the school year, semester, enrollment status, and unit limits.
 * Status workflow: DRAFT -> SUBMITTED -> APPROVED -> ENROLLED (or CANCELLED).
 */
CREATE TABLE IF NOT EXISTS enrollments
(
    id           bigint PRIMARY KEY AUTO_INCREMENT,
    student_id   varchar(32),
    school_year  varchar(9),
    semester     tinyint,
    status       ENUM ('DRAFT', 'SUBMITTED', 'APPROVED', 'ENROLLED', 'CANCELLED'),
    max_units    float,
    total_units  float,
    submitted_at datetime,
    updated_at   timestamp default current_timestamp(),
    created_at   timestamp default current_timestamp()
);

/**
 * Detailed line items for each enrollment record.
 * Specifies which sections and subjects the student selected.
 * Tracks unit counts and allows for selective dropping of subjects.
 */
CREATE TABLE IF NOT EXISTS enrollments_details
(
    id            bigint PRIMARY KEY AUTO_INCREMENT,
    enrollment_id bigint,
    section_id    bigint,
    subject_id    bigint,
    units         float,
    status        ENUM ('SELECTED', 'DROPPED'),
    created_at    timestamp default current_timestamp(),
    updated_at    timestamp default current_timestamp()
);

/**
 * Tracks all subjects that students have enrolled in or completed.
 * Links students to semester subjects with status tracking (ENROLLED/COMPLETED/DROPPED).
 * Essential for prerequisite checking and academic history tracking.
 */
CREATE TABLE IF NOT EXISTS student_enrolled_subjects
(
    student_id varchar(32),
    subject_id bigint,
    semester_id bigint NOT NULL,
    status varchar(20) CHECK (status IN ('ENROLLED', 'COMPLETED', 'DROPPED')) DEFAULT 'ENROLLED',
    created_at timestamp default current_timestamp(),
    updated_at timestamp default current_timestamp(),
    FOREIGN KEY (student_id) REFERENCES students(student_id),
    FOREIGN KEY (subject_id) REFERENCES subjects(id),
    FOREIGN KEY (semester_id) REFERENCES semester(id)
);

/**
 * Academic programs/degrees offered by the university.
 * Each course belongs to a department and has a descriptive overview.
 * Students are enrolled in a specific course as their primary program.
 */
CREATE TABLE IF NOT EXISTS courses
(
    id            bigint PRIMARY KEY AUTO_INCREMENT,
    course_name   varchar(48),
    description   text,
    department_id bigint,
    updated_at    timestamp default current_timestamp(),
    created_at    timestamp default current_timestamp()
);

/**
 * Defines subject prerequisites and course sequencing requirements.
 * Maps prerequisite subjects to dependent subjects that require them.
 * Used to validate that students have completed required prerequisites
 * before enrolling in advanced subjects.
 */
CREATE TABLE IF NOT EXISTS prerequisites
(
    id             bigint PRIMARY KEY AUTO_INCREMENT,
    pre_subject_id bigint,
    subject_id     bigint,
    updated_at     timestamp default current_timestamp(),
    created_at     timestamp default current_timestamp()
);

/**
 * University academic departments and administrative units.
 * Top-level organizational structure grouping courses, subjects, and faculty.
 * Examples: College of Engineering, College of Business, etc.
 */
CREATE TABLE IF NOT EXISTS departments
(
    id              bigint PRIMARY KEY AUTO_INCREMENT,
    department_name varchar(48),
    description     text,
    updated_at      timestamp default current_timestamp(),
    created_at      timestamp default current_timestamp()
);

CREATE UNIQUE INDEX student_enrolled_subjects_index_0 ON student_enrolled_subjects (student_id, subject_id);

ALTER TABLE students
    ADD FOREIGN KEY (user_id) REFERENCES users (id);

ALTER TABLE students
    ADD FOREIGN KEY (course_id) REFERENCES courses (id);

ALTER TABLE subjects
    ADD FOREIGN KEY (curriculum_id) REFERENCES curriculum (id);

ALTER TABLE subjects
    ADD FOREIGN KEY (department_id) REFERENCES departments (id);

ALTER TABLE sections
    ADD FOREIGN KEY (subject_id) REFERENCES subjects (id);

ALTER TABLE curriculum
    ADD FOREIGN KEY (course) REFERENCES courses (id);

ALTER TABLE faculty
    ADD FOREIGN KEY (user_id) REFERENCES users (id);

ALTER TABLE faculty
    ADD FOREIGN KEY (department_id) REFERENCES departments (id);

ALTER TABLE schedules
    ADD FOREIGN KEY (section_id) REFERENCES sections (id);

ALTER TABLE schedules
    ADD FOREIGN KEY (room_id) REFERENCES rooms (id);

ALTER TABLE schedules
    ADD FOREIGN KEY (faculty_id) REFERENCES faculty (id);

ALTER TABLE enrollments
    ADD FOREIGN KEY (student_id) REFERENCES students (student_id);

ALTER TABLE enrollments_details
    ADD FOREIGN KEY (enrollment_id) REFERENCES enrollments (id);

ALTER TABLE enrollments_details
    ADD FOREIGN KEY (section_id) REFERENCES sections (id);

ALTER TABLE enrollments_details
    ADD FOREIGN KEY (subject_id) REFERENCES subjects (id);

ALTER TABLE semester_subjects
    ADD FOREIGN KEY (semester_id) REFERENCES semester (id);

ALTER TABLE semester_subjects
    ADD FOREIGN KEY (subject_id) REFERENCES subjects (id);

ALTER TABLE student_enrolled_subjects
    ADD FOREIGN KEY (student_id) REFERENCES students (student_id);

ALTER TABLE student_enrolled_subjects
    ADD FOREIGN KEY (subject_id) REFERENCES subjects (id);

ALTER TABLE courses
    ADD FOREIGN KEY (department_id) REFERENCES departments (id);

ALTER TABLE prerequisites
    ADD FOREIGN KEY (pre_subject_id) REFERENCES subjects (id);

ALTER TABLE prerequisites
    ADD FOREIGN KEY (subject_id) REFERENCES subjects (id);
