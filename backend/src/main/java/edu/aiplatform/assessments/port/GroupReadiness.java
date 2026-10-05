package edu.aiplatform.assessments.port;

import java.util.List;

public record GroupReadiness(List<ReviewIssue> errors, List<ReviewIssue> warnings) {
    public boolean ready() { return errors.isEmpty(); }
}
