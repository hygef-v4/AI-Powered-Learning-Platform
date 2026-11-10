package edu.aiplatform.materials.service;

import edu.aiplatform.materials.repository.IMaterialModuleRepository;
import edu.aiplatform.quizzes.contract.IQuizLifecycleListener;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cài {@link IQuizLifecycleListener} của U10 cho U13: Quiz nháp bị xóa thì gỡ khỏi module trong cùng transaction,
 * vì {@code material_module_quizzes.quiz_id} tham chiếu {@code quizzes} không cascade.
 */
@Component
public class QuizDeletionHandler implements IQuizLifecycleListener {

    private final IMaterialModuleRepository moduleRepository;

    QuizDeletionHandler(IMaterialModuleRepository moduleRepository) {
        this.moduleRepository = moduleRepository;
    }

    @Override
    public void onRetired(UUID quizId) {
        // Quiz lưu trữ vẫn nằm trong module (người soạn mở lại được).
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void onDeleting(UUID quizId) {
        moduleRepository.unlinkQuiz(quizId);
    }
}
