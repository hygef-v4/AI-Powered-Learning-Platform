package edu.aiplatform.ai.application;

import edu.aiplatform.ai.contract.AiTaskFailure;
import edu.aiplatform.ai.contract.AiTaskResult;
import edu.aiplatform.ai.contract.AiTaskState;
import edu.aiplatform.ai.contract.ApplyOutcome;
import edu.aiplatform.ai.contract.IAiTaskResultHandler;
import edu.aiplatform.ai.domain.AiTask;
import edu.aiplatform.ai.domain.AiTaskRepository;
import edu.aiplatform.identity.contract.AccountContact;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.IAccountLookup;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Chuyển kết quả task đã kết thúc cho handler của unit gọi trong transaction riêng, rồi xóa prompt và kết quả (P3,
 * P6). Handler lỗi thì giữ nguyên để lượt sau chỉ gọi lại handler, không gọi provider.
 */
@Component
public class AiTaskDelivery {

    private final AiTaskRepository tasks;
    private final AiTaskResultHandlerRegistry handlers;
    private final IAccountLookup accounts;
    private final TransactionTemplate transactions;

    public AiTaskDelivery(AiTaskRepository tasks, AiTaskResultHandlerRegistry handlers, IAccountLookup accounts,
                          PlatformTransactionManager transactionManager) {
        this.tasks = tasks;
        this.handlers = handlers;
        this.accounts = accounts;
        this.transactions = new TransactionTemplate(transactionManager);
    }

    /** Trả {@code true} khi đã chuyển xong (hoặc không còn gì để chuyển). */
    public boolean deliver(UUID taskId) {
        Boolean done = transactions.execute(status -> {
            AiTask task = tasks.lock(taskId).orElse(null);
            if (task == null || !task.finished() || task.handlerDone()) {
                return true;
            }
            Optional<IAiTaskResultHandler> handler = handlers.find(task.taskType());
            Actor actor = actorOf(task.actorId());
            ApplyOutcome outcome;
            if (handler.isEmpty() || actor == null) {
                outcome = ApplyOutcome.REJECTED;
            } else if (task.state() == AiTaskState.READY) {
                outcome = handler.get().onReady(new AiTaskResult(task.id(), task.taskType(), actor, task.targetRef(),
                        task.inputVersion(), task.resultPayload()));
            } else {
                handler.get().onFailed(new AiTaskFailure(task.id(), task.taskType(), actor, task.targetRef(), task.state(),
                        task.safeError()));
                outcome = null;
            }
            if (!tasks.update(task.handled(outcome))) {
                throw new IllegalStateException("Task " + taskId + " vừa bị thay đổi");
            }
            return true;
        });
        return Boolean.TRUE.equals(done);
    }

    /** Role hiện hành của người yêu cầu; handler tự kiểm lại quyền trên đích (BR-U06-23). */
    private Actor actorOf(UUID accountId) {
        try {
            return accounts.getContact(accountId).map(AccountContact::role).map(role -> new Actor(accountId, role))
                    .orElse(null);
        } catch (RuntimeException e) {
            return null;
        }
    }
}
