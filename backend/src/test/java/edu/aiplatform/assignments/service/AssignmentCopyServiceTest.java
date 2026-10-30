package edu.aiplatform.assignments.service;

import edu.aiplatform.assignments.dto.AssignmentViews;

import static edu.aiplatform.assignments.service.AssignmentFixtures.heading;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.aiplatform.assignments.contract.AssignmentScope;
import edu.aiplatform.assignments.contract.AssignmentStatus;
import edu.aiplatform.assignments.contract.DocumentModel;
import edu.aiplatform.assignments.model.Assignment;
import edu.aiplatform.assignments.model.AssignmentOrigin;
import edu.aiplatform.assignments.model.AssignmentRubric;
import edu.aiplatform.identity.contract.AuthorizationDecision;
import edu.aiplatform.identity.contract.ResourceRef;
import edu.aiplatform.shared.error.ApiException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** F12, F13; BR-U07-25, 40…43. */
class AssignmentCopyServiceTest {

    private final AssignmentFixtures f = new AssignmentFixtures();

    private static String code(Runnable action) {
        try {
            action.run();
        } catch (ApiException e) {
            return e.code();
        }
        throw new AssertionError("Cần lỗi nghiệp vụ");
    }

    @Test
    void cloneCopiesContentAndNewUnlockedRubricsButNotSchedule() {
        Assignment open = f.put(AssignmentScope.CLASS, AssignmentStatus.OPEN, new DocumentModel(List.of(heading("H1", "Mục"))), true);
        AssignmentViews.Detail copy = f.copies.cloneAssignment(f.teacher, open.id());
        assertThat(copy.status()).isEqualTo(AssignmentStatus.DRAFT);
        assertThat(copy.origin()).isEqualTo(AssignmentOrigin.CLONE);
        assertThat(copy.title()).as("nhân bản giữ nguyên tên như bài thường").isEqualTo(open.title());
        assertThat(copy.opensAt()).isNull();
        assertThat(copy.maxAttempts()).isNull();
        assertThat(copy.skeleton().blocks()).hasSize(1);
        List<AssignmentRubric> rubrics = f.rubricsOf(copy.id());
        assertThat(rubrics).hasSize(1).allSatisfy(r -> {
            assertThat(r.locked()).isFalse();
            assertThat(r.complete()).isTrue();
        });
        assertThat(rubrics.get(0).id()).isNotIn(f.rubricsOf(open.id()).stream().map(AssignmentRubric::id).toList());
        verify(f.audit).record(argThat(e -> e.action().equals("ASSIGNMENT_CLONED")));
    }

    @Test
    void newVersionOnlyFromClosedOrRetiredAndOneOpenVersion() {
        Assignment open = f.put(AssignmentScope.CLASS, AssignmentStatus.OPEN, new DocumentModel(List.of()), true);
        assertThat(code(() -> f.copies.newVersion(f.teacher, open.id()))).isEqualTo("ASSIGNMENT_STATUS_INVALID");
        Assignment closed = f.put(AssignmentScope.CLASS, AssignmentStatus.CLOSED, new DocumentModel(List.of()), true);
        AssignmentViews.Detail next = f.copies.newVersion(f.teacher, closed.id());
        assertThat(next.versionNo()).isEqualTo(closed.versionNo() + 1);
        assertThat(next.sourceAssignmentId()).isEqualTo(closed.id());
        assertThat(code(() -> f.copies.newVersion(f.teacher, closed.id()))).isEqualTo("VERSION_ALREADY_OPEN");
    }

