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
        when(assignmentQuery.get(id)).thenReturn(Optional.of(a));
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
        when(assignmentQuery.get(any()))
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
    void failingClassDoesNotStopOtherClassesOrFollowingAssignments() {
        var a = assignment(now.minusSeconds(1000), now.plus(Duration.ofHours(24)), null);
        var secondClass = UUID.randomUUID();
        var next =
                new AssignmentView(
                        UUID.randomUUID(),
                        AssignmentScope.CLASS,
                        classId,
                        subject,
                        1,
                        "Next",
                        SubmissionMode.INDIVIDUAL,
                        GradingMode.GRADED,
                        false,
                        AssignmentStatus.OPEN,
                        a.opensAt(),
                        a.closesAt(),
                        null,
                        null,
                        false,
                        null,
                        List.of());
        source(a);
        when(assignmentQuery.listClosingBetween(any(), any())).thenReturn(List.of(a, next));
        when(assignmentQuery.get(next.assignmentId())).thenReturn(Optional.of(next));
        when(classAccess.listOpenClassesOfSubject(subject))
                .thenReturn(List.of(course(classId), course(secondClass)));
        when(classAccess.getClassRef(secondClass)).thenReturn(Optional.of(course(secondClass)));
        when(classAccess.listActiveStudents(secondClass)).thenReturn(List.of(student));
        when(submissionQuery.listStudentsWithoutSubmission(id, classId))
                .thenThrow(new IllegalStateException("unavailable"));
        deadlineReminderScanner.scan(now);
        verify(submissionQuery).listStudentsWithoutSubmission(id, secondClass);
        verify(submissionQuery).listStudentsWithoutSubmission(next.assignmentId(), classId);
        verify(notificationService, times(2))
                .createNotification(
                        any(),
                        any(),
                        anyList(),
                        anyString(),
                        anyString(),
                        anyString(),
                        anyString(),
                        any());
    }

    @Test
    void failingAssignmentLookupOrClassExpansionDoesNotStopNextAssignment() {
        var a = assignment(now.minusSeconds(1000), now.plus(Duration.ofHours(24)), classId);
        var broken =
                new AssignmentView(
                        UUID.randomUUID(),
                        AssignmentScope.SUBJECT,
                        null,
                        subject,
                        1,
                        "Broken",
                        SubmissionMode.INDIVIDUAL,
                        GradingMode.GRADED,
                        false,
                        AssignmentStatus.OPEN,
                        a.opensAt(),
                        a.closesAt(),
                        null,
                        null,
                        false,
                        null,
                        List.of());
        source(a);
        when(assignmentQuery.listClosingBetween(any(), any())).thenReturn(List.of(broken, a));
        when(assignmentQuery.get(broken.assignmentId()))
                .thenThrow(new IllegalStateException("lookup unavailable"));
        deadlineReminderScanner.scan(now);
        verify(notificationService)
                .createNotification(
                        any(),
                        any(),
                        anyList(),
                        anyString(),
                        anyString(),
                        anyString(),
                        anyString(),
                        any());
        clearInvocations(notificationService);
        doReturn(Optional.of(broken)).when(assignmentQuery).get(broken.assignmentId());
        when(classAccess.listOpenClassesOfSubject(subject))
                .thenThrow(new IllegalStateException("classes unavailable"));
        deadlineReminderScanner.scan(now);
        verify(notificationService)
                .createNotification(
                        any(),
                        any(),
                        anyList(),
                        anyString(),
                        anyString(),
                        anyString(),
                        anyString(),
                        any());
    }

    @Test
    void reminderOncePerDeadlineAndOnlyNonSubmitters() {
        source(assignment(now.minusSeconds(5), now.plus(Duration.ofHours(24)), classId));
        when(notificationRepository.insertReminder(eq(id), eq(classId), any(), eq("SENT"), eq(now)))
                .thenReturn(true, false);
        deadlineReminderScanner.scan(now);
        deadlineReminderScanner.scan(now);
        verify(notificationService, times(1))
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
    void lateOpenedAssignmentMarkedSkipped() {
        source(assignment(now.plusSeconds(1), now.plus(Duration.ofHours(24)), classId));
        deadlineReminderScanner.scan(now);
        verify(notificationRepository)
                .insertReminder(id, classId, now.plus(Duration.ofHours(24)), "SKIPPED", now);
        verifyNoInteractions(notificationService);
    }

    @Test
    void outsideFiveMinuteWindowNotSent() {
        source(
                assignment(
                        now.minusSeconds(1000),
                        now.plus(Duration.ofHours(24)).minusSeconds(301),
                        classId));
        deadlineReminderScanner.scan(now);
        verifyNoInteractions(notificationService);
        verify(notificationRepository, never())
                .insertReminder(any(), any(), any(), anyString(), any());
    }

    @Test
    void subjectAssignmentExpandsOpenClasses() {
        var a = assignment(now.minusSeconds(1000), now.plus(Duration.ofHours(24)), null);
        source(a);
        when(classAccess.listOpenClassesOfSubject(subject)).thenReturn(List.of(course(classId)));
        deadlineReminderScanner.scan(now);
        verify(submissionQuery).listStudentsWithoutSubmission(id, classId);
    }

    @Test
    void removedEnrollmentIsNotNotified() {
        source(assignment(now.minusSeconds(1000), now.plus(Duration.ofHours(24)), classId));
        when(classAccess.listActiveStudents(classId)).thenReturn(List.of());
        deadlineReminderScanner.scan(now);
        verify(notificationService)
                .createNotification(
                        any(),
                        any(),
                        eq(List.of()),
                        anyString(),
                        anyString(),
                        anyString(),
                        anyString(),
                        any());
    }

    @Test
    void changedDeadlineGetsNewMarker() {
        var a = assignment(now.minusSeconds(1000), now.plus(Duration.ofHours(24)), classId);
        source(a);
        deadlineReminderScanner.scan(now);
        var changed = assignment(a.opensAt(), a.closesAt().plusSeconds(60), classId);
        source(changed);
        deadlineReminderScanner.scan(now.plusSeconds(60));
        verify(notificationRepository).insertReminder(id, classId, a.closesAt(), "SENT", now);
        verify(notificationRepository)
                .insertReminder(id, classId, changed.closesAt(), "SENT", now.plusSeconds(60));
    }

    @Test
    void twoDayRescheduleIgnoresOldDeadlineAndSendsOnlyOnceAtTheNewDeadline() {
        var old = assignment(now.minusSeconds(1000), now.plus(Duration.ofHours(24)), classId);
        var changed = assignment(old.opensAt(), old.closesAt().plus(Duration.ofDays(2)), classId);
        source(old);
        when(assignmentQuery.get(id)).thenReturn(Optional.of(changed));
        deadlineReminderScanner.scan(now);
        verifyNoInteractions(notificationService);
        verifyNoInteractions(notificationRepository);
        source(changed);
        var later = now.plus(Duration.ofDays(2));
        when(notificationRepository.insertReminder(id, classId, changed.closesAt(), "SENT", later))
                .thenReturn(true, false);
        deadlineReminderScanner.scan(later);
        deadlineReminderScanner.scan(later);
        verify(notificationService, times(1))
                .createNotification(
                        any(),
                        eq(NotificationType.DEADLINE_REMINDER),
                        eq(List.of(student)),
                        anyString(),
                        anyString(),
                        anyString(),
                        anyString(),
                        eq(changed.closesAt()));
    }

    @Test
    void assignmentRetiredAfterSelectionDoesNotSendAReminder() {
        var a = assignment(now.minusSeconds(1000), now.plus(Duration.ofHours(24)), classId);
        source(a);
        var stopped =
                new AssignmentView(
                        a.assignmentId(),
                        a.scope(),
                        a.classId(),
                        a.subjectId(),
                        a.versionNo(),
                        a.title(),
                        a.submissionMode(),
                        a.gradingMode(),
                        a.diagramEnabled(),
                        AssignmentStatus.RETIRED,
                        a.opensAt(),
                        a.closesAt(),
                        null,
                        null,
                        false,
                        null,
                        List.of());
        when(assignmentQuery.get(id)).thenReturn(Optional.of(a), Optional.of(stopped));
        deadlineReminderScanner.scan(now);
        verifyNoInteractions(notificationService);
        verifyNoInteractions(notificationRepository);
    }

    @Test
    void reminderTimingWindowIncludesEndpointsAndExcludesOneInstantOutside() {
        var assignment = assignment(now.minusSeconds(60), now.plus(Duration.ofHours(24)), classId);
        assertThat(DeadlineReminderScanner.isEligible(assignment, now.minusNanos(1))).isFalse();
        assertThat(DeadlineReminderScanner.isEligible(assignment, now)).isTrue();
        assertThat(DeadlineReminderScanner.isEligible(assignment, now.plusSeconds(300))).isTrue();
        assertThat(
                DeadlineReminderScanner.isEligible(
                        assignment, now.plusSeconds(300).plusNanos(1))).isFalse();
    }

    @Test
    void closedAssignmentCannotSendEvenWhenSelectedByEarlierQuery() {
        var open = assignment(now.minusSeconds(60), now.plus(Duration.ofHours(24)), classId);
        var closed = new AssignmentView(open.assignmentId(), open.scope(), open.classId(),
                open.subjectId(), open.versionNo(), open.title(), open.submissionMode(), open.gradingMode(),
                open.diagramEnabled(), AssignmentStatus.CLOSED, open.opensAt(), open.closesAt(),
                null, null, false, null, List.of());
        source(closed);
        deadlineReminderScanner.scan(now);
        verifyNoInteractions(notificationRepository, notificationService, submissionQuery);
    }

    @Test
    void dependencyFailureLogContainsSafeCodeButNoPrivateExceptionContent() {
        source(assignment(now.minusSeconds(60), now.plus(Duration.ofHours(24)), classId));
        when(submissionQuery.listStudentsWithoutSubmission(id, classId))
                .thenThrow(new IllegalStateException("PRIVATE_RECIPIENT_BODY_SENTINEL"));
        var logger = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(DeadlineReminderScanner.class);
        var appender = new ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent>();
        appender.start();
        logger.addAppender(appender);
        try {
            deadlineReminderScanner.scan(now);
            assertThat(appender.list).hasSize(1);
            assertThat(appender.list.get(0).getFormattedMessage())
                    .contains("REMINDER_CLASS_FAILED").doesNotContain("PRIVATE_RECIPIENT_BODY_SENTINEL");
            assertThat(appender.list.get(0).getThrowableProxy()).isNull();
            verifyNoInteractions(notificationService);
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    @Test
    void maintenanceRetains180DaysAndDelegatesDeferredPriority() {
        var email = mock(EmailDeliveryService.class);
        new NotificationMaintenanceScanner(email, notificationRepository).scan(now);
        verify(email).releaseDeferredEmails(now);
        verify(notificationRepository).deleteOldNotifications(now.minus(Duration.ofDays(180)));
    }
}
