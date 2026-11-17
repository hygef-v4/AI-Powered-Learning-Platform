package edu.aiplatform.notifications.service;

import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.groupdocs.contract.ISseHub;
import edu.aiplatform.notifications.model.*;
import edu.aiplatform.notifications.repository.*;
import edu.aiplatform.shared.error.ApiException;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;

@Service
@Transactional(timeout = 15)
public class NotificationService {
    public record Item(
            UUID id,
            String type,
            String title,
            String summary,
            String link,
            Instant readAt,
            Instant createdAt) {}

    public record Feed(List<Item> items, int page, int size, boolean hasMore) {}

    private final INotificationRepository notificationRepository;
    private final NotificationAccess notificationAccess;
    private final EmailDeliveryService emailDeliveryService;
    private final ISseHub sseHub;
    private final Clock clock;

    public NotificationService(
            INotificationRepository notificationRepository,
            NotificationAccess notificationAccess,
            EmailDeliveryService emailDeliveryService,
            ISseHub sseHub,
            Clock clock) {
        this.notificationRepository = notificationRepository;
        this.notificationAccess = notificationAccess;
        this.emailDeliveryService = emailDeliveryService;
        this.sseHub = sseHub;
        this.clock = clock;
    }

    public void createNotification(
            UUID eventId,
            NotificationType type,
            Collection<UUID> recipients,
            String title,
            String summary,
            String link,
            String emailType,
            Instant expiresAt) {
        if (!link.startsWith("/")
                || link.startsWith("//")
                || link.contains("\\")
                || link.length() > 500
                || title.length() > 200
                || summary.length() > 500)
            throw new IllegalArgumentException("Invalid notification content");
        Set<UUID> changed = new HashSet<>();
        for (UUID recipient : new LinkedHashSet<>(recipients)) {
            var notification = new Notification();
            notification.id = UUID.randomUUID();
            notification.recipientId = recipient;
            notification.type = type;
            notification.sourceEventId = eventId;
            notification.title = title;
            notification.summary = summary;
            notification.link = link;
            notification.createdAt = clock.instant();
            if (notificationRepository.insertNotification(notification)) {
                emailDeliveryService.scheduleEmail(notification, emailType, expiresAt);
                changed.add(recipient);
            }
        }
        if (!changed.isEmpty())
            runAfterCommit(
                    () ->
                            changed.forEach(
                                    id -> {
                                        try {
                                            sseHub.publish(
                                                    buildChannel(id),
                                                    Map.of("type", "notification-changed"));
                                        } catch (RuntimeException ignored) {
                                            /* REST fallback; committed state stays authoritative. */
                                        }
                                    }));
    }

    @Transactional(readOnly = true, timeout = 5)
    public Feed listNotifications(Actor actor, boolean unread, int page, int size) {
        UUID owner = notificationAccess.requireSelf(actor);
        if (page < 1 || page > 100000 || size < 1 || size > 50)
            throw ApiException.createBadRequest("PAGE_INVALID", "Trang phải từ1, tối đa50 mục");
        var rows = notificationRepository.listNotifications(owner, unread, page, size);
        boolean more =
                rows.size() == size
                        && !notificationRepository
                                .listNotifications(owner, unread, page + 1, size)
                                .isEmpty();
        return new Feed(
                rows.stream()
                        .map(
                                n ->
                                        new Item(
                                                n.id,
                                                n.type.name(),
                                                n.title,
                                                NotificationSummaries.resolveDisplaySummary(
                                                        n.type, n.summary),
                                                n.link,
                                                n.readAt,
                                                n.createdAt))
                        .toList(),
                page,
                size,
                more);
    }

    @Transactional(readOnly = true, timeout = 5)
    public long countUnread(Actor actor) {
        return notificationRepository.countUnread(notificationAccess.requireSelf(actor));
    }

    public void markRead(Actor actor, UUID id) {
        UUID owner = notificationAccess.requireSelf(actor);
        if (!notificationRepository.markRead(id, owner, clock.instant()))
            throw notificationAccess.rejectAccess(actor, id);
        notifyChanged(owner);
    }

    public void markAllRead(Actor actor) {
        UUID owner = notificationAccess.requireSelf(actor);
        notificationRepository.markAllRead(owner, clock.instant());
        notifyChanged(owner);
    }

    public String resolveAuthorizedChannel(Actor actor) {
        return buildChannel(notificationAccess.requireSelf(actor));
    }

    public static String buildChannel(UUID id) {
        return "notifications:account:" + id;
    }

    private void notifyChanged(UUID owner) {
        runAfterCommit(
                () -> {
                    try {
                        sseHub.publish(buildChannel(owner), Map.of("type", "notification-changed"));
                    } catch (RuntimeException ignored) {
                        /* Refresh fallback. */
                    }
                });
    }

    private void runAfterCommit(Runnable work) {
        if (TransactionSynchronizationManager.isSynchronizationActive())
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        public void afterCommit() {
                            work.run();
                        }
                    });
        else work.run();
    }
}
