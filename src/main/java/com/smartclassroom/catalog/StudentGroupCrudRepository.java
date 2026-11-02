package com.smartclassroom.catalog;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class StudentGroupCrudRepository {

    private final JdbcTemplate jdbc;

    public StudentGroupCrudRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // Chuyển dữ liệu SQL thành đối tượng Java
    private final RowMapper<StudentGroup> mapper =
            (rs, rowNum) -> new StudentGroup(
                    rs.getLong("id"),
                    rs.getString("code"),
                    rs.getString("name"),
                    rs.getInt("student_count")
            );

    // Tìm lớp theo ID
    public Optional<StudentGroup> findById(long id) {

        String sql = """
            SELECT id, code, name, student_count
            FROM student_groups
            WHERE id = ?
            """;

        return jdbc.query(sql, mapper, id)
                .stream()
                .findFirst();
    }

    // Tìm lớp theo mã
    public Optional<StudentGroup> findByCode(String code) {

        String sql = """
            SELECT id, code, name, student_count
            FROM student_groups
            WHERE code = ?
            """;

        return jdbc.query(sql, mapper, code)
                .stream()
                .findFirst();
    }

    // Thêm lớp mới
    public StudentGroup insert(StudentGroupRequest group) {

        String sql = """
            INSERT INTO student_groups
            (code, name, student_count)
            VALUES (?, ?, ?)
            """;

        jdbc.update(
                sql,
                group.code(),
                group.name(),
                group.studentCount()
        );

        return findByCode(group.code()).orElseThrow();
    }

    // Cập nhật thông tin lớp
    public int update(long id, StudentGroupRequest group) {

        String sql = """
            UPDATE student_groups
            SET code = ?,
                name = ?,
                student_count = ?
            WHERE id = ?
            """;

        return jdbc.update(
                sql,
                group.code(),
                group.name(),
                group.studentCount(),
                id
        );
    }

    // Xóa lớp
    public int delete(long id) {

        String sql = """
            DELETE FROM student_groups
            WHERE id = ?
            """;

        return jdbc.update(sql, id);
    }
}
