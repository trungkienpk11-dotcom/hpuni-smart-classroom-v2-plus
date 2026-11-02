package com.smartclassroom.catalog;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class CourseCrudRepository {

    private final JdbcTemplate jdbc;

    public CourseCrudRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // Chuyển dữ liệu SQL thành đối tượng Course
    private final RowMapper<Course> mapper =
            (rs, rowNum) -> new Course(
                    rs.getLong("id"),
                    rs.getString("code"),
                    rs.getString("name"),
                    rs.getInt("credits")
            );

    // 1. Tìm môn học theo ID
    public Optional<Course> findById(long id) {

        String sql = """
            SELECT id, code, name, credits
            FROM courses
            WHERE id = ?
            """;

        return jdbc.query(sql, mapper, id)
                .stream()
                .findFirst();
    }

    // 2. Tìm môn học theo mã
    public Optional<Course> findByCode(String code) {

        String sql = """
            SELECT id, code, name, credits
            FROM courses
            WHERE code = ?
            """;

        return jdbc.query(sql, mapper, code)
                .stream()
                .findFirst();
    }

    // 3. Thêm môn học
    public Course insert(CourseRequest course) {

        String sql = """
            INSERT INTO courses (code, name, credits)
            VALUES (?, ?, ?)
            """;

        jdbc.update(
                sql,
                course.code(),
                course.name(),
                course.credits()
        );

        return findByCode(course.code()).orElseThrow();
    }

    // 4. Cập nhật môn học
    public int update(long id, CourseRequest course) {

        String sql = """
            UPDATE courses
            SET code = ?,
                name = ?,
                credits = ?
            WHERE id = ?
            """;

        return jdbc.update(
                sql,
                course.code(),
                course.name(),
                course.credits(),
                id
        );
    }

    // 5. Xóa môn học
    public int delete(long id) {

        String sql = """
            DELETE FROM courses
            WHERE id = ?
            """;

        return jdbc.update(sql, id);
    }
}
