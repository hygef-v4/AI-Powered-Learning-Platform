package edu.aiplatform.notifications.service;

import edu.aiplatform.notifications.repository.*;
import java.time.*;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class EmailCapPolicy {
    public static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    public static final Map<String, Integer> PRIORITY =
            Map.of(
                    "BACKUP_FAILED",
                    6,
                    "DEADLINE_REMINDER",
                    5,
                    "ANNOUNCEMENT_IMPORTANT",
                    4,
                    "ASSIGNMENT_OPENED",
                    3,
                    "GRADE_PUBLISHED",
                    2,
                    "ENROLLED",
                    1,
                    "LEADER_CHANGE_REQUESTED",
                    1,
                    "QUESTION_PROPOSED",
                    1,
                    "QUESTION_REVIEWED",
                    1);
    private final INotificationRepository notificationRepository;
    private final int cap;

    public EmailCapPolicy(
            INotificationRepository notificationRepository,
            @Value("${U14_EMAIL_DAILY_CAP:300}") int cap) {
        if (cap < 0) throw new IllegalArgumentException("Email cap must be nonnegative");
        this.notificationRepository = notificationRepository;
        this.cap = cap;
    }

    public boolean hasCapacity(Instant now) {
        notificationRepository.lockEmailBudget();
        Instant from = now.atZone(ZONE).toLocalDate().atStartOfDay(ZONE).toInstant();
        return notificationRepository.countReservedEmails(from, calculateNextDay(now)) < cap;
    }

    public Instant calculateNextDay(Instant now) {
        return now.atZone(ZONE).toLocalDate().plusDays(1).atStartOfDay(ZONE).toInstant();
    }

    public boolean isSameDay(Instant first, Instant second) {
        return first.atZone(ZONE).toLocalDate().equals(second.atZone(ZONE).toLocalDate());
    }
}
