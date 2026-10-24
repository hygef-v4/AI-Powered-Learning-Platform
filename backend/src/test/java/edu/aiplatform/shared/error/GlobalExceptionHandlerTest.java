package edu.aiplatform.shared.error;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.aiplatform.grading.application.GradingCommands.Save;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

class GlobalExceptionHandlerTest {
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new ValidationProbeController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void oversizedFeedbackExplainsTheLimitInsteadOfSuggestingReload() throws Exception {
        String body = "{\"version\":0,\"checkedItemsByRubric\":{},\"adjustment\":0,\"feedback\":\""
                + "x".repeat(10_001) + "\"}";

        mvc.perform(post("/grading").contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("FEEDBACK_TOO_LONG"))
                .andExpect(jsonPath("$.detail").value("Nhận xét tối đa 10.000 ký tự."));
    }

    @RestController
    static class ValidationProbeController {
        @PostMapping("/grading")
        void save(@Valid @RequestBody Save input) {
        }
    }
}
