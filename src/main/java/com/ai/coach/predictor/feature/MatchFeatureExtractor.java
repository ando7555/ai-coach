package com.ai.coach.predictor.feature;

import com.ai.coach.domain.entity.Match;
import com.ai.coach.domain.repository.MatchRepository;
import com.ai.coach.exception.EntityNotFoundException;
import com.ai.coach.predictor.PredictorProperties;
import com.ai.coach.predictor.model.DataQualityStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MatchFeatureExtractor {
    private final MatchRepository matchRepository;
    private final PredictorProperties properties;

    @Transactional(readOnly = true)
    public MatchFeatureSnapshot extract(Long matchId) {
        Match target = matchRepository.findById(matchId)
                .orElseThrow(() -> new EntityNotFoundException("Match", matchId));
        return extract(target, matchRepository.findAll());
    }

    public MatchFeatureSnapshot extract(Match target, Iterable<Match> allMatches) {
        validateTarget(target);
        LocalDate targetDate = target.getDate();
        OffsetDateTime cutoff = OffsetDateTime.of(targetDate, LocalTime.MIN, ZoneOffset.UTC);

        List<Match> completedBeforeCutoff = new ArrayList<>();
        for (Match match : allMatches) {
            if (isCompletedBeforeCutoff(target, match, targetDate)) {
                completedBeforeCutoff.add(match);
            }
        }
        completedBeforeCutoff.sort(Comparator.comparing(Match::getDate).reversed());

        Long homeId = target.getHomeTeam().getId();
        Long awayId = target.getAwayTeam().getId();
        TeamFeatureSummary homeSummary = summarizeTeam(homeId, target.getHomeTeam().getName(), completedBeforeCutoff);
        TeamFeatureSummary awaySummary = summarizeTeam(awayId, target.getAwayTeam().getName(), completedBeforeCutoff);

        double globalHomeGoals = average(completedBeforeCutoff.stream()
                .map(Match::getHomeGoals)
                .mapToInt(Integer::intValue)
                .toArray());
        double globalAwayGoals = average(completedBeforeCutoff.stream()
                .map(Match::getAwayGoals)
                .mapToInt(Integer::intValue)
                .toArray());

        List<String> available = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        available.add("target match identity");
        available.add("target match date");

        if (completedBeforeCutoff.size() >= properties.getMinGlobalMatches()) {
            available.add("global historical scoring baseline");
        } else {
            missing.add("at least %d completed historical matches before cutoff".formatted(properties.getMinGlobalMatches()));
        }
        if (homeSummary.totalMatches() >= properties.getMinTeamMatches()) {
            available.add("home team historical form");
        } else {
            missing.add("%s needs at least %d completed pre-cutoff matches".formatted(
                    homeSummary.teamName(), properties.getMinTeamMatches()));
        }
        if (awaySummary.totalMatches() >= properties.getMinTeamMatches()) {
            available.add("away team historical form");
        } else {
            missing.add("%s needs at least %d completed pre-cutoff matches".formatted(
                    awaySummary.teamName(), properties.getMinTeamMatches()));
        }
        if (homeSummary.homeMatches() >= properties.getMinVenueMatches()) {
            available.add("home team home performance split");
        } else {
            missing.add("%s has limited home venue sample".formatted(homeSummary.teamName()));
        }
        if (awaySummary.awayMatches() >= properties.getMinVenueMatches()) {
            available.add("away team away performance split");
        } else {
            missing.add("%s has limited away venue sample".formatted(awaySummary.teamName()));
        }

        DataQualityStatus status = determineQuality(homeSummary, awaySummary, completedBeforeCutoff.size());

        return new MatchFeatureSnapshot(
                target.getId(),
                homeId,
                awayId,
                target.getHomeTeam().getName(),
                target.getAwayTeam().getName(),
                targetDate,
                cutoff,
                homeSummary,
                awaySummary,
                completedBeforeCutoff.size(),
                globalHomeGoals > 0 ? globalHomeGoals : properties.getLeagueAverageFallbackGoals(),
                globalAwayGoals > 0 ? globalAwayGoals : properties.getLeagueAverageFallbackGoals(),
                List.copyOf(available),
                List.copyOf(missing),
                status
        );
    }

    private void validateTarget(Match target) {
        if (target.getHomeTeam() == null || target.getAwayTeam() == null) {
            throw new IllegalArgumentException("Match must have both home and away teams before prediction");
        }
        if (target.getHomeTeam().getId() == null || target.getAwayTeam().getId() == null) {
            throw new IllegalArgumentException("Match teams must have persisted IDs before prediction");
        }
        if (target.getHomeTeam().getId().equals(target.getAwayTeam().getId())) {
            throw new IllegalArgumentException("Home and away teams must be different before prediction");
        }
        if (target.getDate() == null) {
            throw new IllegalArgumentException("Match date is required before prediction");
        }
    }

    private boolean isCompletedBeforeCutoff(Match target, Match candidate, LocalDate targetDate) {
        if (candidate.getId() != null && candidate.getId().equals(target.getId())) {
            return false;
        }
        return candidate.getDate() != null
                && candidate.getDate().isBefore(targetDate)
                && candidate.getHomeTeam() != null
                && candidate.getAwayTeam() != null
                && candidate.getHomeTeam().getId() != null
                && candidate.getAwayTeam().getId() != null
                && candidate.getHomeGoals() != null
                && candidate.getAwayGoals() != null;
    }

    private TeamFeatureSummary summarizeTeam(Long teamId, String teamName, List<Match> matches) {
        List<Match> teamMatches = matches.stream()
                .filter(m -> teamId.equals(m.getHomeTeam().getId()) || teamId.equals(m.getAwayTeam().getId()))
                .toList();
        List<Match> recent = teamMatches.stream().limit(properties.getRecentMatchWindow()).toList();

        int homeMatches = 0;
        int awayMatches = 0;
        int goalsFor = 0;
        int goalsAgainst = 0;
        int homeFor = 0;
        int homeAgainst = 0;
        int awayFor = 0;
        int awayAgainst = 0;

        for (Match match : teamMatches) {
            if (teamId.equals(match.getHomeTeam().getId())) {
                homeMatches++;
                goalsFor += match.getHomeGoals();
                goalsAgainst += match.getAwayGoals();
                homeFor += match.getHomeGoals();
                homeAgainst += match.getAwayGoals();
            } else {
                awayMatches++;
                goalsFor += match.getAwayGoals();
                goalsAgainst += match.getHomeGoals();
                awayFor += match.getAwayGoals();
                awayAgainst += match.getHomeGoals();
            }
        }

        int recentFor = 0;
        int recentAgainst = 0;
        for (Match match : recent) {
            if (teamId.equals(match.getHomeTeam().getId())) {
                recentFor += match.getHomeGoals();
                recentAgainst += match.getAwayGoals();
            } else {
                recentFor += match.getAwayGoals();
                recentAgainst += match.getHomeGoals();
            }
        }

        int total = teamMatches.size();
        return new TeamFeatureSummary(
                teamId,
                teamName,
                total,
                homeMatches,
                awayMatches,
                divide(goalsFor, total),
                divide(goalsAgainst, total),
                divide(homeFor, homeMatches),
                divide(homeAgainst, homeMatches),
                divide(awayFor, awayMatches),
                divide(awayAgainst, awayMatches),
                divide(recentFor, recent.size()),
                divide(recentAgainst, recent.size())
        );
    }

    private DataQualityStatus determineQuality(TeamFeatureSummary home, TeamFeatureSummary away, int globalMatches) {
        if (globalMatches < properties.getMinGlobalMatches()
                || home.totalMatches() < properties.getMinTeamMatches()
                || away.totalMatches() < properties.getMinTeamMatches()) {
            return DataQualityStatus.INSUFFICIENT;
        }
        if (home.homeMatches() < properties.getMinVenueMatches()
                || away.awayMatches() < properties.getMinVenueMatches()) {
            return DataQualityStatus.LIMITED;
        }
        return DataQualityStatus.SUFFICIENT;
    }

    private double average(int[] values) {
        if (values.length == 0) {
            return 0;
        }
        int sum = 0;
        for (int value : values) {
            sum += value;
        }
        return (double) sum / values.length;
    }

    private double divide(int numerator, int denominator) {
        return denominator == 0 ? 0 : (double) numerator / denominator;
    }
}
