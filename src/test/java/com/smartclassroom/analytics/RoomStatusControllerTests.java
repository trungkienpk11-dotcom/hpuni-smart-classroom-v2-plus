package com.smartclassroom.analytics;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:room-status-tests;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.sql.init.mode=always"
})
@Transactional
class RoomStatusControllerTests {
    @Autowired private JdbcTemplate jdbc;
    @Autowired private RoomStatusController controller;

    private long id(String table, String code) {
        return jdbc.queryForObject("SELECT id FROM " + table + " WHERE code = ?",
                Long.class, code);
    }

    @Test
    void reportsAvailableOccupiedAndUnsuitableWithoutChangingSavedSchedule() {
        jdbc.update("INSERT INTO rooms(code,name,capacity,room_type,projector,computers) VALUES (?,?,?,?,?,?)",
                "TST_FREE", "Phòng trống", 60, "Lý thuyết", true, true);
        jdbc.update("INSERT INTO rooms(code,name,capacity,room_type,projector,computers) VALUES (?,?,?,?,?,?)",
                "TST_BUSY", "Phòng có lịch", 60, "Lý thuyết", true, true);
        jdbc.update("INSERT INTO rooms(code,name,capacity,room_type,projector,computers) VALUES (?,?,?,?,?,?)",
                "TST_SMALL", "Phòng nhỏ", 20, "Lý thuyết", false, false);
        jdbc.update("INSERT INTO teachers(code,full_name,email) VALUES (?,?,?)",
                "TST_T", "Giảng viên kiểm thử", "test-room-status@example.edu");
        jdbc.update("INSERT INTO courses(code,name,credits) VALUES (?,?,?)",
                "TST_C", "Môn kiểm thử", 3);
        jdbc.update("INSERT INTO student_groups(code,name,student_count) VALUES (?,?,?)",
                "TST_G", "Lớp kiểm thử", 30);
        jdbc.update("""
                INSERT INTO course_offerings
                (course_id, teacher_id, group_id, semester, academic_year, sessions_per_week)
                VALUES (?, ?, ?, ?, ?, ?)
                """, id("courses","TST_C"), id("teachers","TST_T"),
                id("student_groups","TST_G"), "HK1","2026-2027",2);
        long offering = jdbc.queryForObject(
                "SELECT id FROM course_offerings WHERE course_id = ?", Long.class, id("courses","TST_C"));
        jdbc.update("""
                INSERT INTO schedule_sessions(offering_id,room_id,session_date,start_time,end_time)
                VALUES(?,?,?,?,?)
                """, offering,id("rooms","TST_BUSY"),java.sql.Date.valueOf("2026-11-30"),
                java.sql.Time.valueOf("08:00:00"),java.sql.Time.valueOf("10:00:00"));

        LocalDate date = LocalDate.of(2026,11,30);
        var result = controller.roomStatus(date, LocalTime.of(9,0),LocalTime.of(11,0),40,true,true);
        Map<String,RoomStatusController.RoomStatus> rooms=result.rooms().stream()
                .collect(Collectors.toMap(RoomStatusController.RoomStatus::code, Function.identity()));

        assertEquals("AVAILABLE",rooms.get("TST_FREE").status());
        assertEquals("OCCUPIED",rooms.get("TST_BUSY").status());
        assertEquals(1,rooms.get("TST_BUSY").conflicts().size());
        assertEquals("TST_C",rooms.get("TST_BUSY").conflicts().get(0).courseCode());
        assertEquals("UNSUITABLE",rooms.get("TST_SMALL").status());
        assertFalse(rooms.get("TST_SMALL").meetsRequirements());
        assertTrue(rooms.get("TST_SMALL").reasons().size() >= 2);

        // Touching at 10:00 is not overlapping with the saved 08:00–10:00 session.
        var later = controller.roomStatus(date, LocalTime.of(10,0),LocalTime.of(11,0),40,true,true);
        var busyRoomLater = later.rooms().stream().filter(r -> r.code().equals("TST_BUSY"))
                .findFirst().orElseThrow();
        assertEquals("AVAILABLE",busyRoomLater.status());
        assertTrue(busyRoomLater.conflicts().isEmpty());
        // Read-only: no schedule rows were created or removed.
        Integer n=jdbc.queryForObject("SELECT COUNT(*) FROM schedule_sessions WHERE offering_id = ?",
                Integer.class,offering);
        assertEquals(1,n);
    }

    @Test
    void rejectsInvalidTimeAndCapacity() {
        LocalDate date=LocalDate.of(2026,11,30);
        assertThrows(ResponseStatusException.class, () -> controller.roomStatus(
                date,LocalTime.of(10,0),LocalTime.of(9,0),1,false,false));
        assertThrows(ResponseStatusException.class, () -> controller.roomStatus(
                date,LocalTime.of(9,0),LocalTime.of(10,0),0,false,false));
    }
}
