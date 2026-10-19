package edu.aiplatform.academics.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import edu.aiplatform.academics.contract.ClassStatus;
import edu.aiplatform.academics.domain.*;
import edu.aiplatform.audit.contract.*;
import edu.aiplatform.identity.contract.*;
import edu.aiplatform.jobs.contract.IEventPublisher;
import edu.aiplatform.shared.error.ApiException;
import edu.aiplatform.subjects.contract.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.*;

/** Observes service transaction orchestration; does not simulate PostgreSQL durability or locking. */
class AcademicsBatchTransactionTest {
    final ICourseClassRepository classes=mock(ICourseClassRepository.class);
    final IEnrollmentRepository enrollments=mock(IEnrollmentRepository.class);
    final IAcademicsReadRepository reads=mock(IAcademicsReadRepository.class);
    final ISubjectMutationLock lock=mock(ISubjectMutationLock.class);
    final IAccountLookup accounts=mock(IAccountLookup.class);
    final ISubjectQuery subjects=mock(ISubjectQuery.class);
    final ISubjectScope scope=mock(ISubjectScope.class);
    final IAuthorization authorization=mock(IAuthorization.class);
    final IAudit audit=mock(IAudit.class);
    final IEventPublisher events=mock(IEventPublisher.class);
    final RecordingTransactions tx=new RecordingTransactions();
    final UUID managerId=UUID.randomUUID(),subjectId=UUID.randomUUID(),classId=UUID.randomUUID();
    final UUID first=UUID.randomUUID(),second=UUID.randomUUID(),third=UUID.randomUUID();
    final Instant now=Instant.parse("2026-10-18T00:00:00Z");
    final Actor manager=new Actor(managerId,Role.SUBJECT_MANAGER);
    final String input="first@example.test\nsecond@example.test\nthird@example.test";
    AcademicsService service;

    @BeforeEach void setup() {
        service=new AcademicsService(classes,enrollments,reads,lock,accounts,subjects,scope,authorization,audit,events,tx,Clock.fixed(now,ZoneOffset.UTC));
        when(accounts.getContact(managerId)).thenReturn(Optional.of(contact(managerId,"manager@example.test",Role.SUBJECT_MANAGER)));
        when(authorization.authorize(any(),anyString(),any())).thenReturn(AuthorizationDecision.allow());
        when(scope.isSubjectManager(managerId,subjectId)).thenReturn(true);
        when(subjects.getSubject(subjectId)).thenReturn(Optional.of(new SubjectRef(subjectId,"S","Subject",SubjectStatus.ACTIVE,managerId)));
        when(classes.find(classId)).thenReturn(Optional.of(new CourseClass(classId,subjectId,"C","Class","","2026",ClassStatus.OPEN,managerId,false,now,now,0)));
        when(accounts.findByEmails(any())).thenReturn(List.of(contact(first,"first@example.test",Role.STUDENT),contact(second,"second@example.test",Role.STUDENT),contact(third,"third@example.test",Role.STUDENT)));
        when(enrollments.find(eq(classId),any())).thenAnswer(call->{
            UUID account=call.getArgument(1);
            return tx.committed.contains(account)?Optional.of(Enrollment.create(classId,account,EnrollmentSource.LIST,now)):Optional.empty();
        });
        doAnswer(call->{tx.stage(((Enrollment)call.getArgument(0)).accountId());return null;}).when(enrollments).insert(any());
    }
    @AfterEach void noLeakedTransaction() {
        assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
        assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
    }
    AccountContact contact(UUID id,String email,Role role) {return new AccountContact(id,email,"Person",role,AccountStatus.ACTIVE);}

