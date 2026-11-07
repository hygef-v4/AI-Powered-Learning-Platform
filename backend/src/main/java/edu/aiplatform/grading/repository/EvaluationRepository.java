package edu.aiplatform.grading.repository;

import edu.aiplatform.grading.model.Evaluation;
import edu.aiplatform.shared.error.ApiException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class EvaluationRepository implements IEvaluationRepository {
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public EvaluationRepository(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    public Optional<Evaluation> findById(UUID id) {
        return namedParameterJdbcTemplate
                .query("select grading_document, version from evaluations where id=:id",
                        Map.of("id", id), EvaluationRepository::mapEvaluation)
                .stream().findFirst();
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<Evaluation> lockById(UUID id) {
        return namedParameterJdbcTemplate
                .query("select grading_document, version from evaluations where id=:id for update",
                        Map.of("id", id), EvaluationRepository::mapEvaluation)
                .stream().findFirst();
    }

    public Optional<Evaluation> findByAttemptId(UUID id) {
        return namedParameterJdbcTemplate
                .query("select grading_document, version from evaluations where attempt_id=:id",
                        Map.of("id", id), EvaluationRepository::mapEvaluation)
                .stream().findFirst();
    }

    public List<Evaluation> findForAssignmentAndClass(UUID assignmentId, UUID classId) {
        return namedParameterJdbcTemplate.query("""
                select grading_document, version from evaluations
                where assignment_id=:assignmentId and class_id=:classId
                """, Map.of("assignmentId", assignmentId, "classId", classId),
                EvaluationRepository::mapEvaluation);
    }

    public List<Evaluation> findForClass(UUID classId) {
        return namedParameterJdbcTemplate.query(
                "select grading_document, version from evaluations where class_id=:classId",
                Map.of("classId", classId), EvaluationRepository::mapEvaluation);
    }

    @Transactional
    public Evaluation save(Evaluation evaluation) {
        var versions = namedParameterJdbcTemplate.queryForList(
                "select version from evaluations where id=:id", Map.of("id", evaluation.id),
                Long.class);
        if (!versions.isEmpty() && versions.get(0) != evaluation.version)
            throw createVersionConflict();
        var row = new EvaluationRow();
        row.applyEvaluation(evaluation);
        var args = mapParameters(row);
        args.put("expectedVersion", evaluation.version);
        String sql = versions.isEmpty()
                ? """
                        insert into evaluations (id, assignment_id, class_id, attempt_id, group_document_id,
                            account_id, kind, status, submission_hash, score, max_score, grading_document, version)
                        values (:id, :assignmentId, :classId, :attemptId, :groupDocumentId, :accountId,
                            :kind, :status, :submissionHash, :score, :maxScore, cast(:document as jsonb), 0)
                        returning version
                        """
                : """
                        update evaluations set assignment_id=:assignmentId, class_id=:classId, attempt_id=:attemptId,
                            group_document_id=:groupDocumentId, account_id=:accountId, kind=:kind, status=:status,
                            submission_hash=:submissionHash, score=:score, max_score=:maxScore,
                            grading_document=cast(:document as jsonb), version=version+1
                        where id=:id and version=:expectedVersion returning version
                        """;
        var saved = namedParameterJdbcTemplate.query(sql, args,
                (rs, index) -> rs.getLong("version"));
        if (saved.isEmpty())
            throw createVersionConflict();
        row.version = saved.get(0);
        evaluation.version = row.version;
        return row.mapEvaluation();
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void lockIdentity(UUID classId, UUID assignmentId, UUID subjectId) {
        String key = classId + ":" + assignmentId + ":" + subjectId;
        namedParameterJdbcTemplate.query(
                "select pg_advisory_xact_lock(hashtextextended(cast(:key as text), 0))",
                Map.of("key", key), (rs, index) -> rs.getObject(1));
    }

    private static Evaluation mapEvaluation(ResultSet rs, int index) throws SQLException {
        var row = new EvaluationRow();
        row.document = rs.getString("grading_document");
        row.version = rs.getLong("version");
        return row.mapEvaluation();
    }

    private static ApiException createVersionConflict() {
        return ApiException.conflict("GRADE_VERSION_CONFLICT", "Reload evaluation");
    }

    private static Map<String, Object> mapParameters(EvaluationRow row) {
        var args = new HashMap<String, Object>();
        args.put("id", row.id);
        args.put("assignmentId", row.assignmentId);
        args.put("classId", row.classId);
        args.put("attemptId", row.attemptId);
        args.put("groupDocumentId", row.groupDocumentId);
        args.put("accountId", row.accountId);
        args.put("kind", row.kind);
        args.put("status", row.status);
        args.put("submissionHash", row.submissionHash);
        args.put("score", row.score);
        args.put("maxScore", row.maxScore);
        args.put("document", row.document);
        return args;
    }
}
