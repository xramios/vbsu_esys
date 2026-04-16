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
    school_year varchar(32) not null,
    semester    varchar(64) not null,
    start_date  TIMESTAMP   not null,
    end_date    TIMESTAMP   not null,
    description clob,
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
    id         bigint NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email      varchar(64) UNIQUE NOT NULL,
    password   char(60),
    role       varchar(16) CHECK (role IN ('STUDENT', 'REGISTRAR', 'FACULTY', 'ADMIN')),
    created_at timestamp default current_timestamp,
    updated_at timestamp default current_timestamp
);

/**
 * Stores temporary tokens for password reset functionality.
 * Tokens are linked to a user and have an expiration timestamp.
 * Used to verify the identity of a user attempting to reset their password via email.
 */
CREATE TABLE password_reset_tokens
(
    id          bigint NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id     bigint NOT NULL REFERENCES users(id),
    token       varchar(128) UNIQUE NOT NULL,
    expires_at  timestamp NOT NULL,
    created_at  timestamp default current_timestamp,
    used_at     timestamp
);

/**
* Contains administrator information and links to user accounts.
* Admins have elevated permissions for managing the system.
* Stores contact information and employee ID for administrative staff.
*/
CREATE TABLE admins
(
    id             bigint NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id        bigint,
    first_name     varchar(64),
    last_name      varchar(64),
    contact_number varchar(20),
    updated_at     timestamp default current_timestamp,
    created_at     timestamp default current_timestamp
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
    first_name     varchar(64) NOT NULL,
    last_name      varchar(64) NOT NULL,
    middle_name    varchar(32),
    birthdate      date         NOT NULL,
    student_status varchar(16) DEFAULT 'REGULAR' CHECK (student_status IN ('REGULAR', 'IRREGULAR')),
    course_id      bigint,
    curriculum_id  bigint,
    year_level     int       default 1,
    created_at     timestamp default current_timestamp,
    updated_at     timestamp default current_timestamp
);

/**
 * Catalog of all subjects/courses offered by the university.
 * Contains subject details including name, code, units, and description.
 * Linked to department for academic organization.
 */
CREATE TABLE subjects
(
    id            bigint NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    subject_name  varchar(64),
    subject_code  varchar(32),
    units         float,
    estimated_time int default 90,
    schedule_pattern varchar(32) default 'LECTURE_ONLY' CHECK (schedule_pattern IN ('LECTURE_ONLY', 'LECTURE_LAB', 'GE_PAIRED', 'PE_PAIRED', 'NSTP_BLOCK')),
    description   clob,
    department_id bigint,
    updated_at    timestamp default current_timestamp,
    created_at    timestamp default current_timestamp
);

/**
 * Represents class sections independent of specific subjects.
 * Sections act as shared class groups with capacity limits and reusable schedules.
 */
CREATE TABLE sections
(
    id           bigint NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    section_code varchar(64) NOT NULL,
    capacity     int    NOT NULL,
    status      varchar(16) CHECK (status IN ('OPEN', 'CLOSED', 'WAITLIST', 'DISSOLVED')) DEFAULT 'OPEN',
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
    id         bigint NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name       varchar(64) UNIQUE,
    cur_year   date,
    course     bigint,
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
    semester      varchar(64) NOT NULL,
    year_level int NOT NULL,
    created_at    timestamp default current_timestamp,
    updated_at    timestamp default current_timestamp,
    UNIQUE (curriculum_id, semester, year_level)
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
    created_at  timestamp default current_timestamp,
    updated_at  timestamp default current_timestamp,
    UNIQUE (semester_id, subject_id),
    UNIQUE (id, subject_id)
);

/**
 * Tracks all subjects that students have enrolled in or completed.
 * Links actual enrolled offerings to planned semester subjects with status tracking.
 * Essential for prerequisite checking and academic history tracking.
 *
 * Note: This table is needed to track which subjects students have taken,
 * allowing the system to check prerequisites for future enrollments.
 * Students can take subjects incrementally per year.
 */
CREATE TABLE student_enrolled_subjects
(
    student_id          varchar(32) NOT NULL,
    enrollment_id       bigint      NOT NULL,
    offering_id         bigint      NOT NULL,
    semester_subject_id bigint      NOT NULL,
    status              varchar(20) CHECK (status IN ('ENROLLED', 'COMPLETED', 'DROPPED')) NOT NULL DEFAULT 'ENROLLED',
    is_selected         boolean NOT NULL DEFAULT false,
    created_at          timestamp default current_timestamp,
    updated_at          timestamp default current_timestamp
);

