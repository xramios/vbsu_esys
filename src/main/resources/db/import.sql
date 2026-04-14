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
    description text,
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
    id         bigint PRIMARY KEY AUTO_INCREMENT,
    email      varchar(255),
    password   char(60),
    role       ENUM ('STUDENT', 'REGISTRAR', 'FACULTY', 'ADMIN'),
    created_at timestamp default current_timestamp(),
    updated_at timestamp default current_timestamp()
);

/**
 * Contains administrator information and links to user accounts.
 * Admins have elevated permissions for managing the system.
 */
CREATE TABLE IF NOT EXISTS admins
(
    id             bigint PRIMARY KEY AUTO_INCREMENT,
    user_id        bigint,
    first_name     varchar(128),
    last_name      varchar(128),
    contact_number varchar(20),
    updated_at     timestamp default current_timestamp(),
    created_at     timestamp default current_timestamp()
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
    curriculum_id  bigint,
    year_level     int                           default 1,
    created_at     timestamp                     default current_timestamp(),
    updated_at     timestamp                     default current_timestamp()
);

/**
 * Catalog of all subjects/courses offered by the university.
 * Contains subject details including name, code, units, and description.
 * Linked to department for academic organization.
 */
CREATE TABLE IF NOT EXISTS subjects
(
    id            bigint PRIMARY KEY AUTO_INCREMENT,
    subject_name  varchar(32),
    subject_code  varchar(32),
    units         float,
    estimated_time int default 90,
    schedule_pattern varchar(32) default 'LECTURE_ONLY' CHECK (schedule_pattern IN ('LECTURE_ONLY', 'LECTURE_LAB', 'GE_PAIRED', 'PE_PAIRED', 'NSTP_BLOCK')),
    description   text,
    department_id bigint,
    updated_at    timestamp default current_timestamp(),
    created_at    timestamp default current_timestamp()
);

/**
 * Represents class sections independent of specific subjects.
 * Sections act as shared class groups with capacity limits and reusable schedules.
 */
CREATE TABLE IF NOT EXISTS sections
(
    id           bigint PRIMARY KEY AUTO_INCREMENT,
    section_name varchar(48),
    section_code varchar(48),
    capacity     int NOT NULL,
    status       ENUM ('OPEN', 'CLOSED', 'WAITLIST', 'DISSOLVED') DEFAULT 'OPEN',
    updated_at   timestamp default current_timestamp(),
    created_at   timestamp default current_timestamp()
);

/**
 * Defines academic curricula for each course program.
 * A curriculum represents a specific year's academic requirements for a course.
 * Used to organize which subjects are required for each program year.
 */
CREATE TABLE IF NOT EXISTS curriculum
(
    id         bigint PRIMARY KEY AUTO_INCREMENT,
    name       varchar(64),
    cur_year   date,
    course     bigint,
    created_at timestamp default current_timestamp(),
    updated_at timestamp default current_timestamp()
);

/**
 * Organizes curriculum into academic semesters.
 * Each semester belongs to a specific curriculum and contains a set of subjects.
 * Links curriculum structure to the subjects offered each term.
 */
CREATE TABLE IF NOT EXISTS semester
(
    id            bigint PRIMARY KEY AUTO_INCREMENT,
    curriculum_id  bigint NOT NULL,
    semester      varchar(24) NOT NULL,
    year_level    int NOT NULL,
    created_at    timestamp default current_timestamp(),
    updated_at    timestamp default current_timestamp()
);

/**
 * Junction table linking semesters to their required subjects.
 * Specifies which subjects are offered in each semester and the recommended year level.
 * Used to track the academic progression of students through their program.
 */
CREATE TABLE IF NOT EXISTS semester_subjects
(
    id          bigint PRIMARY KEY AUTO_INCREMENT,
    semester_id bigint NOT NULL,
    subject_id  bigint NOT NULL,
    created_at  timestamp default current_timestamp(),
    updated_at  timestamp default current_timestamp()
);

/**
 * Tracks all subjects that students have enrolled in or completed.
 * Links actual enrolled offerings to planned semester subjects with status tracking.
 * Essential for prerequisite checking and academic history tracking.
 */
