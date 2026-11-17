package edu.aiplatform.quizzes.service;

import edu.aiplatform.shared.error.ApiException;
import edu.aiplatform.subjects.contract.ISubjectQuery;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Môn đã lưu trữ thì nội dung của U10 chỉ xem; U04 không trả lời thì từ chối (BR-U10-52). */
@Component
class ActiveSubjects {

    private final ISubjectQuery subjects;

    ActiveSubjects(ISubjectQuery subjects) {
        this.subjects = subjects;
    }

    void require(UUID subjectId) {
        boolean active;
        try {
            active = subjects.isActive(subjectId);
        } catch (RuntimeException unavailable) {
            throw ApiException.createUnavailable("SUBJECT_UNAVAILABLE", "Chưa kiểm tra được môn, vui lòng thử lại");
        }
        if (!active) {
            throw ApiException.createConflict("SUBJECT_NOT_ACTIVE", "Môn đã lưu trữ, ngân hàng câu hỏi chỉ xem");
        }
    }
}
