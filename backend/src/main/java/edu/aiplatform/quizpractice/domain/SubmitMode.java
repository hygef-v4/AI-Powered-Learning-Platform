package edu.aiplatform.quizpractice.domain;

/** Người học tự nộp, hệ thống nộp khi hết giờ, hoặc khi Quiz bị ngưng / có version mới (BR-U11-14). */
public enum SubmitMode {
    MANUAL, AUTO_TIME_LIMIT, AUTO_RETIRED
}
