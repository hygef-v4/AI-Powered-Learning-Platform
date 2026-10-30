package edu.aiplatform.jobs.service;

import edu.aiplatform.jobs.contract.IJob;
import edu.aiplatform.jobs.contract.JobMessage;
import edu.aiplatform.jobs.contract.PendingJob;
import edu.aiplatform.shared.security.ForbiddenKeyGuard;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Cài {@link IJob} (J1): kiểm loại việc và khóa cấm, gửi {@code JobMessage} sau commit, không ghi database. */
@Service
public class JobPublisher implements IJob {

    public static final int SCHEMA_VERSION = 1;

    private static final Logger log = LoggerFactory.getLogger(JobPublisher.class);

    private final IMessageSender sender;
    private final AfterCommitExecutor afterCommit;

    public JobPublisher(IMessageSender sender, AfterCommitExecutor afterCommit) {
        this.sender = sender;
        this.afterCommit = afterCommit;
    }

    @Override
    public void enqueue(String jobType, Map<String, Object> payload, String idempotencyKey) {
        JobMessage message = build(jobType, payload, idempotencyKey);
        afterCommit.run(() -> send(message));
    }

    /** Gửi lại ngay việc còn chờ do {@code IPendingSweeper} trả (J3). */
    public void resend(String jobType, PendingJob pending) {
        send(build(jobType, pending.payload(), pending.idempotencyKey()));
    }

    static JobMessage build(String jobType, Map<String, Object> payload, String idempotencyKey) {
        if (JobRouting.queueOf(jobType).isEmpty()) {
            throw new IllegalArgumentException("Loại việc chưa đăng ký: " + jobType);
        }
        Map<String, Object> safePayload = payload == null ? Map.of() : payload;
        ForbiddenKeyGuard.check(safePayload);
        return new JobMessage(SCHEMA_VERSION, jobType, idempotencyKey, safePayload, 0, UUID.randomUUID().toString());
    }

    private void send(JobMessage message) {
        try {
            sender.sendJob(message);
        } catch (RuntimeException e) {
            log.warn("Gửi việc {} key={} lỗi; sweeper sẽ gửi lại", message.jobType(), message.idempotencyKey());
        }
    }
}