/**
 * Tracks semester-level progress for each student within a curriculum.
 * One record represents a student's progress for a curriculum semester
 * and is marked as NOT_STARTED, IN_PROGRESS, or COMPLETED.
 */
CREATE TABLE student_semester_progress
(
    id            bigint NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    student_id    varchar(32) NOT NULL,
    curriculum_id bigint      NOT NULL,
    semester_id   bigint      NOT NULL,
    status        varchar(16) CHECK (status IN ('NOT_STARTED', 'IN_PROGRESS', 'COMPLETED')) NOT NULL DEFAULT 'NOT_STARTED',
    started_at    timestamp,
    completed_at  timestamp,
    created_at    timestamp default current_timestamp,
    updated_at    timestamp default current_timestamp
);

/**
 * Physical classroom and facility inventory.
 * Stores room identifiers and seating capacity for scheduling purposes.
 * Used by the scheduling system to assign sections to available rooms.
 */
CREATE TABLE rooms
(
    id         bigint      NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    building varchar(64) NOT NULL,
    room_type varchar(32) NOT NULL CHECK (room_type IN ('LECTURE', 'LAB', 'SEMINAR', 'AUDITORIUM', 'OTHER')),
    status varchar(16) NOT NULL CHECK (status IN ('AVAILABLE', 'UNAVAILABLE', 'MAINTENANCE')) DEFAULT 'AVAILABLE',
    room       varchar(32) NOT NULL,
    capacity   int         NOT NULL,
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
    first_name    varchar(64),
    last_name     varchar(64),
    middle_name   varchar(32),
    contact_number varchar(20),
    birthdate     date,
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
 * Class schedule assignments linking offerings to rooms, faculty, and time slots.
 * Defines when and where a concrete class offering meets (day of week, start/end times).
 * Prevents scheduling conflicts for the same offering instance.
 */
CREATE TABLE schedules
(
    id          bigint NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    offering_id bigint NOT NULL,
    room_id     bigint,
    faculty_id  bigint,
    day         varchar(3) CHECK (day IN ('MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT', 'SUN')),
    start_time  time,
    end_time    time,
    updated_at  timestamp default current_timestamp,
    created_at  timestamp default current_timestamp,
    UNIQUE (offering_id, day, start_time)
);

/**
 * Concrete class offerings for a specific enrollment period.
 * Acts as the single source of truth for enrollable classes by pairing
 * subject, section, and term-specific capacity in one record.
 * Optionally maps to planned curriculum entries via semester_subjects.
 */
CREATE TABLE offerings
(
    id                   bigint NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    subject_id           bigint NOT NULL,
    section_id           bigint NOT NULL,
    enrollment_period_id bigint NOT NULL,
    semester_subject_id  bigint,
    capacity             int,
    created_at           timestamp default current_timestamp,
    UNIQUE (subject_id, section_id, enrollment_period_id)
);

/**
 * Enrollment records tracking a student's enrollment for a specific term.
 * Records the school year, semester, enrollment status, and unit limits.
 * Status workflow: DRAFT -> SUBMITTED -> APPROVED -> ENROLLED -> COMPLETED (or CANCELLED).
 */
CREATE TABLE enrollments
(
    id                   bigint NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    student_id           varchar(32) NOT NULL,
    enrollment_period_id bigint NOT NULL,
    status               varchar(20) NOT NULL CHECK (status IN ('DRAFT', 'SUBMITTED', 'APPROVED', 'ENROLLED', 'COMPLETED', 'CANCELLED')),
    max_units            float,
    total_units          float,
    submitted_at         TIMESTAMP,
    approved_by bigint REFERENCES users (id),
    approved_at TIMESTAMP,
    updated_at           timestamp default current_timestamp,
    created_at           timestamp default current_timestamp,
    UNIQUE (student_id, enrollment_period_id)
);

/**
 * Detailed line items for each enrollment record.
 * Specifies which concrete offerings the student selected.
 * Tracks unit counts and allows for selective dropping of offerings.
 */
CREATE TABLE enrollments_details
(
    id            bigint NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    enrollment_id bigint NOT NULL,
    offering_id   bigint NOT NULL,
    units         float,
    status        varchar(20) CHECK (status IN ('SELECTED', 'DROPPED')),
    created_at    timestamp default current_timestamp,
    updated_at    timestamp default current_timestamp,
    UNIQUE (enrollment_id, offering_id)
);


/**
 * Academic programs/degrees offered by the university.
 * Each course belongs to a department and has a descriptive overview.
 * Students are enrolled in a specific course as their primary program.
 */
CREATE TABLE courses
(
    id            bigint NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    course_name   varchar(64),
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
    department_name varchar(64),
    department_code varchar(24),
    description     clob,
    updated_at      timestamp default current_timestamp,
    created_at      timestamp default current_timestamp
);

/**
    * Tracks requests from faculty to drop a student from an offering.
    * Allows faculty to submit drop requests with reasons, which can be approved or rejected by the registrar.
    * Ensures that students cannot be dropped from offerings without proper authorization and documentation.
**/
CREATE TABLE faculty_student_drop_requests (
    id bigint NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    faculty_id bigint NOT NULL,
    student_id varchar(32) NOT NULL,
    offering_id bigint NOT NULL,
    reason clob,
    status varchar(20) CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')) DEFAULT 'PENDING',
    created_at timestamp default current_timestamp,
    updated_at timestamp default current_timestamp,
    UNIQUE (student_id, offering_id)
);

CREATE UNIQUE INDEX student_enrolled_subjects_index_0 ON student_enrolled_subjects (student_id, offering_id);
CREATE UNIQUE INDEX prerequisites_unique_pair_index_0 ON prerequisites (pre_subject_id, subject_id);

CREATE UNIQUE INDEX student_semester_progress_index_0 ON student_semester_progress (student_id, semester_id);

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

ALTER TABLE schedules
    ADD CONSTRAINT fk_schedules_offering FOREIGN KEY (offering_id) REFERENCES offerings (id);

ALTER TABLE schedules
    ADD CONSTRAINT fk_schedules_room FOREIGN KEY (room_id) REFERENCES rooms (id);

ALTER TABLE schedules
    ADD CONSTRAINT fk_schedules_faculty FOREIGN KEY (faculty_id) REFERENCES faculty (id);

ALTER TABLE enrollments
    ADD CONSTRAINT fk_enrollments_student FOREIGN KEY (student_id) REFERENCES students (student_id);

ALTER TABLE enrollments_details
    ADD CONSTRAINT fk_ed_enrollment FOREIGN KEY (enrollment_id) REFERENCES enrollments (id);

ALTER TABLE enrollments_details
    ADD CONSTRAINT fk_ed_offering FOREIGN KEY (offering_id) REFERENCES offerings (id);

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

ALTER TABLE courses
    ADD CONSTRAINT fk_courses_dept FOREIGN KEY (department_id) REFERENCES departments (id);

ALTER TABLE prerequisites
    ADD CONSTRAINT fk_prereq_presubject FOREIGN KEY (pre_subject_id) REFERENCES subjects (id);

ALTER TABLE prerequisites
    ADD CONSTRAINT fk_prereq_subject FOREIGN KEY (subject_id) REFERENCES subjects (id);

ALTER TABLE enrollments
    ADD CONSTRAINT fk_enrollments_enrollment_period FOREIGN KEY (enrollment_period_id) REFERENCES enrollment_period (id);

ALTER TABLE offerings
    ADD CONSTRAINT fk_offerings_subject FOREIGN KEY (subject_id) REFERENCES subjects (id);

ALTER TABLE offerings
    ADD CONSTRAINT fk_offerings_section FOREIGN KEY (section_id) REFERENCES sections (id);

ALTER TABLE offerings
    ADD CONSTRAINT fk_offerings_enrollment_period FOREIGN KEY (enrollment_period_id) REFERENCES enrollment_period (id);

ALTER TABLE offerings
    ADD CONSTRAINT fk_offerings_semester_subject_map
        FOREIGN KEY (semester_subject_id, subject_id) REFERENCES semester_subjects (id, subject_id);
C R E A T E   T A B L E   a u d i t _ l o g s   ( i d   b i g i n t   N O T   N U L L   G E N E R A T E D   A L W A Y S   A S   I D E N T I T Y   P R I M A R Y   K E Y ,   u s e r _ i d   v a r c h a r ( 6 4 ) ,   a c t i o n   v a r c h a r ( 6 4 )   N O T   N U L L ,   d e t a i l s   c l o b ,   c r e a t e d _ a t   t i m e s t a m p   D E F A U L T   C U R R E N T _ T I M E S T A M P ) ; 
 
 