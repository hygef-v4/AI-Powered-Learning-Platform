package edu.aiplatform.groups.application;

import edu.aiplatform.academics.contract.ClassRef;
import edu.aiplatform.academics.contract.IClassAccess;
import edu.aiplatform.audit.contract.AuditEvent;
import edu.aiplatform.audit.contract.AuditResult;
import edu.aiplatform.audit.contract.IAudit;
import edu.aiplatform.groups.domain.ClassGroup;
import edu.aiplatform.groups.domain.GroupRepository;
import edu.aiplatform.groups.domain.LeaderChangeRequest;
import edu.aiplatform.groups.domain.LeaderChangeRequest.Status;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.jobs.contract.DomainEvent;
import edu.aiplatform.jobs.contract.EventTypes;
import edu.aiplatform.jobs.contract.IEventPublisher;
import edu.aiplatform.shared.error.ApiException;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Yêu cầu đổi trưởng nhóm (F3, F5; BR-U12-10–13): thành viên gửi và hủy, giảng viên chính của lớp duyệt hoặc từ
 * chối. Đổi trưởng nhóm không đổi mục ai đang giữ (BR-U12-13).
 */
@Service
public class LeaderRequestService {

    public static final int MAX_REASON = 1000;

    private final GroupRepository groups;
    private final GroupAccess access;
    private final IClassAccess classes;
    private final IAudit audit;
    private final IEventPublisher events;
    private final Clock clock;

    public LeaderRequestService(GroupRepository groups, GroupAccess access, IClassAccess classes, IAudit audit,
                                IEventPublisher events, Clock clock) {
        this.groups = groups;
        this.access = access;
        this.classes = classes;
        this.audit = audit;
        this.events = events;
        this.clock = clock;
    }

    @Transactional
    public RequestResult submit(Actor actor, UUID groupId, String reason, UUID proposedLeaderId) {
        ClassGroup group = groups.findGroup(groupId).orElseThrow(GroupAccess::notFound);
        ClassRef ref = access.requireStudent(actor, group.classId());
        if (!group.isMember(actor.accountId())) {
            throw GroupAccess.notFound();
        }
        GroupAccess.requireOpen(ref);
        // Lý do không bắt buộc (yêu cầu 2026-10-29): bỏ trống thì lưu chuỗi rỗng, tối đa 1.000 ký tự.
        String why = reason == null ? "" : reason.trim();
        if (why.length() > MAX_REASON) {
            throw ApiException.badRequest("LEADER_REQUEST_REASON_INVALID", "Lý do tối đa 1.000 ký tự");
        }
        if (proposedLeaderId != null && (!group.isMember(proposedLeaderId) || proposedLeaderId.equals(group.leaderId()))) {
            throw ApiException.badRequest("LEADER_REQUEST_PROPOSAL_INVALID",
                    "Người được đề cử phải là thành viên khác trưởng nhóm hiện tại");
        }
        if (groups.findPending(groupId).isPresent()) {
            throw pending();
        }
        Instant now = clock.instant();
        LeaderChangeRequest request = new LeaderChangeRequest(UUID.randomUUID(), groupId, actor.accountId(),
                proposedLeaderId, why, Status.PENDING, null, null, null, null, now);
        try {
            groups.insertRequest(request);
        } catch (DuplicateKeyException e) {
            throw pending();
        }
        Map<String, Object> details = details(group);
        details.put("proposedLeaderId", proposedLeaderId == null ? null : proposedLeaderId.toString());
        record(actor, "LEADER_REQUEST_SUBMITTED", request.id(), details);
        publish(EventTypes.GROUP_LEADER_CHANGE_REQUESTED, group, request.id(),
                ref.teacherId() == null ? List.of() : List.of(ref.teacherId()));
        return RequestResult.of(request);
    }

    @Transactional
    public RequestResult cancel(Actor actor, UUID requestId) {
        LeaderChangeRequest request = groups.lockRequest(requestId).orElseThrow(GroupAccess::notFound);
        ClassGroup group = groups.findGroup(request.groupId()).orElseThrow(GroupAccess::notFound);
        ClassRef ref = access.requireStudent(actor, group.classId());
        if (!request.requestedBy().equals(actor.accountId())) {
            throw GroupAccess.notFound();
        }
        GroupAccess.requireOpen(ref);
        requirePending(request);
        LeaderChangeRequest cancelled = request.decide(Status.CANCELLED, null, actor.accountId(), null, clock.instant());
        groups.updateRequest(cancelled);
        record(actor, "LEADER_REQUEST_CANCELLED", requestId, details(group));
        return RequestResult.of(cancelled);
    }

