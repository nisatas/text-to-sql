TRUNCATE TABLE grades, students, classes RESTART IDENTITY CASCADE;

INSERT INTO classes (id, class_name, branch) VALUES
(1, '9-A', 'Sayisal'),
(2, '9-B', 'Sozel'),
(3, '10-A', 'Sayisal'),
(4, '10-B', 'Sozel'),
(5, '11-A', 'Sayisal'),
(6, '11-B', 'Sozel'),
(7, '12-A', 'Sayisal'),
(8, '12-B', 'Sozel');

INSERT INTO students (id, name, student_number, class_id) VALUES
(1, 'Ahmet Yilmaz', '1001', 1),
(2, 'Ayse Demir', '1002', 1),
(3, 'Mehmet Kaya', '1003', 1),
(4, 'Zeynep Arslan', '1004', 1),
(5, 'Can Ozturk', '1005', 1),
(6, 'Elif Koc', '1006', 1),
(7, 'Mert Aydin', '1007', 1),

(8, 'Sude Celik', '1008', 2),
(9, 'Eren Sahin', '1009', 2),
(10, 'Buse Kurt', '1010', 2),
(11, 'Kerem Yildiz', '1011', 2),
(12, 'Duru Aksoy', '1012', 2),
(13, 'Emir Polat', '1013', 2),
(14, 'Naz Kaya', '1014', 2),

(15, 'Arda Demir', '1015', 3),
(16, 'Irem Yilmaz', '1016', 3),
(17, 'Yusuf Tas', '1017', 3),
(18, 'Melis Gunes', '1018', 3),
(19, 'Burak Eren', '1019', 3),
(20, 'Ceren Yildirim', '1020', 3),
(21, 'Omer Karaca', '1021', 3),

(22, 'Nisa Acar', '1022', 4),
(23, 'Kaan Sari', '1023', 4),
(24, 'Esra Bulut', '1024', 4),
(25, 'Furkan Kilic', '1025', 4),
(26, 'Dilara Sen', '1026', 4),
(27, 'Umut Dogan', '1027', 4),
(28, 'Selin Ece', '1028', 4),

(29, 'Baris Yaman', '1029', 5),
(30, 'Gamze Tasci', '1030', 5),
(31, 'Tolga Cinar', '1031', 5),
(32, 'Mina Kose', '1032', 5),
(33, 'Onur Tekin', '1033', 5),
(34, 'Ece Nur', '1034', 5),
(35, 'Ali Riza', '1035', 5),

(36, 'Pelin Ates', '1036', 6),
(37, 'Sinan Tunc', '1037', 6),
(38, 'Rabia Cetin', '1038', 6),
(39, 'Hakan Ucar', '1039', 6),
(40, 'Gizem Er', '1040', 6),
(41, 'Deniz Ak', '1041', 6),
(42, 'Yagiz Korkmaz', '1042', 6),

(43, 'Berk Yildiz', '1043', 7),
(44, 'Sila Karaman', '1044', 7),
(45, 'Merve Tas', '1045', 7),
(46, 'Batuhan Demir', '1046', 7),
(47, 'Asli Koc', '1047', 7),
(48, 'Kubra Sahin', '1048', 7),
(49, 'Tuna Acar', '1049', 7),

(50, 'Defne Yilmaz', '1050', 8),
(51, 'Ege Kurt', '1051', 8),
(52, 'Sevgi Aras', '1052', 8),
(53, 'Musa Kaplan', '1053', 8),
(54, 'Aleyna Sari', '1054', 8),
(55, 'Recep Tas', '1055', 8),
(56, 'Cansu Eren', '1056', 8);

