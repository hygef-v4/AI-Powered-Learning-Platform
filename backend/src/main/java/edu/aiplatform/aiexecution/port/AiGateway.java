package edu.aiplatform.aiexecution.port;

/** Provider-neutral; `GeminiAdapter` là cài đặt duy nhất (P2). */
public interface AiGateway {
    AiResult generate(String task, AiPrompt prompt, String jsonSchema);
}
