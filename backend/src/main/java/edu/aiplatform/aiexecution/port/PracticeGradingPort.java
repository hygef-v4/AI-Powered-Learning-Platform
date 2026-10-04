package edu.aiplatform.aiexecution.port;

import edu.aiplatform.shared.contract.ActorRef;
import java.util.Optional;
import java.util.UUID;

/** U13 cung cấp cho U11: Student bấm chấm attempt PRACTICE Text/Diagram Essay đã nộp (BR-U13-03, 24). */
public interface PracticeGradingPort {
    AiSuggestionView request(ActorRef student, UUID attemptId);
    Optional<AiSuggestionView> latest(UUID attemptId);
}
