package edu.aiplatform.announcements.model;

import edu.aiplatform.shared.error.ApiException;
import java.time.Instant;
import java.util.UUID;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;

public class Announcement {
    public enum Status {
        ACTIVE,
        DELETED
    }

    public UUID id;

    public UUID classId;

    public String title;

    public String body;

    public boolean important;
    public boolean sendEmail;

    public Status status;

    public UUID createdBy;
    public Instant createdAt;
    public UUID updatedBy;
    public Instant updatedAt;
    public UUID deletedBy;
    public Instant deletedAt;
    public long version;

    public Announcement() {}

    public static Announcement createAnnouncement(
            UUID classId,
            UUID actor,
            String title,
            String body,
            boolean important,
            boolean email,
            Instant now) {
        if (email && !important)
            throw ApiException.createBadRequest(
                    "ANNOUNCEMENT_EMAIL_INVALID", "Chỉ thông báo quan trọng mới có email");
        var announcement = new Announcement();
        announcement.id = UUID.randomUUID();
        announcement.classId = classId;
        announcement.createdBy = actor;
        announcement.updatedBy = actor;
        announcement.createdAt = now;
        announcement.updatedAt = now;
        announcement.important = important;
        announcement.sendEmail = email;
        announcement.status = Status.ACTIVE;
        announcement.title = cleanTitle(title);
        announcement.body = cleanBody(body);
        return announcement;
    }

    public void edit(String title, String body, UUID actor, Instant now) {
        String nextTitle = cleanTitle(title), nextBody = cleanBody(body);
        this.title = nextTitle;
        this.body = nextBody;
        updatedBy = actor;
        updatedAt = now;
    }

    public void markDeleted(UUID actor, Instant now) {
        status = Status.DELETED;
        deletedBy = actor;
        deletedAt = now;
        updatedBy = actor;
        updatedAt = now;
    }

    private static String requireTextLength(String text, int max) {
        String value = text == null ? "" : text.strip();
        if (value.isEmpty() || value.codePointCount(0, value.length()) > max)
            throw ApiException.createBadRequest(
                    "ANNOUNCEMENT_INVALID", "Kiểm tra độ dài tiêu đề và nội dung");
        return value;
    }

    private static String cleanTitle(String title) {
        String value = requireTextLength(title, 200);
        if (!Jsoup.isValid(value, Safelist.none()))
            throw ApiException.createBadRequest("ANNOUNCEMENT_INVALID", "Tiêu đề phải là văn bản thuần");
        return value;
    }

    private static String cleanBody(String body) {
        String value = requireTextLength(body, 5000);
        // Keep Markdown/newlines, drop tags/attributes and script/style data. Never unescape
        // the output back into HTML; the renderer also enforces its Markdown URL allowlist.
        String cleaned =
                Jsoup.clean(
                                value,
                                "",
                                Safelist.none(),
                                new Document.OutputSettings().prettyPrint(false))
                        .strip();
        return requireTextLength(cleaned, 5000);
    }
}
