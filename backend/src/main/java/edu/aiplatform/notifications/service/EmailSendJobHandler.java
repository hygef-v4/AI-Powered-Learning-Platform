package edu.aiplatform.notifications.service;

import edu.aiplatform.jobs.contract.*;
import edu.aiplatform.notifications.repository.*;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class EmailSendJobHandler implements IJobHandler, IPendingSweeper {
    private final EmailDeliveryService emailDeliveryService;
    private final INotificationRepository notificationRepository;

    public EmailSendJobHandler(
            EmailDeliveryService emailDeliveryService,
            INotificationRepository notificationRepository) {
        this.emailDeliveryService = emailDeliveryService;
        this.notificationRepository = notificationRepository;
    }

    public String jobType() {
        return JobTypes.EMAIL_SEND;
    }

    public JobQueue queue() {
        return JobQueue.EMAIL;
    }

    public Duration timeout() {
        return Duration.ofSeconds(35);
    }

    public void handle(JobMessage message) {
        UUID deliveryId = readDeliveryId(message);
        try {
            emailDeliveryService.sendEmail(deliveryId);
        } catch (RetryableJobException retry) {
            throw retry;
        } catch (RuntimeException unavailable) {
            throw new RetryableJobException("NOTIFICATION_DELIVERY_UNAVAILABLE");
        }
    }

    public void onFailed(JobMessage message, String ignored) {
        emailDeliveryService.markEmailFailed(readDeliveryId(message));
    }

    @Transactional(readOnly = true, timeout = 5)
    public List<PendingJob> findStalePending(Instant olderThan) {
        return notificationRepository.listPendingEmails(olderThan).stream()
                .map(e -> new PendingJob("email:" + e.id, Map.of("deliveryId", e.id.toString())))
                .toList();
    }

    private UUID readDeliveryId(JobMessage message) {
        return UUID.fromString(Objects.toString(message.payload().get("deliveryId")));
    }
}
