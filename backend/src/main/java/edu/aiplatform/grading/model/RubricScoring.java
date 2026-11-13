package edu.aiplatform.grading.model;

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

public final class RubricScoring {
    /** Prevents instantiation of this scoring utility. */
    private RubricScoring() {
    }

    /** Totals selected rubric item points. */
    public static BigDecimal calculateScore(List<RubricView> rubrics, Map<UUID, Set<UUID>> checks) {
        return calculateScore(rubrics, checks, Map.of());
    }

    /** Totals awarded item scores, falling back to selected rubric point values. */
    public static BigDecimal calculateScore(List<RubricView> rubrics, Map<UUID, Set<UUID>> checks,
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
                throw createInvalidItemScore();
            Set<UUID> allowed = new HashSet<>();
            for (var criterion : rubric.criteria()) {
                for (var item : criterion.items()) {
                    if (item.points() == null) {
                        if (item.text() == null || item.text().isBlank()) continue;
                        throw ApiException.badRequest("INVALID_RUBRIC", "Rubric item is missing points");
                    }
                    allowed.add(item.itemId());
                    if (awarded.containsKey(item.itemId())) {
                        var points = awarded.get(item.itemId());
                        if (points == null || points.scale() > 2 || points.signum() < 0
                                || points.compareTo(item.points()) > 0)
                            throw createInvalidItemScore();
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

    /** Resolves each rubric item to its awarded score or zero. */
    public static Map<UUID, Map<UUID, BigDecimal>> calculateEffectiveItemScores(
            List<RubricView> rubrics, Map<UUID, Set<UUID>> checks,
            Map<UUID, Map<UUID, BigDecimal>> itemScores) {
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
                    if (item.points() == null) continue;
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

    /** Derives full-credit checks from validated effective item scores. */
    public static Map<UUID, Set<UUID>> synchronizeChecks(List<RubricView> rubrics,
            Map<UUID, Map<UUID, BigDecimal>> scores) {
        var result = new LinkedHashMap<UUID, Set<UUID>>();
        for (var rubric : rubrics) {
            var selected = new HashSet<UUID>();
            var awarded = scores.getOrDefault(rubric.rubricId(), Map.of());
            for (var criterion : rubric.criteria()) {
                for (var item : criterion.items()) {
                    if (item.points() != null && item.points().signum() > 0
                            && item.points().compareTo(awarded.getOrDefault(item.itemId(), BigDecimal.ZERO)) == 0)
                        selected.add(item.itemId());
                }
            }
            result.put(rubric.rubricId(), Set.copyOf(selected));
        }
        return Map.copyOf(result);
    }

    /** Copies rubric item scores into immutable nested maps. */
    public static Map<UUID, Map<UUID, BigDecimal>> copyItemScores(
            Map<UUID, Map<UUID, BigDecimal>> scores) {
        if (scores == null || scores.isEmpty())
            return Map.of();
        var copy = new LinkedHashMap<UUID, Map<UUID, BigDecimal>>();
        scores.forEach(
                (id, values) -> copy.put(id, values == null ? Map.of() : Map.copyOf(values)));
        return Map.copyOf(copy);
    }

    /** Copies rubric checks into immutable nested sets. */
    public static Map<UUID, Set<UUID>> copyChecks(Map<UUID, Set<UUID>> checks) {
        if (checks == null || checks.isEmpty())
            return Map.of();
        var copy = new LinkedHashMap<UUID, Set<UUID>>();
        checks.forEach(
                (id, values) -> copy.put(id, values == null ? Set.of() : Set.copyOf(values)));
        return Map.copyOf(copy);
    }

    /** Creates the validation error used for invalid rubric item scores. */
    private static ApiException createInvalidItemScore() {
        return ApiException.badRequest("INVALID_RUBRIC_SCORE",
                "Điểm từng mục phải từ 0 đến điểm tối đa, với tối đa 2 chữ số thập phân.");
    }
}
