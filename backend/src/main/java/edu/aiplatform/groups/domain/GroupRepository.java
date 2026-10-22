package edu.aiplatform.groups.domain;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Bảng nhóm, thành viên, version bộ nhóm và yêu cầu đổi trưởng nhóm; chỉ dùng trong U12. */
@Repository
public class GroupRepository {

    private static final String REQUEST_COLUMNS = "id, group_id, requested_by, proposed_leader_id, reason, status, "
            + "new_leader_id, decided_by, decided_at, decision_reason, created_at";

    private final JdbcClient jdbc;

    public GroupRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    // ---- version bộ nhóm (P2) ------------------------------------------------------------------------------

    public long setVersion(UUID classId) {
        return jdbc.sql("select version from class_group_sets where class_id = :classId").param("classId", classId)
                .query(Long.class).optional().orElse(0L);
    }

    /** Tăng version khi khớp {@code expected}; lớp chưa có dòng thì tạo với version 1 khi {@code expected} = 0. */
    public boolean bumpSetVersion(UUID classId, long expected, Instant now) {
        if (expected == 0) {
            int inserted = jdbc.sql("insert into class_group_sets (class_id, version, updated_at) values (:classId, 1, :now) "
                            + "on conflict (class_id) do nothing")
                    .param("classId", classId).param("now", Timestamp.from(now)).update();
            if (inserted == 1) {
                return true;
            }
        }
        return jdbc.sql("update class_group_sets set version = version + 1, updated_at = :now "
                        + "where class_id = :classId and version = :expected")
                .param("classId", classId).param("expected", expected).param("now", Timestamp.from(now)).update() == 1;
    }

    // ---- nhóm và thành viên ----------------------------------------------------------------------------------

    public List<ClassGroup> listGroups(UUID classId) {
        Map<UUID, List<UUID>> members = new LinkedHashMap<>();
        jdbc.sql("select group_id, account_id from class_group_members where class_id = :classId order by joined_at, account_id")
                .param("classId", classId)
                .query((rs, n) -> members.computeIfAbsent(rs.getObject("group_id", UUID.class), k -> new ArrayList<>())
                        .add(rs.getObject("account_id", UUID.class)))
                .list();
        return jdbc.sql("select id, class_id, name, leader_id, created_by, created_at, updated_at from class_groups "
                        + "where class_id = :classId order by created_at, name")
                .param("classId", classId)
                .query((rs, n) -> group(rs, members.getOrDefault(rs.getObject("id", UUID.class), List.of())))
                .list();
    }

    public Optional<ClassGroup> findGroup(UUID groupId) {
        List<UUID> members = jdbc.sql("select account_id from class_group_members where group_id = :groupId "
                        + "order by joined_at, account_id")
                .param("groupId", groupId).query(UUID.class).list();
        return jdbc.sql("select id, class_id, name, leader_id, created_by, created_at, updated_at from class_groups "
                        + "where id = :groupId")
                .param("groupId", groupId).query((rs, n) -> group(rs, members)).optional();
    }

    public Optional<ClassGroup> findGroupOf(UUID classId, UUID accountId) {
        return jdbc.sql("select group_id from class_group_members where class_id = :classId and account_id = :accountId")
                .param("classId", classId).param("accountId", accountId).query(UUID.class).optional()
                .flatMap(this::findGroup);
    }

    /** Nhóm của một người ở mọi lớp (mỗi lớp tối đa một nhóm). */
    public List<UUID> listGroupIdsOf(UUID accountId) {
        return jdbc.sql("select group_id from class_group_members where account_id = :accountId")
                .param("accountId", accountId).query(UUID.class).list();
    }

    public void insertGroup(ClassGroup group) {
        jdbc.sql("insert into class_groups (id, class_id, name, leader_id, created_by, created_at, updated_at) "
                        + "values (:id, :classId, :name, :leaderId, :createdBy, :createdAt, :updatedAt)")
                .param("id", group.id()).param("classId", group.classId()).param("name", group.name())
                .param("leaderId", group.leaderId()).param("createdBy", group.createdBy())
                .param("createdAt", Timestamp.from(group.createdAt())).param("updatedAt", Timestamp.from(group.updatedAt()))
                .update();
    }

    public void updateGroup(UUID groupId, String name, UUID leaderId, Instant now) {
        jdbc.sql("update class_groups set name = :name, leader_id = :leaderId, updated_at = :now where id = :id")
                .param("id", groupId).param("name", name).param("leaderId", leaderId).param("now", Timestamp.from(now))
                .update();
    }

    public void setLeader(UUID groupId, UUID leaderId, Instant now) {
        jdbc.sql("update class_groups set leader_id = :leaderId, updated_at = :now where id = :id")
                .param("id", groupId).param("leaderId", leaderId).param("now", Timestamp.from(now)).update();
    }

    public void deleteGroup(UUID groupId) {
        jdbc.sql("delete from class_groups where id = :id").param("id", groupId).update();
    }

    /** Đổi tên tạm để tránh vi phạm unique tên khi các nhóm đổi tên cho nhau trong cùng lần lưu. */
    public void renameTemporarily(UUID groupId) {
        jdbc.sql("update class_groups set name = :name where id = :id").param("id", groupId)
                .param("name", "~" + groupId).update();
    }

