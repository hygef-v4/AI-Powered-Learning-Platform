package edu.aiplatform.academics.service;

import edu.aiplatform.academics.dto.AcademicsDtos;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import edu.aiplatform.academics.contract.ClassStatus;
import edu.aiplatform.academics.model.*;
import edu.aiplatform.academics.repository.*;
import edu.aiplatform.audit.contract.*;
import edu.aiplatform.identity.contract.*;
import edu.aiplatform.jobs.contract.IEventPublisher;
import edu.aiplatform.shared.error.ApiException;
import edu.aiplatform.subjects.contract.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.*;

/** Observes service transaction orchestration; does not simulate PostgreSQL durability or locking. */
class AcademicsBatchTransactionTest {
    final ICourseClassRepository courseClassRepository=mock(ICourseClassRepository.class);
    final IEnrollmentRepository enrollmentRepository=mock(IEnrollmentRepository.class);
    final IAcademicsReadRepository academicsReadRepository=mock(IAcademicsReadRepository.class);
    final ISubjectMutationGuard subjectMutationGuard=mock(ISubjectMutationGuard.class);
    final IAccountLookup accountLookup=mock(IAccountLookup.class);
    final ISubjectQuery subjectQuery=mock(ISubjectQuery.class);
    final ISubjectScope scope=mock(ISubjectScope.class);
    final IAuthorization authorization=mock(IAuthorization.class);
    final IAudit audit=mock(IAudit.class);
    final IEventPublisher eventPublisher=mock(IEventPublisher.class);
    final RecordingTransactions recordingTransactions=new RecordingTransactions();
    final UUID managerId=UUID.randomUUID(),subjectId=UUID.randomUUID(),classId=UUID.randomUUID();
    final UUID first=UUID.randomUUID(),second=UUID.randomUUID(),third=UUID.randomUUID();
    final Instant now=Instant.parse("2026-10-18T00:00:00Z");
    final Actor manager=new Actor(managerId,Role.SUBJECT_MANAGER);
    final String input="first@example.test\nsecond@example.test\nthird@example.test";
    AcademicsService academicsService;

    @BeforeEach void setup() {
        academicsService=new AcademicsService(courseClassRepository,enrollmentRepository,academicsReadRepository,subjectMutationGuard,accountLookup,subjectQuery,scope,authorization,audit,eventPublisher,recordingTransactions,Clock.fixed(now,ZoneOffset.UTC));
        when(accountLookup.getContact(managerId)).thenReturn(Optional.of(contact(managerId,"manager@example.test",Role.SUBJECT_MANAGER)));
        when(authorization.authorize(any(),anyString(),any())).thenReturn(AuthorizationDecision.allow());
        when(scope.isSubjectManager(managerId,subjectId)).thenReturn(true);
        when(subjectQuery.getSubject(subjectId)).thenReturn(Optional.of(new SubjectRef(subjectId,"S","Subject",SubjectStatus.ACTIVE,managerId)));
        when(courseClassRepository.find(classId)).thenReturn(Optional.of(new CourseClass(classId,subjectId,"C","Class","",ClassStatus.OPEN,managerId,false,now,now,0)));
        var students=List.of(contact(first,"first@example.test",Role.STUDENT),contact(second,"second@example.test",Role.STUDENT),contact(third,"third@example.test",Role.STUDENT));
        when(accountLookup.findByEmails(any())).thenReturn(students);
        when(accountLookup.lockContact(any())).thenAnswer(call->students.stream()
                .filter(student->student.accountId().equals(call.getArgument(0))).findFirst());
        when(enrollmentRepository.find(eq(classId),any())).thenAnswer(call->{
            UUID account=call.getArgument(1);
            return recordingTransactions.committed.contains(account)?Optional.of(Enrollment.createEnrollment(classId,account,EnrollmentSource.LIST,now)):Optional.empty();
        });
        doAnswer(call->{recordingTransactions.stage(((Enrollment)call.getArgument(0)).accountId());return null;}).when(enrollmentRepository).insert(any());
    }
    @AfterEach void noLeakedTransaction() {
        assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
        assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
    }
    AccountContact contact(UUID id,String email,Role role) {return new AccountContact(id,email,"Person",role,AccountStatus.ACTIVE);}

