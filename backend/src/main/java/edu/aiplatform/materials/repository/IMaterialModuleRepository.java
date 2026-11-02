package edu.aiplatform.materials.repository;

import edu.aiplatform.materials.model.MaterialModule;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IMaterialModuleRepository {
    Optional<MaterialModule> find(UUID id);
    List<MaterialModule> list(UUID subjectId);
    void insert(MaterialModule module);
    boolean update(MaterialModule module, long expectedVersion);
    boolean delete(UUID id, long expectedVersion);
    /** Số học liệu đang dùng (của môn và của lớp) trong module. */
    int activeMaterials(UUID moduleId);
    /** Dời học liệu đã lưu trữ sang module khác trước khi xóa module (học liệu luôn phải thuộc một module). */
    void moveArchivedMaterials(UUID fromModuleId, UUID toModuleId);
    int archivedMaterials(UUID moduleId);
    List<MaterialModule.QuizLink> quizLinks(UUID subjectId);
    Optional<MaterialModule.QuizLink> quizLink(UUID quizId);
    void link(MaterialModule.QuizLink link);
    boolean unlink(UUID moduleId, UUID quizId);
}
