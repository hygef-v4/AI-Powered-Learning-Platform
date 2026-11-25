package edu.aiplatform.notifications.service;

import edu.aiplatform.identity.contract.*;
import edu.aiplatform.jobs.contract.*;
import edu.aiplatform.notifications.model.*;
import edu.aiplatform.notifications.repository.*;
import edu.aiplatform.assignments.contract.*;
import java.net.URI;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(timeout = 20)
public class EmailDeliveryService {
    private final INotificationRepository notificationRepository;
    private final EmailCapPolicy emailCapPolicy;
    private final IJob job;
    private final IAccountLookup accountLookup;
    private final IMail mail;
    private final Clock clock;
    private final String baseUrl;
    private final IAssignmentQuery assignmentQuery;

    public EmailDeliveryService(
            INotificationRepository notificationRepository,
            EmailCapPolicy emailCapPolicy,
            IJob job,
            IAccountLookup accountLookup,
            IMail mail,
            Clock clock,
            IAssignmentQuery assignmentQuery,
            @Value("${APP_PUBLIC_URL:http://localhost:3000}") String baseUrl) {
        var uri = URI.create(baseUrl);
        if (!Set.of("http", "https").contains(uri.getScheme())
                || uri.getHost() == null
                || uri.getUserInfo() != null
                || uri.getQuery() != null
                || uri.getFragment() != null)
            throw new IllegalArgumentException("Invalid U14 public base URL");
        this.notificationRepository = notificationRepository;
        this.emailCapPolicy = emailCapPolicy;
        this.job = job;
        this.accountLookup = accountLookup;
        this.mail = mail;
        this.clock = clock;
        this.assignmentQuery = assignmentQuery;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
    }

    public void scheduleEmail(Notification notification, String type, Instant expiresAt) {
        if (type == null) return;
        Instant now = clock.instant();
        var delivery = new EmailDelivery();
        delivery.id = UUID.randomUUID();
        delivery.notificationId = notification.id;
        delivery.recipientId = notification.recipientId;
        delivery.type = type;
        delivery.priority = EmailCapPolicy.PRIORITY.get(type);
        delivery.createdAt = now;
        delivery.scheduledFor = now;
        delivery.expiresAt = expiresAt;
        if (!type.equals("BACKUP_FAILED")
                && !notificationRepository
                        .findPreferences(notification.recipientId)
                        .getOrDefault(type, NotificationPreferenceService.isEnabledByDefault(type)))
            delivery.status = EmailDelivery.Status.SKIPPED;
        else if (isExpired(delivery, now)) delivery.status = EmailDelivery.Status.SKIPPED;
        else if (emailCapPolicy.hasCapacity(now)) delivery.status = EmailDelivery.Status.PENDING;
        else {
            delivery.status = EmailDelivery.Status.DEFERRED;
            delivery.scheduledFor = emailCapPolicy.calculateNextDay(now);
        }
        notificationRepository.insertEmailDelivery(delivery);
        if (delivery.status == EmailDelivery.Status.PENDING) enqueueEmail(delivery);
    }

    @Transactional(timeout = 30, noRollbackFor = RetryableJobException.class)
    public void sendEmail(UUID id) {
        notificationRepository.lockEmailBudget();
        var delivery = notificationRepository.lockEmailDelivery(id).orElse(null);
        if (delivery == null || delivery.status != EmailDelivery.Status.PENDING) return;
        try {
            deliverPendingEmail(delivery);
        } catch (RetryableJobException retry) {
            // Transaction vẫn commit (noRollbackFor): giữ lịch và số lần thử đã đổi trước lỗi.
            notificationRepository.updateEmailDelivery(delivery);
            throw retry;
        }
        notificationRepository.updateEmailDelivery(delivery);
    }

