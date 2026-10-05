package edu.aiplatform.jobs.infrastructure;

import edu.aiplatform.jobs.application.JobProperties;
import edu.aiplatform.jobs.domain.BackoffPolicy;
import edu.aiplatform.jobs.domain.JobRouting;
import edu.aiplatform.jobs.port.JobQueue;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarable;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Khai báo exchange và queue lúc khởi động (infrastructure-design §5).
 * Queue thử lại không có consumer: hết TTL thì dead-letter về exchange `jobs`; message được gửi vào đó kèm header
 * `CC = [jobType]` nên khi quay về `jobs` nó được định tuyến lại đúng queue gốc.
 */
@Configuration
public class AmqpTopology {

    public static final String JOBS_EXCHANGE = "jobs";
    public static final String RETRY_EXCHANGE = "jobs.retry";
    public static final String EVENTS_EXCHANGE = "platform.events";

    @Bean
    BackoffPolicy backoffPolicy(JobProperties properties) {
        return new BackoffPolicy(properties.backoff());
    }

    @Bean
    MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    Declarables jobTopology(BackoffPolicy backoff) {
        return new Declarables(declarables(backoff));
    }

    static List<Declarable> declarables(BackoffPolicy backoff) {
        List<Declarable> all = new ArrayList<>();
        DirectExchange jobs = new DirectExchange(JOBS_EXCHANGE, true, false);
        DirectExchange retry = new DirectExchange(RETRY_EXCHANGE, true, false);
        all.add(jobs);
        all.add(retry);
        all.add(new TopicExchange(EVENTS_EXCHANGE, true, false));

        for (JobQueue jobQueue : JobQueue.values()) {
            QueueBuilder builder = QueueBuilder.durable(jobQueue.queueName());
            if (jobQueue == JobQueue.EMAIL) {
                builder.maxPriority(JobRouting.EMAIL_MAX_PRIORITY);
            }
            Queue queue = builder.build();
            all.add(queue);
            for (String jobType : JobRouting.jobTypesOf(jobQueue)) {
                all.add(BindingBuilder.bind(queue).to(jobs).with(jobType));
            }
        }

        for (Duration delay : backoff.delays()) {
            String name = BackoffPolicy.queueName(delay);
            Queue queue = QueueBuilder.durable(name)
                    .ttl((int) delay.toMillis())
                    .deadLetterExchange(JOBS_EXCHANGE)
                    .build();
            all.add(queue);
            Binding binding = BindingBuilder.bind(queue).to(retry).with(name);
            all.add(binding);
        }
        return all;
    }
}
