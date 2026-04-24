package com.ai.coach.service;

import com.ai.coach.exception.AiGenerationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Component
public class AiClient {

    private static final String TACTICAL_SYSTEM_PROMPT =
            "You are an expert football/soccer tactical analyst. "
                    + "Provide concise, actionable tactical advice based on the match data and context provided.";

    private static final String SEASON_PLAN_SYSTEM_PROMPT =
            "You are an expert football/soccer season planner and sporting director. "
                    + "Create structured, phased season plans that cover tactical development, "
                    + "squad rotation, and periodisation.";

    private static final String TRAINING_PLAN_SYSTEM_PROMPT =
            "You are an expert football/soccer fitness and training coach. "
                    + "Design detailed weekly microcycles and training sessions that balance "
                    + "intensity, recovery, and tactical preparation.";

    private final ChatClient tacticalClient;
    private final ChatClient seasonPlanClient;
    private final ChatClient trainingPlanClient;

    // Manual constructor: each ChatClient needs a distinct system prompt via builder.clone()
    public AiClient(ChatClient.Builder builder) {
        this.tacticalClient = builder.clone()
                .defaultSystem(TACTICAL_SYSTEM_PROMPT)
                .build();

        this.seasonPlanClient = builder.clone()
                .defaultSystem(SEASON_PLAN_SYSTEM_PROMPT)
                .build();

        this.trainingPlanClient = builder.clone()
                .defaultSystem(TRAINING_PLAN_SYSTEM_PROMPT)
                .build();
    }

    public Mono<String> generateTacticalAdvice(String prompt) {
        return callClient(tacticalClient, prompt, "Tactical advice");
    }

    public Mono<String> generateSeasonPlan(String prompt) {
        return callClient(seasonPlanClient, prompt, "Season plan");
    }

    public Mono<String> generateTrainingPlan(String prompt) {
        return callClient(trainingPlanClient, prompt, "Training plan");
    }

    private Mono<String> callClient(ChatClient client, String prompt, String context) {
        return Mono.fromCallable(() -> client.prompt().user(prompt).call().content())
                .subscribeOn(Schedulers.boundedElastic())
                .doOnError(e -> log.error("{} generation failed", context, e))
                .onErrorMap(e -> new AiGenerationException(context, e));
    }
}
