package edu.aiplatform.groupdocs.realtime;

import java.util.Map;

/** Kênh SSE theo khóa qua fanout `platform.realtime`: U14 dùng `groupDocumentId`, U16 dùng `accountId`. */
public interface SseHub {
    void publish(String channelKey, String eventType, Map<String, Object> payload);
}
