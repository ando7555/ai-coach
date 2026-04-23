package com.ai.coach.service;

import com.ai.coach.domain.entity.Team;
import com.ai.coach.domain.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TeamService {

    private final TeamRepository teamRepository;

    @Cacheable("teams")
    @Transactional(readOnly = true)
    public List<Team> getAllTeams() {
        return teamRepository.findAll();
    }

    @Cacheable(value = "team", key = "#id")
    @Transactional(readOnly = true)
    public Team getTeam(Long id) {
        return teamRepository.findById(id).orElse(null);
    }

    @CacheEvict(value = {"teams", "team"}, allEntries = true)
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
