package com.ai.coach.domain;

import com.ai.coach.domain.entity.FatigueLevel;
import com.ai.coach.domain.entity.InjuryRisk;

/**
 * Stateless domain utility for workload-related calculations.
 * Pure functions — no side effects, no dependencies.
 */
public final class WorkloadCalculator {

    private WorkloadCalculator() {}

    /**
     * Fatigue level based on minutes played in last 28 days.
     *   0-180 min (<=2 full matches)  -> FRESH
     * 181-450 min (3-5 matches)       -> MODERATE
     * 451-720 min (6-8 matches)       -> TIRED
     *   720+ min                       -> EXHAUSTED
     */
    public static FatigueLevel computeFatigueLevel(int minutes) {
        if (minutes <= 180) return FatigueLevel.FRESH;
        if (minutes <= 450) return FatigueLevel.MODERATE;
        if (minutes <= 720) return FatigueLevel.TIRED;
        return FatigueLevel.EXHAUSTED;
    }

    /**
     * Injury risk combines fatigue level with match density.
     * High density (>=6 matches in 28 days) always -> HIGH.
     */
    public static InjuryRisk computeInjuryRisk(FatigueLevel fatigueLevel, int matches) {
        if (matches >= 6) return InjuryRisk.HIGH;
        return switch (fatigueLevel) {
            case EXHAUSTED -> InjuryRisk.HIGH;
            case TIRED -> InjuryRisk.MEDIUM;
            default -> InjuryRisk.LOW;
        };
    }
}
