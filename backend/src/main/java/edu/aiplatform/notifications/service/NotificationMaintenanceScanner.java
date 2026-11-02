package edu.aiplatform.notifications.service;
import edu.aiplatform.jobs.contract.IScheduledScanner;
import edu.aiplatform.notifications.repository.INotificationRepository;
import java.time.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
@Component
public class NotificationMaintenanceScanner implements IScheduledScanner {
    private final EmailDeliveryService emails;private final INotificationRepository repository;
    public NotificationMaintenanceScanner(EmailDeliveryService emails,INotificationRepository repository) {this.emails=emails;this.repository=repository;}
    public String name() {return "u14-notification-maintenance";}
    @Transactional(timeout=15)
    public void scan(Instant now) {emails.releaseDeferred(now);repository.deleteOld(now.minus(Duration.ofDays(180)));}
}
