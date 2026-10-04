package edu.aiplatform.aiexecution.port;

import edu.aiplatform.shared.contract.ActorRef;
import java.util.Optional;
import java.util.UUID;

/** U13 cung cấp cho U06, U08, U09, U10. */
public interface AiDraftPort {
    AiSuggestionView request(ActorRef actor, DraftRequest request);
    Optional<AiSuggestionView> get(UUID suggestionId);
    void accept(UUID suggestionId);
    void discard(UUID suggestionId);
}
