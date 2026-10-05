package edu.aiplatform.content.application;

import edu.aiplatform.academics.port.*;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Khung cài đặt của U05: thân hàm tạm trả giá trị rỗng/an toàn hoặc báo chưa cài.
 */
@Service
public class PublishedContentService implements PublishedContentPort {

    @Override
    public List<ModuleView> listForClass(UUID classId) {
        return List.of();
    }
}
