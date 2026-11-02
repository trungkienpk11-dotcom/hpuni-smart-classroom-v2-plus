package com.smartclassroom.catalog;

public record TeacherRequest(
        String code,
        String fullName,
        String email
) {
}
