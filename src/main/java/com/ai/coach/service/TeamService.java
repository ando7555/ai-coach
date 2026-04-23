package com.ai.coach.service;

import com.ai.coach.domain.entity.Team;
import com.ai.coach.domain.repository.TeamRepository;
import com.ai.coach.exception.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
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
        return teamRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Team", id));
    }

    @CacheEvict(value = {"teams", "team"}, allEntries = true)
    @Transactional
    public Team createTeam(String name, String league, String formation) {
        log.info("Creating team: {}", name);
        Team team = Team.builder()
                .name(name)
                .league(league)
                .formation(formation)
                .build();
        return teamRepository.save(team);
    }
}
