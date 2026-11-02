package edu.aiplatform.grading.application;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.grading.domain.*;
import edu.aiplatform.identity.contract.*;
import edu.aiplatform.shared.error.ApiException;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
class GradingMcpReplayTest {
    @Test void retryCannotReturnAProposalFromAnOlderRubricSnapshot() {
        var json=new ObjectMapper();var actor=new Actor(UUID.randomUUID(),Role.TEACHER);
        var evaluation=Evaluation.pending(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),"hash",BigDecimal.TEN,"[]",Instant.now());
        var grading=mock(GradingService.class);when(grading.requireCurrent(actor,evaluation.id,0L)).thenReturn(evaluation);
        var proposals=mock(IGradingProposalRepository.class);var existing=new GradingProposal();
        existing.id=UUID.randomUUID();existing.evaluationId=evaluation.id;existing.requesterId=actor.accountId();
        existing.submissionHash="hash";existing.rubricFingerprint="previous-rubric";existing.proposal=json.createObjectNode();
        when(proposals.findByRequestRef("retry")).thenReturn(Optional.of(existing));
        var tx=new GradingProposalTransactions(null,proposals,grading,null,null,null,null,mock(GradingProposalValidator.class),json,Clock.systemUTC());
        assertThatThrownBy(() -> tx.recordMcp(actor,evaluation.id,0,"hash","retry",json.createObjectNode(),"Agent"))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("STALE_PROPOSAL"));
    }
}
