package edu.aiplatform.authoring.application;

import edu.aiplatform.authoring.port.*;
import java.io.InputStream;
import org.springframework.stereotype.Service;

/**
 * Khung cài đặt của U09: thân hàm tạm trả giá trị rỗng/an toàn hoặc báo chưa cài.
 */
@Service
public class DocxExportService implements DocxExportPort {

    @Override
    public InputStream export(DocumentModel document, String title) {
        throw new UnsupportedOperationException("Chưa cài: U09");
    }
}
