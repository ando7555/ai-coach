package com.ai.coach.service;

import com.ai.coach.domain.dto.SeasonPlanInput;
import com.ai.coach.domain.entity.*;
import com.ai.coach.domain.repository.PlayerMatchStatRepository;
import com.ai.coach.domain.repository.PlayerRepository;
import com.ai.coach.domain.repository.SeasonPlanRepository;
import com.ai.coach.domain.repository.TeamRepository;
import com.ai.coach.exception.EntityNotFoundException;
import com.ai.coach.service.dto.AiSeasonPlanResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SeasonPlanService {

    private final TeamRepository teamRepository;
    private final PlayerRepository playerRepository;
    private final PlayerMatchStatRepository playerMatchStatRepository;
    private final SeasonPlanRepository seasonPlanRepository;
    private final AiClient aiClient;
    private final AiResponseParser aiResponseParser;

    @Transactional
    public SeasonPlan generateSeasonPlan(SeasonPlanInput input) {
        Team team = teamRepository.findById(input.teamId())
                .orElseThrow(() -> new EntityNotFoundException("Team", input.teamId()));

        List<Player> players = playerRepository.findByTeamId(team.getId());
        LocalDate cutoff = LocalDate.now().minusDays(28);

        List<PlayerWorkloadSnapshot> snapshots = players.stream()
                .map(p -> buildWorkloadSnapshot(p, cutoff))
                .toList();

        String prompt = buildPrompt(team, snapshots, input);

        String aiResponse = aiClient.generateSeasonPlan(prompt)
                .blockOptional()
                .orElse("No season plan generated.");

        AiSeasonPlanResponse fallback = new AiSeasonPlanResponse(
                aiResponse, List.of("See summary for details"));
        AiSeasonPlanResponse parsed = aiResponseParser.parseAiResponse(
                aiResponse, AiSeasonPlanResponse.class, fallback);

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
     *   0-180 min (<=2 full matches)  -> FRESH
     * 181-450 min (3-5 matches)       -> MODERATE
     * 451-720 min (6-8 matches)       -> TIRED
     *   720+ min                       -> EXHAUSTED
     */
    private FatigueLevel computeFatigueLevel(int minutes) {
        if (minutes <= 180) return FatigueLevel.FRESH;
        if (minutes <= 450) return FatigueLevel.MODERATE;
        if (minutes <= 720) return FatigueLevel.TIRED;
        return FatigueLevel.EXHAUSTED;
    }

    /**
     * Injury risk combines fatigue level with match density.
     * High density (>=6 matches in 28 days) always -> HIGH.
     */
    private InjuryRisk computeInjuryRisk(FatigueLevel fatigueLevel, int matches) {
        if (matches >= 6) return InjuryRisk.HIGH;
        return switch (fatigueLevel) {
            case EXHAUSTED -> InjuryRisk.HIGH;
            case TIRED -> InjuryRisk.MEDIUM;
            default -> InjuryRisk.LOW;
        };
    }

    private String buildPrompt(Team team, List<PlayerWorkloadSnapshot> snapshots, SeasonPlanInput input) {
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
