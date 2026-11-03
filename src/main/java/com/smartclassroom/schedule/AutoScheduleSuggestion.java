
package com.smartclassroom.schedule;
import java.time.LocalDate;
import java.time.LocalTime;

public record AutoScheduleSuggestion(
        Long offeringId,
        String courseName,
        String teacherName,
        String groupCode,
        Long roomId,
        String roomCode,
        LocalDate sessionDate,
        LocalTime startTime,
        LocalTime endTime,
        String explanation
) {
}
