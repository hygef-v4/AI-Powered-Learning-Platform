package edu.aiplatform.groups.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.aiplatform.academics.contract.ClassRef;
import edu.aiplatform.academics.contract.ClassStatus;
import edu.aiplatform.academics.contract.IClassAccess;
import edu.aiplatform.audit.contract.IAudit;
import edu.aiplatform.groups.model.ClassGroup;
import edu.aiplatform.groups.model.GroupFormationSettings;
import edu.aiplatform.groups.model.GroupProposal;
import edu.aiplatform.groups.model.GroupSetRules.Draft;
import edu.aiplatform.groups.model.ProposalMember;
import edu.aiplatform.groups.repository.GroupProposalRepository;
import edu.aiplatform.groups.repository.GroupRepository;
import edu.aiplatform.groups.service.GroupProposalReviewService.ApproveCommand;
import edu.aiplatform.groups.service.GroupProposalReviewService.ApproveResult;
import edu.aiplatform.groups.service.GroupProposalReviewService.ClassProposalsView;
import edu.aiplatform.groups.service.GroupProposalReviewService.RejectProposalCommand;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class GroupProposalReviewServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-11T08:00:00Z");
    private final UUID classId = UUID.randomUUID();
    private final Actor teacher = new Actor(UUID.randomUUID(), Role.TEACHER);
    private final List<UUID> students = IntStream.range(0, 8).mapToObj(i -> UUID.randomUUID()).toList();
    private final GroupProposalRepository proposals = mock(GroupProposalRepository.class);
    private final GroupRepository groups = mock(GroupRepository.class);
    private final ClassGroupService classGroupService = mock(ClassGroupService.class);
    private final GroupProposalCleanup cleanup = mock(GroupProposalCleanup.class);
    private final IAuthorization authorization = mock(IAuthorization.class);
    private final IClassAccess classes = mock(IClassAccess.class);
    private final IAudit audit = mock(IAudit.class);
    private final IEventPublisher events = mock(IEventPublisher.class);
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final GroupAccess access = new GroupAccess(authorization, classes, audit, clock);
    private final GroupProposalReviewService service = new GroupProposalReviewService(proposals, groups,
            classGroupService, new GroupFormationService(proposals, access, audit, clock), cleanup, access, classes,
            mock(IAccountLookup.class), audit, events, clock);

    @BeforeEach
    void setUp() {
        when(authorization.authorize(eq(teacher), any(), any())).thenReturn(AuthorizationDecision.allow());
        classStatus(ClassStatus.OPEN);
        when(classes.listActiveStudents(classId)).thenReturn(students);
        when(groups.setVersion(classId)).thenReturn(4L);
        when(proposals.findSettings(classId)).thenReturn(Optional.of(
                new GroupFormationSettings(classId, true, 2, 4, null, null, NOW, 1)));
    }

    @Test
    void validProposalsBecomeGroupsAndInvalidOnesAreReportedWithoutBlockingThem() {
        GroupProposal ok = submitted("Nhóm Mây", 0, 1);
        GroupProposal taken = submitted("nhóm a", 2, 3);
        when(groups.listGroups(classId)).thenReturn(List.of(new ClassGroup(UUID.randomUUID(), classId, "Nhóm A",
                students.get(6), null, NOW, NOW, List.of(students.get(6), students.get(7)))));
        UUID groupId = UUID.randomUUID();
        when(classGroupService.addApprovedGroups(eq(teacher), eq(classId), eq(4L), anyList()))
                .thenReturn(Map.of("proposal-" + ok.id(), groupId));

        ApproveResult result = service.approveProposals(teacher, classId,
                new ApproveCommand(List.of(ok.id(), taken.id())));

        assertThat(result.approved()).singleElement().satisfies(a -> {
            assertThat(a.proposalId()).isEqualTo(ok.id());
            assertThat(a.groupId()).isEqualTo(groupId);
        });
        assertThat(result.failed()).singleElement().satisfies(f -> {
            assertThat(f.proposalId()).isEqualTo(taken.id());
            assertThat(f.code()).isEqualTo("GROUP_NAME_DUPLICATE");
        });
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Draft>> drafts = ArgumentCaptor.forClass(List.class);
        verify(classGroupService).addApprovedGroups(eq(teacher), eq(classId), eq(4L), drafts.capture());
        assertThat(drafts.getValue()).singleElement().satisfies(d -> {
            assertThat(d.leaderId()).isEqualTo(students.get(0));
            assertThat(d.memberIds()).containsExactly(students.get(0), students.get(1));
        });
        ArgumentCaptor<GroupProposal> updated = ArgumentCaptor.forClass(GroupProposal.class);
        verify(proposals).updateProposal(updated.capture());
        assertThat(updated.getValue().status()).isEqualTo(GroupProposal.Status.APPROVED);
        assertThat(updated.getValue().approvedGroupId()).isEqualTo(groupId);
        verify(proposals).deactivateMembers(ok.id());
        verify(cleanup).releasePlacedStudents(eq(classId), eq(java.util.Set.of(students.get(0), students.get(1))),
                eq(NOW));
        ArgumentCaptor<DomainEvent> event = ArgumentCaptor.forClass(DomainEvent.class);
        verify(events).publish(event.capture());
        assertThat(event.getValue().eventType()).isEqualTo(EventTypes.GROUP_PROPOSAL_APPROVED);
    }

    @Test
    void proposalThatLostMembersReturnsToDraftAndNothingIsWritten() {
        GroupProposal shrunk = submitted("Nhóm Mây", 0, 1);
        when(classes.listActiveStudents(classId)).thenReturn(students.subList(0, 1));

        ApproveResult result = service.approveProposals(teacher, classId, new ApproveCommand(List.of(shrunk.id())));

        assertThat(result.failed()).singleElement()
                .satisfies(f -> assertThat(f.code()).isEqualTo("PROPOSAL_INCOMPLETE"));
        verify(proposals).updateMember(shrunk.id(), students.get(1), ProposalMember.Status.LEFT, false, NOW);
        ArgumentCaptor<GroupProposal> updated = ArgumentCaptor.forClass(GroupProposal.class);
        verify(proposals).updateProposal(updated.capture());
        assertThat(updated.getValue().status()).isEqualTo(GroupProposal.Status.DRAFT);
        verify(classGroupService, never()).addApprovedGroups(any(), any(), anyLong(), anyList());
        verify(cleanup, never()).releasePlacedStudents(any(), any(), any());
    }

    @Test
    void draftsUnknownIdsAndBadBatchesAreRefused() {
        GroupProposal draft = GroupProposal.draft(UUID.randomUUID(), classId, "Nhóm Mây", students.get(0), NOW);
        when(proposals.lockProposal(draft.id())).thenReturn(Optional.of(draft));
        UUID unknown = UUID.randomUUID();

        ApproveResult result = service.approveProposals(teacher, classId,
                new ApproveCommand(List.of(draft.id(), unknown)));

        assertThat(result.failed()).extracting(GroupProposalReviewService.Failed::code)
                .containsExactly("PROPOSAL_NOT_SUBMITTED", "GROUP_PROPOSAL_NOT_FOUND");
        assertThat(result.groupSetVersion()).isEqualTo(4);
        assertCode(() -> service.approveProposals(teacher, classId, new ApproveCommand(List.of())),
                "PROPOSAL_IDS_INVALID");
        assertCode(() -> service.approveProposals(teacher, classId, new ApproveCommand(List.of(unknown, unknown))),
                "PROPOSAL_IDS_INVALID");
        classStatus(ClassStatus.ARCHIVED);
        assertCode(() -> service.approveProposals(teacher, classId, new ApproveCommand(List.of(unknown))),
                "GROUP_CLASS_ARCHIVED");
    }

    @Test
    void rejectingFreesMembersWithAnOptionalTrimmedReason() {
        GroupProposal proposal = submitted("Nhóm Mây", 0, 1);

        assertCode(() -> service.rejectProposal(teacher, proposal.id(), new RejectProposalCommand("x".repeat(1001))),
                "PROPOSAL_REASON_TOO_LONG");
        ClassProposalsView view = service.rejectProposal(teacher, proposal.id(),
                new RejectProposalCommand("  Trùng đề tài  "));

        ArgumentCaptor<GroupProposal> updated = ArgumentCaptor.forClass(GroupProposal.class);
        verify(proposals).updateProposal(updated.capture());
        assertThat(updated.getValue().status()).isEqualTo(GroupProposal.Status.REJECTED);
        assertThat(updated.getValue().decisionReason()).isEqualTo("Trùng đề tài");
        verify(proposals).deactivateMembers(proposal.id());
        ArgumentCaptor<DomainEvent> event = ArgumentCaptor.forClass(DomainEvent.class);
        verify(events).publish(event.capture());
        assertThat(event.getValue().eventType()).isEqualTo(EventTypes.GROUP_PROPOSAL_REJECTED);
        assertThat(view.classId()).isEqualTo(classId);
    }

    @Test
    void listShowsWarningsAndUngroupedStudentsWithTheirProposal() {
        GroupProposal proposal = submitted("Nhóm Mây", 0);
        when(proposals.listActiveProposals(classId)).thenReturn(List.of(proposal));
        List<ProposalMember> rows = proposals.listMembers(proposal.id());
        when(proposals.listMembersOf(List.of(proposal.id()))).thenReturn(rows);

        ClassProposalsView view = service.listClassProposals(teacher, classId);

        assertThat(view.proposals()).singleElement()
                .satisfies(p -> assertThat(p.warnings()).containsExactly("BELOW_MIN"));
        assertThat(view.ungrouped()).hasSize(8);
        assertThat(view.ungrouped().get(0).proposalId()).isEqualTo(proposal.id());
        assertThat(view.ungrouped().get(1).proposalId()).isNull();
    }

    private GroupProposal submitted(String name, int... memberIndexes) {
        GroupProposal proposal = GroupProposal.draft(UUID.randomUUID(), classId, name,
                students.get(memberIndexes[0]), NOW).submitted(NOW);
        when(proposals.lockProposal(proposal.id())).thenReturn(Optional.of(proposal));
        List<ProposalMember> rows = IntStream.of(memberIndexes).mapToObj(i -> new ProposalMember(proposal.id(), classId,
                students.get(i), ProposalMember.Status.ACCEPTED, true, null, NOW, null)).toList();
        when(proposals.listMembers(proposal.id())).thenReturn(rows);
        return proposal;
    }

    private void classStatus(ClassStatus status) {
        when(classes.getClassRef(classId)).thenReturn(Optional.of(
                new ClassRef(classId, UUID.randomUUID(), "SE1", "SE1", status, teacher.accountId(), false)));
    }

    private static void assertCode(Runnable call, String code) {
        assertThatThrownBy(call::run).isInstanceOfSatisfying(ApiException.class,
                e -> assertThat(e.code()).isEqualTo(code));
    }
}
