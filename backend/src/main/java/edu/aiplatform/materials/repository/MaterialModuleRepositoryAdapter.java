package edu.aiplatform.materials.repository;

import edu.aiplatform.materials.model.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true, timeout = 10)
public class MaterialModuleRepositoryAdapter implements IMaterialModuleRepository {
    private static final String COLUMNS =
            "id,subject_id,title,order_no,created_by,created_at,updated_at,version";
    private final JdbcClient jdbcClient;

    public MaterialModuleRepositoryAdapter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public Optional<MaterialModule> findModule(UUID id) {
        return jdbcClient
                .sql("select " + COLUMNS + " from material_modules where id = :id")
                .param("id", id)
                .query(MaterialModuleRepositoryAdapter::mapModule)
                .optional();
    }

    @Override
    public List<MaterialModule> listModules(UUID subjectId) {
        return jdbcClient
                .sql("select " + COLUMNS
                        + " from material_modules where subject_id = :subjectId order by order_no,id")
                .param("subjectId", subjectId)
                .query(MaterialModuleRepositoryAdapter::mapModule)
                .list();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void lockModuleCreation(UUID subjectId) {
        jdbcClient.sql("select 1 from (select pg_advisory_xact_lock(hashtextextended(:key, 0))) locked")
                .param("key", "materials:module-creation:" + subjectId)
                .query(Integer.class)
                .single();
    }

    @Override
    @Transactional(timeout = 10)
    public void insertModule(MaterialModule module) {
        jdbcClient
                .sql("insert into material_modules(" + COLUMNS + ") values "
                        + "(:id,:subjectId,:title,:orderNo,:createdBy,:createdAt,:updatedAt,:version)")
                .param("id", module.id())
                .param("subjectId", module.subjectId())
                .param("title", module.title())
                .param("orderNo", module.orderNo())
                .param("createdBy", module.createdBy())
                .param("createdAt", Timestamp.from(module.createdAt()))
                .param("updatedAt", Timestamp.from(module.updatedAt()))
                .param("version", module.version())
                .update();
    }

    @Override
    @Transactional(timeout = 10)
    public boolean updateModule(MaterialModule module, long expectedVersion) {
        return jdbcClient
                        .sql("update material_modules set title = :title, order_no = :orderNo, "
                                + "updated_at = :updatedAt, version = :version "
                                + "where id = :id and version = :expectedVersion")
                        .param("title", module.title())
                        .param("orderNo", module.orderNo())
                        .param("updatedAt", Timestamp.from(module.updatedAt()))
                        .param("version", module.version())
                        .param("id", module.id())
                        .param("expectedVersion", expectedVersion)
                        .update()
                == 1;
    }

    @Override
    @Transactional(timeout = 10)
    public boolean deleteModule(UUID id, long expectedVersion) {
        return jdbcClient
                        .sql("delete from material_modules where id = :id and version = :expectedVersion")
                        .param("id", id)
                        .param("expectedVersion", expectedVersion)
                        .update()
                == 1;
    }

    @Override
    public int countActiveMaterials(UUID moduleId) {
        return countByModule(
                "select count(*) from materials where module_id = :moduleId and status = 'ACTIVE'",
                moduleId);
    }

    @Override
    public int countArchivedMaterials(UUID moduleId) {
        return countByModule(
                "select count(*) from materials where module_id = :moduleId and status <> 'ACTIVE'",
                moduleId);
    }

    @Override
    @Transactional(timeout = 10)
    public void moveArchivedMaterials(UUID fromModuleId, UUID toModuleId) {
        jdbcClient
                .sql("update materials set module_id = :toModuleId "
                        + "where module_id = :fromModuleId and status <> 'ACTIVE'")
                .param("fromModuleId", fromModuleId)
                .param("toModuleId", toModuleId)
                .update();
    }

    @Override
    public List<MaterialModule.QuizLink> listQuizLinks(UUID subjectId) {
        return jdbcClient
                .sql("select q.module_id,q.quiz_id,q.order_no from material_module_quizzes q "
                        + "join material_modules m on m.id = q.module_id "
                        + "where m.subject_id = :subjectId order by q.order_no,q.created_at,q.quiz_id")
                .param("subjectId", subjectId)
                .query(MaterialModuleRepositoryAdapter::mapQuizLink)
                .list();
    }

    @Override
    public Optional<MaterialModule.QuizLink> findQuizLink(UUID quizId) {
        return jdbcClient
                .sql("select module_id,quiz_id,order_no from material_module_quizzes "
                        + "where quiz_id = :quizId")
                .param("quizId", quizId)
                .query(MaterialModuleRepositoryAdapter::mapQuizLink)
                .optional();
    }

    @Override
    @Transactional(timeout = 10)
    public void linkQuiz(MaterialModule.QuizLink quizLink) {
        jdbcClient
                .sql("insert into material_module_quizzes(module_id,quiz_id,order_no,created_at) "
                        + "values (:moduleId,:quizId,:orderNo,:createdAt)")
                .param("moduleId", quizLink.moduleId())
                .param("quizId", quizLink.quizId())
                .param("orderNo", quizLink.orderNo())
                .param("createdAt", Timestamp.from(Instant.now()))
                .update();
    }

    @Override
    @Transactional(timeout = 10)
    public boolean unlinkModuleQuiz(UUID moduleId, UUID quizId) {
        return jdbcClient
                        .sql("delete from material_module_quizzes "
                                + "where module_id = :moduleId and quiz_id = :quizId")
                        .param("moduleId", moduleId)
                        .param("quizId", quizId)
                        .update()
                == 1;
    }

    @Override
    @Transactional(timeout = 10)
    public void unlinkQuiz(UUID quizId) {
        jdbcClient
                .sql("delete from material_module_quizzes where quiz_id = :quizId")
                .param("quizId", quizId)
                .update();
    }

    private int countByModule(String sql, UUID moduleId) {
        return jdbcClient.sql(sql).param("moduleId", moduleId).query(Long.class).single().intValue();
    }

    private static MaterialModule mapModule(ResultSet row, int rowNumber) throws SQLException {
        return new MaterialModule(
                row.getObject("id", UUID.class),
                row.getObject("subject_id", UUID.class),
                row.getString("title"),
                row.getInt("order_no"),
                row.getObject("created_by", UUID.class),
                row.getTimestamp("created_at").toInstant(),
                row.getTimestamp("updated_at").toInstant(),
                row.getLong("version"));
    }

    private static MaterialModule.QuizLink mapQuizLink(ResultSet row, int rowNumber)
            throws SQLException {
        return new MaterialModule.QuizLink(
                row.getObject("module_id", UUID.class),
                row.getObject("quiz_id", UUID.class),
                row.getInt("order_no"));
    }
}
