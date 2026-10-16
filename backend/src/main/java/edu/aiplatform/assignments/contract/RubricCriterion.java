package edu.aiplatform.assignments.contract;

import java.util.List;
import java.util.UUID;

public record RubricCriterion(UUID criterionId, String title, String description, CriterionKind kind,
                              List<RubricItem> items) {
}
