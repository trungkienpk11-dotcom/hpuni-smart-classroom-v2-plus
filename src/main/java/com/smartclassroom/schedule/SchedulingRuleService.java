package com.smartclassroom.schedule;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Date;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
public class SchedulingRuleService {

    private final JdbcTemplate jdbc;

    public SchedulingRuleService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // Dữ liệu nhận khi khai báo lịch bận
    public record BusyRequest(
            Long teacherId,
            LocalDate date,
            LocalTime start,
            LocalTime end,
            String reason
    ) {}

    // Dữ liệu lịch bận trả về giao diện
    public record TeacherBusy(
            Long id,
            Long teacherId,
            String teacherName,
            LocalDate date,
            LocalTime start,
            LocalTime end,
            String reason
    ) {}

    // Thông tin yêu cầu thiết bị theo môn
    public record CourseRule(
            Long courseId,
            String courseCode,
            String courseName,
            boolean needsComputers,
            boolean needsProjector
    ) {}

    // Dữ liệu nhận khi cấu hình môn
    public record CourseRuleRequest(
            Boolean needsComputers,
            Boolean needsProjector
    ) {}

    // 1. Lấy lịch bận của giảng viên trong tuần
    public List<TeacherBusy> getBusyForWeek(
            LocalDate weekStart) {

        if (weekStart == null) {
            throw badRequest("Thiếu ngày xem lịch");
        }

        LocalDate monday = weekStart.minusDays(
                (weekStart.getDayOfWeek().getValue() + 6) % 7
        );

        String sql = """
            SELECT b.id, b.teacher_id,
                   t.full_name AS teacher_name,
                   b.unavailable_date,
                   b.start_time, b.end_time,
                   b.reason
            FROM teacher_unavailability b
            JOIN teachers t ON b.teacher_id = t.id
            WHERE b.unavailable_date BETWEEN ? AND ?
            ORDER BY b.unavailable_date,
                     b.start_time, b.id
            """;

        return jdbc.query(sql, (rs, n) ->
                        new TeacherBusy(
                                rs.getLong("id"),
                                rs.getLong("teacher_id"),
                                rs.getString("teacher_name"),
                                rs.getDate("unavailable_date").toLocalDate(),
                                rs.getTime("start_time").toLocalTime(),
                                rs.getTime("end_time").toLocalTime(),
                                rs.getString("reason")
                        ),
                Date.valueOf(monday),
                Date.valueOf(monday.plusDays(6))
        );
    }

    // 2. Tạo lịch bận
    public void addBusy(BusyRequest data) {

        if (data == null ||
                data.teacherId() == null ||
                data.teacherId() <= 0 ||
                data.date() == null ||
                data.start() == null ||
                data.end() == null) {

            throw badRequest("Thiếu thông tin lịch bận");
        }

        if (!data.start().isBefore(data.end())) {
            throw badRequest(
                    "Giờ bắt đầu phải trước giờ kết thúc"
            );
        }

        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM teachers WHERE id = ?",
                Integer.class,
                data.teacherId()
        );

        if (count == null || count == 0) {
            throw badRequest("Giảng viên không tồn tại");
        }

        String reason = data.reason() == null
                ? ""
                : data.reason().trim();

        if (reason.length() > 200) {
            throw badRequest("Lý do tối đa 200 ký tự");
        }

        jdbc.update("""
            INSERT INTO teacher_unavailability
            (teacher_id, unavailable_date,
             start_time, end_time, reason)
            VALUES (?, ?, ?, ?, ?)
            """,
                data.teacherId(),
                Date.valueOf(data.date()),
                Time.valueOf(data.start()),
                Time.valueOf(data.end()),
                reason
        );
    }

    // 3. Xóa lịch bận
    public void deleteBusy(long id) {

        int deleted = jdbc.update(
                "DELETE FROM teacher_unavailability WHERE id = ?",
                id
        );

        if (deleted == 0) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Không tìm thấy lịch bận"
            );
        }
    }

    // 4. Danh sách môn và yêu cầu thiết bị
    public List<CourseRule> getCourseRules() {

        String sql = """
            SELECT c.id, c.code, c.name,
                   COALESCE(r.needs_computers, FALSE)
                       AS needs_computers,
                   COALESCE(r.needs_projector, FALSE)
                       AS needs_projector
            FROM courses c
            LEFT JOIN course_room_requirements r
                ON c.id = r.course_id
            ORDER BY c.code
            """;

        return jdbc.query(sql, (rs, n) ->
                new CourseRule(
                        rs.getLong("id"),
                        rs.getString("code"),
                        rs.getString("name"),
                        rs.getBoolean("needs_computers"),
                        rs.getBoolean("needs_projector")
                )
        );
    }

    // 5. Cập nhật yêu cầu thiết bị của môn
    @Transactional
    public void setCourseRule(
            long courseId,
            CourseRuleRequest data) {

        if (data == null ||
                data.needsComputers() == null ||
                data.needsProjector() == null) {

            throw badRequest(
                    "Phải cung cấp đủ yêu cầu thiết bị"
            );
        }

        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM courses WHERE id = ?",
                Integer.class,
                courseId
        );

        if (count == null || count == 0) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Môn học không tồn tại"
            );
        }

        int updated = jdbc.update("""
            UPDATE course_room_requirements
            SET needs_computers = ?,
                needs_projector = ?
            WHERE course_id = ?
            """,
                data.needsComputers(),
                data.needsProjector(),
                courseId
        );

        if (updated == 0) {
            jdbc.update("""
                INSERT INTO course_room_requirements
                (course_id, needs_computers, needs_projector)
                VALUES (?, ?, ?)
                """,
                    courseId,
                    data.needsComputers(),
                    data.needsProjector()
            );
        }
    }

    // Kiem tra phong co dap ung thiet bi cua mon hoc khong
    public boolean isRoomSuitable(
            long offeringId,
            boolean hasComputers,
            boolean hasProjector) {

        String sql = """
            SELECT
                COALESCE(r.needs_computers, FALSE)
                    AS needs_computers,
                COALESCE(r.needs_projector, FALSE)
                    AS needs_projector
            FROM course_offerings o
            LEFT JOIN course_room_requirements r
                ON r.course_id = o.course_id
            WHERE o.id = ?
            """;

        var requirements = jdbc.query(
                sql,
                (rs, n) -> new boolean[] {
                        rs.getBoolean("needs_computers"),
                        rs.getBoolean("needs_projector")
                },
                offeringId
        );

        if (requirements.isEmpty()) {
            return false;
        }

        boolean[] required = requirements.get(0);

        return (!required[0] || hasComputers)
                && (!required[1] || hasProjector);
    }

    // Danh sach giang vien cho giao dien xep lich
    public record TeacherOption(
            Long id,
            String code,
            String fullName
    ) {}

    public List<TeacherOption> getTeacherOptions() {

        String sql = """
            SELECT id, code, full_name
            FROM teachers
            ORDER BY full_name
            """;

        return jdbc.query(sql, (rs, rowNum) ->
                new TeacherOption(
                        rs.getLong("id"),
                        rs.getString("code"),
                        rs.getString("full_name")
                )
        );
    }

    private ResponseStatusException badRequest(
            String message) {

        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                message
        );
    }
}
