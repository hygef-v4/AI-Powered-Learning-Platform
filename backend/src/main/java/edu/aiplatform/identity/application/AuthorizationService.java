package edu.aiplatform.identity.application;

import edu.aiplatform.identity.port.*;
import edu.aiplatform.shared.contract.ActorRef;
import edu.aiplatform.shared.contract.ResourceRef;
import org.springframework.stereotype.Service;

/**
 * Khung cài đặt của U01: thân hàm tạm trả giá trị rỗng/an toàn hoặc báo chưa cài.
 * Plan U01: Bước 15.
 */
@Service
public class AuthorizationService implements AuthorizationPort {

    @Override
    public AuthorizationDecision authorize(ActorRef actor, String action, ResourceRef resource) {
        return new AuthorizationDecision(false, "NOT_IMPLEMENTED", actor.accountId(), action, resource);
    }
}
