package edu.aiplatform.content.port;

import java.util.List;

public interface EmbeddingPort {
    List<float[]> embed(List<String> texts);
}
