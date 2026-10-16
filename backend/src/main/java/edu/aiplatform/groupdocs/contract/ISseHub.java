package edu.aiplatform.groupdocs.contract;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** Kênh realtime qua SSE cho mọi instance backend (fanout {@code platform.realtime}); U14 dùng cho chuông thông báo. */
public interface ISseHub {

    void publish(String channelKey, Object event);

    SseEmitter subscribe(String channelKey);
}
