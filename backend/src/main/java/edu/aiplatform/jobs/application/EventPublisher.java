package edu.aiplatform.jobs.application;

import edu.aiplatform.jobs.port.*;
import org.springframework.stereotype.Service;

/**
 * Khung cài đặt của U03: thân hàm tạm trả giá trị rỗng/an toàn hoặc báo chưa cài.
 * Plan U03: Bước J4.
 */
@Service
public class EventPublisher implements EventPublisherPort {

    @Override
    public void publish(DomainEvent event) {
        // Chưa cài: U03.
    }
}
