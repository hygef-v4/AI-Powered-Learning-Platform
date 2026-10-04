package edu.aiplatform.assessments.port;

import java.math.BigDecimal;
import java.util.UUID;

public record AssignmentQuestionRef(UUID questionId, int orderNo, BigDecimal points) {}
