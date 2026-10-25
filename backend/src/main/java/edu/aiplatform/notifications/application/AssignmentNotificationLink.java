package edu.aiplatform.notifications.application;

import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/** Internal student links; legacy paths remain readable by already queued reminder emails. */
final class AssignmentNotificationLink {
    private static final String UUID_PATTERN = "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}";
    private static final Pattern LINK = Pattern.compile("^/(?:my-assignments|assignments)/(" + UUID_PATTERN
            + ")(?:\\?classId=" + UUID_PATTERN + ")?$");

    private AssignmentNotificationLink() { }

    static String student(UUID assignmentId, UUID classId) {
        return "/my-assignments/" + Objects.requireNonNull(assignmentId) + "?classId=" + Objects.requireNonNull(classId);
    }

    static UUID assignmentId(String link) {
        if (link == null) throw new IllegalArgumentException("Missing assignment link");
        var match = LINK.matcher(link);
        if (!match.matches()) throw new IllegalArgumentException("Invalid assignment link");
        return UUID.fromString(match.group(1));
    }
}
