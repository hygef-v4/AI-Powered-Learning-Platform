package edu.aiplatform.jobs.contract;

import java.util.Map;

/** Gửi việc nền sau commit (rollback thì không gửi), không ghi database. Dùng bởi U01, U03, U06, U12, U13, U14. */
public interface IJob {

    void enqueue(String jobType, Map<String, Object> payload, String idempotencyKey);
}