    @Test void laterStorageFailureRollsBackOnlyThatLineAndStopsBeforeFollowingLine() {
        doAnswer(call->{
            UUID account=((Enrollment)call.getArgument(0)).accountId();recordingTransactions.stage(account);
            if(account.equals(second))throw new DataAccessResourceFailureException("password=must-not-leak");
            return null;
        }).when(enrollmentRepository).insert(any());
        doAnswer(call->{
            AuditEvent event=call.getArgument(0);
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isEqualTo(event.result()==AuditResult.SUCCESS);
            return null;
        }).when(audit).record(any());
        assertThatThrownBy(()->academicsService.enrollStudents(manager,classId,input)).isInstanceOfSatisfying(ApiException.class,error->{
            assertThat(error.status().value()).isEqualTo(503);assertThat(error.getMessage()).doesNotContain("password");
        });
        assertThat(recordingTransactions.committed).containsExactly(first);
        assertThat(recordingTransactions.commits).isEqualTo(1);assertThat(recordingTransactions.rollbacks).isEqualTo(1);
        assertThat(recordingTransactions.propagations).containsExactly(TransactionDefinition.PROPAGATION_REQUIRES_NEW,TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        verify(enrollmentRepository,never()).insert(argThat(value->value.accountId().equals(third)));
        verify(eventPublisher).publish(argThat(event->event.payload().get("accountId").equals(first.toString())));
        verify(eventPublisher,times(1)).publish(any());
        verify(audit).record(argThat(event->event.result()==AuditResult.FAILURE));
    }

    @Test void retryAfterPartialSuccessDoesNotRepublishOrRewriteAlreadyEnrolledStudent() {
        var failure=new HashSet<>(Set.of(second));
        doAnswer(call->{UUID account=((Enrollment)call.getArgument(0)).accountId();recordingTransactions.stage(account);
            if(failure.remove(account))throw new DataAccessResourceFailureException("temporary");return null;
        }).when(enrollmentRepository).insert(any());
        assertThatThrownBy(()->academicsService.enrollStudents(manager,classId,input)).isInstanceOf(ApiException.class);
        assertThat(academicsService.enrollStudents(manager,classId,input)).extracting(AcademicsDtos.RowResult::result)
                .containsExactly(EnrollmentResult.ALREADY_ENROLLED,EnrollmentResult.ENROLLED,EnrollmentResult.ENROLLED);
        assertThat(recordingTransactions.committed).containsExactly(first,second,third);
        verify(enrollmentRepository,times(1)).insert(argThat(value->value.accountId().equals(first)));
        verify(eventPublisher,times(1)).publish(argThat(event->event.payload().get("accountId").equals(first.toString())));
        verify(accountLookup,times(2)).findByEmails(any());
        assertThat(recordingTransactions.commits).isEqualTo(4);assertThat(recordingTransactions.rollbacks).isEqualTo(1);
    }

    @Test void invalidAndDuplicateRowsDoNotOpenTransactionsButLookupMissIsAnIsolatedOutcome() {
        var rows=academicsService.enrollStudents(manager,classId,"first@example.test\nbad\nfirst@example.test\nmissing@example.test\nthird@example.test");
        assertThat(rows).extracting(AcademicsDtos.RowResult::result).containsExactly(EnrollmentResult.ENROLLED,EnrollmentResult.INVALID_IDENTIFIER,
                EnrollmentResult.DUPLICATE_IN_LIST,EnrollmentResult.NOT_FOUND,EnrollmentResult.ENROLLED);
        assertThat(rows).extracting(AcademicsDtos.RowResult::line).containsExactly(1,2,3,4,5);
        assertThat(recordingTransactions.propagations).hasSize(3).containsOnly(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        assertThat(recordingTransactions.commits).isEqualTo(3);assertThat(recordingTransactions.rollbacks).isZero();
        assertThat(recordingTransactions.committed).containsExactly(first,third);
        verify(accountLookup).findByEmails(List.of("first@example.test","missing@example.test","third@example.test"));
    }

    @Test void successAuditFailureRollsBackThePendingEnrollmentAndSuppressesActivation() {
        doAnswer(call->{AuditEvent event=call.getArgument(0);if(event.result()==AuditResult.SUCCESS)throw new IllegalStateException("audit down");return null;}).when(audit).record(any());
        assertThatThrownBy(()->academicsService.enrollStudents(manager,classId,"first@example.test")).isInstanceOfSatisfying(ApiException.class,error->assertThat(error.status().value()).isEqualTo(503));
        assertThat(recordingTransactions.committed).isEmpty();assertThat(recordingTransactions.rollbacks).isEqualTo(1);assertThat(recordingTransactions.commits).isZero();
        verifyNoInteractions(eventPublisher);
        verify(audit).record(argThat(event->event.result()==AuditResult.FAILURE));
    }

    @Test void authorityRevokedBetweenRowsKeepsPriorCommitButRejectsTheRemainingBatch() {
        // Quyền bị thu hồi ngay sau khi dòng đầu commit.
        when(authorization.authorize(any(),anyString(),any())).thenAnswer(call->
                call.<String>getArgument(1).equals("CLASS_MANAGE")&&!recordingTransactions.committed.isEmpty()?AuthorizationDecision.deny("revoked"):AuthorizationDecision.allow());
        assertThatThrownBy(()->academicsService.enrollStudents(manager,classId,input)).isInstanceOfSatisfying(ApiException.class,error->assertThat(error.status().value()).isEqualTo(404));
        assertThat(recordingTransactions.committed).containsExactly(first);assertThat(recordingTransactions.commits).isEqualTo(1);assertThat(recordingTransactions.rollbacks).isEqualTo(1);
        verify(enrollmentRepository,never()).insert(argThat(value->!value.accountId().equals(first)));
        verify(audit).record(argThat(event->event.action().equals("ACCESS_DENIED")&&event.result()==AuditResult.DENIED));
    }

    @ParameterizedTest
    @CsvSource({"STUDENT,DISABLED,DISABLED", "TEACHER,ACTIVE,NOT_STUDENT"})
    void studentEligibilityIsRevalidatedInsideEachRowAfterEarlierCommit(
            Role currentRole, AccountStatus currentStatus, EnrollmentResult expected) {
        // Bulk resolution sees all three as eligible. The second account changes after row one commits.
        when(accountLookup.lockContact(any())).thenAnswer(call -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
            UUID id = call.getArgument(0);
            if (id.equals(second)) {
                assertThat(recordingTransactions.committed).contains(first);
                return Optional.of(new AccountContact(second, "second@example.test", "Person", currentRole, currentStatus));
            }
            return Optional.of(contact(id, id.equals(first) ? "first@example.test" : "third@example.test", Role.STUDENT));
        });

        var rows = academicsService.enrollStudents(manager, classId, input);

        assertThat(rows).extracting(AcademicsDtos.RowResult::result)
                .containsExactly(EnrollmentResult.ENROLLED, expected, EnrollmentResult.ENROLLED);
        assertThat(recordingTransactions.committed).containsExactly(first, third);
        verify(accountLookup).findByEmails(List.of("first@example.test", "second@example.test", "third@example.test"));
        verify(enrollmentRepository, never()).insert(argThat(value -> value.accountId().equals(second)));
        verify(eventPublisher, never()).publish(argThat(event -> event.payload().get("accountId").equals(second.toString())));
        verify(audit, never()).record(argThat(event -> event.result() == AuditResult.SUCCESS
                && second.toString().equals(event.details().get("accountId"))));
        var order = inOrder(accountLookup, subjectMutationGuard, enrollmentRepository);
        order.verify(accountLookup).lockContact(first);
        order.verify(subjectMutationGuard).lockActive(eq(subjectId), any());
        order.verify(enrollmentRepository).insert(argThat(value -> value.accountId().equals(first)));
        order.verify(accountLookup).lockContact(second);
        order.verify(subjectMutationGuard).lockActive(eq(subjectId), any());
        order.verify(accountLookup).lockContact(third);
        order.verify(subjectMutationGuard).lockActive(eq(subjectId), any());
        assertThat(recordingTransactions.commits).isEqualTo(3);
        assertThat(recordingTransactions.rollbacks).isZero();
    }

    @Test void rowCommitIsIndependentOfAnAlreadyActiveCallerTransaction() {
        var caller=new TransactionTemplate(recordingTransactions);
        assertThatThrownBy(()->caller.execute(status->{
            academicsService.enrollStudents(manager,classId,"first@example.test");
            throw new IllegalStateException("caller rolls back afterward");
        })).isInstanceOf(IllegalStateException.class).hasMessage("caller rolls back afterward");
        assertThat(recordingTransactions.committed).containsExactly(first);
        assertThat(recordingTransactions.commits).isEqualTo(1);assertThat(recordingTransactions.rollbacks).isEqualTo(1);
        assertThat(recordingTransactions.propagations).containsExactly(TransactionDefinition.PROPAGATION_REQUIRED,TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    private static final class RecordingTransactions extends AbstractPlatformTransactionManager {
        private static final class Pending {final List<UUID>writes=new ArrayList<>();boolean active=true;}
        private static final class Context {Pending pending;Context(Pending pending){this.pending=pending;}}
        final Set<UUID>committed=new LinkedHashSet<>();
        final List<Integer>propagations=new ArrayList<>();
        int commits,rollbacks;
        Pending current;
        @Override protected Object doGetTransaction(){return new Context(current);}
        @Override protected boolean isExistingTransaction(Object transaction){var pending=((Context)transaction).pending;return pending!=null&&pending.active;}
        @Override protected void doBegin(Object transaction,TransactionDefinition definition){
            var context=(Context)transaction;context.pending=new Pending();current=context.pending;propagations.add(definition.getPropagationBehavior());
        }
        @Override protected Object doSuspend(Object transaction){var context=(Context)transaction;var suspended=context.pending;context.pending=null;current=null;return suspended;}
        @Override protected void doResume(Object transaction,Object suspended){((Context)transaction).pending=(Pending)suspended;current=(Pending)suspended;}
        @Override protected void doCommit(DefaultTransactionStatus status){committed.addAll(((Context)status.getTransaction()).pending.writes);commits++;}
        @Override protected void doRollback(DefaultTransactionStatus status){rollbacks++;}
        @Override protected void doCleanupAfterCompletion(Object transaction){var context=(Context)transaction;context.pending.active=false;context.pending=null;current=null;}
        void stage(UUID account){assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();assertThat(current).isNotNull();current.writes.add(account);}
    }
}
