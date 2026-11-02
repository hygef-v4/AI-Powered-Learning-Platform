package edu.aiplatform.groupdocs.repository;

import edu.aiplatform.groupdocs.model.PeerReview;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Đánh giá chéo của thành viên trong tài liệu nhóm; chỉ dùng trong U12. */
@Repository
public class GroupPeerReviewRepository {

    private static final String COLUMNS = "id, group_document_id, reviewer_id, reviewee_id, score, comment, created_at, "
            + "updated_at";

    private final JdbcClient jdbc;

    public GroupPeerReviewRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public List<PeerReview> listByDocument(UUID documentId) {
        return jdbc.sql("select " + COLUMNS + " from group_peer_reviews where group_document_id = :id "
                        + "order by reviewee_id, created_at")
                .param("id", documentId).query(GroupPeerReviewRepository::review).list();
    }

    /** Một người chấm một người một lần; chấm lại thì thay điểm và nhận xét. */
    public void upsert(PeerReview r) {
        jdbc.sql("insert into group_peer_reviews (" + COLUMNS + ") values (:id, :documentId, :reviewer, :reviewee, "
                        + ":score, :comment, :createdAt, :updatedAt) on conflict (group_document_id, reviewer_id, reviewee_id) "
                        + "do update set score = :score, comment = :comment, updated_at = :updatedAt")
                .param("id", r.id()).param("documentId", r.documentId()).param("reviewer", r.reviewerId())
                .param("reviewee", r.revieweeId()).param("score", r.score()).param("comment", r.comment())
                .param("createdAt", Timestamp.from(r.createdAt())).param("updatedAt", Timestamp.from(r.updatedAt()))
                .update();
    }

    private static PeerReview review(ResultSet rs, int n) throws SQLException {
        return new PeerReview(rs.getObject("id", UUID.class), rs.getObject("group_document_id", UUID.class),
                rs.getObject("reviewer_id", UUID.class), rs.getObject("reviewee_id", UUID.class), rs.getInt("score"),
                rs.getString("comment"), rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant());
    }
}
