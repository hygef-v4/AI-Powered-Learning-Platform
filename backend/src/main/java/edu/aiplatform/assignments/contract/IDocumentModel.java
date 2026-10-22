package edu.aiplatform.assignments.contract;

import java.util.List;
import java.util.Map;

/** Kiểm bài làm không đụng khung, đủ sơ đồ khi nộp; chữ và XML rút gọn cho AI. Dùng bởi U08, U09, U12. */
public interface IDocumentModel {

    ValidationResult validateSkeleton(DocumentModel skeleton);

    ValidationResult validateForSave(DocumentModel skeleton, DocumentModel document);

    ValidationResult validateForSubmit(DocumentModel skeleton, DocumentModel document,
                                       Map<DiagramType, Integer> requiredDiagrams);

    List<DocumentPart> computeParts(DocumentModel skeleton);

    String toPlainText(DocumentModel document);

    String compactDiagramXml(String xml);

    /**
     * Bản để lưu: làm sạch SVG xem trước của sơ đồ (bỏ script, thuộc tính sự kiện, tham chiếu ngoài), bỏ khóa lạ
     * trong nội dung block. Gọi trước {@code validate*} khi lưu khung hoặc bài làm (NFR-U07-30).
     */
    DocumentModel sanitize(DocumentModel document);
}
