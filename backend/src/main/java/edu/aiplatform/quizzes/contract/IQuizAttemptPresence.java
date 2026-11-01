package edu.aiplatform.quizzes.contract;

import java.util.UUID;

/**
 * U10 khai báo, U11 cài: Quiz đã có lượt làm chưa. Lượt chụp thứ tự câu lúc bắt đầu còn chấm và xem kết quả theo câu,
 * đáp án hiện hành của U10, nên Quiz đang mở đã có lượt thì không sửa câu tại chỗ, phải tạo version mới.
 */
public interface IQuizAttemptPresence {

    boolean hasAttempts(UUID quizId);
}
