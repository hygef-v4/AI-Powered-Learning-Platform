package edu.aiplatform.questionbank.application;

import edu.aiplatform.questionbank.port.*;
import edu.aiplatform.shared.contract.ActorRef;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Khung cài đặt của U06: thân hàm tạm trả giá trị rỗng/an toàn hoặc báo chưa cài.
 */
@Service
public class BankItemService implements RubricPort, InlineQuestionPort, BankCopyPort, QuestionVerificationPort {

    @Override
    public RubricView createForAssignment(ActorRef actor, BankScope scope, String title, RubricCriteria criteria) {
        throw new UnsupportedOperationException("Chưa cài: U06");
    }

    @Override
    public RubricView revise(ActorRef actor, UUID rubricId, RubricCriteria criteria) {
        throw new UnsupportedOperationException("Chưa cài: U06");
    }

    @Override
    public RubricView cloneForAssignment(UUID rubricId, BankScope targetScope) {
        throw new UnsupportedOperationException("Chưa cài: U06");
    }

    @Override
    public Optional<RubricView> getRubric(UUID rubricId) {
        return Optional.empty();
    }

    @Override
    public BigDecimal score(UUID rubricId, Set<String> checkedItemIds) {
        return BigDecimal.ZERO;
    }

    @Override
    public QuestionVersion save(ActorRef actor, UUID assignmentId, QuestionType questionType, String definitionJson) {
        throw new UnsupportedOperationException("Chưa cài: U06");
    }

    @Override
    public QuestionVersion copyToAssignment(UUID questionId, UUID targetAssignmentId) {
        throw new UnsupportedOperationException("Chưa cài: U06");
    }

    @Override
    public QuestionVersion copyToClass(UUID questionId, UUID targetClassId) {
        throw new UnsupportedOperationException("Chưa cài: U06");
    }

    @Override
    public void record(UUID questionId, String contentHash, boolean allPassed, String resultJson) {
        // Chưa cài: U06.
    }
}
