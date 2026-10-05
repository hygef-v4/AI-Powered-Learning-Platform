package edu.aiplatform.jobs.worker;

import org.springframework.stereotype.Component;

/**
 * Khung cài đặt của U03: thân hàm tạm trả giá trị rỗng/an toàn hoặc báo chưa cài.
 * Plan U03: Bước J6.
 */
@Component
public class JobHandlerRegistry implements edu.aiplatform.jobs.port.JobHandlerRegistry {

    @Override
    public void register(edu.aiplatform.jobs.port.JobHandler handler) {
        // Chưa cài: U03.
    }

    @Override
    public void registerSweeper(edu.aiplatform.jobs.port.PendingSweeper sweeper) {
        // Chưa cài: U03.
    }

    @Override
    public void registerScanner(edu.aiplatform.jobs.port.ScheduledScanner scanner) {
        // Chưa cài: U03.
    }
}
