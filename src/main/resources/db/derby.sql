/**
 * Apache Derby Database Schema for Enrollment System
 *
 * Defines the complete database structure for a university enrollment management
 * system, including student records, course catalogs, scheduling, and enrollment
 * tracking with full referential integrity.
 *
 * @database Apache Derby
 * @schema enrollment_system
 */

/**
 * Defines enrollment periods for each academic term.
 * Stores the school year, semester (e.g., First, Second, Summer),
 * and the date range when enrollment is open for students.
 * Used to control when students can enroll in classes.
 */
CREATE TABLE enrollment_period
(
    id          bigint      NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    school_year varchar(24) not null,
    semester    varchar(24) not null,
    start_date  TIMESTAMP   not null,
    end_date    TIMESTAMP   not null,
    updated_at  timestamp default current_timestamp,
    created_at  timestamp default current_timestamp
);

/**
 * Stores user credentials and roles for authentication.
 * Links to students, faculty, or registrars via role-based access.
 * Passwords should be stored as hashed values (bcrypt recommended).
 */
CREATE TABLE users
(
    id       bigint NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email    varchar(255),
    password char(60),
    role     varchar(20) CHECK (role IN ('STUDENT', 'REGISTRAR', 'FACULTY')),
    created_at timestamp default current_timestamp,
    updated_at timestamp default current_timestamp
);

/**
 * Contains student personal and academic information.
 * Links to users table for authentication and courses table for program enrollment.
 * Tracks student status (REGULAR/IRREGULAR) and current year level.
 */
CREATE TABLE students
(
    student_id     varchar(32) PRIMARY KEY,
    user_id        bigint,
    first_name     varchar(128) NOT NULL,
    last_name      varchar(128) NOT NULL,
    middle_name    varchar(48),
    birthdate      date         NOT NULL,
    student_status varchar(20) DEFAULT 'REGULAR' CHECK (student_status IN ('REGULAR', 'IRREGULAR')),
    course_id      bigint,
    year_level     int         default 1,
    created_at     timestamp   default current_timestamp,
    updated_at     timestamp   default current_timestamp,
);

/**
 * Catalog of all subjects/courses offered by the university.
 * Contains subject details including name, code, units, and description.
 * Linked to curriculum and department for academic organization.
 */
CREATE TABLE subjects
(
    id            bigint NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    subject_name  varchar(32),
    subject_code  varchar(32),
    units         float,
    description   clob,
    curriculum_id bigint,
    department_id bigint,
    updated_at    timestamp default current_timestamp,
    created_at    timestamp default current_timestamp
);

/**
 * Represents class sections for subjects.
 * Each section belongs to a specific subject and has a capacity limit.
 * Sections are scheduled in the schedules table with time and room assignments.
 */
CREATE TABLE sections
(
    id           bigint NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    section_name varchar(48),
    section_code varchar(48),
    subject_id   bigint,
    capacity     int    NOT NULL,
    updated_at   timestamp default current_timestamp,
    created_at   timestamp default current_timestamp
);

/**
 * Defines academic curricula for each course program.
 * A curriculum represents a specific year's academic requirements for a course.
 * Used to organize which subjects are required for each program year.
 */
CREATE TABLE curriculum
(
    id       bigint NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    cur_year date,
    course   bigint,
    created_at timestamp default current_timestamp,
    updated_at timestamp default current_timestamp
);

/**
 * Organizes curriculum into academic semesters.
 * Each semester belongs to a specific curriculum and contains a set of subjects.
 * Links curriculum structure to the subjects offered each term.
 */
CREATE TABLE semester
(
    id            bigint      NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    curriculum_id bigint      NOT NULL REFERENCES curriculum (id),
    semester      varchar(24) NOT NULL,
    created_at    timestamp default current_timestamp,
    updated_at    timestamp default current_timestamp
);

/**
 * Junction table linking semesters to their required subjects.
 * Specifies which subjects are offered in each semester and the recommended year level.
 * Used to track the academic progression of students through their program.
 */
CREATE TABLE semester_subjects
(
    id          bigint NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    semester_id bigint NOT NULL REFERENCES semester (id),
    subject_id  bigint NOT NULL REFERENCES subjects (id),
    year_level  int    NOT NULL, -- Ito yung year na dapat kunin ng student
    created_at  timestamp default current_timestamp,
    updated_at  timestamp default current_timestamp
);

