package edu.aiplatform.subjects.contract;

import java.util.UUID;

/** Lớp của môn, chỉ đọc; {@code status} là trạng thái lớp do U05 trả. */
public record SubjectClassSummary(UUID classId, String code, String name, String term, String status,
                                  String teacherName, int activeStudentCount) {
}
