package com.smartclassroom.catalog;

public record Course(
        Long id,
        String code,
        String name,
        Integer credits
) {
}
