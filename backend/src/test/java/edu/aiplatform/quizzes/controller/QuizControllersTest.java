package edu.aiplatform.quizzes.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import edu.aiplatform.ai.contract.AiProvider;
import edu.aiplatform.ai.contract.AiSource;
import edu.aiplatform.ai.contract.SelectableMaterial;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.Role;
import edu.aiplatform.quizzes.service.QuizAiDraftService;
import edu.aiplatform.quizzes.service.QuizAiDraftService.DraftRequest;
import edu.aiplatform.quizzes.service.QuizAuthoringService;
import edu.aiplatform.quizzes.service.QuizCopyService;
import edu.aiplatform.quizzes.service.QuizPublicationService;
import edu.aiplatform.quizzes.service.QuizReferenceMaterials;
import edu.aiplatform.quizzes.service.QuizViews.QuizDetail;
import edu.aiplatform.quizzes.service.QuizViews.QuizInfo;
import edu.aiplatform.quizzes.service.RuleViolationException;
import edu.aiplatform.quizzes.contract.QuizScope;
import edu.aiplatform.quizzes.contract.QuizSettings;
import edu.aiplatform.quizzes.contract.QuizStatus;
import edu.aiplatform.quizzes.model.Difficulty;
import edu.aiplatform.quizzes.model.QuestionContent;
import edu.aiplatform.quizzes.model.QuizOrigin;
import edu.aiplatform.quizzes.model.Violation;
import edu.aiplatform.shared.error.GlobalExceptionHandler;
import edu.aiplatform.shared.error.ApiException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** HTTP của Quiz (UC 48); principal giả đặt phía server, service là mock (kiểm riêng ở application). */
class QuizControllersTest {