CREATE TABLE IF NOT EXISTS student_enrolled_subjects
(
    student_id          varchar(32) NOT NULL,
    enrollment_id       bigint      NOT NULL,
    offering_id         bigint      NOT NULL,
    semester_subject_id bigint      NOT NULL,
    status              ENUM ('ENROLLED', 'COMPLETED', 'DROPPED') NOT NULL DEFAULT 'ENROLLED',
    is_selected         BOOLEAN NOT NULL DEFAULT FALSE,
    created_at          timestamp default current_timestamp(),
    updated_at          timestamp default current_timestamp()
);

/**
 * Tracks semester-level progress for each student within a curriculum.
 * One record represents a student's progress for a curriculum semester
 * and is marked as NOT_STARTED, IN_PROGRESS, or COMPLETED.
 */
CREATE TABLE IF NOT EXISTS student_semester_progress
(
    id            bigint PRIMARY KEY AUTO_INCREMENT,
    student_id    varchar(32) NOT NULL,
    curriculum_id bigint      NOT NULL,
    semester_id   bigint      NOT NULL,
    status        ENUM ('NOT_STARTED', 'IN_PROGRESS', 'COMPLETED') NOT NULL DEFAULT 'NOT_STARTED',
    started_at    timestamp,
    completed_at  timestamp,
    created_at    timestamp default current_timestamp(),
    updated_at    timestamp default current_timestamp()
);

/**
 * Physical classroom and facility inventory.
 * Stores room identifiers and seating capacity for scheduling purposes.
 * Used by the scheduling system to assign sections to available rooms.
 */
CREATE TABLE IF NOT EXISTS rooms
(
    id         bigint PRIMARY KEY AUTO_INCREMENT,
    building   varchar(64) NOT NULL,
    room_type  ENUM ('LECTURE', 'LAB', 'SEMINAR', 'AUDITORIUM', 'OTHER') NOT NULL,
    status     ENUM ('AVAILABLE', 'UNAVAILABLE', 'MAINTENANCE') NOT NULL DEFAULT 'AVAILABLE',
    room       varchar(32) NOT NULL,
    capacity   int         NOT NULL,
    created_at timestamp default current_timestamp(),
    updated_at timestamp default current_timestamp()
);

/**
 * Contains faculty/instructor information and assignments.
 * Links to users table for authentication and departments for organizational structure.
 * Faculty are assigned to teach sections via the schedules table.
 */
CREATE TABLE IF NOT EXISTS faculty
(
    id             bigint PRIMARY KEY AUTO_INCREMENT,
    user_id        bigint,
    first_name     varchar(128),
    last_name      varchar(128),
    middle_name    varchar(48),
    contact_number varchar(20),
    birthdate      date,
    department_id  bigint,
    updated_at     timestamp default current_timestamp(),
    created_at     timestamp default current_timestamp()
);

/**
 * Contains registrar information and assignments.
 * Links to users table for authentication and administrative workflow.
 */
CREATE TABLE IF NOT EXISTS registrar
(
    id             bigint PRIMARY KEY AUTO_INCREMENT,
    user_id        bigint,
    employee_id    varchar(20),
    first_name     varchar(128),
    last_name      varchar(128),
    contact_number varchar(20),
    updated_at     timestamp default current_timestamp(),
    created_at     timestamp default current_timestamp()
);

/**
 * Class schedule assignments linking offerings to rooms, faculty, and time slots.
 * Defines when and where each offering meets (day of week, start/end times).
 * Prevents scheduling conflicts for rooms, faculty, and students.
 */
CREATE TABLE IF NOT EXISTS schedules
(
    id          bigint PRIMARY KEY AUTO_INCREMENT,
    offering_id bigint NOT NULL,
    room_id     bigint,
    faculty_id  bigint,
    day         ENUM ('MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT', 'SUN'),
    start_time  time,
    end_time    time,
    updated_at  timestamp default current_timestamp(),
    created_at  timestamp default current_timestamp()
);

/**
 * Concrete class offerings for a specific enrollment period.
 * Acts as the single source of truth for enrollable classes by pairing
 * subject, section, and term-specific capacity in one record.
 * Optionally maps to planned curriculum entries via semester_subjects.
 */
