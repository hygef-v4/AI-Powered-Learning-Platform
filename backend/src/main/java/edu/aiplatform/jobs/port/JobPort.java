package edu.aiplatform.jobs.port;

import java.util.Map;

/** Có transaction thì gửi sau commit, không có thì gửi ngay; không ghi database (BR-U03-50). */
public interface JobPort {
    void enqueue(String jobType, Map<String, Object> payload, String idempotencyKey);
}
