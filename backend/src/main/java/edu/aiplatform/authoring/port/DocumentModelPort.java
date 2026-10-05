package edu.aiplatform.authoring.port;

import java.util.List;
import java.util.Map;

/** U09 cung cấp cho U06, U11, U13, U14, U15 (BR-U09-35…37). */
public interface DocumentModelPort {
    List<DocumentIssue> validateSkeleton(DocumentModel skeleton);
    List<DocumentIssue> validateForSave(DocumentModel skeleton, DocumentModel document);
    List<DocumentIssue> validateForSubmit(DocumentModel skeleton, DocumentModel document, Map<String, Integer> requiredDiagrams);
    String toPlainText(DocumentModel document);
}
