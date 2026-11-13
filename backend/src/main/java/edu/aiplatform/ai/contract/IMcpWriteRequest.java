package edu.aiplatform.ai.contract;

import edu.aiplatform.identity.contract.Actor;
import java.util.Map;
import java.util.function.Supplier;

/** Caller must reauthorize the current resource before every execute, including replay. */
public interface IMcpWriteRequest {
    Map<String,Object> execute(Actor actor, String operation, String requestRef, String fingerprint,
                              Supplier<Map<String,Object>> mutation);
}
