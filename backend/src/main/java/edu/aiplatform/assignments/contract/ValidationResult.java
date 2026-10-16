package edu.aiplatform.assignments.contract;

import java.util.List;

public record ValidationResult(List<ValidationIssue> errors, List<ValidationIssue> warnings) {

    public boolean ok() {
        return errors.isEmpty();
    }
}
