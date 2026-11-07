package edu.aiplatform.academics.service;

import edu.aiplatform.academics.dto.AcademicsDtos;

import edu.aiplatform.academics.model.*;
import edu.aiplatform.academics.repository.*;
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
    ICourseClassRepository courseClassRepository = mock(ICourseClassRepository.class);
    IEnrollmentRepository enrollmentRepository = mock(IEnrollmentRepository.class);
    IAcademicsReadRepository academicsReadRepository = mock(IAcademicsReadRepository.class);
    ISubjectMutationGuard subjectMutationGuard = mock(ISubjectMutationGuard.class);
    IAccountLookup accountLookup = mock(IAccountLookup.class);
    ISubjectQuery subjectQuery = mock(ISubjectQuery.class);
    ISubjectScope scope = mock(ISubjectScope.class);
    IAuthorization authorization = mock(IAuthorization.class);
    IAudit audit = mock(IAudit.class);
    IEventPublisher eventPublisher = mock(IEventPublisher.class);
    UUID managerId = UUID.randomUUID(), subjectId = UUID.randomUUID(), classId = UUID.randomUUID(), studentId = UUID.randomUUID(), teacherId = UUID.randomUUID();
    Instant now = Instant.parse("2026-10-18T00:00:00Z");
    Actor manager = new Actor(managerId, Role.SUBJECT_MANAGER);
    AcademicsService academicsService;
    @BeforeEach void setup() {
        PlatformTransactionManager tx = new AbstractPlatformTransactionManager() {
            protected Object doGetTransaction() { return new Object(); }
            protected void doBegin(Object tx, TransactionDefinition def) { }
            protected void doCommit(DefaultTransactionStatus status) { }
            protected void doRollback(DefaultTransactionStatus status) { }
        };
        academicsService = new AcademicsService(courseClassRepository,enrollmentRepository,academicsReadRepository,subjectMutationGuard,accountLookup,subjectQuery,scope,authorization,audit,eventPublisher,tx,Clock.fixed(now,ZoneOffset.UTC));
        when(accountLookup.getContact(managerId)).thenReturn(Optional.of(contact(managerId,Role.SUBJECT_MANAGER,AccountStatus.ACTIVE)));
        when(accountLookup.getContact(teacherId)).thenReturn(Optional.of(contact(teacherId,Role.TEACHER,AccountStatus.ACTIVE)));
        when(accountLookup.lockContact(teacherId)).thenReturn(Optional.of(contact(teacherId,Role.TEACHER,AccountStatus.ACTIVE)));
        when(scope.isSubjectManager(managerId,subjectId)).thenReturn(true);
        when(authorization.authorize(any(),anyString(),any())).thenReturn(AuthorizationDecision.allow());
        when(subjectQuery.getSubject(subjectId)).thenReturn(Optional.of(new SubjectRef(subjectId,"S","Subject",SubjectStatus.ACTIVE,managerId)));
        when(subjectQuery.isActive(subjectId)).thenReturn(true);
        when(courseClassRepository.find(classId)).thenReturn(Optional.of(course(ClassStatus.DRAFT,0)));
    }
    AccountContact contact(UUID id, Role role, AccountStatus status) { return new AccountContact(id,id+"@example.com","Person",role,status); }
    CourseClass course(ClassStatus status,long version) { return new CourseClass(classId,subjectId,"C","Class",null,null,status,teacherId,false,now,now,version); }
    @Test void studentCannotReadDraftClassAndDenialIsAudited() {
        var actor = new Actor(studentId,Role.STUDENT);
        when(accountLookup.getContact(studentId)).thenReturn(Optional.of(contact(studentId,Role.STUDENT,AccountStatus.ACTIVE)));
        when(enrollmentRepository.find(classId,studentId)).thenReturn(Optional.of(Enrollment.createEnrollment(classId,studentId,EnrollmentSource.MANUAL,now)));
        assertThatThrownBy(() -> academicsService.loadClass(actor,"student",classId)).isInstanceOfSatisfying(ApiException.class,e -> assertThat(e.status().value()).isEqualTo(404));
        verify(audit).record(argThat(e -> e.result()==AuditResult.DENIED));
    }
    @Test void createLocksTheSubjectRowThroughU04AndArchivedSubjectBlocksTheClass() {
        doThrow(ApiException.conflict("SUBJECT_ARCHIVED","Môn đã lưu trữ")).when(subjectMutationGuard).lockActive(eq(subjectId),any());
        assertThatThrownBy(() -> academicsService.createClass(manager,subjectId,"NEW","New class",null,null,null))
                .isInstanceOfSatisfying(ApiException.class,e -> assertThat(e.code()).isEqualTo("SUBJECT_ARCHIVED"));
        verify(subjectMutationGuard).lockActive(eq(subjectId),eq(java.time.Duration.ofSeconds(1)));
        verify(courseClassRepository,never()).insert(any());
    }
    @Test void adminCannotListClasses() {
        Actor admin = new Actor(UUID.randomUUID(),Role.ADMIN);
        assertThatThrownBy(() -> academicsService.listClasses(admin,"manager",null,null,null,1,20)).isInstanceOfSatisfying(ApiException.class,e -> assertThat(e.status().value()).isEqualTo(403));
        verifyNoInteractions(academicsReadRepository);
    }
    @Test void unauthorizedCallerWithInvalidPagingIsRejectedWithForbiddenAndDeniedAudit() {
        Actor student = new Actor(studentId, Role.STUDENT);
        assertThatThrownBy(() -> academicsService.listClasses(student, "manager", null, null, null, 0, 20))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.status().value()).isEqualTo(403));
        verify(audit).record(argThat(e -> e.action().equals("ACCESS_DENIED") && e.result() == AuditResult.DENIED));

        assertThatThrownBy(() -> academicsService.listStudents(student, "manager", classId, 0, 20))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.status().value()).isEqualTo(403));
        verify(audit, times(2)).record(argThat(e -> e.action().equals("ACCESS_DENIED") && e.result() == AuditResult.DENIED));
    }
    @Test void staleVersionDoesNotWrite() {
        assertThatThrownBy(() -> academicsService.updateClassInfo(manager,classId,"New",null,null,2)).isInstanceOfSatisfying(ApiException.class,e -> assertThat(e.status().value()).isEqualTo(409));
        verify(courseClassRepository,never()).update(any(),anyLong());
    }
    @Test void updateLocksSubjectBeforeRereadingAndPreservesCode() {
        var result = academicsService.updateClassInfo(manager,classId,"New",null,"2026",0);
        assertThat(result.name()).isEqualTo("New");
        assertThat(result.description()).isEmpty();
        assertThat(result.code()).isEqualTo("C");
        var order = inOrder(courseClassRepository,subjectMutationGuard);
        order.verify(courseClassRepository).find(classId); order.verify(subjectMutationGuard).lockActive(org.mockito.ArgumentMatchers.eq(subjectId),org.mockito.ArgumentMatchers.any()); order.verify(courseClassRepository).find(classId);
        verify(audit).record(argThat(e -> e.action().equals("CLASS_UPDATED") && e.result()==AuditResult.SUCCESS));
    }
    @Test void dependencyFailureIsUnavailableAndWritesNothing() {
        when(scope.isSubjectManager(managerId,subjectId)).thenThrow(new IllegalStateException("internal secret"));
        assertThatThrownBy(() -> academicsService.updateClassInfo(manager,classId,"New",null,null,0)).isInstanceOfSatisfying(ApiException.class,e -> {assertThat(e.status().value()).isEqualTo(503);assertThat(e.getMessage()).doesNotContain("secret");});
        verify(courseClassRepository,never()).update(any(),anyLong());
    }
    @Test void reopenReturnsConflictingStudents() {
        when(courseClassRepository.find(classId)).thenReturn(Optional.of(course(ClassStatus.ARCHIVED,0)));
        when(enrollmentRepository.conflictingStudents(classId,subjectId)).thenReturn(List.of(studentId));
        var studentName = "Nguyen Van A";
        when(accountLookup.getContact(studentId)).thenReturn(Optional.of(
                new AccountContact(studentId,"student@example.com",studentName,Role.STUDENT,AccountStatus.ACTIVE)));
        assertThatThrownBy(() -> academicsService.changeClassStatus(manager,classId,ClassStatus.OPEN,0)).isInstanceOfSatisfying(AcademicsProblem.class,e -> assertThat(e.properties().get("conflictingStudents")).isEqualTo(List.of(studentName)));
        verify(courseClassRepository,never()).update(any(),anyLong());
    }
    @Test void openDraftPublishesActivationForAllActiveStudents() {
        when(enrollmentRepository.listByClass(classId,true,100,0)).thenReturn(List.of(Enrollment.createEnrollment(classId,studentId,EnrollmentSource.MANUAL,now)));
        academicsService.changeClassStatus(manager,classId,ClassStatus.OPEN,0);
        verify(eventPublisher).publish(argThat(e -> e.eventType().equals("enrollment.activated") && e.payload().get("accountId").equals(studentId.toString())));
    }
    @Test void outOfScopeManualAndRestoreRequestsNeverLockTheTargetAccount() {
        when(scope.isSubjectManager(managerId,subjectId)).thenReturn(false);
        assertThatThrownBy(() -> academicsService.enrollStudent(manager,classId,studentId)).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> academicsService.changeEnrollmentStatus(manager,classId,studentId,EnrollmentStatus.ACTIVE)).isInstanceOf(ApiException.class);
        verify(accountLookup,never()).lockContact(studentId);
        verifyNoInteractions(subjectMutationGuard);
        verify(enrollmentRepository,never()).insert(any());
    }
    @Test void manualEnrollmentLocksTheTargetBeforeTheSubject() {
        when(accountLookup.lockContact(studentId)).thenReturn(Optional.of(contact(studentId,Role.STUDENT,AccountStatus.ACTIVE)));
        assertThat(academicsService.enrollStudent(manager,classId,studentId)).isEqualTo(EnrollmentResult.ENROLLED);
        var order=inOrder(accountLookup,subjectMutationGuard,enrollmentRepository);
        order.verify(accountLookup).lockContact(studentId);
        order.verify(subjectMutationGuard).lockActive(eq(subjectId),any());
        order.verify(enrollmentRepository).insert(any());
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({"STUDENT,DISABLED,DISABLED","TEACHER,ACTIVE,NOT_STUDENT"})
    void manualAndRestoreUseLockedEligibilityRatherThanAnOlderRead(Role role,AccountStatus status,EnrollmentResult expected) {
        when(accountLookup.getContact(studentId)).thenReturn(Optional.of(contact(studentId,Role.STUDENT,AccountStatus.ACTIVE)));
        when(accountLookup.lockContact(studentId)).thenReturn(Optional.of(contact(studentId,role,status)));
        assertThat(academicsService.enrollStudent(manager,classId,studentId)).isEqualTo(expected);
        assertThatThrownBy(() -> academicsService.changeEnrollmentStatus(manager,classId,studentId,EnrollmentStatus.ACTIVE))
                .isInstanceOfSatisfying(ApiException.class,e -> assertThat(e.code()).isEqualTo(expected.name()));
        verify(enrollmentRepository,never()).insert(any());
        verify(enrollmentRepository,never()).update(any(),any());
        verify(accountLookup,never()).getContact(studentId);
    }
    @Test void restoringEnrollmentLocksTheTargetBeforeTheSubject() {
        when(accountLookup.lockContact(studentId)).thenReturn(Optional.of(contact(studentId,Role.STUDENT,AccountStatus.PENDING)));
        when(enrollmentRepository.find(classId,studentId)).thenReturn(Optional.of(Enrollment.createEnrollment(classId,studentId,EnrollmentSource.MANUAL,now.minusSeconds(2)).removeEnrollment(now.minusSeconds(1))));
        assertThat(academicsService.changeEnrollmentStatus(manager,classId,studentId,EnrollmentStatus.ACTIVE)).isEqualTo(EnrollmentResult.RESTORED);
        var order=inOrder(accountLookup,subjectMutationGuard,enrollmentRepository);
        order.verify(accountLookup).lockContact(studentId);
        order.verify(subjectMutationGuard).lockActive(eq(subjectId),any());
        order.verify(enrollmentRepository).update(any(),eq(EnrollmentStatus.REMOVED));
    }
    @Test void pendingStudentCanEnrollAndDuplicateDoesNotRepublish() {
        when(courseClassRepository.find(classId)).thenReturn(Optional.of(course(ClassStatus.OPEN,0)));
        when(accountLookup.lockContact(studentId)).thenReturn(Optional.of(contact(studentId,Role.STUDENT,AccountStatus.PENDING)));
        assertThat(academicsService.enrollStudent(manager,classId,studentId)).isEqualTo(EnrollmentResult.ENROLLED);
        verify(enrollmentRepository).insert(any()); verify(eventPublisher).publish(any());
        when(enrollmentRepository.find(classId,studentId)).thenReturn(Optional.of(Enrollment.createEnrollment(classId,studentId,EnrollmentSource.MANUAL,now)));
        assertThat(academicsService.enrollStudent(manager,classId,studentId)).isEqualTo(EnrollmentResult.ALREADY_ENROLLED);
        verify(eventPublisher,times(1)).publish(any());
    }
    @Test void batchLooksUpOnceAndReturnsAllNineResultTypes() {
        var restored = UUID.randomUUID(); var duplicate = UUID.randomUUID(); var other = UUID.randomUUID(); var wrong = UUID.randomUUID(); var disabled = UUID.randomUUID();
        var found=List.of(
            new AccountContact(studentId,"new@a.com","N",Role.STUDENT,AccountStatus.ACTIVE),new AccountContact(restored,"restore@a.com","N",Role.STUDENT,AccountStatus.PENDING),
            new AccountContact(duplicate,"has@a.com","N",Role.STUDENT,AccountStatus.ACTIVE),new AccountContact(other,"other@a.com","N",Role.STUDENT,AccountStatus.ACTIVE),
            new AccountContact(wrong,"teacher@a.com","N",Role.TEACHER,AccountStatus.ACTIVE),new AccountContact(disabled,"disabled@a.com","N",Role.STUDENT,AccountStatus.DISABLED));
        when(accountLookup.findByEmails(any())).thenReturn(found);
        when(accountLookup.lockContact(any())).thenAnswer(call->found.stream()
                .filter(account->account.accountId().equals(call.getArgument(0))).findFirst());
        when(enrollmentRepository.find(classId,restored)).thenReturn(Optional.of(Enrollment.createEnrollment(classId,restored,EnrollmentSource.MANUAL,now.minusSeconds(10)).removeEnrollment(now.minusSeconds(5))));
        when(enrollmentRepository.find(classId,duplicate)).thenReturn(Optional.of(Enrollment.createEnrollment(classId,duplicate,EnrollmentSource.MANUAL,now)));
        when(enrollmentRepository.activeInOtherClass(other,subjectId,classId)).thenReturn(true);
        var rows = academicsService.enrollStudents(manager,classId,"email\nnew@a.com\nrestore@a.com\nhas@a.com\nmissing@a.com\nteacher@a.com\ndisabled@a.com\nother@a.com\nbad\nnew@a.com");
        assertThat(rows).extracting(AcademicsDtos.RowResult::result).containsExactly(EnrollmentResult.ENROLLED,EnrollmentResult.RESTORED,EnrollmentResult.ALREADY_ENROLLED,EnrollmentResult.NOT_FOUND,EnrollmentResult.NOT_STUDENT,EnrollmentResult.DISABLED,EnrollmentResult.IN_OTHER_CLASS,EnrollmentResult.INVALID_EMAIL,EnrollmentResult.DUPLICATE_IN_LIST);
        assertThat(rows.get(0).line()).isEqualTo(2);
        verify(accountLookup,times(1)).findByEmails(any());
    }
    @Test void removedStudentImmediatelyLosesAccess() {
        var actor = new Actor(studentId,Role.STUDENT);
        when(accountLookup.getContact(studentId)).thenReturn(Optional.of(contact(studentId,Role.STUDENT,AccountStatus.ACTIVE)));
        when(courseClassRepository.find(classId)).thenReturn(Optional.of(course(ClassStatus.OPEN,0)));
        when(enrollmentRepository.find(classId,studentId)).thenReturn(Optional.of(Enrollment.createEnrollment(classId,studentId,EnrollmentSource.MANUAL,now).removeEnrollment(now)));
        assertThatThrownBy(() -> academicsService.loadClass(actor,"student",classId)).isInstanceOfSatisfying(ApiException.class,e -> assertThat(e.status().value()).isEqualTo(404));
    }
    @Test void viewPermissionDoesNotGrantWritePermission() {
        when(authorization.authorize(eq(manager),eq("CLASS_MANAGE"),any())).thenReturn(AuthorizationDecision.deny("readonly"));
        assertThatThrownBy(() -> academicsService.updateClassInfo(manager,classId,"New",null,null,0)).isInstanceOfSatisfying(ApiException.class,e -> assertThat(e.status().value()).isEqualTo(404));
        verify(courseClassRepository,never()).update(any(),anyLong());
        verify(audit).record(argThat(e -> e.result()==AuditResult.DENIED));
    }
    @Test void restoringUnknownStudentDoesNotReportSuccess() {
        assertThatThrownBy(() -> academicsService.changeEnrollmentStatus(manager,classId,studentId,EnrollmentStatus.ACTIVE))
            .isInstanceOfSatisfying(ApiException.class,e -> assertThat(e.status().value()).isEqualTo(400));
        verify(enrollmentRepository,never()).insert(any());
        verify(enrollmentRepository,never()).update(any(),any());
    }
    @Test void createStartsDraftAndAuditsTeacherAssignmentInTransaction() {
        doAnswer(invocation -> {
            AuditEvent event=invocation.getArgument(0);
            if(event.result()==AuditResult.SUCCESS) assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
            return null;
        }).when(audit).record(any());
        var result=academicsService.createClass(manager,subjectId,"NEW","New class",null,null,teacherId);
        assertThat(result.status()).isEqualTo(ClassStatus.DRAFT);
        assertThat(result.version()).isZero();
        verify(courseClassRepository).insert(argThat(c -> c.code().equals("NEW") && !c.showGradeDistribution()));
        verify(audit).record(argThat(e -> e.action().equals("CLASS_CREATED")));
        verify(audit).record(argThat(e -> e.action().equals("CLASS_TEACHER_CHANGED")));
    }
    @Test void inactiveTeacherCannotBeAssigned() {
        when(accountLookup.lockContact(teacherId)).thenReturn(Optional.of(contact(teacherId,Role.TEACHER,AccountStatus.DISABLED)));
        assertThatThrownBy(() -> academicsService.assignClassTeacher(manager,classId,teacherId,0)).isInstanceOfSatisfying(ApiException.class,e -> assertThat(e.code()).isEqualTo("INVALID_TEACHER"));
        verify(courseClassRepository,never()).update(any(),anyLong());
    }
    @Test void teacherLosesReadAccessAfterReassignment() {
        Actor teacher=new Actor(teacherId,Role.TEACHER);
        when(courseClassRepository.find(classId)).thenReturn(Optional.of(CourseClass.createClass(classId,subjectId,"C","Name",null,null,UUID.randomUUID(),now)));
        assertThatThrownBy(() -> academicsService.loadClass(teacher,"teacher",classId)).isInstanceOfSatisfying(ApiException.class,e -> assertThat(e.status().value()).isEqualTo(404));
    }
    @Test void archivedClassCannotRemoveStudents() {
        when(courseClassRepository.find(classId)).thenReturn(Optional.of(course(ClassStatus.ARCHIVED,0)));
        assertThatThrownBy(() -> academicsService.changeEnrollmentStatus(manager,classId,studentId,EnrollmentStatus.REMOVED)).isInstanceOfSatisfying(ApiException.class,e -> assertThat(e.code()).isEqualTo("CLASS_ARCHIVED"));
        verify(enrollmentRepository,never()).update(any(),any());
    }
    @Test void removedEnrollmentIsPreservedAndFailureAuditRunsAfterRollback() {
        when(enrollmentRepository.find(classId,studentId)).thenReturn(Optional.of(Enrollment.createEnrollment(classId,studentId,EnrollmentSource.MANUAL,now.minusSeconds(10))));
        academicsService.changeEnrollmentStatus(manager,classId,studentId,EnrollmentStatus.REMOVED);
        verify(enrollmentRepository).update(argThat(e -> e.status()==EnrollmentStatus.REMOVED && e.enrolledAt().equals(now.minusSeconds(10))),eq(EnrollmentStatus.ACTIVE));
        doAnswer(invocation -> {
            AuditEvent event=invocation.getArgument(0);
            if(event.result()==AuditResult.FAILURE) assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            return null;
        }).when(audit).record(any());
        assertThatThrownBy(() -> academicsService.updateClassInfo(manager,classId,"New",null,null,99)).isInstanceOf(ApiException.class);
        verify(audit).record(argThat(e -> e.result()==AuditResult.FAILURE));
    }
    @Test void reopeningDoesNotDuplicateOriginalActivationNotifications() {
        when(courseClassRepository.find(classId)).thenReturn(Optional.of(course(ClassStatus.ARCHIVED,0)));
        academicsService.changeClassStatus(manager,classId,ClassStatus.OPEN,0);
        verifyNoInteractions(eventPublisher);
    }
    @Test void teacherCandidatesFilterWrongRoleAndDisabledAccounts() {
        when(accountLookup.findAccounts(anyString(),anySet(),eq(20))).thenReturn(List.of(contact(teacherId,Role.TEACHER,AccountStatus.ACTIVE),contact(studentId,Role.STUDENT,AccountStatus.ACTIVE),contact(UUID.randomUUID(),Role.SUBJECT_MANAGER,AccountStatus.DISABLED)));
        assertThat(academicsService.listTeacherCandidates(manager,"Person")).extracting(AcademicsDtos.Person::id).containsExactly(teacherId);
    }
    @Test void batchInfrastructureFailureIsNotMisreportedAsStudentError() {
        when(accountLookup.findByEmails(any())).thenThrow(new IllegalStateException("upstream secret"));
        assertThatThrownBy(() -> academicsService.enrollStudents(manager,classId,"one@a.com")).isInstanceOfSatisfying(ApiException.class,e -> assertThat(e.status().value()).isEqualTo(503));
        verify(enrollmentRepository,never()).insert(any());
    }
    @Test
    void updatingClassPreservesVersionAndAuditsInsideTheTransaction() {
        doAnswer(invocation -> {
            AuditEvent event = invocation.getArgument(0);
            if (event.result() == AuditResult.SUCCESS) {
                assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
            }
            return null;
        }).when(audit).record(any());

        var result = academicsService.updateClassInfo(manager, classId, "Updated", "Description",
                "Term", 0);

        assertThat(result.name()).isEqualTo("Updated");
        assertThat(result.version()).isEqualTo(1);
        var order = inOrder(subjectMutationGuard, courseClassRepository, audit);
        order.verify(subjectMutationGuard).lockActive(subjectId, Duration.ofSeconds(1));
        order.verify(courseClassRepository).update(argThat(value -> value.name().equals("Updated")
                && value.description().equals("Description") && value.term().equals("Term")
                && value.version() == 1), eq(0L));
        order.verify(audit).record(argThat(event -> event.action().equals("CLASS_UPDATED")
                && event.result() == AuditResult.SUCCESS && event.details().isEmpty()));
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void staleVersionIsRejectedBeforeInvalidClassInformation() {
        assertThatThrownBy(() -> academicsService.updateClassInfo(manager, classId, "", null,
                null, 99)).isInstanceOfSatisfying(ApiException.class,
                        error -> assertThat(error.code()).isEqualTo("CLASS_VERSION_CONFLICT"));

        verify(courseClassRepository, never()).update(any(), anyLong());
        verify(audit, never()).record(argThat(event -> event.result() == AuditResult.SUCCESS));
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void invalidClassInformationDoesNotWriteOrAuditSuccess() {
        assertThatThrownBy(() -> academicsService.updateClassInfo(manager, classId, "", null,
                null, 0)).isInstanceOfSatisfying(ApiException.class,
                        error -> assertThat(error.code()).isEqualTo("INVALID_CLASS_FIELD"));

        verify(subjectMutationGuard).lockActive(subjectId, Duration.ofSeconds(1));
        verify(courseClassRepository, never()).update(any(), anyLong());
        verify(audit, never()).record(argThat(event -> event.result() == AuditResult.SUCCESS));
    }

    @Test void pagingRejectsOverflowAndOversizedPage() {
        assertThatThrownBy(() -> academicsService.listClasses(manager,"manager",null,null,null,Integer.MAX_VALUE,100)).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> academicsService.listClasses(manager,"manager",null,null,null,1,101)).isInstanceOf(ApiException.class);
    }
}
