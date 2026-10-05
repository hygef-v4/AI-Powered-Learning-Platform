package edu.aiplatform.jobs.worker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import edu.aiplatform.jobs.port.JobHandler;
import edu.aiplatform.jobs.port.JobMessage;
import edu.aiplatform.jobs.port.JobQueue;
import edu.aiplatform.jobs.port.JobTypes;
import edu.aiplatform.jobs.port.PendingSweeper;
import edu.aiplatform.jobs.port.ScheduledScanner;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

/** BR-U03-63. */
class JobHandlerRegistryTest {

    private final JobHandlerRegistry registry = new JobHandlerRegistry(empty(JobHandler.class),
            empty(PendingSweeper.class), empty(ScheduledScanner.class));

    @Test
    void registersHandlerOnItsQueue() {
        JobHandler handler = handler(JobTypes.DRIVE_CLEANUP, JobQueue.DRIVE);
        registry.register(handler);
        registry.register(handler);
        assertThat(registry.handlerFor(JobTypes.DRIVE_CLEANUP)).contains(handler);
    }

    @Test
    void rejectsWrongQueueUnknownTypeAndDuplicates() {
        assertThatThrownBy(() -> registry.register(handler(JobTypes.LESSON_SCAN, JobQueue.TRIGGERED)))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> registry.register(handler("NEW_QUEUE_JOB", JobQueue.TRIGGERED)))
                .isInstanceOf(IllegalStateException.class);
        registry.register(handler(JobTypes.CODE_RUN, JobQueue.CODE));
        assertThatThrownBy(() -> registry.register(handler(JobTypes.CODE_RUN, JobQueue.CODE)))
                .isInstanceOf(IllegalStateException.class);
    }

    private static <T> org.springframework.beans.factory.ObjectProvider<T> empty(Class<T> type) {
        return new StaticListableBeanFactory().getBeanProvider(type);
    }

    private static JobHandler handler(String jobType, JobQueue queue) {
        return new JobHandler() {
            public String jobType() { return jobType; }
            public JobQueue queue() { return queue; }
            public void handle(JobMessage message) {}
            public void onFailed(JobMessage message, Throwable lastError) {}
        };
    }
}
