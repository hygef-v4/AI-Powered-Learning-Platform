package edu.aiplatform.identity.port;

import edu.aiplatform.shared.contract.ActorRef;
import edu.aiplatform.shared.contract.ResourceRef;

/** U01 cung cấp cho mọi unit: mặc định từ chối, kết hợp role và phạm vi U04 (F13, BR-U01-62, 93). */
public interface AuthorizationPort {
    AuthorizationDecision authorize(ActorRef actor, String action, ResourceRef resource);
}
