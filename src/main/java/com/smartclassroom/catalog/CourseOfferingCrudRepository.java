package com.smartclassroom.catalog;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class CourseOfferingCrudRepository {

    private final JdbcTemplate jdbc;

    public CourseOfferingCrudRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private final RowMapper<CourseOfferingDetail> mapper =
            (rs, rowNum) -> new CourseOfferingDetail(
                    rs.getLong("id"),
                    rs.getLong("course_id"),
                    rs.getLong("teacher_id"),
                    rs.getLong("group_id"),
                    rs.getString("semester"),
                    rs.getString("academic_year"),
                    rs.getInt("sessions_per_week")
            );

    // 1. Tìm học phần theo ID
    public Optional<CourseOfferingDetail> findById(long id) {

        String sql = """
            SELECT id, course_id, teacher_id, group_id,
                   semester, academic_year, sessions_per_week
            FROM course_offerings
            WHERE id = ?
            """;

        return jdbc.query(sql, mapper, id)
                .stream()
                .findFirst();
    }

    // 2. Kiểm tra khóa ngoại tồn tại
    public boolean existsCourse(long id) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM courses WHERE id = ?",
                Integer.class, id
        );
        return n != null && n > 0;
    }

    public boolean existsTeacher(long id) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM teachers WHERE id = ?",
                Integer.class, id
        );
        return n != null && n > 0;
    }

    public boolean existsGroup(long id) {
        Integer n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM student_groups WHERE id = ?",
                Integer.class, id
        );
        return n != null && n > 0;
    }

    // 3. Tạo lớp học phần mới
    public CourseOfferingDetail insert(
            CourseOfferingRequest data) {

        String sql = """
            INSERT INTO course_offerings
            (course_id, teacher_id, group_id,
             semester, academic_year, sessions_per_week)
            VALUES (?, ?, ?, ?, ?, ?)
            """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbc.update(connection -> {

            var ps = connection.prepareStatement(
                    sql, new String[]{"id"}
            );

            ps.setLong(1, data.courseId());
            ps.setLong(2, data.teacherId());
            ps.setLong(3, data.groupId());
            ps.setString(4, data.semester());
            ps.setString(5, data.academicYear());
            ps.setInt(6, data.sessionsPerWeek());

            return ps;

        }, keyHolder);

        Number id = keyHolder.getKey();

        if (id == null) {
            throw new IllegalStateException(
                    "Database không trả về ID lớp học phần"
            );
        }

        return findById(id.longValue()).orElseThrow();
    }

    // 4. Cập nhật phân công
    public int update(
            long id, CourseOfferingRequest data) {

        String sql = """
            UPDATE course_offerings
            SET course_id = ?,
                teacher_id = ?,
                group_id = ?,
                semester = ?,
                academic_year = ?,
                sessions_per_week = ?
            WHERE id = ?
            """;

        return jdbc.update(
                sql,
                data.courseId(),
                data.teacherId(),
                data.groupId(),
                data.semester(),
                data.academicYear(),
                data.sessionsPerWeek(),
                id
        );
    }

    // 5. Xóa lớp học phần
    public int delete(long id) {

        return jdbc.update(
                "DELETE FROM course_offerings WHERE id = ?",
                id
        );
    }
}
