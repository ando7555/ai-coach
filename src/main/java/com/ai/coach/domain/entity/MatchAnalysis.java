package com.ai.coach.domain.entity;

import lombok.*;
import org.springframework.data.neo4j.core.schema.*;

import java.time.OffsetDateTime;
import java.util.List;

@Node
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchAnalysis {

    @Id
    @GeneratedValue
    private Long id;

    @Relationship(type = "ANALYSIS_FOR", direction = Relationship.Direction.OUTGOING)
    private Match match;

    @Property("focus_area")
    private FocusArea focusArea;

    @Property("style")
    private TacticalStyle style;

    @Property("risk_level")
    private RiskLevel riskLevel;

    @Property("summary")
    private String summary;

    private List<String> keyFactors;

    @Property("created_at")
    private OffsetDateTime createdAt;
}
