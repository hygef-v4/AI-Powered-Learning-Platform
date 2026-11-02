package edu.aiplatform.ai.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import edu.aiplatform.ai.contract.IAiTaskResultHandler;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class AiTaskResultHandlerRegistryTest {

    @Test
    void handlersAreLookedUpOnFirstFindNotAtConstruction() {
        AtomicInteger lookups = new AtomicInteger();
        IAiTaskResultHandler draft = handler("ASSIGNMENT_DRAFT");
        AiTaskResultHandlerRegistry registry = new AiTaskResultHandlerRegistry(() -> {
            lookups.incrementAndGet();
            return List.of(draft);
        });

        assertThat(lookups).as("dựng bean không đụng tới handler (tránh vòng phụ thuộc)").hasValue(0);
        assertThat(registry.find("ASSIGNMENT_DRAFT")).contains(draft);
        assertThat(registry.find("UNKNOWN")).isEmpty();
        assertThat(lookups).as("chỉ lấy danh sách một lần").hasValue(1);
    }

    @Test
    void twoHandlersForOneTaskTypeAreRejected() {
        AiTaskResultHandlerRegistry registry = AiTaskResultHandlerRegistry.of(
                List.of(handler("PRACTICE_FEEDBACK"), handler("PRACTICE_FEEDBACK")));

        assertThatThrownBy(() -> registry.find("PRACTICE_FEEDBACK")).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PRACTICE_FEEDBACK");
    }

    private static IAiTaskResultHandler handler(String taskType) {
        IAiTaskResultHandler handler = mock(IAiTaskResultHandler.class);
        when(handler.taskType()).thenReturn(taskType);
        return handler;
    }
}
