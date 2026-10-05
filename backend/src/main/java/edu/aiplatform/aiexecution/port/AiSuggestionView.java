package edu.aiplatform.aiexecution.port;

import java.util.UUID;

public record AiSuggestionView(UUID id, String taskType, SuggestionStatus status, String resultJson) {}
