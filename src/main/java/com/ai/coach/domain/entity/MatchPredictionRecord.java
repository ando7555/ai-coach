package com.ai.coach.domain.entity;

import com.ai.coach.predictor.model.ConfidenceLevel;
import com.ai.coach.predictor.model.DataQualityStatus;
import com.ai.coach.predictor.model.UncertaintyLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
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
public class MatchPredictionRecord {
    @Id
    @GeneratedValue
    private Long id;

    @Relationship(type = "PREDICTION_FOR", direction = Relationship.Direction.OUTGOING)
    private Match match;

    private OffsetDateTime featureCutoffTimestamp;
    private String featureSummary;
    private Double homeWinProbability;
    private Double drawProbability;
    private Double awayWinProbability;
    private Double expectedHomeGoals;
    private Double expectedAwayGoals;
    private Double bothTeamsToScoreProbability;
    private Double over25GoalsProbability;
    private Double under25GoalsProbability;
    private String mostLikelyScore;
    private ConfidenceLevel confidenceLevel;
    private UncertaintyLevel uncertaintyLevel;
    private DataQualityStatus dataQualityStatus;
    private List<String> explanationFactors;
    private List<String> warnings;
    private String modelName;
    private String modelVersion;
    private Integer predictionVersion;
    private OffsetDateTime generatedAt;
}
