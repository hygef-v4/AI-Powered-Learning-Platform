package edu.aiplatform.assignments.contract;

import java.util.UUID;

public record ValidationIssue(String code, UUID blockId, String message) {
}
