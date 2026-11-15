package edu.aiplatform.subjects.contract;

import java.util.UUID;

/** Lớp của môn, chỉ đọc; {@code status} là trạng thái lớp do U05 trả; {@code term} là học kỳ (D-U04-05). */
public record SubjectClassSummary(
        UUID classId,
        String code,
        String term,
        String name,
        String status,
        String teacherName,
        int activeStudentCount) {}
