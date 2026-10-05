package edu.aiplatform.authoring.docx;

import edu.aiplatform.authoring.port.*;
import edu.aiplatform.shared.contract.ActorRef;
import java.io.InputStream;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Khung cài đặt của U09: thân hàm tạm trả giá trị rỗng/an toàn hoặc báo chưa cài.
 */
@Service
public class DocxImporter implements DocxStudentImportPort {

    @Override
    public DocxImportPreview preview(ActorRef student, UUID attemptId, InputStream docx) {
        throw new UnsupportedOperationException("Chưa cài: U09");
    }
}
