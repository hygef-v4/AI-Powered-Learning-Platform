package edu.aiplatform.groups.service;

import edu.aiplatform.academics.contract.ClassRef;
import edu.aiplatform.academics.contract.IClassAccess;
import edu.aiplatform.audit.contract.AuditEvent;
import edu.aiplatform.audit.contract.AuditResult;
import edu.aiplatform.audit.contract.IAudit;
import edu.aiplatform.groups.model.ClassGroup;
import edu.aiplatform.groups.model.GroupFormationSettings;
import edu.aiplatform.groups.model.GroupProposal;
import edu.aiplatform.groups.model.GroupProposal.Status;
import edu.aiplatform.groups.model.GroupSetRules;
import edu.aiplatform.groups.model.GroupSetRules.Draft;
import edu.aiplatform.groups.model.GroupSetRules.Issue;
import edu.aiplatform.groups.model.ProposalMember;
import edu.aiplatform.groups.model.ProposalRules;
import edu.aiplatform.groups.repository.GroupProposalRepository;
import edu.aiplatform.groups.repository.GroupRepository;
import edu.aiplatform.groups.service.GroupFormationService.FormationView;
import edu.aiplatform.groups.service.ProposalPeople.Person;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.IAccountLookup;
import edu.aiplatform.jobs.contract.DomainEvent;
import edu.aiplatform.jobs.contract.EventTypes;
import edu.aiplatform.jobs.contract.IEventPublisher;
import edu.aiplatform.shared.error.ApiException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Giảng viên chính xem, duyệt, từ chối đề xuất nhóm (F21, F22, F24, BR-U12-82…90). Duyệt dùng lõi lưu bộ nhóm của
 * {@link ClassGroupService}; duyệt nhiều chạy trong một transaction và tăng {@code version} bộ nhóm một lần.
 */
@Service
public class GroupProposalReviewService {

    public static final int MAX_APPROVE_BATCH = 100;

    private final GroupProposalRepository proposalRepository;
    private final GroupRepository groupRepository;
    private final ClassGroupService classGroupService;
    private final GroupFormationService groupFormationService;
    private final GroupProposalCleanup groupProposalCleanup;
    private final GroupAccess access;
    private final IClassAccess classes;
    private final IAccountLookup accounts;
    private final IAudit audit;
    private final IEventPublisher events;
    private final Clock clock;

    public GroupProposalReviewService(GroupProposalRepository proposalRepository, GroupRepository groupRepository,
                                      ClassGroupService classGroupService, GroupFormationService groupFormationService,
                                      GroupProposalCleanup groupProposalCleanup, GroupAccess access,
                                      IClassAccess classes, IAccountLookup accounts, IAudit audit,
                                      IEventPublisher events, Clock clock) {
        this.proposalRepository = proposalRepository;
        this.groupRepository = groupRepository;
        this.classGroupService = classGroupService;
        this.groupFormationService = groupFormationService;
        this.groupProposalCleanup = groupProposalCleanup;
        this.access = access;
        this.classes = classes;
        this.accounts = accounts;
        this.audit = audit;
        this.events = events;
        this.clock = clock;
    }

