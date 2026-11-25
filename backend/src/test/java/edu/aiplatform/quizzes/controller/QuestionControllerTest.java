package edu.aiplatform.quizzes.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.Role;
import edu.aiplatform.quizzes.service.QuestionBankService;
import edu.aiplatform.quizzes.service.QuestionImporter;
import edu.aiplatform.quizzes.service.QuizAiDraftService;
import edu.aiplatform.quizzes.service.QuizReferenceMaterials;
import edu.aiplatform.quizzes.service.QuestionInvalidException;
import edu.aiplatform.quizzes.service.QuestionViews.QuestionDetail;
import edu.aiplatform.quizzes.service.QuestionViews.QuestionPage;
import edu.aiplatform.quizzes.service.QuestionViews.QuestionView;
import edu.aiplatform.quizzes.contract.QuestionType;
import edu.aiplatform.quizzes.model.Difficulty;
import edu.aiplatform.quizzes.model.QuestionContent;
import edu.aiplatform.quizzes.model.QuestionOption;
import edu.aiplatform.quizzes.model.QuestionStatus;
import edu.aiplatform.quizzes.model.Violation;
import edu.aiplatform.shared.error.ApiException;
import edu.aiplatform.shared.error.GlobalExceptionHandler;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** HTTP của ngân hàng câu hỏi; principal giả đặt phía server, service là mock (kiểm riêng ở application). */
class QuestionControllerTest {

    private static final Instant NOW = Instant.parse("2026-10-20T08:00:00Z");
    private static final String BODY = """
            {"type":"MCQ_SINGLE","stem":"Thủ đô?","options":[{"id":"1","text":"Hà Nội"},{"id":"2","text":"Huế"}],
             "correctOptionIds":["1"],"explanation":null,"defaultPoints":1.5,"difficulty":"EASY","tags":["địa lý"]}""";
    private final UUID subject = UUID.randomUUID();
    private final UUID questionId = UUID.randomUUID();
    private final Actor manager = new Actor(UUID.randomUUID(), Role.SUBJECT_MANAGER);
    private final QuestionBankService bank = mock(QuestionBankService.class);
    private final QuestionImporter importer = mock(QuestionImporter.class);
    private final QuizAiDraftService drafts = mock(QuizAiDraftService.class);
    private final QuizReferenceMaterials materials = mock(QuizReferenceMaterials.class);
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        ObjectMapper mapper = Jackson2ObjectMapperBuilder.json()
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS).build();
        mvc = MockMvcBuilders.standaloneSetup(new QuestionController(bank, importer, drafts, materials))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(mapper))
                .setControllerAdvice(new QuizzesExceptionHandler(), new GlobalExceptionHandler()).build();
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(manager, null));
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private QuestionDetail detail() {
        var view = new QuestionView(questionId, questionId, 1, subject, QuestionType.MCQ_SINGLE, "Thủ đô?",
                List.of(new QuestionOption("1", "Hà Nội"), new QuestionOption("2", "Huế")), List.of("1"), null,
                BigDecimal.ONE, Difficulty.EASY, List.of(), QuestionStatus.DRAFT, null, manager.accountId(), NOW, NOW, 0);
        return new QuestionDetail(view, List.of(), 0, null);
    }

    @Test
    void notFoundAndConflictKeepTheirCodes() throws Exception {
        when(bank.loadQuestion(manager, questionId)).thenThrow(ApiException.createNotFound("QUESTION_NOT_FOUND"));
        mvc.perform(get("/api/v1/questions/{id}", questionId))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("QUESTION_NOT_FOUND"));
        when(bank.updateQuestion(eq(manager), eq(questionId), any(), eq(3L)))
                .thenThrow(ApiException.createConflict("QUESTION_VERSION_CONFLICT", "x"));
        mvc.perform(patch("/api/v1/questions/{id}", questionId).contentType(MediaType.APPLICATION_JSON)
                        .content(BODY.substring(0, BODY.lastIndexOf('}')) + ",\"version\":3}"))
                .andExpect(status().isConflict());
    }

    @Test
    void bankAiDraftRoutesAcceptWithoutVersion() throws Exception {
        UUID draft = UUID.randomUUID();
        mvc.perform(post("/api/v1/subjects/{s}/question-ai-drafts", subject).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"instructions\":\"Ôn tập\",\"count\":3,\"source\":\"SHARED\",\"requestRef\":\"r\"}"))
                .andExpect(status().isCreated());
        verify(drafts).requestForBank(eq(manager), eq(subject), any());

        mvc.perform(post("/api/v1/subjects/{s}/question-ai-drafts/{d}:accept", subject, draft)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"questions\":[" + BODY + "]}"))
                .andExpect(status().isOk());
        verify(drafts).acceptForBank(eq(manager), eq(subject), eq(draft), any());
        mvc.perform(post("/api/v1/subjects/{s}/question-ai-drafts/{d}:accept", subject, draft)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"questions\":[" + BODY + "],\"version\":1}"))
                .andExpect(status().isBadRequest());
    }
}
