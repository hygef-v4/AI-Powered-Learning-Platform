package edu.aiplatform.academics.web;

import edu.aiplatform.academics.application.AcademicsDtos.*;
import edu.aiplatform.academics.application.AcademicsService;
import edu.aiplatform.academics.contract.ClassStatus;
import edu.aiplatform.academics.domain.EnrollmentResult;
import edu.aiplatform.academics.domain.EnrollmentStatus;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.Role;
import edu.aiplatform.shared.error.ApiException;
import edu.aiplatform.subjects.contract.SubjectStatus;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Controller mapping/serialization tests only: service authorization and session filters are not exercised. */
class AcademicsApiContractTest {
    private final AcademicsService service = mock(AcademicsService.class);
    private final UUID classId = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private final UUID subjectId = UUID.fromString("20000000-0000-0000-0000-000000000002");
    private final UUID accountId = UUID.fromString("30000000-0000-0000-0000-000000000003");
    private final Actor serverActor = new Actor(UUID.fromString("40000000-0000-0000-0000-000000000004"), Role.SUBJECT_MANAGER);
    private final Person teacher = new Person(accountId, "Teacher", "teacher@example.test");
    private final Instant timestamp = Instant.parse("2026-10-18T10:00:00Z");
    private MockMvc mvc;

    @BeforeEach void setup() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(serverActor, null));
        mvc = MockMvcBuilders.standaloneSetup(new AcademicsController(service))
                .setControllerAdvice(new AcademicsExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver()).build();
    }

    @AfterEach void clearPrincipal() { SecurityContextHolder.clearContext(); }

    private ClassView course() {
        return new ClassView(classId, new Subject(subjectId, "CS101", "Computing"), "C01", "Morning class",
                "Description", "2026-1", ClassStatus.OPEN, teacher, 2, 1, timestamp, true, 7);
    }

    private Student student() {
        return new Student(accountId, "Student", "student@example.test", timestamp, EnrollmentStatus.ACTIVE);
    }

    @ParameterizedTest
    @CsvSource({"me,student", "teaching,teacher", "manager,manager"})
    void listForwardsFiltersAndPaginationButNeverRequestActor(String route, String audience) throws Exception {
        UUID requestedSubject = route.equals("manager") ? subjectId : null;
        when(service.list(serverActor, audience, requestedSubject, "morning", ClassStatus.OPEN, 3, 40))
                .thenReturn(List.of(course()));
        var request = get("/api/v1/" + route + "/classes").param("q", "morning").param("status", "OPEN")
                .param("page", "3").param("size", "40").param("actor", "forged")
                .param("userId", accountId.toString()).param("role", "ADMIN");
        if (requestedSubject != null) request.param("subjectId", requestedSubject.toString());
        mvc.perform(request).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(classId.toString()))
                .andExpect(jsonPath("$[0].subject.code").value("CS101"))
                .andExpect(jsonPath("$[0].teacher.email").value("teacher@example.test"))
                .andExpect(jsonPath("$[0].activeStudentCount").value(2))
                .andExpect(jsonPath("$[0].showGradeDistribution").value(true));
        verify(service).list(serverActor, audience, requestedSubject, "morning", ClassStatus.OPEN, 3, 40);
        verifyNoMoreInteractions(service);
    }

    @ParameterizedTest
    @CsvSource({"me,student", "teaching,teacher", "manager,manager"})
    void detailPreservesAudienceClassIdAndOptimisticVersion(String route, String audience) throws Exception {
        when(service.detail(serverActor, audience, classId)).thenReturn(course());
        mvc.perform(get("/api/v1/" + route + "/classes/" + classId))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(classId.toString()))
                .andExpect(jsonPath("$.version").value(7)).andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.removedStudentCount").value(1));
        verify(service).detail(serverActor, audience, classId);
        verifyNoMoreInteractions(service);
    }

    @Test void subjectSummariesExposeStatusCountsAndDetail() throws Exception {
        var summary = new SubjectSummary(subjectId, "CS101", "Computing", SubjectStatus.ACTIVE,
                Map.of(ClassStatus.DRAFT, 1L, ClassStatus.OPEN, 2L, ClassStatus.ARCHIVED, 3L));
        when(service.managedSubjects(serverActor)).thenReturn(List.of(summary));
        when(service.managedSubject(serverActor, subjectId)).thenReturn(summary);
        mvc.perform(get("/api/v1/manager/subjects")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$[0].classCounts.OPEN").value(2));
        mvc.perform(get("/api/v1/manager/subjects/" + subjectId)).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(subjectId.toString()))
                .andExpect(jsonPath("$.classCounts.ARCHIVED").value(3));
        verify(service).managedSubjects(serverActor);
        verify(service).managedSubject(serverActor, subjectId);
        verifyNoMoreInteractions(service);
    }

    @ParameterizedTest
    @CsvSource({"teaching,teacher", "manager,manager"})
    void studentListForwardsClassAndPageWithoutInventingAnActor(String route, String audience) throws Exception {
        when(service.students(serverActor, audience, classId, 2, 25)).thenReturn(List.of(student()));
        mvc.perform(get("/api/v1/" + route + "/classes/" + classId + "/students").param("page", "2").param("size", "25"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].accountId").value(accountId.toString()))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));
        verify(service).students(serverActor, audience, classId, 2, 25);
        verifyNoMoreInteractions(service);
    }

    @Test void teacherStudentDetailForwardsBothResourceIds() throws Exception {
        when(service.student(serverActor, classId, accountId)).thenReturn(student());
        mvc.perform(get("/api/v1/teaching/classes/" + classId + "/students/" + accountId))
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").value("student@example.test"));
        verify(service).student(serverActor, classId, accountId);
        verifyNoMoreInteractions(service);
    }

    @Test void candidateSearchKeepsClassScopeAndQuery() throws Exception {
        when(service.teacherCandidates(serverActor, "teacher & manager")).thenReturn(List.of(teacher));
        when(service.studentCandidates(serverActor, classId, "student")).thenReturn(List.of(teacher));
        mvc.perform(get("/api/v1/manager/teacher-candidates").param("q", "teacher & manager"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(accountId.toString()));
        mvc.perform(get("/api/v1/manager/classes/" + classId + "/student-candidates").param("q", "student"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].name").value("Teacher"));
        verify(service).teacherCandidates(serverActor, "teacher & manager");
        verify(service).studentCandidates(serverActor, classId, "student");
        verifyNoMoreInteractions(service);
    }

    @Test void createReturns201AndMapsOptionalTeacher() throws Exception {
        when(service.create(serverActor, subjectId, "C01", "Morning class", "Description", "2026-1", accountId)).thenReturn(course());
        String body = "{\"subjectId\":\"" + subjectId + "\",\"code\":\"C01\",\"name\":\"Morning class\",\"description\":\"Description\",\"term\":\"2026-1\",\"teacherId\":\"" + accountId + "\"}";
        mvc.perform(post("/api/v1/manager/classes").contentType("application/json").content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.version").value(7));
        verify(service).create(serverActor, subjectId, "C01", "Morning class", "Description", "2026-1", accountId);
        verifyNoMoreInteractions(service);
    }

    @Test void classMutationsForwardVersionAndTypedValues() throws Exception {
        when(service.updateInfo(serverActor, classId, "Renamed", "Updated", "2027-1", 6)).thenReturn(course());
        when(service.assignTeacher(serverActor, classId, null, 7)).thenReturn(course());
        when(service.gradeDistribution(serverActor, classId, false, 8)).thenReturn(course());
        when(service.changeStatus(serverActor, classId, ClassStatus.ARCHIVED, 9)).thenReturn(course());
        mvc.perform(patch("/api/v1/manager/classes/" + classId).contentType("application/json")
                .content("{\"name\":\"Renamed\",\"description\":\"Updated\",\"term\":\"2027-1\",\"version\":6}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(classId.toString()));
        mvc.perform(put("/api/v1/manager/classes/" + classId + "/teacher").contentType("application/json")
                .content("{\"teacherId\":null,\"version\":7}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.teacher.id").value(accountId.toString()));
        mvc.perform(put("/api/v1/manager/classes/" + classId + "/grade-distribution").contentType("application/json")
                .content("{\"enabled\":false,\"version\":8}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(7));
        mvc.perform(put("/api/v1/manager/classes/" + classId + "/status").contentType("application/json")
                .content("{\"status\":\"ARCHIVED\",\"version\":9}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("C01"));
        verify(service).updateInfo(serverActor, classId, "Renamed", "Updated", "2027-1", 6);
        verify(service).assignTeacher(serverActor, classId, null, 7);
        verify(service).gradeDistribution(serverActor, classId, false, 8);
        verify(service).changeStatus(serverActor, classId, ClassStatus.ARCHIVED, 9);
        verifyNoMoreInteractions(service);
    }

    @Test void enrollmentOutcomeAndBatchRowsRemainExplicitJsonContracts() throws Exception {
        when(service.enroll(serverActor, classId, accountId)).thenReturn(EnrollmentResult.IN_OTHER_CLASS);
        String text = "student@example.test\n\ninvalid";
        when(service.enrollBatch(serverActor, classId, text)).thenReturn(List.of(
                new RowResult(1, "student@example.test", EnrollmentResult.ENROLLED),
                new RowResult(3, "invalid", EnrollmentResult.INVALID_EMAIL)));
        mvc.perform(post("/api/v1/manager/classes/" + classId + "/enrollments").contentType("application/json")
                .content("{\"accountId\":\"" + accountId + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.result").value("IN_OTHER_CLASS"));
        mvc.perform(post("/api/v1/manager/classes/" + classId + "/enrollments:batch").contentType("application/json")
                .content("{\"text\":\"student@example.test\\n\\ninvalid\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].result").value("ENROLLED"))
                .andExpect(jsonPath("$[1].line").value(3)).andExpect(jsonPath("$[1].email").value("invalid"))
                .andExpect(jsonPath("$[1].result").value("INVALID_EMAIL"));
        verify(service).enroll(serverActor, classId, accountId);
        verify(service).enrollBatch(serverActor, classId, text);
        verifyNoMoreInteractions(service);
    }

    @ParameterizedTest @ValueSource(strings = {"ACTIVE", "REMOVED"})
    void removalAndRestorationReturn204WithoutLeakingServiceResult(String value) throws Exception {
        var enrollmentStatus = EnrollmentStatus.valueOf(value);
        when(service.enrollmentStatus(serverActor, classId, accountId, enrollmentStatus)).thenReturn(EnrollmentResult.RESTORED);
        mvc.perform(put("/api/v1/manager/classes/" + classId + "/enrollments/" + accountId + "/status")
                .contentType("application/json").content("{\"status\":\"" + value + "\"}"))
                .andExpect(status().isNoContent()).andExpect(content().string(""));
        verify(service).enrollmentStatus(serverActor, classId, accountId, enrollmentStatus);
        verifyNoMoreInteractions(service);
    }

    static Stream<Arguments> malformedMutations() {
        return Stream.of(
                Arguments.of("POST", "", "{\"code\":\"C\",\"name\":\"Name\"}"),
                Arguments.of("PATCH", "/class", "{\"name\":\"Name\",\"version\":-1}"),
                Arguments.of("PATCH", "/class", "{\"name\":\"" + "x".repeat(201) + "\",\"version\":0}"),
                Arguments.of("PUT", "/class/teacher", "{\"teacherId\":\"internal-secret\",\"version\":0}"),
                Arguments.of("PUT", "/class/teacher", "{\"version\":0,\"actor\":\"forged\"}"),
                Arguments.of("PUT", "/class/grade-distribution", "{\"version\":0}"),
                Arguments.of("PUT", "/class/status", "{\"status\":\"internal-secret\",\"version\":0}"),
                Arguments.of("POST", "/class/enrollments", "{}"),
                Arguments.of("POST", "/class/enrollments:batch", "{\"text\":null}"),
                Arguments.of("PUT", "/class/enrollments/account/status", "{\"status\":\"ACTIVE\",\"userId\":\"forged\",\"role\":\"ADMIN\"}"));
    }

    @ParameterizedTest @MethodSource("malformedMutations")
    void invalidMutationsFailSafelyBeforeCallingService(String method, String suffix, String body) throws Exception {
        String path = "/api/v1/manager/classes" + suffix.replace("/class", "/" + classId).replace("/account", "/" + accountId);
        MockHttpServletRequestBuilder request = switch (method) {
            case "POST" -> post(path);
            case "PATCH" -> patch(path);
            default -> put(path);
        };
        mvc.perform(request.contentType("application/json").content(body)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(content().string(not(containsString("internal-secret"))))
                .andExpect(content().string(not(containsString("Exception"))));
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/manager/classes/not-a-uuid", "/me/classes?status=internal-secret", "/teaching/classes?page=internal-secret"})
    void invalidPathAndQueryConversionsDoNotReachService(String path) throws Exception {
        mvc.perform(get("/api/v1" + path)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(content().string(not(containsString("internal-secret"))));
        verifyNoInteractions(service);
    }

    @ParameterizedTest @ValueSource(ints = {401, 403, 404, 409, 413, 503})
    void declaredApiErrorsPreserveStatusAndCodeWithoutSerializingInternalCause(int httpStatus) throws Exception {
        if (httpStatus == 401) SecurityContextHolder.clearContext();
        Actor principal = httpStatus == 401 ? null : serverActor;
        var error = new ApiException(HttpStatus.valueOf(httpStatus), "EXPECTED_PROBLEM", "Safe client message");
        error.initCause(new IllegalStateException("SELECT password FROM internal_accounts; internal-secret"));
        when(service.detail(principal, "manager", classId)).thenThrow(error);
        mvc.perform(get("/api/v1/manager/classes/" + classId)).andExpect(status().is(httpStatus))
                .andExpect(jsonPath("$.status").value(httpStatus))
                .andExpect(jsonPath("$.code").value("EXPECTED_PROBLEM"))
                .andExpect(jsonPath("$.detail").value("Safe client message"))
                .andExpect(content().string(not(containsString("internal-secret"))))
                .andExpect(content().string(not(containsString("internal_accounts"))));
        verify(service).detail(principal, "manager", classId);
        verifyNoMoreInteractions(service);
    }
}
