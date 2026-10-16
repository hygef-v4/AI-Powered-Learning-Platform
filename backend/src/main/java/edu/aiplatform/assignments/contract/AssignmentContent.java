package edu.aiplatform.assignments.contract;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Nội dung đã khóa của một version bài. */
public record AssignmentContent(UUID assignmentId, int versionNo, String instructions, DocumentModel skeleton,
                                List<DocumentPart> parts, Map<DiagramType, Integer> requiredDiagrams,
                                List<RubricView> rubrics) {
}
