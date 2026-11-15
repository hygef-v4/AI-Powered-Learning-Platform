package edu.aiplatform.materials.repository;

import edu.aiplatform.materials.model.MaterialModule;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IMaterialModuleRepository {
    Optional<MaterialModule> findModule(UUID id);

    List<MaterialModule> listModules(UUID subjectId);

    void insertModule(MaterialModule module);

    boolean updateModule(MaterialModule module, long expectedVersion);

    boolean deleteModule(UUID id, long expectedVersion);

    /** Số học liệu đang dùng (của môn và của lớp) trong module. */
    int countActiveMaterials(UUID moduleId);

    /** Dời học liệu đã lưu trữ sang module khác trước khi xóa module (học liệu luôn phải thuộc một module). */
    void moveArchivedMaterials(UUID fromModuleId, UUID toModuleId);

    int countArchivedMaterials(UUID moduleId);

    List<MaterialModule.QuizLink> listQuizLinks(UUID subjectId);

    Optional<MaterialModule.QuizLink> findQuizLink(UUID quizId);

    void linkQuiz(MaterialModule.QuizLink quizLink);

    boolean unlinkModuleQuiz(UUID moduleId, UUID quizId);

    /** Gỡ Quiz khỏi mọi module (Quiz sắp bị xóa). */
    void unlinkQuiz(UUID quizId);
}
