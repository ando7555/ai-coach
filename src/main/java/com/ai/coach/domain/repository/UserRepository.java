package com.ai.coach.domain.repository;

import com.ai.coach.domain.entity.User;
import org.springframework.data.neo4j.repository.Neo4jRepository;

import java.util.Optional;

public interface UserRepository extends Neo4jRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByGoogleSubject(String googleSubject);
}
