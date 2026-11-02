package edu.aiplatform.jobs.config;

import edu.aiplatform.jobs.service.JobRouting;
import edu.aiplatform.jobs.contract.JobQueue;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarable;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Exchange và queue của việc nền (P9, P11): exchange {@code jobs} (direct, routing key là loại việc), 5 queue
 * durable; mỗi mức chờ thử lại một exchange fanout {@code jobs.retry.<mức>} và một queue có TTL, hết hạn thì quay về
 * {@code jobs} với routing key cũ; exchange {@code platform.events} (topic) cho sự kiện thông báo.
 */
@Configuration
public class AmqpTopology {

    @Bean
    Declarables jobTopology() {
        List<Declarable> declarables = new ArrayList<>();
        DirectExchange jobs = new DirectExchange(JobRouting.JOBS_EXCHANGE, true, false);
        declarables.add(jobs);
        declarables.add(new TopicExchange(JobRouting.EVENTS_EXCHANGE, true, false));
        for (JobQueue queue : JobQueue.values()) {
            QueueBuilder builder = QueueBuilder.durable(queue.queueName());
            if (queue == JobQueue.EMAIL) {
                builder.maxPriority(JobRouting.EMAIL_MAX_PRIORITY);
            }
            Queue declared = builder.build();
            declarables.add(declared);
            for (Map.Entry<String, JobQueue> route : JobRouting.all().entrySet()) {
                if (route.getValue() == queue) {
                    declarables.add(BindingBuilder.bind(declared).to(jobs).with(route.getKey()));
                }
            }
        }
        for (JobRouting.RetryDelay delay : JobRouting.RETRY_DELAYS) {
            FanoutExchange exchange = new FanoutExchange(delay.exchange(), true, false);
            Queue retry = QueueBuilder.durable(delay.exchange())
                    .ttl((int) delay.delay().toMillis())
                    .deadLetterExchange(JobRouting.JOBS_EXCHANGE)
                    .build();
            Binding binding = BindingBuilder.bind(retry).to(exchange);
            declarables.add(exchange);
            declarables.add(retry);
            declarables.add(binding);
        }
        return new Declarables(declarables);
    }
}
