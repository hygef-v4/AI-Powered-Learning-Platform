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
import edu.aiplatform.audit.contract.IAudit;
import edu.aiplatform.groups.domain.ClassGroup;
import edu.aiplatform.groups.domain.GroupRepository;
import edu.aiplatform.groups.domain.LeaderChangeRequest;
import edu.aiplatform.groups.domain.LeaderChangeRequest.Status;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.AuthorizationDecision;
import edu.aiplatform.identity.contract.IAuthorization;
import edu.aiplatform.identity.contract.Role;
import edu.aiplatform.jobs.contract.DomainEvent;
import edu.aiplatform.jobs.contract.IEventPublisher;
import edu.aiplatform.shared.error.ApiException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class LeaderRequestServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-22T03:00:00Z");
    private static final String REASON = "Trưởng nhóm bận thực tập, không theo được bài.";
    private final UUID classId = UUID.randomUUID();
    private final Actor teacher = new Actor(UUID.randomUUID(), Role.TEACHER);
    private final Actor leader = new Actor(UUID.randomUUID(), Role.STUDENT);
    private final Actor member = new Actor(UUID.randomUUID(), Role.STUDENT);
    private final Actor outsider = new Actor(UUID.randomUUID(), Role.STUDENT);
    private final GroupRepository groups = mock(GroupRepository.class);
    private final IAuthorization authorization = mock(IAuthorization.class);
    private final IClassAccess classes = mock(IClassAccess.class);
    private final IAudit audit = mock(IAudit.class);
    private final IEventPublisher events = mock(IEventPublisher.class);
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final LeaderRequestService service = new LeaderRequestService(groups,
            new GroupAccess(authorization, classes, audit, clock), classes, audit, events, clock);
    private final ClassGroup group = new ClassGroup(UUID.randomUUID(), classId, "Nhóm 1", leader.accountId(), null, NOW,
            NOW, List.of(leader.accountId(), member.accountId()));

    @BeforeEach
    void setUp() {
        when(authorization.authorize(any(), any(), any())).thenReturn(AuthorizationDecision.allow());
        when(classes.isActiveStudent(any(), eq(classId))).thenReturn(true);
        status(ClassStatus.OPEN);
        when(groups.findGroup(group.id())).thenReturn(Optional.of(group));
        when(groups.findPending(group.id())).thenReturn(Optional.empty());
    }

    @Test
    void memberSubmitsWithValidReasonAndTeacherIsNotified() {
        var result = service.submit(member, group.id(), "  " + REASON + "  ", null);

        assertThat(result.status()).isEqualTo(Status.PENDING);
        ArgumentCaptor<LeaderChangeRequest> saved = ArgumentCaptor.forClass(LeaderChangeRequest.class);
        verify(groups).insertRequest(saved.capture());
        assertThat(saved.getValue().reason()).isEqualTo(REASON);
        ArgumentCaptor<DomainEvent> event = ArgumentCaptor.forClass(DomainEvent.class);
        verify(events).publish(event.capture());
        assertThat(event.getValue().eventType()).isEqualTo("group.leader-change-requested");
        assertThat(event.getValue().payload().get("recipientIds")).isEqualTo(List.of(teacher.accountId().toString()));
    }

    @Test
    void submitRejectsShortReasonBadProposalSecondPendingOutsiderAndClosedClass() {
        assertThatThrownBy(() -> service.submit(member, group.id(), "ngắn", null))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("LEADER_REQUEST_REASON_INVALID"));
        assertThatThrownBy(() -> service.submit(member, group.id(), REASON, leader.accountId()))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("LEADER_REQUEST_PROPOSAL_INVALID"));
        assertThatThrownBy(() -> service.submit(outsider, group.id(), REASON, null))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo(GroupAccess.NOT_FOUND));
        when(groups.findPending(group.id())).thenReturn(Optional.of(pending(null)));
        assertThatThrownBy(() -> service.submit(member, group.id(), REASON, null))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("LEADER_REQUEST_PENDING"));
        status(ClassStatus.DRAFT);
        assertThatThrownBy(() -> service.submit(member, group.id(), REASON, null))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("GROUP_CLASS_NOT_OPEN"));
        verify(groups, never()).insertRequest(any());
    }

    @Test
    void onlyRequesterCancels() {
        LeaderChangeRequest request = pending(null);
        when(groups.lockRequest(request.id())).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> service.cancel(leader, request.id())).isInstanceOf(ApiException.class);
        assertThat(service.cancel(member, request.id()).status()).isEqualTo(Status.CANCELLED);
    }

    @Test
    void approveUsesProposalByDefaultAndRequiresAChoiceOtherwise() {
        LeaderChangeRequest proposed = pending(member.accountId());
        when(groups.lockRequest(proposed.id())).thenReturn(Optional.of(proposed));
        when(groups.setVersion(classId)).thenReturn(5L);

        var approved = service.approve(teacher, proposed.id(), null);

        assertThat(approved.newLeaderId()).isEqualTo(member.accountId());
        verify(groups).setLeader(group.id(), member.accountId(), NOW);
        verify(groups).bumpSetVersion(classId, 5L, NOW);

        LeaderChangeRequest open = pending(null);
        when(groups.lockRequest(open.id())).thenReturn(Optional.of(open));
        assertThatThrownBy(() -> service.approve(teacher, open.id(), null))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("LEADER_REQUIRED"));
        assertThatThrownBy(() -> service.approve(teacher, open.id(), outsider.accountId()))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("LEADER_NOT_MEMBER"));
    }

    @Test
    void rejectNeedsReasonAndClosedRequestCannotBeDecidedAgain() {
        LeaderChangeRequest request = pending(null);
        when(groups.lockRequest(request.id())).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> service.reject(teacher, request.id(), " "))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("REJECT_REASON_REQUIRED"));
        assertThat(service.reject(teacher, request.id(), "Nhóm đã ổn định").status()).isEqualTo(Status.REJECTED);

        LeaderChangeRequest done = request.decide(Status.REJECTED, null, teacher.accountId(), "x", NOW);
        when(groups.lockRequest(request.id())).thenReturn(Optional.of(done));
        assertThatThrownBy(() -> service.approve(teacher, request.id(), member.accountId()))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("LEADER_REQUEST_CLOSED"));
        verify(groups, never()).bumpSetVersion(any(), anyLong(), any());
    }

    private LeaderChangeRequest pending(UUID proposed) {
        return new LeaderChangeRequest(UUID.randomUUID(), group.id(), member.accountId(), proposed, REASON, Status.PENDING,
                null, null, null, null, NOW);
    }

    private void status(ClassStatus status) {
        when(classes.getClassRef(classId)).thenReturn(Optional.of(
                new ClassRef(classId, UUID.randomUUID(), "SE1801", "SWP391", status, teacher.accountId(), true)));
    }
}
