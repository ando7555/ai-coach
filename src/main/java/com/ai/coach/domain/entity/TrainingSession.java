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
    private FocusArea focusArea;
    private TrainingIntensity intensity;
    private Integer durationMinutes;
    private String notes;
}
