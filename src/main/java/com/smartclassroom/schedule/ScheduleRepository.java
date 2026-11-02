package com.smartclassroom.schedule;

import com.smartclassroom.room.Room;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public class ScheduleRepository {

    private final JdbcTemplate jdbc;

    public ScheduleRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // Thông tin dùng để kiểm tra giảng viên,
    // lớp sinh viên và sức chứa.
    public record OfferingInfo(
            long teacherId,
            long groupId,
            int studentCount
    ) {}

    // Truy vấn thông tin lịch học kèm tên môn,
    // giảng viên, lớp và phòng.
    private static final String SELECT_SQL = """
        SELECT
            s.id,
            s.offering_id,
            c.code AS course_code,
            c.name AS course_name,
            t.full_name AS teacher_name,
            g.code AS group_code,
            s.room_id,
            r.code AS room_code,
            s.session_date,
            s.start_time,
            s.end_time
        FROM schedule_sessions s
        JOIN course_offerings o ON s.offering_id = o.id
        JOIN courses c ON o.course_id = c.id
        JOIN teachers t ON o.teacher_id = t.id
        JOIN student_groups g ON o.group_id = g.id
        JOIN rooms r ON s.room_id = r.id
        """;

    private final RowMapper<ScheduleSession> mapper =
            (rs, rowNum) -> new ScheduleSession(
                    rs.getLong("id"),
                    rs.getLong("offering_id"),
                    rs.getString("course_code"),
                    rs.getString("course_name"),
                    rs.getString("teacher_name"),
                    rs.getString("group_code"),
                    rs.getLong("room_id"),
                    rs.getString("room_code"),
                    rs.getDate("session_date").toLocalDate(),
                    rs.getTime("start_time").toLocalTime(),
                    rs.getTime("end_time").toLocalTime()
            );

    // 1. Xem lịch trong một tuần
    public List<ScheduleSession> findWeek(LocalDate monday) {

        String sql = SELECT_SQL + """
            WHERE s.session_date BETWEEN ? AND ?
            ORDER BY s.session_date, s.start_time
            """;

        return jdbc.query(
                sql,
                mapper,
                Date.valueOf(monday),
                Date.valueOf(monday.plusDays(6))
        );
    }

    // 2. Tìm một buổi học theo ID
    public Optional<ScheduleSession> findById(long id) {

        String sql = SELECT_SQL + " WHERE s.id = ?";

        return jdbc.query(sql, mapper, id)
                .stream()
                .findFirst();
    }

    // 3. Lấy thông tin lớp học phần
    public Optional<OfferingInfo> findOfferingInfo(long id) {

        String sql = """
            SELECT o.teacher_id, o.group_id,
                   g.student_count
            FROM course_offerings o
            JOIN student_groups g ON o.group_id = g.id
            WHERE o.id = ?
            """;

        return jdbc.query(sql, (rs, rowNum) ->
                new OfferingInfo(
                        rs.getLong("teacher_id"),
                        rs.getLong("group_id"),
                        rs.getInt("student_count")
                ), id
        ).stream().findFirst();
    }

    // 4. Kiểm tra có trùng phòng không
    public boolean hasRoomConflict(
            long roomId,
            LocalDate date,
            LocalTime start,
            LocalTime end,
            long excludeId) {

        String sql = """
            SELECT COUNT(*)
            FROM schedule_sessions s
            WHERE s.room_id = ?
              AND s.session_date = ?
              AND s.start_time < ?
              AND s.end_time > ?
              AND s.id <> ?
            """;

        return count(sql,
                roomId, Date.valueOf(date),
                Time.valueOf(end), Time.valueOf(start),
                excludeId
        ) > 0;
    }

    // 5. Kiểm tra có trùng giảng viên không
    public boolean hasTeacherConflict(
            long teacherId,
            LocalDate date,
            LocalTime start,
            LocalTime end,
            long excludeId) {

        String sql = """
            SELECT COUNT(*)
            FROM schedule_sessions s
            JOIN course_offerings o
                ON s.offering_id = o.id
            WHERE o.teacher_id = ?
              AND s.session_date = ?
              AND s.start_time < ?
              AND s.end_time > ?
              AND s.id <> ?
            """;

        return count(sql,
                teacherId, Date.valueOf(date),
                Time.valueOf(end), Time.valueOf(start),
                excludeId
        ) > 0;
    }

    // 6. Kiểm tra có trùng lớp sinh viên không
    public boolean hasGroupConflict(
            long groupId,
            LocalDate date,
            LocalTime start,
            LocalTime end,
            long excludeId) {

        String sql = """
            SELECT COUNT(*)
            FROM schedule_sessions s
            JOIN course_offerings o
                ON s.offering_id = o.id
            WHERE o.group_id = ?
              AND s.session_date = ?
              AND s.start_time < ?
              AND s.end_time > ?
              AND s.id <> ?
            """;

        return count(sql,
                groupId, Date.valueOf(date),
                Time.valueOf(end), Time.valueOf(start),
                excludeId
        ) > 0;
    }

    private int count(String sql, Object... params) {
        Integer value = jdbc.queryForObject(
                sql, Integer.class, params
        );
        return value == null ? 0 : value;
    }

    // 7. Thêm buổi học
    public ScheduleSession insert(ScheduleRequest data) {

        String sql = """
            INSERT INTO schedule_sessions
            (offering_id, room_id, session_date,
             start_time, end_time)
            VALUES (?, ?, ?, ?, ?)
            """;

        var keyHolder = new GeneratedKeyHolder();

        jdbc.update(connection -> {
            var ps = connection.prepareStatement(
                    sql, new String[]{"id"}
            );

            ps.setLong(1, data.offeringId());
            ps.setLong(2, data.roomId());
            ps.setDate(3, Date.valueOf(data.sessionDate()));
            ps.setTime(4, Time.valueOf(data.startTime()));
            ps.setTime(5, Time.valueOf(data.endTime()));

            return ps;
        }, keyHolder);

        Number id = keyHolder.getKey();

        if (id == null) {
            throw new IllegalStateException(
                    "Không nhận được ID buổi học"
            );
        }

        return findById(id.longValue()).orElseThrow();
    }

    // 8. Sửa buổi học
    public int update(long id, ScheduleRequest data) {

        String sql = """
            UPDATE schedule_sessions
            SET offering_id = ?,
                room_id = ?,
                session_date = ?,
                start_time = ?,
                end_time = ?
            WHERE id = ?
            """;

        return jdbc.update(
                sql,
                data.offeringId(),
                data.roomId(),
                Date.valueOf(data.sessionDate()),
                Time.valueOf(data.startTime()),
                Time.valueOf(data.endTime()),
                id
        );
    }

    // 9. Xóa buổi học
    public int delete(long id) {
        return jdbc.update(
                "DELETE FROM schedule_sessions WHERE id = ?",
                id
        );
    }

    // 10. Tìm phòng trống có đủ sức chứa
    public List<Room> findAvailableRooms(
            LocalDate date,
            LocalTime start,
            LocalTime end,
            int minCapacity) {

        String sql = """
            SELECT r.id, r.code, r.name,
                   r.capacity, r.room_type,
                   r.projector, r.computers
            FROM rooms r
            WHERE r.capacity >= ?
              AND NOT EXISTS (
                  SELECT 1
                  FROM schedule_sessions s
                  WHERE s.room_id = r.id
                    AND s.session_date = ?
                    AND s.start_time < ?
                    AND s.end_time > ?
              )
            ORDER BY r.capacity, r.code
            """;

        return jdbc.query(
                sql,
                (rs, rowNum) -> new Room(
                        rs.getLong("id"),
                        rs.getString("code"),
                        rs.getString("name"),
                        rs.getInt("capacity"),
                        rs.getString("room_type"),
                        rs.getBoolean("projector"),
                        rs.getBoolean("computers")
                ),
                minCapacity,
                Date.valueOf(date),
                Time.valueOf(end),
                Time.valueOf(start)
        );
    }
}
