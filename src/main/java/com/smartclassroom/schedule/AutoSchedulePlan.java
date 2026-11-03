package com.smartclassroom.schedule;

import java.time.LocalDate;
import java.util.List;

public record AutoSchedulePlan(
        LocalDate weekStart,
        int existingSessions,
        int neededSessions,
        List<AutoScheduleSuggestion> suggestions,
        List<String> unscheduled
) {
}
