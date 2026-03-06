package com.ai.coach.service;

import com.ai.coach.domain.dto.MatchAnalysisInput;
import com.ai.coach.domain.dto.SeasonPlanInput;
import com.ai.coach.domain.dto.TrainingPlanInput;
import com.ai.coach.domain.entity.*;
import com.ai.coach.domain.repository.*;
import com.ai.coach.service.dto.AiTrainingPlanResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class CoachService {

    private static final Set<String> VALID_INTENSITIES = Set.of("LOW", "MEDIUM", "HIGH");
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final MatchRepository matchRepository;
    private final TeamRepository teamRepository;
    private final PlayerRepository playerRepository;
    private final MatchAnalysisRepository matchAnalysisRepository;
    private final TrainingPlanRepository trainingPlanRepository;
    private final SeasonPlanRepository seasonPlanRepository;
    private final AiClient aiClient;


    @Transactional
    public MatchAnalysis generateMatchAnalysis(MatchAnalysisInput input) {
        Match match = matchRepository.findById(input.matchId())
                .orElseThrow(() -> new RuntimeException("Match not found"));

        String prompt = buildMatchAnalysisPrompt(match, input);

        String aiResponse = aiClient.generateTacticalAdvice(prompt)
                .blockOptional()
                .orElse("No analysis generated.");

        List<String> keyFactors = List.of(aiResponse.split("\\n"));

        MatchAnalysis analysis = MatchAnalysis.builder()
                .match(match)
                .focusArea(input.focusArea())
                .style(input.style())
                .riskLevel(input.riskLevel())
                .summary(aiResponse)
                .keyFactors(keyFactors)
                .createdAt(OffsetDateTime.now())
                .build();

        return matchAnalysisRepository.save(analysis);
    }

    private String buildMatchAnalysisPrompt(Match match, MatchAnalysisInput input) {
        return """
                You are an elite football tactical coach.
                Analyse the upcoming match with the following context:

                Match ID: %d
                Home Team: %s
                Away Team: %s
                Focus Area: %s
                Style: %s
                Risk Level: %s

                Consider recent form, strengths, weaknesses, and tactical nuances.
                Provide a concise, coach-ready summary with bullet points.
                """.formatted(
                match.getId(),
                match.getHomeTeam().getName(),
                match.getAwayTeam().getName(),
                input.focusArea(),
                input.style(),
                input.riskLevel()
        );
    }


    @Transactional
    public TrainingPlan generateTrainingPlan(TrainingPlanInput input) {
        Team team = teamRepository.findById(input.teamId())
                .orElseThrow(() -> new RuntimeException("Team not found"));

        LocalDate start = LocalDate.parse(input.weekStart());
        LocalDate end = LocalDate.parse(input.weekEnd());

        String prompt = buildTrainingPlanPrompt(team, input);

        String aiResponse = aiClient.generateTrainingPlan(prompt)
                .blockOptional()
                .orElse("No training plan generated.");

        AiTrainingPlanResponse parsed = parseTrainingPlanResponse(aiResponse);

        List<TrainingSession> sessions = parsed.sessions().stream()
                .map(entry -> TrainingSession.builder()
                        .date(start.plusDays(entry.dayOffset()).atStartOfDay().atOffset(ZoneOffset.UTC))
                        .focusArea(entry.focusArea() != null ? entry.focusArea() : input.primaryFocus())
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

    /**
     * Parses the AI JSON response into an AiTrainingPlanResponse.
     * If parsing fails (malformed JSON, markdown fences, etc.), falls back to
     * a single-session plan with the raw AI text as notes.
     */
    private AiTrainingPlanResponse parseTrainingPlanResponse(String aiResponse) {
        try {
            String json = stripMarkdownFences(aiResponse);
            return objectMapper.readValue(json, AiTrainingPlanResponse.class);
        } catch (Exception e) {
            log.warn("Failed to parse AI training plan JSON, falling back to single session: {}", e.getMessage());
            return new AiTrainingPlanResponse(
                    aiResponse,
                    List.of(new AiTrainingPlanResponse.SessionEntry(0, "BUILD_UP", "MEDIUM", 90, aiResponse))
            );
        }
    }

    /**
     * Strips markdown code fences (```json ... ```) that LLMs sometimes wrap around JSON.
     */
    private String stripMarkdownFences(String text) {
        String trimmed = text.strip();
        if (trimmed.startsWith("```")) {
            // Remove opening fence (```json or ```)
            int firstNewline = trimmed.indexOf('\n');
            if (firstNewline != -1) {
                trimmed = trimmed.substring(firstNewline + 1);
            }
        }
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }
        return trimmed.strip();
    }

    private String normalizeIntensity(String intensity) {
        if (intensity == null) return "MEDIUM";
        String upper = intensity.toUpperCase();
        return VALID_INTENSITIES.contains(upper) ? upper : "MEDIUM";
    }

    private String buildTrainingPlanPrompt(Team team, TrainingPlanInput input) {
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

    // ---------- C. SEASON / ROTATION COACH ----------

    @Transactional
    public SeasonPlan generateSeasonPlan(SeasonPlanInput input) {
        Team team = teamRepository.findById(input.teamId())
                .orElseThrow(() -> new RuntimeException("Team not found"));

        List<Player> players = playerRepository.findByTeamId(team.getId());

        String prompt = buildSeasonPlanPrompt(team, players, input);

        String aiResponse = aiClient.generateSeasonPlan(prompt)
                .blockOptional()
                .orElse("No season plan generated.");

        // Very simple workload snapshot – later you can compute from Match history.
        List<PlayerWorkloadSnapshot> snapshots = players.stream()
                .map(p -> PlayerWorkloadSnapshot.builder()
                        .player(p)
                        .matchesLast28Days(0)
                        .minutesLast28Days(0)
                        .fatigueLevel("UNKNOWN")
                        .injuryRisk("MEDIUM")
                        .comment("Initial plan – workload yet to be calculated.")
                        .createdAt(OffsetDateTime.now())
                        .build()
                ).toList();

        SeasonPlan plan = SeasonPlan.builder()
                .team(team)
                .season(input.season())
                .objectives(List.of(
                        "Finish in top 4",
                        "Reach at least quarter-finals in main cup"
                ))
                .workloadSnapshots(snapshots)
                .summary(aiResponse)
                .createdAt(OffsetDateTime.now())
                .build();

        return seasonPlanRepository.save(plan);
    }

    private String buildSeasonPlanPrompt(Team team, List<Player> players, SeasonPlanInput input) {
        String playerList = players.stream()
                .map(p -> p.getName() + " (" + p.getPosition() + ")")
                .reduce((a, b) -> a + ", " + b)
                .orElse("No players");

        return """
                You are a head coach planning an entire season.

                Team: %s
                Season: %s
                Priority: %s

                Current squad: %s

                Provide:
                - 3–5 key tactical and performance objectives
                - Guidance on rotation policy and minute management
                - Risk management for injuries and fatigue
                - Focus areas in different phases of the season
                """.formatted(
                team.getName(),
                input.season(),
                input.priority() != null ? input.priority() : "Balanced",
                playerList
        );
    }
}
