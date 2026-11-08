package edu.aiplatform.grading.repository;

import edu.aiplatform.grading.model.GradingProposal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class GradingProposalRepository implements IGradingProposalRepository {
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    /** Creates the repository with named-parameter JDBC access. */
    public GradingProposalRepository(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    /** Finds the evaluation identifier linked to a proposal. */
    public Optional<UUID> findEvaluationId(UUID id) {
        return namedParameterJdbcTemplate
                .queryForList("select evaluation_id from grading_proposals where id=:id",
                        Map.of("id", id), UUID.class)
                .stream().findFirst();
    }

    /** Loads a grading proposal by its identifier. */
    public Optional<GradingProposal> findById(UUID id) {
        return namedParameterJdbcTemplate
                .query("select proposal_document from grading_proposals where id=:id",
                        Map.of("id", id), GradingProposalRepository::mapProposal)
                .stream().findFirst();
    }

    /** Loads a grading proposal by its request reference. */
    public Optional<GradingProposal> findByRequestRef(String ref) {
        return namedParameterJdbcTemplate
                .query("select proposal_document from grading_proposals where request_ref=:ref",
                        Map.of("ref", ref), GradingProposalRepository::mapProposal)
                .stream().findFirst();
    }

    /** Lists proposals for an evaluation from newest to oldest. */
    public List<GradingProposal> findForEvaluation(UUID id) {
        return namedParameterJdbcTemplate.query(
                """
                        select proposal_document from grading_proposals where evaluation_id=:id order by version_no desc
                        """,
                Map.of("id", id), GradingProposalRepository::mapProposal);
    }

    /** Inserts or updates a grading proposal using optimistic version checks. */
    @Transactional
    public GradingProposal save(GradingProposal proposal) {
        var versions = namedParameterJdbcTemplate.queryForList(
                "select version from grading_proposals where id=:id for update",
                Map.of("id", proposal.id), Long.class);
        var row = new GradingProposalRow();
        row.applyProposal(proposal);
        var args = Map.of("id", row.id, "evaluationId", row.evaluationId, "versionNo",
                row.versionNo, "requestRef", row.requestRef, "status", row.status, "document",
                row.document, "expectedVersion", versions.isEmpty() ? 0 : versions.get(0));
        String sql = versions.isEmpty()
                ? """
                        insert into grading_proposals (id, evaluation_id, version_no, request_ref, status, proposal_document, version)
                        values (:id, :evaluationId, :versionNo, :requestRef, :status, cast(:document as jsonb), 0)
                        """
                : """
                        update grading_proposals set evaluation_id=:evaluationId, version_no=:versionNo,
                            request_ref=:requestRef, status=:status, proposal_document=cast(:document as jsonb), version=version+1
                        where id=:id and version=:expectedVersion
                        """;
        if (namedParameterJdbcTemplate.update(sql, args) != 1) {
            throw new OptimisticLockingFailureException(
                    "Grading proposal changed; reload and retry");
        }
        return row.mapProposal();
    }

    /** Maps a database row to its grading proposal document. */
    private static GradingProposal mapProposal(ResultSet rs, int index) throws SQLException {
        var row = new GradingProposalRow();
        row.document = rs.getString("proposal_document");
        return row.mapProposal();
    }
}
