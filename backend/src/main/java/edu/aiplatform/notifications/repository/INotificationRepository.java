package edu.aiplatform.notifications.repository;

import edu.aiplatform.notifications.model.EmailDelivery;
import edu.aiplatform.notifications.model.Notification;
import java.time.Instant;
import java.util.*;

public interface INotificationRepository {
    boolean insertNotification(Notification notification);

    Optional<Notification> findNotification(UUID id);

    List<Notification> listNotifications(UUID recipient, boolean unread, int page, int size);

    long countUnread(UUID recipient);

    boolean markRead(UUID id, UUID recipient, Instant now);

    void markAllRead(UUID recipient, Instant now);

    Map<String, Boolean> findPreferences(UUID account);

    void savePreference(UUID account, String type, boolean enabled);

    void insertEmailDelivery(EmailDelivery delivery);

    /** Ghi trạng thái gửi (status, lịch, số lần thử, lỗi, thời điểm gửi) của email đã khóa. */
    void updateEmailDelivery(EmailDelivery delivery);

    Optional<EmailDelivery> lockEmailDelivery(UUID id);

    List<EmailDelivery> listPendingEmails(Instant olderThan);

    List<EmailDelivery> listDeferredEmails(Instant now);

    long countReservedEmails(Instant from, Instant to);

    void lockEmailBudget();

    boolean insertReminder(
            UUID assignment, UUID classId, Instant closesAt, String status, Instant now);

    int deleteOldNotifications(Instant cutoff);
}
