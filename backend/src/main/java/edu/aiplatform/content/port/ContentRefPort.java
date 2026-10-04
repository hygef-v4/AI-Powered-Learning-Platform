package edu.aiplatform.content.port;

import java.util.UUID;

/** U05 cung cấp cho U06, U08: kiểm module/học liệu thuộc phạm vi. */
public interface ContentRefPort {
    boolean moduleInSubject(UUID moduleId, UUID subjectId);
    /** `classId` rỗng: chỉ học liệu của môn. */
    boolean lessonInScope(UUID lessonId, UUID subjectId, UUID classId);
}
