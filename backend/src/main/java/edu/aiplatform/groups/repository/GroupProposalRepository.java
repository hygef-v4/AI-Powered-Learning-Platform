package edu.aiplatform.groups.repository;

import edu.aiplatform.groups.model.GroupFormationSettings;
import edu.aiplatform.groups.model.GroupProposal;
import edu.aiplatform.groups.model.ProposalMember;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Đợt tự chia nhóm, đề xuất nhóm và lời mời của U12 (BR-U12-70…96); không xóa dòng để giữ lịch sử. */
@Repository
public class GroupProposalRepository {

    private static final String PROPOSAL_COLUMNS = "p.id, p.class_id, p.name, p.leader_id, p.status, p.submitted_at, "
            + "p.decided_by, p.decided_at, p.decision_reason, p.approved_group_id, p.end_reason, p.created_at, "
            + "p.updated_at, p.version";
    private static final String MEMBER_COLUMNS = "m.proposal_id, m.class_id, m.account_id, m.status, m.active, "
            + "m.invited_by, m.created_at, m.responded_at";

    private final JdbcClient jdbc;

    public GroupProposalRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    // ---- cài đặt đợt ------------------------------------------------------------------------------------------

    public Optional<GroupFormationSettings> findSettings(UUID classId) {
        return jdbc.sql("select class_id, enabled, min_size, max_size, deadline, updated_by, updated_at, version "
                        + "from group_formation_settings where class_id = :classId")
                .param("classId", classId).query(GroupProposalRepository::settings).optional();
    }

