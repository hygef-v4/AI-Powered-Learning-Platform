package edu.aiplatform.subjects.web;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import edu.aiplatform.audit.contract.IAudit;
import edu.aiplatform.identity.contract.*;
import edu.aiplatform.shared.error.GlobalExceptionHandler;
import edu.aiplatform.subjects.application.*;
import edu.aiplatform.subjects.contract.*;
import edu.aiplatform.subjects.domain.ISubjectRepository;
import edu.aiplatform.subjects.domain.Subject;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** HTTP + service consumer, principal giả đặt phía server; không phải JWT/filter-chain integration. */
class SubjectAdminControllerTest {
    private static final String BASE = "/api/v1/admin/subjects";
    private static final Instant NOW = Instant.parse("2026-10-18T10:00:00Z");
    private final UUID id = UUID.randomUUID(), manager = UUID.randomUUID();
    private final Actor admin = new Actor(UUID.randomUUID(), Role.ADMIN);
    private final ISubjectRepository repository = mock(ISubjectRepository.class);
    private final IAuthorization authorization = mock(IAuthorization.class);
    private final IAccountLookup accounts = mock(IAccountLookup.class);
    private final ISubjectClasses legacyClasses = mock(ISubjectClasses.class);
    private final IAudit audit = mock(IAudit.class);
    private final ISubjectArchiveCheck archiveCheck = mock(ISubjectArchiveCheck.class);
    private final ISubjectManagerAssignmentGate gate = mock(ISubjectManagerAssignmentGate.class);
    @SuppressWarnings("unchecked")
    private final ObjectProvider<ISubjectArchiveCheck> checks = mock(ObjectProvider.class);
    @SuppressWarnings("unchecked")
    private final ObjectProvider<ISubjectManagerAssignmentGate> gates = mock(ObjectProvider.class);
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final ObjectMapper mapper = Jackson2ObjectMapperBuilder.json()
            .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS).build();
    private SubjectClassesReader reader;
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        when(authorization.authorize(any(), anyString(), any())).thenReturn(AuthorizationDecision.allow());
        when(repository.findById(id)).thenReturn(Optional.of(subject()));
        when(repository.findByIdForUpdate(id)).thenReturn(Optional.of(subject()));
        when(repository.findByIdForUpdate(eq(id), any(Duration.class))).thenReturn(Optional.of(subject()));
        when(repository.findPage(null, null, 0, 20)).thenReturn(List.of(subject()));
        when(repository.countByStatus(null)).thenReturn(Map.of(SubjectStatus.ACTIVE, 1L, SubjectStatus.ARCHIVED, 0L));
        when(repository.count(null, null)).thenReturn(1L);
        when(repository.update(any(), anyLong())).thenReturn(true);
        when(legacyClasses.countClasses(any())).thenReturn(Map.of(id, 0));
        when(gates.getIfAvailable()).thenReturn(gate);
        when(checks.getIfAvailable()).thenReturn(archiveCheck);
        when(archiveCheck.checkArchive(eq(id), any())).thenReturn(new ISubjectArchiveCheck.Result(0, List.of()));
        when(accounts.getContact(manager)).thenReturn(Optional.of(new AccountContact(manager, "manager@example.test", "Manager", Role.SUBJECT_MANAGER, AccountStatus.ACTIVE)));
        var recorder = new SubjectAuditRecorder(audit);
        reader = new SubjectClassesReader(legacyClasses);
        var reads = new SubjectAdminService(repository, authorization, accounts, reader, recorder, clock);
        var commands = new SubjectCommandService(repository, authorization, accounts, gates, recorder, clock);
        var statuses = new SubjectStatusService(repository, authorization, checks, recorder, clock);
        mvc = MockMvcBuilders.standaloneSetup(new SubjectAdminController(reads, commands, statuses))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(mapper))
                .setControllerAdvice(new SubjectExceptionHandler(), new GlobalExceptionHandler()).build();
        principal(admin);
    }

    @AfterEach
    void cleanup() {
        reader.close();
        SecurityContextHolder.clearContext();
    }

    @Test
    void listHasWebPageShapeAndZeroCountIsVerified() throws Exception {
        mvc.perform(get(BASE)).andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0)).andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.items[0].id").value(id.toString()))
                .andExpect(jsonPath("$.items[0].classCount").value(0))
                .andExpect(jsonPath("$.statusCounts.ACTIVE").value(1))
                .andExpect(jsonPath("$.statusCounts.ARCHIVED").value(0));
        verify(authorization).authorize(admin, "SUBJECT_LIST", ResourceRef.none());
    }

    @Test
    void filteredPageBindsQueryStatusAndCountsBeforeStatusFilter() throws Exception {
        when(repository.findPage("math", SubjectStatus.ACTIVE, 2, 10)).thenReturn(List.of(subject()));
        when(repository.count("math", SubjectStatus.ACTIVE)).thenReturn(25L);
        when(repository.countByStatus("math")).thenReturn(Map.of(SubjectStatus.ACTIVE, 25L, SubjectStatus.ARCHIVED, 3L));
        mvc.perform(get(BASE).param("q", " math ").param("status", "ACTIVE").param("page", "2").param("size", "10"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.size").value(10)).andExpect(jsonPath("$.totalElements").value(25))
                .andExpect(jsonPath("$.statusCounts.ARCHIVED").value(3));
        verify(repository).findPage("math", SubjectStatus.ACTIVE, 2, 10);
    }

    @Test
    void createReturns201RelativeLocationAndCommittedSnapshot() throws Exception {
        var response = mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\" abc \",\"name\":\" Name \",\"description\":null}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.code").value("ABC"))
                .andExpect(jsonPath("$.version").value(1)).andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.createdAt").value(NOW.toString())).andReturn().getResponse();
        var body = mapper.readTree(response.getContentAsString());
        assertThat(response.getHeader("Location")).isEqualTo(BASE + "/" + body.get("id").textValue());
        assertThat(body.get("manager").isNull()).isTrue();
        assertThat(body.get("classCount").isNull()).isTrue();
        verify(repository, never()).findById(any());
        verifyNoInteractions(legacyClasses);
    }

    @Test
    void detailReturnsDescriptionTimestampsAndVersion() throws Exception {
        mvc.perform(get(BASE + "/" + id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Description"))
                .andExpect(jsonPath("$.createdAt").value(NOW.minusSeconds(60).toString()))
                .andExpect(jsonPath("$.version").value(1));
    }

    @Test
    void classesAvailabilityDistinguishesEmptyFromUnavailable() throws Exception {
        mvc.perform(get(BASE + "/" + id + "/classes")).andExpect(status().isOk())
                .andExpect(jsonPath("$.availability").value("AVAILABLE")).andExpect(jsonPath("$.items").isEmpty());
        when(legacyClasses.listClassesOfSubject(id)).thenThrow(new IllegalStateException("SQL internals"));
        var response = mvc.perform(get(BASE + "/" + id + "/classes")).andExpect(status().isOk())
                .andExpect(jsonPath("$.availability").value("UNAVAILABLE")).andReturn().getResponse();
        assertThat(mapper.readTree(response.getContentAsString()).get("items").isNull()).isTrue();
    }

    @Test
    void patchChangesOnlyMetadataAndReturnsNewVersion() throws Exception {
        mvc.perform(patch(BASE + "/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"New\",\"description\":null,\"version\":1}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("New"))
                .andExpect(jsonPath("$.code").value("ABC")).andExpect(jsonPath("$.version").value(2));
        verify(repository).update(argThat(value -> value.managerId() == null && value.status() == SubjectStatus.ACTIVE), eq(1L));
    }

    @Test
    void managerPutUsesDedicatedCommandAndVersion() throws Exception {
        mvc.perform(put(BASE + "/" + id + "/manager").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"managerId\":\"" + manager + "\",\"version\":1}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.managerId").value(manager.toString()))
                .andExpect(jsonPath("$.version").value(2));
        verify(gate).lockAccounts(List.of(manager));
    }

    @Test
    void explicitNullManagerUnassignsInsteadOfBeingTreatedAsMissing() throws Exception {
        Subject assigned = subject().changeManager(manager, NOW.minusSeconds(1));
        when(repository.findById(id)).thenReturn(Optional.of(assigned));
        when(repository.findByIdForUpdate(id)).thenReturn(Optional.of(assigned));
        var response = mvc.perform(put(BASE + "/" + id + "/manager").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"managerId\":null,\"version\":2}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(3)).andReturn().getResponse();
        assertThat(mapper.readTree(response.getContentAsString()).get("managerId").isNull()).isTrue();
        verify(gate).lockAccounts(List.of(manager));
        verifyNoInteractions(accounts);
    }

    @Test
    void statusPutArchivesWithVerifiedProviderAndReopenDoesNotQueryClasses() throws Exception {
        mvc.perform(put(BASE + "/" + id + "/status").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ARCHIVED\",\"version\":1}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ARCHIVED"))
                .andExpect(jsonPath("$.version").value(2));
        clearInvocations(archiveCheck);
        when(repository.findByIdForUpdate(eq(id), any(Duration.class))).thenReturn(Optional.of(subject().archive(NOW)));
        mvc.perform(put(BASE + "/" + id + "/status").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ACTIVE\",\"version\":2}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.version").value(3));
        verifyNoInteractions(archiveCheck);
    }

    @Test
    void archiveRejectionHasAuthorizedCountAndBlockersExtensions() throws Exception {
        var blocker = new SubjectClassSummary(UUID.randomUUID(), "C1", "Class", "Fall26", "OPEN", "Teacher", 2);
        when(archiveCheck.checkArchive(eq(id), any())).thenReturn(new ISubjectArchiveCheck.Result(1, List.of(blocker)));
        mvc.perform(put(BASE + "/" + id + "/status").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ARCHIVED\",\"version\":1}"))
                .andExpect(status().isConflict()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("SUBJECT_HAS_UNARCHIVED_CLASSES"))
                .andExpect(jsonPath("$.unarchivedCount").value(1))
                .andExpect(jsonPath("$.blockingClasses[0].classId").value(blocker.classId().toString()));
    }

    @Test
    void missingGuardOrArchiveProviderIs503NotFabricatedSuccess() throws Exception {
        when(gates.getIfAvailable()).thenReturn(null);
        mvc.perform(put(BASE + "/" + id + "/manager").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"managerId\":\"" + manager + "\",\"version\":1}"))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value("SUBJECT_ASSIGNMENT_GUARD_UNAVAILABLE"));
        when(checks.getIfAvailable()).thenReturn(null);
        mvc.perform(put(BASE + "/" + id + "/status").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ARCHIVED\",\"version\":1}"))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value("SUBJECT_CLASSES_UNAVAILABLE"));
        verify(repository, never()).update(any(), anyLong());
    }

    static Stream<Arguments> invalidBodies() {
        return Stream.of(
                Arguments.of("PATCH", "", "{\"name\":\"New\",\"description\":null,\"version\":1,\"code\":\"XYZ\"}"),
                Arguments.of("PATCH", "", "{\"name\":\"New\",\"description\":null,\"version\":1,\"status\":\"ARCHIVED\"}"),
                Arguments.of("PATCH", "", "{\"name\":\"New\",\"description\":null,\"version\":1,\"managerId\":null}"),
                Arguments.of("PATCH", "", "{\"name\":\"New\",\"version\":1}"),
                Arguments.of("PATCH", "", "{\"name\":123,\"description\":null,\"version\":1}"),
                Arguments.of("PATCH", "", "{\"name\":\"New\",\"description\":null,\"version\":\"1\"}"),
                Arguments.of("PATCH", "", "{\"name\":\"New\",\"description\":null,\"version\":1.0}"),
                Arguments.of("PATCH", "", "{\"name\":\"New\",\"description\":null,\"version\":9223372036854775808}"),
                Arguments.of("PUT", "/manager", "{\"version\":1}"),
                Arguments.of("PUT", "/manager", "{\"managerId\":\"1-1-1-1-1\",\"version\":1}"),
                Arguments.of("PUT", "/manager", "{\"managerId\":\"\",\"version\":1}"),
                Arguments.of("PUT", "/status", "{\"status\":\"OPEN\",\"version\":1}"),
                Arguments.of("PATCH", "", "[]"),
                Arguments.of("PATCH", "", "{\"name\":\"New\",\"description\":null}"));
    }

    @ParameterizedTest
    @MethodSource("invalidBodies")
    void strictJsonRejectsUnknownMissingAndCoercedFieldsBeforeAnyDataAccess(String method, String suffix, String body) throws Exception {
        var request = method.equals("PATCH") ? patch(BASE + "/" + id + suffix) : put(BASE + "/" + id + suffix);
        mvc.perform(request.contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SUBJECT_REQUEST_INVALID"));
        verifyNoInteractions(repository, gates, archiveCheck, accounts);
    }

    @Test
    void postCannotSupplyActorOrRoleAndNameCannotBeNumber() throws Exception {
        for (String body : List.of("{\"code\":\"ABC\",\"name\":\"Name\",\"role\":\"ADMIN\"}",
                "{\"code\":\"ABC\",\"name\":\"Name\",\"actorId\":\"" + admin.accountId() + "\"}", "{\"code\":\"ABC\",\"name\":123}")) {
            mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        }
        verifyNoInteractions(repository);
    }

    @Test
    void malformedJsonInvalidUuidAndEnumAreSafe400() throws Exception {
        mvc.perform(patch(BASE + "/" + id).contentType(MediaType.APPLICATION_JSON).content("{broken"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("SUBJECT_REQUEST_INVALID"));
        mvc.perform(get(BASE + "/not-a-uuid")).andExpect(status().isBadRequest());
        mvc.perform(get(BASE).param("status", "OPEN")).andExpect(status().isBadRequest());
        mvc.perform(get(BASE).param("page", "abc")).andExpect(status().isBadRequest());
        verifyNoInteractions(repository);
    }

    @Test
    void pagingQueryWhitelistAndSearchBoundAreValidated() throws Exception {
        mvc.perform(get(BASE).param("size", "101")).andExpect(status().isBadRequest());
        mvc.perform(get(BASE).param("page", "-1")).andExpect(status().isBadRequest());
        mvc.perform(get(BASE).param("sort", "name")).andExpect(status().isBadRequest());
        mvc.perform(get(BASE).param("q", "one", "two")).andExpect(status().isBadRequest());
        mvc.perform(get(BASE).param("q", "x".repeat(201))).andExpect(status().isBadRequest());
        verify(repository, never()).findPage(any(), any(), anyInt(), anyInt());
    }

    @Test
    void anonymousAndWrongPrincipalCannotReadData() throws Exception {
        SecurityContextHolder.clearContext();
        mvc.perform(get(BASE)).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("unexpected-principal", "unused", "ROLE_ADMIN"));
        mvc.perform(get(BASE)).andExpect(status().isUnauthorized());
        verifyNoInteractions(repository);
        verify(audit, times(2)).record(any());
    }

    @Test
    void forgedRoleHeaderDoesNotOverrideServerPrincipalOrRevealBlockers() throws Exception {
        principal(new Actor(UUID.randomUUID(), Role.STUDENT));
        mvc.perform(put(BASE + "/" + id + "/status").header("X-Role", "ADMIN").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ARCHIVED\",\"version\":1}"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.blockingClasses").doesNotExist());
        verifyNoInteractions(repository, archiveCheck);
        verify(audit).record(any());
    }

    @Test
    void missingStaleDuplicateAndDatabaseErrorsRemainDistinctAndSafe() throws Exception {
        when(repository.findById(id)).thenReturn(Optional.empty());
        mvc.perform(get(BASE + "/" + id)).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("SUBJECT_NOT_FOUND"));
        when(repository.findById(id)).thenReturn(Optional.of(subject()));
        mvc.perform(patch(BASE + "/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"New\",\"description\":null,\"version\":2}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("SUBJECT_VERSION_CONFLICT"));
        when(repository.existsByCode(any())).thenReturn(true);
        mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content("{\"code\":\"ABC\",\"name\":\"Name\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("SUBJECT_CODE_DUPLICATE"));
        when(repository.findById(id)).thenThrow(new DataAccessResourceFailureException("password=secret select from subjects"));
        var unavailable = mvc.perform(get(BASE + "/" + id)).andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("SUBJECT_STORAGE_UNAVAILABLE")).andReturn().getResponse().getContentAsString();
        assertThat(unavailable).doesNotContain("password", "secret", "select from", "stackTrace");
    }

    @Test
    void integrityErrorIsNotMislabeledDuplicateAndUnknownFailureIsSafe500() throws Exception {
        when(repository.findById(id)).thenThrow(new DataIntegrityViolationException("fk_subjects_manager SQL"));
        mvc.perform(get(BASE + "/" + id)).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("SUBJECT_DATA_CONFLICT"));
        doThrow(new IllegalStateException("internal secret path")).when(repository).findById(id);
        var failure = mvc.perform(get(BASE + "/" + id)).andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("SUBJECT_INTERNAL_ERROR")).andReturn().getResponse().getContentAsString();
        assertThat(failure).doesNotContain("secret", "internal secret path", "stackTrace");
    }

    private Subject subject() { return Subject.create(id, "ABC", "Name", "Description", null, NOW.minusSeconds(60)); }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(value = Role.class, names = "ADMIN", mode = org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE)
    void everyEndpointRejectsNonAdminBeforeReadingOrWriting(Role role) throws Exception {
        principal(new Actor(UUID.randomUUID(), role));
        assertEveryEndpointDenied(403, "SUBJECT_FORBIDDEN");
    }

    @Test
    void everyEndpointRejectsAnonymousConsumer() throws Exception {
        SecurityContextHolder.clearContext();
        assertEveryEndpointDenied(401, "AUTHENTICATION_REQUIRED");
    }

    @Test
    void everyEndpointHonorsCurrentAccountDenialFromU01() throws Exception {
        when(authorization.authorize(any(), anyString(), any())).thenReturn(AuthorizationDecision.deny("ACCOUNT_DISABLED"));
        assertEveryEndpointDenied(403, "SUBJECT_FORBIDDEN");
    }

    @Test
    void everyEndpointFailsClosedWhenU01IsUnavailable() throws Exception {
        when(authorization.authorize(any(), anyString(), any())).thenThrow(new IllegalStateException("session provider unavailable"));
        assertEveryEndpointDenied(503, "SUBJECT_AUTHORIZATION_UNAVAILABLE");
    }

    @Test
    void everyEndpointFailsClosedWhenU01HasNoDecision() throws Exception {
        when(authorization.authorize(any(), anyString(), any())).thenReturn(null);
        assertEveryEndpointDenied(503, "SUBJECT_AUTHORIZATION_UNAVAILABLE");
    }

    private void assertEveryEndpointDenied(int httpStatus, String code) throws Exception {
        var requests = List.of(get(BASE), get(BASE + "/" + id), get(BASE + "/" + id + "/classes"),
                post(BASE).contentType(MediaType.APPLICATION_JSON).content("{\"code\":\"ABC\",\"name\":\"Name\"}"),
                patch(BASE + "/" + id).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"New\",\"description\":null,\"version\":1}"),
                put(BASE + "/" + id + "/manager").contentType(MediaType.APPLICATION_JSON).content("{\"managerId\":null,\"version\":1}"),
                put(BASE + "/" + id + "/status").contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ACTIVE\",\"version\":1}"));
        for (var request : requests) {
            mvc.perform(request).andExpect(status().is(httpStatus)).andExpect(jsonPath("$.code").value(code))
                    .andExpect(jsonPath("$.blockingClasses").doesNotExist());
        }
        verifyNoInteractions(repository, accounts, gate, gates, archiveCheck, checks, legacyClasses);
        verify(audit, times(7)).record(argThat(event -> event.result() == edu.aiplatform.audit.contract.AuditResult.DENIED));
    }

    private void principal(Actor actor) {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(actor, "unused", "ROLE_" + actor.role()));
    }
}
