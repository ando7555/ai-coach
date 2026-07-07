package com.ai.coach.service;

import com.ai.coach.domain.EnumParser;
import com.ai.coach.domain.dto.TrainingPlanInput;
import com.ai.coach.domain.entity.FocusArea;
import com.ai.coach.domain.entity.Team;
import com.ai.coach.domain.entity.TrainingIntensity;
import com.ai.coach.domain.entity.TrainingPlan;
import com.ai.coach.domain.entity.TrainingSession;
import com.ai.coach.domain.repository.TeamRepository;
import com.ai.coach.domain.repository.TrainingPlanRepository;
import com.ai.coach.exception.EntityNotFoundException;
import com.ai.coach.exception.AiGenerationException;
import com.ai.coach.service.dto.AiTrainingPlanResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrainingPlanService {

    private final TeamRepository teamRepository;
    private final TrainingPlanRepository trainingPlanRepository;
    private final AiClient aiClient;
    private final AiResponseParser aiResponseParser;

    @Transactional(readOnly = true)
    public List<TrainingPlan> getByTeam(Long teamId) {
        return trainingPlanRepository.findByTeamId(teamId);
    }

    @Transactional
    public TrainingPlan generateTrainingPlan(TrainingPlanInput input) {
        log.info("Generating training plan for team {}", input.teamId());
        Team team = teamRepository.findById(input.teamId())
                .orElseThrow(() -> new EntityNotFoundException("Team", input.teamId()));

        LocalDate start = parseDate(input.weekStart(), "weekStart");
        LocalDate end = parseDate(input.weekEnd(), "weekEnd");
        long days = ChronoUnit.DAYS.between(start, end);
        if (days < 0) {
            throw new IllegalArgumentException("weekEnd must not be before weekStart");
        }
        if (days > 7) {
            throw new IllegalArgumentException("Training plan range must not exceed 7 days");
        }

        String prompt = buildPrompt(team, input);

        AiTrainingPlanResponse fallback = buildFallback(team, input, days);
        AiTrainingPlanResponse parsed;
        try {
            String aiResponse = aiClient.generateTrainingPlan(prompt).blockOptional().orElse("");
            parsed = aiResponseParser.parseAiResponse(aiResponse, AiTrainingPlanResponse.class, fallback);
        } catch (AiGenerationException e) {
            log.warn("AI unavailable; using deterministic training plan for team {}", team.getName());
            parsed = fallback;
        }

        List<AiTrainingPlanResponse.SessionEntry> entries = parsed.sessions() == null || parsed.sessions().isEmpty()
                ? fallback.sessions() : parsed.sessions();
        List<AiTrainingPlanResponse.SessionEntry> validEntries = entries.stream()
                .filter(entry -> entry.dayOffset() >= 0 && entry.dayOffset() <= days)
                .toList();
        if (validEntries.isEmpty()) {
            validEntries = fallback.sessions();
        }

        List<TrainingSession> sessions = validEntries.stream()
                .map(entry -> TrainingSession.builder()
                        .date(start.plusDays(entry.dayOffset()).atStartOfDay().atOffset(ZoneOffset.UTC))
                        .focusArea(parseFocusArea(entry.focusArea(), input.primaryFocus()))
                        .intensity(normalizeIntensity(entry.intensity(), input.intensity()))
                        .durationMinutes(entry.durationMinutes() > 0 ? entry.durationMinutes() : 90)
                        .notes(entry.notes())
                        .build())
                .toList();

        TrainingPlan plan = TrainingPlan.builder()
                .team(team)
                .weekStart(start.atStartOfDay().atOffset(ZoneOffset.UTC))
                .weekEnd(end.atStartOfDay().atOffset(ZoneOffset.UTC))
                .sessions(sessions)
                .summary(parsed.summary() == null || parsed.summary().isBlank() ? fallback.summary() : parsed.summary())
                .createdAt(OffsetDateTime.now())
                .build();

        return trainingPlanRepository.save(plan);
    }

    private LocalDate parseDate(String value, String field) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(field + " must use ISO format YYYY-MM-DD");
        }
    }

    private AiTrainingPlanResponse buildFallback(Team team, TrainingPlanInput input, long days) {
        String focus = (input.primaryFocus() != null ? input.primaryFocus() : FocusArea.BUILD_UP).name();
        String overall = (input.intensity() != null ? input.intensity() : TrainingIntensity.MEDIUM).name();
        String[] intensities = {"LOW", overall, "HIGH", overall, "LOW", "LOW"};
        List<AiTrainingPlanResponse.SessionEntry> sessions = new ArrayList<>();
        int sessionCount = (int) Math.min(days + 1, 6);
        for (int day = 0; day < sessionCount; day++) {
            sessions.add(new AiTrainingPlanResponse.SessionEntry(
                    day, focus, intensities[day], day == 2 ? 90 : 75,
                    day == 0 ? "Recovery, mobility, and light technical work"
                            : day == sessionCount - 1 ? "Tactical walkthrough and set pieces"
                            : "Progressive tactical session with monitored workload"));
        }
        return new AiTrainingPlanResponse(
                "Provider-independent weekly microcycle for %s, balancing %s development with recovery."
                        .formatted(team.getName(), focus), sessions);
    }

    private FocusArea parseFocusArea(String value, FocusArea fallback) {
        return EnumParser.parse(FocusArea.class, value,
                fallback != null ? fallback : FocusArea.BUILD_UP);
    }

    private TrainingIntensity normalizeIntensity(String aiValue, TrainingIntensity inputIntensity) {
        return EnumParser.parse(TrainingIntensity.class, aiValue,
                inputIntensity != null ? inputIntensity : TrainingIntensity.MEDIUM);
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
