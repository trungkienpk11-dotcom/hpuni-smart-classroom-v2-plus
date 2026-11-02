
INSERT INTO rooms
(code, name, capacity, room_type, projector, computers)
SELECT 'A101', 'Phòng học A101', 40, 'NORMAL', TRUE, FALSE
    WHERE NOT EXISTS (
    SELECT 1 FROM rooms WHERE code = 'A101'
);

INSERT INTO rooms
(code, name, capacity, room_type, projector, computers)
SELECT 'A102', 'Phòng học A102', 60, 'NORMAL', TRUE, FALSE
    WHERE NOT EXISTS (
    SELECT 1 FROM rooms WHERE code = 'A102'
);

INSERT INTO rooms
(code, name, capacity, room_type, projector, computers)
SELECT 'B201', 'Phòng máy B201', 45, 'COMPUTER_LAB', TRUE, TRUE
    WHERE NOT EXISTS (
    SELECT 1 FROM rooms WHERE code = 'B201'
);

INSERT INTO teachers (code, full_name, email)
SELECT 'GV001', 'Nguyễn Văn An', 'an@example.edu.vn'
    WHERE NOT EXISTS (
    SELECT 1 FROM teachers WHERE code = 'GV001'
);

INSERT INTO teachers (code, full_name, email)
SELECT 'GV002', 'Trần Thị Mai', 'mai@example.edu.vn'
    WHERE NOT EXISTS (
    SELECT 1 FROM teachers WHERE code = 'GV002'
);

INSERT INTO student_groups
(code, name, student_count)
SELECT 'CNTT01', 'Công nghệ thông tin 01', 40
    WHERE NOT EXISTS (
    SELECT 1 FROM student_groups
    WHERE code = 'CNTT01'
);

INSERT INTO student_groups
(code, name, student_count)
SELECT 'CNTT02', 'Công nghệ thông tin 02', 45
    WHERE NOT EXISTS (
    SELECT 1 FROM student_groups
    WHERE code = 'CNTT02'
);

INSERT INTO courses (code, name, credits)
SELECT 'JAVA01', 'Lập trình Java', 3
    WHERE NOT EXISTS (
    SELECT 1 FROM courses WHERE code = 'JAVA01'
);

INSERT INTO courses (code, name, credits)
SELECT 'DB01', 'Cơ sở dữ liệu', 3
    WHERE NOT EXISTS (
    SELECT 1 FROM courses WHERE code = 'DB01'
);

INSERT INTO course_offerings
(course_id, teacher_id, group_id,
 semester, academic_year, sessions_per_week)

SELECT
    c.id, t.id, g.id,
    'HK1', '2026-2027', 2

FROM courses c
         JOIN teachers t ON t.code = 'GV001'
         JOIN student_groups g ON g.code = 'CNTT01'

WHERE c.code = 'JAVA01'
  AND NOT EXISTS (
    SELECT 1 FROM course_offerings o
    WHERE o.course_id = c.id
      AND o.group_id = g.id
      AND o.semester = 'HK1'
      AND o.academic_year = '2026-2027'
);

INSERT INTO course_offerings
(course_id, teacher_id, group_id,
 semester, academic_year, sessions_per_week)

SELECT
    c.id, t.id, g.id,
    'HK1', '2026-2027', 2

FROM courses c
         JOIN teachers t ON t.code = 'GV002'
         JOIN student_groups g ON g.code = 'CNTT02'

WHERE c.code = 'DB01'
  AND NOT EXISTS (
    SELECT 1 FROM course_offerings o
    WHERE o.course_id = c.id
      AND o.group_id = g.id
      AND o.semester = 'HK1'
      AND o.academic_year = '2026-2027'
);
