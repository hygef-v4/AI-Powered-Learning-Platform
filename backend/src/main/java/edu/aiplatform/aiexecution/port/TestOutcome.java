package edu.aiplatform.aiexecution.port;

public record TestOutcome(String testId, String status, double timeSeconds, int memoryKb, String stdout, String stderr) {}
