package com.ai.coach.service;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class AiClient {

    // TODO: inject WebClient and call your AI model
    public Mono<String> generateTacticalAdvice(String prompt) {
        String demo = """
                Suggested tactics:
                - Press high on the right flank.
                - Overload half-spaces with your #8 and #10.
                - Switch to 4-3-3 in possession, 4-1-4-1 in defence.
                """;
        return Mono.just(demo);
    }

    // Season-long tactical / development plan
    public Mono<String> generateSeasonPlan(String prompt) {
        String demo = """
                Season plan:
                - Phase 1 (Rounds 1–10): Stabilise defence, focus on compact mid-block.
                - Phase 2 (Rounds 11–20): Introduce higher pressing and rotations.
                - Phase 3 (Rounds 21–30): Refine automatisms, add set-piece variations.
                """;
        return Mono.just(demo);
    }

    // Weekly training / microcycle plan
    public Mono<String> generateTrainingPlan(String prompt) {
        String demo = """
                Training microcycle:
                - Monday: Recovery + video tactical review.
                - Tuesday: Small-sided games, high-intensity pressing drills.
                - Wednesday: Tactical 11v11, build-up under pressure.
                - Thursday: Finishing + set pieces.
                - Friday: Low-intensity walkthrough + rest.
                """;
        return Mono.just(demo);
    }
}
