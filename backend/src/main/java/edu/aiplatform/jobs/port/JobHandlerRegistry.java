package edu.aiplatform.jobs.port;

/** Unit đăng ký handler, sweeper, scanner của mình khi khởi động (BR-U03-53, 58, 60). */
public interface JobHandlerRegistry {
    void register(JobHandler handler);

    void registerSweeper(PendingSweeper sweeper);

    void registerScanner(ScheduledScanner scanner);
}
