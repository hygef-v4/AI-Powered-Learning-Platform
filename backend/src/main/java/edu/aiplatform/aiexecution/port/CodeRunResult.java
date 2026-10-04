package edu.aiplatform.aiexecution.port;

import java.math.BigDecimal;

public record CodeRunResult(CodeRunKind kind, String status, BigDecimal score, String resultsJson, String contentHash) {}
