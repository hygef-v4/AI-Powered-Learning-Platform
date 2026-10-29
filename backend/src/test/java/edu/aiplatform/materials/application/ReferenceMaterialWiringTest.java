package edu.aiplatform.materials.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import edu.aiplatform.ai.contract.IReferenceMaterial;
import edu.aiplatform.ai.contract.stub.AiContractStubs;
import edu.aiplatform.materials.domain.IMaterialRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class ReferenceMaterialWiringTest {
    @Test void realU13ProviderReplacesOnlyConditionalReferenceStub() {
        var provider=new ReferenceMaterialService(mock(MaterialQueryService.class),mock(IMaterialRepository.class));
        new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(AiContractStubs.class))
                .withBean(ReferenceMaterialService.class,()->provider).run(context->{
                    assertThat(context).hasSingleBean(IReferenceMaterial.class);
                    assertThat(context.getBean(IReferenceMaterial.class)).isSameAs(provider);
                });
    }
}
