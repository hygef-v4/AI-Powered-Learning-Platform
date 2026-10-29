package edu.aiplatform.groupdocs.application;

import java.util.Collection;
import java.util.UUID;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Kênh SSE của tài liệu nhóm gắn với người mở (P4): mở sau khi đã kiểm quyền; người rời nhóm thì đóng kênh của họ
 * với tài liệu cũ trên mọi instance (F14 bước 1). Đóng chạy sau commit.
 */
public interface IGroupDocStreams {

    SseEmitter open(UUID documentId, UUID accountId);

    void closeFor(UUID accountId, Collection<UUID> documentIds);
}
