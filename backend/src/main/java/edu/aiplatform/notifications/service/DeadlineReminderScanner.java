package edu.aiplatform.notifications.service;

import edu.aiplatform.academics.contract.*;
import edu.aiplatform.assignments.contract.*;
import edu.aiplatform.assignmentwork.contract.ISubmissionQuery;
import edu.aiplatform.jobs.contract.IScheduledScanner;
import edu.aiplatform.notifications.model.*;
import edu.aiplatform.notifications.repository.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class DeadlineReminderScanner implements IScheduledScanner {
    private static final Logger log = LoggerFactory.getLogger(DeadlineReminderScanner.class);
    private final IAssignmentQuery assignmentQuery;
    private final IClassAccess classAccess;
    private final ISubmissionQuery submissionQuery;
    private final INotificationRepository notificationRepository;
    private final NotificationService notificationService;
    private final TransactionTemplate transactionTemplate;

    public DeadlineReminderScanner(
            IAssignmentQuery assignmentQuery,
            IClassAccess classAccess,
            ISubmissionQuery submissionQuery,
            INotificationRepository notificationRepository,
            NotificationService notificationService,
            PlatformTransactionManager transactionManager) {
        this.assignmentQuery = assignmentQuery;
        this.classAccess = classAccess;
        this.submissionQuery = submissionQuery;
        this.notificationRepository = notificationRepository;
        this.notificationService = notificationService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setTimeout(15);
    }

    public String name() {
        return "u14-deadline-reminders";
    }

    public void scan(Instant now) {
        for (var candidate :
                assignmentQuery
                        .listClosingBetween(
                                now.plus(Duration.ofHours(24)).minus(Duration.ofMinutes(5)),
                                now.plus(Duration.ofHours(24)))) {
            try {
                var current = assignmentQuery.get(candidate.assignmentId()).orElse(null);
                if (current == null || !isEligible(current, now)) continue;
                List<ClassRef> targets =
                        current.classId() == null
                                ? classAccess.listOpenClassesOfSubject(current.subjectId())
                                : classAccess.getClassRef(current.classId())
                                        .map(List::of)
                                        .orElse(List.of());
                for (var courseClass :
                        targets.stream()
                                .filter(c -> c.status() == ClassStatus.OPEN)
                                .toList()) {
                    try {
                        transactionTemplate.executeWithoutResult(
                                tx -> remindStudents(current, courseClass, now));
                    } catch (RuntimeException failure) {
                        // Catch after transaction rollback; dependency messages may contain private
                        // data.
                        log.warn(
                                "scanner={} assignmentId={} classId={} scanTime={} code=REMINDER_CLASS_FAILED errorType={}",
                                name(),
                                current.assignmentId(),
                                courseClass.classId(),
                                now,
                                failure.getClass().getSimpleName());
                    }
                }
            } catch (RuntimeException failure) {
                log.warn(
                        "scanner={} assignmentId={} scanTime={} code=REMINDER_ASSIGNMENT_FAILED errorType={}",
                        name(),
                        candidate.assignmentId(),
                        now,
                        failure.getClass().getSimpleName());
            }
        }
    }

    public static boolean isEligible(AssignmentView assignment, Instant now) {
        if (assignment.status() != AssignmentStatus.OPEN
                || assignment.opensAt() == null
                || assignment.closesAt() == null
                || !assignment.closesAt().isAfter(now)) return false;
        Instant due = assignment.closesAt().minus(Duration.ofHours(24));
        return !now.isBefore(due) && !now.isAfter(due.plus(Duration.ofMinutes(5)));
    }

    private void remindStudents(AssignmentView candidate, ClassRef courseClass, Instant now) {
        var assignment = assignmentQuery.get(candidate.assignmentId()).orElse(null);
        var currentClass = classAccess.getClassRef(courseClass.classId()).orElse(null);
        if (assignment == null
                || !Objects.equals(assignment.closesAt(), candidate.closesAt())
                || !isEligible(assignment, now)
                || currentClass == null
                || currentClass.status() != ClassStatus.OPEN) return;
        boolean late =
                assignment.opensAt().isAfter(assignment.closesAt().minus(Duration.ofHours(24)));
        if (!notificationRepository.insertReminder(
                        assignment.assignmentId(),
                        courseClass.classId(),
                        assignment.closesAt(),
                        late ? "SKIPPED" : "SENT",
                        now)
                || late) return;
        Set<UUID> active = new HashSet<>(classAccess.listActiveStudents(courseClass.classId()));
        var recipients =
                submissionQuery
                        .listStudentsWithoutSubmission(
                                assignment.assignmentId(), courseClass.classId())
                        .stream()
                        .filter(active::contains)
                        .distinct()
                        .toList();
        UUID event =
                UUID.nameUUIDFromBytes(
                        (assignment.assignmentId()
                                        + ":"
                                        + courseClass.classId()
                                        + ":"
                                        + assignment.closesAt())
                                .getBytes(StandardCharsets.UTF_8));
        notificationService.createNotification(
                event,
                NotificationType.DEADLINE_REMINDER,
                recipients,
                NotificationEventService.resolveTitle(NotificationType.DEADLINE_REMINDER),
                NotificationSummaries.summarizeType(NotificationType.DEADLINE_REMINDER),
                AssignmentNotificationLink.buildStudentLink(
                        assignment.assignmentId(), courseClass.classId()),
                "DEADLINE_REMINDER",
                assignment.closesAt());
    }
}
