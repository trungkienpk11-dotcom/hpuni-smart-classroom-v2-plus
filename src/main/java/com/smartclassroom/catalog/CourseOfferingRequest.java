package com.smartclassroom.catalog;

public record CourseOfferingRequest(
        Long courseId,
        Long teacherId,
        Long groupId,
        String semester,
        String academicYear,
        Integer sessionsPerWeek
) {
}
