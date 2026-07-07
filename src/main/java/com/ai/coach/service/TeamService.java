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
        String normalizedName = requireText(name, "Team name");
        if (teamRepository.existsByNameIgnoreCase(normalizedName)) {
            throw new IllegalArgumentException("A team named '%s' already exists".formatted(normalizedName));
        }
        log.info("Creating team: {}", normalizedName);
        Team team = Team.builder()
                .name(normalizedName)
                .league(normalizeOptional(league))
                .formation(normalizeOptional(formation))
                .build();
        return teamRepository.save(team);
    }

    private String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.strip();
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
