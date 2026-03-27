package com.ai.coach.domain.entity;

import lombok.*;
import org.springframework.data.neo4j.core.schema.*;

import java.time.LocalDate;

@Node
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Match {

    @Id
    @GeneratedValue
    private Long id;

    @Relationship(type = "HOME_TEAM")
    private Team homeTeam;

    @Relationship(type = "AWAY_TEAM")
    private Team awayTeam;

    private Integer homeGoals;
    private Integer awayGoals;
    private LocalDate date;
}
