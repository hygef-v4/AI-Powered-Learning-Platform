package edu.aiplatform.aiexecution.application;

import edu.aiplatform.aiexecution.port.*;
import edu.aiplatform.shared.contract.ActorRef;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Khung cài đặt của U13: thân hàm tạm trả giá trị rỗng/an toàn hoặc báo chưa cài.
 */
@Service
public class AiSuggestionService implements AiDraftPort, AiGradingPort, PracticeGradingPort {

    @Override
    public AiSuggestionView request(ActorRef actor, DraftRequest request) {
        throw new UnsupportedOperationException("Chưa cài: U13");
    }

    @Override
    public Optional<AiSuggestionView> get(UUID suggestionId) {
        return Optional.empty();
    }

    @Override
    public void accept(UUID suggestionId) {
        // Chưa cài: U13.
    }

    @Override
    public void discard(UUID suggestionId) {
        // Chưa cài: U13.
    }

    @Override
    public AiSuggestionView request(ActorRef teacher, SubmissionRef submission) {
        throw new UnsupportedOperationException("Chưa cài: U13");
    }

    @Override
    public List<AiSuggestionView> requestBatch(ActorRef teacher, List<SubmissionRef> submissions) {
        return List.of();
    }

    @Override
    public AiSuggestionView request(ActorRef student, UUID attemptId) {
        throw new UnsupportedOperationException("Chưa cài: U13");
    }

    @Override
    public Optional<AiSuggestionView> latest(UUID attemptId) {
        return Optional.empty();
    }
}
