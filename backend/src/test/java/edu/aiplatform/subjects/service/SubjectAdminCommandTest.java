package edu.aiplatform.subjects.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import edu.aiplatform.audit.contract.AuditEvent;
import edu.aiplatform.audit.contract.AuditResult;
import edu.aiplatform.audit.contract.IAudit;
import edu.aiplatform.identity.contract.*;
import edu.aiplatform.shared.error.ApiException;
import edu.aiplatform.subjects.contract.SubjectStatus;
import edu.aiplatform.subjects.repository.ISubjectRepository;
import edu.aiplatform.subjects.model.Subject;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;

class SubjectAdminCommandTest {
    private static final Instant NOW = Instant.parse("2026-10-18T08:00:00Z");
    private final UUID subjectId = UUID.randomUUID(),
            oldManager = UUID.randomUUID(),
            newManager = UUID.randomUUID();
    private final Actor admin = new Actor(UUID.randomUUID(), Role.ADMIN);
    private final ISubjectRepository subjectRepository = mock(ISubjectRepository.class);
    private final IAuthorization authorization = mock(IAuthorization.class);
    private final IAccountLookup accountLookup = mock(IAccountLookup.class);
    private final IAudit audit = mock(IAudit.class);
    private final SubjectCommandService subjectCommandService =
            new SubjectCommandService(
                    subjectRepository,
                    authorization,
                    accountLookup,
                    new SubjectAuditRecorder(audit),
                    Clock.fixed(NOW, ZoneOffset.UTC));

    @BeforeEach
    void setup() {
        when(authorization.authorize(any(), anyString(), any()))
                .thenReturn(AuthorizationDecision.allow());
        when(subjectRepository.updateSubject(any(), anyLong())).thenReturn(true);
    }

    @Test
    void createManagerIsValidatedBeforeInsert() {
        eligible(newManager);
        var after = subjectCommandService.createSubject(admin, create(newManager));
        var order = inOrder(accountLookup, subjectRepository, audit);
        order.verify(subjectRepository).hasSubjectWithCode(after.code());
        order.verify(accountLookup).lockContact(newManager);
        order.verify(subjectRepository).insertSubject(after);
        order.verify(audit).record(any());
        assertThat(after.managerId()).isEqualTo(newManager);
    }

    static Stream<Arguments> invalidManagers() {
        return Stream.of(
                Arguments.of(Role.ADMIN, AccountStatus.ACTIVE),
                Arguments.of(Role.STUDENT, AccountStatus.ACTIVE),
                Arguments.of(Role.TEACHER, AccountStatus.ACTIVE),
                Arguments.of(Role.SUBJECT_MANAGER, AccountStatus.PENDING),
                Arguments.of(Role.SUBJECT_MANAGER, AccountStatus.DISABLED));
    }

    @Test
    void unassignmentAuditContainsExplicitNull() {
        Subject before = subject(oldManager);
        when(subjectRepository.findById(subjectId)).thenReturn(Optional.of(before));
        when(subjectRepository.findByIdForUpdate(subjectId)).thenReturn(Optional.of(before));
        Subject after =
                subjectCommandService.changeManager(
                        admin, subjectId, new SubjectCommandService.ChangeManager(null, 1L));
        assertThat(after.managerId()).isNull();
        assertThat(after.version()).isEqualTo(2);
        assertThat(event().details()).containsEntry("newManagerId", null);
        verifyNoInteractions(accountLookup);
    }

    private SubjectCommandService.Create create(UUID manager) {
        return new SubjectCommandService.Create("SWR302", "Name", "Description", manager);
    }

    private Subject subject(UUID manager) {
        return Subject.createSubject(
                subjectId, "SWR302", "Name", "Description", manager, NOW.minusSeconds(60));
    }

    private void eligible(UUID id) {
        when(accountLookup.lockContact(id))
                .thenReturn(
                        Optional.of(
                                new AccountContact(
                                        id,
                                        "manager@example.test",
                                        "Manager",
                                        Role.SUBJECT_MANAGER,
                                        AccountStatus.ACTIVE)));
    }

    private AuditEvent event() {
        var captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(audit).record(captor.capture());
        return captor.getValue();
    }

    private void code(Runnable call, String code) {
        assertThatThrownBy(call::run)
                .isInstanceOfSatisfying(
                        ApiException.class, error -> assertThat(error.readCode()).isEqualTo(code));
    }
}
