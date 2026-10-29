package edu.aiplatform.assignments.contract;

import java.time.LocalDate;

/**
 * Số bài được giao trong một ngày theo một loại: {@code PRACTICE} (luyện tập), còn lại theo {@link SubmissionMode}
 * ({@code INDIVIDUAL}, {@code GROUP}).
 */
public record AssignmentDayCount(LocalDate day, String kind, long count) {
}
