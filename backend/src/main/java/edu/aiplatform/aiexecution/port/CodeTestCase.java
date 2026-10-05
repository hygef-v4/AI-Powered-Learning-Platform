package edu.aiplatform.aiexecution.port;

import java.math.BigDecimal;

public record CodeTestCase(String id, String stdin, String expectedOutput, boolean hidden, BigDecimal weight) {}
