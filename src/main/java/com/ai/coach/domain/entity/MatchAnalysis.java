package com.ai.coach.domain.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.neo4j.core.schema.*;
import java.time.OffsetDateTime;
import java.util.List;

@Node
@Getter @Setter
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
    private String focusArea;  // e.g. "PRESSING"

    @Property("style")
    private String style;      // e.g. "POSSESSION"

    @Property("risk_level")
    private String riskLevel;  // e.g. "MEDIUM"

    @Property("summary")
    private String summary;

    @CompositeProperty
    private List<String> keyFactors;

    @Property("created_at")
    private OffsetDateTime createdAt;
}
