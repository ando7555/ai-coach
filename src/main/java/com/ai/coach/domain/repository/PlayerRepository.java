// PlayerRepository.java
package com.ai.coach.domain.repository;

import com.ai.coach.domain.entity.Player;
import org.springframework.data.neo4j.repository.Neo4jRepository;

import java.util.List;

public interface PlayerRepository extends Neo4jRepository<Player, Long> {
    List<Player> findByTeamId(Long teamId);
}
