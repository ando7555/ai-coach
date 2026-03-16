package com.ai.coach.service;

import com.ai.coach.domain.dto.MatchInput;
import com.ai.coach.exception.EntityNotFoundException;
import com.ai.coach.domain.entity.Match;
import com.ai.coach.domain.entity.Team;
import com.ai.coach.domain.repository.MatchRepository;
import com.ai.coach.domain.repository.TeamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class MatchService {

    private final MatchRepository matchRepository;
    private final TeamRepository teamRepository;

    public MatchService(MatchRepository matchRepository, TeamRepository teamRepository) {
        this.matchRepository = matchRepository;
        this.teamRepository = teamRepository;
    }

    @Transactional(readOnly = true)
    public Match getMatch(Long id) {
        return matchRepository.findById(id).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<Match> getMatchesByTeam(Long teamId) {
        return matchRepository.findByHomeTeamIdOrAwayTeamId(teamId, teamId);
    }

    @Transactional
    public Match recordMatch(MatchInput input) {
        Team home = teamRepository.findById(input.homeTeamId())
                .orElseThrow(() -> new EntityNotFoundException("Team", input.homeTeamId()));
        Team away = teamRepository.findById(input.awayTeamId())
                .orElseThrow(() -> new EntityNotFoundException("Team", input.awayTeamId()));

        LocalDate date = input.date() != null
                ? LocalDate.parse(input.date())
                : LocalDate.now();

        Match match = new Match(home, away, input.homeGoals(), input.awayGoals(), date);
        return matchRepository.save(match);
    }
}
