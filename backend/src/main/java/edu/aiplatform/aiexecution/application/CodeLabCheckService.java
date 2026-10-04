package edu.aiplatform.aiexecution.application;

import edu.aiplatform.assessments.port.*;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Khung cài đặt của U13: thân hàm tạm trả giá trị rỗng/an toàn hoặc báo chưa cài.
 * Plan U13: Bước 12.
 */
@Service
public class CodeLabCheckService implements CodeLabCheckPort {

    @Override
    public List<ReviewIssue> check(UUID assignmentId) {
        return List.of();
    }
}
