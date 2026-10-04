package edu.aiplatform.content.port;

import java.util.List;
import java.util.UUID;

/** Phạm vi lớp: học liệu của môn và của lớp; phạm vi môn: chỉ học liệu của môn (BR-U05-41). */
public record RagScope(UUID subjectId, UUID classId, List<UUID> lessonIds) {}
