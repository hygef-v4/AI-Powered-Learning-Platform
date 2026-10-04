package edu.aiplatform.jobs.application;

import edu.aiplatform.jobs.port.*;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Khung cài đặt của U03: thân hàm tạm trả giá trị rỗng/an toàn hoặc báo chưa cài.
 * Plan U03: Bước J4.
 */
@Service
public class JobPublisher implements JobPort {

    @Override
    public void enqueue(String jobType, Map<String, Object> payload, String idempotencyKey) {
        // Chưa cài: U03.
    }
}
