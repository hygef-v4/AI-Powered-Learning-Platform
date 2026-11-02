package edu.aiplatform.groupdocs.integration;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Các {@link SseEmitter} đang mở trên instance này theo kênh. Emitter lỗi hoặc đã đóng bị bỏ khi gửi; gửi tới một
 * emitter lỗi không ảnh hưởng emitter khác.
 */
final class SseChannels {

    record Subscriber(String owner, SseEmitter emitter) {
    }

    private final Map<String, List<Subscriber>> channels = new ConcurrentHashMap<>();

    void add(String channel, String owner, SseEmitter emitter) {
        Subscriber subscriber = new Subscriber(owner, emitter);
        channels.computeIfAbsent(channel, k -> new CopyOnWriteArrayList<>()).add(subscriber);
        Runnable remove = () -> remove(channel, subscriber);
        emitter.onCompletion(remove);
        emitter.onTimeout(remove);
        emitter.onError(e -> remove.run());
    }

    /** Gửi sự kiện JSON tới mọi người đang mở kênh; trả số emitter đã gửi được. */
    int send(String channel, Object event) {
        int delivered = 0;
        for (Subscriber s : channels.getOrDefault(channel, List.of())) {
            try {
                s.emitter().send(SseEmitter.event().name("message").data(event, MediaType.APPLICATION_JSON));
                delivered++;
            } catch (IOException | IllegalStateException e) {
                remove(channel, s);
            }
        }
        return delivered;
    }

    /** Đóng kênh của một người (người rời nhóm). */
    void close(String channel, String owner) {
        for (Subscriber s : channels.getOrDefault(channel, List.of())) {
            if (Objects.equals(owner, s.owner())) {
                remove(channel, s);
                try {
                    s.emitter().complete();
                } catch (IllegalStateException ignored) {
                    // đã đóng
                }
            }
        }
    }

    /** Giữ kết nối qua proxy (Nginx {@code proxy_read_timeout} 120 giây): gửi dòng chú thích. */
    void heartbeat() {
        channels.forEach((channel, subscribers) -> subscribers.forEach(s -> {
            try {
                s.emitter().send(SseEmitter.event().comment("hb"));
            } catch (IOException | IllegalStateException e) {
                remove(channel, s);
            }
        }));
    }

    int size(String channel) {
        return channels.getOrDefault(channel, List.of()).size();
    }

    private void remove(String channel, Subscriber subscriber) {
        channels.computeIfPresent(channel, (k, list) -> {
            list.remove(subscriber);
            return list.isEmpty() ? null : list;
        });
    }
}
