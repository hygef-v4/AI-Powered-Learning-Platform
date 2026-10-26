package edu.aiplatform.ai.contract.stub;

import edu.aiplatform.ai.contract.AiProvider;
import edu.aiplatform.ai.contract.AiSource;
import edu.aiplatform.ai.contract.AiTaskRequest;
import edu.aiplatform.ai.contract.AiTaskState;
import edu.aiplatform.ai.contract.AiTaskView;
import edu.aiplatform.ai.contract.IAiTask;
import edu.aiplatform.ai.contract.IReferenceMaterial;
import edu.aiplatform.ai.contract.MaterialText;
import edu.aiplatform.ai.contract.SelectableMaterial;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.ResourceRef;
import edu.aiplatform.shared.stub.Stubs;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * Bản tạm của interface U06 (và {@code IReferenceMaterial} do U13 cài): task AI trả ngay {@code FAILED} kèm lý do
 * để màn hình không chờ mãi; chưa có tài liệu tham khảo nào.
 */
@AutoConfiguration
@ConditionalOnProperty(name = "platform.contract-stubs.enabled", havingValue = "true", matchIfMissing = true)
public class AiContractStubs {

    @Bean
    @ConditionalOnMissingBean
    IAiTask aiTaskStub() {
        return Stubs.announce(IAiTask.class, new IAiTask() {
            @Override
            public AiTaskView submit(AiTaskRequest request) {
                return new AiTaskView(UUID.randomUUID(), AiTaskState.FAILED, request.source(), request.provider(), null,
                        null, "AI chưa sẵn sàng", null);
            }

            @Override
            public void requireSource(Actor actor, AiSource source, AiProvider provider) {
            }

            @Override
            public Optional<AiTaskView> get(UUID taskId) {
                return Optional.empty();
            }

            @Override
            public void cancel(Actor actor, UUID taskId) {
            }
        });
    }

    @Bean
    @ConditionalOnMissingBean
    IReferenceMaterial referenceMaterialStub() {
        return Stubs.announce(IReferenceMaterial.class, new IReferenceMaterial() {
            @Override
            public List<SelectableMaterial> listSelectable(Actor actor, ResourceRef scopeRef) {
                return List.of();
            }

            @Override
            public List<MaterialText> extractText(Actor actor, List<UUID> materialIds, int maxChars) {
                return List.of();
            }
        });
    }
}
