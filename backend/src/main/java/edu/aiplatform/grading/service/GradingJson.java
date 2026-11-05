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
    private GradingJson() {
    }

    static String write(ObjectMapper json, Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot encode grading snapshot", ex);
        }
    }

    static List<RubricView> rubrics(ObjectMapper json, String value) {
        try {
            return json.readValue(value, new TypeReference<List<RubricView>>() {
            });
        } catch (Exception ex) {
            throw ApiException.unavailable("INVALID_RUBRIC_SNAPSHOT", "Rubric snapshot unavailable");
        }
    }

    static Map<UUID, Set<UUID>> readChecks(ObjectMapper json, String value) {
        if (value == null || value.isBlank()) {
            return Map.of();
        }
        try {
            Map<UUID, Set<UUID>> parsed = json.readValue(value, new TypeReference<Map<UUID, Set<UUID>>>() {
            });
            return parsed == null ? Map.of() : RubricScoring.copyChecks(parsed);
        } catch (Exception ex) {
            return Map.of();
        }
    }

}
