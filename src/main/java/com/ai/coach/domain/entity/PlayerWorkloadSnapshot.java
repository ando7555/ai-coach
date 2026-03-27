package com.ai.coach.domain.entity;

import lombok.*;
import org.springframework.data.neo4j.core.schema.*;

import java.time.OffsetDateTime;

@Node
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerWorkloadSnapshot {

    @Id
    @GeneratedValue
    private Long id;

    @Relationship(type = "WORKLOAD_FOR", direction = Relationship.Direction.OUTGOING)
    private Player player;

    private Integer matchesLast28Days;
    private Integer minutesLast28Days;
    private FatigueLevel fatigueLevel;
    private InjuryRisk injuryRisk;
    private String comment;
    private OffsetDateTime createdAt;
}
