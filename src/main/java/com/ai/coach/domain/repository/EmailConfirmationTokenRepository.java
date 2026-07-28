package com.ai.coach.domain.repository;

import com.ai.coach.domain.entity.EmailConfirmationToken;
import org.springframework.data.neo4j.repository.Neo4jRepository;

import java.util.List;
import java.util.Optional;

public interface EmailConfirmationTokenRepository extends Neo4jRepository<EmailConfirmationToken, Long> {
    Optional<EmailConfirmationToken> findByToken(String token);
    List<EmailConfirmationToken> findByEmailAndConsumedAtIsNull(String email);
}
