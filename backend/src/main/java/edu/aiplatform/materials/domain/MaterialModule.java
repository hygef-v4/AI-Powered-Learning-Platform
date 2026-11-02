package edu.aiplatform.materials.domain;

import java.time.Instant;
import java.util.UUID;

/** Module của môn: nhóm học liệu (của môn và của các lớp) và Quiz theo chương; Chủ nhiệm môn thêm, sửa, xóa, sắp xếp. */
public record MaterialModule(UUID id, UUID subjectId, String title, int orderNo, UUID createdBy,
                             Instant createdAt, Instant updatedAt, long version) {
    public MaterialModule {
        if (id == null || subjectId == null || createdBy == null || createdAt == null || updatedAt == null || version < 0)
            throw new IllegalArgumentException("Invalid module");
        title = title == null ? "" : title.strip();
        if (title.isEmpty() || title.length() > 200 || orderNo < 0)
            throw new IllegalArgumentException("Invalid module information");
    }

    public MaterialModule revise(String title, int order, Instant now) {
        return new MaterialModule(id, subjectId, title, order, createdBy, createdAt, now, version + 1);
    }

    /** Quiz gắn vào module, theo thứ tự hiện trong module. */
    public record QuizLink(UUID moduleId, UUID quizId, int orderNo) { }
}
