package edu.aiplatform.grading.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

import edu.aiplatform.academics.contract.*;
import edu.aiplatform.assignments.contract.*;
import edu.aiplatform.audit.contract.IAudit;
import edu.aiplatform.grading.repository.GradeDistributionVisibilityRepository;
import edu.aiplatform.grading.repository.GradeDistributionVisibilityRepository.Visibility;
import edu.aiplatform.identity.contract.*;
import edu.aiplatform.shared.error.ApiException;
import java.time.Clock;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GradeDistributionVisibilityServiceTest {
    final UUID classId = UUID.randomUUID(), assignmentId = UUID.randomUUID(), subjectId = UUID.randomUUID();
    final Actor teacher = new Actor(UUID.randomUUID(), Role.TEACHER);
    final IClassAccess classAccess = mock(IClassAccess.class);
    final IAssignmentQuery assignmentQuery = mock(IAssignmentQuery.class);
    final IAuthorization auth = mock(IAuthorization.class);
    final IAudit audit = mock(IAudit.class);
    final GradeDistributionVisibilityRepository gradeDistributionVisibilityRepository = mock(GradeDistributionVisibilityRepository.class);
    final GradeDistributionVisibilityService gradeDistributionVisibilityService = new GradeDistributionVisibilityService(gradeDistributionVisibilityRepository,
            new GradingAccess(auth, classAccess, audit, Clock.systemUTC()), classAccess, assignmentQuery, audit, Clock.systemUTC());

    @BeforeEach void setup() {
        when(auth.authorize(any(), anyString(), any())).thenReturn(AuthorizationDecision.allow());
        course(ClassStatus.OPEN);
        assignment(AssignmentScope.CLASS, AssignmentStatus.DRAFT, classId, GradingMode.GRADED);
        when(gradeDistributionVisibilityRepository.findVisibility(classId, assignmentId)).thenReturn(new Visibility(false, 0));
    }

    void course(ClassStatus status) {
        when(classAccess.getClassRef(classId)).thenReturn(Optional.of(
                new ClassRef(classId, subjectId, "C", "Class", status, teacher.accountId(), true)));
    }

    void assignment(AssignmentScope scope, AssignmentStatus status, UUID ownerClassId, GradingMode mode) {
        when(assignmentQuery.get(assignmentId)).thenReturn(Optional.of(new AssignmentView(assignmentId, scope,
                ownerClassId, subjectId, 1, "Task", SubmissionMode.INDIVIDUAL, mode, false, status,
                null, null, null, 1, false, null, List.of())));
    }

    @Test void absentDefaultsOffDespiteLegacyClassFlagAndClassDraftIsEditable() {
        assertThat(gradeDistributionVisibilityService.loadVisibility(teacher, classId, assignmentId))
                .isEqualTo(new GradeDistributionVisibilityService.View(false, 0, true));
        when(gradeDistributionVisibilityRepository.updateVisibility(classId, assignmentId, true, 0)).thenReturn(true);
        assertThat(gradeDistributionVisibilityService.updateVisibility(teacher, classId, assignmentId, true, 0).version()).isEqualTo(1);
        verify(auth, atLeastOnce()).authorize(eq(teacher), eq(Actions.GRADING_WRITE), any());
        verify(audit).record(argThat(e -> e.action().equals("GRADE_DISTRIBUTION_VISIBILITY_CHANGED")));
    }

    @Test void staleVersionCannotOverwriteAndDoesNotAuditSuccess() {
        assertThatThrownBy(() -> gradeDistributionVisibilityService.updateVisibility(teacher, classId, assignmentId, true, 3))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.status().value()).isEqualTo(409));
        verifyNoInteractions(audit);
    }

    @Test void managerMustBeAssignedAndAssignedManagerCanEdit() {
        var manager = new Actor(UUID.randomUUID(), Role.SUBJECT_MANAGER);
        assertThatThrownBy(() -> gradeDistributionVisibilityService.loadVisibility(manager, classId, assignmentId)).isInstanceOf(ApiException.class);
        var assignedManager = new Actor(teacher.accountId(), Role.SUBJECT_MANAGER);
        assertThat(gradeDistributionVisibilityService.loadVisibility(assignedManager, classId, assignmentId).canEdit()).isTrue();
    }

    @Test void studentAndFormerTeacherCannotReadOrChangePreference() {
        for (var actor : List.of(new Actor(UUID.randomUUID(), Role.STUDENT), new Actor(UUID.randomUUID(), Role.TEACHER))) {
            assertThatThrownBy(() -> gradeDistributionVisibilityService.loadVisibility(actor, classId, assignmentId)).isInstanceOf(ApiException.class);
            assertThatThrownBy(() -> gradeDistributionVisibilityService.updateVisibility(actor, classId, assignmentId, true, 0)).isInstanceOf(ApiException.class);
        }
        verifyNoInteractions(gradeDistributionVisibilityRepository);
    }

    @Test void policyDenialFailsClosedEvenForAssignedTeacher() {
        when(auth.authorize(any(), anyString(), any())).thenReturn(AuthorizationDecision.deny("DENIED"));
        assertThatThrownBy(() -> gradeDistributionVisibilityService.loadVisibility(teacher, classId, assignmentId)).isInstanceOf(ApiException.class);
        verifyNoInteractions(gradeDistributionVisibilityRepository);
    }

    @Test void archivedOrRetiredCanBeReadButCannotBeChanged() {
        course(ClassStatus.ARCHIVED);
        assertThat(gradeDistributionVisibilityService.loadVisibility(teacher, classId, assignmentId).canEdit()).isFalse();
        assertThatThrownBy(() -> gradeDistributionVisibilityService.updateVisibility(teacher, classId, assignmentId, true, 0)).isInstanceOf(ApiException.class);
        course(ClassStatus.OPEN);
        assignment(AssignmentScope.CLASS, AssignmentStatus.RETIRED, classId, GradingMode.GRADED);
        assertThat(gradeDistributionVisibilityService.loadVisibility(teacher, classId, assignmentId).canEdit()).isFalse();
        assertThatThrownBy(() -> gradeDistributionVisibilityService.updateVisibility(teacher, classId, assignmentId, true, 0)).isInstanceOf(ApiException.class);
        verify(gradeDistributionVisibilityRepository, never()).updateVisibility(any(), any(), anyBoolean(), anyLong());
    }

    @Test void rejectsOtherClassUngradedAndUnpublishedSubjectAssignments() {
        assignment(AssignmentScope.CLASS, AssignmentStatus.OPEN, UUID.randomUUID(), GradingMode.GRADED);
        assertThatThrownBy(() -> gradeDistributionVisibilityService.loadVisibility(teacher, classId, assignmentId)).isInstanceOf(ApiException.class);
        assignment(AssignmentScope.CLASS, AssignmentStatus.OPEN, classId, GradingMode.values()[1]);
        assertThatThrownBy(() -> gradeDistributionVisibilityService.loadVisibility(teacher, classId, assignmentId)).isInstanceOf(ApiException.class);
        for (var status : List.of(AssignmentStatus.DRAFT, AssignmentStatus.REVIEWED)) {
            assignment(AssignmentScope.SUBJECT, status, null, GradingMode.GRADED);
            assertThatThrownBy(() -> gradeDistributionVisibilityService.loadVisibility(teacher, classId, assignmentId)).isInstanceOf(ApiException.class);
        }
        assignment(AssignmentScope.SUBJECT, AssignmentStatus.OPEN, null, GradingMode.GRADED);
        assertThat(gradeDistributionVisibilityService.loadVisibility(teacher, classId, assignmentId).canEdit()).isTrue();
    }
}
