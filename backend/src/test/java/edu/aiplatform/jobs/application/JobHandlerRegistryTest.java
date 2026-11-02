package edu.aiplatform.jobs.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import edu.aiplatform.jobs.contract.IJobHandler;
import edu.aiplatform.jobs.contract.JobMessage;
import edu.aiplatform.jobs.contract.JobQueue;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class JobHandlerRegistryTest {

    record Handler(String jobType, JobQueue queue) implements IJobHandler {
        @Override
        public void handle(JobMessage message) {
        }

        @Override
        public void onFailed(JobMessage message, String error) {
        }

        @Override
        public Duration timeout() {
            return Duration.ofSeconds(1);
        }
    }

    @Test
    void findsTheHandlerOfEachJobType() {
        JobHandlerRegistry registry = new JobHandlerRegistry(List.of(new Handler("BACKUP_RUN", JobQueue.BACKUP)));

        assertThat(registry.find("BACKUP_RUN")).isPresent();
        assertThat(registry.find("EMAIL_SEND")).isEmpty();
    }

    @Test
    void rejectsDuplicatesWrongQueuesAndUnroutedTypes() {
        assertThatThrownBy(() -> new JobHandlerRegistry(List.of(new Handler("BACKUP_RUN", JobQueue.BACKUP),
                new Handler("BACKUP_RUN", JobQueue.BACKUP)))).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new JobHandlerRegistry(List.of(new Handler("BACKUP_RUN", JobQueue.EMAIL))))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new JobHandlerRegistry(List.of(new Handler("PAYOS_CHECK", JobQueue.TRIGGERED))))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void otpHasTheHighestPriorityOnTheEmailQueue() {
        assertThat(JobRouting.priorityOf("OTP_DELIVERY")).isGreaterThan(JobRouting.priorityOf("EMAIL_SEND"));
        assertThat(JobRouting.queueOf("OTP_DELIVERY")).contains(JobQueue.EMAIL);
    }
}
