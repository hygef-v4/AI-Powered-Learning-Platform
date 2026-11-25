package edu.aiplatform.grading.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
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
                    assertThat(error.readStatus().value()).isEqualTo(400);
                    assertThat(error.readCode()).isEqualTo("INVALID_BATCH");
                    assertThat(error.getMessage()).isEqualTo("Batch must contain 1–200 items");
                });
        verifyNoInteractions(gradingService, manager);
    }
}
