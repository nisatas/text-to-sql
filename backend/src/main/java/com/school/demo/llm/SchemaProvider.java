package com.school.demo.llm;

import org.springframework.stereotype.Component;

@Component
public class SchemaProvider {

    public String getSchemaDescription() {
        return """
                Database: PostgreSQL

                Tables:

                1) classes
                - id (integer, primary key)
                - class_name (varchar)   -- examples: 9-A, 12-B (format: {grade}-{section})
                - branch (varchar)       -- examples: Sayisal, Sozel
                - grade_level (smallint) -- 9, 10, 11, or 12; use for "9. sinif" / "tum 9lar" questions

                2) students
                - id (integer, primary key)
                - name (varchar)
                - student_number (varchar)
                - class_id (integer, foreign key -> classes.id)

                3) grades
                - id (integer, primary key)
                - student_id (integer, foreign key -> students.id)
                - subject (varchar)      -- examples: Matematik, Fizik, Kimya
                - score (integer, 0-100)

                Relationships:
                - classes.id = students.class_id
                - students.id = grades.student_id
                """;
    }
}