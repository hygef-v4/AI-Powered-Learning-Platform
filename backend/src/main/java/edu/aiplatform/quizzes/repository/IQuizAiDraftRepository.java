package edu.aiplatform.quizzes.repository;

import edu.aiplatform.quizzes.model.DraftStatus;
import edu.aiplatform.quizzes.model.Quiz;
import edu.aiplatform.quizzes.model.QuizAiDraft;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** {@code quiz_ai_drafts}; chỉ dùng trong package U10. */
public interface IQuizAiDraftRepository {

    void insert(QuizAiDraft draft);

    Optional<QuizAiDraft> find(UUID id);

    /** Đề xuất người dùng đã gửi với cùng {@code requestRef} (chống gửi trùng). */
    Optional<QuizAiDraft> findByRequest(UUID createdBy, String requestRef);

    /** Đề xuất chưa chốt ({@code PENDING}, {@code READY}, {@code FAILED}) của Quiz, mới nhất trước. */
    List<QuizAiDraft> listOpenForQuiz(UUID quizId);

    /** Đề xuất chưa chốt cho ngân hàng của môn, mới nhất trước. */
    List<QuizAiDraft> listOpenForBank(UUID subjectId);

    /** Ghi trạng thái, đề xuất, provenance mới nếu trạng thái hiện tại là {@code expected}. */
    boolean transition(QuizAiDraft next, DraftStatus expected);

    /** Ghi {@code taskId} sau khi gửi task; kết quả có thể đã về trước. */
    void attachTask(UUID id, UUID taskId);
}
