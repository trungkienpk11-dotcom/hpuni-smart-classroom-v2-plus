package com.smartclassroom.schedule;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public class AutoScheduleRepository {

    private final JdbcTemplate jdbc;

    public AutoScheduleRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // Thông tin của một lớp học phần

    public record Offering(
            long id,
            long courseId,
            long teacherId,
            long groupId,
            String courseName,
            String teacherName,
            String groupCode,
            int studentCount,
            int sessionsPerWeek,
            boolean needsComputers,
            boolean needsProjector
    ) {}


    // Thông tin một buổi đã chiếm lịch
    public record Occupied(
            long offeringId,
            long roomId,
            long teacherId,
            long groupId,
            LocalDate date,
            LocalTime start,
            LocalTime end
    ) {}

    // 1. Lấy học phần cần xếp trong học kỳ
    public List<Offering> findOfferings(
            String semester,
            String academicYear) {

        String sql = """
            SELECT
                o.id,
                o.course_id,
                o.teacher_id,
                o.group_id,
                c.name AS course_name,
                t.full_name AS teacher_name,
                g.code AS group_code,
                g.student_count,
                o.sessions_per_week,
                COALESCE(rr.needs_computers, FALSE)
                    AS needs_computers,
                COALESCE(rr.needs_projector, FALSE)
                    AS needs_projector

            FROM course_offerings o

            JOIN courses c
                ON o.course_id = c.id
            LEFT JOIN course_room_requirements rr
                ON rr.course_id = c.id

            JOIN teachers t
                ON o.teacher_id = t.id

            JOIN student_groups g
                ON o.group_id = g.id

            WHERE o.semester = ?
              AND o.academic_year = ?

            ORDER BY g.student_count DESC,
                     o.sessions_per_week DESC,
                     o.id
            """;

        return jdbc.query(sql, (rs, rowNum) ->

                        new Offering(
                                rs.getLong("id"),
                                rs.getLong("course_id"),
                                rs.getLong("teacher_id"),
                                rs.getLong("group_id"),
                                rs.getString("course_name"),
                                rs.getString("teacher_name"),
                                rs.getString("group_code"),
                                rs.getInt("student_count"),
                                rs.getInt("sessions_per_week"),
                                rs.getBoolean("needs_computers"),
                                rs.getBoolean("needs_projector")
                        ),

                semester,
                academicYear
        );
    }

    // 2. Lấy tất cả buổi đã được xếp trong tuần
    public List<Occupied> findOccupied(LocalDate monday) {

        String sql = """
            SELECT
                s.offering_id,
                s.room_id,
                o.teacher_id,
                o.group_id,
                s.session_date,
                s.start_time,
                s.end_time

            FROM schedule_sessions s

            JOIN course_offerings o
                ON s.offering_id = o.id

            WHERE s.session_date BETWEEN ? AND ?

            ORDER BY s.session_date, s.start_time
            """;

        return jdbc.query(sql, (rs, rowNum) ->
                        new Occupied(
                                rs.getLong("offering_id"),
                                rs.getLong("room_id"),
                                rs.getLong("teacher_id"),
                                rs.getLong("group_id"),
                                rs.getDate("session_date").toLocalDate(),
                                rs.getTime("start_time").toLocalTime(),
                                rs.getTime("end_time").toLocalTime()
                        ),
                Date.valueOf(monday),
                Date.valueOf(monday.plusDays(6))
        );
    }
}
