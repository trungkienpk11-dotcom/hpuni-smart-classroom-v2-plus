package com.smartclassroom.catalog;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class CatalogRepository {

    private final JdbcTemplate jdbc;

    public CatalogRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // 1. Lấy danh sách giảng viên
    public List<Teacher> findTeachers() {

        String sql = """
            SELECT id, code, full_name, email
            FROM teachers
            ORDER BY code
            """;

        return jdbc.query(sql, (rs, rowNum) ->
                new Teacher(
                        rs.getLong("id"),
                        rs.getString("code"),
                        rs.getString("full_name"),
                        rs.getString("email")
                )
        );
    }

    // 2. Lấy danh sách lớp sinh viên
    public List<StudentGroup> findGroups() {

        String sql = """
            SELECT id, code, name, student_count
            FROM student_groups
            ORDER BY code
            """;

        return jdbc.query(sql, (rs, rowNum) ->
                new StudentGroup(
                        rs.getLong("id"),
                        rs.getString("code"),
                        rs.getString("name"),
                        rs.getInt("student_count")
                )
        );
    }

    // 3. Lấy danh sách môn học
    public List<Course> findCourses() {

        String sql = """
            SELECT id, code, name, credits
            FROM courses
            ORDER BY code
            """;

        return jdbc.query(sql, (rs, rowNum) ->
                new Course(
                        rs.getLong("id"),
                        rs.getString("code"),
                        rs.getString("name"),
                        rs.getInt("credits")
                )
        );
    }

    // 4. Lấy danh sách lớp học phần
    public List<CourseOffering> findOfferings() {

        String sql = """
            SELECT
                o.id,
                c.code AS course_code,
                c.name AS course_name,
                t.code AS teacher_code,
                t.full_name AS teacher_name,
                g.code AS group_code,
                g.name AS group_name,
                o.semester,
                o.academic_year,
                o.sessions_per_week

            FROM course_offerings o

            JOIN courses c
                ON o.course_id = c.id

            JOIN teachers t
                ON o.teacher_id = t.id

            JOIN student_groups g
                ON o.group_id = g.id

            ORDER BY o.id
            """;

        return jdbc.query(sql, (rs, rowNum) ->
                new CourseOffering(
                        rs.getLong("id"),
                        rs.getString("course_code"),
                        rs.getString("course_name"),
                        rs.getString("teacher_code"),
                        rs.getString("teacher_name"),
                        rs.getString("group_code"),
                        rs.getString("group_name"),
                        rs.getString("semester"),
                        rs.getString("academic_year"),
                        rs.getInt("sessions_per_week")
                )
        );
    }
}
