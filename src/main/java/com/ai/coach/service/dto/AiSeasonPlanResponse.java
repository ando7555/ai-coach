package com.ai.coach.service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * Maps the structured JSON response from the AI when generating a season plan.
 *
 * Expected AI response format:
 * {
 *   "summary": "Season overview...",
 *   "objectives": ["Objective 1", "Objective 2", ...]
 * }
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AiSeasonPlanResponse(
        String summary,
        List<String> objectives

) {}