    private void deliverPendingEmail(EmailDelivery delivery) {
        Instant now = clock.instant();
        if (isExpired(delivery, now)) {
            delivery.status = EmailDelivery.Status.SKIPPED;
            return;
        }
        if (!emailCapPolicy.isSameDay(delivery.scheduledFor, now)) {
            if (!emailCapPolicy.hasCapacity(now)) {
                delivery.status = EmailDelivery.Status.DEFERRED;
                delivery.scheduledFor = emailCapPolicy.calculateNextDay(now);
                return;
            }
            delivery.scheduledFor = now;
        }
        AccountContact contact;
        try {
            contact = accountLookup.getContact(delivery.recipientId).orElse(null);
        } catch (RuntimeException unavailable) {
            throw new RetryableJobException("NOTIFICATION_ACCOUNT_UNAVAILABLE");
        }
        if (contact == null
                || contact.status() != AccountStatus.ACTIVE
                || !NotificationPreferenceService
                        .listTypesForRole(contact.role())
                        .contains(delivery.type)) {
            delivery.status = EmailDelivery.Status.SKIPPED;
            return;
        }
        var notification =
                notificationRepository.findNotification(delivery.notificationId).orElse(null);
        if (notification == null) {
            delivery.status = EmailDelivery.Status.SKIPPED;
            return;
        }
        if (delivery.type.equals("DEADLINE_REMINDER")) {
            UUID assignmentId;
            try {
                assignmentId = AssignmentNotificationLink.parseAssignmentId(notification.link);
            } catch (IllegalArgumentException invalidLink) {
                delivery.status = EmailDelivery.Status.SKIPPED;
                delivery.lastError = "INVALID_ASSIGNMENT_LINK";
                return;
            }
            AssignmentView assignment;
            try {
                assignment = assignmentQuery.loadAssignment(assignmentId).orElse(null);
            } catch (RuntimeException unavailable) {
                throw new RetryableJobException("NOTIFICATION_ASSIGNMENT_UNAVAILABLE");
            }
            if (assignment == null
                    || assignment.status() != AssignmentStatus.OPEN
                    || !Objects.equals(assignment.closesAt(), delivery.expiresAt)) {
                delivery.status = EmailDelivery.Status.SKIPPED;
                return;
            }
        }
        delivery.attempts++;
        // Provider acceptance and DB commit cannot be exactly-once without provider idempotency
        // support.
        try {
            mail.send(
                    new MailMessage(
                            contact.email(),
                            NotificationEmailText.buildSubject(notification.type, notification.title),
                            NotificationEmailText.buildBody(baseUrl + notification.link),
                            null));
        } catch (RuntimeException failure) {
            throw new RetryableJobException("NOTIFICATION_MAIL_UNAVAILABLE");
        }
        delivery.status = EmailDelivery.Status.SENT;
        delivery.sentAt = now;
        delivery.lastError = null;
    }

    public void markEmailFailed(UUID id) {
        notificationRepository
                .lockEmailDelivery(id)
                .filter(e -> e.status == EmailDelivery.Status.PENDING)
                .ifPresent(
                        e -> {
                            e.status = EmailDelivery.Status.FAILED;
                            e.attempts = Math.max(e.attempts, 5);
                            e.lastError = "MAIL_RETRIES_EXHAUSTED";
                            notificationRepository.updateEmailDelivery(e);
                        });
    }

    public void releaseDeferredEmails(Instant now) {
        // All paths take the budget lock before row locks to avoid deadlocks across midnight.
        notificationRepository.lockEmailBudget();
        for (var candidate :
                notificationRepository.listDeferredEmails(now).stream()
                        .sorted(Comparator.comparingInt((EmailDelivery e) -> e.priority).reversed())
                        .toList()) {
            var delivery = notificationRepository.lockEmailDelivery(candidate.id).orElse(null);
            if (delivery == null || delivery.status != EmailDelivery.Status.DEFERRED) continue;
            if (isExpired(delivery, now)) {
                delivery.status = EmailDelivery.Status.SKIPPED;
                notificationRepository.updateEmailDelivery(delivery);
                continue;
            }
            if (!emailCapPolicy.hasCapacity(now)) break;
            delivery.status = EmailDelivery.Status.PENDING;
            delivery.scheduledFor = now;
            // Ghi trước lần đếm hạn mức kế tiếp để email này được tính vào ngân sách trong ngày.
            notificationRepository.updateEmailDelivery(delivery);
            enqueueEmail(delivery);
        }
    }

    private boolean isExpired(EmailDelivery delivery, Instant now) {
        return delivery.expiresAt != null && !delivery.expiresAt.isAfter(now);
    }

    private void enqueueEmail(EmailDelivery delivery) {
        job.enqueue(
                JobTypes.EMAIL_SEND,
                Map.of("deliveryId", delivery.id.toString()),
                "email:" + delivery.id);
    }
}
