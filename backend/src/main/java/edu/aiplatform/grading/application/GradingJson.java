package edu.aiplatform.grading.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.assignments.contract.RubricView;
import edu.aiplatform.shared.error.ApiException;
import java.math.BigDecimal;
import java.util.HashSet;
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

    static BigDecimal score(List<RubricView> rubrics, Map<UUID, Set<UUID>> checks) {
        if (checks == null)
            throw ApiException.badRequest("INVALID_RUBRIC", "Missing rubric checks");
        Set<UUID> known = new HashSet<>();
        BigDecimal total = BigDecimal.ZERO;
        for (var rubric : rubrics) {
            known.add(rubric.rubricId());
            var selected = checks.getOrDefault(rubric.rubricId(), Set.of());
            if (selected == null)
                throw ApiException.badRequest("INVALID_RUBRIC", "Invalid rubric checks");
            Set<UUID> allowed = new HashSet<>();
            for (var criterion : rubric.criteria())
                for (var item : criterion.items()) {
                    allowed.add(item.itemId());
                    if (selected.contains(item.itemId()))
                        total = total.add(item.points());
                }
            if (!allowed.containsAll(selected))
                throw ApiException.badRequest("INVALID_RUBRIC", "Unknown rubric item");
        }
        if (!known.containsAll(checks.keySet()))
            throw ApiException.badRequest("INVALID_RUBRIC", "Unknown rubric");
        return total;
    }
}
