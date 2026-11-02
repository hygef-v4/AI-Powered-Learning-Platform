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
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.AuthorizationDecision;
import edu.aiplatform.identity.contract.IAuthorization;
import edu.aiplatform.identity.contract.Role;
import edu.aiplatform.shared.error.ApiException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GroupRenameServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-29T03:00:00Z");
    private final UUID classId = UUID.randomUUID();
    private final Actor leader = new Actor(UUID.randomUUID(), Role.STUDENT);
    private final Actor member = new Actor(UUID.randomUUID(), Role.STUDENT);
    private final GroupRepository groups = mock(GroupRepository.class);
    private final IAuthorization authorization = mock(IAuthorization.class);
    private final IClassAccess classes = mock(IClassAccess.class);
    private final IAudit audit = mock(IAudit.class);
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final GroupRenameService service = new GroupRenameService(groups,
            new GroupAccess(authorization, classes, audit, clock), audit, clock);
    private final ClassGroup group = new ClassGroup(UUID.randomUUID(), classId, "Nhóm 1", leader.accountId(), null, NOW,
            NOW, List.of(leader.accountId(), member.accountId()));
    private final ClassGroup other = new ClassGroup(UUID.randomUUID(), classId, "Nhóm 2", null, null, NOW, NOW, List.of());

    @BeforeEach
    void setUp() {
        when(authorization.authorize(any(), any(), any())).thenReturn(AuthorizationDecision.allow());
        when(classes.isActiveStudent(any(), eq(classId))).thenReturn(true);
        status(ClassStatus.OPEN);
        when(groups.findGroup(group.id())).thenReturn(Optional.of(group));
        when(groups.listGroups(classId)).thenReturn(List.of(group, other));
        when(groups.setVersion(classId)).thenReturn(4L);
    }

    @Test
    void leaderRenamesWithoutApprovalAndTheSetVersionMoves() {
        var renamed = service.rename(leader, group.id(), "  Nhóm Sao Băng  ");

        assertThat(renamed.name()).isEqualTo("Nhóm Sao Băng");
        verify(groups).updateGroup(group.id(), "Nhóm Sao Băng", leader.accountId(), NOW);
        verify(groups).bumpSetVersion(classId, 4L, NOW);
        verify(audit).record(org.mockito.ArgumentMatchers.argThat(e -> e.action().equals("GROUP_RENAMED")));
    }

    @Test
    void onlyTheLeaderRenamesAndTheNameFollowsTheGroupRules() {
        assertThatThrownBy(() -> service.rename(member, group.id(), "Nhóm mới"))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("GROUP_RENAME_LEADER_ONLY"));
        assertThatThrownBy(() -> service.rename(leader, group.id(), "   "))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("GROUP_NAME_INVALID"));
        assertThatThrownBy(() -> service.rename(leader, group.id(), "x".repeat(101)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("GROUP_NAME_INVALID"));
        assertThatThrownBy(() -> service.rename(leader, group.id(), "nhóm 2"))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("GROUP_NAME_DUPLICATE"));
        status(ClassStatus.ARCHIVED);
        assertThatThrownBy(() -> service.rename(leader, group.id(), "Nhóm mới"))
                .isInstanceOf(ApiException.class);
        verify(groups, never()).updateGroup(any(), any(), any(), any());
        verify(groups, never()).bumpSetVersion(any(), anyLong(), any());
    }

    private void status(ClassStatus status) {
        when(classes.getClassRef(classId)).thenReturn(Optional.of(
                new ClassRef(classId, UUID.randomUUID(), "SE1801", "SWP391", status, UUID.randomUUID(), true)));
    }
}
