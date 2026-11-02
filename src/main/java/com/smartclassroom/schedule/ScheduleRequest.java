package com.smartclassroom.schedule;

import java.time.LocalDate;
import java.time.LocalTime;

public record ScheduleRequest(
        Long offeringId,
        Long roomId,
        LocalDate sessionDate,
        LocalTime startTime,
        LocalTime endTime
) {
}