    /** Duyệt: chọn trưởng nhóm mới, mặc định người được đề cử; bắt buộc chọn khi không có đề cử (BR-U12-11). */
    @Transactional
    public RequestResult approve(Actor actor, UUID requestId, UUID newLeaderId) {
        LeaderChangeRequest request = groups.lockRequest(requestId).orElseThrow(GroupAccess::notFound);
        ClassGroup group = groups.findGroup(request.groupId()).orElseThrow(GroupAccess::notFound);
        ClassRef ref = access.requireTeacher(actor, group.classId());
        GroupAccess.requireEditable(ref);
        requirePending(request);
        UUID leader = newLeaderId != null ? newLeaderId : request.proposedLeaderId();
        if (leader == null) {
            throw ApiException.badRequest("LEADER_REQUIRED", "Cần chọn trưởng nhóm mới");
        }
        if (!group.isMember(leader) || !activeStudent(leader, group.classId())) {
            throw ApiException.badRequest("LEADER_NOT_MEMBER", "Trưởng nhóm mới phải là thành viên đang học của nhóm");
        }
        Instant now = clock.instant();
        groups.setLeader(group.id(), leader, now);
        groups.bumpSetVersion(group.classId(), groups.setVersion(group.classId()), now);
        LeaderChangeRequest approved = request.decide(Status.APPROVED, leader, actor.accountId(), null, now);
        groups.updateRequest(approved);
        Map<String, Object> change = details(group);
        change.put("before", group.leaderId() == null ? null : group.leaderId().toString());
        change.put("after", leader.toString());
        record(actor, "LEADER_REQUEST_APPROVED", requestId, change);
        record(actor, "GROUP_LEADER_CHANGED", group.id(), change);
        publish(EventTypes.GROUP_LEADER_CHANGED, group, requestId, group.memberIds());
        return RequestResult.of(approved);
    }

    @Transactional
    public RequestResult reject(Actor actor, UUID requestId, String reason) {
        LeaderChangeRequest request = groups.lockRequest(requestId).orElseThrow(GroupAccess::notFound);
        ClassGroup group = groups.findGroup(request.groupId()).orElseThrow(GroupAccess::notFound);
        ClassRef ref = access.requireTeacher(actor, group.classId());
        GroupAccess.requireEditable(ref);
        requirePending(request);
        // Từ chối không cần lý do (yêu cầu 2026-10-29); có gửi thì vẫn lưu, tối đa 1.000 ký tự.
        String why = reason == null || reason.isBlank() ? null : reason.trim();
        if (why != null && why.length() > MAX_REASON) {
            throw ApiException.badRequest("REJECT_REASON_INVALID", "Lý do từ chối tối đa 1.000 ký tự");
        }
        LeaderChangeRequest rejected = request.decide(Status.REJECTED, null, actor.accountId(), why, clock.instant());
        groups.updateRequest(rejected);
        Map<String, Object> details = details(group);
        details.put("reason", why);
        record(actor, "LEADER_REQUEST_REJECTED", requestId, details);
        publish(EventTypes.GROUP_LEADER_REQUEST_REJECTED, group, requestId, List.of(request.requestedBy()));
        return RequestResult.of(rejected);
    }

    private boolean activeStudent(UUID accountId, UUID classId) {
        try {
            return classes.isActiveStudent(accountId, classId);
        } catch (RuntimeException e) {
            throw ApiException.unavailable("GROUP_DEPENDENCY_UNAVAILABLE", "Hệ thống tạm bận, vui lòng thử lại sau");
        }
    }

    private static void requirePending(LeaderChangeRequest request) {
        if (request.status() != Status.PENDING) {
            throw ApiException.conflict("LEADER_REQUEST_CLOSED", "Yêu cầu đã được xử lý hoặc đã hủy");
        }
    }

    private static ApiException pending() {
        return ApiException.conflict("LEADER_REQUEST_PENDING", "Nhóm đang có một yêu cầu đổi trưởng nhóm chờ duyệt");
    }

    private static Map<String, Object> details(ClassGroup group) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("classId", group.classId().toString());
        details.put("groupId", group.id().toString());
        return details;
    }

    private void record(Actor actor, String action, UUID objectId, Map<String, Object> details) {
        audit.record(new AuditEvent(UUID.randomUUID(), actor.accountId(), action, "LEADER_CHANGE_REQUEST",
                objectId.toString(), AuditResult.SUCCESS, null, details, clock.instant()));
    }

    private void publish(String type, ClassGroup group, UUID requestId, List<UUID> recipients) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("classId", group.classId().toString());
        payload.put("groupId", group.id().toString());
        payload.put("requestId", requestId.toString());
        payload.put("recipientIds", recipients.stream().map(UUID::toString).toList());
        events.publish(new DomainEvent(1, UUID.randomUUID(), type, "U12", clock.instant(), null, payload));
    }

    public record RequestResult(UUID id, UUID groupId, Status status, UUID proposedLeaderId, UUID newLeaderId,
                                String decisionReason, Instant createdAt, Instant decidedAt) {

        static RequestResult of(LeaderChangeRequest r) {
            return new RequestResult(r.id(), r.groupId(), r.status(), r.proposedLeaderId(), r.newLeaderId(),
                    r.decisionReason(), r.createdAt(), r.decidedAt());
        }
    }
}
