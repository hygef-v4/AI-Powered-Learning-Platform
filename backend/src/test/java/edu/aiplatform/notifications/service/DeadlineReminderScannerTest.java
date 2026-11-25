package edu.aiplatform.notifications.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import edu.aiplatform.academics.contract.*;
import edu.aiplatform.assignments.contract.*;
import edu.aiplatform.assignmentwork.contract.ISubmissionQuery;
import edu.aiplatform.notifications.model.*;
import edu.aiplatform.notifications.repository.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.transaction.*;
import org.springframework.transaction.support.*;

class DeadlineReminderScannerTest {
    IAssignmentQuery assignmentQuery;
    IClassAccess classAccess;
    ISubmissionQuery submissionQuery;
    INotificationRepository notificationRepository;
    NotificationService notificationService;
    DeadlineReminderScanner deadlineReminderScanner;
    Instant now = Instant.parse("2026-10-21T08:00:00Z");
    UUID id = UUID.randomUUID(),
            classId = UUID.randomUUID(),
            subject = UUID.randomUUID(),
            student = UUID.randomUUID();

    @BeforeEach
    void setup() {
        assignmentQuery = mock(IAssignmentQuery.class);
        classAccess = mock(IClassAccess.class);
        submissionQuery = mock(ISubmissionQuery.class);
        notificationRepository = mock(INotificationRepository.class);
        notificationService = mock(NotificationService.class);
        var manager =
                new AbstractPlatformTransactionManager() {
                    protected Object doGetTransaction() {
                        return new Object();
                    }

                    protected void doBegin(Object t, TransactionDefinition d) {}

                    protected void doCommit(DefaultTransactionStatus s) {}

                    protected void doRollback(DefaultTransactionStatus s) {}
                };
        deadlineReminderScanner =
                new DeadlineReminderScanner(
                        assignmentQuery,
                        classAccess,
                        submissionQuery,
                        notificationRepository,
                        notificationService,
                        manager);
        when(classAccess.getClassRef(classId)).thenReturn(Optional.of(course(classId)));
        when(classAccess.listActiveStudents(classId)).thenReturn(List.of(student));
        when(submissionQuery.listStudentsWithoutSubmission(id, classId))
                .thenReturn(List.of(student));
        when(notificationRepository.insertReminder(any(), any(), any(), anyString(), any()))
                .thenReturn(true);
    }

    ClassRef course(UUID id) {
        return new ClassRef(id, subject, "C", "Class", ClassStatus.OPEN, UUID.randomUUID(), false);
    }

    AssignmentView assignment(Instant opens, Instant closes, UUID assignedClass) {
        return new AssignmentView(
                id,
                assignedClass == null ? AssignmentScope.SUBJECT : AssignmentScope.CLASS,
                assignedClass,
                subject,
                1,
                "Bài",
                SubmissionMode.INDIVIDUAL,
                GradingMode.GRADED,
                false,
                AssignmentStatus.OPEN,
                opens,
                closes,
                null,
                null,
                false,
                null,
                List.of());
    }

    void source(AssignmentView a) {
        when(assignmentQuery.listClosingBetween(any(), any())).thenReturn(List.of(a));
        when(assignmentQuery.loadAssignment(id)).thenReturn(Optional.of(a));
    }

    @Test
    void windowTraversalDoesNotSilentlyDropTheFiveHundredAndFirstAssignment() {
        var candidates = java.util.stream.IntStream.range(0, 501).mapToObj(index -> {
            var original = assignment(now.minusSeconds(60), now.plus(Duration.ofHours(24)), classId);
            return new AssignmentView(UUID.randomUUID(), original.scope(), classId, subject, 1,
                    "Assignment " + index, SubmissionMode.INDIVIDUAL, GradingMode.GRADED, false,
                    AssignmentStatus.OPEN, original.opensAt(), original.closesAt(), null, null, false, null, List.of());
        }).toList();
        var byId = candidates.stream().collect(java.util.stream.Collectors.toMap(AssignmentView::assignmentId,
                java.util.function.Function.identity()));
        when(assignmentQuery.listClosingBetween(any(), any())).thenReturn(candidates);
        when(assignmentQuery.loadAssignment(any()))
                .thenAnswer(call -> Optional.ofNullable(byId.get(call.getArgument(0))));
        deadlineReminderScanner.scan(now);
        verify(notificationRepository)
                .insertReminder(
                        candidates.get(500).assignmentId(),
                        classId,
                        candidates.get(500).closesAt(),
                        "SENT",
                        now);
        verify(notificationRepository, times(501))
                .insertReminder(any(), eq(classId), any(), eq("SENT"), eq(now));
    }

