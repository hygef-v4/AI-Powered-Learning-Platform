package edu.aiplatform.grading.service;

import edu.aiplatform.grading.dto.GradingViews;

import edu.aiplatform.grading.service.GradingCommands.Batch;
import edu.aiplatform.grading.dto.GradingViews.BatchResult;
import edu.aiplatform.grading.dto.GradingViews.ItemResult;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.shared.error.ApiException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class GradingBatchService {
    private final GradingService grading;
    private final TransactionTemplate tx;

    public GradingBatchService(GradingService grading, PlatformTransactionManager manager) {
        this.grading = grading;
        tx = new TransactionTemplate(manager);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public BatchResult run(Actor actor, Batch batch, boolean publish) {
        if (batch.items() == null || batch.items().isEmpty() || batch.items().size() > 200)
            throw ApiException.badRequest("INVALID_BATCH", "Batch must contain 1–200 items");
        var outcomes = new ArrayList<ItemResult>();
        Set<UUID> seen = new HashSet<>();
        for (var item : batch.items()) {
            if (!seen.add(item.evaluationId())) {
                outcomes.add(new ItemResult(item.evaluationId(), "SKIPPED", "DUPLICATE_ITEM", null));
                continue;
            }
            try {
                var evaluation = tx.execute(
                        status -> publish ? grading.publishOne(actor, item) : grading.finalizeOne(actor, item));
                outcomes.add(new ItemResult(item.evaluationId(), "SUCCESS", null, evaluation.version));
            } catch (ApiException ex) {
                outcomes.add(new ItemResult(item.evaluationId(), "FAILED", ex.code(), null));
            } catch (RuntimeException ex) {
                outcomes.add(new ItemResult(item.evaluationId(), "FAILED", "GRADING_WRITE_FAILED", null));
            }
        }
        return new BatchResult(outcomes);
    }
}
