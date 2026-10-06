package edu.aiplatform.jobs.application;

import edu.aiplatform.jobs.domain.JobRouting;
import edu.aiplatform.jobs.infrastructure.AmqpPublisher;
import edu.aiplatform.jobs.port.JobMessage;
import edu.aiplatform.jobs.port.JobPort;
import edu.aiplatform.shared.security.ForbiddenKeyGuard;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Gửi việc nền (J1, P8): không ghi database; trong transaction thì gửi sau commit, rollback không gửi;
 * ngoài transaction thì gửi ngay (BR-U03-50, 52).
 */
@Service
public class JobPublisher implements JobPort {

    public static final int SCHEMA_VERSION = 1;

    private final AmqpPublisher amqp;

    public JobPublisher(AmqpPublisher amqp) {
        this.amqp = amqp;
    }

    @Override
    public void enqueue(String jobType, Map<String, Object> payload, String idempotencyKey) {
        if (!JobRouting.isKnown(jobType)) {
            throw new IllegalArgumentException("Unknown jobType: " + jobType);
        }
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("idempotencyKey is required");
        }
        Map<String, Object> safePayload = payload == null ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(payload));
        ForbiddenKeyGuard.check(safePayload);
        JobMessage message = new JobMessage(SCHEMA_VERSION, jobType, idempotencyKey, safePayload, 0,
                AfterCommit.correlationId());
        AfterCommit.run(() -> amqp.publishJob(message));
    }
}
