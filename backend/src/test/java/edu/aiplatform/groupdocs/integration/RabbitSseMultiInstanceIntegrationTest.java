package edu.aiplatform.groupdocs.integration;

import edu.aiplatform.shared.testsupport.SharedPostgres;
import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.*;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.jdbc.datasource.*;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.utility.DockerImageName;

/** Two actual hubs and broker subscriptions, real transaction commit/rollback; records server emitters. */
@Testcontainers(disabledWithoutDocker = true)
class RabbitSseMultiInstanceIntegrationTest {
    @Container
    static final GenericContainer<?> BROKER = new GenericContainer<>(DockerImageName.parse("rabbitmq:4.1-management-alpine"))
            .withExposedPorts(5672)
            // Các fork test song song cùng bật RabbitMQ thì có thể khởi động chậm hơn mức chờ mặc định 60 s.
            .waitingFor(Wait.forLogMessage(".*Server startup complete;.*\\n", 1)
                    .withStartupTimeout(java.time.Duration.ofMinutes(3)));
    static final SharedPostgres.Database DATABASE = SharedPostgres.createDatabase();

    @Test
    void committedNoticeReachesBothInstancesOnlyForItsAccountAndRollbackPublishesNothing() throws Exception {
        var connections = new CachingConnectionFactory(BROKER.getHost(), BROKER.getMappedPort(5672));
        connections.setUsername("guest");
        connections.setPassword("guest");
        var rabbit = new RabbitTemplate(connections);
        var converter = new Jackson2JsonMessageConverter(new ObjectMapper());
        rabbit.setMessageConverter(converter);
        var admin = new RabbitAdmin(connections);
        var exchange = new FanoutExchange("platform.realtime", true, false);
        admin.declareExchange(exchange);
        var first = new RabbitSseHub(rabbit);
        var second = new RabbitSseHub(rabbit);
        var listeners = new ArrayList<SimpleMessageListenerContainer>();
        var delivered = new CountDownLatch(2);
        try {
            for (var hub : List.of(first, second)) {
                var queue = new AnonymousQueue();
                admin.declareQueue(queue);
                admin.declareBinding(BindingBuilder.bind(queue).to(exchange));
                var listener = new SimpleMessageListenerContainer(connections);
                listener.setQueueNames(queue.getName());
                listener.setMessageListener((MessageListener) message -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> event = (Map<String, Object>) converter.fromMessage(message);
                    hub.receive(event);
                    delivered.countDown();
                });
                listener.start();
                listeners.add(listener);
            }
            String ownChannel = "notifications:account:" + UUID.randomUUID();
            var ownFirst = new RecordingEmitter(false);
            var ownSecond = new RecordingEmitter(false);
            var other = new RecordingEmitter(false);
            first.readChannels().addEmitter(ownChannel, null, ownFirst);
            first.readChannels().addEmitter(ownChannel, null, new RecordingEmitter(true));
            second.readChannels().addEmitter(ownChannel, null, ownSecond);
            second.readChannels().addEmitter("notifications:account:" + UUID.randomUUID(), null, other);
            var source = new DriverManagerDataSource(DATABASE.getJdbcUrl(), DATABASE.getUsername(), DATABASE.getPassword());
            var transaction = new TransactionTemplate(new DataSourceTransactionManager(source));
            var committedAt = new java.util.concurrent.atomic.AtomicLong();
            Map<String, Object> notice = Map.of("type", "notification.created", "id", UUID.randomUUID().toString());
            transaction.executeWithoutResult(status -> {
                org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                        new org.springframework.transaction.support.TransactionSynchronization() {
                            @Override
                            public void afterCommit() {
                                committedAt.set(System.nanoTime());
                            }
                        });
                first.publish(ownChannel, notice);
                assertThat(ownFirst.events).isEmpty();
                assertThat(ownSecond.events).isEmpty();
            });
            assertThat(ownFirst.events.poll(3, TimeUnit.SECONDS)).isEqualTo(notice);
            assertThat(ownSecond.events.poll(3, TimeUnit.SECONDS)).isEqualTo(notice);
            assertThat(delivered.await(3, TimeUnit.SECONDS)).isTrue();
            assertThat(committedAt.get()).isPositive();
            assertThat(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - committedAt.get())).isLessThan(5000);
            assertThat(other.events).isEmpty();
            assertThat(first.readChannels().countSubscribers(ownChannel)).isEqualTo(1);
            transaction.executeWithoutResult(status -> {
                first.publish(ownChannel, Map.of("type", "rolled-back"));
                status.setRollbackOnly();
            });
            assertThat(ownFirst.events.poll(250, TimeUnit.MILLISECONDS)).isNull();
            assertThat(ownSecond.events.poll(250, TimeUnit.MILLISECONDS)).isNull();
        } finally {
            listeners.forEach(SimpleMessageListenerContainer::stop);
            first.stop();
            second.stop();
            connections.destroy();
        }
    }

    private static final class RecordingEmitter extends SseEmitter {
        final BlockingQueue<Object> events = new LinkedBlockingQueue<>();
        final boolean broken;

        RecordingEmitter(boolean broken) {
            this.broken = broken;
        }

        @Override
        public void send(SseEventBuilder builder) throws IOException {
            if (broken) throw new IOException("fixture disconnected");
            builder.build().stream().map(part -> part.getData()).filter(Map.class::isInstance)
                    .forEach(events::add);
        }
    }
}
