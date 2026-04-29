CREATE TABLE IF NOT EXISTS classes (
    id SERIAL PRIMARY KEY,
    class_name VARCHAR(20) NOT NULL,
    branch VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS students (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    student_number VARCHAR(20) UNIQUE,
    class_id INTEGER REFERENCES classes(id)
);

CREATE TABLE IF NOT EXISTS grades (
    id SERIAL PRIMARY KEY,
    student_id INTEGER REFERENCES students(id),
    subject VARCHAR(50),
    exam_no SMALLINT,
    score INTEGER CHECK (score >= 0 AND score <= 100)
);

-- Ensure same name is not repeated within the same class
CREATE UNIQUE INDEX IF NOT EXISTS ux_students_class_name ON students (class_id, name);

-- Add exam_no for existing installs; default old rows to 1
ALTER TABLE grades ADD COLUMN IF NOT EXISTS exam_no SMALLINT;
UPDATE grades SET exam_no = 1 WHERE exam_no IS NULL;

-- exam_no must be 1 or 2 (two grades per subject)
ALTER TABLE grades DROP CONSTRAINT IF EXISTS grades_exam_no_chk;
ALTER TABLE grades ADD CONSTRAINT grades_exam_no_chk CHECK (exam_no IN (1, 2));

-- Uniqueness: one grade per (student, subject, exam_no)
CREATE UNIQUE INDEX IF NOT EXISTS ux_grades_student_subject_exam ON grades (student_id, subject, exam_no);

-- Mevcut kurulumlarda sütun yoksa ekler (Spring init tekrar çalıştığında idempotent)
ALTER TABLE classes ADD COLUMN IF NOT EXISTS grade_level SMALLINT;

UPDATE classes
SET grade_level = split_part(class_name, '-', 1)::smallint
WHERE grade_level IS NULL
  AND split_part(class_name, '-', 1) ~ '^[0-9]+$';

ALTER TABLE classes DROP CONSTRAINT IF EXISTS classes_grade_level_chk;
ALTER TABLE classes ADD CONSTRAINT classes_grade_level_chk
    CHECK (grade_level IS NULL OR (grade_level >= 9 AND grade_level <= 12));