/**
 * Tracks all subjects that students have enrolled in or completed.
 * Links students to semester subjects with status tracking (ENROLLED/COMPLETED/DROPPED).
 * Essential for prerequisite checking and academic history tracking.
 *
 * Note: This table is needed to track which subjects students have taken,
 * allowing the system to check prerequisites for future enrollments.
 * Students can take subjects incrementally per year.
 */
CREATE TABLE student_enrolled_subjects
(
    student_id          varchar(32)                                                        NOT NULL REFERENCES students (student_id),
    semester_subject_id bigint                                                             NOT NULL REFERENCES semester_subjects (id),
    status              varchar(20) CHECK (status IN ('ENROLLED', 'COMPLETED', 'DROPPED')) NOT NULL DEFAULT 'ENROLLED',
    created_at          timestamp                                                                   default current_timestamp,
    updated_at          timestamp                                                                   default current_timestamp
);

/**
 * Physical classroom and facility inventory.
 * Stores room identifiers and seating capacity for scheduling purposes.
 * Used by the scheduling system to assign sections to available rooms.
 */
CREATE TABLE rooms
(
    id       bigint      NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    room     varchar(32) NOT NULL,
    capacity int         NOT NULL,
    created_at timestamp default current_timestamp,
    updated_at timestamp default current_timestamp
);

/**
 * Contains faculty/instructor information and assignments.
 * Links to users table for authentication and departments for organizational structure.
 * Faculty are assigned to teach sections via the schedules table.
 */
CREATE TABLE faculty
(
    id            bigint NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id       bigint,
    first_name    varchar(128),
    last_name     varchar(128),
    department_id bigint,
    updated_at    timestamp default current_timestamp,
    created_at    timestamp default current_timestamp
);

CREATE TABLE registrar
(
    id             bigint NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id        bigint,
    employee_id    varchar(20),
    first_name     varchar(128),
    last_name      varchar(128),
    contact_number varchar(20),
    updated_at     timestamp default current_timestamp,
    created_at     timestamp default current_timestamp
);

/**
 * Class schedule assignments linking sections to rooms, faculty, and time slots.
 * Defines when and where each section meets (day of week, start/end times).
 * Prevents scheduling conflicts for rooms, faculty, and students.
 */
CREATE TABLE schedules
(
    id          bigint NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    section_id  bigint,
    room_id     bigint,
    faculty_id  bigint,
    day         varchar(3) CHECK (day IN ('MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT', 'SUN')),
    start_time  time,
    end_time    time,
    school_year varchar(9),
    semester    SMALLINT,
    updated_at  timestamp default current_timestamp,
    created_at  timestamp default current_timestamp
);

/**
 * Enrollment records tracking a student's enrollment for a specific term.
 * Records the school year, semester, enrollment status, and unit limits.
 * Status workflow: DRAFT -> SUBMITTED -> APPROVED -> ENROLLED (or CANCELLED).
 */
CREATE TABLE enrollments
(
    id           bigint NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    student_id   varchar(32),
    school_year  varchar(9),
    semester     SMALLINT,
    status       varchar(20) CHECK (status IN ('DRAFT', 'SUBMITTED', 'APPROVED', 'ENROLLED', 'CANCELLED')),
    max_units    float,
    total_units  float,
    submitted_at TIMESTAMP,
    updated_at   timestamp default current_timestamp,
    created_at   timestamp default current_timestamp
);

/**
 * Detailed line items for each enrollment record.
 * Specifies which sections and subjects the student selected.
 * Tracks unit counts and allows for selective dropping of subjects.
 */
CREATE TABLE enrollments_details
(
    id            bigint NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    enrollment_id bigint,
    section_id    bigint,
    subject_id    bigint,
    units         float,
    status        varchar(20) CHECK (status IN ('SELECTED', 'DROPPED')),
    created_at    timestamp default current_timestamp,
    updated_at    timestamp default current_timestamp
);


/**
 * Academic programs/degrees offered by the university.
 * Each course belongs to a department and has a descriptive overview.
 * Students are enrolled in a specific course as their primary program.
 */
