package edu.aiplatform.groups.service;

import edu.aiplatform.academics.contract.ClassRef;
import edu.aiplatform.academics.contract.IClassAccess;
import edu.aiplatform.audit.contract.AuditEvent;
import edu.aiplatform.audit.contract.AuditResult;
import edu.aiplatform.audit.contract.IAudit;
import edu.aiplatform.groups.model.ClassGroup;
import edu.aiplatform.groups.model.GroupFormationSettings;
import edu.aiplatform.groups.model.GroupFormationSettings.ClosedReason;
import edu.aiplatform.groups.model.GroupProposal;
import edu.aiplatform.groups.model.GroupProposal.EndReason;
import edu.aiplatform.groups.model.GroupProposal.Status;
import edu.aiplatform.groups.model.ProposalMember;
import edu.aiplatform.groups.model.ProposalRules;
import edu.aiplatform.groups.repository.GroupProposalRepository;
import edu.aiplatform.groups.repository.GroupRepository;
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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sinh viên tự lập đề xuất nhóm (F17–F20, BR-U12-71…81): đọc màn, tạo, đổi tên, mời, thu hồi, chấp nhận, từ chối,
 * rời, hủy, gửi duyệt, rút về. Mọi kiểm ở backend; mỗi thao tác ghi là một transaction trên dòng đề xuất đã khóa.
 */
@Service
public class GroupProposalService {

    private final GroupProposalRepository proposalRepository;
    private final GroupRepository groupRepository;
    private final GroupFormationService groupFormationService;
    private final GroupProposalCleanup groupProposalCleanup;
    private final GroupAccess access;
    private final IClassAccess classes;
    private final IAccountLookup accounts;
    private final IAudit audit;
    private final IEventPublisher events;
    private final Clock clock;

    public GroupProposalService(GroupProposalRepository proposalRepository, GroupRepository groupRepository,
                                GroupFormationService groupFormationService, GroupProposalCleanup groupProposalCleanup,
                                GroupAccess access, IClassAccess classes, IAccountLookup accounts, IAudit audit,
                                IEventPublisher events, Clock clock) {
        this.proposalRepository = proposalRepository;
        this.groupRepository = groupRepository;
        this.groupFormationService = groupFormationService;
        this.groupProposalCleanup = groupProposalCleanup;
        this.access = access;
        this.classes = classes;
        this.accounts = accounts;
        this.audit = audit;
        this.events = events;
        this.clock = clock;
    }

