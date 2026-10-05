package edu.aiplatform.questionbank.port;

import edu.aiplatform.shared.contract.ActorRef;
import java.util.UUID;

/** Câu riêng của bài/template, `scope_type = ASSIGNMENT`; U08, U10 dùng. */
public interface InlineQuestionPort {
    QuestionVersion save(ActorRef actor, UUID assignmentId, QuestionType questionType, String definitionJson);
    QuestionVersion copyToAssignment(UUID questionId, UUID targetAssignmentId);
}
