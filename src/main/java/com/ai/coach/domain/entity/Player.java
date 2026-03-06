package com.ai.coach.domain.entity;

import org.springframework.data.neo4j.core.schema.*;

@Node
public class Player {

    @Id
    @GeneratedValue
    private Long id;

    private String name;
    private String position;
    private Double rating;

    @Relationship(type = "PLAYS_FOR")
    private Team team;

    public Player() {}

    public Player(String name, String position, Double rating) {
        this.name = name;
        this.position = position;
        this.rating = rating;
    }

    public Long getId() { return id; }

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    public String getPosition() { return position; }

    public void setPosition(String position) { this.position = position; }

    public Double getRating() { return rating; }

    public void setRating(Double rating) { this.rating = rating; }

    public Team getTeam() { return team; }

    public void setTeam(Team team) { this.team = team; }
}
