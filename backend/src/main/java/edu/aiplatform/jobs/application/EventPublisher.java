package edu.aiplatform.jobs.application;

import edu.aiplatform.jobs.contract.DomainEvent;
import edu.aiplatform.jobs.contract.IEventPublisher;
import edu.aiplatform.shared.security.ForbiddenKeyGuard;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Cài {@link IEventPublisher} (E1): gửi {@code platform.events} sau commit, lỗi chỉ log WARN (BR-U03-70). */
@Service
public class EventPublisher implements IEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(EventPublisher.class);

    private final IMessageSender sender;
    private final AfterCommitExecutor afterCommit;

    public EventPublisher(IMessageSender sender, AfterCommitExecutor afterCommit) {
        this.sender = sender;
        this.afterCommit = afterCommit;
    }

    @Override
    public void publish(DomainEvent event) {
        ForbiddenKeyGuard.check(event.payload() == null ? Map.of() : event.payload());
        afterCommit.run(() -> {
            try {
                sender.sendEvent(event);
            } catch (RuntimeException e) {
                log.warn("Gửi sự kiện {} lỗi, bỏ qua", event.eventType());
            }
        });
    }
}
