package edu.aiplatform.notifications.service;

import edu.aiplatform.identity.contract.*;
import edu.aiplatform.notifications.repository.INotificationRepository;
import edu.aiplatform.shared.error.ApiException;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(timeout = 10)
public class NotificationPreferenceService {
    public static final Set<String> TYPES =
            Set.of(
                    "ENROLLED",
                    "ASSIGNMENT_OPENED",
                    "GRADE_PUBLISHED",
                    "DEADLINE_REMINDER",
                    "ANNOUNCEMENT_IMPORTANT");

    public static Set<String> listTypesForRole(Role role) {
        return switch (role) {
            case STUDENT -> TYPES;
            case TEACHER -> Set.of("LEADER_CHANGE_REQUESTED", "QUESTION_REVIEWED");
            case SUBJECT_MANAGER ->
                    Set.of("LEADER_CHANGE_REQUESTED", "QUESTION_REVIEWED", "QUESTION_PROPOSED");
            case ADMIN -> Set.of("BACKUP_FAILED");
        };
    }

    /** Mọi loại email của mọi vai trò mặc định bật (yêu cầu 2026-11-05); người dùng tự tắt nếu không cần. */
    public static boolean isEnabledByDefault(String type) {
        return true;
    }

    private final INotificationRepository notificationRepository;
    private final NotificationAccess notificationAccess;

    public NotificationPreferenceService(
            INotificationRepository notificationRepository, NotificationAccess notificationAccess) {
        this.notificationRepository = notificationRepository;
        this.notificationAccess = notificationAccess;
    }

    public Map<String, Boolean> loadPreferences(Actor actor) {
        UUID id = notificationAccess.requireSelf(actor);
        var stored = notificationRepository.findPreferences(id);
        Map<String, Boolean> values = new TreeMap<>();
        listTypesForRole(actor.role())
                .forEach(
                        t ->
                                values.put(
                                        t,
                                        "BACKUP_FAILED".equals(t)
                                                || stored.getOrDefault(t, isEnabledByDefault(t))));
        return values;
    }

    public Map<String, Boolean> updatePreferences(Actor actor, Map<String, Boolean> values) {
        UUID id = notificationAccess.requireSelf(actor);
        if (values == null
                || values.isEmpty()
                || values.keySet().stream().anyMatch(Objects::isNull)
                || !listTypesForRole(actor.role()).containsAll(values.keySet())
                || values.values().stream().anyMatch(Objects::isNull)
                || Boolean.FALSE.equals(values.get("BACKUP_FAILED")))
            throw ApiException.badRequest(
                    "NOTIFICATION_PREFERENCE_INVALID", "Loại email không hợp lệ");
        values.forEach((type, enabled) -> notificationRepository.savePreference(id, type, enabled));
        return loadPreferences(actor);
    }
}
