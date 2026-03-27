package com.ai.coach.service;

import com.ai.coach.domain.entity.Team;
import com.ai.coach.domain.repository.TeamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TeamService {

    private final TeamRepository teamRepository;

    public TeamService(TeamRepository teamRepository) {
        this.teamRepository = teamRepository;
    }

    @Transactional(readOnly = true)
    public List<Team> getAllTeams() {
        return teamRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Team getTeam(Long id) {
        return teamRepository.findById(id).orElse(null);
    }

    @Transactional
    public Team createTeam(String name, String league, String formation) {
        Team team = Team.builder()
                .name(name)
                .league(league)
                .formation(formation)
                .build();
        return teamRepository.save(team);
    }
}
