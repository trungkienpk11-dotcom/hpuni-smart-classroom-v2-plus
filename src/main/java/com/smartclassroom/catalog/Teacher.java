package com.smartclassroom.catalog;

public record Teacher(
        Long id,
        String code,
        String fullName,
        String email
) {
}
