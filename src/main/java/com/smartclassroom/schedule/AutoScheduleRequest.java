package com.smartclassroom.schedule;

import java.time.LocalDate;

public record AutoScheduleRequest(
        String semester,
        String academicYear,
        LocalDate weekStart
) {
}
