package edu.aiplatform.identity.port;

import edu.aiplatform.shared.contract.ResourceRef;
import java.util.UUID;

public record AuthorizationDecision(boolean allowed, String reasonCode, UUID actorId, String action, ResourceRef resource) {}
