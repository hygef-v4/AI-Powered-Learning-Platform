package edu.aiplatform.authoring.application;

import edu.aiplatform.authoring.port.*;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Khung cài đặt của U09: thân hàm tạm trả giá trị rỗng/an toàn hoặc báo chưa cài.
 * Plan U09: Bước 10.
 */
@Service
public class DocumentModelService implements DocumentModelPort {

    @Override
    public List<DocumentIssue> validateSkeleton(DocumentModel skeleton) {
        return List.of();
    }

    @Override
    public List<DocumentIssue> validateForSave(DocumentModel skeleton, DocumentModel document) {
        return List.of();
    }

    @Override
    public List<DocumentIssue> validateForSubmit(DocumentModel skeleton, DocumentModel document, Map<String, Integer> requiredDiagrams) {
        return List.of();
    }

    @Override
    public String toPlainText(DocumentModel document) {
        return "";
    }
}
