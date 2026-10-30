package edu.aiplatform.assignmentwork.repository;

import edu.aiplatform.assignmentwork.model.ActivityEvent;

import edu.aiplatform.assignmentwork.contract.ActivityKind;
import edu.aiplatform.assignmentwork.contract.WorkKind;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Bảng {@code activity_events}: ghi {@code on conflict do nothing}, đọc theo công việc, dọn theo lô (P4, P5). */
@Repository
public class ActivityEventRepository {

    private final JdbcClient jdbc;

    public ActivityEventRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Trả 1 nếu ghi, 0 nếu trùng {@code clientEventId}. */
    public int insertIgnore(ActivityEvent e) {
        return jdbc.sql("insert into activity_events (id, account_id, class_id, work_kind, work_id, session_id, "
                        + "client_event_id, sequence, kind, client_time, server_time, policy_version) values (:id, "
                        + ":accountId, :classId, :workKind, :workId, :sessionId, :clientEventId, :sequence, :kind, "
                        + ":clientTime, :serverTime, :policyVersion) "
                        + "on conflict (account_id, work_kind, work_id, client_event_id) do nothing")
                .param("id", e.id()).param("accountId", e.accountId()).param("classId", e.classId())
                .param("workKind", e.workKind().name()).param("workId", e.workId()).param("sessionId", e.sessionId())
                .param("clientEventId", e.clientEventId()).param("sequence", e.sequence())
                .param("kind", e.kind().name()).param("clientTime", AttemptRepository.ts(e.clientTime()))
                .param("serverTime", AttemptRepository.ts(e.serverTime())).param("policyVersion", e.policyVersion())
                .update();
    }

    public List<ActivityEvent> list(WorkKind kind, UUID workId, UUID accountId) {
        return jdbc.sql("select * from activity_events where work_kind = :kind and work_id = :workId "
                        + "and account_id = :accountId order by server_time, sequence")
                .param("kind", kind.name()).param("workId", workId).param("accountId", accountId)
                .query(ActivityEventRepository::map).list();
    }

    /** Xóa tối đa {@code limit} sự kiện cũ hơn {@code cutoff} theo index giờ server (P5). */
    public int deleteOlderThan(Instant cutoff, int limit) {
        return jdbc.sql("delete from activity_events where id in (select id from activity_events "
                        + "where server_time < :cutoff order by server_time limit :limit)")
                .param("cutoff", AttemptRepository.ts(cutoff)).param("limit", limit).update();
    }

    private static ActivityEvent map(ResultSet rs, int row) throws SQLException {
        return new ActivityEvent(rs.getObject("id", UUID.class), rs.getObject("account_id", UUID.class),
                rs.getObject("class_id", UUID.class), WorkKind.valueOf(rs.getString("work_kind")),
                rs.getObject("work_id", UUID.class), rs.getObject("session_id", UUID.class),
                rs.getString("client_event_id"), rs.getLong("sequence"), ActivityKind.valueOf(rs.getString("kind")),
                AttemptRepository.instant(rs, "client_time"), AttemptRepository.instant(rs, "server_time"),
                rs.getInt("policy_version"));
    }
}
