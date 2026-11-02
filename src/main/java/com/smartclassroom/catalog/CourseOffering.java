package com.smartclassroom.catalog;

public record CourseOffering(
        Long id,
        String courseCode,
        String courseName,
        String teacherCode,
        String teacherName,
        String groupCode,
        String groupName,
        String semester,
        String academicYear,
        Integer sessionsPerWeek
) {
}
