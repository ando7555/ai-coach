package com.ai.coach.predictor;

import com.ai.coach.predictor.feature.MatchFeatureSnapshot;
import com.ai.coach.predictor.model.MatchPredictionResult;

public interface MatchPredictor {
    MatchPredictionResult predict(MatchFeatureSnapshot snapshot);
}
