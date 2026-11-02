package edu.aiplatform.notifications.repository;

import edu.aiplatform.notifications.model.EmailDelivery;
import edu.aiplatform.notifications.model.Notification;
import java.time.Instant;
import java.util.*;
public interface INotificationRepository {
    boolean insert(Notification n);
    Optional<Notification> find(UUID id);
    List<Notification> list(UUID recipient,boolean unread,int page,int size);
    long unread(UUID recipient);
    boolean read(UUID id,UUID recipient,Instant now);
    void readAll(UUID recipient,Instant now);
    Map<String,Boolean> preferences(UUID account);
    void preference(UUID account,String type,boolean enabled);
    void addEmail(EmailDelivery delivery);
    Optional<EmailDelivery> lockEmail(UUID id);
    List<EmailDelivery> pending(Instant olderThan);
    List<EmailDelivery> deferred(Instant now);
    long reserved(Instant from,Instant to);
    void lockBudget();
    boolean reminder(UUID assignment,UUID classId,Instant closesAt,String status,Instant now);
    int deleteOld(Instant cutoff);
}
