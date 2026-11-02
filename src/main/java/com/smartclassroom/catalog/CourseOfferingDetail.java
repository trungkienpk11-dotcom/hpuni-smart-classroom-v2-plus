package com.smartclassroom.catalog;

public record CourseOfferingDetail(
        Long id,
        Long courseId,
        Long teacherId,
        Long groupId,
        String semester,
        String academicYear,
        Integer sessionsPerWeek
) {
}
