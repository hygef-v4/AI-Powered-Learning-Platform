package edu.aiplatform.aiexecution.port;

public record AiResult(String json, long inputTokens, long outputTokens) {}