    @Test
    void subjectExpansionDoesNotSilentlyDropTheFiveHundredAndFirstOpenClass() {
        var source = assignment(now.minusSeconds(60), now.plus(Duration.ofHours(24)), null);
        source(source);
        var targets = java.util.stream.IntStream.range(0, 501).mapToObj(index -> course(UUID.randomUUID())).toList();
        var byId = targets.stream().collect(java.util.stream.Collectors.toMap(ClassRef::classId,
                java.util.function.Function.identity()));
        when(classAccess.listOpenClassesOfSubject(subject)).thenReturn(targets);
        when(classAccess.getClassRef(any()))
                .thenAnswer(call -> Optional.ofNullable(byId.get(call.getArgument(0))));
        deadlineReminderScanner.scan(now);
        verify(notificationRepository)
                .insertReminder(id, targets.get(500).classId(), source.closesAt(), "SENT", now);
        verify(notificationRepository, times(501))
                .insertReminder(eq(id), any(), any(), eq("SENT"), eq(now));
    }

    @Test
    void reminderUsesStudentRouteWithClassContext() {
        source(assignment(now.minusSeconds(1000), now.plus(Duration.ofHours(24)), classId));
        deadlineReminderScanner.scan(now);
        verify(notificationService)
                .createNotification(
                        any(),
                        eq(NotificationType.DEADLINE_REMINDER),
                        eq(List.of(student)),
                        anyString(),
                        anyString(),
                        eq("/my-assignments/" + id + "?classId=" + classId),
                        eq("DEADLINE_REMINDER"),
                        any());
    }

    @Test
    void missedFirstWindowIsRemindedOnTheNextScan() {
        var missed = assignment(now.minus(Duration.ofDays(3)), now.plus(Duration.ofHours(14)), classId);
        source(missed);
        deadlineReminderScanner.scan(now);
        verify(assignmentQuery).listClosingBetween(now, now.plus(Duration.ofHours(24)));
        verify(notificationRepository).insertReminder(id, classId, missed.closesAt(), "SENT", now);
        verify(notificationService)
                .createNotification(
                        any(),
                        eq(NotificationType.DEADLINE_REMINDER),
                        eq(List.of(student)),
                        anyString(),
                        anyString(),
                        anyString(),
                        eq("DEADLINE_REMINDER"),
                        any());
    }

    @Test
    void assignmentMoreThanADayFromItsDeadlineIsNotReminded() {
        source(
                assignment(
                        now.minusSeconds(1000),
                        now.plus(Duration.ofHours(24)).plusSeconds(1),
                        classId));
        deadlineReminderScanner.scan(now);
        verifyNoInteractions(notificationService);
        verify(notificationRepository, never())
                .insertReminder(any(), any(), any(), anyString(), any());
    }

    @Test
    void reminderWindowStaysOpenUntilTheDeadlineSoMissedScansCatchUp() {
        var assignment = assignment(now.minusSeconds(60), now.plus(Duration.ofHours(24)), classId);
        assertThat(DeadlineReminderScanner.isEligible(assignment, now.minusNanos(1))).isFalse();
        assertThat(DeadlineReminderScanner.isEligible(assignment, now)).isTrue();
        assertThat(DeadlineReminderScanner.isEligible(assignment, now.plusSeconds(300))).isTrue();
        // Quét bù: lỡ cửa sổ đầu vẫn nhắc cho tới sát hạn; tới hạn thì thôi.
        assertThat(DeadlineReminderScanner.isEligible(assignment, now.plusSeconds(301))).isTrue();
        assertThat(DeadlineReminderScanner.isEligible(assignment, assignment.closesAt().minusNanos(1))).isTrue();
        assertThat(DeadlineReminderScanner.isEligible(assignment, assignment.closesAt())).isFalse();
    }
}
