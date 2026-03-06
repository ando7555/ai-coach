package com.ai.coach.domain.entity;

import org.springframework.data.neo4j.core.schema.*;

import java.util.HashSet;
import java.util.Set;

@Node
public class Team {

    @Id
    @GeneratedValue
    private Long id;

    private String name;
    private String league;
    private String formation;

    @Relationship(type = "HAS_PLAYER")
    private Set<Player> players = new HashSet<>();

    // constructors, getters, setters

    public Team() {}

    public Team(String name, String league, String formation) {
        this.name = name;
        this.league = league;
        this.formation = formation;
    }

    public Long getId() { return id; }

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    public String getLeague() { return league; }

    public void setLeague(String league) { this.league = league; }

    public String getFormation() { return formation; }

    public void setFormation(String formation) { this.formation = formation; }

    public Set<Player> getPlayers() { return players; }

    public void setPlayers(Set<Player> players) { this.players = players; }
}
