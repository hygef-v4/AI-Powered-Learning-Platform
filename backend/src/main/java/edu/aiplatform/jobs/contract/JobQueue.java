package edu.aiplatform.jobs.contract;

/** Năm queue việc nền (BR-U03-63); không tạo queue mới cho từng loại việc. */
public enum JobQueue {
    TRIGGERED("jobs.triggered"),
    EMAIL("jobs.email"),
    AI("jobs.ai"),
    DRIVE("jobs.drive"),
    BACKUP("jobs.backup");

    private final String queueName;

    JobQueue(String queueName) {
        this.queueName = queueName;
    }

    public String queueName() {
        return queueName;
    }
}
