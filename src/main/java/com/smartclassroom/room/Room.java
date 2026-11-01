package com.smartclassroom.room;

public record Room(
        Long id,
        String code,
        String name,
        Integer capacity,
        String roomType,
        Boolean projector,
        Boolean computers
) {
}
