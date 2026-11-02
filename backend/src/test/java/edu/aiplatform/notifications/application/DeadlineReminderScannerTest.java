package edu.aiplatform.notifications.application;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import edu.aiplatform.academics.contract.*;
import edu.aiplatform.assignments.contract.*;
import edu.aiplatform.assignmentwork.contract.ISubmissionQuery;
import edu.aiplatform.notifications.domain.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.transaction.*;
import org.springframework.transaction.support.*;

class DeadlineReminderScannerTest {
    IAssignmentQuery assignments;IClassAccess classes;ISubmissionQuery submissions;INotificationRepository repo;NotificationService notices;DeadlineReminderScanner scanner;
    Instant now=Instant.parse("2026-10-21T08:00:00Z");UUID id=UUID.randomUUID(),classId=UUID.randomUUID(),subject=UUID.randomUUID(),student=UUID.randomUUID();
    @BeforeEach void setup(){assignments=mock(IAssignmentQuery.class);classes=mock(IClassAccess.class);submissions=mock(ISubmissionQuery.class);repo=mock(INotificationRepository.class);notices=mock(NotificationService.class);
        var manager=new AbstractPlatformTransactionManager(){protected Object doGetTransaction(){return new Object();}protected void doBegin(Object t,TransactionDefinition d){}protected void doCommit(DefaultTransactionStatus s){}protected void doRollback(DefaultTransactionStatus s){}};
        scanner=new DeadlineReminderScanner(assignments,classes,submissions,repo,notices,manager);
        when(classes.getClassRef(classId)).thenReturn(Optional.of(course(classId)));when(classes.listActiveStudents(classId)).thenReturn(List.of(student));when(submissions.listStudentsWithoutSubmission(id,classId)).thenReturn(List.of(student));when(repo.reminder(any(),any(),any(),anyString(),any())).thenReturn(true);
    }
    ClassRef course(UUID id){return new ClassRef(id,subject,"C","Class",ClassStatus.OPEN,UUID.randomUUID(),false);}
    AssignmentView assignment(Instant opens,Instant closes,UUID assignedClass){return new AssignmentView(id,assignedClass==null?AssignmentScope.SUBJECT:AssignmentScope.CLASS,assignedClass,subject,1,"Bài",SubmissionMode.INDIVIDUAL,GradingMode.GRADED,false,AssignmentStatus.OPEN,opens,closes,null,null,false,null,List.of());}
    void source(AssignmentView a){when(assignments.listClosingBetween(any(),any())).thenReturn(List.of(a));when(assignments.get(id)).thenReturn(Optional.of(a));}
    @Test void reminderUsesStudentRouteWithClassContext() {
        source(assignment(now.minusSeconds(1000),now.plus(Duration.ofHours(24)),classId));
        scanner.scan(now);
        verify(notices).create(any(),eq(NotificationType.DEADLINE_REMINDER),eq(List.of(student)),anyString(),anyString(),
                eq("/my-assignments/"+id+"?classId="+classId),eq("DEADLINE_REMINDER"),any());
    }
    @Test void failingClassDoesNotStopOtherClassesOrFollowingAssignments() {
        var a=assignment(now.minusSeconds(1000),now.plus(Duration.ofHours(24)),null);
        var secondClass=UUID.randomUUID();
        var next=new AssignmentView(UUID.randomUUID(),AssignmentScope.CLASS,classId,subject,1,"Next",SubmissionMode.INDIVIDUAL,
                GradingMode.GRADED,false,AssignmentStatus.OPEN,a.opensAt(),a.closesAt(),null,null,false,null,List.of());
        source(a);
        when(assignments.listClosingBetween(any(),any())).thenReturn(List.of(a,next));
        when(assignments.get(next.assignmentId())).thenReturn(Optional.of(next));
        when(classes.listOpenClassesOfSubject(subject)).thenReturn(List.of(course(classId),course(secondClass)));
        when(classes.getClassRef(secondClass)).thenReturn(Optional.of(course(secondClass)));
        when(classes.listActiveStudents(secondClass)).thenReturn(List.of(student));
        when(submissions.listStudentsWithoutSubmission(id,classId)).thenThrow(new IllegalStateException("unavailable"));
        scanner.scan(now);
        verify(submissions).listStudentsWithoutSubmission(id,secondClass);
        verify(submissions).listStudentsWithoutSubmission(next.assignmentId(),classId);
        verify(notices,times(2)).create(any(),any(),anyList(),anyString(),anyString(),anyString(),anyString(),any());
    }
    @Test void failingAssignmentLookupOrClassExpansionDoesNotStopNextAssignment() {
        var a=assignment(now.minusSeconds(1000),now.plus(Duration.ofHours(24)),classId);
        var broken=new AssignmentView(UUID.randomUUID(),AssignmentScope.SUBJECT,null,subject,1,"Broken",SubmissionMode.INDIVIDUAL,
                GradingMode.GRADED,false,AssignmentStatus.OPEN,a.opensAt(),a.closesAt(),null,null,false,null,List.of());
        source(a);
        when(assignments.listClosingBetween(any(),any())).thenReturn(List.of(broken,a));
        when(assignments.get(broken.assignmentId())).thenThrow(new IllegalStateException("lookup unavailable"));
        scanner.scan(now);
        verify(notices).create(any(),any(),anyList(),anyString(),anyString(),anyString(),anyString(),any());
        clearInvocations(notices);
        doReturn(Optional.of(broken)).when(assignments).get(broken.assignmentId());
        when(classes.listOpenClassesOfSubject(subject)).thenThrow(new IllegalStateException("classes unavailable"));
        scanner.scan(now);
        verify(notices).create(any(),any(),anyList(),anyString(),anyString(),anyString(),anyString(),any());
    }
    @Test void reminderOncePerDeadlineAndOnlyNonSubmitters(){source(assignment(now.minusSeconds(5),now.plus(Duration.ofHours(24)),classId));when(repo.reminder(eq(id),eq(classId),any(),eq("SENT"),eq(now))).thenReturn(true,false);scanner.scan(now);scanner.scan(now);verify(notices,times(1)).create(any(),eq(NotificationType.DEADLINE_REMINDER),eq(List.of(student)),anyString(),anyString(),anyString(),eq("DEADLINE_REMINDER"),any());}
    @Test void lateOpenedAssignmentMarkedSkipped(){source(assignment(now.plusSeconds(1),now.plus(Duration.ofHours(24)),classId));scanner.scan(now);verify(repo).reminder(id,classId,now.plus(Duration.ofHours(24)),"SKIPPED",now);verifyNoInteractions(notices);}
    @Test void outsideFiveMinuteWindowNotSent(){source(assignment(now.minusSeconds(1000),now.plus(Duration.ofHours(24)).minusSeconds(301),classId));scanner.scan(now);verifyNoInteractions(notices);verify(repo,never()).reminder(any(),any(),any(),anyString(),any());}
    @Test void subjectAssignmentExpandsOpenClasses(){var a=assignment(now.minusSeconds(1000),now.plus(Duration.ofHours(24)),null);source(a);when(classes.listOpenClassesOfSubject(subject)).thenReturn(List.of(course(classId)));scanner.scan(now);verify(submissions).listStudentsWithoutSubmission(id,classId);}
    @Test void removedEnrollmentIsNotNotified(){source(assignment(now.minusSeconds(1000),now.plus(Duration.ofHours(24)),classId));when(classes.listActiveStudents(classId)).thenReturn(List.of());scanner.scan(now);verify(notices).create(any(),any(),eq(List.of()),anyString(),anyString(),anyString(),anyString(),any());}
    @Test void changedDeadlineGetsNewMarker(){var a=assignment(now.minusSeconds(1000),now.plus(Duration.ofHours(24)),classId);source(a);scanner.scan(now);var changed=assignment(a.opensAt(),a.closesAt().plusSeconds(60),classId);source(changed);scanner.scan(now.plusSeconds(60));verify(repo).reminder(id,classId,a.closesAt(),"SENT",now);verify(repo).reminder(id,classId,changed.closesAt(),"SENT",now.plusSeconds(60));}
    @Test void twoDayRescheduleIgnoresOldDeadlineAndSendsOnlyOnceAtTheNewDeadline(){
        var old=assignment(now.minusSeconds(1000),now.plus(Duration.ofHours(24)),classId);
        var changed=assignment(old.opensAt(),old.closesAt().plus(Duration.ofDays(2)),classId);
        source(old);when(assignments.get(id)).thenReturn(Optional.of(changed));
        scanner.scan(now);verifyNoInteractions(notices);verifyNoInteractions(repo);
        source(changed);var later=now.plus(Duration.ofDays(2));
        when(repo.reminder(id,classId,changed.closesAt(),"SENT",later)).thenReturn(true,false);
        scanner.scan(later);scanner.scan(later);
        verify(notices,times(1)).create(any(),eq(NotificationType.DEADLINE_REMINDER),eq(List.of(student)),anyString(),anyString(),anyString(),anyString(),eq(changed.closesAt()));
    }
    @Test void assignmentRetiredAfterSelectionDoesNotSendAReminder(){
        var a=assignment(now.minusSeconds(1000),now.plus(Duration.ofHours(24)),classId);source(a);
        var stopped=new AssignmentView(a.assignmentId(),a.scope(),a.classId(),a.subjectId(),a.versionNo(),a.title(),a.submissionMode(),a.gradingMode(),a.diagramEnabled(),AssignmentStatus.RETIRED,a.opensAt(),a.closesAt(),null,null,false,null,List.of());
        when(assignments.get(id)).thenReturn(Optional.of(a),Optional.of(stopped));
        scanner.scan(now);verifyNoInteractions(notices);verifyNoInteractions(repo);
    }
    @Test void maintenanceRetains180DaysAndDelegatesDeferredPriority(){var email=mock(EmailDeliveryService.class);new NotificationMaintenanceScanner(email,repo).scan(now);verify(email).releaseDeferred(now);verify(repo).deleteOld(now.minus(Duration.ofDays(180)));}
}
