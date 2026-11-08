package edu.aiplatform.grading.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.assignments.contract.RubricView;
import edu.aiplatform.shared.error.ApiException;
import edu.aiplatform.grading.model.RubricScoring;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

final class GradingJson {
    /** Prevents instantiation of this JSON utility class. */
    private GradingJson() {
    }

    /** Serializes a value into the JSON format used for grading snapshots. */
    static String writeSnapshot(ObjectMapper json, Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot encode grading snapshot", ex);
        }
    }

    /** Reads rubric data from an evaluation snapshot. */
    static List<RubricView> readRubrics(ObjectMapper json, String value) {
        try {
            return json.readValue(value, new TypeReference<List<RubricView>>() {
            });
        } catch (Exception ex) {
            throw ApiException.unavailable("INVALID_RUBRIC_SNAPSHOT",
                    "Rubric snapshot unavailable");
        }
    }

    /** Reads checked rubric item IDs from stored JSON, returning an empty map when absent or invalid. */
    static Map<UUID, Set<UUID>> readChecks(ObjectMapper json, String value) {
        if (value == null || value.isBlank()) {
            return Map.of();
        }
        try {
            Map<UUID, Set<UUID>> parsed = json.readValue(value,
                    new TypeReference<Map<UUID, Set<UUID>>>() {
                    });
            return parsed == null ? Map.of() : RubricScoring.copyChecks(parsed);
        } catch (Exception ex) {
            return Map.of();
        }
    }

}
