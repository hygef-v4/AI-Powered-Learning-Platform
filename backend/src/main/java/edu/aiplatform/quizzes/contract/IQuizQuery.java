package edu.aiplatform.quizzes.contract;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Hiện Quiz cho người học (U11). */
public interface IQuizQuery {

    List<QuizView> listForClass(UUID classId);

    /**
     * Đề cho người học, không có đáp án. Quiz đã phát hành ({@code OPEN}, {@code RETIRED}) có đề cố định: không sửa
     * được câu hay danh sách câu, câu ngân hàng {@code ACTIVE} không sửa tại chỗ, sửa thì ra version mới với
     * {@code quizId} mới (BR-U10-04, 22). U11 dựa vào điều này để hiện đề và kết quả của lượt cũ mà không chụp nội dung
     * câu; đổi quy tắc này phải đổi cả U11. Quiz sinh đề không có câu ở đây: xem {@link #drawPaper}.
     */
    Optional<QuizStudentView> getStudentView(UUID quizId);

    /**
     * Đề cho một lượt mới (BR-U10-17). Quiz đề cố định trả {@link #getStudentView}; Quiz sinh đề bốc câu theo quy tắc,
     * ưu tiên câu người học chưa gặp ({@code seenQuestionIds}), và từ chối {@code 409 QUIZ_POOL_INSUFFICIENT} khi kho
     * không đủ câu. U11 phải lưu câu và điểm đã bốc vào lượt.
     */
    default Optional<QuizStudentView> drawPaper(UUID quizId, Set<UUID> seenQuestionIds, long seed) {
        return getStudentView(quizId);
    }

    /** Đề của lượt sinh đề: đúng các câu và điểm đã lưu trong lượt; câu {@code ACTIVE} không đổi nội dung (BR-U10-04). */
    default Optional<QuizStudentView> getPaper(UUID quizId, Map<UUID, BigDecimal> questionPoints) {
        return getStudentView(quizId);
    }

    Optional<QuizView> get(UUID quizId);
}
