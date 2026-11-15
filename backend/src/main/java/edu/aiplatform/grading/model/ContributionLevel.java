package edu.aiplatform.grading.model;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum ContributionLevel {
    EXCEEDS_EXPECTATIONS,
    MEETS_EXPECTATIONS,
    PARTIALLY_MEETS,
    NEEDS_SUPPORT,
    NOT_COMPLETED;

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static ContributionLevel parseLevel(String value) {
        return valueOf(value);
    }
}
