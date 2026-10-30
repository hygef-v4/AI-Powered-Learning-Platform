package edu.aiplatform.assignments.service;

import edu.aiplatform.assignments.dto.AssignmentViews;

import static edu.aiplatform.assignments.service.AssignmentFixtures.NOW;
import static edu.aiplatform.assignments.service.AssignmentFixtures.heading;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.aiplatform.academics.contract.ClassStatus;
import edu.aiplatform.assignments.contract.AssignmentScope;
import edu.aiplatform.assignments.contract.AssignmentStatus;
import edu.aiplatform.assignments.contract.DocumentModel;
import edu.aiplatform.assignments.model.Assignment;
import edu.aiplatform.assignments.model.AssignmentRubric;
import edu.aiplatform.jobs.contract.EventTypes;
import edu.aiplatform.shared.error.ApiException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;

/** F9, F10, F11; BR-U07-33…36; P2, P3. */
class AssignmentPublicationServiceTest {

    private final AssignmentFixtures f = new AssignmentFixtures();

    private static String code(Runnable action) {
        try {
            action.run();
        } catch (ApiException e) {
            return e.code();
        }
        throw new AssertionError("Cần lỗi nghiệp vụ");
    }

    private Assignment reviewed() {
        return f.put(AssignmentScope.CLASS, AssignmentStatus.REVIEWED, new DocumentModel(List.of(heading("H1", "Mục"))), true);
    }

    private static AssignmentCommands.Publish publish(long version, Instant opens, Instant closes, Instant late, int attempts) {
        return new AssignmentCommands.Publish(version, opens, false, closes, late, attempts, false);
    }

    @Test
    void publishGoesStraightFromDraftCheckingReadinessAndSchedule() {
        Instant later = NOW.plus(Duration.ofDays(1));
        Instant end = NOW.plus(Duration.ofDays(7));
        Assignment empty = f.put(AssignmentScope.CLASS, AssignmentStatus.DRAFT, new DocumentModel(List.of(heading("H1", "Mục"))), false);
        assertThat(code(() -> f.publication.publish(f.teacher, empty.id(), publish(empty.version(), later, end, null, 1))))
                .as("bài nháp chưa đủ: luật duyệt kiểm ngay khi phát hành").isEqualTo("ASSIGNMENT_NOT_READY");
        Assignment draft = f.put(AssignmentScope.CLASS, AssignmentStatus.DRAFT, new DocumentModel(List.of(heading("H1", "Mục"))), true);
        assertThat(f.publication.publish(f.teacher, draft.id(), publish(draft.version(), later, end, null, 1)).status())
                .as("không cần qua Đã duyệt").isEqualTo(AssignmentStatus.SCHEDULED);
        Assignment open = f.put(AssignmentScope.CLASS, AssignmentStatus.OPEN, new DocumentModel(List.of(heading("H1", "Mục"))), true);
        assertThat(code(() -> f.publication.publish(f.teacher, open.id(), publish(open.version(), later, end, null, 1))))
                .isEqualTo("ASSIGNMENT_STATUS_INVALID");
        Assignment a = reviewed();
        assertThat(code(() -> f.publication.publish(f.teacher, a.id(), publish(a.version(), end, later, null, 1))))
                .isEqualTo("SCHEDULE_INVALID");
        assertThat(code(() -> f.publication.publish(f.teacher, a.id(),
                publish(a.version(), later, end, end.plus(Duration.ofDays(31)), 1)))).isEqualTo("SCHEDULE_INVALID");
        assertThat(code(() -> f.publication.publish(f.teacher, a.id(), publish(a.version(), later, end, null, 11))))
                .isEqualTo("SCHEDULE_INVALID");
        assertThat(code(() -> f.publication.publish(f.teacher, a.id(),
                publish(a.version(), NOW.minus(Duration.ofDays(9)), NOW.minus(Duration.ofDays(1)), null, 1))))
                .isEqualTo("SCHEDULE_INVALID");
    }

    @Test
    void publishSchedulesLocksRubricsAndAudits() {
        Assignment a = reviewed();
        AssignmentViews.Detail detail = f.publication.publish(f.teacher, a.id(),
                publish(a.version(), NOW.plus(Duration.ofDays(1)), NOW.plus(Duration.ofDays(7)), NOW.plus(Duration.ofDays(8)), 3));
        assertThat(detail.status()).isEqualTo(AssignmentStatus.SCHEDULED);
        assertThat(detail.maxAttempts()).isEqualTo(3);
        assertThat(f.rubricsOf(a.id())).allMatch(AssignmentRubric::locked);
        verify(f.audit).record(argThat(e -> e.action().equals("ASSIGNMENT_PUBLISHED")));
        verify(f.listener, never()).onOpened(any());
    }

    @Test
    void openNowRunsListenersAndPublishesEvent() {
        Assignment a = reviewed();
        AssignmentViews.Detail detail = f.publication.publish(f.teacher, a.id(),
                new AssignmentCommands.Publish(a.version(), null, true, NOW.plus(Duration.ofDays(7)), null, 1, false));
        assertThat(detail.status()).isEqualTo(AssignmentStatus.OPEN);
        verify(f.listener).onOpened(a.id());
        verify(f.events).publish(argThat(e -> e.eventType().equals(EventTypes.ASSIGNMENT_OPENED)
                && a.id().toString().equals(e.payload().get("assignmentId"))));
    }

