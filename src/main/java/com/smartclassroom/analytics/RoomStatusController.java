package com.smartclassroom.analytics;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tinh trang TAT CA phong trong khoang thoi gian chon.
 * Chi doc schedule_sessions da luu; khong coi xep thu la lich chinh thuc.
 * Giu nguyen /api/upgrade/availability de tuong thich cac client cu.
 */
@RestController
@RequestMapping("/api/upgrade")
public class RoomStatusController {
    private final JdbcTemplate jdbc;

    public RoomStatusController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record ConflictingSession(
            long id, String courseCode, String courseName,
            String teacherName, String groupCode, String startTime, String endTime
    ) {}

    public record RoomStatus(
            long id, String code, String name, int capacity, String roomType,
            boolean computers, boolean projector,
            String status, boolean occupied, boolean meetsRequirements,
            List<String> reasons, List<ConflictingSession> conflicts
    ) {}

    public record RoomStatusResponse(
            LocalDate date, LocalTime start, LocalTime end,
            int totalRooms, int availableCount, int occupiedCount, int unsuitableCount,
            List<RoomStatus> rooms
    ) {}

    private static class RoomRow {
        final long id;
        final String code;
        final String name;
        final int capacity;
        final String roomType;
        final boolean computers;
        final boolean projector;
        final List<ConflictingSession> conflicts = new ArrayList<>();

        RoomRow(ResultSet rs) throws SQLException {
            id = rs.getLong("room_id");
            code = rs.getString("room_code");
            name = rs.getString("room_name");
            capacity = rs.getInt("capacity");
            roomType = rs.getString("room_type");
            computers = rs.getBoolean("computers");
            projector = rs.getBoolean("projector");
        }
    }

    @GetMapping("/room-status")
    public RoomStatusResponse roomStatus(
            @RequestParam("date") LocalDate date,
            @RequestParam("start") LocalTime start,
            @RequestParam("end") LocalTime end,
            @RequestParam(name = "capacity", defaultValue = "1") int capacity,
            @RequestParam(name = "computers", defaultValue = "false") boolean computers,
            @RequestParam(name = "projector", defaultValue = "false") boolean projector
    ) {
        if (date == null || start == null || end == null
                || !start.isBefore(end) || capacity < 1 || capacity > 10000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ngay, gio hoac suc chua khong hop le");
        }

        // Chi tinh la ban neu lich giao voi khoang [start, end).
        // Lich ket thuc dung luc start hoac bat dau dung luc end KHONG trung.
        Map<Long, RoomRow> byId = jdbc.query("""
                SELECT r.id AS room_id, r.code AS room_code,
                       r.name AS room_name, r.capacity, r.room_type,
                       r.computers, r.projector,
                       s.id AS session_id, s.start_time, s.end_time,
                       c.code AS course_code, c.name AS course_name,
                       t.full_name AS teacher_name, g.code AS group_code
                FROM rooms r
                LEFT JOIN schedule_sessions s
                  ON s.room_id = r.id
                 AND s.session_date = ?
                 AND s.start_time < ?
                 AND s.end_time > ?
                LEFT JOIN course_offerings o ON o.id = s.offering_id
                LEFT JOIN courses c ON c.id = o.course_id
                LEFT JOIN teachers t ON t.id = o.teacher_id
                LEFT JOIN student_groups g ON g.id = o.group_id
                ORDER BY r.code, s.start_time, s.id
                """, rs -> {
            Map<Long, RoomRow> rows = new LinkedHashMap<>();
            while (rs.next()) {
                long roomId = rs.getLong("room_id");
                RoomRow row = rows.get(roomId);
                if (row == null) {
                    row = new RoomRow(rs);
                    rows.put(roomId, row);
                }
                long sessionId = rs.getLong("session_id");
                if (!rs.wasNull()) {
                    row.conflicts.add(new ConflictingSession(
                            sessionId,
                            rs.getString("course_code"),
                            rs.getString("course_name"),
                            rs.getString("teacher_name"),
                            rs.getString("group_code"),
                            rs.getTime("start_time").toLocalTime().toString(),
                            rs.getTime("end_time").toLocalTime().toString()
                    ));
                }
            }
            return rows;
        }, Date.valueOf(date), Time.valueOf(end), Time.valueOf(start));

        List<RoomStatus> results = new ArrayList<>();
        int availableCount = 0, occupiedCount = 0, unsuitableCount = 0;
        for (RoomRow room : byId.values()) {
            boolean occupied = !room.conflicts.isEmpty();
            List<String> reasons = new ArrayList<>();
            if (occupied) reasons.add("Da co buoi hoc trung khung gio");
            if (room.capacity < capacity) reasons.add("Khong du suc chua (can " + capacity + " cho)");
            if (computers && !room.computers) reasons.add("Khong co may tinh");
            if (projector && !room.projector) reasons.add("Khong co may chieu");
            boolean meetsRequirements = room.capacity >= capacity
                    && (!computers || room.computers) && (!projector || room.projector);
            String status;
            if (occupied) {
                status = "OCCUPIED";
                occupiedCount++;
            } else if (!meetsRequirements) {
                status = "UNSUITABLE";
                unsuitableCount++;
            } else {
                status = "AVAILABLE";
                availableCount++;
            }
            results.add(new RoomStatus(
                    room.id, room.code, room.name, room.capacity, room.roomType,
                    room.computers, room.projector, status, occupied, meetsRequirements,
                    List.copyOf(reasons), List.copyOf(room.conflicts)
            ));
        }
        // Phong co the su dung hien truoc; khong loai bo phong da co lich.
        results.sort(Comparator.comparingInt((RoomStatus r) -> switch (r.status()) {
            case "AVAILABLE" -> 0;
            case "OCCUPIED" -> 1;
            default -> 2;
        }).thenComparing(RoomStatus::code));
        return new RoomStatusResponse(date, start, end,
                results.size(), availableCount, occupiedCount, unsuitableCount,
                List.copyOf(results));
    }
}