WITH subjects AS (
    SELECT 'Matematik' AS subject UNION ALL
    SELECT 'Fizik' UNION ALL
    SELECT 'Kimya' UNION ALL
    SELECT 'Biyoloji' UNION ALL
    SELECT 'Turkce' UNION ALL
    SELECT 'Tarih' UNION ALL
    SELECT 'Ingilizce'
)
INSERT INTO grades (student_id, subject, score)
SELECT
    s.id,
    sub.subject,
    GREATEST(
        35,
        LEAST(
            100,
            (
                CASE
                    WHEN s.class_id = 1 THEN
                        CASE sub.subject
                            WHEN 'Matematik' THEN 68
                            WHEN 'Fizik' THEN 66
                            WHEN 'Kimya' THEN 64
                            WHEN 'Biyoloji' THEN 70
                            WHEN 'Turkce' THEN 62
                            WHEN 'Tarih' THEN 60
                            WHEN 'Ingilizce' THEN 65
                        END
                    WHEN s.class_id = 2 THEN
                        CASE sub.subject
                            WHEN 'Matematik' THEN 50
                            WHEN 'Fizik' THEN 48
                            WHEN 'Kimya' THEN 52
                            WHEN 'Biyoloji' THEN 58
                            WHEN 'Turkce' THEN 74
                            WHEN 'Tarih' THEN 78
                            WHEN 'Ingilizce' THEN 66
                        END
                    WHEN s.class_id = 3 THEN
                        CASE sub.subject
                            WHEN 'Matematik' THEN 76
                            WHEN 'Fizik' THEN 73
                            WHEN 'Kimya' THEN 71
                            WHEN 'Biyoloji' THEN 75
                            WHEN 'Turkce' THEN 67
                            WHEN 'Tarih' THEN 65
                            WHEN 'Ingilizce' THEN 70
                        END
                    WHEN s.class_id = 4 THEN
                        CASE sub.subject
                            WHEN 'Matematik' THEN 54
                            WHEN 'Fizik' THEN 52
                            WHEN 'Kimya' THEN 55
                            WHEN 'Biyoloji' THEN 60
                            WHEN 'Turkce' THEN 72
                            WHEN 'Tarih' THEN 76
                            WHEN 'Ingilizce' THEN 68
                        END
                    WHEN s.class_id = 5 THEN
                        CASE sub.subject
                            WHEN 'Matematik' THEN 84
                            WHEN 'Fizik' THEN 81
                            WHEN 'Kimya' THEN 79
                            WHEN 'Biyoloji' THEN 82
                            WHEN 'Turkce' THEN 74
                            WHEN 'Tarih' THEN 70
                            WHEN 'Ingilizce' THEN 76
                        END
                    WHEN s.class_id = 6 THEN
                        CASE sub.subject
                            WHEN 'Matematik' THEN 58
                            WHEN 'Fizik' THEN 56
                            WHEN 'Kimya' THEN 59
                            WHEN 'Biyoloji' THEN 63
                            WHEN 'Turkce' THEN 80
                            WHEN 'Tarih' THEN 84
                            WHEN 'Ingilizce' THEN 72
                        END
                    WHEN s.class_id = 7 THEN
                        CASE sub.subject
                            WHEN 'Matematik' THEN 88
                            WHEN 'Fizik' THEN 86
                            WHEN 'Kimya' THEN 84
                            WHEN 'Biyoloji' THEN 85
                            WHEN 'Turkce' THEN 78
                            WHEN 'Tarih' THEN 75
                            WHEN 'Ingilizce' THEN 80
                        END
                    WHEN s.class_id = 8 THEN
                        CASE sub.subject
                            WHEN 'Matematik' THEN 62
                            WHEN 'Fizik' THEN 60
                            WHEN 'Kimya' THEN 64
                            WHEN 'Biyoloji' THEN 66
                            WHEN 'Turkce' THEN 82
                            WHEN 'Tarih' THEN 86
                            WHEN 'Ingilizce' THEN 75
                        END
                END
                + ((s.id % 5) * 3)
                - ((s.id % 3) * 2)
                + CASE
                    WHEN sub.subject = 'Matematik' AND s.id IN (2, 9, 22, 37, 54) THEN -18
                    WHEN sub.subject = 'Fizik' AND s.id IN (4, 11, 24, 39, 55) THEN -16
                    WHEN sub.subject = 'Kimya' AND s.id IN (6, 13, 26, 41, 56) THEN -14
                    WHEN sub.subject = 'Turkce' AND s.id IN (1, 15, 29, 43, 50) THEN 8
                    WHEN sub.subject = 'Tarih' AND s.id IN (8, 12, 36, 40, 52) THEN 7
                    WHEN sub.subject = 'Ingilizce' AND s.id IN (18, 21, 33, 47, 49) THEN 6
                    ELSE 0
                  END
            )
        )
    ) AS score
FROM students s
CROSS JOIN subjects sub;

SELECT setval('classes_id_seq', (SELECT MAX(id) FROM classes));
SELECT setval('students_id_seq', (SELECT MAX(id) FROM students));
SELECT setval('grades_id_seq', (SELECT MAX(id) FROM grades));