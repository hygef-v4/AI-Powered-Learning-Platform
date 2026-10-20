package edu.aiplatform.quizzes.web;

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
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.Role;
import edu.aiplatform.quizzes.application.QuizAuthoringService;
import edu.aiplatform.quizzes.application.QuizCopyService;
import edu.aiplatform.quizzes.application.QuizPublicationService;
import edu.aiplatform.quizzes.application.QuizViews.QuizDetail;
import edu.aiplatform.quizzes.application.QuizViews.QuizInfo;
import edu.aiplatform.quizzes.application.RuleViolationException;
import edu.aiplatform.quizzes.contract.QuizScope;
import edu.aiplatform.quizzes.contract.QuizSettings;
import edu.aiplatform.quizzes.contract.QuizStatus;
import edu.aiplatform.quizzes.domain.Difficulty;
import edu.aiplatform.quizzes.domain.QuizOrigin;
import edu.aiplatform.quizzes.domain.Violation;
import edu.aiplatform.shared.error.GlobalExceptionHandler;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        ObjectMapper mapper = Jackson2ObjectMapperBuilder.json()
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS).build();
        mvc = MockMvcBuilders.standaloneSetup(new QuizController(authoring, publication, copies),
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
}
