package edu.aiplatform.authoring.application;

import edu.aiplatform.authoring.port.*;
import org.springframework.stereotype.Service;

/**
 * Khung cài đặt của U09: thân hàm tạm trả giá trị rỗng/an toàn hoặc báo chưa cài.
 */
@Service
public class DiagramCompactor implements DiagramCompactPort {

    @Override
    public String compact(String fullXml) {
        return fullXml;
    }
}
