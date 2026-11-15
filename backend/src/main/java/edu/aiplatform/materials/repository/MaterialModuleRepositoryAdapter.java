package edu.aiplatform.materials.repository;

import edu.aiplatform.materials.model.*;
import jakarta.persistence.*;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true, timeout = 10)
public class MaterialModuleRepositoryAdapter implements IMaterialModuleRepository {
    private static final String COLUMNS =
            "id,subject_id,title,order_no,created_by,created_at,updated_at,version";
    @PersistenceContext private EntityManager entityManager;

    @Override
    public Optional<MaterialModule> findModule(UUID id) {
        List<?> rows =
                entityManager
                        .createNativeQuery(
                                "select " + COLUMNS + " from material_modules where id=?1")
                        .setParameter(1, id)
                        .getResultList();
        return rows.stream().findFirst().map(row -> mapModule((Object[]) row));
    }

    @Override
    public List<MaterialModule> listModules(UUID subjectId) {
        List<?> rows =
                entityManager.createNativeQuery(
                                "select "
                                        + COLUMNS
                                        + " from material_modules where subject_id=?1 order by order_no,id")
                        .setParameter(1, subjectId)
                        .getResultList();
        return rows.stream().map(row -> mapModule((Object[]) row)).toList();
    }

    @Override
    @Transactional(timeout = 10)
    public void insertModule(MaterialModule module) {
        entityManager.createNativeQuery(
                        "insert into material_modules("
                                + COLUMNS
                                + ") values (?1,?2,?3,?4,?5,?6,?7,?8)")
                .setParameter(1, module.id())
                .setParameter(2, module.subjectId())
                .setParameter(3, module.title())
                .setParameter(4, module.orderNo())
                .setParameter(5, module.createdBy())
                .setParameter(6, Timestamp.from(module.createdAt()))
                .setParameter(7, Timestamp.from(module.updatedAt()))
                .setParameter(8, module.version())
                .executeUpdate();
    }

    @Override
    @Transactional(timeout = 10)
    public boolean updateModule(MaterialModule module, long expectedVersion) {
        return entityManager.createNativeQuery(
                                "update material_modules set title=?1,order_no=?2,updated_at=?3,version=?4 where id=?5 and version=?6")
                        .setParameter(1, module.title())
                        .setParameter(2, module.orderNo())
                        .setParameter(3, Timestamp.from(module.updatedAt()))
                        .setParameter(4, module.version())
                        .setParameter(5, module.id())
                        .setParameter(6, expectedVersion)
                        .executeUpdate()
                == 1;
    }

    @Override
    @Transactional(timeout = 10)
    public boolean deleteModule(UUID id, long expectedVersion) {
        return entityManager
                        .createNativeQuery(
                                "delete from material_modules where id=?1 and version=?2")
                        .setParameter(1, id)
                        .setParameter(2, expectedVersion)
                        .executeUpdate()
                == 1;
    }

    @Override
    public int countActiveMaterials(UUID moduleId) {
        return countByModule(
                "select count(*) from materials where module_id=?1 and status='ACTIVE'", moduleId);
    }

    @Override
    public int countArchivedMaterials(UUID moduleId) {
        return countByModule(
                "select count(*) from materials where module_id=?1 and status<>'ACTIVE'", moduleId);
    }

    @Override
    @Transactional(timeout = 10)
    public void moveArchivedMaterials(UUID fromModuleId, UUID toModuleId) {
        entityManager.createNativeQuery(
                        "update materials set module_id=?2 where module_id=?1 and status<>'ACTIVE'")
                .setParameter(1, fromModuleId)
                .setParameter(2, toModuleId)
                .executeUpdate();
    }

    @Override
    public List<MaterialModule.QuizLink> listQuizLinks(UUID subjectId) {
        List<?> rows =
                entityManager.createNativeQuery(
                                "select q.module_id,q.quiz_id,q.order_no from material_module_quizzes q "
                                        + "join material_modules m on m.id=q.module_id where m.subject_id=?1 order by q.order_no,q.created_at,q.quiz_id")
                        .setParameter(1, subjectId)
                        .getResultList();
        return rows.stream().map(row -> mapQuizLink((Object[]) row)).toList();
    }

    @Override
    public Optional<MaterialModule.QuizLink> findQuizLink(UUID quizId) {
        List<?> rows =
                entityManager.createNativeQuery(
                                "select module_id,quiz_id,order_no from material_module_quizzes where quiz_id=?1")
                        .setParameter(1, quizId)
                        .getResultList();
        return rows.stream().findFirst().map(row -> mapQuizLink((Object[]) row));
    }

    @Override
    @Transactional(timeout = 10)
    public void linkQuiz(MaterialModule.QuizLink quizLink) {
        entityManager.createNativeQuery(
                        "insert into material_module_quizzes(module_id,quiz_id,order_no,created_at) values (?1,?2,?3,?4)")
                .setParameter(1, quizLink.moduleId())
                .setParameter(2, quizLink.quizId())
                .setParameter(3, quizLink.orderNo())
                .setParameter(4, Timestamp.from(Instant.now()))
                .executeUpdate();
    }

    @Override
    @Transactional(timeout = 10)
    public boolean unlinkModuleQuiz(UUID moduleId, UUID quizId) {
        return entityManager.createNativeQuery(
                                "delete from material_module_quizzes where module_id=?1 and quiz_id=?2")
                        .setParameter(1, moduleId)
                        .setParameter(2, quizId)
                        .executeUpdate()
                == 1;
    }

    @Override
    @Transactional(timeout = 10)
    public void unlinkQuiz(UUID quizId) {
        entityManager.createNativeQuery("delete from material_module_quizzes where quiz_id=?1")
                .setParameter(1, quizId)
                .executeUpdate();
    }

    private int countByModule(String sql, UUID id) {
        return ((Number) entityManager.createNativeQuery(sql).setParameter(1, id).getSingleResult())
                .intValue();
    }

    private static MaterialModule mapModule(Object[] row) {
        return new MaterialModule(
                (UUID) row[0],
                (UUID) row[1],
                (String) row[2],
                ((Number) row[3]).intValue(),
                (UUID) row[4],
                convertInstant(row[5]),
                convertInstant(row[6]),
                ((Number) row[7]).longValue());
    }

    private static MaterialModule.QuizLink mapQuizLink(Object[] row) {
        return new MaterialModule.QuizLink(
                (UUID) row[0], (UUID) row[1], ((Number) row[2]).intValue());
    }

    private static Instant convertInstant(Object value) {
        if (value instanceof Instant i) return i;
        if (value instanceof Timestamp t) return t.toInstant();
        if (value instanceof java.time.OffsetDateTime o) return o.toInstant();
        throw new IllegalStateException("Unexpected timestamp " + value);
    }
}
