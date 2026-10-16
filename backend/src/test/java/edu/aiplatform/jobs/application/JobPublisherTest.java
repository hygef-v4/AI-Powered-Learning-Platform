package edu.aiplatform.jobs.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import edu.aiplatform.jobs.contract.JobMessage;
import edu.aiplatform.jobs.contract.JobTypes;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class JobPublisherTest {

    final IMessageSender sender = mock(IMessageSender.class);
    final JobPublisher publisher = new JobPublisher(sender, new AfterCommitExecutor(Runnable::run));

    @AfterEach
    void clearTransaction() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void sendsImmediatelyOutsideATransaction() {
        publisher.enqueue(JobTypes.DRIVE_CLEANUP, Map.of("ref", "f1"), "cleanup:f1");

        ArgumentCaptor<JobMessage> message = ArgumentCaptor.forClass(JobMessage.class);
        verify(sender).sendJob(message.capture());
        assertThat(message.getValue().attempt()).isZero();
        assertThat(message.getValue().schemaVersion()).isEqualTo(1);
        assertThat(message.getValue().idempotencyKey()).isEqualTo("cleanup:f1");
    }

    @Test
    void sendsOnlyAfterCommitAndNothingOnRollback() {
        TransactionSynchronizationManager.initSynchronization();
        publisher.enqueue(JobTypes.DRIVE_CLEANUP, Map.of("ref", "f1"), "k");
        verify(sender, never()).sendJob(any());

        for (TransactionSynchronization sync : TransactionSynchronizationManager.getSynchronizations()) {
            sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
        }
        verify(sender, never()).sendJob(any());

        for (TransactionSynchronization sync : TransactionSynchronizationManager.getSynchronizations()) {
            sync.afterCommit();
        }
        verify(sender).sendJob(any());
    }

    @Test
    void rejectsUnknownJobTypesAndForbiddenKeys() {
        assertThatThrownBy(() -> publisher.enqueue("CODE_RUN", Map.of(), "k"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> publisher.enqueue(JobTypes.EMAIL_SEND, Map.of("otp", "123456"), "k"))
                .isInstanceOf(IllegalArgumentException.class);
        verify(sender, never()).sendJob(any());
    }

    @Test
    void brokerFailureDoesNotFailTheBusinessOperation() {
        doThrow(new RuntimeException("broker down")).when(sender).sendJob(any());

        publisher.enqueue(JobTypes.DRIVE_CLEANUP, Map.of(), "k");

        verify(sender).sendJob(any());
    }
}
