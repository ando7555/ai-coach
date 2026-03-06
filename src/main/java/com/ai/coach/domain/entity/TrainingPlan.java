package com.ai.coach.domain.entity;

import lombok.*;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.time.OffsetDateTime;
import java.util.List;

@Node
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainingPlan {

    @Id
    @GeneratedValue
    private Long id;

    @Relationship(type = "PLAN_FOR", direction = Relationship.Direction.OUTGOING)
    private Team team;

    private OffsetDateTime weekStart;
    private OffsetDateTime weekEnd;

    @Relationship(type = "HAS_SESSION", direction = Relationship.Direction.OUTGOING)
    private List<TrainingSession> sessions;

    private String summary;
    private OffsetDateTime createdAt;
}