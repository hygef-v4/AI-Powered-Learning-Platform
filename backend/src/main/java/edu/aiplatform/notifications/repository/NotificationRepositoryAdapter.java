package edu.aiplatform.notifications.repository;

import edu.aiplatform.notifications.model.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class NotificationRepositoryAdapter implements INotificationRepository {
    private static final String NOTIFICATION_COLUMNS =
            "id,recipient_id,type,source_event_id,title,summary,link,read_at,created_at";
    private static final String EMAIL_COLUMNS =
            "id,notification_id,recipient_id,type,status,priority,scheduled_for,attempts,"
                    + "last_error,created_at,sent_at,expires_at";
    private final JdbcClient jdbcClient;

    public NotificationRepositoryAdapter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public boolean insertNotification(Notification notification) {
        return jdbcClient
                        .sql("insert into notifications(id,recipient_id,type,source_event_id,title,summary,link,created_at) values (:id,:recipient,:type,:event,:title,:summary,:link,:now) on conflict(source_event_id,recipient_id,type) do nothing")
                        .param("id", notification.id)
                        .param("recipient", notification.recipientId)
                        .param("type", notification.type.name())
                        .param("event", notification.sourceEventId)
                        .param("title", notification.title)
                        .param("summary", notification.summary)
                        .param("link", notification.link)
                        .param("now", toTimestamp(notification.createdAt))
                        .update()
                == 1;
    }

    public Optional<Notification> findNotification(UUID id) {
        return jdbcClient
                .sql("select " + NOTIFICATION_COLUMNS + " from notifications where id = :id")
                .param("id", id)
                .query(NotificationRepositoryAdapter::mapNotification)
                .optional();
    }

    public List<Notification> listNotifications(
            UUID recipient, boolean unread, int page, int size) {
        return jdbcClient
                .sql("select " + NOTIFICATION_COLUMNS + " from notifications "
                        + "where recipient_id = :recipient"
                        + (unread ? " and read_at is null" : "")
                        + " order by created_at desc,id desc offset :offset limit :size")
                .param("recipient", recipient)
                .param("offset", (page - 1) * size)
                .param("size", size)
                .query(NotificationRepositoryAdapter::mapNotification)
                .list();
    }

    public long countUnread(UUID recipient) {
        return jdbcClient
                .sql("select count(*) from notifications "
                        + "where recipient_id = :id and read_at is null")
                .param("id", recipient)
                .query(Long.class)
                .single();
    }

    public boolean markRead(UUID id, UUID recipient, Instant now) {
        var notification = findNotification(id);
        if (notification.isEmpty() || !notification.get().recipientId.equals(recipient))
            return false;
        jdbcClient
                .sql("update notifications set read_at = :now "
                        + "where id = :id and recipient_id = :recipient and read_at is null")
                .param("now", toTimestamp(now))
                .param("id", id)
                .param("recipient", recipient)
                .update();
        return true;
    }

    public void markAllRead(UUID recipient, Instant now) {
        jdbcClient
                .sql("update notifications set read_at = :now "
                        + "where recipient_id = :recipient and read_at is null")
                .param("now", toTimestamp(now))
                .param("recipient", recipient)
                .update();
    }

    public Map<String, Boolean> findPreferences(UUID account) {
        Map<String, Boolean> result = new HashMap<>();
        jdbcClient
                .sql("select type,email_enabled from notification_preferences "
                        + "where account_id = :id")
                .param("id", account)
                .query(row -> {
                    result.put(row.getString("type"), row.getBoolean("email_enabled"));
                });
        return result;
    }

    public void savePreference(UUID account, String type, boolean enabled) {
        jdbcClient
                .sql("insert into notification_preferences(account_id,type,email_enabled) values (:id,:type,:enabled) on conflict(account_id,type) do update set email_enabled=excluded.email_enabled")
                .param("id", account)
                .param("type", type)
                .param("enabled", enabled)
                .update();
    }

    public void insertEmailDelivery(EmailDelivery delivery) {
        jdbcClient
                .sql("insert into email_deliveries(" + EMAIL_COLUMNS + ") values (:id,"
                        + ":notificationId,:recipientId,:type,:status,:priority,:scheduledFor,"
                        + ":attempts,:lastError,:createdAt,:sentAt,:expiresAt)")
                .param("id", delivery.id)
                .param("notificationId", delivery.notificationId)
                .param("recipientId", delivery.recipientId)
                .param("type", delivery.type)
                .param("status", delivery.status.name())
                .param("priority", delivery.priority)
                .param("scheduledFor", toTimestamp(delivery.scheduledFor))
                .param("attempts", delivery.attempts)
                .param("lastError", delivery.lastError)
                .param("createdAt", toTimestamp(delivery.createdAt))
                .param("sentAt", toTimestamp(delivery.sentAt))
                .param("expiresAt", toTimestamp(delivery.expiresAt))
                .update();
    }

    public void updateEmailDelivery(EmailDelivery delivery) {
        jdbcClient
                .sql("update email_deliveries set status = :status, priority = :priority, "
                        + "scheduled_for = :scheduledFor, attempts = :attempts, "
                        + "last_error = :lastError, sent_at = :sentAt, expires_at = :expiresAt "
                        + "where id = :id")
                .param("status", delivery.status.name())
                .param("priority", delivery.priority)
                .param("scheduledFor", toTimestamp(delivery.scheduledFor))
                .param("attempts", delivery.attempts)
                .param("lastError", delivery.lastError)
                .param("sentAt", toTimestamp(delivery.sentAt))
                .param("expiresAt", toTimestamp(delivery.expiresAt))
                .param("id", delivery.id)
                .update();
    }

    /**
     * Hibernate từng bỏ qua hint lock.timeout trên PostgreSQL; giữ hành vi đó: chỉ chịu giới hạn
     * lock_timeout/transaction timeout mà caller đã đặt (lockEmailBudget đặt 2s).
     */
    public Optional<EmailDelivery> lockEmailDelivery(UUID id) {
        return jdbcClient
                .sql("select " + EMAIL_COLUMNS + " from email_deliveries where id = :id "
                        + "for no key update")
                .param("id", id)
                .query(NotificationRepositoryAdapter::mapEmailDelivery)
                .optional();
    }

    public List<EmailDelivery> listPendingEmails(Instant olderThan) {
        return jdbcClient
                .sql("select " + EMAIL_COLUMNS + " from email_deliveries "
                        + "where status = :status and scheduled_for < :time "
                        + "order by scheduled_for,id limit 100")
                .param("status", EmailDelivery.Status.PENDING.name())
                .param("time", toTimestamp(olderThan))
                .query(NotificationRepositoryAdapter::mapEmailDelivery)
                .list();
    }

    public List<EmailDelivery> listDeferredEmails(Instant now) {
        return jdbcClient
                .sql("select " + EMAIL_COLUMNS + " from email_deliveries "
                        + "where status = :status and scheduled_for <= :now "
                        + "order by priority desc,created_at,id limit 300")
                .param("status", EmailDelivery.Status.DEFERRED.name())
                .param("now", toTimestamp(now))
                .query(NotificationRepositoryAdapter::mapEmailDelivery)
                .list();
    }

    public long countReservedEmails(Instant from, Instant to) {
        return jdbcClient
                .sql("select count(*) from email_deliveries where status in (:statuses) "
                        + "and scheduled_for >= :from and scheduled_for < :to")
                .param(
                        "statuses",
                        List.of(EmailDelivery.Status.PENDING.name(), EmailDelivery.Status.SENT.name()))
                .param("from", toTimestamp(from))
                .param("to", toTimestamp(to))
                .query(Long.class)
                .single();
    }

    public void lockEmailBudget() {
        jdbcClient.sql("select set_config('lock_timeout','2s',true)").query(String.class).single();
        jdbcClient.sql("select 1 from pg_advisory_xact_lock(14002000)").query(Integer.class).single();
    }

    public boolean insertReminder(
            UUID assignment, UUID classId, Instant closesAt, String status, Instant now) {
        return jdbcClient
                        .sql("insert into deadline_reminders(assignment_id,class_id,closes_at,status,processed_at) values (:assignment,:class,:closes,:status,:now) on conflict do nothing")
                        .param("assignment", assignment)
                        .param("class", classId)
                        .param("closes", toTimestamp(closesAt))
                        .param("status", status)
                        .param("now", toTimestamp(now))
                        .update()
                == 1;
    }

    public int deleteOldNotifications(Instant cutoff) {
        return jdbcClient
                .sql("delete from notifications where id in (select id from notifications where created_at<:cutoff order by created_at,id limit 10000)")
                .param("cutoff", toTimestamp(cutoff))
                .update();
    }

    private static Timestamp toTimestamp(Instant time) {
        return time == null ? null : Timestamp.from(time);
    }

    private static Instant readInstant(ResultSet row, String column) throws SQLException {
        Timestamp time = row.getTimestamp(column);
        return time == null ? null : time.toInstant();
    }

    private static Notification mapNotification(ResultSet row, int rowNumber)
            throws SQLException {
        var notification = new Notification();
        notification.id = row.getObject("id", UUID.class);
        notification.recipientId = row.getObject("recipient_id", UUID.class);
        notification.type = NotificationType.valueOf(row.getString("type"));
        notification.sourceEventId = row.getObject("source_event_id", UUID.class);
        notification.title = row.getString("title");
        notification.summary = row.getString("summary");
        notification.link = row.getString("link");
        notification.readAt = readInstant(row, "read_at");
        notification.createdAt = readInstant(row, "created_at");
        return notification;
    }

    private static EmailDelivery mapEmailDelivery(ResultSet row, int rowNumber)
            throws SQLException {
        var delivery = new EmailDelivery();
        delivery.id = row.getObject("id", UUID.class);
        delivery.notificationId = row.getObject("notification_id", UUID.class);
        delivery.recipientId = row.getObject("recipient_id", UUID.class);
        delivery.type = row.getString("type");
        delivery.status = EmailDelivery.Status.valueOf(row.getString("status"));
        delivery.priority = row.getInt("priority");
        delivery.scheduledFor = readInstant(row, "scheduled_for");
        delivery.attempts = row.getInt("attempts");
        delivery.lastError = row.getString("last_error");
        delivery.createdAt = readInstant(row, "created_at");
        delivery.sentAt = readInstant(row, "sent_at");
        delivery.expiresAt = readInstant(row, "expires_at");
        return delivery;
    }
}
