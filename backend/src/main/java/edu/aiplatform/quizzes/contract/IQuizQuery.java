package edu.aiplatform.quizzes.contract;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Hiện Quiz cho người học (U11); Quiz gắn tài liệu trên Learning Material (U13). */
public interface IQuizQuery {

    List<QuizView> listForClass(UUID classId);

    Optional<QuizStudentView> getStudentView(UUID quizId);

    List<QuizView> listByMaterial(UUID materialId, UUID classId);

    Optional<QuizView> get(UUID quizId);
}
