package edu.aiplatform.grading.service;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import org.springframework.transaction.*;
import org.springframework.transaction.support.SimpleTransactionStatus;
import edu.aiplatform.grading.model.Evaluation;
import edu.aiplatform.grading.service.GradingCommands.*;
import edu.aiplatform.identity.contract.*;
import edu.aiplatform.shared.error.ApiException;
import java.util.*;

class GradingBatchTest {
    @Test
    void mixedBatchHasIndependentTransactionsAndDeduplicates() {
        var grading = mock(GradingService.class);
        var manager = mock(PlatformTransactionManager.class);
        when(manager.getTransaction(any())).thenAnswer(i -> new SimpleTransactionStatus());
        var a = UUID.randomUUID();
        var b = UUID.randomUUID();
        var actor = new Actor(UUID.randomUUID(), Role.TEACHER);
        var first = new ItemVersion(a, 2);
        var second = new ItemVersion(b, 4);
        var e = new Evaluation();
        e.version = 3;
        when(grading.finalizeOne(actor, first)).thenReturn(e);
        when(grading.finalizeOne(actor, second)).thenThrow(ApiException.conflict("GRADE_VERSION_CONFLICT", "Reload"));
        var result = new GradingBatchService(grading, manager).run(actor, new Batch(List.of(first, second, first)),
                false);
        assertThat(result.items()).extracting(v -> v.outcome()).containsExactly("SUCCESS", "FAILED", "SKIPPED");
        verify(manager, times(2)).getTransaction(
                argThat(d -> d.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRES_NEW));
        verify(manager).commit(any());
        verify(manager).rollback(any());
        verify(grading, times(1)).finalizeOne(actor, first);
    }
}
