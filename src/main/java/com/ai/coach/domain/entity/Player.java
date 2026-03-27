package com.ai.coach.domain.entity;

import lombok.*;
import org.springframework.data.neo4j.core.schema.*;

@Node
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Player {

    @Id
    @GeneratedValue
    private Long id;

    private String name;
    private String position;
    private Double rating;

    @Relationship(type = "PLAYS_FOR")
    private Team team;
}