    @Test
    void copyBetweenClassesNeedsTeachingBothClasses() {
        Assignment source = f.put(AssignmentScope.CLASS, AssignmentStatus.CLOSED, new DocumentModel(List.of()), true);
        UUID target = UUID.randomUUID();
        when(f.authorization.authorize(any(), any(), any())).thenAnswer(i -> {
            ResourceRef ref = i.getArgument(2);
            return target.equals(ref.id()) ? AuthorizationDecision.allow() : AuthorizationDecision.deny("OUT");
        });
        assertThat(code(() -> f.copies.copyToClass(f.teacher, target, source.id()))).isEqualTo("ASSIGNMENT_NOT_FOUND");
        doReturn(AuthorizationDecision.allow()).when(f.authorization).authorize(any(), any(), any());
        AssignmentViews.Detail copy = f.copies.copyToClass(f.teacher, target, source.id());
        assertThat(copy.classId()).isEqualTo(target);
        assertThat(copy.origin()).isEqualTo(AssignmentOrigin.CLASS_COPY);
        assertThat(code(() -> f.copies.copyToClass(f.teacher, source.classId(), source.id()))).isEqualTo("COPY_SAME_CLASS");
    }

    @Test
    void classCopySourcesListPublishedSubjectAssignmentsFirst() {
        f.put(AssignmentScope.SUBJECT, AssignmentStatus.OPEN, new DocumentModel(List.of()), true);
        f.put(AssignmentScope.SUBJECT, AssignmentStatus.DRAFT, new DocumentModel(List.of()), true);
        UUID other = UUID.randomUUID();
        when(f.classScope.listTaughtClasses(f.teacher.accountId())).thenReturn(List.of(f.classId, other));
        f.put(AssignmentScope.CLASS, AssignmentStatus.CLOSED, new DocumentModel(List.of()), true);
        List<AssignmentViews.CopySource> sources = f.copies.copySources(f.teacher, other);
        assertThat(sources).extracting(AssignmentViews.CopySource::kind).containsExactly("SUBJECT", "CLASS");
        assertThat(sources.get(0).assignments()).as("chỉ bài môn đã phát hành").hasSize(1);
        assertThat(sources.get(1).classId()).isEqualTo(f.classId);
    }

    @Test
    void copyPublishedSubjectAssignmentIntoClassButNotDraft() {
        Assignment open = f.put(AssignmentScope.SUBJECT, AssignmentStatus.OPEN, new DocumentModel(List.of()), true);
        AssignmentViews.Detail copy = f.copies.copyToClass(f.teacher, f.classId, open.id());
        assertThat(copy.scope()).isEqualTo(AssignmentScope.CLASS);
        assertThat(copy.classId()).isEqualTo(f.classId);
        assertThat(copy.status()).isEqualTo(AssignmentStatus.DRAFT);
        Assignment draft = f.put(AssignmentScope.SUBJECT, AssignmentStatus.DRAFT, new DocumentModel(List.of()), true);
        assertThat(code(() -> f.copies.copyToClass(f.teacher, f.classId, draft.id()))).isEqualTo("ASSIGNMENT_NOT_FOUND");
    }

    @Test
    void copyIntoSubjectFromSameSubjectOrTaughtClass() {
        Assignment sibling = f.put(AssignmentScope.SUBJECT, AssignmentStatus.DRAFT, new DocumentModel(List.of()), true);
        AssignmentViews.Detail fromSubject = f.copies.copyToSubject(f.manager, f.subjectId, sibling.id());
        assertThat(fromSubject.scope()).isEqualTo(AssignmentScope.SUBJECT);
        assertThat(fromSubject.classId()).isNull();
        assertThat(fromSubject.subjectId()).isEqualTo(f.subjectId);
        Assignment ofClass = f.put(AssignmentScope.CLASS, AssignmentStatus.CLOSED, new DocumentModel(List.of()), true);
        assertThat(f.copies.copyToSubject(f.manager, f.subjectId, ofClass.id()).scope()).isEqualTo(AssignmentScope.SUBJECT);
        assertThat(code(() -> f.copies.copyToSubject(f.manager, UUID.randomUUID(), sibling.id())))
                .isEqualTo("ASSIGNMENT_NOT_FOUND");
    }
}
