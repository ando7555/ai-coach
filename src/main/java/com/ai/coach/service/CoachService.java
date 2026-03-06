package com.ai.coach.service;

import com.ai.coach.domain.dto.MatchAnalysisInput;
import com.ai.coach.domain.dto.SeasonPlanInput;
import com.ai.coach.domain.dto.TrainingPlanInput;
import com.ai.coach.domain.entity.*;
import com.ai.coach.domain.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CoachService {

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

        // For now create a simple single session; later parse AI into sessions.
        TrainingSession session = TrainingSession.builder()
                .date(start.atStartOfDay().atOffset(ZoneOffset.UTC))
                .focusArea(input.primaryFocus())
                .intensity(input.intensity() != null ? input.intensity() : "MEDIUM")
                .durationMinutes(90)
                .notes("Generated automatically based on AI plan.")
                .build();

        TrainingPlan plan = TrainingPlan.builder()
                .team(team)
                .weekStart(start.atStartOfDay().atOffset(ZoneOffset.UTC))
                .weekEnd(end.atStartOfDay().atOffset(ZoneOffset.UTC))
                .sessions(List.of(session))
                .summary(aiResponse)
                .createdAt(OffsetDateTime.now())
                .build();

        return trainingPlanRepository.save(plan);
    }

    private String buildTrainingPlanPrompt(Team team, TrainingPlanInput input) {
        return """
                You are a professional football fitness and tactics coach.
                Design a one-week training plan for team %s.

                Week: %s to %s
                Primary focus: %s
                Intensity: %s (overall)

                Consider:
                - Upcoming matches
                - Need for recovery and conditioning
                - Tactical focus requested

                Return the plan in short natural language blocks per day.
                """.formatted(
                team.getName(),
                input.weekStart(),
                input.weekEnd(),
                input.primaryFocus(),
                input.intensity() != null ? input.intensity() : "MEDIUM"
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
