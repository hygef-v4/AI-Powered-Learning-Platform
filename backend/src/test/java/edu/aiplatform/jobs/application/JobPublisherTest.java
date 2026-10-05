package edu.aiplatform.jobs.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import edu.aiplatform.jobs.infrastructure.AmqpPublisher;
import edu.aiplatform.jobs.port.JobMessage;
import edu.aiplatform.jobs.port.JobTypes;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronizationUtils;

/** BR-U03-50, 52; P8. */
class JobPublisherTest {

    private final AmqpPublisher amqp = mock(AmqpPublisher.class);
    private final JobPublisher publisher = new JobPublisher(amqp);

    @AfterEach
    void clearTransaction() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    @Test
    void sendsImmediatelyWithoutTransaction() {
        publisher.enqueue(JobTypes.OTP_DELIVERY, Map.of("accountId", "a1"), "a1:ACTIVATION:202610050900");

        ArgumentCaptor<JobMessage> sent = ArgumentCaptor.forClass(JobMessage.class);
        verify(amqp).publishJob(sent.capture());
        assertThat(sent.getValue().jobType()).isEqualTo(JobTypes.OTP_DELIVERY);
        assertThat(sent.getValue().attempt()).isZero();
        assertThat(sent.getValue().schemaVersion()).isEqualTo(1);
        assertThat(sent.getValue().correlationId()).isNotBlank();
    }

    @Test
    void sendsOnlyAfterCommitInsideTransaction() {
        beginTransaction();
        publisher.enqueue(JobTypes.LESSON_SCAN, Map.of("lessonId", "l1"), "l1");
        verify(amqp, never()).publishJob(any());

        TransactionSynchronizationUtils.triggerAfterCommit();
        verify(amqp).publishJob(any());
    }

    @Test
    void doesNotSendWhenTransactionRollsBack() {
        beginTransaction();
        publisher.enqueue(JobTypes.LESSON_SCAN, Map.of("lessonId", "l1"), "l1");

        TransactionSynchronizationUtils.triggerAfterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
        verify(amqp, never()).publishJob(any());
    }

    @Test
    void rejectsSensitivePayloadKeys() {
        assertThatThrownBy(() -> publisher.enqueue(JobTypes.OTP_DELIVERY, Map.of("otpCode", "123456"), "k"))
                .isInstanceOf(IllegalArgumentException.class);
        verify(amqp, never()).publishJob(any());
    }

    @Test
    void rejectsUnknownJobTypeAndMissingKey() {
        assertThatThrownBy(() -> publisher.enqueue("NOPE", Map.of(), "k")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> publisher.enqueue(JobTypes.CODE_RUN, Map.of(), " "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static void beginTransaction() {
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
    }
}
