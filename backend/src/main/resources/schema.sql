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
    score INTEGER CHECK (score >= 0 AND score <= 100)
);

-- Mevcut kurulumlarda sütun yoksa ekler (Spring init tekrar çalıştığında idempotent)
ALTER TABLE classes ADD COLUMN IF NOT EXISTS grade_level SMALLINT;

UPDATE classes
SET grade_level = split_part(class_name, '-', 1)::smallint
WHERE grade_level IS NULL
  AND split_part(class_name, '-', 1) ~ '^[0-9]+$';

ALTER TABLE classes DROP CONSTRAINT IF EXISTS classes_grade_level_chk;
ALTER TABLE classes ADD CONSTRAINT classes_grade_level_chk
    CHECK (grade_level IS NULL OR (grade_level >= 9 AND grade_level <= 12));
