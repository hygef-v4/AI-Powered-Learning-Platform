package edu.aiplatform.aiexecution.port;

public record RunLimits(double cpuSeconds, int memoryKb, double wallSeconds) {}
