package com.ai.coach.service;

import com.ai.coach.domain.dto.MatchAnalysisInput;
import com.ai.coach.domain.entity.Match;
import com.ai.coach.domain.entity.MatchAnalysis;
import com.ai.coach.domain.repository.MatchAnalysisRepository;
import com.ai.coach.domain.repository.MatchRepository;
import com.ai.coach.exception.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MatchAnalysisService {

    private final MatchRepository matchRepository;
    private final MatchAnalysisRepository matchAnalysisRepository;
    private final AiClient aiClient;

    @Transactional
    public MatchAnalysis generateMatchAnalysis(MatchAnalysisInput input) {
        Match match = matchRepository.findById(input.matchId())
                .orElseThrow(() -> new EntityNotFoundException("Match", input.matchId()));

        String prompt = buildPrompt(match, input);

        String aiResponse = aiClient.generateTacticalAdvice(prompt)
                .blockOptional()
                .orElse("No analysis generated.");

        List<String> keyFactors = List.of(aiResponse.split("\\n"));

        MatchAnalysis analysis = MatchAnalysis.builder()
                .match(match)
                .focusArea(input.focusArea())
                .style(input.style())
                .riskLevel(input.riskLevel())
                .summary(aiResponse)
                .keyFactors(keyFactors)
                .createdAt(OffsetDateTime.now())
                .build();

        return matchAnalysisRepository.save(analysis);
    }

    private String buildPrompt(Match match, MatchAnalysisInput input) {
        return """
                You are an elite football tactical coach.
                Analyse the upcoming match with the following context:

                Match ID: %d
                Home Team: %s
                Away Team: %s
                Focus Area: %s
                Style: %s
                Risk Level: %s

                Consider recent form, strengths, weaknesses, and tactical nuances.
                Provide a concise, coach-ready summary with bullet points.
                """.formatted(
                match.getId(),
                match.getHomeTeam().getName(),
                match.getAwayTeam().getName(),
                input.focusArea(),
                input.style(),
                input.riskLevel()
        );
    }
}