    @Test void laterStorageFailureRollsBackOnlyThatLineAndStopsBeforeFollowingLine() {
        doAnswer(call->{
            UUID account=((Enrollment)call.getArgument(0)).accountId();tx.stage(account);
            if(account.equals(second))throw new DataAccessResourceFailureException("password=must-not-leak");
            return null;
        }).when(enrollments).insert(any());
        doAnswer(call->{
            AuditEvent event=call.getArgument(0);
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isEqualTo(event.result()==AuditResult.SUCCESS);
            return null;
        }).when(audit).record(any());
        assertThatThrownBy(()->service.enrollBatch(manager,classId,input)).isInstanceOfSatisfying(ApiException.class,error->{
            assertThat(error.status().value()).isEqualTo(503);assertThat(error.getMessage()).doesNotContain("password");
        });
        assertThat(tx.committed).containsExactly(first);
        assertThat(tx.commits).isEqualTo(1);assertThat(tx.rollbacks).isEqualTo(1);
        assertThat(tx.propagations).containsExactly(TransactionDefinition.PROPAGATION_REQUIRES_NEW,TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        verify(enrollments,never()).insert(argThat(value->value.accountId().equals(third)));
        verify(events).publish(argThat(event->event.payload().get("accountId").equals(first.toString())));
        verify(events,times(1)).publish(any());
        verify(audit).record(argThat(event->event.result()==AuditResult.FAILURE));
    }

    @Test void retryAfterPartialSuccessDoesNotRepublishOrRewriteAlreadyEnrolledStudent() {
        var failure=new HashSet<>(Set.of(second));
        doAnswer(call->{UUID account=((Enrollment)call.getArgument(0)).accountId();tx.stage(account);
            if(failure.remove(account))throw new DataAccessResourceFailureException("temporary");return null;
        }).when(enrollments).insert(any());
        assertThatThrownBy(()->service.enrollBatch(manager,classId,input)).isInstanceOf(ApiException.class);
        assertThat(service.enrollBatch(manager,classId,input)).extracting(AcademicsDtos.RowResult::result)
                .containsExactly(EnrollmentResult.ALREADY_ENROLLED,EnrollmentResult.ENROLLED,EnrollmentResult.ENROLLED);
        assertThat(tx.committed).containsExactly(first,second,third);
        verify(enrollments,times(1)).insert(argThat(value->value.accountId().equals(first)));
        verify(events,times(1)).publish(argThat(event->event.payload().get("accountId").equals(first.toString())));
        verify(accounts,times(2)).findByEmails(any());
        assertThat(tx.commits).isEqualTo(4);assertThat(tx.rollbacks).isEqualTo(1);
    }

    @Test void invalidAndDuplicateRowsDoNotOpenTransactionsButLookupMissIsAnIsolatedOutcome() {
        var rows=service.enrollBatch(manager,classId,"first@example.test\nbad\nfirst@example.test\nmissing@example.test\nthird@example.test");
        assertThat(rows).extracting(AcademicsDtos.RowResult::result).containsExactly(EnrollmentResult.ENROLLED,EnrollmentResult.INVALID_EMAIL,
                EnrollmentResult.DUPLICATE_IN_LIST,EnrollmentResult.NOT_FOUND,EnrollmentResult.ENROLLED);
        assertThat(rows).extracting(AcademicsDtos.RowResult::line).containsExactly(1,2,3,4,5);
        assertThat(tx.propagations).hasSize(3).containsOnly(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        assertThat(tx.commits).isEqualTo(3);assertThat(tx.rollbacks).isZero();
        assertThat(tx.committed).containsExactly(first,third);
        verify(accounts).findByEmails(List.of("first@example.test","missing@example.test","third@example.test"));
    }

    @Test void successAuditFailureRollsBackThePendingEnrollmentAndSuppressesActivation() {
        doAnswer(call->{AuditEvent event=call.getArgument(0);if(event.result()==AuditResult.SUCCESS)throw new IllegalStateException("audit down");return null;}).when(audit).record(any());
        assertThatThrownBy(()->service.enrollBatch(manager,classId,"first@example.test")).isInstanceOfSatisfying(ApiException.class,error->assertThat(error.status().value()).isEqualTo(503));
        assertThat(tx.committed).isEmpty();assertThat(tx.rollbacks).isEqualTo(1);assertThat(tx.commits).isZero();
        verifyNoInteractions(events);
        verify(audit).record(argThat(event->event.result()==AuditResult.FAILURE));
    }

    @Test void authorityRevokedBetweenRowsKeepsPriorCommitButRejectsTheRemainingBatch() {
        // Quyền bị thu hồi ngay sau khi dòng đầu commit.
        when(authorization.authorize(any(),anyString(),any())).thenAnswer(call->
                call.<String>getArgument(1).equals("CLASS_MANAGE")&&!tx.committed.isEmpty()?AuthorizationDecision.deny("revoked"):AuthorizationDecision.allow());
        assertThatThrownBy(()->service.enrollBatch(manager,classId,input)).isInstanceOfSatisfying(ApiException.class,error->assertThat(error.status().value()).isEqualTo(404));
        assertThat(tx.committed).containsExactly(first);assertThat(tx.commits).isEqualTo(1);assertThat(tx.rollbacks).isEqualTo(1);
        verify(enrollments,never()).insert(argThat(value->!value.accountId().equals(first)));
        verify(audit).record(argThat(event->event.action().equals("ACCESS_DENIED")&&event.result()==AuditResult.DENIED));
    }

    @Test void rowCommitIsIndependentOfAnAlreadyActiveCallerTransaction() {
        var caller=new TransactionTemplate(tx);
        assertThatThrownBy(()->caller.execute(status->{
            service.enrollBatch(manager,classId,"first@example.test");
            throw new IllegalStateException("caller rolls back afterward");
        })).isInstanceOf(IllegalStateException.class).hasMessage("caller rolls back afterward");
        assertThat(tx.committed).containsExactly(first);
        assertThat(tx.commits).isEqualTo(1);assertThat(tx.rollbacks).isEqualTo(1);
        assertThat(tx.propagations).containsExactly(TransactionDefinition.PROPAGATION_REQUIRED,TransactionDefinition.PROPAGATION_REQUIRES_NEW);
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
