package edu.aiplatform.shared.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Chỉ worker chạy việc hẹn giờ ({@code IScheduledScanner}, {@code IPendingSweeper}) mỗi phút. */
@Configuration
@Profile("worker")
@EnableScheduling
public class WorkerConfig {
}
