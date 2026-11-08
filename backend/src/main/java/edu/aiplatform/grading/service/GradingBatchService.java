package edu.aiplatform.grading.service;


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
    private final GradingService gradingService;
    private final TransactionTemplate transactionTemplate;

    /** Creates the batch executor with grading and transaction dependencies. */
    public GradingBatchService(GradingService gradingService, PlatformTransactionManager manager) {
        this.gradingService = gradingService;
        transactionTemplate = new TransactionTemplate(manager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /** Applies each grading item independently and optionally publishes successful grades. */
    public BatchResult executeBatch(Actor actor, Batch batch, boolean publish) {
        // Step 1: Validate batch items count (must be between 1 and 200 items)
        GradingCommands.validateBatchItems(batch.items());
        var outcomes = new ArrayList<ItemResult>();
        Set<UUID> seen = new HashSet<>();

        // Step 2: Iterate over each evaluation item in the batch
        for (var item : batch.items()) {
            // Deduplicate items in the request
            if (!seen.add(item.evaluationId())) {
                outcomes.add(
                        new ItemResult(item.evaluationId(), "SKIPPED", "DUPLICATE_ITEM", null));
                continue;
            }
            try {
                // Step 3: Execute finalize or publish inside an isolated transaction per item (REQUIRES_NEW)
                var evaluation = transactionTemplate
                        .execute(status -> publish ? gradingService.publishEvaluation(actor, item)
                                : gradingService.finalizeEvaluation(actor, item));
                outcomes.add(
                        new ItemResult(item.evaluationId(), "SUCCESS", null, evaluation.version));
            } catch (ApiException ex) {
                // Individual item failure does not roll back other items in the batch
                outcomes.add(new ItemResult(item.evaluationId(), "FAILED", ex.code(), null));
            } catch (RuntimeException ex) {
                outcomes.add(new ItemResult(item.evaluationId(), "FAILED", "GRADING_WRITE_FAILED",
                        null));
            }
        }
        return new BatchResult(outcomes);
    }
}
