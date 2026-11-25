package edu.aiplatform.groups.service;

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
import edu.aiplatform.groups.model.GroupFormationSettings;
import edu.aiplatform.groups.repository.GroupProposalRepository;
import edu.aiplatform.groups.service.GroupFormationService.FormationView;
import edu.aiplatform.groups.service.GroupFormationService.UpdateFormationCommand;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.AuthorizationDecision;
import edu.aiplatform.identity.contract.IAuthorization;
import edu.aiplatform.identity.contract.Role;
import edu.aiplatform.shared.error.ApiException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class GroupFormationServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-11T08:00:00Z");
    private final UUID classId = UUID.randomUUID();
    private final Actor teacher = new Actor(UUID.randomUUID(), Role.TEACHER);
    private final GroupProposalRepository proposals = mock(GroupProposalRepository.class);
    private final IAuthorization authorization = mock(IAuthorization.class);
    private final IClassAccess classes = mock(IClassAccess.class);
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final GroupFormationService service = new GroupFormationService(proposals, new GroupAccess(authorization, classes), clock);

    @BeforeEach
    void setUp() {
        when(authorization.authorize(eq(teacher), any(), any())).thenReturn(AuthorizationDecision.allow());
        classStatus(ClassStatus.OPEN);
        when(proposals.saveSettings(any(), anyLong())).thenReturn(true);
    }

    @Test
    void invalidSizesDeadlineAndArchivedClassAreRefused() {
        assertCode(new UpdateFormationCommand(true, 1, 4, null, 0L), "FORMATION_SIZE_INVALID");
        assertCode(new UpdateFormationCommand(true, 5, 4, null, 0L), "FORMATION_SIZE_INVALID");
        assertCode(new UpdateFormationCommand(true, 2, 4, NOW, 0L), "FORMATION_DEADLINE_PAST");
        assertCode(new UpdateFormationCommand(true, 2, 4, null, null), "FORMATION_INVALID");
        assertCode(new UpdateFormationCommand(true, 2, 4, null, 3L), "FORMATION_VERSION_CONFLICT");
        classStatus(ClassStatus.ARCHIVED);
        assertCode(new UpdateFormationCommand(true, 2, 4, null, 0L), "GROUP_CLASS_ARCHIVED");
        verify(proposals, never()).saveSettings(any(), anyLong());
    }

    private void assertCode(UpdateFormationCommand command, String code) {
        assertThatThrownBy(() -> service.updateFormation(teacher, classId, command))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.readCode()).isEqualTo(code));
    }

    private void classStatus(ClassStatus status) {
        when(classes.getClassRef(classId)).thenReturn(Optional.of(
                new ClassRef(classId, UUID.randomUUID(), "SE1", "SE1", status, teacher.accountId(), false)));
    }
}
