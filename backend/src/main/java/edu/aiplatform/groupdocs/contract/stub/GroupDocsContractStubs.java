package edu.aiplatform.groupdocs.contract.stub;

import edu.aiplatform.groupdocs.contract.ISseHub;
import edu.aiplatform.shared.stub.Stubs;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** Bản tạm của {@code ISseHub} (U12): không đẩy realtime; kênh SSE mở ra rồi đóng ngay, frontend tự tải lại. */
@AutoConfiguration
@ConditionalOnProperty(name = "platform.contract-stubs.enabled", havingValue = "true", matchIfMissing = true)
public class GroupDocsContractStubs {

    @Bean
    @ConditionalOnMissingBean
    ISseHub sseHubStub() {
        return Stubs.announce(ISseHub.class, new ISseHub() {
            @Override
            public void publish(String channelKey, Object event) {
            }

            @Override
            public SseEmitter subscribe(String channelKey) {
                SseEmitter emitter = new SseEmitter();
                emitter.complete();
                return emitter;
            }
        });
    }
}
