package com.ai.coach.service;

import com.ai.coach.domain.WorkloadCalculator;
import com.ai.coach.domain.dto.SeasonPlanInput;
import com.ai.coach.domain.entity.*;
import com.ai.coach.domain.repository.PlayerMatchStatRepository;
import com.ai.coach.domain.repository.PlayerRepository;
import com.ai.coach.domain.repository.SeasonPlanRepository;
import com.ai.coach.domain.repository.TeamRepository;
import com.ai.coach.exception.EntityNotFoundException;
import com.ai.coach.service.dto.AiSeasonPlanResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeasonPlanService {

    private static final int RECENT_WINDOW_DAYS = 28;
    private static final String DEFAULT_PRIORITY = "Balanced";

    private final TeamRepository teamRepository;
    private final PlayerRepository playerRepository;
    private final PlayerMatchStatRepository playerMatchStatRepository;
    private final SeasonPlanRepository seasonPlanRepository;
    private final AiClient aiClient;
    private final AiResponseParser aiResponseParser;

    @Transactional(readOnly = true)
    public List<SeasonPlan> getByTeam(Long teamId) {
        return seasonPlanRepository.findByTeamId(teamId);
    }

    @Transactional
    public SeasonPlan generateSeasonPlan(SeasonPlanInput input) {
        log.info("Generating season plan for team {} season {}", input.teamId(), input.season());
        Team team = teamRepository.findById(input.teamId())
                .orElseThrow(() -> new EntityNotFoundException("Team", input.teamId()));

        List<Player> players = playerRepository.findByTeamId(team.getId());
        LocalDate cutoff = LocalDate.now().minusDays(RECENT_WINDOW_DAYS);

        List<Long> playerIds = players.stream().map(Player::getId).toList();
        Map<Long, List<PlayerMatchStat>> statsByPlayer =
                playerMatchStatRepository.findByPlayerIdInAndMatchDateAfter(playerIds, cutoff)
                        .stream()
                        .collect(Collectors.groupingBy(s -> s.getPlayer().getId()));

        List<PlayerWorkloadSnapshot> snapshots = players.stream()
                .map(p -> buildWorkloadSnapshot(p, statsByPlayer.getOrDefault(p.getId(), List.of())))
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

    private PlayerWorkloadSnapshot buildWorkloadSnapshot(Player player, List<PlayerMatchStat> recentStats) {
        int matches = recentStats.size();
        int minutes = recentStats.stream().mapToInt(PlayerMatchStat::getMinutesPlayed).sum();
        FatigueLevel fatigue = WorkloadCalculator.computeFatigueLevel(minutes);
        InjuryRisk injuryRisk = WorkloadCalculator.computeInjuryRisk(fatigue, matches);

        String comment = "%d matches, %d min in last %d days".formatted(matches, minutes, RECENT_WINDOW_DAYS);

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

    private String buildPrompt(Team team, List<PlayerWorkloadSnapshot> snapshots, SeasonPlanInput input) {
        String priority = input.priority() != null ? input.priority() : DEFAULT_PRIORITY;

        String workloadReport = snapshots.stream()
                .map(s -> "- %s (%s): %d matches, %d min, fatigue=%s, injury risk=%s".formatted(
                        s.getPlayer().getName(),
                        s.getPlayer().getPosition(),
                        s.getMatchesLast28Days(),
                        s.getMinutesLast28Days(),
                        s.getFatigueLevel(),
                        s.getInjuryRisk()))
                .collect(Collectors.joining("\n"));

        return """
                You are a head coach planning an entire season.

                Team: %s
                Season: %s
                Priority: %s

                Squad workload (last %d days):
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
                priority,
                RECENT_WINDOW_DAYS,
                workloadReport,
                priority
        );
    }
}
