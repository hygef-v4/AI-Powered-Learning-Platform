package edu.aiplatform.jobs.service;

import edu.aiplatform.jobs.contract.JobQueue;
import edu.aiplatform.jobs.contract.JobTypes;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Bảng định tuyến loại việc → queue (BR-U03-63), độ ưu tiên trên {@code jobs.email} và mức chờ thử lại. */
public final class JobRouting {

    public static final String JOBS_EXCHANGE = "jobs";
    public static final String EVENTS_EXCHANGE = "platform.events";
    public static final String RETRY_EXCHANGE_PREFIX = "jobs.retry.";
    public static final int MAX_ATTEMPT = 5;
    public static final int EMAIL_MAX_PRIORITY = 10;

    /** Mức chờ trước lần thử lại thứ 1–5 (NFR-U03-44). */
    public static final List<RetryDelay> RETRY_DELAYS = List.of(
            new RetryDelay("30s", Duration.ofSeconds(30)), new RetryDelay("1m", Duration.ofMinutes(1)),
            new RetryDelay("2m", Duration.ofMinutes(2)), new RetryDelay("4m", Duration.ofMinutes(4)),
            new RetryDelay("8m", Duration.ofMinutes(8)));

    static final Map<String, JobQueue> QUEUES = Map.of(
            JobTypes.OTP_DELIVERY, JobQueue.EMAIL,
            JobTypes.EMAIL_SEND, JobQueue.EMAIL,
            JobTypes.AI_TASK, JobQueue.AI,
            JobTypes.GROUP_DOC_CREATE, JobQueue.TRIGGERED,
            JobTypes.MATERIAL_TEXT_EXTRACT, JobQueue.TRIGGERED,
            JobTypes.DRIVE_CLEANUP, JobQueue.DRIVE,
            JobTypes.BACKUP_RUN, JobQueue.BACKUP);

    private JobRouting() {
    }

    public record RetryDelay(String label, Duration delay) {

        public String exchange() {
            return RETRY_EXCHANGE_PREFIX + label;
        }
    }

    public static Optional<JobQueue> queueOf(String jobType) {
        return Optional.ofNullable(QUEUES.get(jobType));
    }

    public static Map<String, JobQueue> all() {
        return QUEUES;
    }

    /** OTP được gửi trước email thông báo. */
    public static int priorityOf(String jobType) {
        return switch (jobType) {
            case JobTypes.OTP_DELIVERY -> EMAIL_MAX_PRIORITY;
            case JobTypes.EMAIL_SEND -> 1;
            default -> 0;
        };
    }
}
