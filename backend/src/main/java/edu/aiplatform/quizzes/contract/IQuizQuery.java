package edu.aiplatform.quizzes.contract;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Hiện Quiz cho người học (U11); Quiz gắn tài liệu trên Learning Material (U13). */
public interface IQuizQuery {

    List<QuizView> listForClass(UUID classId);

    /**
     * Đề cho người học, không có đáp án. Quiz đã phát hành ({@code OPEN}, {@code RETIRED}) có đề cố định: không sửa
     * được câu hay danh sách câu, câu ngân hàng {@code ACTIVE} không sửa tại chỗ, sửa thì ra version mới với
     * {@code quizId} mới (BR-U10-04, 22). U11 dựa vào điều này để hiện đề và kết quả của lượt cũ mà không chụp nội dung
     * câu; đổi quy tắc này phải đổi cả U11.
     */
    Optional<QuizStudentView> getStudentView(UUID quizId);

    List<QuizView> listByMaterial(UUID materialId, UUID classId);

    Optional<QuizView> get(UUID quizId);
}
