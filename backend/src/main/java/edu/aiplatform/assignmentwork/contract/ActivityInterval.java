package edu.aiplatform.assignmentwork.contract;

import java.time.Instant;

public record ActivityInterval(Instant start, Instant end, IntervalCertainty certainty) {
}