CREATE TABLE IF NOT EXISTS offerings
(
    id                   bigint PRIMARY KEY AUTO_INCREMENT,
    subject_id           bigint NOT NULL,
    section_id           bigint NOT NULL,
    enrollment_period_id bigint NOT NULL,
    semester_subject_id  bigint,
    capacity             int,
    created_at           timestamp default current_timestamp()
);

/**
 * Enrollment records tracking a student's enrollment for a specific term.
 * Records the school year, semester, enrollment status, and unit limits.
 * Status workflow: DRAFT -> SUBMITTED -> APPROVED -> ENROLLED -> COMPLETED (or CANCELLED).
 */
CREATE TABLE IF NOT EXISTS enrollments
(
    id                   bigint PRIMARY KEY AUTO_INCREMENT,
    student_id           varchar(32) NOT NULL,
    enrollment_period_id bigint NOT NULL,
    status               ENUM ('DRAFT', 'SUBMITTED', 'APPROVED', 'ENROLLED', 'COMPLETED', 'CANCELLED') NOT NULL,
    max_units            float,
    total_units          float,
    submitted_at         datetime,
    approved_by          bigint,
    approved_at          datetime,
    updated_at           timestamp default current_timestamp(),
    created_at           timestamp default current_timestamp()
);

/**
 * Detailed line items for each enrollment record.
 * Specifies which concrete offerings the student selected.
 * Tracks unit counts and allows for selective dropping of offerings.
 */
CREATE TABLE IF NOT EXISTS enrollments_details
(
    id            bigint PRIMARY KEY AUTO_INCREMENT,
    enrollment_id bigint NOT NULL,
    offering_id   bigint NOT NULL,
    units         float,
    status        ENUM ('SELECTED', 'DROPPED'),
    created_at    timestamp default current_timestamp(),
    updated_at    timestamp default current_timestamp()
);

/**
 * Tracks requests from faculty to drop a student from an offering.
 * Registrar staff review and act on these requests.
 */
CREATE TABLE IF NOT EXISTS faculty_student_drop_requests
(
    id          bigint PRIMARY KEY AUTO_INCREMENT,
    faculty_id  bigint NOT NULL,
    student_id  varchar(32) NOT NULL,
    offering_id bigint NOT NULL,
    reason      text,
    status      ENUM ('PENDING', 'APPROVED', 'REJECTED') DEFAULT 'PENDING',
    created_at  timestamp default current_timestamp(),
    updated_at  timestamp default current_timestamp(),
    UNIQUE (student_id, offering_id)
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
    department_code varchar(24),
    description     text,
    updated_at      timestamp default current_timestamp(),
    created_at      timestamp default current_timestamp()
);

CREATE UNIQUE INDEX curriculum_name_unique_index_0 ON curriculum (name);
CREATE UNIQUE INDEX semester_unique_pair_index_0 ON semester (curriculum_id, semester, year_level);
CREATE UNIQUE INDEX semester_subjects_unique_pair_index_0 ON semester_subjects (semester_id, subject_id);
CREATE UNIQUE INDEX semester_subjects_id_subject_index_0 ON semester_subjects (id, subject_id);
CREATE UNIQUE INDEX student_enrolled_subjects_index_0 ON student_enrolled_subjects (student_id, offering_id);
CREATE UNIQUE INDEX student_semester_progress_index_0 ON student_semester_progress (student_id, semester_id);
CREATE UNIQUE INDEX offerings_unique_pair_index_0 ON offerings (subject_id, section_id, enrollment_period_id);
CREATE UNIQUE INDEX enrollments_unique_pair_index_0 ON enrollments (student_id, enrollment_period_id);
CREATE UNIQUE INDEX enrollments_details_unique_pair_index_0 ON enrollments_details (enrollment_id, offering_id);
CREATE UNIQUE INDEX prerequisites_unique_pair_index_0 ON prerequisites (pre_subject_id, subject_id);

CREATE INDEX idx_enrollments_details_status ON enrollments_details (status);
CREATE INDEX idx_enrollments_details_offering ON enrollments_details (offering_id, status);
CREATE INDEX idx_enrollments_details_enrollment ON enrollments_details (enrollment_id, status);

