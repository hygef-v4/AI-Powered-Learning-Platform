package edu.aiplatform.groups.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.aiplatform.academics.contract.ClassRef;
import edu.aiplatform.academics.contract.ClassStatus;
import edu.aiplatform.academics.contract.IClassAccess;
import edu.aiplatform.audit.contract.AuditEvent;
import edu.aiplatform.audit.contract.IAudit;
import edu.aiplatform.groups.model.ClassGroup;
import edu.aiplatform.groups.model.GroupFormationSettings;
import edu.aiplatform.groups.model.GroupProposal;
import edu.aiplatform.groups.model.ProposalMember;
import edu.aiplatform.groups.repository.GroupProposalRepository;
import edu.aiplatform.groups.repository.GroupRepository;
import edu.aiplatform.groups.service.GroupProposalService.CreateProposalCommand;
import edu.aiplatform.groups.service.GroupProposalService.InviteMembersCommand;
import edu.aiplatform.groups.service.GroupProposalService.MyProposalView;
import edu.aiplatform.groups.service.GroupProposalService.RenameProposalCommand;
import edu.aiplatform.groups.service.GroupProposalService.VersionCommand;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.AuthorizationDecision;
import edu.aiplatform.identity.contract.IAccountLookup;
import edu.aiplatform.identity.contract.IAuthorization;
import edu.aiplatform.identity.contract.Role;
import edu.aiplatform.jobs.contract.DomainEvent;
import edu.aiplatform.jobs.contract.EventTypes;
import edu.aiplatform.jobs.contract.IEventPublisher;
import edu.aiplatform.shared.error.ApiException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;

class GroupProposalServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-11T08:00:00Z");
    private final UUID classId = UUID.randomUUID();
    private final UUID teacherId = UUID.randomUUID();
    private final List<UUID> students = IntStream.range(0, 6).mapToObj(i -> UUID.randomUUID()).toList();
    private final Actor leader = new Actor(students.get(0), Role.STUDENT);
    private final Actor invitee = new Actor(students.get(1), Role.STUDENT);
    private final GroupProposalRepository proposals = mock(GroupProposalRepository.class);
    private final GroupRepository groups = mock(GroupRepository.class);
    private final IAuthorization authorization = mock(IAuthorization.class);
    private final IClassAccess classes = mock(IClassAccess.class);
    private final IAudit audit = mock(IAudit.class);
    private final IEventPublisher events = mock(IEventPublisher.class);
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final GroupAccess access = new GroupAccess(authorization, classes, audit, clock);
    private final GroupProposalCleanup cleanup = new GroupProposalCleanup(proposals);
    private final GroupProposalService service = new GroupProposalService(proposals, groups,
            new GroupFormationService(proposals, access, audit, clock), cleanup, access, classes,
            mock(IAccountLookup.class), audit, events, clock);
    private final GroupProposal draft = GroupProposal.draft(UUID.randomUUID(), classId, "Nhóm Mây", leader.accountId(),
            NOW.minusSeconds(60));

    @BeforeEach
    void setUp() {
        when(authorization.authorize(any(), any(), any())).thenReturn(AuthorizationDecision.allow());
        when(classes.getClassRef(classId)).thenReturn(Optional.of(
                new ClassRef(classId, UUID.randomUUID(), "SE1", "SE1", ClassStatus.OPEN, teacherId, false)));
        when(classes.isActiveStudent(any(), eq(classId))).thenReturn(true);
        when(classes.listActiveStudents(classId)).thenReturn(students);
        settings(true, 2, 3);
        when(proposals.lockProposal(draft.id())).thenReturn(Optional.of(draft));
    }

    @Test
    void creatingMakesTheStudentLeaderOfANewDraft() {
        service.createProposal(leader, classId, new CreateProposalCommand("  Nhóm Mây  "));

        ArgumentCaptor<GroupProposal> created = ArgumentCaptor.forClass(GroupProposal.class);
        verify(proposals).insertProposal(created.capture());
        assertThat(created.getValue().name()).isEqualTo("Nhóm Mây");
        assertThat(created.getValue().leaderId()).isEqualTo(leader.accountId());
        assertThat(created.getValue().status()).isEqualTo(GroupProposal.Status.DRAFT);
        verify(proposals).insertActiveMember(created.getValue().id(), classId, leader.accountId(), NOW);
    }

    @Test
    void creatingIsRefusedWhenClosedGroupedInAnotherProposalOrNameTaken() {
        settings(false, 2, 3);
        assertCode(() -> service.createProposal(leader, classId, new CreateProposalCommand("A")), "FORMATION_CLOSED");
        settings(true, 2, 3);

        assertCode(() -> service.createProposal(leader, classId, new CreateProposalCommand("  ")),
                "PROPOSAL_NAME_INVALID");

        when(groups.listGroups(classId)).thenReturn(List.of(new ClassGroup(UUID.randomUUID(), classId, "nhóm mây",
                students.get(5), null, NOW, NOW, List.of(students.get(5)))));
        assertCode(() -> service.createProposal(leader, classId, new CreateProposalCommand("Nhóm Mây")),
                "GROUP_NAME_DUPLICATE");

        when(groups.listGroups(classId)).thenReturn(List.of(new ClassGroup(UUID.randomUUID(), classId, "Khác",
                leader.accountId(), null, NOW, NOW, List.of(leader.accountId()))));
        assertCode(() -> service.createProposal(leader, classId, new CreateProposalCommand("Nhóm Mây")),
                "STUDENT_ALREADY_GROUPED");

        when(groups.listGroups(classId)).thenReturn(List.of());
        when(proposals.findActiveProposalOf(classId, leader.accountId())).thenReturn(Optional.of(draft));
        assertCode(() -> service.createProposal(leader, classId, new CreateProposalCommand("Nhóm Sao")),
                "STUDENT_IN_PROPOSAL");
        verify(proposals, never()).insertProposal(any());
    }

    @Test
    void invitingRespectsFreeSeatsAndNotifiesInvitees() {
        members(member(leader.accountId(), ProposalMember.Status.ACCEPTED, true));

        assertCode(() -> service.inviteMembers(leader, draft.id(),
                        new InviteMembersCommand(List.of(students.get(1), students.get(1)), 0L)),
                "PROPOSAL_INVITE_INVALID");
        assertCode(() -> service.inviteMembers(leader, draft.id(),
                new InviteMembersCommand(List.of(students.get(1), students.get(2), students.get(3)), 0L)),
                "PROPOSAL_FULL");

        service.inviteMembers(leader, draft.id(), new InviteMembersCommand(List.of(students.get(1)), 0L));

        verify(proposals).upsertInvitation(draft.id(), classId, students.get(1), leader.accountId(), NOW);
        DomainEvent event = publishedEvent();
        assertThat(event.eventType()).isEqualTo(EventTypes.GROUP_PROPOSAL_INVITED);
        assertThat(event.payload().get("recipientIds")).isEqualTo(List.of(students.get(1).toString()));
    }

    @Test
    void invitingSomeoneOutsideTheClassOrAlreadyGroupedIsRefused() {
        members(member(leader.accountId(), ProposalMember.Status.ACCEPTED, true));

        assertCode(() -> service.inviteMembers(leader, draft.id(),
                new InviteMembersCommand(List.of(UUID.randomUUID()), 0L)), "STUDENT_NOT_INVITABLE");
        when(groups.listGroups(classId)).thenReturn(List.of(new ClassGroup(UUID.randomUUID(), classId, "G",
                students.get(4), null, NOW, NOW, List.of(students.get(4)))));
        assertCode(() -> service.inviteMembers(leader, draft.id(),
                new InviteMembersCommand(List.of(students.get(4)), 0L)), "STUDENT_ALREADY_GROUPED");
    }

    @Test
    void onlyTheLeaderWithTheCurrentVersionChangesTheProposal() {
        members(member(leader.accountId(), ProposalMember.Status.ACCEPTED, true));

        assertThatThrownBy(() -> service.renameProposal(invitee, draft.id(), new RenameProposalCommand("B", 0L)))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("GROUP_PROPOSAL_NOT_FOUND"));
        ArgumentCaptor<AuditEvent> denied = ArgumentCaptor.forClass(AuditEvent.class);
        verify(audit).record(denied.capture());
        assertThat(denied.getValue().action()).isEqualTo("ACCESS_DENIED");

        assertCode(() -> service.renameProposal(leader, draft.id(), new RenameProposalCommand("B", 7L)),
                "PROPOSAL_VERSION_CONFLICT");
    }

    @Test
    void acceptingJoinsAndRevokesOtherInvitations() {
        members(member(leader.accountId(), ProposalMember.Status.ACCEPTED, true),
                member(invitee.accountId(), ProposalMember.Status.INVITED, false));

        service.acceptInvitation(invitee, draft.id());

        verify(proposals).updateMember(draft.id(), invitee.accountId(), ProposalMember.Status.ACCEPTED, true, NOW);
        verify(proposals).revokeInvitationsOf(classId, List.of(invitee.accountId()), draft.id(), NOW);
    }

    @Test
    void acceptingWhileInAnotherProposalIsRefusedEvenWhenRacing() {
        members(member(leader.accountId(), ProposalMember.Status.ACCEPTED, true),
                member(invitee.accountId(), ProposalMember.Status.INVITED, false));
        doThrow(new DuplicateKeyException("uq")).when(proposals)
                .updateMember(eq(draft.id()), eq(invitee.accountId()), eq(ProposalMember.Status.ACCEPTED), anyBoolean(),
                        any());

        assertCode(() -> service.acceptInvitation(invitee, draft.id()), "STUDENT_IN_PROPOSAL");

        Actor stranger = new Actor(students.get(4), Role.STUDENT);
        assertCode(() -> service.acceptInvitation(stranger, draft.id()), "GROUP_PROPOSAL_NOT_FOUND");
    }

    @Test
    void submittingNeedsTheMinimumAndNotifiesTheTeacher() {
        members(member(leader.accountId(), ProposalMember.Status.ACCEPTED, true),
                member(invitee.accountId(), ProposalMember.Status.INVITED, false));
        assertCode(() -> service.submitProposal(leader, draft.id(), new VersionCommand(0L)), "PROPOSAL_BELOW_MIN");

        members(member(leader.accountId(), ProposalMember.Status.ACCEPTED, true),
                member(invitee.accountId(), ProposalMember.Status.ACCEPTED, true));
        service.submitProposal(leader, draft.id(), new VersionCommand(0L));

        ArgumentCaptor<GroupProposal> updated = ArgumentCaptor.forClass(GroupProposal.class);
        verify(proposals).updateProposal(updated.capture());
        assertThat(updated.getValue().status()).isEqualTo(GroupProposal.Status.SUBMITTED);
        verify(proposals).closeInvitations(draft.id(), ProposalMember.Status.REVOKED, NOW);
        DomainEvent event = publishedEvent();
        assertThat(event.eventType()).isEqualTo(EventTypes.GROUP_PROPOSAL_SUBMITTED);
        assertThat(event.payload().get("recipientIds")).isEqualTo(List.of(teacherId.toString()));
    }

    @Test
    void submittedProposalIsLockedUntilWithdrawn() {
        GroupProposal submitted = draft.submitted(NOW);
        when(proposals.lockProposal(draft.id())).thenReturn(Optional.of(submitted));
        members(member(leader.accountId(), ProposalMember.Status.ACCEPTED, true),
                member(invitee.accountId(), ProposalMember.Status.ACCEPTED, true));

        assertCode(() -> service.inviteMembers(leader, draft.id(),
                new InviteMembersCommand(List.of(students.get(2)), 1L)), "PROPOSAL_NOT_DRAFT");
        assertCode(() -> service.leaveProposal(invitee, draft.id()), "PROPOSAL_NOT_DRAFT");

        service.withdrawProposal(leader, draft.id(), new VersionCommand(1L));

        ArgumentCaptor<GroupProposal> updated = ArgumentCaptor.forClass(GroupProposal.class);
        verify(proposals).updateProposal(updated.capture());
        assertThat(updated.getValue().status()).isEqualTo(GroupProposal.Status.DRAFT);
    }

    @Test
    void leaderCancelsInsteadOfLeaving() {
        members(member(leader.accountId(), ProposalMember.Status.ACCEPTED, true),
                member(invitee.accountId(), ProposalMember.Status.INVITED, false));

        assertCode(() -> service.leaveProposal(leader, draft.id()), "PROPOSAL_LEADER_CANNOT_LEAVE");
        service.cancelProposal(leader, draft.id(), new VersionCommand(0L));

        verify(proposals).deactivateMembers(draft.id());
        verify(proposals).closeInvitations(draft.id(), ProposalMember.Status.REVOKED, NOW);
    }

    @Test
    void groupedStudentSeesOnlyTheGroupedFlag() {
        when(groups.listGroups(classId)).thenReturn(List.of(new ClassGroup(UUID.randomUUID(), classId, "G",
                leader.accountId(), null, NOW, NOW, List.of(leader.accountId()))));

        MyProposalView view = service.loadMyProposal(leader, classId);

        assertThat(view.grouped()).isTrue();
        assertThat(view.proposal()).isNull();
        assertThat(view.candidates()).isEmpty();
    }

    @Test
    void leaderOfAnOpenDraftSeesInvitableClassmates() {
        when(proposals.findActiveProposalOf(classId, leader.accountId())).thenReturn(Optional.of(draft));
        members(member(leader.accountId(), ProposalMember.Status.ACCEPTED, true),
                member(invitee.accountId(), ProposalMember.Status.INVITED, false));

        MyProposalView view = service.loadMyProposal(leader, classId);

        assertThat(view.proposal().leader()).isTrue();
        assertThat(view.proposal().members()).hasSize(1);
        assertThat(view.proposal().invitations()).hasSize(1);
        assertThat(view.proposal().freeSeats()).isEqualTo(1);
        assertThat(view.candidates()).extracting(GroupProposalService.CandidateView::accountId)
                .containsExactlyElementsOf(students.subList(2, 6));
    }

    private void settings(boolean enabled, int min, int max) {
        when(proposals.findSettings(classId)).thenReturn(Optional.of(
                new GroupFormationSettings(classId, enabled, min, max, null, null, NOW, 1)));
    }

    private void members(ProposalMember... rows) {
        when(proposals.listMembers(draft.id())).thenReturn(new ArrayList<>(List.of(rows)));
    }

    private ProposalMember member(UUID accountId, ProposalMember.Status status, boolean active) {
        return new ProposalMember(draft.id(), classId, accountId, status, active, null, NOW, null);
    }

    private DomainEvent publishedEvent() {
        ArgumentCaptor<DomainEvent> event = ArgumentCaptor.forClass(DomainEvent.class);
        verify(events).publish(event.capture());
        return event.getValue();
    }

    private static void assertCode(Runnable call, String code) {
        assertThatThrownBy(call::run).isInstanceOfSatisfying(ApiException.class,
                e -> assertThat(e.code()).isEqualTo(code));
    }
}
