
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
