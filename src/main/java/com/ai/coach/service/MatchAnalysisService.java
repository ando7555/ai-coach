package com.ai.coach.service;

import com.ai.coach.domain.dto.MatchAnalysisInput;
import com.ai.coach.domain.entity.Match;
import com.ai.coach.domain.entity.MatchAnalysis;
import com.ai.coach.domain.repository.MatchAnalysisRepository;
import com.ai.coach.domain.repository.MatchRepository;
import com.ai.coach.exception.EntityNotFoundException;
import com.ai.coach.service.dto.AiMatchAnalysisResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchAnalysisService {

    private final MatchRepository matchRepository;
    private final MatchAnalysisRepository matchAnalysisRepository;
    private final AiClient aiClient;
    private final AiResponseParser aiResponseParser;

    @Transactional(readOnly = true)
    public List<MatchAnalysis> getByMatch(Long matchId) {
        return matchAnalysisRepository.findByMatchId(matchId);
    }

    @Transactional
    public MatchAnalysis generateMatchAnalysis(MatchAnalysisInput input) {
        log.info("Generating match analysis for match {}", input.matchId());
        Match match = matchRepository.findById(input.matchId())
                .orElseThrow(() -> new EntityNotFoundException("Match", input.matchId()));

        String prompt = buildPrompt(match, input);

        String aiResponse = aiClient.generateTacticalAdvice(prompt)
                .blockOptional()
                .orElse("No analysis generated.");

        AiMatchAnalysisResponse fallback = new AiMatchAnalysisResponse(
                aiResponse, List.of("See summary for details"));
        AiMatchAnalysisResponse parsed = aiResponseParser.parseAiResponse(
                aiResponse, AiMatchAnalysisResponse.class, fallback);

        MatchAnalysis analysis = MatchAnalysis.builder()
                .match(match)
                .focusArea(input.focusArea())
                .style(input.style())
                .riskLevel(input.riskLevel())
                .summary(parsed.summary())
                .keyFactors(parsed.keyFactors())
                .createdAt(OffsetDateTime.now())
                .build();

        return matchAnalysisRepository.save(analysis);
    }

    private String buildPrompt(Match match, MatchAnalysisInput input) {
        return """
                Analyse the upcoming match with the following context:

                Home Team: %s
                Away Team: %s
                Match Date: %s
                Focus Area: %s
                Style: %s
                Risk Level: %s

                Consider recent form, strengths, weaknesses, and tactical nuances.

                Return ONLY valid JSON (no markdown, no explanation) in this exact format:
                {
                  "summary": "Concise, coach-ready tactical overview",
                  "keyFactors": [
                    "Key factor 1",
                    "Key factor 2"
                  ]
                }

                Rules:
                - Provide 3-6 specific, actionable key factors
                - The summary should be a concise tactical overview
                - Focus on the stated focus area and playing style
                """.formatted(
                match.getHomeTeam().getName(),
                match.getAwayTeam().getName(),
                match.getDate(),
                input.focusArea(),
                input.style(),
                input.riskLevel()
        );
    }
}
