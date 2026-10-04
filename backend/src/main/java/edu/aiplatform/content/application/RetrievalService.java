package edu.aiplatform.content.application;

import edu.aiplatform.content.port.*;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Khung cài đặt của U05: thân hàm tạm trả giá trị rỗng/an toàn hoặc báo chưa cài.
 */
@Service
public class RetrievalService implements RagRetrievalPort {

    @Override
    public List<LessonPassage> retrieve(RagScope scope, String query, int k, UUID requesterId, String requestRef) {
        return List.of();
    }
}
