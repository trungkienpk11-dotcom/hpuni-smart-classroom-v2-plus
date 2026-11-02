package com.smartclassroom.schedule;

import java.time.LocalDate;
import java.time.LocalTime;

public record ScheduleSession(
        Long id,
        Long offeringId,
        String courseCode,
        String courseName,
        String teacherName,
        String groupCode,
        Long roomId,
        String roomCode,
        LocalDate sessionDate,
        LocalTime startTime,
        LocalTime endTime
) {
}
