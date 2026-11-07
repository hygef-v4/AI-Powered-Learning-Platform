package edu.aiplatform.grading.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
    @ParameterizedTest
    @CsvSource({"-1,false", "0,false", "201,false", "-1,true", "0,true", "201,true"})
    void invalidBatchSizeFailsBeforeTransactionOrGrading(int size, boolean publish) {
        var gradingService = mock(GradingService.class);
        var manager = mock(PlatformTransactionManager.class);
        var service = new GradingBatchService(gradingService, manager);
        var actor = new Actor(UUID.randomUUID(), Role.TEACHER);
        List<ItemVersion> items = size < 0 ? null
                : Collections.nCopies(size, new ItemVersion(UUID.randomUUID(), 0));

        assertThatThrownBy(() -> service.executeBatch(actor, new Batch(items), publish))
                .isInstanceOfSatisfying(ApiException.class, error -> {
                    assertThat(error.status().value()).isEqualTo(400);
                    assertThat(error.code()).isEqualTo("INVALID_BATCH");
                    assertThat(error.getMessage()).isEqualTo("Batch must contain 1–200 items");
                });
        verifyNoInteractions(gradingService, manager);
    }

    @ParameterizedTest
    @CsvSource({"1,false", "200,false", "1,true", "200,true"})
    void boundaryBatchSizesProcessEveryItemIndependently(int size, boolean publish) {
        var gradingService = mock(GradingService.class);
        var manager = mock(PlatformTransactionManager.class);
        when(manager.getTransaction(any())).thenAnswer(call -> new SimpleTransactionStatus());
        var evaluation = new Evaluation();
        evaluation.version = 1;
        if (publish) {
            when(gradingService.publishEvaluation(any(), any())).thenReturn(evaluation);
        } else {
            when(gradingService.finalizeEvaluation(any(), any())).thenReturn(evaluation);
        }
        var items = java.util.stream.IntStream.range(0, size)
                .mapToObj(index -> new ItemVersion(UUID.randomUUID(), 0)).toList();

        var result = new GradingBatchService(gradingService, manager).executeBatch(
                new Actor(UUID.randomUUID(), Role.TEACHER), new Batch(items), publish);

        assertThat(result.items()).hasSize(size)
                .allSatisfy(item -> assertThat(item.outcome()).isEqualTo("SUCCESS"));
        verify(manager, times(size)).getTransaction(argThat(definition -> definition
                .getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRES_NEW));
        verify(manager, times(size)).commit(any());
        verify(manager, never()).rollback(any());
    }

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
        when(grading.finalizeEvaluation(actor, first)).thenReturn(e);
        when(grading.finalizeEvaluation(actor, second)).thenThrow(ApiException.conflict("GRADE_VERSION_CONFLICT", "Reload"));
        var result = new GradingBatchService(grading, manager).executeBatch(actor, new Batch(List.of(first, second, first)),
                false);
        assertThat(result.items()).extracting(v -> v.outcome()).containsExactly("SUCCESS", "FAILED", "SKIPPED");
        verify(manager, times(2)).getTransaction(
                argThat(d -> d.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRES_NEW));
        verify(manager).commit(any());
        verify(manager).rollback(any());
        verify(grading, times(1)).finalizeEvaluation(actor, first);
    }
}
