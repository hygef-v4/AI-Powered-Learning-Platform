package edu.aiplatform.assignments.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import edu.aiplatform.ai.contract.MaterialText;
import edu.aiplatform.assignments.contract.AssignmentScope;
import edu.aiplatform.assignments.contract.AssignmentStatus;
import edu.aiplatform.assignments.contract.DocumentModel;
import edu.aiplatform.assignments.contract.GradingMode;
import edu.aiplatform.assignments.contract.SubmissionMode;
import edu.aiplatform.assignments.model.Assignment;
import edu.aiplatform.assignments.model.AssignmentOrigin;
import java.math.BigDecimal;
import java.util.List;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AssignmentDraftPromptBuilderTest {

    @Test
    void vietnameseOptionAsksForVietnameseContentAndJapaneseIsRejected() {
        // Bỏ tiếng Nhật khỏi hệ thống (2026-11-21): chỉ còn VI, EN, AUTO.
        Assignment assignment = new Assignment(UUID.randomUUID(), AssignmentScope.CLASS, UUID.randomUUID(),
                UUID.randomUUID(), null, AssignmentOrigin.MANUAL, 1, SubmissionMode.INDIVIDUAL, true, GradingMode.GRADED,
                "Draft", "", new DocumentModel(List.of()), List.of(), Map.of(), List.of(), false, null,
                AssignmentStatus.DRAFT, null, null, null, null, null, UUID.randomUUID(), Instant.now(), Instant.now(), 0);
        var options = new AssignmentCommands.DraftOptions("AUTO", "VI", 2, null);

        var prompt = AssignmentDraftPromptBuilder.buildPrompt(assignment, "Create an exam.", List.of(), options);

        assertThat(prompt.system()).contains("Write all student-facing content in Vietnamese");
        assertThat(prompt.user()).contains("outputLanguage=Vietnamese");
        assertThatThrownBy(() -> AssignmentAiDraftService.normalizeOptions(
                new AssignmentCommands.DraftOptions("AUTO", "JA", 2, null)))
                .isInstanceOfSatisfying(edu.aiplatform.shared.error.ApiException.class,
                        ex -> assertThat(ex.readCode()).isEqualTo("AI_OPTIONS_INVALID"));
    }

    @Test
    void missingLanguageDefaultsToEnglish() {
        Assignment assignment = new Assignment(UUID.randomUUID(), AssignmentScope.CLASS, UUID.randomUUID(),
                UUID.randomUUID(), null, AssignmentOrigin.MANUAL, 1, SubmissionMode.INDIVIDUAL, true, GradingMode.GRADED,
                "Draft", "", new DocumentModel(List.of()), List.of(), Map.of(), List.of(), false, null,
                AssignmentStatus.DRAFT, null, null, null, null, null, UUID.randomUUID(), Instant.now(), Instant.now(), 0);

        var prompt = AssignmentDraftPromptBuilder.buildPrompt(assignment, "Create an exam.", List.of(), null);

        assertThat(prompt.system()).contains("Write all student-facing content in English")
                .doesNotContain("QUESTION_COUNT_CONFLICT");
        assertThat(prompt.user()).contains("outputLanguage=English");
    }
}
