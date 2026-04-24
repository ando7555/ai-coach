package com.ai.coach.service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * Maps the structured JSON response from the AI when generating a match analysis.
 *
 * Expected AI response format:
 * {
 *   "summary": "Tactical overview...",
 *   "keyFactors": ["Factor 1", "Factor 2", ...]
 * }
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AiMatchAnalysisResponse(
        String summary,
        List<String> keyFactors
) {}