    /**
     * Ghi cài đặt khi {@code version} hiện tại khớp {@code expected}; lớp chưa có dòng thì tạo khi {@code expected} =
     * 0. Dòng mới mang {@code version} của {@code settings}.
     */
    public boolean saveSettings(GroupFormationSettings settings, long expected) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("classId", settings.classId());
        params.put("enabled", settings.enabled());
        params.put("minSize", settings.minSize());
        params.put("maxSize", settings.maxSize());
        params.put("deadline", timestamp(settings.deadline()));
        params.put("updatedBy", settings.updatedBy());
        params.put("updatedAt", timestamp(settings.updatedAt()));
        params.put("version", settings.version());
        params.put("expected", expected);
        if (expected == 0) {
            int inserted = jdbc.sql("insert into group_formation_settings (class_id, enabled, min_size, max_size, "
                            + "deadline, updated_by, updated_at, version) values (:classId, :enabled, :minSize, "
                            + ":maxSize, :deadline, :updatedBy, :updatedAt, :version) on conflict (class_id) do nothing")
                    .params(params).update();
            if (inserted == 1) {
                return true;
            }
        }
        return jdbc.sql("update group_formation_settings set enabled = :enabled, min_size = :minSize, "
                        + "max_size = :maxSize, deadline = :deadline, updated_by = :updatedBy, "
                        + "updated_at = :updatedAt, version = :version where class_id = :classId and version = :expected")
                .params(params).update() == 1;
    }

    // ---- đề xuất ------------------------------------------------------------------------------------------------

    public void insertProposal(GroupProposal proposal) {
        jdbc.sql("insert into group_proposals (id, class_id, name, leader_id, status, submitted_at, decided_by, "
                        + "decided_at, decision_reason, approved_group_id, end_reason, created_at, updated_at, version) "
                        + "values (:id, :classId, :name, :leaderId, :status, :submittedAt, :decidedBy, :decidedAt, "
                        + ":decisionReason, :approvedGroupId, :endReason, :createdAt, :updatedAt, :version)")
                .params(proposalParams(proposal)).update();
    }

    /** Ghi mọi trường thay đổi được của đề xuất đang giữ khóa. */
    public void updateProposal(GroupProposal proposal) {
        jdbc.sql("update group_proposals set name = :name, status = :status, submitted_at = :submittedAt, "
                        + "decided_by = :decidedBy, decided_at = :decidedAt, decision_reason = :decisionReason, "
                        + "approved_group_id = :approvedGroupId, end_reason = :endReason, updated_at = :updatedAt, "
                        + "version = :version where id = :id")
                .params(proposalParams(proposal)).update();
    }

    /** Khóa dòng đề xuất để các thao tác trên cùng đề xuất không chạy chồng nhau. */
    public Optional<GroupProposal> lockProposal(UUID proposalId) {
        return jdbc.sql("select " + PROPOSAL_COLUMNS + " from group_proposals p where p.id = :id for update")
                .param("id", proposalId).query(GroupProposalRepository::proposal).optional();
    }

    public List<GroupProposal> listActiveProposals(UUID classId) {
        return jdbc.sql("select " + PROPOSAL_COLUMNS + " from group_proposals p where p.class_id = :classId "
                        + "and p.status in ('DRAFT', 'SUBMITTED') order by p.created_at, p.id")
                .param("classId", classId).query(GroupProposalRepository::proposal).list();
    }

    /** Đề xuất đang hoạt động mà sinh viên là thành viên. */
    public Optional<GroupProposal> findActiveProposalOf(UUID classId, UUID accountId) {
        return jdbc.sql("select " + PROPOSAL_COLUMNS + " from group_proposals p join group_proposal_members m "
                        + "on m.proposal_id = p.id where m.class_id = :classId and m.account_id = :accountId "
                        + "and m.active")
                .param("classId", classId).param("accountId", accountId)
                .query(GroupProposalRepository::proposal).optional();
    }

    /** Đề xuất bị từ chối gần nhất mà sinh viên là thành viên lúc gửi duyệt (BR-U12-86). */
    public Optional<GroupProposal> findLatestRejectedOf(UUID classId, UUID accountId) {
        return jdbc.sql("select " + PROPOSAL_COLUMNS + " from group_proposals p join group_proposal_members m "
                        + "on m.proposal_id = p.id where m.class_id = :classId and m.account_id = :accountId "
                        + "and m.status = 'ACCEPTED' and p.status = 'REJECTED' order by p.decided_at desc limit 1")
                .param("classId", classId).param("accountId", accountId)
                .query(GroupProposalRepository::proposal).optional();
    }

    /** Đề xuất đang hoạt động có lời mời chờ gửi tới sinh viên. */
    public List<GroupProposal> listInvitingProposals(UUID classId, UUID accountId) {
        return jdbc.sql("select " + PROPOSAL_COLUMNS + " from group_proposals p join group_proposal_members m "
                        + "on m.proposal_id = p.id where m.class_id = :classId and m.account_id = :accountId "
                        + "and m.status = 'INVITED' and p.status in ('DRAFT', 'SUBMITTED') order by m.created_at")
                .param("classId", classId).param("accountId", accountId)
                .query(GroupProposalRepository::proposal).list();
    }

    // ---- thành viên và lời mời -----------------------------------------------------------------------------------

    public List<ProposalMember> listMembers(UUID proposalId) {
        return jdbc.sql("select " + MEMBER_COLUMNS + " from group_proposal_members m where m.proposal_id = :id "
                        + "order by m.created_at, m.account_id")
                .param("id", proposalId).query(GroupProposalRepository::member).list();
    }

    public List<ProposalMember> listMembersOf(Collection<UUID> proposalIds) {
        if (proposalIds.isEmpty()) {
            return List.of();
        }
        return jdbc.sql("select " + MEMBER_COLUMNS + " from group_proposal_members m where m.proposal_id in (:ids) "
                        + "order by m.created_at, m.account_id")
                .param("ids", proposalIds).query(GroupProposalRepository::member).list();
    }

    /** Dòng thành viên đang hoạt động của các sinh viên trong lớp. */
    public List<ProposalMember> listActiveMemberships(UUID classId, Collection<UUID> accountIds) {
        if (accountIds.isEmpty()) {
            return List.of();
        }
        return jdbc.sql("select " + MEMBER_COLUMNS + " from group_proposal_members m where m.class_id = :classId "
                        + "and m.account_id in (:ids) and m.active")
                .param("classId", classId).param("ids", accountIds).query(GroupProposalRepository::member).list();
    }

    /** Sinh viên đang ở đề xuất đang hoạt động nào của lớp: tài khoản → đề xuất. */
    public Map<UUID, UUID> mapActiveMemberships(UUID classId) {
        Map<UUID, UUID> result = new LinkedHashMap<>();
        jdbc.sql("select account_id, proposal_id from group_proposal_members where class_id = :classId and active")
                .param("classId", classId)
                .query((rs, n) -> result.put(rs.getObject("account_id", UUID.class),
                        rs.getObject("proposal_id", UUID.class)))
                .list();
        return result;
    }

    /** Thêm thành viên đang hoạt động; trùng chỉ mục một-đề-xuất-mỗi-sinh-viên thì ném {@code DuplicateKeyException}. */
    public void insertActiveMember(UUID proposalId, UUID classId, UUID accountId, Instant now) {
        jdbc.sql("insert into group_proposal_members (proposal_id, class_id, account_id, status, active, invited_by, "
                        + "created_at, responded_at) values (:proposalId, :classId, :accountId, 'ACCEPTED', true, null, "
                        + ":now, :now)")
                .param("proposalId", proposalId).param("classId", classId).param("accountId", accountId)
                .param("now", Timestamp.from(now)).update();
    }

    /** Mời, hoặc mời lại người đã từ chối, bị thu hồi hay hết hạn: dòng cũ quay về {@code INVITED}. */
    public void upsertInvitation(UUID proposalId, UUID classId, UUID accountId, UUID invitedBy, Instant now) {
        jdbc.sql("insert into group_proposal_members (proposal_id, class_id, account_id, status, active, invited_by, "
                        + "created_at, responded_at) values (:proposalId, :classId, :accountId, 'INVITED', false, "
                        + ":invitedBy, :now, null) on conflict (proposal_id, account_id) do update set "
                        + "status = 'INVITED', active = false, invited_by = :invitedBy, created_at = :now, "
                        + "responded_at = null")
                .param("proposalId", proposalId).param("classId", classId).param("accountId", accountId)
                .param("invitedBy", invitedBy).param("now", Timestamp.from(now)).update();
    }

    /** Đổi trạng thái một dòng; trùng chỉ mục khi chuyển sang hoạt động thì ném {@code DuplicateKeyException}. */
    public void updateMember(UUID proposalId, UUID accountId, ProposalMember.Status status, boolean active,
                             Instant now) {
        jdbc.sql("update group_proposal_members set status = :status, active = :active, responded_at = :now "
                        + "where proposal_id = :proposalId and account_id = :accountId")
                .param("proposalId", proposalId).param("accountId", accountId).param("status", status.name())
                .param("active", active).param("now", Timestamp.from(now)).update();
    }

    /** Đề xuất kết thúc: mọi thành viên thôi hoạt động, giữ trạng thái làm lịch sử. */
    public void deactivateMembers(UUID proposalId) {
        jdbc.sql("update group_proposal_members set active = false where proposal_id = :id and active")
                .param("id", proposalId).update();
    }

    /** Lời mời đang chờ của một đề xuất chuyển sang {@code to} ({@code REVOKED} hoặc {@code EXPIRED}). */
    public int closeInvitations(UUID proposalId, ProposalMember.Status to, Instant now) {
        return jdbc.sql("update group_proposal_members set status = :to, responded_at = :now "
                        + "where proposal_id = :id and status = 'INVITED'")
                .param("id", proposalId).param("to", to.name()).param("now", Timestamp.from(now)).update();
    }

    /** Thu hồi lời mời đang chờ của các sinh viên trong lớp, trừ đề xuất {@code exceptProposalId} (có thể trống). */
    public int revokeInvitationsOf(UUID classId, Collection<UUID> accountIds, UUID exceptProposalId, Instant now) {
        if (accountIds.isEmpty()) {
            return 0;
        }
        return jdbc.sql("update group_proposal_members set status = 'REVOKED', responded_at = :now "
                        + "where class_id = :classId and account_id in (:ids) and status = 'INVITED' "
                        + "and (cast(:except as uuid) is null or proposal_id <> cast(:except as uuid))")
                .param("classId", classId).param("ids", accountIds).param("except", exceptProposalId)
                .param("now", Timestamp.from(now)).update();
    }

    /** Lời mời đang chờ của cả lớp hết hạn, dùng khi mở lại đợt (BR-U12-72). */
    public int expireInvitations(UUID classId, Instant now) {
        return jdbc.sql("update group_proposal_members set status = 'EXPIRED', responded_at = :now "
                        + "where class_id = :classId and status = 'INVITED'")
                .param("classId", classId).param("now", Timestamp.from(now)).update();
    }

    // ---- ánh xạ -------------------------------------------------------------------------------------------------

    private static Map<String, Object> proposalParams(GroupProposal p) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("id", p.id());
        params.put("classId", p.classId());
        params.put("name", p.name());
        params.put("leaderId", p.leaderId());
        params.put("status", p.status().name());
        params.put("submittedAt", timestamp(p.submittedAt()));
        params.put("decidedBy", p.decidedBy());
        params.put("decidedAt", timestamp(p.decidedAt()));
        params.put("decisionReason", p.decisionReason());
        params.put("approvedGroupId", p.approvedGroupId());
        params.put("endReason", p.endReason() == null ? null : p.endReason().name());
        params.put("createdAt", timestamp(p.createdAt()));
        params.put("updatedAt", timestamp(p.updatedAt()));
        params.put("version", p.version());
        return params;
    }

    private static GroupFormationSettings settings(ResultSet rs, int n) throws SQLException {
        return new GroupFormationSettings(rs.getObject("class_id", UUID.class), rs.getBoolean("enabled"),
                rs.getInt("min_size"), rs.getInt("max_size"), instant(rs, "deadline"),
                rs.getObject("updated_by", UUID.class), instant(rs, "updated_at"), rs.getLong("version"));
    }

    private static GroupProposal proposal(ResultSet rs, int n) throws SQLException {
        String endReason = rs.getString("end_reason");
        return new GroupProposal(rs.getObject("id", UUID.class), rs.getObject("class_id", UUID.class),
                rs.getString("name"), rs.getObject("leader_id", UUID.class),
                GroupProposal.Status.valueOf(rs.getString("status")), instant(rs, "submitted_at"),
                rs.getObject("decided_by", UUID.class), instant(rs, "decided_at"), rs.getString("decision_reason"),
                rs.getObject("approved_group_id", UUID.class),
                endReason == null ? null : GroupProposal.EndReason.valueOf(endReason), instant(rs, "created_at"),
                instant(rs, "updated_at"), rs.getLong("version"));
    }

    private static ProposalMember member(ResultSet rs, int n) throws SQLException {
        return new ProposalMember(rs.getObject("proposal_id", UUID.class), rs.getObject("class_id", UUID.class),
                rs.getObject("account_id", UUID.class), ProposalMember.Status.valueOf(rs.getString("status")),
                rs.getBoolean("active"), rs.getObject("invited_by", UUID.class), instant(rs, "created_at"),
                instant(rs, "responded_at"));
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }

    private static Timestamp timestamp(Instant value) {
        return value == null ? null : Timestamp.from(value);
    }
}
