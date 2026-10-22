package edu.aiplatform.groupdocs.infrastructure;

import edu.aiplatform.groupdocs.application.GroupSectionService;
import edu.aiplatform.groupdocs.application.IGroupDocStreams;
import edu.aiplatform.groupdocs.contract.ISseHub;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * {@link ISseHub} qua RabbitMQ (P4, NFR-U12-30): gửi tới exchange fanout {@code platform.realtime} sau commit; mỗi
 * instance nghe bằng một queue tạm tự xóa và đẩy tới emitter của kênh trên instance đó. Heartbeat 25 giây. Realtime
 * là phụ: gửi lỗi chỉ ghi log, người dùng tải lại khi kết nối lại.
 */
@Component
public class RabbitSseHub implements ISseHub, IGroupDocStreams {

    static final String EXCHANGE = "platform.realtime";
    static final Duration HEARTBEAT = Duration.ofSeconds(25);
    static final Duration EMITTER_TIMEOUT = Duration.ofMinutes(30);

    private static final Logger log = LoggerFactory.getLogger(RabbitSseHub.class);

    private final RabbitTemplate rabbit;
    private final SseChannels channels = new SseChannels();
    private final ScheduledExecutorService heartbeat = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "sse-heartbeat");
        thread.setDaemon(true);
        return thread;
    });

    public RabbitSseHub(RabbitTemplate rabbit) {
        this.rabbit = rabbit;
        heartbeat.scheduleAtFixedRate(channels::heartbeat, HEARTBEAT.toSeconds(), HEARTBEAT.toSeconds(), TimeUnit.SECONDS);
    }

    @Bean
    FanoutExchange realtimeExchange() {
        return new FanoutExchange(EXCHANGE, true, false);
    }

    @Override
    public void publish(String channelKey, Object event) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("channel", channelKey);
        message.put("event", event);
        afterCommit(() -> send(message));
    }

    @Override
    public SseEmitter subscribe(String channelKey) {
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT.toMillis());
        channels.add(channelKey, null, emitter);
        return emitter;
    }

    @Override
    public SseEmitter open(UUID documentId, UUID accountId) {
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT.toMillis());
        channels.add(GroupSectionService.channel(documentId), accountId.toString(), emitter);
        return emitter;
    }

    @Override
    public void closeFor(UUID accountId, Collection<UUID> documentIds) {
        if (documentIds.isEmpty()) {
            return;
        }
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("close", accountId.toString());
        message.put("channels", documentIds.stream().map(GroupSectionService::channel).toList());
        afterCommit(() -> send(message));
    }

    /** Nhận từ mọi instance (kể cả chính mình) và đẩy tới emitter cục bộ. */
    @RabbitListener(bindings = @QueueBinding(value = @Queue(autoDelete = "true", exclusive = "true", durable = "false"),
            exchange = @Exchange(value = EXCHANGE, type = "fanout")))
    public void receive(Map<String, Object> message) {
        deliver(message);
    }

    @SuppressWarnings("unchecked")
    void deliver(Map<String, Object> message) {
        Object close = message.get("close");
        if (close != null) {
            for (Object channel : (List<Object>) message.getOrDefault("channels", List.of())) {
                channels.close(String.valueOf(channel), String.valueOf(close));
            }
            return;
        }
        Object channel = message.get("channel");
        if (channel != null) {
            channels.send(String.valueOf(channel), message.get("event"));
        }
    }

    SseChannels channels() {
        return channels;
    }

    private void send(Map<String, Object> message) {
        try {
            rabbit.convertAndSend(EXCHANGE, "", message);
        } catch (RuntimeException e) {
            log.warn("Không gửi được sự kiện realtime: {}", e.getClass().getName());
        }
    }

    private static void afterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }

    @PreDestroy
    void stop() {
        heartbeat.shutdownNow();
    }
}
