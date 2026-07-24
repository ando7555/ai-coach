package com.ai.coach.predictor;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "pitchmind.predictor")
public class PredictorProperties {
    private String modelName = "Transparent Poisson Baseline";
    private String modelVersion = "baseline-poisson-v1";
    private int minTeamMatches = 3;
    private int minGlobalMatches = 6;
    private int minVenueMatches = 2;
    private int recentMatchWindow = 5;
    private double recentWeight = 0.65;
    private double leagueAverageFallbackGoals = 1.35;
    private double minExpectedGoals = 0.2;
    private double maxExpectedGoals = 4.5;
    private int maxGoals = 8;
    private double normalizationTolerance = 0.000001;
    private double highConfidenceSampleSize = 10;
    private double mediumConfidenceSampleSize = 6;
    private double highSeparationThreshold = 0.18;
    private double mediumSeparationThreshold = 0.08;
}
