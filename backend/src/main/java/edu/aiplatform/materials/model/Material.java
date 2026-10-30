package edu.aiplatform.materials.model;

import java.time.Instant;
import java.util.UUID;

/** Học liệu của môn hay của lớp; luôn thuộc một module của môn ({@code moduleId}). */
public record Material(UUID id, Scope scope, UUID subjectId, UUID classId, UUID moduleId, Kind kind, String title,
                       String description, int orderNo, Status status, int currentVersionNo,
                       UUID createdBy, Instant createdAt, Instant updatedAt, long version) {
    public enum Scope { SUBJECT, CLASS }
    public enum Kind { FILE, YOUTUBE_LINK }
    public enum Status { ACTIVE, ARCHIVED }

    public Material {
        if (id == null || scope == null || subjectId == null || moduleId == null || kind == null || status == null
                || createdBy == null || createdAt == null || updatedAt == null
                || (scope == Scope.CLASS) != (classId != null) || currentVersionNo < 1 || version < 0)
            throw new IllegalArgumentException("Invalid material");
        title = title == null ? "" : title.strip();
        description = description == null ? "" : description.strip();
        if (title.isEmpty() || title.length() > 200 || description.length() > 2000 || orderNo < 0)
            throw new IllegalArgumentException("Invalid material information");
    }

    public Material revise(String title, String description, int order, int contentVersion, Status status, Instant now) {
        return new Material(id, scope, subjectId, classId, moduleId, kind, title, description, order, status,
                contentVersion, createdBy, createdAt, now, version + 1);
    }

    /** Cùng học liệu ở module khác của cùng môn; gọi {@link #revise} sau đó để tăng version. */
    public Material inModule(UUID module) {
        return new Material(id, scope, subjectId, classId, module, kind, title, description, orderNo, status,
                currentVersionNo, createdBy, createdAt, updatedAt, version);
    }
}
