package com.ai.coach.domain.entity;

import lombok.*;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

import java.time.OffsetDateTime;

@Node
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainingSession {

    @Id
    @GeneratedValue
    private Long id;

    private OffsetDateTime date;
    private String focusArea;     // enum name
    private String intensity;     // LOW / MEDIUM / HIGH
    private Integer durationMinutes;
    private String notes;
}

