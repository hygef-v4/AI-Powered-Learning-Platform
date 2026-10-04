package edu.aiplatform.groups.application;

import edu.aiplatform.assessments.port.*;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Khung cài đặt của U12: thân hàm tạm trả giá trị rỗng/an toàn hoặc báo chưa cài.
 * Plan U12: Bước 5.
 */
@Service
public class GroupReadinessService implements GroupReadinessPort {

    @Override
    public GroupReadiness check(UUID classId) {
        throw new UnsupportedOperationException("Chưa cài: U12");
    }
}
