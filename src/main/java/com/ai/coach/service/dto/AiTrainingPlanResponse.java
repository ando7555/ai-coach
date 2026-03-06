package com.ai.coach.service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * Maps the structured JSON response from the AI when generating a training plan.
 *
 * Expected AI response format:
 * {
 *   "summary": "Week overview...",
 *   "sessions": [
 *     { "dayOffset": 0, "focusArea": "PRESSING", "intensity": "LOW", "durationMinutes": 60, "notes": "..." },
 *     ...
 *   ]
 * }
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AiTrainingPlanResponse(
        String summary,
        List<SessionEntry> sessions
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SessionEntry(
            int dayOffset,
            String focusArea,
            String intensity,
            int durationMinutes,
            String notes
    ) {}
}
