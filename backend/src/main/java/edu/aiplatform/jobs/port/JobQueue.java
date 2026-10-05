package edu.aiplatform.jobs.port;

/** 7 queue việc nền (BR-U03-63). */
public enum JobQueue {
    TRIGGERED("jobs.triggered"), EMAIL("jobs.email"), GEMINI("jobs.gemini"), YOUTUBE("jobs.youtube"),
    CODE("jobs.code"), DRIVE("jobs.drive"), PAYOS("jobs.payos");

    private final String queueName;

    JobQueue(String queueName) { this.queueName = queueName; }

    public String queueName() { return queueName; }
}
