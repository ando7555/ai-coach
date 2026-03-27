package com.ai.coach.domain.entity;

import lombok.*;
import org.springframework.data.neo4j.core.schema.*;

import java.util.HashSet;
import java.util.Set;

@Node
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Team {

    @Id
    @GeneratedValue
    private Long id;

    private String name;
    private String league;
    private String formation;

    @Relationship(type = "HAS_PLAYER")
    @Builder.Default
    private Set<Player> players = new HashSet<>();
}
