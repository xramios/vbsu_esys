# Real Data Seeder

Seeds real data from CSV into selected tables.

## Scope

Supported targets:

- `bundle` (clean reset + BSIT + NITEN2023 + students + faculty + registrar + admin)
- `curriculum`
- `subjects`
- `semester`
- `semester_subjects`
- `prerequisites`
- `users` (student accounts)
- `students`

It does not seed schedules, enrollments, or offerings.

## Input Format

The importer is designed for the provided curriculum CSV format with these relevant columns:

- `subject_code`
- `subject_name`
- `units`
- `estimated_time` is automatically set to `90` minutes when the column exists
- `schedule_pattern` is automatically inferred (lecture/lab/GE/PE/NSTP) when the column exists
- `prerequisite` (uses only `P-` prerequisite codes)
- `year`
- `semester`

## Usage

### One-Command Bundle Target (Recommended)

This is the simple reset workflow. For Derby, it drops known APP tables and reruns `src/main/resources/db/derby.sql` before seeding.
For MySQL, it performs table clears.

Then it seeds:

- Course: BSIT (`Bachelor of Science in Information Technology`)
- Curriculum: `NITEN2023`
- Students from `docs/students.csv`
- Student semester progress baseline (`NOT_STARTED` at first-year first-semester)
- Faculty: Faker-generated, assigned to `College of Engineering`
- Registrar account: `registrar@vbsu.edu.ph` with password `12345678`
- Admin account: `admin@vbsu.edu.ph` with password `12345678`

Run from the `scripts` directory:

```bash
python -m real_seeder.cli \
  --db-type derby \
  --database university_db \
  --user app \
  --password derby \
  --faculty-count 10
```

`bundle` is the default target, so `--target bundle` is optional.

MySQL example:

```bash
python -m real_seeder.cli \
  --db-type mysql \
  --host localhost \
  --port 3306 \
  --database university_db \
  --user root \
  --password "" \
  --faculty-count 10
```

### Curriculum Target

Run from the `scripts` directory:

```bash
python -m real_seeder.cli
```

With explicit Derby connection:

```bash
python -m real_seeder.cli \
  --db-type derby \
  --host localhost \
  --database sample \
  --user APP \
  --password derby
```

If the target course is missing and you want the script to create it:

```bash
python -m real_seeder.cli \
  --target curriculum \
  --course-name "Bachelor of Science in Information Technology" \
  --create-course-if-missing
```

With explicit MySQL connection:

```bash
python -m real_seeder.cli \
  --target curriculum \
  --db-type mysql \
  --host localhost \
  --port 3306 \
  --database university_db \
  --user root \
  --password ""
```

### Students Target

Seed users + students from `docs/students.csv` with fixed course/curriculum mapping:

```bash
python -m real_seeder.cli \
  --target students \
  --db-type derby \
  --database university_db \
  --user app \
  --password derby \
  --students-csv ../docs/students.csv \
  --students-course BSIT \
  --students-curriculum NITEN2023
```

MySQL example:

```bash
python -m real_seeder.cli \
  --target students \
  --db-type mysql \
  --host localhost \
  --port 3306 \
  --database university_db \
  --user root \
  --password "" \
  --students-csv ../docs/students.csv \
  --students-course BSIT \
  --students-curriculum NITEN2023
```

Credential rules for student import:

- Email: `lastname.firstname@vbsu.edu.ph`
- Plain password basis: `lastname_YYYY` (YYYY = birth year)
- Stored password: bcrypt hash of the plain password basis

Credential rules for bundle faculty/registrar/admin seeding:

- Faculty count is dynamic via `--faculty-count` (default: `10`)
- Faculty email pattern: `Faculty1@vbsu.edu.ph`, `Faculty2@vbsu.edu.ph`, and so on
- Faculty plain password basis: `LastName_YYYY-MM-DD` (same format used by FacultyForm)
- Faculty stored password: bcrypt hash of that plain basis
- Registrar email/password: `registrar@vbsu.edu.ph` / `12345678` (stored as bcrypt hash)
- Admin email/password: `admin@vbsu.edu.ph` / `12345678` (stored as bcrypt hash)

## Notes

- Default CSV: `docs/ENROLLMENT SYSTEM CCP FINALS - Curriculum NITEN2023.csv`
- Default curriculum name/year: `NITEN2023` / `2023`
- Optional DB port override: `--port` (defaults from config when omitted)
- Subject names are capped to 32 chars by default to align with current schema constraints (`--subject-name-max-length`).
- Student target defaults: `--students-course BSIT`, `--students-curriculum NITEN2023`.
