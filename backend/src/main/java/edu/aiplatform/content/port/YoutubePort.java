package edu.aiplatform.content.port;

import java.util.Optional;

public interface YoutubePort {
    String videoTitle(String videoId);
    /** Phụ đề có sẵn, ưu tiên vi → en → tự động; rỗng nếu không có (BR-U05-33). */
    Optional<String> captionText(String videoId);
}
