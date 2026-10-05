package edu.aiplatform.content.port;

import java.util.List;
import java.util.UUID;

/** Chỉ U13 gọi; tính credit embedding cho `requesterId` (BR-U05-40…44). */
public interface RagRetrievalPort {
    List<LessonPassage> retrieve(RagScope scope, String query, int k, UUID requesterId, String requestRef);
}
