package edu.aiplatform.quizpractice.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.Role;
import edu.aiplatform.quizpractice.application.PracticeViews.SaveResult;
import edu.aiplatform.quizpractice.application.PracticeViews.TakingView;
import edu.aiplatform.quizpractice.application.QuizActivityService;
import edu.aiplatform.quizpractice.application.QuizAttemptService;
import edu.aiplatform.quizpractice.application.QuizPracticeQueryService;
import edu.aiplatform.quizpractice.application.TeacherQuizAttemptService;
import edu.aiplatform.shared.error.ApiException;
import edu.aiplatform.shared.error.GlobalExceptionHandler;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** HTTP của Quiz Practice; principal giả đặt phía server, service là mock (kiểm riêng ở application). */
class QuizPracticeControllerTest {

    private static final Instant NOW = Instant.parse("2026-10-20T08:00:00Z");
    private final UUID quizId = UUID.randomUUID();
    private final UUID classId = UUID.randomUUID();
    private final UUID attemptId = UUID.randomUUID();
    private final Actor student = new Actor(UUID.randomUUID(), Role.STUDENT);
    private final QuizPracticeQueryService queries = mock(QuizPracticeQueryService.class);
    private final QuizAttemptService attempts = mock(QuizAttemptService.class);
    private final QuizActivityService activity = mock(QuizActivityService.class);
    private final TeacherQuizAttemptService teacher = mock(TeacherQuizAttemptService.class);
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        ObjectMapper mapper = Jackson2ObjectMapperBuilder.json()
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS).build();
        mvc = MockMvcBuilders.standaloneSetup(new QuizPracticeController(queries, attempts, activity),
                        new TeacherQuizAttemptController(teacher))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(mapper))
                .setControllerAdvice(new QuizPracticeExceptionHandler(), new GlobalExceptionHandler()).build();
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(student, null));
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void startReadsClassIdFromBody() throws Exception {
        when(attempts.start(student, quizId, classId)).thenReturn(new TakingView(attemptId, quizId, classId, "Quiz", 1,
                NOW, null, NOW, 0, List.of(), Map.of()));
        mvc.perform(post("/api/v1/quizzes/{q}/attempts", quizId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"classId\":\"" + classId + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.attemptId").value(attemptId.toString()));
        mvc.perform(post("/api/v1/quizzes/{q}/attempts", quizId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"classId\":\"x\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("QUIZ_PRACTICE_REQUEST_INVALID"));
    }

    @Test
    void answersMapQuestionIdsToOptionListsWithVersion() throws Exception {
        UUID question = UUID.randomUUID();
        when(attempts.saveAnswers(eq(student), eq(attemptId), any(), eq(3L))).thenReturn(new SaveResult(4, NOW));
        mvc.perform(put("/api/v1/quiz-attempts/{a}/answers", attemptId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"answers\":{\"" + question + "\":[\"1\",\"3\"]},\"answerVersion\":3}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.answerVersion").value(4));
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<UUID, List<String>>> captor = ArgumentCaptor.forClass(Map.class);
        verify(attempts).saveAnswers(eq(student), eq(attemptId), captor.capture(), eq(3L));
        assertThat(captor.getValue()).containsEntry(question, List.of("1", "3"));

        mvc.perform(put("/api/v1/quiz-attempts/{a}/answers", attemptId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"answers\":{\"not-a-uuid\":[\"1\"]},\"answerVersion\":3}"))
                .andExpect(status().isBadRequest());
        when(attempts.saveAnswers(eq(student), eq(attemptId), any(), eq(1L)))
                .thenThrow(ApiException.conflict("ANSWER_VERSION_CONFLICT", "x"));
        mvc.perform(put("/api/v1/quiz-attempts/{a}/answers", attemptId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"answers\":{},\"answerVersion\":1}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("ANSWER_VERSION_CONFLICT"));
    }

    @Test
    void readEndpointsAndActivityValidateInput() throws Exception {
        mvc.perform(get("/api/v1/me/quizzes/{q}", quizId)).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/me/quizzes").param("other", "1")).andExpect(status().isBadRequest());
        UUID material = UUID.randomUUID();
        mvc.perform(get("/api/v1/me/quizzes").param("materialId", material.toString()))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/me/quizzes").param("materialId", material.toString())
                .param("classId", quizId.toString())).andExpect(status().isOk());
        verify(queries).byMaterial(student, quizId, material);
        mvc.perform(post("/api/v1/quiz-attempts/{a}/activity", attemptId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"events\":[{\"sessionId\":\"" + UUID.randomUUID() + "\",\"clientEventId\":\"e1\","
                                + "\"sequence\":1,\"kind\":\"HIDDEN\",\"clientTime\":\"2026-10-20T08:00:00Z\"}]}"))
                .andExpect(status().isNoContent());
        verify(activity).record(eq(student), eq(attemptId), anyList());
        mvc.perform(post("/api/v1/quiz-attempts/{a}/activity", attemptId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"events\":[{\"kind\":\"BOGUS\"}]}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/classes/{c}/quizzes/{q}/attempts", classId, quizId)).andExpect(status().isOk());
        verify(teacher).list(student, classId, quizId);
        verify(queries, never()).detail(any(), any(), any());
    }
}
