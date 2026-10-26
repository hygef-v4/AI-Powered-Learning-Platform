package edu.aiplatform.grading.domain;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import edu.aiplatform.shared.error.ApiException;
import java.util.List;
import java.util.UUID;

/**
 * Versioned structured grading feedback for overall summary and per-rubric-item
 * evidence, explanation, improvement suggestion, and teacher review flags.
 */
public record GradingFeedback(int schemaVersion, Summary summary, List<Item> items) {

    @JsonAnySetter
    public void unknown(String key, Object value) {
        throw ApiException.badRequest("INVALID_STRUCTURED_FEEDBACK", "Unknown structuredFeedback field: " + key);
    }

    public GradingFeedback copy() {
        return new GradingFeedback(
                schemaVersion,
                summary == null ? new Summary(List.of(), List.of(), List.of()) : summary.copy(),
                items == null ? List.of() : items.stream().map(Item::copy).toList());
    }

    public record Summary(List<String> strengths, List<String> improvements, List<String> nextSteps) {
        @JsonAnySetter
        public void unknown(String key, Object value) {
            throw ApiException.badRequest("INVALID_STRUCTURED_FEEDBACK", "Unknown summary field: " + key);
        }

        public Summary copy() {
            return new Summary(
                    strengths == null ? List.of() : List.copyOf(strengths),
                    improvements == null ? List.of() : List.copyOf(improvements),
                    nextSteps == null ? List.of() : List.copyOf(nextSteps));
        }
    }

    public record Item(UUID rubricId, UUID itemId, String evidence, String reason, String suggestion,
            String evidenceBlockId, boolean requiresReview, String reviewReason) {
        @JsonAnySetter
        public void unknown(String key, Object value) {
            throw ApiException.badRequest("INVALID_STRUCTURED_FEEDBACK", "Unknown item field: " + key);
        }

        public Item copy() {
            return new Item(rubricId, itemId, evidence, reason, suggestion, evidenceBlockId, requiresReview,
                    reviewReason);
        }
    }
}
