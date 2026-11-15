package edu.aiplatform.notifications.repository;

import edu.aiplatform.notifications.model.*;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Repository;

@Repository
public class NotificationRepositoryAdapter implements INotificationRepository {
    private final EntityManager entityManager;

    public NotificationRepositoryAdapter(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public boolean insertNotification(Notification notification) {
        return entityManager.createNativeQuery(
                                "insert into notifications(id,recipient_id,type,source_event_id,title,summary,link,created_at) values (:id,:recipient,:type,:event,:title,:summary,:link,:now) on conflict(source_event_id,recipient_id,type) do nothing")
                        .setParameter("id", notification.id)
                        .setParameter("recipient", notification.recipientId)
                        .setParameter("type", notification.type.name())
                        .setParameter("event", notification.sourceEventId)
                        .setParameter("title", notification.title)
                        .setParameter("summary", notification.summary)
                        .setParameter("link", notification.link)
                        .setParameter("now", notification.createdAt)
                        .executeUpdate()
                == 1;
    }

    public Optional<Notification> findNotification(UUID id) {
        return Optional.ofNullable(entityManager.find(Notification.class, id));
    }

    public List<Notification> listNotifications(
            UUID recipient, boolean unread, int page, int size) {
        return entityManager.createQuery(
                        "select n from Notification n where n.recipientId=:recipient"
                                + (unread ? " and n.readAt is null" : "")
                                + " order by n.createdAt desc,n.id desc",
                        Notification.class)
                .setParameter("recipient", recipient)
                .setFirstResult((page - 1) * size)
                .setMaxResults(size)
                .getResultList();
    }

    public long countUnread(UUID recipient) {
        return entityManager.createQuery(
                        "select count(n) from Notification n where n.recipientId=:id and n.readAt is null",
                        Long.class)
                .setParameter("id", recipient)
                .getSingleResult();
    }

    public boolean markRead(UUID id, UUID recipient, Instant now) {
        var notification = findNotification(id);
        if (notification.isEmpty() || !notification.get().recipientId.equals(recipient))
            return false;
        entityManager.createQuery(
                        "update Notification n set n.readAt=:now where n.id=:id and n.recipientId=:recipient and n.readAt is null")
                .setParameter("now", now)
                .setParameter("id", id)
                .setParameter("recipient", recipient)
                .executeUpdate();
        return true;
    }

    public void markAllRead(UUID recipient, Instant now) {
        entityManager.createQuery(
                        "update Notification n set n.readAt=:now where n.recipientId=:recipient and n.readAt is null")
                .setParameter("now", now)
                .setParameter("recipient", recipient)
                .executeUpdate();
    }

    public Map<String, Boolean> findPreferences(UUID account) {
        Map<String, Boolean> result = new HashMap<>();
        entityManager.createQuery(
                        "select p from NotificationPreference p where p.accountId=:id",
                        NotificationPreference.class)
                .setParameter("id", account)
                .getResultList()
                .forEach(p -> result.put(p.type, p.emailEnabled));
        return result;
    }

    public void savePreference(UUID account, String type, boolean enabled) {
        entityManager.createNativeQuery(
                        "insert into notification_preferences(account_id,type,email_enabled) values (:id,:type,:enabled) on conflict(account_id,type) do update set email_enabled=excluded.email_enabled")
                .setParameter("id", account)
                .setParameter("type", type)
                .setParameter("enabled", enabled)
                .executeUpdate();
    }

    public void insertEmailDelivery(EmailDelivery delivery) {
        entityManager.persist(delivery);
    }

    public Optional<EmailDelivery> lockEmailDelivery(UUID id) {
        return Optional.ofNullable(
                entityManager.find(
                        EmailDelivery.class,
                        id,
                        LockModeType.PESSIMISTIC_WRITE,
                        Map.of("jakarta.persistence.lock.timeout", 2000)));
    }

    public List<EmailDelivery> listPendingEmails(Instant olderThan) {
        return entityManager.createQuery(
                        "select e from EmailDelivery e where e.status=:status and e.scheduledFor<:time order by e.scheduledFor,e.id",
                        EmailDelivery.class)
                .setParameter("status", EmailDelivery.Status.PENDING)
                .setParameter("time", olderThan)
                .setMaxResults(100)
                .getResultList();
    }

    public List<EmailDelivery> listDeferredEmails(Instant now) {
        return entityManager.createQuery(
                        "select e from EmailDelivery e where e.status=:status and e.scheduledFor<=:now order by e.priority desc,e.createdAt,e.id",
                        EmailDelivery.class)
                .setParameter("status", EmailDelivery.Status.DEFERRED)
                .setParameter("now", now)
                .setMaxResults(300)
                .getResultList();
    }

    public long countReservedEmails(Instant from, Instant to) {
        return entityManager.createQuery(
                        "select count(e) from EmailDelivery e where e.status in :statuses and e.scheduledFor>=:from and e.scheduledFor<:to",
                        Long.class)
                .setParameter(
                        "statuses",
                        List.of(EmailDelivery.Status.PENDING, EmailDelivery.Status.SENT))
                .setParameter("from", from)
                .setParameter("to", to)
                .getSingleResult();
    }

    public void lockEmailBudget() {
        entityManager
                .createNativeQuery("select set_config('lock_timeout','2s',true)")
                .getSingleResult();
        entityManager
                .createNativeQuery("select 1 from pg_advisory_xact_lock(14002000)")
                .getSingleResult();
    }

    public boolean insertReminder(
            UUID assignment, UUID classId, Instant closesAt, String status, Instant now) {
        return entityManager.createNativeQuery(
                                "insert into deadline_reminders(assignment_id,class_id,closes_at,status,processed_at) values (:assignment,:class,:closes,:status,:now) on conflict do nothing")
                        .setParameter("assignment", assignment)
                        .setParameter("class", classId)
                        .setParameter("closes", closesAt)
                        .setParameter("status", status)
                        .setParameter("now", now)
                        .executeUpdate()
                == 1;
    }

    public int deleteOldNotifications(Instant cutoff) {
        return entityManager.createNativeQuery(
                        "delete from notifications where id in (select id from notifications where created_at<:cutoff order by created_at,id limit 10000)")
                .setParameter("cutoff", cutoff)
                .executeUpdate();
    }
}
