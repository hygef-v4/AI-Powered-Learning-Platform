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
    void scheduledCloseWaitsForGraceAndRunsListenersBeforeCommit() {
        Assignment draft = reviewed();
        Assignment open = f.withStatus(draft, AssignmentStatus.OPEN, NOW.minusSeconds(60), NOW.minusSeconds(30));
        assertThat(f.lifecycle.closeAssignment(open.id(), NOW, true)).isFalse();
        assertThat(f.lifecycle.closeAssignment(open.id(), NOW.plusNanos(1), true)).isTrue();
        verify(f.listener).onClosed(open.id());
        assertThat(f.store.get(open.id()).status()).isEqualTo(AssignmentStatus.CLOSED);
    }
}
