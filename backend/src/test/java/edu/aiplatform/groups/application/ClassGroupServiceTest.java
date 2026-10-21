package edu.aiplatform.groups.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.aiplatform.academics.contract.ClassRef;
import edu.aiplatform.academics.contract.ClassStatus;
import edu.aiplatform.academics.contract.IClassAccess;
import edu.aiplatform.audit.contract.AuditEvent;
import edu.aiplatform.audit.contract.IAudit;
import edu.aiplatform.groupdocs.domain.GroupDocumentRepository;
import edu.aiplatform.groupdocs.domain.GroupSection;
import edu.aiplatform.groups.application.ClassGroupService.SaveCommand;
import edu.aiplatform.groups.domain.ClassGroup;
import edu.aiplatform.groups.domain.GroupRepository;
import edu.aiplatform.groups.domain.GroupSetRules.Draft;
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
    private final IAuthorization authorization = mock(IAuthorization.class);
    private final IClassAccess classes = mock(IClassAccess.class);
    private final IAudit audit = mock(IAudit.class);
    private final IEventPublisher events = mock(IEventPublisher.class);
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final ClassGroupService service = new ClassGroupService(groups, documents,
            new GroupAccess(authorization, classes, audit, clock), classes, mock(IAccountLookup.class), audit, events, clock,
            new Random(7));
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

        assertThatThrownBy(() -> service.save(teacher, classId, new SaveCommand(3L, drafts)))
                .isInstanceOfSatisfying(GroupSetInvalidException.class, e -> assertThat(e.issues())
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

        assertThatThrownBy(() -> service.save(teacher, classId, new SaveCommand(3L, List.of())))
                .isInstanceOfSatisfying(GroupSetInvalidException.class,
                        e -> assertThat(e.issues()).extracting(i -> i.code()).containsExactly("GROUP_IN_USE"));
        verify(groups, never()).deleteGroup(any());
    }

    @Test
    void staleVersionAndArchivedClassAreRefused() {
        assertThatThrownBy(() -> service.save(teacher, classId, new SaveCommand(2L, List.of())))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("GROUP_SET_VERSION_CONFLICT"));
        classStatus(ClassStatus.ARCHIVED);
        assertThatThrownBy(() -> service.save(teacher, classId, new SaveCommand(3L, List.of())))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("GROUP_CLASS_ARCHIVED"));
    }

    @Test
    void movingMemberReleasesHeldSectionsAndCancelsPendingOnLeaderChange() {
        ClassGroup a = group("Nhóm A", students.get(0), students.get(0), students.get(1));
        ClassGroup b = group("Nhóm B", students.get(2), students.get(2), students.get(3));
        when(groups.listGroups(classId)).thenReturn(List.of(a, b));
        GroupSection held = new GroupSection(UUID.randomUUID(), UUID.randomUUID(), "p1", null, List.of(), 1, "Mục 1",
                GroupSection.Status.CLAIMED, students.get(1), NOW, null, null, List.of(), NOW, null, 4);
        when(documents.lockSectionsHeldBy(a.id(), students.get(1))).thenReturn(List.of(held));
        when(documents.updateSection(any())).thenReturn(true);

        // Học sinh 1 chuyển từ A sang B; trưởng nhóm B đổi sang học sinh 3.
        service.save(teacher, classId, new SaveCommand(3L, List.of(
                new Draft(null, a.id(), "Nhóm A", students.get(0), List.of(students.get(0))),
                new Draft(null, b.id(), "Nhóm B", students.get(3), List.of(students.get(2), students.get(3), students.get(1))))));

        ArgumentCaptor<GroupSection> released = ArgumentCaptor.forClass(GroupSection.class);
        verify(documents).updateSection(released.capture());
        assertThat(released.getValue().status()).isEqualTo(GroupSection.Status.OPEN);
        assertThat(released.getValue().claimedBy()).isNull();
        verify(groups).removeMembers(classId, java.util.Set.of(students.get(1)));
        verify(groups).addMember(b.id(), classId, students.get(1), NOW);
        verify(groups).cancelPendingOf(eq(b.id()), eq(teacher.accountId()), any(), eq(NOW));
        ArgumentCaptor<DomainEvent> published = ArgumentCaptor.forClass(DomainEvent.class);
        verify(events, org.mockito.Mockito.times(2)).publish(published.capture());
        assertThat(published.getAllValues()).extracting(DomainEvent::eventType)
                .containsExactly("group.membership-changed", "group.leader-changed");
        ArgumentCaptor<AuditEvent> audited = ArgumentCaptor.forClass(AuditEvent.class);
        verify(audit, org.mockito.Mockito.atLeastOnce()).record(audited.capture());
        assertThat(audited.getAllValues()).extracting(AuditEvent::action)
                .contains("GROUP_SECTION_RELEASED", "GROUP_LEADER_CHANGED", "GROUP_UPDATED");
    }

    @Test
    void studentWhoLeftClassIsDroppedOnSave() {
        UUID left = UUID.randomUUID();
        ClassGroup a = group("Nhóm A", students.get(0), students.get(0), left);
        when(groups.listGroups(classId)).thenReturn(List.of(a));

        service.save(teacher, classId, new SaveCommand(3L, List.of(
                new Draft(null, a.id(), "Nhóm A", students.get(0), List.of(students.get(0), left)))));

        verify(groups).removeMembers(classId, java.util.Set.of(left));
        verify(groups, never()).addMember(any(), any(), eq(left), any());
    }

    @Test
    void randomPreviewKeepsExistingGroupsAndSplitsTheRestEvenly() {
        ClassGroup a = group("Nhóm 1", students.get(0), students.get(0));
        when(groups.listGroups(classId)).thenReturn(List.of(a));

        var preview = service.randomPreview(teacher, classId, 4);

        assertThat(preview.get(0).id()).isEqualTo(a.id());
        var generated = preview.stream().filter(ClassGroupService.DraftView::generated).toList();
        assertThat(generated).extracting(d -> d.memberIds().size()).containsExactlyInAnyOrder(3, 3);
        assertThat(generated).extracting(ClassGroupService.DraftView::name).containsExactly("Nhóm 2", "Nhóm 3");
        generated.forEach(d -> assertThat(d.memberIds()).contains(d.leaderId()).doesNotContain(students.get(0)));
        assertThatThrownBy(() -> service.randomPreview(teacher, classId, 21))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("GROUP_SIZE_INVALID"));
        verify(groups, never()).insertGroup(any());
    }

    @Test
    void teacherOfAnotherClassGetsNotFound() {
        Actor other = new Actor(UUID.randomUUID(), Role.TEACHER);
        when(authorization.authorize(eq(other), any(), any())).thenReturn(AuthorizationDecision.deny("OUT_OF_CLASS_SCOPE"));

        assertThatThrownBy(() -> service.view(other, classId))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo(GroupAccess.NOT_FOUND));
        verify(audit).record(any());
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
