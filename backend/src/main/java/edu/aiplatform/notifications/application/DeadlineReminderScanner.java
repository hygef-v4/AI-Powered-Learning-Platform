package edu.aiplatform.notifications.application;
import edu.aiplatform.academics.contract.*;
import edu.aiplatform.assignments.contract.*;
import edu.aiplatform.assignmentwork.contract.ISubmissionQuery;
import edu.aiplatform.jobs.contract.IScheduledScanner;
import edu.aiplatform.notifications.domain.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class DeadlineReminderScanner implements IScheduledScanner {
    private final IAssignmentQuery assignments;private final IClassAccess classes;private final ISubmissionQuery submissions;
    private final INotificationRepository repository;private final NotificationService notifications;private final TransactionTemplate transaction;
    public DeadlineReminderScanner(IAssignmentQuery assignments,IClassAccess classes,ISubmissionQuery submissions,INotificationRepository repository,NotificationService notifications,PlatformTransactionManager manager) {
        this.assignments=assignments;this.classes=classes;this.submissions=submissions;this.repository=repository;this.notifications=notifications;this.transaction=new TransactionTemplate(manager);this.transaction.setTimeout(15);
    }
    public String name() {return "u14-deadline-reminders";}
    public void scan(Instant now) {
        for(var candidate:assignments.listClosingBetween(now.plus(Duration.ofHours(24)).minus(Duration.ofMinutes(5)),now.plus(Duration.ofHours(24))).stream().limit(500).toList()) {
            var current=assignments.get(candidate.assignmentId()).orElse(null);if(current==null||!eligible(current,now))continue;
            List<ClassRef> targets=current.classId()==null?classes.listOpenClassesOfSubject(current.subjectId()):classes.getClassRef(current.classId()).map(List::of).orElse(List.of());
            for(var c:targets.stream().filter(c->c.status()==ClassStatus.OPEN).limit(500).toList())transaction.executeWithoutResult(tx->remind(current,c,now));
        }
    }
    public static boolean eligible(AssignmentView a,Instant now) {
        if(a.status()!=AssignmentStatus.OPEN||a.opensAt()==null||a.closesAt()==null||!a.closesAt().isAfter(now))return false;
        Instant due=a.closesAt().minus(Duration.ofHours(24));return !now.isBefore(due)&&!now.isAfter(due.plus(Duration.ofMinutes(5)));
    }
    private void remind(AssignmentView candidate,ClassRef c,Instant now) {
        var a=assignments.get(candidate.assignmentId()).orElse(null);var currentClass=classes.getClassRef(c.classId()).orElse(null);
        if(a==null||!Objects.equals(a.closesAt(),candidate.closesAt())||!eligible(a,now)||currentClass==null||currentClass.status()!=ClassStatus.OPEN)return;
        boolean late=a.opensAt().isAfter(a.closesAt().minus(Duration.ofHours(24)));
        if(!repository.reminder(a.assignmentId(),c.classId(),a.closesAt(),late?"SKIPPED":"SENT",now)||late)return;
        Set<UUID> active=new HashSet<>(classes.listActiveStudents(c.classId()));
        var recipients=submissions.listStudentsWithoutSubmission(a.assignmentId(),c.classId()).stream().filter(active::contains).distinct().toList();
        UUID event=UUID.nameUUIDFromBytes((a.assignmentId()+":"+c.classId()+":"+a.closesAt()).getBytes(StandardCharsets.UTF_8));
        notifications.create(event,NotificationType.DEADLINE_REMINDER,recipients,NotificationEventService.title(NotificationType.DEADLINE_REMINDER),"Hạn nộp bài còn khoảng24giờ.","/assignments/"+a.assignmentId()+"?classId="+c.classId(),"DEADLINE_REMINDER",a.closesAt());
    }
}
