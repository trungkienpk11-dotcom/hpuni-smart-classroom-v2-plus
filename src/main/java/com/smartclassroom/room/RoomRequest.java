package com.smartclassroom.room;

public record RoomRequest(
        String code,
        String name,
        Integer capacity,
        String roomType,
        Boolean projector,
        Boolean computers
) {
}
