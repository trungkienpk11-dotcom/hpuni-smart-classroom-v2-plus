package com.smartclassroom.catalog;

public record CourseRequest(
        String code,
        String name,
        Integer credits
) {
}
