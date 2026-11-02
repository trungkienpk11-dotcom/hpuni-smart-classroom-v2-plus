package com.smartclassroom.catalog;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class TeacherCrudRepository {

    private final JdbcTemplate jdbc;

    public TeacherCrudRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private final RowMapper<Teacher> mapper =
            (rs, rowNum) -> new Teacher(
                    rs.getLong("id"),
                    rs.getString("code"),
                    rs.getString("full_name"),
                    rs.getString("email")
            );

    // Tìm giảng viên bằng ID
    public Optional<Teacher> findById(long id) {

        String sql = """
            SELECT id, code, full_name, email
            FROM teachers
            WHERE id = ?
            """;

        return jdbc.query(sql, mapper, id)
                .stream()
                .findFirst();
    }

    // Tìm giảng viên bằng mã
    public Optional<Teacher> findByCode(String code) {

        String sql = """
            SELECT id, code, full_name, email
            FROM teachers
            WHERE code = ?
            """;

        return jdbc.query(sql, mapper, code)
                .stream()
                .findFirst();
    }

    // Thêm giảng viên
    public Teacher insert(TeacherRequest teacher) {

        String sql = """
            INSERT INTO teachers (code, full_name, email)
            VALUES (?, ?, ?)
            """;

        jdbc.update(
                sql,
                teacher.code(),
                teacher.fullName(),
                teacher.email()
        );

        return findByCode(teacher.code()).orElseThrow();
    }

    // Sửa giảng viên
    public int update(long id, TeacherRequest teacher) {

        String sql = """
            UPDATE teachers
            SET code = ?, full_name = ?, email = ?
            WHERE id = ?
            """;

        return jdbc.update(
                sql,
                teacher.code(),
                teacher.fullName(),
                teacher.email(),
                id
        );
    }

    // Xóa giảng viên
    public int delete(long id) {

        String sql = "DELETE FROM teachers WHERE id = ?";

        return jdbc.update(sql, id);
    }
}
