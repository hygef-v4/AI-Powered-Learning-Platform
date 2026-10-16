package edu.aiplatform.assignments.contract;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Rubric của một phần, đã khóa khi phát hành. */
public record RubricView(UUID rubricId, UUID assignmentId, String partId, List<RubricCriterion> criteria,
                         BigDecimal maxPoints) {
}
