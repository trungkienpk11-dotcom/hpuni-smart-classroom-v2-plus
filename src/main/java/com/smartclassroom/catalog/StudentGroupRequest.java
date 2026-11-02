package com.smartclassroom.catalog;

public record StudentGroupRequest(
        String code,
        String name,
        Integer studentCount
) {
}
