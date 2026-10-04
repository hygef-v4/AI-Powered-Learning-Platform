package edu.aiplatform.aiexecution.port;

import edu.aiplatform.shared.contract.ActorRef;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** U13 cung cấp cho U15: đề xuất chấm bài GRADED, credit của giảng viên (BR-U13-20…24). */
public interface AiGradingPort {
    AiSuggestionView request(ActorRef teacher, SubmissionRef submission);
    List<AiSuggestionView> requestBatch(ActorRef teacher, List<SubmissionRef> submissions);
    Optional<AiSuggestionView> get(UUID suggestionId);
}
