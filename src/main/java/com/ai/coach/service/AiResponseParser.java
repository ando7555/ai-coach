package com.ai.coach.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiResponseParser {

    private final ObjectMapper objectMapper;

    public <T> T parseAiResponse(String aiResponse, Class<T> type, T fallback) {
        try {
            String json = stripMarkdownFences(aiResponse);
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            log.warn("Failed to parse AI response as {}: {}", type.getSimpleName(), e.getMessage());
            return fallback;
        }
    }

    public String stripMarkdownFences(String text) {
        String trimmed = text.strip();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            if (firstNewline != -1) {
                trimmed = trimmed.substring(firstNewline + 1);
            }
        }
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }
        return trimmed.strip();
    }
}
