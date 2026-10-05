package edu.aiplatform.aiexecution.port;

import java.util.List;
import java.util.UUID;

/** QUESTION_DRAFT: questionType, count (1-20), difficulty; SKELETON_DRAFT: description (BR-U13-13). */
public record DraftRequest(String taskType, String targetType, UUID targetId, String questionType, Integer count,
                           String difficulty, String description, List<UUID> moduleIds, List<UUID> lessonIds, String note) {}
