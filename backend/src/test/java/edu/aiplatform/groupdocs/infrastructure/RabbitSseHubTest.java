package edu.aiplatform.groupdocs.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class RabbitSseHubTest {

    private final RabbitTemplate rabbit = mock(RabbitTemplate.class);
    private final RabbitSseHub hub = new RabbitSseHub(rabbit);

    @AfterEach
    void tearDown() {
        hub.stop();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void publishWaitsForCommit() {
        TransactionSynchronizationManager.initSynchronization();

        hub.publish("group-doc:1", Map.of("type", "section.done"));

        verify(rabbit, never()).convertAndSend(anyString(), anyString(), any(Object.class));
        for (TransactionSynchronization s : TransactionSynchronizationManager.getSynchronizations()) {
            s.afterCommit();
        }
        verify(rabbit).convertAndSend(eq("platform.realtime"), eq(""),
                eq(Map.of("channel", "group-doc:1", "event", Map.of("type", "section.done"))));
    }

    @Test
    void deliveredEventReachesOnlyEmittersOfThatChannelAndBrokenEmitterIsDropped() {
        UUID doc = UUID.randomUUID();
        Recording mine = new Recording(false);
        Recording broken = new Recording(true);
        Recording other = new Recording(false);
        hub.channels().add("group-doc:" + doc, "a", mine);
        hub.channels().add("group-doc:" + doc, "b", broken);
        hub.channels().add("group-doc:" + UUID.randomUUID(), "c", other);

        hub.deliver(Map.of("channel", "group-doc:" + doc, "event", Map.of("type", "section.claimed")));

        assertThat(mine.sent).hasSize(1);
        assertThat(other.sent).isEmpty();
        assertThat(hub.channels().size("group-doc:" + doc)).isEqualTo(1);
    }

    @Test
    void closeMessageCompletesOnlyTheRemovedMembersStream() {
        UUID doc = UUID.randomUUID();
        UUID removed = UUID.randomUUID();
        Recording leaving = new Recording(false);
        Recording staying = new Recording(false);
        hub.channels().add("group-doc:" + doc, removed.toString(), leaving);
        hub.channels().add("group-doc:" + doc, UUID.randomUUID().toString(), staying);

        hub.closeFor(removed, Set.of(doc));
        hub.deliver(Map.of("close", removed.toString(), "channels", List.of("group-doc:" + doc)));

        verify(rabbit).convertAndSend(eq("platform.realtime"), eq(""), any(Object.class));
        assertThat(leaving.completed).isTrue();
        assertThat(staying.completed).isFalse();
        assertThat(hub.channels().size("group-doc:" + doc)).isEqualTo(1);
    }

    /** Emitter ghi lại sự kiện; {@code failing} giả lập kết nối đã đứt. */
    static final class Recording extends SseEmitter {
        final List<Object> sent = new ArrayList<>();
        final boolean failing;
        boolean completed;

        Recording(boolean failing) {
            this.failing = failing;
        }

        @Override
        public void send(SseEventBuilder builder) throws IOException {
            if (failing) {
                throw new IOException("broken pipe");
            }
            sent.add(builder);
        }

        @Override
        public void complete() {
            completed = true;
        }
    }
}
