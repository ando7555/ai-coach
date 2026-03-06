package com.ai.coach.domain.entity;

import lombok.*;
import org.springframework.data.neo4j.core.schema.*;

import java.time.OffsetDateTime;
import java.util.List;

@Node
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeasonPlan {

    @Id
    @GeneratedValue
    private Long id;

    @Relationship(type = "SEASON_PLAN_FOR", direction = Relationship.Direction.OUTGOING)
    private Team team;

    private String season;  // "2025/26"

    private List<String> objectives;

    @Relationship(type = "HAS_WORKLOAD", direction = Relationship.Direction.OUTGOING)
    private List<PlayerWorkloadSnapshot> workloadSnapshots;

    private String summary;
    private OffsetDateTime createdAt;
}
