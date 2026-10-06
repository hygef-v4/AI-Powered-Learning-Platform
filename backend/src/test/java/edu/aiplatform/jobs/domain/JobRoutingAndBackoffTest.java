package edu.aiplatform.jobs.domain;

import static org.assertj.core.api.Assertions.assertThat;

import edu.aiplatform.jobs.port.JobQueue;
import edu.aiplatform.jobs.port.JobTypes;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/** BR-U03-56, 63; P9, P11. */
class JobRoutingAndBackoffTest {

    @Test
    void everyJobTypeHasExactlyOneQueue() throws IllegalAccessException {
        for (Field field : JobTypes.class.getFields()) {
            if (Modifier.isStatic(field.getModifiers()) && field.getType() == String.class) {
                String jobType = (String) field.get(null);
                assertThat(JobRouting.isKnown(jobType)).as(jobType).isTrue();
            }
        }
    }

    @Test
    void otpIsPrioritisedOverNotificationEmail() {
        assertThat(JobRouting.routeOf(JobTypes.OTP_DELIVERY).queue()).isEqualTo(JobQueue.EMAIL);
        assertThat(JobRouting.routeOf(JobTypes.OTP_DELIVERY).priority())
                .isGreaterThan(JobRouting.routeOf(JobTypes.EMAIL_SEND).priority());
    }

    @Test
    void queueThreadsMatchP11() {
        int total = Arrays.stream(JobQueue.values()).mapToInt(JobRouting::concurrencyOf).sum();
        assertThat(total).isEqualTo(12);
        assertThat(JobRouting.jobTypesOf(JobQueue.GEMINI)).containsExactly(JobTypes.AI_TASK, JobTypes.LESSON_SCAN);
    }

    @Test
    void backoffUsesOneQueuePerLevel() {
        BackoffPolicy policy = new BackoffPolicy(List.of(Duration.ofSeconds(30), Duration.ofMinutes(1),
                Duration.ofMinutes(2), Duration.ofMinutes(4), Duration.ofMinutes(8)));

        assertThat(policy.retryQueueFor(0)).isEqualTo("jobs.retry.30s");
        assertThat(policy.retryQueueFor(4)).isEqualTo("jobs.retry.8m");
        assertThat(policy.canRetry(4)).isTrue();
        assertThat(policy.canRetry(5)).isFalse();
    }
}
