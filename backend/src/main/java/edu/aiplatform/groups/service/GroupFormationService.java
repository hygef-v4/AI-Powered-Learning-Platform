package edu.aiplatform.groups.service;

import edu.aiplatform.academics.contract.ClassRef;
import edu.aiplatform.audit.contract.AuditEvent;
import edu.aiplatform.audit.contract.AuditResult;
import edu.aiplatform.audit.contract.IAudit;
import edu.aiplatform.groups.model.GroupFormationSettings;
import edu.aiplatform.groups.model.GroupFormationSettings.ClosedReason;
import edu.aiplatform.groups.repository.GroupProposalRepository;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.shared.error.ApiException;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Cài đặt đợt sinh viên tự chia nhóm của một lớp (F16, BR-U12-70…73); chỉ giảng viên chính của lớp. */
@Service
public class GroupFormationService {

    private final GroupProposalRepository proposalRepository;
    private final GroupAccess access;
    private final IAudit audit;
    private final Clock clock;

    public GroupFormationService(GroupProposalRepository proposalRepository, GroupAccess access, IAudit audit,
                                 Clock clock) {
        this.proposalRepository = proposalRepository;
        this.access = access;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public FormationView loadFormation(Actor actor, UUID classId) {
        ClassRef ref = access.requireTeacher(actor, classId);
        return FormationView.of(loadSettings(classId), ref, clock.instant());
    }

    @Transactional
    public FormationView updateFormation(Actor actor, UUID classId, UpdateFormationCommand command) {
        ClassRef ref = access.requireTeacher(actor, classId);
        GroupAccess.requireEditable(ref);
        if (command == null || command.version() == null || command.enabled() == null || command.minSize() == null
                || command.maxSize() == null) {
            throw ApiException.badRequest("FORMATION_INVALID", "Cần gửi đủ cài đặt kèm version");
        }
        int minSize = command.minSize();
        int maxSize = command.maxSize();
        if (!GroupFormationSettings.isValidSize(minSize) || !GroupFormationSettings.isValidSize(maxSize)) {
            throw ApiException.badRequest("FORMATION_SIZE_INVALID", "Sĩ số từ 2 đến 20");
        }
        if (minSize > maxSize) {
            throw ApiException.badRequest("FORMATION_SIZE_INVALID", "Sĩ số tối thiểu không lớn hơn tối đa");
        }
        Instant now = clock.instant();
        if (command.enabled() && command.deadline() != null && !command.deadline().isAfter(now)) {
            throw ApiException.badRequest("FORMATION_DEADLINE_PAST", "Hạn chót phải ở tương lai");
        }
        GroupFormationSettings before = loadSettings(classId);
        if (before.version() != command.version()) {
            throw ApiException.conflict("FORMATION_VERSION_CONFLICT", "Cài đặt vừa được thay đổi, hãy tải lại");
        }
        GroupFormationSettings after = new GroupFormationSettings(classId, command.enabled(), minSize, maxSize,
                command.deadline(), actor.accountId(), now, before.version() + 1);
        if (!proposalRepository.saveSettings(after, before.version())) {
            throw ApiException.conflict("FORMATION_VERSION_CONFLICT", "Cài đặt vừa được thay đổi, hãy tải lại");
        }
        // Mở lại đợt: lời mời cũ không tự sống lại (BR-U12-72).
        if (!before.isOpen(ref.status(), now) && after.isOpen(ref.status(), now)) {
            proposalRepository.expireInvitations(classId, now);
        }
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("classId", classId.toString());
        details.put("before", snapshot(before));
        details.put("after", snapshot(after));
        audit.record(new AuditEvent(UUID.randomUUID(), actor.accountId(), "GROUP_FORMATION_UPDATED", "CLASS",
                classId.toString(), AuditResult.SUCCESS, null, details, now));
        return FormationView.of(after, ref, now);
    }

    /** Cài đặt đã lưu, hoặc mặc định (tắt) khi lớp chưa có. */
    GroupFormationSettings loadSettings(UUID classId) {
        return proposalRepository.findSettings(classId).orElseGet(() -> GroupFormationSettings.defaults(classId));
    }

    private static Map<String, Object> snapshot(GroupFormationSettings s) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("enabled", s.enabled());
        values.put("minSize", s.minSize());
        values.put("maxSize", s.maxSize());
        values.put("deadline", s.deadline() == null ? null : s.deadline().toString());
        return values;
    }

    public record UpdateFormationCommand(Boolean enabled, Integer minSize, Integer maxSize, Instant deadline,
                                         Long version) {
    }

    /** Cài đặt kèm trạng thái mở/đóng tính tại thời điểm đọc; {@code closedReason} rỗng khi đợt mở. */
    public record FormationView(UUID classId, boolean enabled, int minSize, int maxSize, Instant deadline,
                                long version, boolean open, String closedReason) {

        static FormationView of(GroupFormationSettings s, ClassRef ref, Instant now) {
            ClosedReason reason = s.closedReason(ref.status(), now);
            return new FormationView(s.classId(), s.enabled(), s.minSize(), s.maxSize(), s.deadline(), s.version(),
                    reason == null, reason == null ? null : reason.name());
        }
    }
}
