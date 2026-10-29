package edu.aiplatform.assignments.application;

import edu.aiplatform.assignments.contract.AssignmentScope;
import edu.aiplatform.assignments.contract.IAssignmentLifecycleListener;
import edu.aiplatform.assignments.domain.Assignment;
import edu.aiplatform.assignments.domain.AssignmentRepository;
import edu.aiplatform.jobs.contract.DomainEvent;
import edu.aiplatform.jobs.contract.EventTypes;
import edu.aiplatform.jobs.contract.IEventPublisher;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Mở bài và báo ngưng giao (BR-U07-34, 36): chạy trong transaction của người gọi; listener của U08, U12 chạy cùng
 * transaction; {@code assignment.opened} phát sau commit (U03). Chưa có unit cài listener thì không làm gì.
 */
@Component
class AssignmentLifecycle {

    private final AssignmentRepository assignments;
    private final ObjectProvider<IAssignmentLifecycleListener> listeners;
    private final IEventPublisher events;

    AssignmentLifecycle(AssignmentRepository assignments, ObjectProvider<IAssignmentLifecycleListener> listeners,
                        IEventPublisher events) {
        this.assignments = assignments;
        this.listeners = listeners;
        this.events = events;
    }

    /** {@code SCHEDULED → OPEN} có điều kiện; trả {@code false} nếu bài không còn chờ mở (chạy lại không trùng). */
    boolean open(UUID id, Instant now) {
        if (assignments.open(id, now) != 1) {
            return false;
        }
        listeners.orderedStream().forEach(listener -> listener.onOpened(id));
        assignments.find(id).ifPresent(a -> events.publish(opened(a, now)));
        return true;
    }

    void retired(UUID id) {
        listeners.orderedStream().forEach(listener -> listener.onRetired(id));
    }

    private static DomainEvent opened(Assignment a, Instant now) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("assignmentId", a.id().toString());
        payload.put("scope", a.scope().name());
        if (a.scope() == AssignmentScope.CLASS) {
            payload.put("classId", a.classId().toString());
        }
        payload.put("subjectId", a.subjectId().toString());
        payload.put("title", a.title());
        payload.put("versionNo", a.versionNo());
        payload.put("opensAt", a.opensAt().toString());
        payload.put("closesAt", a.closesAt().toString());
        if (a.lateUntil() != null) {
            payload.put("lateUntil", a.lateUntil().toString());
        }
        return new DomainEvent(1, UUID.randomUUID(), EventTypes.ASSIGNMENT_OPENED, "U07", now, a.id().toString(),
                Map.copyOf(payload));
    }
}
