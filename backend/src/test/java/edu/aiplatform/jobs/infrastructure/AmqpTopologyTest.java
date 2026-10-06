package edu.aiplatform.jobs.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import edu.aiplatform.jobs.domain.BackoffPolicy;
import edu.aiplatform.jobs.port.JobTypes;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Declarable;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;

/** infrastructure-design §5, P9, P11. */
class AmqpTopologyTest {

    private final List<Declarable> declarables = AmqpTopology.declarables(new BackoffPolicy(List.of(
            Duration.ofSeconds(30), Duration.ofMinutes(1), Duration.ofMinutes(2), Duration.ofMinutes(4),
            Duration.ofMinutes(8))));

    @Test
    void declaresExchangesAndDurableQueues() {
        assertThat(declarables.stream().filter(Exchange.class::isInstance).map(d -> ((Exchange) d).getName()))
                .containsExactlyInAnyOrder("jobs", "jobs.retry", "platform.events");
        Map<String, Queue> queues = queues();
        assertThat(queues).hasSize(12);
        assertThat(queues.values()).allMatch(Queue::isDurable);
        assertThat(queues.get("jobs.email").getArguments()).containsEntry("x-max-priority", 10);
    }

    @Test
    void retryQueuesDeadLetterBackToJobs() {
        Queue retry = queues().get("jobs.retry.2m");
        assertThat(retry.getArguments())
                .containsEntry("x-message-ttl", 120_000)
                .containsEntry("x-dead-letter-exchange", "jobs")
                .doesNotContainKey("x-dead-letter-routing-key");
    }

    @Test
    void bindsEachJobTypeToItsQueue() {
        List<Binding> bindings = declarables.stream().filter(Binding.class::isInstance).map(Binding.class::cast)
                .filter(b -> b.getExchange().equals("jobs")).toList();
        assertThat(bindings).hasSize(9);
        assertThat(bindings).anyMatch(b -> b.getRoutingKey().equals(JobTypes.DRIVE_CLEANUP)
                && b.getDestination().equals("jobs.drive"));
    }

    private Map<String, Queue> queues() {
        return declarables.stream().filter(Queue.class::isInstance).map(Queue.class::cast)
                .collect(Collectors.toMap(Queue::getName, q -> q));
    }
}