CREATE INDEX idx_enrollments_student ON enrollments (student_id);
CREATE INDEX idx_enrollments_period ON enrollments (enrollment_period_id);
CREATE INDEX idx_enrollments_student_period ON enrollments (student_id, enrollment_period_id, created_at);
CREATE INDEX idx_enrollments_status ON enrollments (status);

CREATE INDEX idx_schedules_faculty ON schedules (faculty_id);
CREATE INDEX idx_schedules_room ON schedules (room_id);
CREATE INDEX idx_schedules_offering ON schedules (offering_id);
CREATE INDEX idx_schedules_day_time ON schedules (day, start_time, end_time);
CREATE INDEX idx_schedules_faculty_day ON schedules (faculty_id, day, start_time, end_time);
CREATE INDEX idx_schedules_room_day ON schedules (room_id, day, start_time, end_time);

CREATE INDEX idx_offerings_period ON offerings (enrollment_period_id);
CREATE INDEX idx_offerings_subject ON offerings (subject_id);
CREATE INDEX idx_offerings_section ON offerings (section_id);
CREATE INDEX idx_offerings_period_subject ON offerings (enrollment_period_id, subject_id);
CREATE INDEX idx_offerings_period_section ON offerings (enrollment_period_id, section_id);

CREATE INDEX idx_student_enrolled_subjects_student ON student_enrolled_subjects (student_id);
CREATE INDEX idx_student_enrolled_subjects_status ON student_enrolled_subjects (status);
CREATE INDEX idx_student_enrolled_subjects_semester ON student_enrolled_subjects (semester_subject_id);

CREATE INDEX idx_semester_subjects_semester ON semester_subjects (semester_id);
CREATE INDEX idx_semester_subjects_subject ON semester_subjects (subject_id);

CREATE INDEX idx_semester_curriculum ON semester (curriculum_id);

CREATE INDEX idx_curriculum_course ON curriculum (course);
CREATE INDEX idx_curriculum_year ON curriculum (cur_year DESC, created_at DESC);

CREATE INDEX idx_enrollments_details_covering ON enrollments_details (enrollment_id, status, offering_id, units, created_at, id);
CREATE INDEX idx_schedules_covering ON schedules (offering_id, day, start_time, end_time, room_id, faculty_id);
CREATE INDEX idx_offerings_covering ON offerings (id, subject_id, section_id, enrollment_period_id, capacity, semester_subject_id);
CREATE INDEX idx_student_enrolled_subjects_covering ON student_enrolled_subjects (student_id, semester_subject_id, offering_id, enrollment_id, status);

ALTER TABLE students
    ADD CONSTRAINT fk_students_user FOREIGN KEY (user_id) REFERENCES users (id);

ALTER TABLE students
    ADD CONSTRAINT fk_students_course FOREIGN KEY (course_id) REFERENCES courses (id);

ALTER TABLE students
    ADD CONSTRAINT fk_students_curriculum FOREIGN KEY (curriculum_id) REFERENCES curriculum (id);

ALTER TABLE subjects
    ADD CONSTRAINT fk_subjects_dept FOREIGN KEY (department_id) REFERENCES departments (id);

ALTER TABLE curriculum
    ADD CONSTRAINT fk_curriculum_course FOREIGN KEY (course) REFERENCES courses (id);

ALTER TABLE faculty
    ADD CONSTRAINT fk_faculty_user FOREIGN KEY (user_id) REFERENCES users (id);

ALTER TABLE faculty
    ADD CONSTRAINT fk_faculty_dept FOREIGN KEY (department_id) REFERENCES departments (id);

ALTER TABLE semester
    ADD CONSTRAINT fk_semester_curriculum FOREIGN KEY (curriculum_id) REFERENCES curriculum (id);

ALTER TABLE semester_subjects
    ADD CONSTRAINT fk_semester_subjects_semester FOREIGN KEY (semester_id) REFERENCES semester (id);

ALTER TABLE semester_subjects
    ADD CONSTRAINT fk_semester_subjects_subject FOREIGN KEY (subject_id) REFERENCES subjects (id);

ALTER TABLE student_enrolled_subjects
    ADD CONSTRAINT fk_ses_student FOREIGN KEY (student_id) REFERENCES students (student_id);