    // ---- đọc (F17) ------------------------------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public MyProposalView loadMyProposal(Actor actor, UUID classId) {
        return buildView(actor, loadContext(actor, classId));
    }

    // ---- tạo, đổi tên, mời, thu hồi (F18) --------------------------------------------------------------------

    @Transactional
    public MyProposalView createProposal(Actor actor, UUID classId, CreateProposalCommand command) {
        Context ctx = loadContext(actor, classId);
        requireOpen(ctx);
        requireEligible(ctx, actor.accountId());
        String name = requireName(command == null ? null : command.name());
        if (proposalRepository.findActiveProposalOf(classId, actor.accountId()).isPresent()) {
            throw inProposal();
        }
        requireUniqueName(ctx, name, null);
        Instant now = clock.instant();
        GroupProposal proposal = GroupProposal.draft(UUID.randomUUID(), classId, name, actor.accountId(), now);
        proposalRepository.insertProposal(proposal);
        try {
            proposalRepository.insertActiveMember(proposal.id(), classId, actor.accountId(), now);
        } catch (DataIntegrityViolationException e) {
            throw inProposal();
        }
        return buildView(actor, ctx);
    }

    @Transactional
    public MyProposalView renameProposal(Actor actor, UUID proposalId, RenameProposalCommand command) {
        Locked locked = lockAsLeader(actor, proposalId, command == null ? null : command.version());
        requireDraft(locked.proposal());
        requireOpen(locked.ctx());
        String name = requireName(command.name());
        requireUniqueName(locked.ctx(), name, proposalId);
        proposalRepository.updateProposal(locked.proposal().renamed(name, clock.instant()));
        return buildView(actor, locked.ctx());
    }

    @Transactional
    public MyProposalView inviteMembers(Actor actor, UUID proposalId, InviteMembersCommand command) {
        Locked locked = lockAsLeader(actor, proposalId, command == null ? null : command.version());
        GroupProposal proposal = locked.proposal();
        Context ctx = locked.ctx();
        requireDraft(proposal);
        requireOpen(ctx);
        List<UUID> invitees = command.accountIds() == null ? List.of() : command.accountIds();
        if (invitees.isEmpty() || invitees.size() > ctx.settings().maxSize()
                || invitees.stream().anyMatch(Objects::isNull)
                || new HashSet<>(invitees).size() != invitees.size()) {
            throw ApiException.badRequest("PROPOSAL_INVITE_INVALID", "Danh sách người mời không hợp lệ");
        }
        List<ProposalMember> members = proposalRepository.listMembers(proposalId);
        Set<UUID> present = new HashSet<>();
        members.stream().filter(m -> m.isActiveMember() || m.isInvited()).forEach(m -> present.add(m.accountId()));
        for (UUID invitee : invitees) {
            if (present.contains(invitee)) {
                throw ApiException.conflict("PROPOSAL_MEMBER_EXISTS", "Người này đã ở trong hoặc đã được mời vào đề xuất");
            }
            if (!ctx.activeStudents().contains(invitee)) {
                throw ApiException.badRequest("STUDENT_NOT_INVITABLE", "Chỉ mời được sinh viên đang học của lớp");
            }
            if (ctx.groupedStudents().contains(invitee)) {
                throw alreadyGrouped();
            }
        }
        long accepted = ProposalRules.countEffectiveMembers(members, ctx.activeStudents(), ctx.groupedStudents());
        long pending = members.stream().filter(ProposalMember::isInvited).count();
        if (ProposalRules.countFreeSeats(ctx.settings().maxSize(), (int) accepted, (int) pending) < invitees.size()) {
            throw ApiException.conflict("PROPOSAL_FULL", "Đề xuất đã đủ sĩ số tối đa");
        }
        Instant now = clock.instant();
        for (UUID invitee : invitees) {
            proposalRepository.upsertInvitation(proposalId, ctx.ref().classId(), invitee, actor.accountId(), now);
        }
        proposalRepository.updateProposal(proposal.touched(now));
        publish(EventTypes.GROUP_PROPOSAL_INVITED, proposal, invitees);
        return buildView(actor, ctx);
    }

    @Transactional
    public MyProposalView revokeInvitation(Actor actor, UUID proposalId, UUID accountId, VersionCommand command) {
        Locked locked = lockAsLeader(actor, proposalId, command == null ? null : command.version());
        requireDraft(locked.proposal());
        ProposalMember invitation = locked.memberOf(accountId).filter(ProposalMember::isInvited)
                .orElseThrow(() -> ApiException.notFound("PROPOSAL_INVITATION_NOT_FOUND"));
        Instant now = clock.instant();
        proposalRepository.updateMember(proposalId, invitation.accountId(), ProposalMember.Status.REVOKED, false, now);
        proposalRepository.updateProposal(locked.proposal().touched(now));
        return buildView(actor, locked.ctx());
    }

    // ---- trả lời lời mời, rời, hủy (F19) ------------------------------------------------------------------------

    @Transactional
    public MyProposalView acceptInvitation(Actor actor, UUID proposalId) {
        Locked locked = lockAsInvitee(actor, proposalId);
        Context ctx = locked.ctx();
        requireDraft(locked.proposal());
        requireOpen(ctx);
        requireEligible(ctx, actor.accountId());
        if (proposalRepository.findActiveProposalOf(ctx.ref().classId(), actor.accountId()).isPresent()) {
            throw inProposal();
        }
        Instant now = clock.instant();
        try {
            proposalRepository.updateMember(proposalId, actor.accountId(), ProposalMember.Status.ACCEPTED, true, now);
        } catch (DataIntegrityViolationException e) {
            throw inProposal();
        }
        proposalRepository.revokeInvitationsOf(ctx.ref().classId(), List.of(actor.accountId()), proposalId, now);
        proposalRepository.updateProposal(locked.proposal().touched(now));
        return buildView(actor, ctx);
    }

    @Transactional
    public MyProposalView declineInvitation(Actor actor, UUID proposalId) {
        Locked locked = lockAsInvitee(actor, proposalId);
        if (!locked.proposal().status().isActive()) {
            throw notActive();
        }
        Instant now = clock.instant();
        proposalRepository.updateMember(proposalId, actor.accountId(), ProposalMember.Status.DECLINED, false, now);
        proposalRepository.updateProposal(locked.proposal().touched(now));
        return buildView(actor, locked.ctx());
    }

    @Transactional
    public MyProposalView leaveProposal(Actor actor, UUID proposalId) {
        Locked locked = lockForStudent(actor, proposalId);
        ProposalMember membership = locked.memberOf(actor.accountId()).filter(ProposalMember::isActiveMember)
                .orElseThrow(() -> access.denyProposal(actor, proposalId, "NOT_MEMBER"));
        if (locked.proposal().isLeader(actor.accountId())) {
            throw ApiException.conflict("PROPOSAL_LEADER_CANNOT_LEAVE", "Trưởng nhóm hủy đề xuất thay vì rời");
        }
        requireDraft(locked.proposal());
        Instant now = clock.instant();
        proposalRepository.updateMember(proposalId, membership.accountId(), ProposalMember.Status.LEFT, false, now);
        proposalRepository.updateProposal(locked.proposal().touched(now));
        return buildView(actor, locked.ctx());
    }

    @Transactional
    public MyProposalView cancelProposal(Actor actor, UUID proposalId, VersionCommand command) {
        Locked locked = lockAsLeader(actor, proposalId, command == null ? null : command.version());
        requireDraft(locked.proposal());
        groupProposalCleanup.cancelProposal(locked.proposal(), EndReason.LEADER_CANCELLED, clock.instant());
        return buildView(actor, locked.ctx());
    }

    // ---- gửi duyệt, rút về (F20) --------------------------------------------------------------------------------

    @Transactional
    public MyProposalView submitProposal(Actor actor, UUID proposalId, VersionCommand command) {
        Locked locked = lockAsLeader(actor, proposalId, command == null ? null : command.version());
        Context ctx = locked.ctx();
        requireDraft(locked.proposal());
        requireOpen(ctx);
        Instant now = clock.instant();
        GroupProposal proposal = refreshMembers(locked, now);
        long members = ProposalRules.countEffectiveMembers(proposalRepository.listMembers(proposalId),
                ctx.activeStudents(), ctx.groupedStudents());
        if (members < ctx.settings().minSize()) {
            throw ApiException.conflict("PROPOSAL_BELOW_MIN", "Đề xuất chưa đủ sĩ số tối thiểu");
        }
        proposalRepository.closeInvitations(proposalId, ProposalMember.Status.REVOKED, now);
        GroupProposal submitted = proposal.submitted(now);
        proposalRepository.updateProposal(submitted);
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("classId", ctx.ref().classId().toString());
        details.put("members", members);
        audit.record(new AuditEvent(UUID.randomUUID(), actor.accountId(), "GROUP_PROPOSAL_SUBMITTED",
                "GROUP_PROPOSAL", proposalId.toString(), AuditResult.SUCCESS, null, details, now));
        if (ctx.ref().teacherId() != null) {
            publish(EventTypes.GROUP_PROPOSAL_SUBMITTED, submitted, List.of(ctx.ref().teacherId()));
        }
        return buildView(actor, ctx);
    }

    @Transactional
    public MyProposalView withdrawProposal(Actor actor, UUID proposalId, VersionCommand command) {
        Locked locked = lockAsLeader(actor, proposalId, command == null ? null : command.version());
        if (locked.proposal().status() != Status.SUBMITTED) {
            throw ApiException.conflict("PROPOSAL_NOT_SUBMITTED", "Đề xuất không ở trạng thái chờ duyệt");
        }
        proposalRepository.updateProposal(locked.proposal().backToDraft(clock.instant()));
        return buildView(actor, locked.ctx());
    }

    // ---- khóa và kiểm --------------------------------------------------------------------------------------------

    private Locked lockForStudent(Actor actor, UUID proposalId) {
        GroupProposal proposal = proposalRepository.lockProposal(proposalId)
                .orElseThrow(() -> access.denyProposal(actor, proposalId, "NOT_FOUND"));
        Context ctx = loadContext(actor, proposal.classId());
        return new Locked(proposal, ctx, proposalRepository.listMembers(proposalId));
    }

    private Locked lockAsLeader(Actor actor, UUID proposalId, Long version) {
        if (version == null) {
            throw ApiException.badRequest("PROPOSAL_VERSION_REQUIRED", "Cần gửi version của đề xuất");
        }
        Locked locked = lockForStudent(actor, proposalId);
        if (!locked.proposal().isLeader(actor.accountId())) {
            throw access.denyProposal(actor, proposalId, "NOT_LEADER");
        }
        if (locked.proposal().version() != version) {
            throw ApiException.conflict("PROPOSAL_VERSION_CONFLICT", "Đề xuất vừa thay đổi, hãy tải lại");
        }
        return locked;
    }

    private Locked lockAsInvitee(Actor actor, UUID proposalId) {
        Locked locked = lockForStudent(actor, proposalId);
        locked.memberOf(actor.accountId()).filter(ProposalMember::isInvited)
                .orElseThrow(() -> access.denyProposal(actor, proposalId, "NOT_INVITED"));
        return locked;
    }

    /**
     * Dọn lười (BR-U12-89): thành viên không còn hiệu lực rời đề xuất. Trưởng nhóm không còn hiệu lực thì đề xuất bị
     * hủy và thao tác dừng.
     */
    private GroupProposal refreshMembers(Locked locked, Instant now) {
        GroupProposal proposal = locked.proposal();
        Context ctx = locked.ctx();
        boolean changed = false;
        for (ProposalMember member : locked.members()) {
            if (!member.isActiveMember()
                    || ProposalRules.isEligible(member.accountId(), ctx.activeStudents(), ctx.groupedStudents())) {
                continue;
            }
            if (proposal.isLeader(member.accountId())) {
                EndReason reason = ctx.activeStudents().contains(member.accountId())
                        ? EndReason.LEADER_PLACED : EndReason.LEADER_LEFT_CLASS;
                groupProposalCleanup.cancelProposal(proposal, reason, now);
                throw notActive();
            }
            proposalRepository.updateMember(proposal.id(), member.accountId(), ProposalMember.Status.LEFT, false, now);
            changed = true;
        }
        if (!changed) {
            return proposal;
        }
        GroupProposal touched = proposal.touched(now);
        proposalRepository.updateProposal(touched);
        return touched;
    }

    private Context loadContext(Actor actor, UUID classId) {
        ClassRef ref = access.requireStudent(actor, classId);
        GroupFormationSettings settings = groupFormationService.loadSettings(classId);
        Set<UUID> active;
        try {
            active = new LinkedHashSet<>(classes.listActiveStudents(classId));
        } catch (RuntimeException e) {
            throw ApiException.unavailable("GROUP_DEPENDENCY_UNAVAILABLE", "Hệ thống tạm bận, vui lòng thử lại sau");
        }
        List<ClassGroup> groups = groupRepository.listGroups(classId);
        Set<UUID> grouped = new HashSet<>();
        Set<String> groupNames = new HashSet<>();
        for (ClassGroup group : groups) {
            grouped.addAll(group.memberIds());
            groupNames.add(ProposalRules.nameKey(group.name()));
        }
        Instant now = clock.instant();
        return new Context(ref, settings, settings.closedReason(ref.status(), now), active, grouped, groupNames);
    }

    private static void requireOpen(Context ctx) {
        GroupAccess.requireOpen(ctx.ref());
        if (ctx.closedReason() != null) {
            throw ApiException.conflict("FORMATION_CLOSED", "Đợt tự chia nhóm đang đóng");
        }
    }

    private static void requireEligible(Context ctx, UUID accountId) {
        if (ctx.groupedStudents().contains(accountId)) {
            throw alreadyGrouped();
        }
    }

    private static void requireDraft(GroupProposal proposal) {
        if (!proposal.status().isActive()) {
            throw notActive();
        }
        if (proposal.status() != Status.DRAFT) {
            throw ApiException.conflict("PROPOSAL_NOT_DRAFT", "Đề xuất đang chờ duyệt, rút về để sửa");
        }
    }

    private static String requireName(String raw) {
        String name = ProposalRules.cleanName(raw);
        if (name == null) {
            throw ApiException.badRequest("PROPOSAL_NAME_INVALID", "Tên nhóm cần 1–100 ký tự");
        }
        return name;
    }

    private void requireUniqueName(Context ctx, String name, UUID ownProposalId) {
        String key = ProposalRules.nameKey(name);
        boolean taken = ctx.groupNames().contains(key)
                || proposalRepository.listActiveProposals(ctx.ref().classId()).stream()
                .anyMatch(p -> !p.id().equals(ownProposalId) && ProposalRules.nameKey(p.name()).equals(key));
        if (taken) {
            throw ApiException.conflict("GROUP_NAME_DUPLICATE", "Tên nhóm đã được dùng trong lớp");
        }
    }

    private static ApiException inProposal() {
        return ApiException.conflict("STUDENT_IN_PROPOSAL", "Bạn đang ở một đề xuất khác, hãy rời trước");
    }

    private static ApiException alreadyGrouped() {
        return ApiException.conflict("STUDENT_ALREADY_GROUPED", "Sinh viên đã có nhóm chính thức");
    }

    private static ApiException notActive() {
        return ApiException.conflict("PROPOSAL_NOT_ACTIVE", "Đề xuất đã kết thúc");
    }

    // ---- dựng màn ------------------------------------------------------------------------------------------------

    private MyProposalView buildView(Actor actor, Context ctx) {
        UUID classId = ctx.ref().classId();
        UUID me = actor.accountId();
        GroupFormationSettings s = ctx.settings();
        FormationSummary formation = new FormationSummary(s.enabled(), s.minSize(), s.maxSize(), s.deadline(),
                ctx.closedReason() == null, ctx.closedReason() == null ? null : ctx.closedReason().name());
        if (ctx.groupedStudents().contains(me)) {
            return new MyProposalView(classId, ctx.ref().status().name(), true, formation, null, List.of(), null,
                    List.of());
        }
        ProposalPeople people = new ProposalPeople(accounts);
        Optional<GroupProposal> mine = proposalRepository.findActiveProposalOf(classId, me);
        ProposalView proposal = mine.map(p -> proposalView(p, me, ctx, people)).orElse(null);
        List<InvitationView> invitations = proposalRepository.listInvitingProposals(classId, me).stream()
                .map(p -> invitationView(p, ctx, people, mine.isPresent())).toList();
        RejectedView rejected = mine.isPresent() ? null : proposalRepository.findLatestRejectedOf(classId, me)
                .map(p -> new RejectedView(p.id(), p.name(), p.decisionReason(), p.decidedAt())).orElse(null);
        List<CandidateView> candidates = proposal != null && proposal.leader() && Status.DRAFT.name()
                .equals(proposal.status()) && ctx.closedReason() == null
                ? candidates(ctx, mine.get(), people) : List.of();
        return new MyProposalView(classId, ctx.ref().status().name(), false, formation, proposal, invitations, rejected,
                candidates);
    }

    private ProposalView proposalView(GroupProposal p, UUID me, Context ctx, ProposalPeople people) {
        List<ProposalMember> rows = proposalRepository.listMembers(p.id());
        List<MemberView> members = new ArrayList<>();
        List<MemberView> invited = new ArrayList<>();
        boolean expired = ctx.closedReason() != null;
        for (ProposalMember row : rows) {
            if (row.isActiveMember()
                    && ProposalRules.isEligible(row.accountId(), ctx.activeStudents(), ctx.groupedStudents())) {
                members.add(MemberView.of(people.loadPerson(row.accountId()), p.isLeader(row.accountId()), false));
            } else if (row.isInvited()) {
                invited.add(MemberView.of(people.loadPerson(row.accountId()), false, expired));
            }
        }
        int pending = expired ? 0 : invited.size();
        return new ProposalView(p.id(), p.name(), p.status().name(), p.leaderId(), p.isLeader(me), p.version(),
                members, invited, ProposalRules.countFreeSeats(ctx.settings().maxSize(), members.size(), pending),
                p.submittedAt());
    }

    private InvitationView invitationView(GroupProposal p, Context ctx, ProposalPeople people, boolean inProposal) {
        long members = ProposalRules.countEffectiveMembers(proposalRepository.listMembers(p.id()),
                ctx.activeStudents(), ctx.groupedStudents());
        boolean canAccept = !inProposal && p.status() == Status.DRAFT && ctx.closedReason() == null;
        return new InvitationView(p.id(), p.name(), p.status().name(), people.loadPerson(p.leaderId()).name(),
                (int) members, ctx.closedReason() != null, canAccept);
    }

    private List<CandidateView> candidates(Context ctx, GroupProposal mine, ProposalPeople people) {
        Map<UUID, UUID> memberships = proposalRepository.mapActiveMemberships(ctx.ref().classId());
        Set<UUID> present = new HashSet<>();
        proposalRepository.listMembers(mine.id()).stream().filter(m -> m.isActiveMember() || m.isInvited())
                .forEach(m -> present.add(m.accountId()));
        return ctx.activeStudents().stream()
                .filter(id -> !ctx.groupedStudents().contains(id) && !present.contains(id))
                .map(id -> {
                    Person person = people.loadPerson(id);
                    UUID other = memberships.get(id);
                    return new CandidateView(id, person.name(), person.email(),
                            other != null && !Objects.equals(other, mine.id()), person.studentCode());
                }).toList();
    }

    private void publish(String type, GroupProposal proposal, Collection<UUID> recipients) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("classId", proposal.classId().toString());
        payload.put("proposalId", proposal.id().toString());
        payload.put("recipientIds", recipients.stream().map(UUID::toString).toList());
        events.publish(new DomainEvent(1, UUID.randomUUID(), type, "U12", clock.instant(), null, payload));
    }

    // ---- kiểu dữ liệu ----------------------------------------------------------------------------------------

    private record Context(ClassRef ref, GroupFormationSettings settings, ClosedReason closedReason,
                           Set<UUID> activeStudents, Set<UUID> groupedStudents, Set<String> groupNames) {
    }

    private record Locked(GroupProposal proposal, Context ctx, List<ProposalMember> members) {

        Optional<ProposalMember> memberOf(UUID accountId) {
            return members.stream().filter(m -> m.accountId().equals(accountId)).findFirst();
        }
    }

    public record CreateProposalCommand(String name) {
    }

    public record RenameProposalCommand(String name, Long version) {
    }

    public record InviteMembersCommand(List<UUID> accountIds, Long version) {
    }

    public record VersionCommand(Long version) {
    }

    /** Màn đề xuất của sinh viên; {@code grouped} đúng thì sinh viên đã có nhóm chính thức và các phần khác trống. */
    public record MyProposalView(UUID classId, String classStatus, boolean grouped, FormationSummary formation,
                                 ProposalView proposal, List<InvitationView> invitations, RejectedView lastRejected,
                                 List<CandidateView> candidates) {
    }

    public record FormationSummary(boolean enabled, int minSize, int maxSize, Instant deadline, boolean open,
                                   String closedReason) {
    }

    public record ProposalView(UUID id, String name, String status, UUID leaderId, boolean leader, long version,
                               List<MemberView> members, List<MemberView> invitations, int freeSeats,
                               Instant submittedAt) {
    }

    /** {@code expired} chỉ dùng cho lời mời khi đợt đóng. */
    public record MemberView(UUID accountId, String name, String email, boolean leader, boolean expired,
                             String studentCode) {

        static MemberView of(Person person, boolean leader, boolean expired) {
            return new MemberView(person.accountId(), person.name(), person.email(), leader, expired,
                    person.studentCode());
        }
    }

    public record InvitationView(UUID proposalId, String proposalName, String status, String leaderName,
                                 int memberCount, boolean expired, boolean canAccept) {
    }

    public record RejectedView(UUID proposalId, String name, String reason, Instant decidedAt) {
    }

    /** Bạn mời được (BR-U12-92, 98): id, họ tên, email trường, MSSV và cờ đang ở đề xuất khác. */
    public record CandidateView(UUID accountId, String name, String email, boolean inOtherProposal,
                                String studentCode) {
    }
}
