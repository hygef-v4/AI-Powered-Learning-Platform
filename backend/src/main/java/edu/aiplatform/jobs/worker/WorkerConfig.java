package edu.aiplatform.jobs.worker;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.jobs.application.JobProperties;
import edu.aiplatform.jobs.domain.JobRouting;
import edu.aiplatform.jobs.infrastructure.AmqpPublisher;
import edu.aiplatform.jobs.port.JobQueue;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

/**
 * Chỉ chạy ở container `worker` (`platform.worker.enabled=true`): mỗi queue một listener container với số luồng
 * và prefetch theo P11, ack thủ công; sweeper và scanner chạy định kỳ (P10).
 */
@Configuration
@ConditionalOnProperty(name = "platform.worker.enabled", havingValue = "true")
public class WorkerConfig implements SchedulingConfigurer {

    private final JobHandlerRegistry registry;
    private final AmqpPublisher amqp;
    private final JobProperties properties;

    public WorkerConfig(JobHandlerRegistry registry, AmqpPublisher amqp, JobProperties properties) {
        this.registry = registry;
        this.amqp = amqp;
        this.properties = properties;
    }

    @Bean
    JobListener jobListener(JobRetryPublisher retryPublisher, ObjectMapper objectMapper) {
        return new JobListener(registry, retryPublisher, objectMapper, properties);
    }

    @Bean
    SmartLifecycle jobListenerContainers(ConnectionFactory connectionFactory, JobListener listener) {
        List<SimpleMessageListenerContainer> containers = new ArrayList<>();
        for (JobQueue queue : JobQueue.values()) {
            int threads = Math.min(JobRouting.concurrencyOf(queue), properties.workerMaxConcurrency());
            SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(connectionFactory);
            container.setQueueNames(queue.queueName());
            container.setConcurrentConsumers(threads);
            container.setMaxConcurrentConsumers(threads);
            container.setPrefetchCount(threads);
            container.setAcknowledgeMode(AcknowledgeMode.MANUAL);
            container.setDefaultRequeueRejected(false);
            container.setMessageListener(listener);
            container.afterPropertiesSet();
            containers.add(container);
        }
        return new SmartLifecycle() {
            private volatile boolean running;

            @Override
            public void start() {
                containers.forEach(SimpleMessageListenerContainer::start);
                running = true;
            }

            @Override
            public void stop() {
                containers.forEach(SimpleMessageListenerContainer::stop);
                listener.shutdown();
                running = false;
            }

            @Override
            public boolean isRunning() {
                return running;
            }
        };
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        Clock clock = Clock.systemUTC();
        registrar.addFixedDelayTask(
                new PendingSweepRunner(registry, amqp, properties.sweepRepublishAfter(), clock),
                properties.sweepInterval());
        registrar.addFixedDelayTask(new ScheduledScanRunner(registry, clock), properties.scanInterval());
    }
}
