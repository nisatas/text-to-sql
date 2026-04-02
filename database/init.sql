-- Referans şema (manuel kurulum / dokümantasyon).
-- Uygulama açılışında kullanılan güncel şema: backend/src/main/resources/schema.sql
-- pgAdmin ile toplu veri: database/pgadmin_expand.sql

CREATE TABLE IF NOT EXISTS classes (
    id SERIAL PRIMARY KEY,
    class_name VARCHAR(20) NOT NULL,
    branch VARCHAR(50),
    grade_level SMALLINT
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
