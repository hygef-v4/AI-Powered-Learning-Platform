package edu.aiplatform.grading.service;

import static org.assertj.core.api.Assertions.assertThat;

import edu.aiplatform.grading.model.GradingProposal;
import edu.aiplatform.shared.error.ApiException;
import java.time.Clock;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GradingProposalErrorTest {
    @Test
    void totalMismatchExplainsTheScoreErrorRatherThanImplyingTokenExhaustion() {
        var proposal = new GradingProposal();
        var error = ApiException.badRequest("INVALID_AI_PROPOSAL", "AI total does not match rubric item scores");
        GradingProposalCallbackSupport.applyRejectionFromException(proposal, UUID.randomUUID(), error, Clock.systemUTC());
        assertThat(proposal.safeError).contains("Tổng điểm", "không khớp")
                .doesNotContain("PROPOSAL_", "token");
        assertThat(GradingProposalValidator.buildRejectionDiagnostic(error)).isEqualTo("PROPOSAL_TOTAL_MISMATCH");
    }

    @Test
    void legacyMessagesAndDispatchCodesAreReadableWhilePreciseProviderReasonsArePreserved() {
        assertThat(GradingProposalCallbackSupport.formatTaskError(
                "Phản hồi AI chưa hợp lệ (PROPOSAL_TOTAL_MISMATCH). Bạn vẫn có thể chấm thủ công."))
                .contains("Tổng điểm", "không khớp").doesNotContain("PROPOSAL_");
        assertThat(GradingProposalCallbackSupport.formatTaskError("AI_QUOTA_EXCEEDED"))
                .contains("hạn mức", "token").doesNotContain("AI_QUOTA");
        assertThat(GradingProposalCallbackSupport.formatTaskError("AI_SERVICE_UNAVAILABLE"))
                .contains("kết nối").doesNotContain("AI_SERVICE");
        String timeout = "Nhà cung cấp AI không trả lời kịp";
        assertThat(GradingProposalCallbackSupport.formatTaskError(timeout)).isEqualTo(timeout);
        assertThat(GradingProposalCallbackSupport.formatTaskError("UNRECOGNIZED_INTERNAL_ERROR"))
                .doesNotContain("UNRECOGNIZED_INTERNAL_ERROR");
    }
}