ALTER TABLE student_enrolled_subjects
    ADD CONSTRAINT fk_ses_enrollment FOREIGN KEY (enrollment_id) REFERENCES enrollments (id);

ALTER TABLE student_enrolled_subjects
    ADD CONSTRAINT fk_ses_offering FOREIGN KEY (offering_id) REFERENCES offerings (id);

ALTER TABLE student_enrolled_subjects
    ADD CONSTRAINT fk_ses_enrollment_offering
        FOREIGN KEY (enrollment_id, offering_id) REFERENCES enrollments_details (enrollment_id, offering_id);

ALTER TABLE student_enrolled_subjects
    ADD CONSTRAINT fk_ses_subject FOREIGN KEY (semester_subject_id) REFERENCES semester_subjects (id);

ALTER TABLE student_semester_progress
    ADD CONSTRAINT fk_ssp_student FOREIGN KEY (student_id) REFERENCES students (student_id);

ALTER TABLE student_semester_progress
    ADD CONSTRAINT fk_ssp_curriculum FOREIGN KEY (curriculum_id) REFERENCES curriculum (id);

ALTER TABLE student_semester_progress
    ADD CONSTRAINT fk_ssp_semester FOREIGN KEY (semester_id) REFERENCES semester (id);

ALTER TABLE schedules
    ADD CONSTRAINT fk_schedules_offering FOREIGN KEY (offering_id) REFERENCES offerings (id);

ALTER TABLE schedules
    ADD CONSTRAINT fk_schedules_room FOREIGN KEY (room_id) REFERENCES rooms (id);

ALTER TABLE schedules
    ADD CONSTRAINT fk_schedules_faculty FOREIGN KEY (faculty_id) REFERENCES faculty (id);

ALTER TABLE enrollments
    ADD CONSTRAINT fk_enrollments_student FOREIGN KEY (student_id) REFERENCES students (student_id);

ALTER TABLE enrollments
    ADD CONSTRAINT fk_enrollments_enrollment_period FOREIGN KEY (enrollment_period_id) REFERENCES enrollment_period (id);

ALTER TABLE enrollments
    ADD CONSTRAINT fk_enrollments_approved_by FOREIGN KEY (approved_by) REFERENCES users (id);

ALTER TABLE enrollments_details
    ADD CONSTRAINT fk_ed_enrollment FOREIGN KEY (enrollment_id) REFERENCES enrollments (id);

ALTER TABLE enrollments_details
    ADD CONSTRAINT fk_ed_offering FOREIGN KEY (offering_id) REFERENCES offerings (id);

ALTER TABLE faculty_student_drop_requests
    ADD CONSTRAINT fk_drop_requests_faculty FOREIGN KEY (faculty_id) REFERENCES faculty (id);

ALTER TABLE faculty_student_drop_requests
    ADD CONSTRAINT fk_drop_requests_student FOREIGN KEY (student_id) REFERENCES students (student_id);

ALTER TABLE faculty_student_drop_requests
    ADD CONSTRAINT fk_drop_requests_offering FOREIGN KEY (offering_id) REFERENCES offerings (id);

ALTER TABLE offerings
    ADD CONSTRAINT fk_offerings_subject FOREIGN KEY (subject_id) REFERENCES subjects (id);

ALTER TABLE offerings
    ADD CONSTRAINT fk_offerings_section FOREIGN KEY (section_id) REFERENCES sections (id);

ALTER TABLE offerings
    ADD CONSTRAINT fk_offerings_enrollment_period FOREIGN KEY (enrollment_period_id) REFERENCES enrollment_period (id);

ALTER TABLE offerings
    ADD CONSTRAINT fk_offerings_semester_subject_map
        FOREIGN KEY (semester_subject_id, subject_id) REFERENCES semester_subjects (id, subject_id);

ALTER TABLE courses
    ADD CONSTRAINT fk_courses_dept FOREIGN KEY (department_id) REFERENCES departments (id);

ALTER TABLE prerequisites
    ADD CONSTRAINT fk_prereq_presubject FOREIGN KEY (pre_subject_id) REFERENCES subjects (id);

ALTER TABLE prerequisites
    ADD CONSTRAINT fk_prereq_subject FOREIGN KEY (subject_id) REFERENCES subjects (id);