    @Test
    void classMustBeOpenToPublish() {
        Assignment a = reviewed();
        when(f.classes.getClassRef(any())).thenAnswer(i -> Optional.of(f.classRef(i.getArgument(0), ClassStatus.DRAFT)));
        assertThat(code(() -> f.publication.publish(f.teacher, a.id(),
                publish(a.version(), NOW.plus(Duration.ofDays(1)), NOW.plus(Duration.ofDays(7)), null, 1))))
                .isEqualTo("CLASS_NOT_OPEN");
    }

    @Test
    void openAssignmentOnlyExtendsDeadlines() {
        Assignment open = f.put(AssignmentScope.CLASS, AssignmentStatus.OPEN, new DocumentModel(List.of()), true);
        assertThat(code(() -> f.publication.changeSchedule(f.teacher, open.id(), new AssignmentCommands.Schedule(
                open.version(), null, open.closesAt().minus(Duration.ofHours(1)), null, null))))
                .isEqualTo("SCHEDULE_SHORTENED");
        assertThat(code(() -> f.publication.changeSchedule(f.teacher, open.id(), new AssignmentCommands.Schedule(
                open.version(), null, null, null, 5)))).isEqualTo("SCHEDULE_OPEN_LOCKED");
        AssignmentViews.Detail extended = f.publication.changeSchedule(f.teacher, open.id(), new AssignmentCommands.Schedule(
                open.version(), null, open.closesAt().plus(Duration.ofDays(2)), null, null));
        assertThat(extended.closesAt()).isEqualTo(open.closesAt().plus(Duration.ofDays(2)));
        verify(f.audit).record(argThat(e -> e.action().equals("ASSIGNMENT_SCHEDULE_CHANGED")));
    }

    @Test
    void lateWindowCanStillBeExtended() {
        Assignment put = f.put(AssignmentScope.CLASS, AssignmentStatus.OPEN, new DocumentModel(List.of()), true);
        Assignment late = put.withSchedule(AssignmentStatus.OPEN, NOW.minus(Duration.ofDays(2)), NOW.minus(Duration.ofHours(1)),
                NOW.plus(Duration.ofDays(1)), 2, NOW);
        f.store.put(late.id(), late);
        AssignmentViews.Detail extended = f.publication.changeSchedule(f.teacher, late.id(), new AssignmentCommands.Schedule(
                late.version(), null, null, NOW.plus(Duration.ofDays(3)), null));
        assertThat(extended.lateUntil()).isEqualTo(NOW.plus(Duration.ofDays(3)));
        assertThat(code(() -> f.publication.changeSchedule(f.teacher, late.id(), new AssignmentCommands.Schedule(
                extended.version(), null, null, null, null)))).isEqualTo("SCHEDULE_SHORTENED");
    }

    @Test
    void retireNeedsReasonAndRunsListenerInSameCall() {
        Assignment open = f.put(AssignmentScope.CLASS, AssignmentStatus.OPEN, new DocumentModel(List.of()), true);
        assertThat(code(() -> f.publication.retire(f.teacher, open.id(), new AssignmentCommands.Retire(open.version(), " "))))
                .isEqualTo("RETIRE_REASON_INVALID");
        AssignmentViews.Detail retired = f.publication.retire(f.teacher, open.id(),
                new AssignmentCommands.Retire(open.version(), "Đề sai"));
        assertThat(retired.status()).isEqualTo(AssignmentStatus.RETIRED);
        assertThat(retired.retiredReason()).isEqualTo("Đề sai");
        verify(f.listener).onRetired(open.id());
        verify(f.audit).record(argThat(e -> e.action().equals("ASSIGNMENT_RETIRED") && "Đề sai".equals(e.details().get("reason"))));
        Assignment closed = f.put(AssignmentScope.CLASS, AssignmentStatus.CLOSED, new DocumentModel(List.of()), true);
        assertThat(code(() -> f.publication.retire(f.teacher, closed.id(), new AssignmentCommands.Retire(closed.version(), "x"))))
                .isEqualTo("ASSIGNMENT_STATUS_INVALID");
    }

    @Test
    void scannerOpensDueAssignmentsOnceAndClosesExpired() {
        Assignment a = f.put(AssignmentScope.CLASS, AssignmentStatus.REVIEWED, new DocumentModel(List.of()), true);
        Assignment scheduled = f.withStatus(a, AssignmentStatus.SCHEDULED, NOW.minusSeconds(30), NOW.plus(Duration.ofDays(1)));
        when(f.assignments.dueToOpen(any(), org.mockito.ArgumentMatchers.anyInt())).thenReturn(List.of(scheduled.id()));
        PlatformTransactionManager transactions = f.transactions;
        AssignmentScheduleScanner scanner = new AssignmentScheduleScanner(f.assignments, f.lifecycle, transactions);
        scanner.scan(NOW);
        scanner.scan(NOW);
        assertThat(f.store.get(scheduled.id()).status()).isEqualTo(AssignmentStatus.OPEN);
        verify(f.listener).onOpened(scheduled.id());
        verify(f.events).publish(any());
        verify(f.assignments, org.mockito.Mockito.times(2)).closeDue(NOW);
    }
}
