package edu.aiplatform.aiexecution.port;

import java.util.List;

public record AiPrompt(String system, String user, List<String> contextPassages) {}
