package edu.aiplatform.subjects.service;

import edu.aiplatform.shared.error.ApiException;
import java.time.Duration;

/** Đồng hồ đơn điệu cho ngân sách lock+classes, không dùng Clock nghiệp vụ để đo elapsed. */
final class SubjectLifecycleBudget {
    private final long start = System.nanoTime();
    private final long nanos;

    SubjectLifecycleBudget(Duration timeout) {
        if (timeout == null || timeout.isNegative() || timeout.isZero()) {
            throw ApiException.badRequest("SUBJECT_TIMEOUT_INVALID", "Ngân sách thời gian phải dương");
        }
        nanos = timeout.compareTo(Duration.ofSeconds(1)) > 0 ? Duration.ofSeconds(1).toNanos() : timeout.toNanos();
    }

    Duration remaining() {
        long left = nanos - (System.nanoTime() - start);
        if (left <= 0) {
            throw ApiException.unavailable("SUBJECT_LIFECYCLE_TIMEOUT", "Kiểm tra môn quá thời gian, vui lòng thử lại");
        }
        return Duration.ofNanos(left);
    }
}
