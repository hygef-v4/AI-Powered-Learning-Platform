package edu.aiplatform.subjects.contract;

import java.util.UUID;

public record SubjectRef(
        UUID subjectId,
        String code,
        String name,
        SubjectStatus status,
        UUID managerId,
        String description) {

    /** Caller không cần mô tả môn (kiểm quyền, tham chiếu) giữ chữ ký cũ. */
    public SubjectRef(UUID subjectId, String code, String name, SubjectStatus status, UUID managerId) {
        this(subjectId, code, name, status, managerId, null);
    }
}
