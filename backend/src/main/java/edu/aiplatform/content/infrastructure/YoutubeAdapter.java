package edu.aiplatform.content.infrastructure;

import edu.aiplatform.content.port.*;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Khung cài đặt của U05: thân hàm tạm trả giá trị rỗng/an toàn hoặc báo chưa cài.
 * Plan U05: Adapter YouTube.
 */
@Component
public class YoutubeAdapter implements YoutubePort {

    @Override
    public String videoTitle(String videoId) {
        throw new UnsupportedOperationException("Chưa cài: U05");
    }

    @Override
    public Optional<String> captionText(String videoId) {
        return Optional.empty();
    }
}