    // ---- đọc (F24) ------------------------------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public ClassProposalsView listClassProposals(Actor actor, UUID classId) {
        ClassRef ref = access.requireTeacher(actor, classId);
        GroupFormationSettings settings = groupFormationService.loadSettings(classId);
        Set<UUID> active = activeStudents(classId);
        Set<UUID> grouped = new HashSet<>();
        groupRepository.listGroups(classId).forEach(g -> grouped.addAll(g.memberIds()));
        List<GroupProposal> proposals = proposalRepository.listActiveProposals(classId);
        Map<UUID, List<ProposalMember>> rowsByProposal = proposalRepository
                .listMembersOf(proposals.stream().map(GroupProposal::id).toList()).stream()
                .collect(Collectors.groupingBy(ProposalMember::proposalId));
        ProposalPeople people = new ProposalPeople(accounts);
        Map<UUID, GroupProposal> byMember = new HashMap<>();
        List<ReviewProposalView> views = new ArrayList<>();
        for (GroupProposal p : proposals) {
            List<ProposalMember> rows = rowsByProposal.getOrDefault(p.id(), List.of());
            List<Person> members = new ArrayList<>();
            int pending = 0;
            for (ProposalMember row : rows) {
                if (row.isActiveMember() && ProposalRules.isEligible(row.accountId(), active, grouped)) {
                    members.add(people.loadPerson(row.accountId()));
                    byMember.put(row.accountId(), p);
                } else if (row.isInvited()) {
                    pending++;
                }
            }
            views.add(new ReviewProposalView(p.id(), p.name(), p.status().name(), people.loadPerson(p.leaderId()),
                    members, pending, p.submittedAt(), warnings(p, members, settings, active, grouped)));
        }
        List<UngroupedView> ungrouped = active.stream().filter(id -> !grouped.contains(id)).map(id -> {
            GroupProposal p = byMember.get(id);
            return new UngroupedView(people.loadPerson(id), p == null ? null : p.id(), p == null ? null : p.name(),
                    p == null ? null : p.status().name());
        }).toList();
        return new ClassProposalsView(classId, FormationView.of(settings, ref, clock.instant()), views, ungrouped);
    }

    // ---- duyệt (F21) ----------------------------------------------------------------------------------------------

    @Transactional
    public ApproveResult approveProposals(Actor actor, UUID classId, ApproveCommand command) {
        ClassRef ref = access.requireTeacher(actor, classId);
        GroupAccess.requireEditable(ref);
        List<UUID> ids = command == null || command.proposalIds() == null ? List.of() : command.proposalIds();
        if (ids.isEmpty() || ids.size() > MAX_APPROVE_BATCH || ids.stream().anyMatch(Objects::isNull)
                || new HashSet<>(ids).size() != ids.size()) {
            throw ApiException.badRequest("PROPOSAL_IDS_INVALID", "Chọn từ 1 đến 100 đề xuất, không trùng");
        }
        Instant now = clock.instant();
        long version = groupRepository.setVersion(classId);
        Set<UUID> active = activeStudents(classId);
        List<ClassGroup> current = groupRepository.listGroups(classId);
        int minSize = groupFormationService.loadSettings(classId).minSize();
        // Khóa theo thứ tự id để hai lần duyệt chồng nhau không deadlock.
        Map<UUID, GroupProposal> locked = new HashMap<>();
        ids.stream().sorted().forEach(id -> proposalRepository.lockProposal(id).ifPresent(p -> locked.put(id, p)));

        List<Draft> accumulated = new ArrayList<>();
        Set<UUID> grouped = new HashSet<>();
        for (ClassGroup g : current) {
            accumulated.add(new Draft(g.id().toString(), g.id(), g.name(), g.leaderId(), g.memberIds()));
            grouped.addAll(g.memberIds());
        }
        List<Failed> failed = new ArrayList<>();
        List<Accepted> accepted = new ArrayList<>();
        for (UUID id : ids) {
            GroupProposal proposal = locked.get(id);
            if (proposal == null || !proposal.classId().equals(classId)) {
                failed.add(new Failed(id, "GROUP_PROPOSAL_NOT_FOUND", "Không tìm thấy đề xuất"));
                continue;
            }
            if (proposal.status() != Status.SUBMITTED) {
                failed.add(new Failed(id, "PROPOSAL_NOT_SUBMITTED", "Đề xuất không ở trạng thái chờ duyệt"));
                continue;
            }
            List<UUID> members = new ArrayList<>();
            for (ProposalMember row : proposalRepository.listMembers(id)) {
                if (!row.isActiveMember()) {
                    continue;
                }
                if (ProposalRules.isEligible(row.accountId(), active, grouped)) {
                    members.add(row.accountId());
                } else {
                    proposalRepository.updateMember(id, row.accountId(), ProposalMember.Status.LEFT, false, now);
                }
            }
            if (!members.contains(proposal.leaderId()) || members.size() < minSize) {
                proposalRepository.updateProposal(proposal.backToDraft(now));
                failed.add(new Failed(id, "PROPOSAL_INCOMPLETE",
                        "Đề xuất thiếu trưởng nhóm hoặc chưa đủ sĩ số, đã trả về nháp"));
                continue;
            }
            Draft draft = new Draft("proposal-" + id, null, proposal.name().trim(), proposal.leaderId(), members);
            List<Draft> candidate = new ArrayList<>(accumulated);
            candidate.add(draft);
            List<Issue> issues = GroupSetRules.validate(candidate, active).stream()
                    .filter(i -> draft.key().equals(i.groupKey())).toList();
            if (!issues.isEmpty()) {
                failed.add(new Failed(id, issues.get(0).code(), issues.get(0).message()));
                continue;
            }
            accumulated.add(draft);
            grouped.addAll(members);
            accepted.add(new Accepted(proposal, draft));
        }

        List<Approved> approved = new ArrayList<>();
        if (!accepted.isEmpty()) {
            Map<String, UUID> created = classGroupService.addApprovedGroups(actor, classId, version,
                    accepted.stream().map(Accepted::draft).toList());
            Set<UUID> placed = new LinkedHashSet<>();
            for (Accepted a : accepted) {
                UUID groupId = created.get(a.draft().key());
                GroupProposal proposal = a.proposal();
                proposalRepository.updateProposal(proposal.approved(actor.accountId(), groupId, now));
                proposalRepository.deactivateMembers(proposal.id());
                placed.addAll(a.draft().memberIds());
                Map<String, Object> details = new LinkedHashMap<>();
                details.put("classId", classId.toString());
                details.put("groupId", groupId.toString());
                details.put("members", a.draft().memberIds().size());
                record(actor, "GROUP_PROPOSAL_APPROVED", proposal.id(), details, now);
                publish(EventTypes.GROUP_PROPOSAL_APPROVED, proposal, a.draft().memberIds());
                approved.add(new Approved(proposal.id(), groupId));
            }
            // Lời mời còn treo của người vừa có nhóm ở các đề xuất khác bị thu hồi (BR-U12-88).
            groupProposalCleanup.releasePlacedStudents(classId, placed, now);
        }
        return new ApproveResult(approved, failed, groupRepository.setVersion(classId));
    }

    // ---- từ chối (F22) -------------------------------------------------------------------------------------------

    @Transactional
    public ClassProposalsView rejectProposal(Actor actor, UUID proposalId, RejectProposalCommand command) {
        GroupProposal proposal = proposalRepository.lockProposal(proposalId)
                .orElseThrow(() -> access.denyProposal(actor, proposalId, "NOT_FOUND"));
        ClassRef ref = access.requireTeacher(actor, proposal.classId());
        GroupAccess.requireEditable(ref);
        if (proposal.status() != Status.SUBMITTED) {
            throw ApiException.conflict("PROPOSAL_NOT_SUBMITTED", "Đề xuất không ở trạng thái chờ duyệt");
        }
        String reason = ProposalRules.cleanReason(command == null ? null : command.reason());
        if (reason != null && reason.length() > ProposalRules.MAX_REASON) {
            throw ApiException.badRequest("PROPOSAL_REASON_TOO_LONG", "Lý do tối đa 1.000 ký tự");
        }
        Instant now = clock.instant();
        List<UUID> members = proposalRepository.listMembers(proposalId).stream()
                .filter(ProposalMember::isActiveMember).map(ProposalMember::accountId).toList();
        proposalRepository.updateProposal(proposal.rejected(actor.accountId(), reason, now));
        proposalRepository.deactivateMembers(proposalId);
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("classId", proposal.classId().toString());
        details.put("hasReason", reason != null);
        record(actor, "GROUP_PROPOSAL_REJECTED", proposalId, details, now);
        publish(EventTypes.GROUP_PROPOSAL_REJECTED, proposal, members);
        return listClassProposals(actor, proposal.classId());
    }

    // ---- tiện ích -----------------------------------------------------------------------------------------------

    private static List<String> warnings(GroupProposal p, List<Person> members, GroupFormationSettings settings,
                                         Set<UUID> active, Set<UUID> grouped) {
        List<String> warnings = new ArrayList<>();
        if (!ProposalRules.isEligible(p.leaderId(), active, grouped)) {
            warnings.add("LEADER_INACTIVE");
        }
        if (members.size() < settings.minSize()) {
            warnings.add("BELOW_MIN");
        }
        if (members.size() > settings.maxSize()) {
            warnings.add("ABOVE_MAX");
        }
        return warnings;
    }

    private Set<UUID> activeStudents(UUID classId) {
        try {
            return new LinkedHashSet<>(classes.listActiveStudents(classId));
        } catch (RuntimeException e) {
            throw ApiException.unavailable("GROUP_DEPENDENCY_UNAVAILABLE", "Hệ thống tạm bận, vui lòng thử lại sau");
        }
    }

    private void record(Actor actor, String action, UUID proposalId, Map<String, Object> details, Instant now) {
        audit.record(new AuditEvent(UUID.randomUUID(), actor.accountId(), action, "GROUP_PROPOSAL",
                proposalId.toString(), AuditResult.SUCCESS, null, details, now));
    }

    private void publish(String type, GroupProposal proposal, Collection<UUID> recipients) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("classId", proposal.classId().toString());
        payload.put("proposalId", proposal.id().toString());
        payload.put("recipientIds", recipients.stream().map(UUID::toString).toList());
        events.publish(new DomainEvent(1, UUID.randomUUID(), type, "U12", clock.instant(), null, payload));
    }

    // ---- kiểu dữ liệu ----------------------------------------------------------------------------------------

    private record Accepted(GroupProposal proposal, Draft draft) {
    }

    public record ApproveCommand(List<UUID> proposalIds) {
    }

    public record RejectProposalCommand(String reason) {
    }

    public record ClassProposalsView(UUID classId, FormationView formation, List<ReviewProposalView> proposals,
                                     List<UngroupedView> ungrouped) {
    }

    /** {@code warnings}: {@code LEADER_INACTIVE}, {@code BELOW_MIN}, {@code ABOVE_MAX} (BR-U12-73, 90). */
    public record ReviewProposalView(UUID id, String name, String status, Person leader, List<Person> members,
                                     int pendingInvitations, Instant submittedAt, List<String> warnings) {
    }

    /** Sinh viên đang học chưa có nhóm, kèm đề xuất họ đang ở nếu có. */
    public record UngroupedView(Person person, UUID proposalId, String proposalName, String proposalStatus) {
    }

    public record ApproveResult(List<Approved> approved, List<Failed> failed, long groupSetVersion) {
    }

    public record Approved(UUID proposalId, UUID groupId) {
    }

    public record Failed(UUID proposalId, String code, String message) {
    }
}
