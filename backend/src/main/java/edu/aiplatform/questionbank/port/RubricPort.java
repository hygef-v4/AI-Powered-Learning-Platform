package edu.aiplatform.questionbank.port;

import edu.aiplatform.shared.contract.ActorRef;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** U06 cung cấp cho U09, U11, U13, U15 (BR-U06-32, 34…36). */
public interface RubricPort {
    RubricView createForAssignment(ActorRef actor, BankScope scope, String title, RubricCriteria criteria);
    RubricView revise(ActorRef actor, UUID rubricId, RubricCriteria criteria);
    RubricView cloneForAssignment(UUID rubricId, BankScope targetScope);
    Optional<RubricView> getRubric(UUID rubricId);
    BigDecimal score(UUID rubricId, Set<String> checkedItemIds);
}
