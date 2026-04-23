package com.ai.coach.domain;

/**
 * Generic enum parser — eliminates parallel Map + parse-method structures.
 * Matches by case-insensitive name lookup against the enum's values.
 */
public final class EnumParser {

    private EnumParser() {}

    /**
     * Parse a string into an enum constant by case-insensitive name match.
     *
     * @param enumClass the enum type
     * @param value     the string to parse (may be null)
     * @param fallback  returned when value is null or doesn't match any constant
     * @return the matched enum constant, or fallback
     */
    public static <E extends Enum<E>> E parse(Class<E> enumClass, String value, E fallback) {
        if (value == null) return fallback;
        for (E constant : enumClass.getEnumConstants()) {
            if (constant.name().equalsIgnoreCase(value)) {
                return constant;
            }
        }
        return fallback;
    }
}
