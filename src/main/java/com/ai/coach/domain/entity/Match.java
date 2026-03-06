package com.ai.coach.domain.entity;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.*;

import java.time.LocalDate;

@Getter
@Node
public class Match {

    @Id
    @GeneratedValue
    private Long id;

    @Relationship(type = "HOME_TEAM")
    private Team homeTeam;

    @Relationship(type = "AWAY_TEAM")
    private Team awayTeam;

    @Setter
    private Integer homeGoals;
    @Setter
    private Integer awayGoals;
    @Setter
    private LocalDate date;

    public Match() {}

    public Match(Team homeTeam, Team awayTeam, Integer homeGoals, Integer awayGoals, LocalDate date) {
        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
        this.homeGoals = homeGoals;
        this.awayGoals = awayGoals;
        this.date = date;
    }

}
