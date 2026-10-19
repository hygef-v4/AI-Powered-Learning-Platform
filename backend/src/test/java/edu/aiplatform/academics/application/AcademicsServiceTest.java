package edu.aiplatform.academics.application;

import edu.aiplatform.academics.domain.*;
import edu.aiplatform.academics.contract.ClassStatus;
import edu.aiplatform.identity.contract.*;
import edu.aiplatform.subjects.contract.*;
import edu.aiplatform.audit.contract.*;
import edu.aiplatform.jobs.contract.*;
import edu.aiplatform.shared.error.ApiException;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.transaction.*;
import org.springframework.transaction.support.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AcademicsServiceTest {
    ICourseClassRepository classes = mock(ICourseClassRepository.class);
    IEnrollmentRepository enrollments = mock(IEnrollmentRepository.class);
    IAcademicsReadRepository reads = mock(IAcademicsReadRepository.class);
    ISubjectMutationGuard lock = mock(ISubjectMutationGuard.class);
    IAccountLookup accounts = mock(IAccountLookup.class);
    ISubjectQuery subjects = mock(ISubjectQuery.class);
    ISubjectScope scope = mock(ISubjectScope.class);
    IAuthorization authorization = mock(IAuthorization.class);
    IAudit audit = mock(IAudit.class);
    IEventPublisher events = mock(IEventPublisher.class);
    UUID managerId = UUID.randomUUID(), subjectId = UUID.randomUUID(), classId = UUID.randomUUID(), studentId = UUID.randomUUID(), teacherId = UUID.randomUUID();
    Instant now = Instant.parse("2026-10-18T00:00:00Z");
    Actor manager = new Actor(managerId, Role.SUBJECT_MANAGER);
    AcademicsService service;
    @BeforeEach void setup() {
        PlatformTransactionManager tx = new AbstractPlatformTransactionManager() {
            protected Object doGetTransaction() { return new Object(); }
            protected void doBegin(Object tx, TransactionDefinition def) { }
            protected void doCommit(DefaultTransactionStatus status) { }
            protected void doRollback(DefaultTransactionStatus status) { }
        };
        service = new AcademicsService(classes,enrollments,reads,lock,accounts,subjects,scope,authorization,audit,events,tx,Clock.fixed(now,ZoneOffset.UTC));
        when(accounts.getContact(managerId)).thenReturn(Optional.of(contact(managerId,Role.SUBJECT_MANAGER,AccountStatus.ACTIVE)));
        when(accounts.getContact(teacherId)).thenReturn(Optional.of(contact(teacherId,Role.TEACHER,AccountStatus.ACTIVE)));
        when(scope.isSubjectManager(managerId,subjectId)).thenReturn(true);
        when(authorization.authorize(any(),anyString(),any())).thenReturn(AuthorizationDecision.allow());
        when(subjects.getSubject(subjectId)).thenReturn(Optional.of(new SubjectRef(subjectId,"S","Subject",SubjectStatus.ACTIVE,managerId)));
        when(subjects.isActive(subjectId)).thenReturn(true);
        when(classes.find(classId)).thenReturn(Optional.of(course(ClassStatus.DRAFT,0)));
    }
    AccountContact contact(UUID id, Role role, AccountStatus status) { return new AccountContact(id,id+"@example.com","Person",role,status); }
    CourseClass course(ClassStatus status,long version) { return new CourseClass(classId,subjectId,"C","Class",null,null,status,teacherId,false,now,now,version); }
    @Test void studentCannotReadDraftClassAndDenialIsAudited() {
        var actor = new Actor(studentId,Role.STUDENT);
        when(accounts.getContact(studentId)).thenReturn(Optional.of(contact(studentId,Role.STUDENT,AccountStatus.ACTIVE)));
        when(enrollments.find(classId,studentId)).thenReturn(Optional.of(Enrollment.create(classId,studentId,EnrollmentSource.MANUAL,now)));
        assertThatThrownBy(() -> service.detail(actor,"student",classId)).isInstanceOfSatisfying(ApiException.class,e -> assertThat(e.status().value()).isEqualTo(404));
        verify(audit).record(argThat(e -> e.result()==AuditResult.DENIED));
    }
    @Test void createLocksTheSubjectRowThroughU04AndArchivedSubjectBlocksTheClass() {
        doThrow(ApiException.conflict("SUBJECT_ARCHIVED","Môn đã lưu trữ")).when(lock).lockActive(eq(subjectId),any());
        assertThatThrownBy(() -> service.create(manager,subjectId,"NEW","New class",null,null,null))
                .isInstanceOfSatisfying(ApiException.class,e -> assertThat(e.code()).isEqualTo("SUBJECT_ARCHIVED"));
        verify(lock).lockActive(eq(subjectId),eq(java.time.Duration.ofSeconds(1)));
        verify(classes,never()).insert(any());
    }
    @Test void adminCannotListClasses() {
        Actor admin = new Actor(UUID.randomUUID(),Role.ADMIN);
        assertThatThrownBy(() -> service.list(admin,"manager",null,null,null,1,20)).isInstanceOfSatisfying(ApiException.class,e -> assertThat(e.status().value()).isEqualTo(403));
        verifyNoInteractions(reads);
    }
    @Test void staleVersionDoesNotWrite() {
        assertThatThrownBy(() -> service.updateInfo(manager,classId,"New",null,null,2)).isInstanceOfSatisfying(ApiException.class,e -> assertThat(e.status().value()).isEqualTo(409));
        verify(classes,never()).update(any(),anyLong());
    }
    @Test void updateLocksSubjectBeforeRereadingAndPreservesCode() {
        var result = service.updateInfo(manager,classId,"New",null,"2026",0);
        assertThat(result.name()).isEqualTo("New");
        assertThat(result.description()).isEmpty();
        assertThat(result.code()).isEqualTo("C");
        var order = inOrder(classes,lock);
        order.verify(classes).find(classId); order.verify(lock).lockActive(org.mockito.ArgumentMatchers.eq(subjectId),org.mockito.ArgumentMatchers.any()); order.verify(classes).find(classId);
        verify(audit).record(argThat(e -> e.action().equals("CLASS_UPDATED") && e.result()==AuditResult.SUCCESS));
    }
    @Test void dependencyFailureIsUnavailableAndWritesNothing() {
        when(scope.isSubjectManager(managerId,subjectId)).thenThrow(new IllegalStateException("internal secret"));
        assertThatThrownBy(() -> service.updateInfo(manager,classId,"New",null,null,0)).isInstanceOfSatisfying(ApiException.class,e -> {assertThat(e.status().value()).isEqualTo(503);assertThat(e.getMessage()).doesNotContain("secret");});
        verify(classes,never()).update(any(),anyLong());
    }
    @Test void reopenReturnsConflictingStudents() {
        when(classes.find(classId)).thenReturn(Optional.of(course(ClassStatus.ARCHIVED,0)));
        when(enrollments.conflictingStudents(classId,subjectId)).thenReturn(List.of(studentId));
        assertThatThrownBy(() -> service.changeStatus(manager,classId,ClassStatus.OPEN,0)).isInstanceOfSatisfying(AcademicsProblem.class,e -> assertThat(e.properties().get("conflictingStudents")).isEqualTo(List.of(studentId)));
        verify(classes,never()).update(any(),anyLong());
    }
    @Test void openDraftPublishesActivationForAllActiveStudents() {
        when(enrollments.listByClass(classId,true,100,0)).thenReturn(List.of(Enrollment.create(classId,studentId,EnrollmentSource.MANUAL,now)));
        service.changeStatus(manager,classId,ClassStatus.OPEN,0);
        verify(events).publish(argThat(e -> e.eventType().equals("enrollment.activated") && e.payload().get("accountId").equals(studentId.toString())));
    }
    @Test void pendingStudentCanEnrollAndDuplicateDoesNotRepublish() {
        when(classes.find(classId)).thenReturn(Optional.of(course(ClassStatus.OPEN,0)));
        when(accounts.getContact(studentId)).thenReturn(Optional.of(contact(studentId,Role.STUDENT,AccountStatus.PENDING)));
        assertThat(service.enroll(manager,classId,studentId)).isEqualTo(EnrollmentResult.ENROLLED);
        verify(enrollments).insert(any()); verify(events).publish(any());
        when(enrollments.find(classId,studentId)).thenReturn(Optional.of(Enrollment.create(classId,studentId,EnrollmentSource.MANUAL,now)));
        assertThat(service.enroll(manager,classId,studentId)).isEqualTo(EnrollmentResult.ALREADY_ENROLLED);
        verify(events,times(1)).publish(any());
    }
    @Test void batchLooksUpOnceAndReturnsAllNineResultTypes() {
        var restored = UUID.randomUUID(); var duplicate = UUID.randomUUID(); var other = UUID.randomUUID(); var wrong = UUID.randomUUID(); var disabled = UUID.randomUUID();
        when(accounts.findByEmails(any())).thenReturn(List.of(
            new AccountContact(studentId,"new@a.com","N",Role.STUDENT,AccountStatus.ACTIVE),new AccountContact(restored,"restore@a.com","N",Role.STUDENT,AccountStatus.PENDING),
            new AccountContact(duplicate,"has@a.com","N",Role.STUDENT,AccountStatus.ACTIVE),new AccountContact(other,"other@a.com","N",Role.STUDENT,AccountStatus.ACTIVE),
            new AccountContact(wrong,"teacher@a.com","N",Role.TEACHER,AccountStatus.ACTIVE),new AccountContact(disabled,"disabled@a.com","N",Role.STUDENT,AccountStatus.DISABLED)));
        when(enrollments.find(classId,restored)).thenReturn(Optional.of(Enrollment.create(classId,restored,EnrollmentSource.MANUAL,now.minusSeconds(10)).remove(now.minusSeconds(5))));
        when(enrollments.find(classId,duplicate)).thenReturn(Optional.of(Enrollment.create(classId,duplicate,EnrollmentSource.MANUAL,now)));
        when(enrollments.activeInOtherClass(other,subjectId,classId)).thenReturn(true);
        var rows = service.enrollBatch(manager,classId,"email\nnew@a.com\nrestore@a.com\nhas@a.com\nmissing@a.com\nteacher@a.com\ndisabled@a.com\nother@a.com\nbad\nnew@a.com");
        assertThat(rows).extracting(AcademicsDtos.RowResult::result).containsExactly(EnrollmentResult.ENROLLED,EnrollmentResult.RESTORED,EnrollmentResult.ALREADY_ENROLLED,EnrollmentResult.NOT_FOUND,EnrollmentResult.NOT_STUDENT,EnrollmentResult.DISABLED,EnrollmentResult.IN_OTHER_CLASS,EnrollmentResult.INVALID_EMAIL,EnrollmentResult.DUPLICATE_IN_LIST);
        assertThat(rows.get(0).line()).isEqualTo(2);
        verify(accounts,times(1)).findByEmails(any());
    }
    @Test void removedStudentImmediatelyLosesAccess() {
        var actor = new Actor(studentId,Role.STUDENT);
        when(accounts.getContact(studentId)).thenReturn(Optional.of(contact(studentId,Role.STUDENT,AccountStatus.ACTIVE)));
        when(classes.find(classId)).thenReturn(Optional.of(course(ClassStatus.OPEN,0)));
        when(enrollments.find(classId,studentId)).thenReturn(Optional.of(Enrollment.create(classId,studentId,EnrollmentSource.MANUAL,now).remove(now)));
        assertThatThrownBy(() -> service.detail(actor,"student",classId)).isInstanceOfSatisfying(ApiException.class,e -> assertThat(e.status().value()).isEqualTo(404));
    }
    @Test void viewPermissionDoesNotGrantWritePermission() {
        when(authorization.authorize(eq(manager),eq("CLASS_MANAGE"),any())).thenReturn(AuthorizationDecision.deny("readonly"));
        assertThatThrownBy(() -> service.updateInfo(manager,classId,"New",null,null,0)).isInstanceOfSatisfying(ApiException.class,e -> assertThat(e.status().value()).isEqualTo(404));
        verify(classes,never()).update(any(),anyLong());
        verify(audit).record(argThat(e -> e.result()==AuditResult.DENIED));
    }
    @Test void restoringUnknownStudentDoesNotReportSuccess() {
        assertThatThrownBy(() -> service.enrollmentStatus(manager,classId,studentId,EnrollmentStatus.ACTIVE))
            .isInstanceOfSatisfying(ApiException.class,e -> assertThat(e.status().value()).isEqualTo(400));
        verify(enrollments,never()).insert(any());
        verify(enrollments,never()).update(any(),any());
    }
    @Test void createStartsDraftAndAuditsTeacherAssignmentInTransaction() {
        doAnswer(invocation -> {
            AuditEvent event=invocation.getArgument(0);
            if(event.result()==AuditResult.SUCCESS) assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
            return null;
        }).when(audit).record(any());
        var result=service.create(manager,subjectId,"NEW","New class",null,null,teacherId);
        assertThat(result.status()).isEqualTo(ClassStatus.DRAFT);
        assertThat(result.version()).isZero();
        verify(classes).insert(argThat(c -> c.code().equals("NEW") && !c.showGradeDistribution()));
        verify(audit).record(argThat(e -> e.action().equals("CLASS_CREATED")));
        verify(audit).record(argThat(e -> e.action().equals("CLASS_TEACHER_CHANGED")));
    }
    @Test void inactiveTeacherCannotBeAssigned() {
        when(accounts.getContact(teacherId)).thenReturn(Optional.of(contact(teacherId,Role.TEACHER,AccountStatus.DISABLED)));
        assertThatThrownBy(() -> service.assignTeacher(manager,classId,teacherId,0)).isInstanceOfSatisfying(ApiException.class,e -> assertThat(e.code()).isEqualTo("INVALID_TEACHER"));
        verify(classes,never()).update(any(),anyLong());
    }
    @Test void teacherLosesReadAccessAfterReassignment() {
        Actor teacher=new Actor(teacherId,Role.TEACHER);
        when(classes.find(classId)).thenReturn(Optional.of(CourseClass.create(classId,subjectId,"C","Name",null,null,UUID.randomUUID(),now)));
        assertThatThrownBy(() -> service.detail(teacher,"teacher",classId)).isInstanceOfSatisfying(ApiException.class,e -> assertThat(e.status().value()).isEqualTo(404));
    }
    @Test void archivedClassCannotRemoveStudents() {
        when(classes.find(classId)).thenReturn(Optional.of(course(ClassStatus.ARCHIVED,0)));
        assertThatThrownBy(() -> service.enrollmentStatus(manager,classId,studentId,EnrollmentStatus.REMOVED)).isInstanceOfSatisfying(ApiException.class,e -> assertThat(e.code()).isEqualTo("CLASS_ARCHIVED"));
        verify(enrollments,never()).update(any(),any());
    }
    @Test void removedEnrollmentIsPreservedAndFailureAuditRunsAfterRollback() {
        when(enrollments.find(classId,studentId)).thenReturn(Optional.of(Enrollment.create(classId,studentId,EnrollmentSource.MANUAL,now.minusSeconds(10))));
        service.enrollmentStatus(manager,classId,studentId,EnrollmentStatus.REMOVED);
        verify(enrollments).update(argThat(e -> e.status()==EnrollmentStatus.REMOVED && e.enrolledAt().equals(now.minusSeconds(10))),eq(EnrollmentStatus.ACTIVE));
        doAnswer(invocation -> {
            AuditEvent event=invocation.getArgument(0);
            if(event.result()==AuditResult.FAILURE) assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            return null;
        }).when(audit).record(any());
        assertThatThrownBy(() -> service.updateInfo(manager,classId,"New",null,null,99)).isInstanceOf(ApiException.class);
        verify(audit).record(argThat(e -> e.result()==AuditResult.FAILURE));
    }
    @Test void reopeningDoesNotDuplicateOriginalActivationNotifications() {
        when(classes.find(classId)).thenReturn(Optional.of(course(ClassStatus.ARCHIVED,0)));
        service.changeStatus(manager,classId,ClassStatus.OPEN,0);
        verifyNoInteractions(events);
    }
    @Test void gradeDistributionIsVersionedAndCodeRemainsImmutable() {
        var result=service.gradeDistribution(manager,classId,true,0);
        assertThat(result.showGradeDistribution()).isTrue();
        assertThat(result.version()).isEqualTo(1);
        verify(classes).update(argThat(c -> c.subjectId().equals(subjectId) && c.code().equals("C")),eq(0L));
    }
    @Test void teacherCandidatesFilterWrongRoleAndDisabledAccounts() {
        when(accounts.findAccounts(anyString(),anySet(),eq(20))).thenReturn(List.of(contact(teacherId,Role.TEACHER,AccountStatus.ACTIVE),contact(studentId,Role.STUDENT,AccountStatus.ACTIVE),contact(UUID.randomUUID(),Role.SUBJECT_MANAGER,AccountStatus.DISABLED)));
        assertThat(service.teacherCandidates(manager,"Person")).extracting(AcademicsDtos.Person::id).containsExactly(teacherId);
    }
    @Test void batchInfrastructureFailureIsNotMisreportedAsStudentError() {
        when(accounts.findByEmails(any())).thenThrow(new IllegalStateException("upstream secret"));
        assertThatThrownBy(() -> service.enrollBatch(manager,classId,"one@a.com")).isInstanceOfSatisfying(ApiException.class,e -> assertThat(e.status().value()).isEqualTo(503));
        verify(enrollments,never()).insert(any());
    }
    @Test void pagingRejectsOverflowAndOversizedPage() {
        assertThatThrownBy(() -> service.list(manager,"manager",null,null,null,Integer.MAX_VALUE,100)).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.list(manager,"manager",null,null,null,1,101)).isInstanceOf(ApiException.class);
    }
}
