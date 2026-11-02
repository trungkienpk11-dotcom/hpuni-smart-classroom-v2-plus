package com.smartclassroom.catalog;

public record StudentGroup(
        Long id,
        String code,
        String name,
        Integer studentCount
) {
}
