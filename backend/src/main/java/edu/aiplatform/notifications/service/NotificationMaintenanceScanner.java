package edu.aiplatform.notifications.service;

import edu.aiplatform.jobs.contract.IScheduledScanner;
import edu.aiplatform.notifications.repository.INotificationRepository;
import java.time.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class NotificationMaintenanceScanner implements IScheduledScanner {
    private final EmailDeliveryService emailDeliveryService;
    private final INotificationRepository notificationRepository;

    public NotificationMaintenanceScanner(
            EmailDeliveryService emailDeliveryService,
            INotificationRepository notificationRepository) {
        this.emailDeliveryService = emailDeliveryService;
        this.notificationRepository = notificationRepository;
    }

    public String name() {
        return "u14-notification-maintenance";
    }

    @Transactional(timeout = 15)
    public void scan(Instant now) {
        emailDeliveryService.releaseDeferredEmails(now);
        notificationRepository.deleteOldNotifications(now.minus(Duration.ofDays(180)));
    }
}
