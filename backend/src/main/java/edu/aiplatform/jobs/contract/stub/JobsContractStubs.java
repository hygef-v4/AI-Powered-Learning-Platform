package edu.aiplatform.jobs.contract.stub;

import edu.aiplatform.jobs.contract.IEventPublisher;
import edu.aiplatform.jobs.contract.IJob;
import edu.aiplatform.shared.stub.Stubs;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/** Bản tạm của interface việc nền và sự kiện (U03): chỉ ghi log, không gửi lên RabbitMQ nên việc nền không chạy. */
@AutoConfiguration
@ConditionalOnProperty(name = "platform.contract-stubs.enabled", havingValue = "true", matchIfMissing = true)
public class JobsContractStubs {

    @Bean
    @ConditionalOnMissingBean
    IJob jobStub() {
        return Stubs.announce(IJob.class, (jobType, payload, idempotencyKey) ->
                Stubs.called(IJob.class, "bỏ qua việc " + jobType + " key=" + idempotencyKey));
    }

    @Bean
    @ConditionalOnMissingBean
    IEventPublisher eventPublisherStub() {
        return Stubs.announce(IEventPublisher.class, event ->
                Stubs.called(IEventPublisher.class, "bỏ qua sự kiện " + event.eventType()));
    }
}
