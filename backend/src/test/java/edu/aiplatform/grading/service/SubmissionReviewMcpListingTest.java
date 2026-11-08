package edu.aiplatform.grading.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.academics.contract.*;
import edu.aiplatform.assignments.contract.*;
import edu.aiplatform.assignmentwork.contract.*;
import edu.aiplatform.grading.model.Evaluation;
import edu.aiplatform.grading.repository.IEvaluationRepository;
import edu.aiplatform.identity.contract.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class SubmissionReviewMcpListingTest {
    @Test void authorizedMetadataIsBuiltOnceAndContactsAreFetchedInBulk() {
        var actor=new Actor(UUID.randomUUID(),Role.TEACHER);
        UUID classId=UUID.randomUUID(),assignmentId=UUID.randomUUID(),subjectId=UUID.randomUUID();
        var repo=mock(IEvaluationRepository.class);var access=mock(GradingAccess.class);
        var assignments=mock(IAssignmentQuery.class);var assignment=mock(AssignmentView.class);
        when(assignments.get(assignmentId)).thenReturn(Optional.of(assignment));
        when(assignment.gradingMode()).thenReturn(GradingMode.GRADED);
        when(assignment.scope()).thenReturn(AssignmentScope.SUBJECT);
        when(assignment.subjectId()).thenReturn(subjectId);
        when(assignment.submissionMode()).thenReturn(SubmissionMode.INDIVIDUAL);
        var classes=mock(IClassAccess.class);
        when(classes.getClassRef(classId)).thenReturn(Optional.of(new ClassRef(classId,subjectId,"C","Class",ClassStatus.OPEN,actor.accountId(),false)));
        var submissions=mock(ISubmissionQuery.class);var accounts=mock(IAccountLookup.class);var activity=mock(IActivityQuery.class);
        List<UUID> students=new ArrayList<>();List<StudentSubmissionStatus> statuses=new ArrayList<>();List<Evaluation> evals=new ArrayList<>();
        for(int n=0;n<200;n++) {
            var student=UUID.randomUUID();students.add(student);
            var e=Evaluation.createPendingEvaluation(UUID.randomUUID(),assignmentId,classId,student,"hash",BigDecimal.TEN,"[]",Instant.now());evals.add(e);
            statuses.add(new StudentSubmissionStatus(student,e.attemptId,1,AttemptStatus.SUBMITTED,Instant.now(),n%2==0));
        }
        when(classes.listActiveStudents(classId)).thenReturn(students);
        when(repo.findForAssignmentAndClass(assignmentId,classId)).thenReturn(evals);
        when(submissions.listSubmissions(assignmentId,classId)).thenReturn(statuses);
        when(accounts.getContacts(students)).thenReturn(Map.of());
        var service=new SubmissionReviewService(repo,access,assignments,submissions,classes,accounts,activity,
                mock(ObjectProvider.class),new ObjectMapper());
        var listing=service.listRows(actor,classId,assignmentId,"PENDING",true);
        assertThat(listing.rows()).hasSize(100);
        verify(accounts).getContacts(students);
        verify(accounts,never()).getContact(any());
        verify(submissions,never()).getSubmission(any());
        verifyNoInteractions(activity);
        verify(repo).findForAssignmentAndClass(assignmentId,classId);
        doThrow(edu.aiplatform.shared.error.ApiException.notFound("CLASS_NOT_FOUND"))
                .when(access).requireTeacher(actor,classId,Actions.GRADING_REVIEW);
        assertThatThrownBy(() -> service.listRows(actor,classId,assignmentId,"ALL",null))
                .isInstanceOf(edu.aiplatform.shared.error.ApiException.class);
    }
}
