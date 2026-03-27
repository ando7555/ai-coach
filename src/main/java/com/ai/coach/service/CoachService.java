package com.ai.coach.service;

import com.ai.coach.domain.dto.MatchAnalysisInput;
import com.ai.coach.domain.dto.SeasonPlanInput;
import com.ai.coach.domain.dto.TrainingPlanInput;
import com.ai.coach.domain.entity.*;
import com.ai.coach.domain.repository.*;
import com.ai.coach.service.dto.AiSeasonPlanResponse;
import com.ai.coach.service.dto.AiTrainingPlanResponse;
import com.ai.coach.exception.EntityNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CoachService {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final MatchRepository matchRepository;
    private final TeamRepository teamRepository;
    private final PlayerRepository playerRepository;
    private final PlayerMatchStatRepository playerMatchStatRepository;
    private final MatchAnalysisRepository matchAnalysisRepository;
    private final TrainingPlanRepository trainingPlanRepository;
    private final SeasonPlanRepository seasonPlanRepository;
    private final AiClient aiClient;


    @Transactional
    public MatchAnalysis generateMatchAnalysis(MatchAnalysisInput input) {
        Match match = matchRepository.findById(input.matchId())
                .orElseThrow(() -> new EntityNotFoundException("Match", input.matchId()));

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
                .orElseThrow(() -> new EntityNotFoundException("Team", input.teamId()));

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
                .orElseThrow(() -> new EntityNotFoundException("Team", input.teamId()));

        List<Player> players = playerRepository.findByTeamId(team.getId());
        LocalDate cutoff = LocalDate.now().minusDays(28);

        // Compute real workload snapshots from PlayerMatchStat data
        List<PlayerWorkloadSnapshot> snapshots = players.stream()
                .map(p -> buildWorkloadSnapshot(p, cutoff))
                .toList();

        String prompt = buildSeasonPlanPrompt(team, players, snapshots, input);

        String aiResponse = aiClient.generateSeasonPlan(prompt)
                .blockOptional()
                .orElse("No season plan generated.");

        AiSeasonPlanResponse parsed = parseSeasonPlanResponse(aiResponse);

        SeasonPlan plan = SeasonPlan.builder()
                .team(team)
                .season(input.season())
                .objectives(parsed.objectives())
                .workloadSnapshots(snapshots)
                .summary(parsed.summary())
                .createdAt(OffsetDateTime.now())
                .build();

        return seasonPlanRepository.save(plan);
    }

    /**
     * Builds a workload snapshot for a player based on real match stats
     * from the last 28 days.
     */
    private PlayerWorkloadSnapshot buildWorkloadSnapshot(Player player, LocalDate cutoff) {
        List<PlayerMatchStat> recentStats =
                playerMatchStatRepository.findByPlayerIdAndMatchDateAfter(player.getId(), cutoff);

        int matches = recentStats.size();
        int minutes = recentStats.stream().mapToInt(PlayerMatchStat::getMinutesPlayed).sum();
        FatigueLevel fatigue = computeFatigueLevel(minutes);
        InjuryRisk injuryRisk = computeInjuryRisk(fatigue, matches);

        String comment = "%d matches, %d min in last 28 days".formatted(matches, minutes);

        return PlayerWorkloadSnapshot.builder()
                .player(player)
                .matchesLast28Days(matches)
                .minutesLast28Days(minutes)
                .fatigueLevel(fatigue)
                .injuryRisk(injuryRisk)
                .comment(comment)
                .createdAt(OffsetDateTime.now())
                .build();
    }

    /**
     * Fatigue level based on minutes played in last 28 days.
     *   0–180 min (≤2 full matches)  → FRESH
     * 181–450 min (3–5 matches)      → MODERATE
     * 451–720 min (6–8 matches)      → TIRED
     *   720+ min                      → EXHAUSTED
     */
    private FatigueLevel computeFatigueLevel(int minutes) {
        if (minutes <= 180) return FatigueLevel.FRESH;
        if (minutes <= 450) return FatigueLevel.MODERATE;
        if (minutes <= 720) return FatigueLevel.TIRED;
        return FatigueLevel.EXHAUSTED;
    }

    /**
     * Injury risk combines fatigue level with match density.
     * High density (≥6 matches in 28 days) always → HIGH.
     */
    private InjuryRisk computeInjuryRisk(FatigueLevel fatigueLevel, int matches) {
        if (matches >= 6) return InjuryRisk.HIGH;
        return switch (fatigueLevel) {
            case EXHAUSTED -> InjuryRisk.HIGH;
            case TIRED -> InjuryRisk.MEDIUM;
            default -> InjuryRisk.LOW;
        };
    }

    private AiSeasonPlanResponse parseSeasonPlanResponse(String aiResponse) {
        try {
            String json = stripMarkdownFences(aiResponse);
            return objectMapper.readValue(json, AiSeasonPlanResponse.class);
        } catch (Exception e) {
            log.warn("Failed to parse AI season plan JSON, using raw text: {}", e.getMessage());
            return new AiSeasonPlanResponse(aiResponse, List.of("See summary for details"));
        }
    }

    private String buildSeasonPlanPrompt(Team team, List<Player> players,
                                         List<PlayerWorkloadSnapshot> snapshots, SeasonPlanInput input) {
        String workloadReport = snapshots.stream()
                .map(s -> "- %s (%s): %d matches, %d min, fatigue=%s, injury risk=%s".formatted(
                        s.getPlayer().getName(),
                        s.getPlayer().getPosition(),
                        s.getMatchesLast28Days(),
                        s.getMinutesLast28Days(),
                        s.getFatigueLevel(),
                        s.getInjuryRisk()))
                .reduce((a, b) -> a + "\n" + b)
                .orElse("No player data available");

        return """
                You are a head coach planning an entire season.

                Team: %s
                Season: %s
                Priority: %s

                Squad workload (last 28 days):
                %s

                Based on the workload data above, return ONLY valid JSON in this format:
                {
                  "summary": "Detailed season plan covering rotation, periodisation, and tactics",
                  "objectives": [
                    "Objective 1",
                    "Objective 2"
                  ]
                }

                Rules:
                - Provide 3-5 specific, measurable objectives
                - Reference specific players who need rest or increased minutes
                - Address injury risks for TIRED/EXHAUSTED players
                - Include periodisation phases (preparation, competition, transition)
                - Consider the stated priority: %s
                """.formatted(
                team.getName(),
                input.season(),
                input.priority() != null ? input.priority() : "Balanced",
                workloadReport,
                input.priority() != null ? input.priority() : "Balanced"
        );
    }
}
