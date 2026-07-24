package com.ai.coach.betting;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "pitchmind.betting")
public class BettingProperties {
    private double potentialValueThreshold = 0.08;
    private double weakValueThreshold = 0.0;
    private double highUncertaintyProbabilityThreshold = 0.05;
}
