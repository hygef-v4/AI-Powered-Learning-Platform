package edu.aiplatform.quizzes.contract;

import java.util.UUID;

/** U10 khai báo, U11 cài: tự nộp lượt đang làm trong cùng transaction ngưng Quiz hoặc phát hành version mới. */
public interface IQuizLifecycleListener {

    void onRetired(UUID quizId);
}
