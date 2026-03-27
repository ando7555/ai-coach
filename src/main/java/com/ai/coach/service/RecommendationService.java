package com.ai.coach.service;

import com.ai.coach.domain.entity.Match;
import com.ai.coach.exception.EntityNotFoundException;
import com.ai.coach.domain.entity.Recommendation;
import com.ai.coach.domain.dto.RecommendationContextInput;
import com.ai.coach.domain.repository.MatchRepository;
import com.ai.coach.domain.repository.RecommendationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final RecommendationRepository recommendationRepository;
    private final MatchRepository matchRepository;
    private final AiClient aiClient;

    @Transactional(readOnly = true)
    public List<Recommendation> getByMatch(Long matchId) {
        return recommendationRepository.findByMatchId(matchId);
    }

    @Transactional
    public Recommendation generateRecommendation(RecommendationContextInput input) {
        Match match = matchRepository.findById(input.matchId())
                .orElseThrow(() -> new EntityNotFoundException("Match", input.matchId()));

        String prompt = buildPrompt(match, input);

        String advice = aiClient.generateTacticalAdvice(prompt)
                .blockOptional()
                .orElse("No advice generated.");

        Recommendation rec = Recommendation.builder()
                .match(match)
                .context(prompt)
                .advice(advice)
                .createdAt(OffsetDateTime.now())
                .build();

        return recommendationRepository.save(rec);
    }

    private String buildPrompt(Match match, RecommendationContextInput input) {
        return """
                You are an elite football tactical coach.
                Analyze the following situation and give detailed tactical recommendations.

                Match:
                  Home team: %s
                  Away team: %s
                  Score: %d - %d
                  Date: %s

                Tactical focus:
                  Focus area: %s
                  Style: %s
                  Risk level: %s

                Please provide:
                  - Pressing strategy
                  - Defensive shape
                  - Attacking patterns
                  - Adjustments by phase of play
                  - Player role tweaks

                """.formatted(
                match.getHomeTeam().getName(),
                match.getAwayTeam().getName(),
                match.getHomeGoals() != null ? match.getHomeGoals() : 0,
                match.getAwayGoals() != null ? match.getAwayGoals() : 0,
                match.getDate(),
                input.focusArea(),
                input.style(),
                input.riskLevel()
        );
    }
}
