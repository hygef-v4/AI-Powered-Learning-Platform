package edu.aiplatform.ai.contract;

/** Chữ trích được từ nguồn riêng; không bao gồm binary hay mã định danh lưu trữ. */
public record AssignmentSourceText(String fileName, String text, boolean truncated) {
}
