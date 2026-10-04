package edu.aiplatform.content.application;

import edu.aiplatform.content.port.*;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Khung cài đặt của U05: thân hàm tạm trả giá trị rỗng/an toàn hoặc báo chưa cài.
 */
@Service
public class ModuleService implements ContentRefPort {

    @Override
    public boolean moduleInSubject(UUID moduleId, UUID subjectId) {
        return false;
    }

    @Override
    public boolean lessonInScope(UUID lessonId, UUID subjectId, UUID classId) {
        return false;
    }
}
