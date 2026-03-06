package com.ai.coach.domain.entity;

import org.springframework.data.neo4j.core.schema.*;

import java.time.OffsetDateTime;

@Node
public class Recommendation {

    @Id
    @GeneratedValue
    private Long id;

    @Relationship(type = "FOR_MATCH")
    private Match match;

    private String context;
    private String advice;
    private OffsetDateTime createdAt;

    public Recommendation() {}

    public Recommendation(Match match, String context, String advice, OffsetDateTime createdAt) {
        this.match = match;
        this.context = context;
        this.advice = advice;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }

    public Match getMatch() { return match; }

    public String getContext() { return context; }

    public String getAdvice() { return advice; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
}
