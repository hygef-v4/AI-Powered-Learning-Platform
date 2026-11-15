package edu.aiplatform.announcements.dto;

import java.time.Instant;
import java.util.*;

public final class AnnouncementViews {
    private AnnouncementViews() {}

    public record Item(
            UUID id,
            UUID classId,
            String className,
            String title,
            String body,
            boolean important,
            boolean sendEmail,
            UUID createdBy,
            String authorName,
            Instant createdAt,
            Instant updatedAt,
            long version,
            boolean canManage) {}

    /** {@code term}: học kỳ của lớp (D-U04-05), có thể null ở chỗ tạo tay. */
    public record ClassOption(
            UUID classId, String code, String subjectCode, boolean canManage, int studentCount,
            String term) {
        public ClassOption(
                UUID classId, String code, String subjectCode, boolean canManage, int studentCount) {
            this(classId, code, subjectCode, canManage, studentCount, null);
        }
    }

    public record Feed(
            List<Item> items, List<ClassOption> classes, int page, int size, boolean hasMore) {}
}
