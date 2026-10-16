package edu.aiplatform.assignments.contract;

import java.util.List;

/** Lỗi chặn phát hành và cảnh báo khi bật bài nhóm. */
public record ReadinessResult(List<String> errors, List<String> warnings) {
}
