package edu.aiplatform.grading.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.assignments.contract.RubricView;
import edu.aiplatform.shared.error.ApiException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
            return parsed == null ? Map.of() : copyChecks(parsed);
        } catch (Exception ex) {
            return Map.of();
        }
    }

    static BigDecimal score(List<RubricView> rubrics, Map<UUID, Set<UUID>> checks) {
        return score(rubrics, checks, Map.of());
    }

    static BigDecimal score(List<RubricView> rubrics, Map<UUID, Set<UUID>> checks,
            Map<UUID, Map<UUID, BigDecimal>> itemScores) {
        if (checks == null)
            throw ApiException.badRequest("INVALID_RUBRIC", "Missing rubric checks");
        itemScores = itemScores == null ? Map.of() : itemScores;
        Set<UUID> known = new HashSet<>();
        BigDecimal total = BigDecimal.ZERO;
        for (var rubric : rubrics) {
            known.add(rubric.rubricId());
            var selected = checks.getOrDefault(rubric.rubricId(), Set.of());
            var awarded = itemScores.getOrDefault(rubric.rubricId(), Map.of());
            if (selected == null || awarded == null)
                throw invalidItemScore();
            Set<UUID> allowed = new HashSet<>();
            for (var criterion : rubric.criteria()) {
                for (var item : criterion.items()) {
                    allowed.add(item.itemId());
                    if (awarded.containsKey(item.itemId())) {
                        var points = awarded.get(item.itemId());
                        if (points == null || points.scale() > 2 || points.signum() < 0
                                || points.compareTo(item.points()) > 0)
                            throw invalidItemScore();
                        total = total.add(points);
                    } else if (selected.contains(item.itemId())) {
                        total = total.add(item.points());
                    }
                }
            }
            if (!allowed.containsAll(selected) || !allowed.containsAll(awarded.keySet()))
                throw ApiException.badRequest("INVALID_RUBRIC", "Unknown rubric item");
        }
        if (!known.containsAll(checks.keySet()) || !known.containsAll(itemScores.keySet()))
            throw ApiException.badRequest("INVALID_RUBRIC", "Unknown rubric");
        return total;
    }

    static Map<UUID, Map<UUID, BigDecimal>> effectiveItemScores(List<RubricView> rubrics,
            Map<UUID, Set<UUID>> checks, Map<UUID, Map<UUID, BigDecimal>> itemScores) {
        if (rubrics == null || rubrics.isEmpty()) {
            return Map.of();
        }
        checks = checks == null ? Map.of() : checks;
        itemScores = itemScores == null ? Map.of() : itemScores;
        var result = new LinkedHashMap<UUID, Map<UUID, BigDecimal>>();
        for (var rubric : rubrics) {
            var selected = checks.getOrDefault(rubric.rubricId(), Set.of());
            var awarded = itemScores.getOrDefault(rubric.rubricId(), Map.of());
            var rubricMap = new LinkedHashMap<UUID, BigDecimal>();
            for (var criterion : rubric.criteria()) {
                for (var item : criterion.items()) {
                    BigDecimal val;
                    if (awarded != null && awarded.get(item.itemId()) != null) {
                        val = awarded.get(item.itemId()).setScale(2, RoundingMode.HALF_UP);
                    } else if (selected != null && selected.contains(item.itemId())) {
                        val = item.points().setScale(2, RoundingMode.HALF_UP);
                    } else {
                        val = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
                    }
                    rubricMap.put(item.itemId(), val);
                }
            }
            result.put(rubric.rubricId(), Map.copyOf(rubricMap));
        }
        return Map.copyOf(result);
    }

    static Map<UUID, Map<UUID, BigDecimal>> copyItemScores(Map<UUID, Map<UUID, BigDecimal>> scores) {
        if (scores == null || scores.isEmpty())
            return Map.of();
        var copy = new LinkedHashMap<UUID, Map<UUID, BigDecimal>>();
        scores.forEach((id, values) -> copy.put(id, values == null ? Map.of() : Map.copyOf(values)));
        return Map.copyOf(copy);
    }

    static Map<UUID, Set<UUID>> copyChecks(Map<UUID, Set<UUID>> checks) {
        if (checks == null || checks.isEmpty())
            return Map.of();
        var copy = new LinkedHashMap<UUID, Set<UUID>>();
        checks.forEach((id, values) -> copy.put(id, values == null ? Set.of() : Set.copyOf(values)));
        return Map.copyOf(copy);
    }

    private static ApiException invalidItemScore() {
        return ApiException.badRequest("INVALID_RUBRIC_SCORE",
                "Điểm từng mục phải từ 0 đến điểm tối đa, với tối đa 2 chữ số thập phân.");
    }
}
