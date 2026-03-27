package com.ai.coach.service;

import com.ai.coach.domain.dto.TrainingPlanInput;
import com.ai.coach.domain.entity.FocusArea;
import com.ai.coach.domain.entity.Team;
import com.ai.coach.domain.entity.TrainingIntensity;
import com.ai.coach.domain.entity.TrainingPlan;
import com.ai.coach.domain.entity.TrainingSession;
import com.ai.coach.domain.repository.TeamRepository;
import com.ai.coach.domain.repository.TrainingPlanRepository;
import com.ai.coach.exception.EntityNotFoundException;
import com.ai.coach.service.dto.AiTrainingPlanResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TrainingPlanService {

    private final TeamRepository teamRepository;
    private final TrainingPlanRepository trainingPlanRepository;
    private final AiClient aiClient;
    private final AiResponseParser aiResponseParser;

    @Transactional
    public TrainingPlan generateTrainingPlan(TrainingPlanInput input) {
        Team team = teamRepository.findById(input.teamId())
                .orElseThrow(() -> new EntityNotFoundException("Team", input.teamId()));

        LocalDate start = LocalDate.parse(input.weekStart());
        LocalDate end = LocalDate.parse(input.weekEnd());

        String prompt = buildPrompt(team, input);

        String aiResponse = aiClient.generateTrainingPlan(prompt)
                .blockOptional()
                .orElse("No training plan generated.");

        AiTrainingPlanResponse fallback = new AiTrainingPlanResponse(
                aiResponse,
                List.of(new AiTrainingPlanResponse.SessionEntry(0, "BUILD_UP", "MEDIUM", 90, aiResponse))
        );
        AiTrainingPlanResponse parsed = aiResponseParser.parseAiResponse(
                aiResponse, AiTrainingPlanResponse.class, fallback);

        List<TrainingSession> sessions = parsed.sessions().stream()
                .map(entry -> TrainingSession.builder()
                        .date(start.plusDays(entry.dayOffset()).atStartOfDay().atOffset(ZoneOffset.UTC))
                        .focusArea(parseFocusArea(entry.focusArea(), input.primaryFocus()))
                        .intensity(normalizeIntensity(entry.intensity()))
                        .durationMinutes(entry.durationMinutes() > 0 ? entry.durationMinutes() : 90)
                        .notes(entry.notes())
                        .build())
                .toList();

        TrainingPlan plan = TrainingPlan.builder()
                .team(team)
                .weekStart(start.atStartOfDay().atOffset(ZoneOffset.UTC))
                .weekEnd(end.atStartOfDay().atOffset(ZoneOffset.UTC))
                .sessions(sessions)
                .summary(parsed.summary())
                .createdAt(OffsetDateTime.now())
                .build();

        return trainingPlanRepository.save(plan);
    }

    private FocusArea parseFocusArea(String value, String fallback) {
        try {
            return value != null ? FocusArea.valueOf(value.toUpperCase()) : FocusArea.valueOf(fallback.toUpperCase());
        } catch (IllegalArgumentException e) {
            return FocusArea.valueOf(fallback.toUpperCase());
        }
    }

    private TrainingIntensity normalizeIntensity(String intensity) {
        if (intensity == null) return TrainingIntensity.MEDIUM;
        try {
            return TrainingIntensity.valueOf(intensity.toUpperCase());
        } catch (IllegalArgumentException e) {
            return TrainingIntensity.MEDIUM;
        }
    }

    private String buildPrompt(Team team, TrainingPlanInput input) {
        return """
                You are a professional football fitness and tactics coach.
                Design a one-week training plan for team %s.

                Week: %s to %s
                Primary focus: %s
                Intensity: %s (overall)

                Return ONLY valid JSON (no markdown, no explanation) in this exact format:
                {
                  "summary": "Brief overview of the week's training philosophy",
                  "sessions": [
                    {
                      "dayOffset": 0,
                      "focusArea": "PRESSING",
                      "intensity": "LOW",
                      "durationMinutes": 60,
                      "notes": "Description of the session"
                    }
                  ]
                }

                Rules:
                - dayOffset 0 = %s (first day of the week), increment by 1 per day
                - focusArea must be one of: PRESSING, BUILD_UP, DEFENCE
                - intensity must be one of: LOW, MEDIUM, HIGH
                - Include 5-6 sessions across the week
                - Balance intensity: start with recovery, peak mid-week, taper before weekend
                """.formatted(
                team.getName(),
                input.weekStart(),
                input.weekEnd(),
                input.primaryFocus(),
                input.intensity() != null ? input.intensity() : "MEDIUM",
                input.weekStart()
        );
    }
}
