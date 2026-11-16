package edu.aiplatform.jobs.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.rabbitmq.client.Channel;
import edu.aiplatform.jobs.service.JobDispatcher;
import edu.aiplatform.jobs.service.JobRouting;
import edu.aiplatform.jobs.contract.JobMessage;
import java.io.IOException;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Worker nghe 5 queue việc nền, mỗi queue luồng riêng (P11); ack thủ công sau khi handler xong (BR-U03-53).
 * Message hỏng thì log rồi ack để không kẹt queue.
 */
@Component
@Profile("worker")
public class JobListener {

    private static final Logger log = LoggerFactory.getLogger(JobListener.class);
    /** Chờ trước khi đưa lại message lỗi hạ tầng, để database hay broker hỏng không thành vòng lặp nóng. */
    static final Duration REQUEUE_BACKOFF = Duration.ofSeconds(5);

    private final JobDispatcher dispatcher;
    private final ObjectMapper objectMapper;
    private final Duration requeueBackoff;

    @Autowired
    public JobListener(JobDispatcher dispatcher, ObjectMapper objectMapper) {
        this(dispatcher, objectMapper, REQUEUE_BACKOFF);
    }

    JobListener(JobDispatcher dispatcher, ObjectMapper objectMapper, Duration requeueBackoff) {
        this.dispatcher = dispatcher;
        this.objectMapper = objectMapper;
        this.requeueBackoff = requeueBackoff;
    }

    @RabbitListener(queues = "jobs.triggered", ackMode = "MANUAL", concurrency = "2")
    public void receiveTriggered(Message message, Channel channel) throws IOException {
        handleDelivery(message, channel);
    }

    @RabbitListener(queues = "jobs.email", ackMode = "MANUAL", concurrency = "1")
    public void receiveEmail(Message message, Channel channel) throws IOException {
        handleDelivery(message, channel);
    }

    @RabbitListener(queues = "jobs.ai", ackMode = "MANUAL", concurrency = "4")
    public void receiveAi(Message message, Channel channel) throws IOException {
        handleDelivery(message, channel);
    }

    @RabbitListener(queues = "jobs.drive", ackMode = "MANUAL", concurrency = "1")
    public void receiveDrive(Message message, Channel channel) throws IOException {
        handleDelivery(message, channel);
    }

    @RabbitListener(queues = "jobs.backup", ackMode = "MANUAL", concurrency = "1")
    public void receiveBackup(Message message, Channel channel) throws IOException {
        handleDelivery(message, channel);
    }

    void handleDelivery(Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        JobMessage job;
        try {
            JsonNode envelope = objectMapper.readTree(message.getBody());
            if (!isValidEnvelope(envelope)) {
                channel.basicAck(deliveryTag, false);
                return;
            }
            job = objectMapper.treeToValue(envelope, JobMessage.class);
        } catch (IOException e) {
            log.error("Bỏ message việc nền không đọc được");
            channel.basicAck(deliveryTag, false);
            return;
        }
        try {
            dispatcher.dispatch(job);
        } catch (RuntimeException e) {
            // Do not expose dependency messages or lose work when terminal persistence/retry handoff fails.
            log.warn("Chưa hoàn tất việc nền; đưa message lại hàng đợi");
            pauseBeforeRequeue();
            channel.basicNack(deliveryTag, false, true);
            return;
        }
        channel.basicAck(deliveryTag, false);
    }

    private void pauseBeforeRequeue() {
        if (requeueBackoff.isZero() || requeueBackoff.isNegative()) {
            return;
        }
        try {
            Thread.sleep(requeueBackoff.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static boolean isValidEnvelope(JsonNode value) {
        if (value == null || !value.isObject()) {
            return false;
        }
        JsonNode schema = value.path("schemaVersion"), attempt = value.path("attempt");
        return schema.isIntegralNumber() && schema.canConvertToInt() && schema.intValue() > 0
                && attempt.isIntegralNumber() && attempt.canConvertToInt()
                && attempt.intValue() >= 0 && attempt.intValue() <= JobRouting.MAX_ATTEMPT
                && isNonBlank(value.path("jobType")) && isNonBlank(value.path("idempotencyKey"))
                && isNonBlank(value.path("correlationId")) && value.path("payload").isObject();
    }

    private static boolean isNonBlank(JsonNode value) {
        return value.isTextual() && !value.textValue().isBlank();
    }
}
