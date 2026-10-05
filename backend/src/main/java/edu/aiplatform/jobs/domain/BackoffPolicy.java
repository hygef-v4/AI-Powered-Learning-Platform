package edu.aiplatform.jobs.domain;

import java.time.Duration;
import java.util.List;

/**
 * Backoff thử lại 30 s, 1, 2, 4, 8 phút (BR-U03-56). Mỗi mức một queue `jobs.retry.{nhãn}` có TTL cố định.
 * `attempt` trong {@code JobMessage} là số lần đã thử lại: lần đầu 0, lỗi tạm ở lượt `n` thì vào mức `n`.
 */
public final class BackoffPolicy {

    public static final String RETRY_QUEUE_PREFIX = "jobs.retry.";

    private final List<Duration> delays;

    public BackoffPolicy(List<Duration> delays) {
        if (delays.isEmpty()) {
            throw new IllegalArgumentException("backoff must not be empty");
        }
        this.delays = List.copyOf(delays);
    }

    /** Số lượt thử lại tối đa. */
    public int maxRetries() {
        return delays.size();
    }

    public List<Duration> delays() {
        return delays;
    }

    /** Còn được thử lại khi đã thử lại `attempt` lần. */
    public boolean canRetry(int attempt) {
        return attempt < delays.size();
    }

    /** Queue thử lại cho message đang ở lượt `attempt` (0-based). */
    public String retryQueueFor(int attempt) {
        return queueName(delays.get(attempt));
    }

    public static String queueName(Duration delay) {
        return RETRY_QUEUE_PREFIX + label(delay);
    }

    /** `30s`, `1m`, `8m`: phút khi chia hết cho 60 giây, ngược lại giây. */
    static String label(Duration delay) {
        long seconds = delay.toSeconds();
        return seconds % 60 == 0 ? (seconds / 60) + "m" : seconds + "s";
    }
}