    public void removeMembers(UUID classId, Collection<UUID> accountIds) {
        if (accountIds.isEmpty()) {
            return;
        }
        jdbc.sql("delete from class_group_members where class_id = :classId and account_id in (:ids)")
                .param("classId", classId).param("ids", accountIds).update();
    }

    public void addMember(UUID groupId, UUID classId, UUID accountId, Instant now) {
        jdbc.sql("insert into class_group_members (group_id, class_id, account_id, joined_at) "
                        + "values (:groupId, :classId, :accountId, :now)")
                .param("groupId", groupId).param("classId", classId).param("accountId", accountId)
                .param("now", Timestamp.from(now)).update();
    }

    // ---- yêu cầu đổi trưởng nhóm -------------------------------------------------------------------------------

    public void insertRequest(LeaderChangeRequest r) {
        jdbc.sql("insert into leader_change_requests (" + REQUEST_COLUMNS + ") values (:id, :groupId, :requestedBy, "
                        + ":proposed, :reason, :status, :newLeader, :decidedBy, :decidedAt, :decisionReason, :createdAt)")
                .params(requestParams(r)).update();
    }

    public void updateRequest(LeaderChangeRequest r) {
        jdbc.sql("update leader_change_requests set status = :status, new_leader_id = :newLeader, decided_by = :decidedBy, "
                        + "decided_at = :decidedAt, decision_reason = :decisionReason where id = :id")
                .params(requestParams(r)).update();
    }

    /** Khóa dòng yêu cầu để duyệt, từ chối, hủy không chạy chồng nhau. */
    public Optional<LeaderChangeRequest> lockRequest(UUID requestId) {
        return jdbc.sql("select " + REQUEST_COLUMNS + " from leader_change_requests where id = :id for update")
                .param("id", requestId).query(GroupRepository::request).optional();
    }

    public Optional<LeaderChangeRequest> findPending(UUID groupId) {
        return jdbc.sql("select " + REQUEST_COLUMNS + " from leader_change_requests where group_id = :groupId "
                        + "and status = 'PENDING'")
                .param("groupId", groupId).query(GroupRepository::request).optional();
    }

    public List<LeaderChangeRequest> listPendingOfClass(UUID classId) {
        return jdbc.sql("select r.id, r.group_id, r.requested_by, r.proposed_leader_id, r.reason, r.status, r.new_leader_id, "
                        + "r.decided_by, r.decided_at, r.decision_reason, r.created_at from leader_change_requests r "
                        + "join class_groups g on g.id = r.group_id where g.class_id = :classId and r.status = 'PENDING' "
                        + "order by r.created_at")
                .param("classId", classId).query(GroupRepository::request).list();
    }

    /** Hai yêu cầu gần nhất của một người trong nhóm (BR-U12-50: gần nhất và dòng tóm tắt lần trước). */
    public List<LeaderChangeRequest> latestOf(UUID groupId, UUID accountId, int limit) {
        return jdbc.sql("select " + REQUEST_COLUMNS + " from leader_change_requests where group_id = :groupId "
                        + "and requested_by = :accountId order by created_at desc limit :limit")
                .param("groupId", groupId).param("accountId", accountId).param("limit", limit)
                .query(GroupRepository::request).list();
    }

    public void cancelPendingOf(UUID groupId, UUID decidedBy, String why, Instant now) {
        jdbc.sql("update leader_change_requests set status = 'CANCELLED', decided_by = :by, decided_at = :now, "
                        + "decision_reason = :why where group_id = :groupId and status = 'PENDING'")
                .param("groupId", groupId).param("by", decidedBy).param("now", Timestamp.from(now)).param("why", why)
                .update();
    }

    private static Map<String, Object> requestParams(LeaderChangeRequest r) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("id", r.id());
        p.put("groupId", r.groupId());
        p.put("requestedBy", r.requestedBy());
        p.put("proposed", r.proposedLeaderId());
        p.put("reason", r.reason());
        p.put("status", r.status().name());
        p.put("newLeader", r.newLeaderId());
        p.put("decidedBy", r.decidedBy());
        p.put("decidedAt", r.decidedAt() == null ? null : Timestamp.from(r.decidedAt()));
        p.put("decisionReason", r.decisionReason());
        p.put("createdAt", Timestamp.from(r.createdAt()));
        return p;
    }

    private static ClassGroup group(ResultSet rs, List<UUID> members) throws SQLException {
        return new ClassGroup(rs.getObject("id", UUID.class), rs.getObject("class_id", UUID.class), rs.getString("name"),
                rs.getObject("leader_id", UUID.class), rs.getObject("created_by", UUID.class),
                rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant(), members);
    }

    private static LeaderChangeRequest request(ResultSet rs, int n) throws SQLException {
        Timestamp decided = rs.getTimestamp("decided_at");
        return new LeaderChangeRequest(rs.getObject("id", UUID.class), rs.getObject("group_id", UUID.class),
                rs.getObject("requested_by", UUID.class), rs.getObject("proposed_leader_id", UUID.class),
                rs.getString("reason"), LeaderChangeRequest.Status.valueOf(rs.getString("status")),
                rs.getObject("new_leader_id", UUID.class), rs.getObject("decided_by", UUID.class),
                decided == null ? null : decided.toInstant(), rs.getString("decision_reason"),
                rs.getTimestamp("created_at").toInstant());
    }
}
