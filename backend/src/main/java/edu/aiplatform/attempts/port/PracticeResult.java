package edu.aiplatform.attempts.port;

import java.math.BigDecimal;

public record PracticeResult(BigDecimal score, BigDecimal maxScore, String feedback, String detailJson) {}
