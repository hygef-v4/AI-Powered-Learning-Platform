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
import edu.aiplatform.assignments.contract.GradingMode;
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
            return e.readCode();
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
    void scheduledOpeningTimeCannotBeInThePast() {
        Instant end = NOW.plus(Duration.ofDays(7));
        Assignment a = reviewed();
        assertThat(code(() -> f.publication.publish(f.teacher, a.id(),
                publish(a.version(), NOW.minus(Duration.ofHours(1)), end, null, 1))))
                .as("hẹn giờ mở ở quá khứ bị từ chối (yêu cầu 2026-11-26)").isEqualTo("SCHEDULE_INVALID");
        assertThat(f.publication.publish(f.teacher, a.id(),
                publish(a.version(), NOW.minus(Duration.ofSeconds(30)), end, null, 1)).status())
                .as("chừa độ trễ mạng vài chục giây").isNotNull();
        assertThat(code(() -> AssignmentPublicationService.requireNotPast(NOW.minus(Duration.ofMinutes(5)), NOW)))
                .isEqualTo("SCHEDULE_INVALID");
    }

    @Test
    void publishNeedsAFinalizedAssignmentThenChecksReadinessAndSchedule() {
        Instant later = NOW.plus(Duration.ofDays(1));
        Instant end = NOW.plus(Duration.ofDays(7));
        Assignment draft = f.put(AssignmentScope.CLASS, AssignmentStatus.DRAFT, new DocumentModel(List.of(heading("H1", "Mục"))), true);
        assertThat(code(() -> f.publication.publish(f.teacher, draft.id(), publish(draft.version(), later, end, null, 1))))
                .as("phải hoàn tất bài rồi mới xếp lịch (yêu cầu 2026-11-11)").isEqualTo("ASSIGNMENT_NOT_FINALIZED");
        assertThat(f.support.buildDetail(f.teacher, draft, List.of()).permissions().canPublish()).isFalse();
        Assignment finalized = f.authoring.review(f.teacher, draft.id(), draft.version()) == null ? null : f.store.get(draft.id());
        assertThat(finalized.status()).isEqualTo(AssignmentStatus.REVIEWED);
        assertThat(f.support.buildDetail(f.teacher, finalized, List.of()).permissions().canPublish()).isTrue();
        assertThat(f.publication.publish(f.teacher, finalized.id(), publish(finalized.version(), later, end, null, 1)).status())
                .isEqualTo(AssignmentStatus.SCHEDULED);
        Assignment open = f.put(AssignmentScope.CLASS, AssignmentStatus.OPEN, new DocumentModel(List.of(heading("H1", "Mục"))), true);
        assertThat(code(() -> f.publication.publish(f.teacher, open.id(), publish(open.version(), later, end, null, 1))))
                .isEqualTo("ASSIGNMENT_STATUS_INVALID");
        Assignment a = reviewed();
        assertThat(code(() -> f.publication.publish(f.teacher, a.id(), publish(a.version(), end, later, null, 1))))
                .isEqualTo("SCHEDULE_INVALID");
        assertThat(code(() -> f.publication.publish(f.teacher, a.id(),
                publish(a.version(), later, end, end.plus(Duration.ofDays(31)), 1)))).isEqualTo("SCHEDULE_INVALID");
        // Số lượt chỉ kiểm với bài Luyện tập; bài có chấm không có số lượt (yêu cầu 2026-11-10).
        assertThat(code(() -> AssignmentPublicationService.validateSchedule(later, end, null, 11, GradingMode.PRACTICE, NOW)))
                .isEqualTo("SCHEDULE_INVALID");
        assertThat(code(() -> AssignmentPublicationService.validateSchedule(later, end, null, null, GradingMode.PRACTICE, NOW)))
                .isEqualTo("SCHEDULE_INVALID");
        AssignmentPublicationService.validateSchedule(later, end, null, null, GradingMode.GRADED, NOW);
        assertThat(code(() -> f.publication.publish(f.teacher, a.id(),
                publish(a.version(), NOW.minus(Duration.ofDays(9)), NOW.minus(Duration.ofDays(1)), null, 1))))
                .isEqualTo("SCHEDULE_INVALID");
    }

    @Test
    void subjectAssignmentIsTemplateAndCannotBePublished() {
        Instant later = NOW.plus(Duration.ofDays(1));
        Instant end = NOW.plus(Duration.ofDays(7));
        Assignment template = f.put(AssignmentScope.SUBJECT, AssignmentStatus.DRAFT,
                new DocumentModel(List.of(heading("H1", "Mục"))), true);
        assertThat(f.authoring.loadAssignment(f.manager, template.id()).permissions().canPublish()).isFalse();
        assertThat(code(() -> f.publication.publish(f.manager, template.id(),
                publish(template.version(), later, end, null, 1))))
                .isEqualTo("SUBJECT_TEMPLATE_NOT_PUBLISHABLE");
        assertThat(f.assignments.findAssignment(template.id()).orElseThrow().status()).isEqualTo(AssignmentStatus.DRAFT);
    }

    @Test
    void publishSchedulesLocksRubricsAndAudits() {
        Assignment a = reviewed();
        AssignmentViews.Detail detail = f.publication.publish(f.teacher, a.id(),
                publish(a.version(), NOW.plus(Duration.ofDays(1)), NOW.plus(Duration.ofDays(7)), NOW.plus(Duration.ofDays(8)), 3));
        assertThat(detail.status()).isEqualTo(AssignmentStatus.SCHEDULED);
        // Bài có chấm nộp không giới hạn khi còn hạn: số lượt gửi lên bị bỏ qua (yêu cầu 2026-11-10).
        assertThat(detail.maxAttempts()).isNull();
        assertThat(f.rubricsOf(a.id())).allMatch(AssignmentRubric::hasLocked);
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
                open.version(), open.opensAt().plus(Duration.ofHours(1)), null, null, null)))).isEqualTo("SCHEDULE_OPEN_LOCKED");
        AssignmentViews.Detail extended = f.publication.changeSchedule(f.teacher, open.id(), new AssignmentCommands.Schedule(
                open.version(), null, open.closesAt().plus(Duration.ofDays(2)), null, null));
        assertThat(extended.closesAt()).isEqualTo(open.closesAt().plus(Duration.ofDays(2)));
    }

    @Test
    void lateWindowCanStillBeExtended() {
        Assignment put = f.put(AssignmentScope.CLASS, AssignmentStatus.OPEN, new DocumentModel(List.of()), true);
        Assignment late = put.applySchedule(AssignmentStatus.OPEN, NOW.minus(Duration.ofDays(2)), NOW.minus(Duration.ofHours(1)),
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
        Assignment closed = f.put(AssignmentScope.CLASS, AssignmentStatus.CLOSED, new DocumentModel(List.of()), true);
        assertThat(code(() -> f.publication.retire(f.teacher, closed.id(), new AssignmentCommands.Retire(closed.version(), "x"))))
                .isEqualTo("ASSIGNMENT_STATUS_INVALID");
    }

    @Test
    void scannerOpensDueAssignmentsOnceAndClosesExpired() {
        Assignment a = f.put(AssignmentScope.CLASS, AssignmentStatus.REVIEWED, new DocumentModel(List.of()), true);
        Assignment scheduled = f.withStatus(a, AssignmentStatus.SCHEDULED, NOW.minusSeconds(30), NOW.plus(Duration.ofDays(1)));
        when(f.assignments.listDueToOpen(any(), org.mockito.ArgumentMatchers.anyInt())).thenReturn(List.of(scheduled.id()));
        PlatformTransactionManager transactions = f.transactions;
        AssignmentScheduleScanner scanner = new AssignmentScheduleScanner(f.assignments, f.lifecycle, transactions);
        scanner.scan(NOW);
        scanner.scan(NOW);
        assertThat(f.store.get(scheduled.id()).status()).isEqualTo(AssignmentStatus.OPEN);
        verify(f.listener).onOpened(scheduled.id());
        verify(f.events).publish(any());
        verify(f.assignments, org.mockito.Mockito.times(2)).listDueToClose(NOW, AssignmentScheduleScanner.BATCH);
    }

    @Test
    void closeStopsSubmissionsAndChotsSavedWorkWithoutChangingTheSchedule() {
        Assignment draft = reviewed();
        Instant end = NOW.plus(Duration.ofDays(7));
        Assignment open = f.withStatus(draft, AssignmentStatus.OPEN, NOW.minusSeconds(60), end);
        var closed = f.publication.closeAssignment(f.teacher, open.id(), open.version());
        assertThat(closed.status()).isEqualTo(AssignmentStatus.CLOSED);
        assertThat(closed.closesAt()).isEqualTo(end);
        verify(f.listener).onClosed(open.id());
        assertThat(code(() -> f.publication.closeAssignment(f.teacher, open.id(), closed.version())))
                .isEqualTo("ASSIGNMENT_STATUS_INVALID");
    }

    @Test
    void closeChecksVersionClassScopeAndClassState() {
        Assignment open = f.put(AssignmentScope.CLASS, AssignmentStatus.OPEN, new DocumentModel(List.of()), true);
        assertThat(code(() -> f.publication.closeAssignment(f.teacher, open.id(), open.version() + 1)))
                .isEqualTo("ASSIGNMENT_VERSION_CONFLICT");
        Assignment template = f.put(AssignmentScope.SUBJECT, AssignmentStatus.OPEN, new DocumentModel(List.of()), true);
        assertThat(code(() -> f.publication.closeAssignment(f.manager, template.id(), template.version())))
                .isEqualTo("ASSIGNMENT_STATUS_INVALID");
        when(f.classes.getClassRef(open.classId())).thenReturn(Optional.of(
                f.classRef(open.classId(), ClassStatus.ARCHIVED)));
        assertThat(code(() -> f.publication.closeAssignment(f.teacher, open.id(), open.version())))
                .isEqualTo("ASSIGNMENT_READ_ONLY");
        verify(f.listener, never()).onClosed(any());
    }

    @Test
    void scheduledCloseWaitsForGraceAndRunsListenersBeforeCommit() {
        Assignment draft = reviewed();
        Assignment open = f.withStatus(draft, AssignmentStatus.OPEN, NOW.minusSeconds(60), NOW.minusSeconds(30));
        assertThat(f.lifecycle.closeAssignment(open.id(), NOW, true)).isFalse();
        assertThat(f.lifecycle.closeAssignment(open.id(), NOW.plusNanos(1), true)).isTrue();
        verify(f.listener).onClosed(open.id());
        assertThat(f.store.get(open.id()).status()).isEqualTo(AssignmentStatus.CLOSED);
    }
}
