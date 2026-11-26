package edu.aiplatform.quizpractice.model;

/**
 * Người học tự nộp, hệ thống nộp khi hết giờ, khi Quiz bị ngưng / có version mới, hoặc khi người học không còn làm
 * được lượt vì lớp không còn mở hay đã rời lớp (BR-U11-14).
 */
public enum SubmitMode {
    MANUAL, AUTO_TIME_LIMIT, AUTO_RETIRED, AUTO_ACCESS_LOST
}
