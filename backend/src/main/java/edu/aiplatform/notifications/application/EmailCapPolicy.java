package edu.aiplatform.notifications.application;
import edu.aiplatform.notifications.domain.*;
import java.time.*;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
@Component
public class EmailCapPolicy {
    public static final ZoneId ZONE=ZoneId.of("Asia/Ho_Chi_Minh");
    public static final Map<String,Integer> PRIORITY=Map.of("BACKUP_FAILED",6,"DEADLINE_REMINDER",5,"ANNOUNCEMENT_IMPORTANT",4,"ASSIGNMENT_OPENED",3,"GRADE_PUBLISHED",2,"ENROLLED",1);
    private final INotificationRepository repository;private final int cap;
    public EmailCapPolicy(INotificationRepository repository,@Value("${U14_EMAIL_DAILY_CAP:300}") int cap) {if(cap<0)throw new IllegalArgumentException("Email cap must be nonnegative");this.repository=repository;this.cap=cap;}
    public boolean available(Instant now) {repository.lockBudget();Instant from=now.atZone(ZONE).toLocalDate().atStartOfDay(ZONE).toInstant();return repository.reserved(from,nextDay(now))<cap;}
    public Instant nextDay(Instant now) {return now.atZone(ZONE).toLocalDate().plusDays(1).atStartOfDay(ZONE).toInstant();}
    public boolean sameDay(Instant a,Instant b) {return a.atZone(ZONE).toLocalDate().equals(b.atZone(ZONE).toLocalDate());}
}
