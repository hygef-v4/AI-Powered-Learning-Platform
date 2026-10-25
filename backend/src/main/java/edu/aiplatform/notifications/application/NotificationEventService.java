package edu.aiplatform.notifications.application;
import edu.aiplatform.jobs.contract.*;
import edu.aiplatform.notifications.domain.NotificationType;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class NotificationEventService {
    public static final Map<String,String> SOURCES=Map.ofEntries(
        Map.entry(EventTypes.ENROLLMENT_ACTIVATED,"U05"),Map.entry(EventTypes.ASSIGNMENT_OPENED,"U07"),Map.entry(EventTypes.GRADE_PUBLISHED,"U09"),Map.entry(EventTypes.QUIZ_PUBLISHED,"U10"),
        Map.entry(EventTypes.GROUP_MEMBERSHIP_CHANGED,"U12"),Map.entry(EventTypes.GROUP_LEADER_CHANGE_REQUESTED,"U12"),Map.entry(EventTypes.GROUP_LEADER_CHANGED,"U12"),Map.entry(EventTypes.GROUP_LEADER_REQUEST_REJECTED,"U12"),Map.entry(EventTypes.GROUP_SUBMITTED,"U12"),Map.entry(EventTypes.BACKUP_FAILED,"U03"),Map.entry(EventTypes.CLASS_ANNOUNCEMENT_POSTED,"U14"));
    private final RecipientResolver resolver;private final NotificationService notifications;
    public NotificationEventService(RecipientResolver resolver,NotificationService notifications) {this.resolver=resolver;this.notifications=notifications;}
    @Transactional(timeout=15)
    public void accept(DomainEvent event) {
        if(event==null||event.schemaVersion()!=1||event.eventType()==null||!SOURCES.containsKey(event.eventType()))return;
        if(event.eventId()==null||event.payload()==null||event.payload().size()>30||!SOURCES.get(event.eventType()).equals(event.sourceUnit()))throw new IllegalArgumentException("Invalid notification event");
        var r=resolver.resolve(event);notifications.create(event.eventId(),r.type(),r.recipients(),title(r.type()),NotificationSummaries.forType(r.type()),r.link(),r.emailType(),null);
    }
    public static String title(NotificationType type) {return switch(type){
        case ENROLLED->"Bạn đã được ghi danh vào lớp";case ASSIGNMENT_OPENED->"Có bài tập mới mở";case QUIZ_PUBLISHED->"Có Quiz mới";case GRADE_PUBLISHED->"Bạn có điểm mới";
        case DEADLINE_REMINDER->"Bài tập sắp đến hạn";case ANNOUNCEMENT->"Có thông báo mới của lớp";case BACKUP_FAILED->"Sao lưu hệ thống thất bại";
        case LEADER_CHANGE_REQUESTED->"Có yêu cầu đổi trưởng nhóm";case LEADER_CHANGED->"Trưởng nhóm đã thay đổi";case LEADER_REQUEST_REJECTED->"Yêu cầu đổi trưởng nhóm bị từ chối";
        case GROUP_MEMBERSHIP_CHANGED->"Thành viên nhóm đã thay đổi";case GROUP_SUBMITTED->"Nhóm đã nộp bài";};}
}
