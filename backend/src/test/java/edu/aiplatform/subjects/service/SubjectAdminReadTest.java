package edu.aiplatform.subjects.service;

import edu.aiplatform.subjects.dto.SubjectAdminViews;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import edu.aiplatform.identity.contract.*;
import edu.aiplatform.shared.error.ApiException;
import edu.aiplatform.subjects.contract.ISubjectClasses;
import edu.aiplatform.subjects.contract.SubjectStatus;
import edu.aiplatform.subjects.repository.ISubjectRepository;
import edu.aiplatform.subjects.model.Subject;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class SubjectAdminReadTest {
    private final UUID id = UUID.randomUUID();
    private final UUID managerId = UUID.randomUUID();
    private final Actor admin = new Actor(UUID.randomUUID(), Role.ADMIN);
    private final ISubjectRepository repository = mock(ISubjectRepository.class);
    private final IAuthorization authorization = mock(IAuthorization.class);
    private final IAccountLookup accounts = mock(IAccountLookup.class);
    private final ISubjectClasses provider = mock(ISubjectClasses.class);
    private final SubjectClassesReader reader = new SubjectClassesReader(provider);
    private final edu.aiplatform.audit.contract.IAudit audit = mock(edu.aiplatform.audit.contract.IAudit.class);
    private final SubjectAdminService service = new SubjectAdminService(repository, authorization, accounts, reader,
            new SubjectAuditRecorder(audit), java.time.Clock.systemUTC());

    @BeforeEach
    void authorize() {
        when(authorization.authorize(any(), anyString(), any())).thenReturn(AuthorizationDecision.allow());
    }

    @AfterEach
    void close() {
        reader.close();
    }

    @Test
    void anonymousCannotReadAnyOfTheThreeViews() {
        assertCode(() -> service.list(null, null, null, null, null), "AUTHENTICATION_REQUIRED");
        assertCode(() -> service.detail(null, id), "AUTHENTICATION_REQUIRED");
        assertCode(() -> service.classes(null, id), "AUTHENTICATION_REQUIRED");
        verifyNoInteractions(repository, accounts, provider, authorization);
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = "ADMIN", mode = EnumSource.Mode.EXCLUDE)
    void otherRolesCannotReadEvenWithAllowingAuthorizationStub(Role role) {
        Actor actor = new Actor(UUID.randomUUID(), role);
        assertCode(() -> service.list(actor, null, null, null, null), "SUBJECT_FORBIDDEN");
        assertCode(() -> service.detail(actor, id), "SUBJECT_FORBIDDEN");
        assertCode(() -> service.classes(actor, id), "SUBJECT_FORBIDDEN");
        verifyNoInteractions(repository, accounts, provider, authorization);
    }

    @Test
    void adminDeniedByU01CannotReadSubjectExistence() {
        when(authorization.authorize(any(), anyString(), any())).thenReturn(AuthorizationDecision.deny("DISABLED"));
        assertCode(() -> service.detail(admin, id), "SUBJECT_FORBIDDEN");
        verify(authorization).authorize(admin, "SUBJECT_VIEW", ResourceRef.subject(id));
        verifyNoInteractions(repository, accounts, provider);
    }

    @Test
    void authorizationErrorOrNullFailsClosedBeforeRepository() {
        when(authorization.authorize(any(), anyString(), any())).thenThrow(new IllegalStateException("internal"));
        assertCode(() -> service.list(admin, null, null, 0, 20), "SUBJECT_AUTHORIZATION_UNAVAILABLE");
        doReturn(null).when(authorization).authorize(any(), anyString(), any());
        assertCode(() -> service.classes(admin, id), "SUBJECT_AUTHORIZATION_UNAVAILABLE");
        verifyNoInteractions(repository, accounts, provider);
    }

    @Test
    void defaultsAndEmptyResultDoNotCallContactOrClassesProviders() {
        when(repository.countByStatus(null)).thenReturn(Map.of(SubjectStatus.ACTIVE, 0L, SubjectStatus.ARCHIVED, 0L));
        var page = service.list(admin, null, null, null, null);
        assertThat(page.page()).isZero();
        assertThat(page.size()).isEqualTo(20);
        assertThat(page.items()).isEmpty();
        assertThat(page.totalElements()).isZero();
        assertThat(page.statusCounts()).containsEntry(SubjectStatus.ACTIVE, 0L);
        verify(repository).findPage(null, null, 0, 20);
        verify(authorization).authorize(admin, "SUBJECT_LIST", ResourceRef.none());
        verifyNoInteractions(accounts, provider);
    }

    @Test
    void paginationSearchCountsAndManagerDedupFollowCurrentPage() {
        Subject first = subject(id, "AAA", managerId);
        Subject second = subject(UUID.randomUUID(), "BBB", managerId);
        when(repository.findPage("math", SubjectStatus.ACTIVE, 2, 10)).thenReturn(List.of(first, second));
        when(repository.count("math", SubjectStatus.ACTIVE)).thenReturn(25L);
        when(repository.countByStatus("math")).thenReturn(Map.of(SubjectStatus.ACTIVE, 25L, SubjectStatus.ARCHIVED, 4L));
        when(accounts.getContact(managerId)).thenReturn(Optional.of(contact()));
        when(provider.countClasses(List.of(first.id(), second.id()))).thenReturn(Map.of(first.id(), 0, second.id(), 3));
        var page = service.list(admin, " math ", SubjectStatus.ACTIVE, 2, 10);
        assertThat(page.items()).extracting(SubjectAdminViews.ListItem::code).containsExactly("AAA", "BBB");
        assertThat(page.items()).extracting(SubjectAdminViews.ListItem::classCount).containsExactly(0, 3);
        assertThat(page.items().get(0).manager().displayName()).isEqualTo("Manager");
        assertThat(page.totalElements()).isEqualTo(25);
        assertThat(page.statusCounts()).containsEntry(SubjectStatus.ARCHIVED, 4L);
        verify(accounts, times(1)).getContact(managerId);
        verify(provider, times(1)).countClasses(List.of(first.id(), second.id()));
    }

    @Test
    void unassignedSubjectDoesNotLookUpAnAccount() {
        when(repository.findById(id)).thenReturn(Optional.of(subject(id, "AAA", null)));
        when(provider.countClasses(List.of(id))).thenReturn(Map.of(id, 0));
        var detail = service.detail(admin, id);
        assertThat(detail.managerId()).isNull();
        assertThat(detail.manager()).isNull();
        assertThat(detail.classCount()).isZero();
        verifyNoInteractions(accounts);
    }

    @Test
    void missingContactKeepsAssignmentAndNoContactCacheBetweenRequests() {
        when(repository.findById(id)).thenReturn(Optional.of(subject(id, "AAA", managerId)));
        when(accounts.getContact(managerId)).thenReturn(Optional.empty(), Optional.of(contact()));
        var first = service.detail(admin, id);
        var second = service.detail(admin, id);
        assertThat(first.managerId()).isEqualTo(managerId);
        assertThat(first.manager()).isNull();
        assertThat(second.manager().accountId()).isEqualTo(managerId);
        verify(accounts, times(2)).getContact(managerId);
    }

    @Test
    void providerErrorsDegradeDisplayWithoutLosingCoreDetail() {
        when(repository.findById(id)).thenReturn(Optional.of(subject(id, "AAA", managerId)));
        when(accounts.getContact(managerId)).thenThrow(new IllegalStateException("unavailable"));
        when(provider.countClasses(any())).thenThrow(ApiException.unavailable("SUBJECT_CLASSES_UNAVAILABLE", "retry"));
        var detail = service.detail(admin, id);
        assertThat(detail.id()).isEqualTo(id);
        assertThat(detail.managerId()).isEqualTo(managerId);
        assertThat(detail.manager()).isNull();
        assertThat(detail.classCount()).isNull();
        assertThat(detail.description()).isEqualTo("Description");
        assertThat(detail.version()).isEqualTo(1);
    }

    @Test
    void databaseFailureIsNotConvertedToEmptyOrProviderUnavailable() {
        var failure = new IllegalStateException("DB failure");
        when(repository.findById(id)).thenThrow(failure);
        assertThatThrownBy(() -> service.detail(admin, id)).isSameAs(failure);
        verifyNoInteractions(accounts, provider);
    }

    @Test
    void missingSubjectIs404BeforeClassesProvider() {
        assertCode(() -> service.detail(admin, id), "SUBJECT_NOT_FOUND");
        assertCode(() -> service.classes(admin, id), "SUBJECT_NOT_FOUND");
        verifyNoInteractions(accounts, provider);
    }

    @Test
    void emptyClassesAreAvailableButUnavailableIsDistinct() {
        when(repository.findById(id)).thenReturn(Optional.of(subject(id, "AAA", null)));
        when(provider.listClassesOfSubject(id)).thenReturn(List.of()).thenThrow(new IllegalStateException("down"));
        var available = service.classes(admin, id);
        assertThat(available.availability()).isEqualTo(SubjectAdminViews.Availability.AVAILABLE);
        assertThat(available.items()).isEmpty();
        var unavailable = service.classes(admin, id);
        assertThat(unavailable.availability()).isEqualTo(SubjectAdminViews.Availability.UNAVAILABLE);
        assertThat(unavailable.items()).isNull();
    }

    @Test
    void invalidPaginationIsRejectedBeforeReading() {
        for (int[] input : List.of(new int[]{-1, 20}, new int[]{0, 0}, new int[]{0, 101})) {
            assertCode(() -> service.list(admin, null, null, input[0], input[1]), "SUBJECT_PAGE_INVALID");
        }
        verifyNoInteractions(repository, accounts, provider);
    }

    private Subject subject(UUID subjectId, String code, UUID manager) {
        return Subject.create(subjectId, code, "Name", "Description", manager, Instant.parse("2026-10-18T00:00:00Z"));
    }

    private AccountContact contact() {
        return new AccountContact(managerId, "manager@example.test", "Manager", Role.SUBJECT_MANAGER, AccountStatus.ACTIVE);
    }

    private void assertCode(Runnable call, String code) {
        assertThatThrownBy(call::run).isInstanceOfSatisfying(ApiException.class,
                error -> assertThat(error.code()).isEqualTo(code));
    }
}
