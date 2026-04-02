-- pgAdmin 4: Okul veritabanını genişletmek için
-- Bağlantı: kendi sunucun / veritabanın (ör. postgres veya ayrı DB)
-- Yedek aldıktan sonra Query Tool'da çalıştırın. Tekrar çalıştırmak güvenlidir (çoğu INSERT idempotent).

-- 1) Şema: sınıf seviyesi (9–12)
ALTER TABLE classes ADD COLUMN IF NOT EXISTS grade_level SMALLINT;

UPDATE classes
SET grade_level = split_part(class_name, '-', 1)::smallint
WHERE grade_level IS NULL
  AND split_part(class_name, '-', 1) ~ '^[0-9]+$';

ALTER TABLE classes DROP CONSTRAINT IF EXISTS classes_grade_level_chk;
ALTER TABLE classes ADD CONSTRAINT classes_grade_level_chk
    CHECK (grade_level IS NULL OR (grade_level >= 9 AND grade_level <= 12));

-- 2) Aşağıdaki blok, backend/src/main/resources/data.sql ile aynı seed mantığıdır.
-- İsterseniz data.sql dosyasını doğrudan pgAdmin'den de açıp çalıştırabilirsiniz.

INSERT INTO classes (class_name, branch, grade_level)
SELECT v.class_name, v.branch, v.grade_level
FROM (VALUES
    ('9-A', 'Sayısal', 9::smallint),
    ('9-B', 'Sözel', 9::smallint),
    ('10-A', 'Sayısal', 10::smallint),
    ('10-B', 'Sözel', 10::smallint),
    ('11-A', 'Sayısal', 11::smallint),
    ('12-A', 'Sayısal', 12::smallint),
    ('12-B', 'Sayısal', 12::smallint)
) AS v(class_name, branch, grade_level)
WHERE NOT EXISTS (SELECT 1 FROM classes c WHERE c.class_name = v.class_name);

UPDATE classes
SET grade_level = split_part(class_name, '-', 1)::smallint
WHERE grade_level IS NULL
  AND split_part(class_name, '-', 1) ~ '^[0-9]+$';

INSERT INTO students (name, student_number, class_id)
SELECT v.name, v.sn, c.id
FROM classes c
JOIN (VALUES
    ('9-A', 'Ayşe Yılmaz', '2024-0901'),
    ('9-A', 'Mehmet Kaya', '2024-0902'),
    ('9-A', 'Zeynep Demir', '2024-0903'),
    ('9-B', 'Can Öztürk', '2024-0904'),
    ('9-B', 'Elif Şahin', '2024-0905'),
    ('9-B', 'Burak Aydın', '2024-0906'),
    ('10-A', 'Selin Koç', '2023-1001'),
    ('10-A', 'Emre Çelik', '2023-1002'),
    ('10-B', 'Deniz Arslan', '2023-1003'),
    ('10-B', 'Cem Yıldız', '2023-1004'),
    ('11-A', 'Sude Polat', '2022-1101'),
    ('11-A', 'Kerem Güneş', '2022-1102'),
    ('12-A', 'Demo Öğrenci', 'DEMO-001'),
    ('12-A', 'Ali Vural', '2021-1201'),
    ('12-A', 'Ece Karaca', '2021-1202'),
    ('12-B', 'Onur Tekin', '2021-1203'),
    ('12-B', 'İrem Kurt', '2021-1204')
) AS v(class_name, name, sn) ON c.class_name = v.class_name
WHERE NOT EXISTS (SELECT 1 FROM students s WHERE s.student_number = v.sn);

INSERT INTO grades (student_id, subject, score)
SELECT s.id, v.subject, v.score
FROM students s
JOIN (VALUES
    ('2024-0901', 'Matematik', 88),
    ('2024-0901', 'Fizik', 76),
    ('2024-0901', 'Türkçe', 92),
    ('2024-0902', 'Matematik', 45),
    ('2024-0902', 'Fizik', 62),
    ('2024-0902', 'Kimya', 71),
    ('2024-0903', 'Matematik', 91),
    ('2024-0903', 'Biyoloji', 84),
    ('2024-0904', 'Matematik', 38),
    ('2024-0904', 'Türkçe', 72),
    ('2024-0905', 'Matematik', 95),
    ('2024-0905', 'Fizik', 88),
    ('2024-0906', 'Matematik', 52),
    ('2024-0906', 'Kimya', 49),
    ('2023-1001', 'Matematik', 78),
    ('2023-1001', 'Fizik', 81),
    ('2023-1002', 'Matematik', 44),
    ('2023-1002', 'Kimya', 67),
    ('2023-1003', 'Matematik', 73),
    ('2023-1003', 'Türkçe', 85),
    ('2023-1004', 'Matematik', 61),
    ('2023-1004', 'Fizik', 58),
    ('2022-1101', 'Matematik', 82),
    ('2022-1101', 'Kimya', 79),
    ('2022-1102', 'Matematik', 55),
    ('2022-1102', 'Fizik', 48),
    ('DEMO-001', 'Matematik', 42),
    ('DEMO-001', 'Fizik', 70),
    ('2021-1201', 'Matematik', 77),
    ('2021-1201', 'Kimya', 83),
    ('2021-1202', 'Matematik', 90),
    ('2021-1202', 'Fizik', 74),
    ('2021-1203', 'Matematik', 48),
    ('2021-1203', 'Türkçe', 69),
    ('2021-1204', 'Matematik', 86),
    ('2021-1204', 'Biyoloji', 91)
) AS v(sn, subject, score) ON s.student_number = v.sn
WHERE NOT EXISTS (
    SELECT 1 FROM grades g WHERE g.student_id = s.id AND g.subject = v.subject
);
