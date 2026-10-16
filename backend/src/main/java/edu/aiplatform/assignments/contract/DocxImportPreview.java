package edu.aiplatform.assignments.contract;

import java.util.List;

/** Bản xem trước khi nhập DOCX; ghi rõ sơ đồ nào khôi phục được, sơ đồ nào thành ảnh. */
public record DocxImportPreview(DocumentModel document, List<ValidationIssue> warnings) {
}
