package com.ai.coach.service;

import com.ai.coach.domain.dto.FormIndicator;
import com.ai.coach.domain.dto.PlayerMatchStatInput;
import com.ai.coach.domain.dto.PlayerPerformanceTrend;
import com.ai.coach.exception.EntityNotFoundException;
import com.ai.coach.domain.entity.Match;
import com.ai.coach.domain.entity.Player;
import com.ai.coach.domain.entity.PlayerMatchStat;
import com.ai.coach.domain.repository.MatchRepository;
import com.ai.coach.domain.repository.PlayerMatchStatRepository;
import com.ai.coach.domain.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlayerMatchStatService {

    private final PlayerMatchStatRepository statRepository;
    private final PlayerRepository playerRepository;
    private final MatchRepository matchRepository;

    @Transactional(readOnly = true)
    public List<PlayerMatchStat> getByMatch(Long matchId) {
        return statRepository.findByMatchId(matchId);
    }

    @Transactional(readOnly = true)
    public List<PlayerMatchStat> getByPlayer(Long playerId) {
        return statRepository.findByPlayerId(playerId);
    }

    @Transactional(readOnly = true)
    public PlayerPerformanceTrend getTrendByLastMatches(Long playerId, int lastN) {
        if (lastN < 1) {
            throw new IllegalArgumentException("lastN must be at least 1");
        }
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new EntityNotFoundException("Player", playerId));

        List<PlayerMatchStat> allStats = statRepository.findByPlayerId(playerId);
        List<PlayerMatchStat> chronological = allStats.stream()
                .sorted(Comparator.comparing(s -> s.getMatch().getDate()))
                .toList();
        // Take the last N matches (most recent)
        int fromIndex = Math.max(0, chronological.size() - lastN);
        chronological = chronological.subList(fromIndex, chronological.size());

        return buildTrend(player, chronological);
    }

    @Transactional(readOnly = true)
    public PlayerPerformanceTrend getTrendByDateRange(Long playerId, LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("'from' date must not be after 'to' date");
        }
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new EntityNotFoundException("Player", playerId));

        List<PlayerMatchStat> allStats = statRepository.findByPlayerId(playerId);
        List<PlayerMatchStat> stats = allStats.stream()
                .filter(s -> {
                    LocalDate matchDate = s.getMatch().getDate();
                    return matchDate != null && !matchDate.isBefore(from) && !matchDate.isAfter(to);
                })
                .sorted(Comparator.comparing(s -> s.getMatch().getDate()))
                .toList();

        return buildTrend(player, stats);
    }

    private PlayerPerformanceTrend buildTrend(Player player, List<PlayerMatchStat> stats) {
        int matchCount = stats.size();
        int totalGoals = stats.stream().mapToInt(PlayerMatchStat::getGoals).sum();
        int totalAssists = stats.stream().mapToInt(PlayerMatchStat::getAssists).sum();
        int totalGoalContributions = totalGoals + totalAssists;
        int totalMinutes = stats.stream().mapToInt(PlayerMatchStat::getMinutesPlayed).sum();

        double avgGoals = matchCount > 0 ? (double) totalGoals / matchCount : 0.0;
        double avgAssists = matchCount > 0 ? (double) totalAssists / matchCount : 0.0;
        double avgContributions = matchCount > 0 ? (double) totalGoalContributions / matchCount : 0.0;

        Double avgRating = null;
        if (matchCount > 0) {
            var ratedStats = stats.stream()
                    .filter(s -> s.getRating() != null)
                    .toList();
            if (!ratedStats.isEmpty()) {
                avgRating = ratedStats.stream()
                        .mapToDouble(PlayerMatchStat::getRating)
                        .average()
                        .orElse(0.0);
            }
        }

        FormIndicator form = calculateForm(stats);

        return new PlayerPerformanceTrend(
                player, matchCount, totalGoals, totalAssists, totalGoalContributions,
                avgGoals, avgAssists, avgContributions, avgRating,
                totalMinutes, form, stats
        );
    }

    private FormIndicator calculateForm(List<PlayerMatchStat> stats) {
        if (stats.size() < 2) {
            return FormIndicator.STABLE;
        }
        int mid = stats.size() / 2;
        List<PlayerMatchStat> olderHalf = stats.subList(0, mid);
        List<PlayerMatchStat> newerHalf = stats.subList(mid, stats.size());

        double olderScore = averageCompositeScore(olderHalf);
        double newerScore = averageCompositeScore(newerHalf);

        if (olderScore == 0 && newerScore == 0) {
            return FormIndicator.STABLE;
        }
        if (olderScore == 0) {
            return FormIndicator.IMPROVING;
        }

        double changeRatio = (newerScore - olderScore) / olderScore;
        if (changeRatio > 0.15) {
            return FormIndicator.IMPROVING;
        } else if (changeRatio < -0.15) {
            return FormIndicator.DECLINING;
        }
        return FormIndicator.STABLE;
    }

    private double averageCompositeScore(List<PlayerMatchStat> stats) {
        return stats.stream()
                .mapToDouble(s -> s.getGoals() * 3.0
                        + s.getAssists() * 2.0
                        + (s.getRating() != null ? s.getRating() / 10.0 : 0.0))
                .average()
                .orElse(0.0);
    }

    @Transactional
    public PlayerMatchStat record(PlayerMatchStatInput input) {
        Player player = playerRepository.findById(input.playerId())
                .orElseThrow(() -> new EntityNotFoundException("Player", input.playerId()));

        Match match = matchRepository.findById(input.matchId())
                .orElseThrow(() -> new EntityNotFoundException("Match", input.matchId()));

        PlayerMatchStat stat = PlayerMatchStat.builder()
                .player(player)
                .match(match)
                .minutesPlayed(input.minutesPlayed())
                .goals(input.goals() != null ? input.goals() : 0)
                .assists(input.assists() != null ? input.assists() : 0)
                .yellowCards(input.yellowCards() != null ? input.yellowCards() : 0)
                .redCard(input.redCard() != null && input.redCard())
                .build();

        return statRepository.save(stat);
    }
}
