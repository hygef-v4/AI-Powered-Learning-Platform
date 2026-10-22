package edu.aiplatform.ai.application;

import edu.aiplatform.ai.contract.AiTaskState;
import edu.aiplatform.ai.domain.AiTask;
import edu.aiplatform.ai.domain.AiTaskRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;

/** Bảng {@code ai_tasks} trong bộ nhớ cho unit test; giữ đúng điều kiện {@code version} và {@code request_ref}. */
class FakeAiTaskRepository extends AiTaskRepository {

    final Map<UUID, AiTask> rows = new LinkedHashMap<>();
    final List<AiTaskState> history = new ArrayList<>();

    FakeAiTaskRepository() {
        super(null);
    }

    @Override
    public void insert(AiTask t) {
        if (rows.values().stream().anyMatch(r -> r.requestRef().equals(t.requestRef()))) {
            throw new DuplicateKeyException("request_ref");
        }
        rows.put(t.id(), copy(t, 0));
        history.add(t.state());
    }

    @Override
    public boolean update(AiTask t) {
        AiTask current = rows.get(t.id());
        if (current == null || current.version() != t.version()) {
            return false;
        }
        rows.put(t.id(), copy(t, t.version() + 1));
        history.add(t.state());
        return true;
    }

    @Override
    public Optional<AiTask> findById(UUID id) {
        return Optional.ofNullable(rows.get(id));
    }

    @Override
    public Optional<AiTask> lock(UUID id) {
        return findById(id);
    }

    @Override
    public Optional<AiTask> findByRequestRef(String requestRef) {
        return rows.values().stream().filter(r -> r.requestRef().equals(requestRef)).findFirst();
    }

    @Override
    public int countActive(UUID actorId) {
        return (int) rows.values().stream().filter(r -> r.actorId().equals(actorId) && r.active()).count();
    }

    @Override
    public List<AiTask> listByStateBefore(AiTaskState state, Instant before) {
        return rows.values().stream().filter(r -> r.state() == state && r.createdAt().isBefore(before)).toList();
    }

    @Override
    public List<AiTask> listUnhandled(Instant before) {
        return rows.values().stream().filter(r -> r.finished() && !r.handlerDone() && r.finishedAt().isBefore(before))
                .toList();
    }

    static AiTask copy(AiTask t, long version) {
        return new AiTask(t.id(), t.requestRef(), t.fingerprint(), t.actorId(), t.source(), t.connectionId(), t.provider(),
                t.model(), t.taskType(), t.targetRef(), t.inputVersion(), t.state(), t.inputPayload(), t.outputSchema(),
                t.resultPayload(), t.maxOutputTokens(), t.reservedTokens(), t.quotaDay(), t.dispatchedAt(),
                t.providerRequestId(), t.applyResult(), t.handlerDone(), t.safeError(), t.createdAt(), t.finishedAt(),
                version);
    }
}
