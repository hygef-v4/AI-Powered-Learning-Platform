package edu.aiplatform.groupdocs.realtime;

import edu.aiplatform.groupdocs.realtime.*;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Khung cài đặt của U14: thân hàm tạm trả giá trị rỗng/an toàn hoặc báo chưa cài.
 * Plan U14: Fanout `platform.realtime`.
 */
@Component
public class RabbitSseHub implements SseHub {

    @Override
    public void publish(String channelKey, String eventType, Map<String, Object> payload) {
        // Chưa cài: U14.
    }
}
