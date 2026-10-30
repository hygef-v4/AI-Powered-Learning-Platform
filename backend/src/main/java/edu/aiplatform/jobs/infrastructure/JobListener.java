package edu.aiplatform.jobs.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.rabbitmq.client.Channel;
import edu.aiplatform.jobs.application.JobDispatcher;
import edu.aiplatform.jobs.application.JobRouting;
import edu.aiplatform.jobs.contract.JobMessage;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
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

    private final JobDispatcher dispatcher;
    private final ObjectMapper objectMapper;

    public JobListener(JobDispatcher dispatcher, ObjectMapper objectMapper) {
        this.dispatcher = dispatcher;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = "jobs.triggered", ackMode = "MANUAL", concurrency = "2")
    public void triggered(Message message, Channel channel) throws IOException {
        handle(message, channel);
    }

    @RabbitListener(queues = "jobs.email", ackMode = "MANUAL", concurrency = "1")
    public void email(Message message, Channel channel) throws IOException {
        handle(message, channel);
    }

    @RabbitListener(queues = "jobs.ai", ackMode = "MANUAL", concurrency = "4")
    public void ai(Message message, Channel channel) throws IOException {
        handle(message, channel);
    }

    @RabbitListener(queues = "jobs.drive", ackMode = "MANUAL", concurrency = "1")
    public void drive(Message message, Channel channel) throws IOException {
        handle(message, channel);
    }

    @RabbitListener(queues = "jobs.backup", ackMode = "MANUAL", concurrency = "1")
    public void backup(Message message, Channel channel) throws IOException {
        handle(message, channel);
    }

    void handle(Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        JobMessage job;
        try {
            JsonNode envelope = objectMapper.readTree(message.getBody());
            if (!validEnvelope(envelope)) {
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
            channel.basicNack(deliveryTag, false, true);
            return;
        }
        channel.basicAck(deliveryTag, false);
    }

    private static boolean validEnvelope(JsonNode value) {
        if (value == null || !value.isObject()) {
            return false;
        }
        JsonNode schema = value.path("schemaVersion"), attempt = value.path("attempt");
        return schema.isIntegralNumber() && schema.canConvertToInt() && schema.intValue() > 0
                && attempt.isIntegralNumber() && attempt.canConvertToInt()
                && attempt.intValue() >= 0 && attempt.intValue() <= JobRouting.MAX_ATTEMPT
                && nonBlank(value.path("jobType")) && nonBlank(value.path("idempotencyKey"))
                && nonBlank(value.path("correlationId")) && value.path("payload").isObject();
    }

    private static boolean nonBlank(JsonNode value) {
        return value.isTextual() && !value.textValue().isBlank();
    }
}
