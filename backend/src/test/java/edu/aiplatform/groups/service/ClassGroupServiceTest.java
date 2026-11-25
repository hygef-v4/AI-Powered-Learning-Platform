package edu.aiplatform.groups.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import edu.aiplatform.academics.contract.ClassRef;
import edu.aiplatform.academics.contract.ClassStatus;
import edu.aiplatform.academics.contract.IClassAccess;
import edu.aiplatform.assignments.contract.DocumentModel;
import edu.aiplatform.assignments.contract.IAssignmentQuery;
import edu.aiplatform.groupdocs.service.GroupDocumentService;
import edu.aiplatform.groupdocs.service.IGroupDocStreams;
import edu.aiplatform.groupdocs.repository.GroupDocumentRepository;
import edu.aiplatform.groupdocs.model.GroupDocument;
import edu.aiplatform.groupdocs.model.GroupSection;
import edu.aiplatform.groups.service.ClassGroupService.SaveCommand;
import edu.aiplatform.groups.model.ClassGroup;
import edu.aiplatform.groups.repository.GroupRepository;
import edu.aiplatform.groups.model.GroupSetRules.Draft;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.AuthorizationDecision;
import edu.aiplatform.identity.contract.IAccountLookup;
import edu.aiplatform.identity.contract.IAuthorization;
import edu.aiplatform.identity.contract.Role;
import edu.aiplatform.jobs.contract.DomainEvent;
import edu.aiplatform.jobs.contract.IEventPublisher;
import edu.aiplatform.shared.error.ApiException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ClassGroupServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-22T03:00:00Z");
    private final UUID classId = UUID.randomUUID();
    private final Actor teacher = new Actor(UUID.randomUUID(), Role.TEACHER);
    private final GroupRepository groups = mock(GroupRepository.class);
    private final GroupDocumentRepository documents = mock(GroupDocumentRepository.class);
    private final IAssignmentQuery assignments = mock(IAssignmentQuery.class);
    private final IGroupDocStreams streams = mock(IGroupDocStreams.class);
    private final IAuthorization authorization = mock(IAuthorization.class);
    private final IClassAccess classes = mock(IClassAccess.class);
    private final IEventPublisher events = mock(IEventPublisher.class);
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final GroupDocumentService groupDocuments = new GroupDocumentService(documents, groups, assignments,
            classes, clock);
    private final GroupProposalCleanup proposalCleanup = mock(GroupProposalCleanup.class);
    private final ClassGroupService service = new ClassGroupService(groups, documents, groupDocuments, streams,
            new GroupAccess(authorization, classes), classes, mock(IAccountLookup.class), events, clock,
            new Random(7), proposalCleanup);
    private final List<UUID> students = IntStream.range(0, 7).mapToObj(i -> UUID.randomUUID()).toList();

    @BeforeEach
    void setUp() {
        when(authorization.authorize(eq(teacher), any(), any())).thenReturn(AuthorizationDecision.allow());
        classStatus(ClassStatus.OPEN);
        when(classes.listActiveStudents(classId)).thenReturn(students);
        when(groups.setVersion(classId)).thenReturn(3L);
        when(groups.bumpSetVersion(eq(classId), anyLong(), any())).thenReturn(true);
    }

    @Test
    void conflictingSetIsRejectedWholeWithEveryIssue() {
        List<Draft> drafts = List.of(
                new Draft("a", null, "Nhóm 1", students.get(0), List.of(students.get(0), students.get(1))),
                new Draft("b", null, "nhóm 1", students.get(5), List.of(students.get(1), students.get(2))));

        assertThatThrownBy(() -> service.saveGroups(teacher, classId, new SaveCommand(3L, drafts)))
                .isInstanceOfSatisfying(GroupSetInvalidException.class, e -> assertThat(e.listIssues())
                        .extracting(i -> i.code())
                        .contains("GROUP_NAME_DUPLICATE", "GROUP_WITHOUT_LEADER", "MEMBER_IN_TWO_GROUPS"));
        verify(groups, never()).bumpSetVersion(any(), anyLong(), any());
        verify(groups, never()).insertGroup(any());
    }

    @Test
    void usedGroupCannotBeDeleted() {
        ClassGroup used = group("Nhóm A", students.get(0), students.get(0), students.get(1));
        when(groups.listGroups(classId)).thenReturn(List.of(used));
        when(documents.groupHasContent(used.id())).thenReturn(true);

        assertThatThrownBy(() -> service.saveGroups(teacher, classId, new SaveCommand(3L, List.of())))
                .isInstanceOfSatisfying(GroupSetInvalidException.class,
                        e -> assertThat(e.listIssues()).extracting(i -> i.code()).containsExactly("GROUP_IN_USE"));
        verify(groups, never()).deleteGroup(any());
    }

    @Test
    void staleVersionAndArchivedClassAreRefused() {
        assertThatThrownBy(() -> service.saveGroups(teacher, classId, new SaveCommand(2L, List.of())))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.readCode()).isEqualTo("GROUP_SET_VERSION_CONFLICT"));
        classStatus(ClassStatus.ARCHIVED);
        assertThatThrownBy(() -> service.saveGroups(teacher, classId, new SaveCommand(3L, List.of())))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.readCode()).isEqualTo("GROUP_CLASS_ARCHIVED"));
    }

    @Test
    void movingMemberReleasesHeldSectionsAndCancelsPendingOnLeaderChange() {
        ClassGroup a = group("Nhóm A", students.get(0), students.get(0), students.get(1));
        ClassGroup b = group("Nhóm B", students.get(2), students.get(2), students.get(3));
        when(groups.listGroups(classId)).thenReturn(List.of(a, b));
        UUID assignmentId = UUID.randomUUID();
        GroupDocument groupDocument = GroupDocument.createDocument(UUID.randomUUID(), a.id(), assignmentId, classId,
                new DocumentModel(List.of()), false, NOW);
        when(documents.listByGroup(a.id())).thenReturn(List.of(groupDocument));
        when(documents.lockDocument(groupDocument.id())).thenReturn(Optional.of(groupDocument));
        GroupSection held = new GroupSection(UUID.randomUUID(), groupDocument.id(), "p1", null, List.of(), 1, "Mục 1",
                GroupSection.Status.CLAIMED, students.get(1), NOW, null, null, List.of(), NOW, null, 4);
        when(documents.lockSectionsHeldBy(List.of(groupDocument.id()), students.get(1))).thenReturn(List.of(held));
        when(documents.updateSection(any())).thenReturn(true);

        // Học sinh 1 chuyển từ A sang B; trưởng nhóm B đổi sang học sinh 3.
        service.saveGroups(teacher, classId, new SaveCommand(3L, List.of(
                new Draft(null, a.id(), "Nhóm A", students.get(0), List.of(students.get(0))),
                new Draft(null, b.id(), "Nhóm B", students.get(3), List.of(students.get(2), students.get(3), students.get(1))))));

        var locks = inOrder(assignments, documents);
        locks.verify(assignments).lockSubmission(assignmentId);
        locks.verify(documents).lockDocument(groupDocument.id());
        locks.verify(documents).lockSectionsHeldBy(List.of(groupDocument.id()), students.get(1));

        ArgumentCaptor<GroupSection> released = ArgumentCaptor.forClass(GroupSection.class);
        verify(documents).updateSection(released.capture());
        assertThat(released.getValue().status()).isEqualTo(GroupSection.Status.OPEN);
        assertThat(released.getValue().claimedBy()).isNull();
        verify(groups).removeMembers(classId, java.util.Set.of(students.get(1)));
        verify(groups).addMember(b.id(), classId, students.get(1), NOW);
        verify(groups).cancelPendingOf(eq(b.id()), eq(teacher.accountId()), any(), eq(NOW));
        verify(streams).closeFor(eq(students.get(1)), any());
        ArgumentCaptor<DomainEvent> published = ArgumentCaptor.forClass(DomainEvent.class);
        verify(events, org.mockito.Mockito.times(2)).publish(published.capture());
        assertThat(published.getAllValues()).extracting(DomainEvent::eventType)
                .containsExactly("group.membership-changed", "group.leader-changed");
        // U14 bắt buộc groupId; người chuyển nhóm nhận thông báo theo nhóm mới.
        var membership = published.getAllValues().get(0).payload();
        assertThat(membership).containsEntry("groupId", b.id().toString())
                .containsEntry("recipientIds", List.of(students.get(1).toString()));
        assertThat(
                edu.aiplatform.notifications.service.RecipientResolver
                        .readId(membership, "groupId")).isEqualTo(b
                        .id());
    }

    @Test
    void studentWhoLeftClassIsDroppedOnSave() {
        UUID left = UUID.randomUUID();
        ClassGroup a = group("Nhóm A", students.get(0), students.get(0), left);
        when(groups.listGroups(classId)).thenReturn(List.of(a));

        service.saveGroups(teacher, classId, new SaveCommand(3L, List.of(
                new Draft(null, a.id(), "Nhóm A", students.get(0), List.of(students.get(0), left)))));

        verify(groups).removeMembers(classId, java.util.Set.of(left));
        verify(groups, never()).addMember(any(), any(), eq(left), any());
    }

    @Test
    void randomPreviewKeepsExistingGroupsAndSplitsTheRestEvenly() {
        ClassGroup a = group("Nhóm 1", students.get(0), students.get(0));
        when(groups.listGroups(classId)).thenReturn(List.of(a));

        var preview = service.previewRandomGroups(teacher, classId, 4);

        assertThat(preview.get(0).id()).isEqualTo(a.id());
        var generated = preview.stream().filter(ClassGroupService.DraftView::generated).toList();
        assertThat(generated).extracting(d -> d.memberIds().size()).containsExactlyInAnyOrder(3, 3);
        assertThat(generated).extracting(ClassGroupService.DraftView::name).containsExactly("Nhóm 2", "Nhóm 3");
        generated.forEach(d -> assertThat(d.memberIds()).contains(d.leaderId()).doesNotContain(students.get(0)));
        assertThatThrownBy(() -> service.previewRandomGroups(teacher, classId, 21))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.readCode()).isEqualTo("GROUP_SIZE_INVALID"));
        verify(groups, never()).insertGroup(any());
    }

    @Test
    void teacherOfAnotherClassGetsNotFound() {
        Actor other = new Actor(UUID.randomUUID(), Role.TEACHER);
        when(authorization.authorize(eq(other), any(), any())).thenReturn(AuthorizationDecision.deny("OUT_OF_CLASS_SCOPE"));

        assertThatThrownBy(() -> service.view(other, classId))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.readCode()).isEqualTo(GroupAccess.NOT_FOUND));
    }

    @Test
    void lostSetVersionCasStopsBeforeMembershipDocumentAndNotificationMutations() {
        when(groups.bumpSetVersion(classId, 3L, NOW)).thenReturn(false);
        var command = new SaveCommand(3L, List.of(new Draft("new", null, "New group", students.get(0),
                List.of(students.get(0), students.get(1)))));

        assertThatThrownBy(() -> service.saveGroups(teacher, classId, command))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.readCode()).isEqualTo("GROUP_SET_VERSION_CONFLICT"));

        verify(groups, never()).insertGroup(any());
        verify(groups, never()).removeMembers(any(), any());
        verify(groups, never()).deleteGroup(any());
        verifyNoInteractions(documents, assignments, streams, events);
    }

    @Test
    void savingReleasesNewlyPlacedStudentsFromTheirProposals() {
        ClassGroup kept = group("Nhóm A", students.get(0), students.get(0), students.get(1));
        when(groups.listGroups(classId)).thenReturn(List.of(kept));
        List<Draft> drafts = List.of(
                new Draft(kept.id().toString(), kept.id(), "Nhóm A", students.get(0),
                        List.of(students.get(0), students.get(1), students.get(2))),
                new Draft("new", null, "Nhóm B", students.get(3), List.of(students.get(3), students.get(4))));

        service.saveGroups(teacher, classId, new SaveCommand(3L, drafts));

        verify(proposalCleanup).releasePlacedStudents(classId,
                java.util.Set.of(students.get(2), students.get(3), students.get(4)), NOW);
    }

    @Test
    void approvedGroupsKeepExistingOnesAndMarkTheMembershipEvent() {
        ClassGroup kept = group("Nhóm A", students.get(0), students.get(0), students.get(1));
        when(groups.listGroups(classId)).thenReturn(List.of(kept));
        Draft approved = new Draft("proposal-1", null, "Nhóm Mây", students.get(2),
                List.of(students.get(2), students.get(3)));

        var created = service.addApprovedGroups(teacher, classId, 3L, List.of(approved));

        assertThat(created).containsOnlyKeys("proposal-1");
        verify(groups).insertGroup(any());
        verify(groups, never()).updateGroup(any(), any(), any(), any());
        verify(groups, never()).deleteGroup(any());
        ArgumentCaptor<DomainEvent> event = ArgumentCaptor.forClass(DomainEvent.class);
        verify(events).publish(event.capture());
        assertThat(event.getValue().payload()).containsEntry("reason", ClassGroupService.PROPOSAL_APPROVED);
        verify(proposalCleanup, never()).releasePlacedStudents(any(), any(), any());
    }

    @Test
    void approvedGroupsStopOnALostSetVersion() {
        when(groups.bumpSetVersion(eq(classId), anyLong(), any())).thenReturn(false);

        assertThatThrownBy(() -> service.addApprovedGroups(teacher, classId, 3L, List.of()))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.readCode()).isEqualTo("GROUP_SET_VERSION_CONFLICT"));
        verify(groups, never()).insertGroup(any());
    }

    private void classStatus(ClassStatus status) {
        when(classes.getClassRef(classId)).thenReturn(Optional.of(
                new ClassRef(classId, UUID.randomUUID(), "SE1801", "SWP391", status, teacher.accountId(), true)));
    }

    private ClassGroup group(String name, UUID leader, UUID... members) {
        return new ClassGroup(UUID.randomUUID(), classId, name, leader, teacher.accountId(), NOW, NOW,
                new ArrayList<>(List.of(members)));
    }
}