CREATE TABLE courses
(
    id            bigint NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    course_name   varchar(48),
    description   clob,
    department_id bigint,
    updated_at    timestamp default current_timestamp,
    created_at    timestamp default current_timestamp
);

/**
 * Defines subject prerequisites and course sequencing requirements.
 * Maps prerequisite subjects to dependent subjects that require them.
 * Used to validate that students have completed required prerequisites
 * before enrolling in advanced subjects.
 */
CREATE TABLE prerequisites
(
    id             bigint NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    pre_subject_id bigint,
    subject_id     bigint,
    updated_at     timestamp default current_timestamp,
    created_at     timestamp default current_timestamp
);

/**
 * University academic departments and administrative units.
 * Top-level organizational structure grouping courses, subjects, and faculty.
 * Examples: College of Engineering, College of Business, etc.
 */
CREATE TABLE departments
(
    id              bigint NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    department_name varchar(48),
    description     clob,
    updated_at      timestamp default current_timestamp,
    created_at      timestamp default current_timestamp
);

CREATE UNIQUE INDEX student_enrolled_subjects_index_0 ON student_enrolled_subjects (student_id, semester_subject_id);

ALTER TABLE students
    ADD CONSTRAINT fk_students_user FOREIGN KEY (user_id) REFERENCES users (id);

ALTER TABLE students
    ADD CONSTRAINT fk_students_course FOREIGN KEY (course_id) REFERENCES courses (id);

ALTER TABLE subjects
    ADD CONSTRAINT fk_subjects_curriculum FOREIGN KEY (curriculum_id) REFERENCES curriculum (id);

ALTER TABLE subjects
    ADD CONSTRAINT fk_subjects_dept FOREIGN KEY (department_id) REFERENCES departments (id);

ALTER TABLE sections
    ADD CONSTRAINT fk_sections_subject FOREIGN KEY (subject_id) REFERENCES subjects (id);

ALTER TABLE curriculum
    ADD CONSTRAINT fk_curriculum_course FOREIGN KEY (course) REFERENCES courses (id);

ALTER TABLE faculty
    ADD CONSTRAINT fk_faculty_user FOREIGN KEY (user_id) REFERENCES users (id);

ALTER TABLE faculty
    ADD CONSTRAINT fk_faculty_dept FOREIGN KEY (department_id) REFERENCES departments (id);

ALTER TABLE schedules
    ADD CONSTRAINT fk_schedules_section FOREIGN KEY (section_id) REFERENCES sections (id);

ALTER TABLE schedules
    ADD CONSTRAINT fk_schedules_room FOREIGN KEY (room_id) REFERENCES rooms (id);

ALTER TABLE schedules
    ADD CONSTRAINT fk_schedules_faculty FOREIGN KEY (faculty_id) REFERENCES faculty (id);

ALTER TABLE enrollments
    ADD CONSTRAINT fk_enrollments_student FOREIGN KEY (student_id) REFERENCES students (student_id);

ALTER TABLE enrollments_details
    ADD CONSTRAINT fk_ed_enrollment FOREIGN KEY (enrollment_id) REFERENCES enrollments (id);

ALTER TABLE enrollments_details
    ADD CONSTRAINT fk_ed_section FOREIGN KEY (section_id) REFERENCES sections (id);

ALTER TABLE enrollments_details
    ADD CONSTRAINT fk_ed_subject FOREIGN KEY (subject_id) REFERENCES subjects (id);

ALTER TABLE student_enrolled_subjects
    ADD CONSTRAINT fk_ses_student FOREIGN KEY (student_id) REFERENCES students (student_id);

ALTER TABLE student_enrolled_subjects
    ADD CONSTRAINT fk_ses_subject FOREIGN KEY (semester_subject_id) REFERENCES subjects (id);

ALTER TABLE courses
    ADD CONSTRAINT fk_courses_dept FOREIGN KEY (department_id) REFERENCES departments (id);

ALTER TABLE prerequisites
    ADD CONSTRAINT fk_prereq_presubject FOREIGN KEY (pre_subject_id) REFERENCES subjects (id);

ALTER TABLE prerequisites
    ADD CONSTRAINT fk_prereq_subject FOREIGN KEY (subject_id) REFERENCES subjects (id);
