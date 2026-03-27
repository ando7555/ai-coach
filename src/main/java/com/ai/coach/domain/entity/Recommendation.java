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
public class Recommendation {

    @Id
    @GeneratedValue
    private Long id;

    @Relationship(type = "FOR_MATCH")
    private Match match;

    private String context;
    private String advice;
    private OffsetDateTime createdAt;
}