    private static final Instant NOW = Instant.parse("2026-10-20T08:00:00Z");
    private static final String SETTINGS = """
            {"shuffleQuestions":true,"shuffleOptions":false,"timeLimitMinutes":30,"maxAttempts":null,
             "showScoreAfterSubmit":true,"showCorrectAnswers":false}""";
    private final UUID quizId = UUID.randomUUID();
    private final UUID classId = UUID.randomUUID();
    private final UUID subjectId = UUID.randomUUID();
    private final Actor teacher = new Actor(UUID.randomUUID(), Role.TEACHER);
    private final QuizAuthoringService authoring = mock(QuizAuthoringService.class);
    private final QuizPublicationService publication = mock(QuizPublicationService.class);
    private final QuizCopyService copies = mock(QuizCopyService.class);
    private final QuizAiDraftService drafts = mock(QuizAiDraftService.class);
    private final QuizReferenceMaterials materials = mock(QuizReferenceMaterials.class);
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        ObjectMapper mapper = Jackson2ObjectMapperBuilder.json()
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS).build();
        mvc = MockMvcBuilders.standaloneSetup(new QuizController(authoring, publication, copies, drafts, materials),
                        new ClassQuizController(authoring, copies), new SubjectQuizController(authoring))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(mapper))
                .setControllerAdvice(new QuizzesExceptionHandler(), new GlobalExceptionHandler()).build();
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(teacher, null));
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private QuizDetail detail() {
        QuizInfo info = new QuizInfo(quizId, QuizScope.CLASS, classId, subjectId, null, QuizOrigin.MANUAL, 1, "Luyện tập",
                null, new QuizSettings(false, false, null, 1, true, false), false, null, List.of(), QuizStatus.DRAFT,
                null, null, null, NOW, NOW, 0);
        return new QuizDetail(info, List.of(), BigDecimal.ZERO, true);
    }

    @Test
    void createClassQuizReturns201() throws Exception {
        when(authoring.createForClass(teacher, classId, "Luyện tập")).thenReturn(detail());
        mvc.perform(post("/api/v1/classes/{c}/quizzes", classId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Luyện tập\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/quizzes/" + quizId))
                .andExpect(jsonPath("$.quiz.status").value("DRAFT"));
        mvc.perform(post("/api/v1/subjects/{s}/quizzes", subjectId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"x\",\"classId\":\"y\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("QUIZ_REQUEST_INVALID"));
    }

    @Test
    void detailsParseSettingsStrictly() throws Exception {
        when(authoring.updateDetails(eq(teacher), eq(quizId), any(), eq(4L))).thenReturn(detail());
        mvc.perform(patch("/api/v1/quizzes/{id}", quizId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Bài\",\"instructions\":null,\"settings\":" + SETTINGS
                                + ",\"activityMonitoringEnabled\":false,\"activityNotice\":null,"
                                + "\"referenceMaterialIds\":[],\"version\":4}"))
                .andExpect(status().isOk());
        ArgumentCaptor<QuizAuthoringService.Details> details = ArgumentCaptor.forClass(QuizAuthoringService.Details.class);
        verify(authoring).updateDetails(eq(teacher), eq(quizId), details.capture(), eq(4L));
        assertThat(details.getValue().settings()).isEqualTo(new QuizSettings(true, false, 30, null, true, false));

        mvc.perform(patch("/api/v1/quizzes/{id}", quizId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Bài\",\"settings\":" + SETTINGS.replace("30", "\"30\"")
                                + ",\"activityMonitoringEnabled\":false,\"version\":4}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void itemsAndReviewErrorsCarryViolations() throws Exception {
        UUID question = UUID.randomUUID();
        when(authoring.saveItems(eq(teacher), eq(quizId), any(), eq(1L))).thenReturn(detail());
        mvc.perform(put("/api/v1/quizzes/{id}/items", quizId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"questionId\":\"" + question + "\",\"points\":2.5},"
                                + "{\"questionId\":\"" + question + "\",\"points\":null}],\"version\":1}"))
                .andExpect(status().isOk());
        ArgumentCaptor<List<QuizAuthoringService.ItemInput>> items = ArgumentCaptor.forClass(List.class);
        verify(authoring).saveItems(eq(teacher), eq(quizId), items.capture(), eq(1L));
        assertThat(items.getValue()).extracting(QuizAuthoringService.ItemInput::points)
                .containsExactly(new BigDecimal("2.5"), null);

        when(publication.review(teacher, quizId, 2)).thenThrow(new RuleViolationException(HttpStatus.BAD_REQUEST,
                "QUIZ_REVIEW_FAILED", "x", List.of(new Violation("items[1]", "QUESTION_NOT_ACTIVE", "Câu 1"))));
        mvc.perform(post("/api/v1/quizzes/{id}/review", quizId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":2}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("QUIZ_REVIEW_FAILED"))
                .andExpect(jsonPath("$.violations[0].field").value("items[1]"));
    }

    @Test
    void lifecycleAndCopyEndpointsMapToServices() throws Exception {
        when(publication.publish(teacher, quizId, 3)).thenReturn(detail());
        mvc.perform(post("/api/v1/quizzes/{id}/publish", quizId).contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\":3}")).andExpect(status().isOk());
        when(publication.retire(teacher, quizId, "Đổi đề", 3)).thenReturn(detail());
        mvc.perform(post("/api/v1/quizzes/{id}/retire", quizId).contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"Đổi đề\",\"version\":3}")).andExpect(status().isOk());
        verify(publication).retire(teacher, quizId, "Đổi đề", 3);
        when(publication.reopen(teacher, quizId, 4)).thenReturn(detail());
        mvc.perform(post("/api/v1/quizzes/{id}/reopen", quizId).contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\":4}")).andExpect(status().isOk());
        verify(publication).reopen(teacher, quizId, 4);
        when(copies.newVersion(teacher, quizId)).thenReturn(detail());
        mvc.perform(post("/api/v1/quizzes/{id}/versions", quizId)).andExpect(status().isCreated());
        UUID source = UUID.randomUUID();
        when(copies.copyToClass(teacher, classId, source)).thenReturn(detail());
        mvc.perform(post("/api/v1/classes/{c}/quizzes:copy", classId).contentType(MediaType.APPLICATION_JSON)
                .content("{\"sourceQuizId\":\"" + source + "\"}")).andExpect(status().isCreated());
        mvc.perform(delete("/api/v1/quizzes/{id}", quizId).param("version", "5")).andExpect(status().isNoContent());
        verify(authoring).delete(teacher, quizId, 5);
    }

    @Test
    void retirementAllowsOmittedReasonAndPreservesServerPrincipalAndVersion() throws Exception {
        when(publication.retire(teacher, quizId, null, 6)).thenReturn(detail());
        mvc.perform(post("/api/v1/quizzes/{id}/retire", quizId).contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\":6}"))
                .andExpect(status().isOk());
        verify(publication).retire(teacher, quizId, null, 6);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"version\":-1}", "{\"version\":\"4\"}",
            "{\"version\":4,\"actor\":\"forged\"}"})
    void reopenRejectsMalformedVersionAndExtraFieldsBeforeService(String body) throws Exception {
        mvc.perform(post("/api/v1/quizzes/{id}/reopen", quizId).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        verify(publication, never()).reopen(any(), any(), anyLong());
    }

    @Test
    void reopenPreservesServiceConflictAndScopeDenial() throws Exception {
        when(publication.reopen(teacher, quizId, 4)).thenThrow(ApiException.conflict("QUIZ_VERSION_CONFLICT", "Tải lại"));
        mvc.perform(post("/api/v1/quizzes/{id}/reopen", quizId).contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\":4}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("QUIZ_VERSION_CONFLICT"));
        when(publication.reopen(teacher, quizId, 5)).thenThrow(ApiException.notFound("QUIZ_NOT_FOUND"));
        mvc.perform(post("/api/v1/quizzes/{id}/reopen", quizId).contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\":5}"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("QUIZ_NOT_FOUND"));
    }

    @Test
    void readEndpointsValidateQueryParameters() throws Exception {
        when(authoring.get(teacher, quizId, classId)).thenReturn(detail());
        mvc.perform(get("/api/v1/quizzes/{id}", quizId).param("classId", classId.toString())).andExpect(status().isOk());
        mvc.perform(get("/api/v1/quizzes/{id}", quizId).param("other", "x")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/quizzes/{id}/bank-questions", quizId).param("difficulty", "VERY_HARD"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/quizzes/{id}/items:random-preview", quizId).contentType(MediaType.APPLICATION_JSON)
                .content("{\"count\":5,\"difficulty\":\"EASY\",\"tag\":null}")).andExpect(status().isOk());
        verify(authoring).randomPreview(eq(teacher), eq(quizId), eq(5), eq(Difficulty.EASY),
                isNull());
        verify(authoring, never()).bankQuestions(any(), any(), any(), any(), any(), any(), any());
        verify(authoring, never()).delete(any(), any(), anyLong());
    }

    @Test
    void aiDraftRequestParsesSourceMaterialsAndVersion() throws Exception {
        UUID material = UUID.randomUUID();
        mvc.perform(post("/api/v1/quizzes/{id}/ai-drafts", quizId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"instructions\":\"Chương 1\",\"count\":5,\"type\":\"MCQ_MULTI\",\"difficulty\":null,"
                                + "\"materialIds\":[\"" + material + "\"],\"source\":\"PERSONAL\",\"provider\":\"CLAUDE\","
                                + "\"requestRef\":\"r-1\",\"version\":3}"))
                .andExpect(status().isCreated());
        ArgumentCaptor<DraftRequest> request = ArgumentCaptor.forClass(DraftRequest.class);
        verify(drafts).requestForQuiz(eq(teacher), eq(quizId), request.capture(), eq(3L));
        assertThat(request.getValue().count()).isEqualTo(5);
        assertThat(request.getValue().source()).isEqualTo(AiSource.PERSONAL);
        assertThat(request.getValue().provider()).isEqualTo(AiProvider.CLAUDE);
        assertThat(request.getValue().materialIds()).containsExactly(material);

        mvc.perform(post("/api/v1/quizzes/{id}/ai-drafts", quizId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"instructions\":\"x\",\"count\":5,\"source\":\"SHARED\",\"requestRef\":\"r\","
                                + "\"version\":3,\"studentId\":\"y\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("QUESTION_REQUEST_INVALID"));
    }

    @Test
    void colonActionsRouteToAcceptAndDiscard() throws Exception {
        UUID draft = UUID.randomUUID();
        when(drafts.acceptForQuiz(eq(teacher), eq(quizId), eq(draft), any(), eq(7L))).thenReturn(detail());
        mvc.perform(post("/api/v1/quizzes/{id}/ai-drafts/{d}:accept", quizId, draft)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"questions\":[{\"type\":\"MCQ_SINGLE\",\"stem\":\"Câu\",\"options\":[{\"id\":\"1\","
                                + "\"text\":\"A\"},{\"id\":\"2\",\"text\":\"B\"}],\"correctOptionIds\":[\"1\"],"
                                + "\"explanation\":null,\"defaultPoints\":1,\"difficulty\":\"EASY\",\"tags\":[]}],\"version\":7}"))
                .andExpect(status().isOk());
        ArgumentCaptor<List<QuestionContent>> picked = ArgumentCaptor.forClass(List.class);
        verify(drafts).acceptForQuiz(eq(teacher), eq(quizId), eq(draft), picked.capture(), eq(7L));
        assertThat(picked.getValue()).singleElement().satisfies(c -> assertThat(c.stem()).isEqualTo("Câu"));

        mvc.perform(post("/api/v1/quizzes/{id}/ai-drafts/{d}:discard", quizId, draft)).andExpect(status().isOk());
        verify(drafts).discardForQuiz(teacher, quizId, draft);
        mvc.perform(get("/api/v1/quizzes/{id}/ai-drafts/{d}", quizId, draft)).andExpect(status().isOk());
        verify(drafts).getForQuiz(teacher, quizId, draft);
    }

    @Test
    void referenceMaterialsListedForQuiz() throws Exception {
        UUID material = UUID.randomUUID();
        when(materials.forQuiz(teacher, quizId)).thenReturn(List.of(new SelectableMaterial(material, "Chương 1", true)));
        mvc.perform(get("/api/v1/quizzes/{id}/reference-materials", quizId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].materialId").value(material.toString()))
                .andExpect(jsonPath("$[0].hasText").value(true));
    }
}
