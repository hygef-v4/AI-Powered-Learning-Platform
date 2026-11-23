package edu.aiplatform.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.ai.contract.*;
import edu.aiplatform.ai.repository.McpWriteRequestRepository;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.shared.error.ApiException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** A transaction-scoped PostgreSQL lock serializes retries, including the first insert. */
@Service
public class McpWriteRequestService implements IMcpWriteRequest {
    /** Agents retry within minutes; a week keeps replays safe without growing the table forever. */
    static final Duration RETENTION = Duration.ofDays(7);
    private static final TypeReference<Map<String, Object>> RESPONSE_TYPE = new TypeReference<>() {};

    private final McpWriteRequestRepository mcpWriteRequestRepository;
    private final ObjectMapper json;
    private final TransactionTemplate transaction;

    public McpWriteRequestService(McpWriteRequestRepository mcpWriteRequestRepository, ObjectMapper json,
                                  PlatformTransactionManager manager) {
        this.mcpWriteRequestRepository = mcpWriteRequestRepository;
        this.json = json;
        this.transaction = new TransactionTemplate(manager);
        this.transaction.setTimeout(30);
    }

    @Override
    public Map<String, Object> execute(Actor actor, String operation, String requestRef,
                                       String fingerprint,
                                       Supplier<Map<String, Object>> mutation) {
        McpInput.requireTeaching(actor);
        if (operation == null || operation.isBlank() || operation.length() > 100
                || requestRef == null || requestRef.isBlank() || requestRef.length() > 200
                || fingerprint == null || fingerprint.length() > 10000) {
            throw McpInput.createInvalidArguments();
        }
        return transaction.execute(status -> {
            mcpWriteRequestRepository.lockRequest(actor.accountId() + ":" + operation + ":" + requestRef);
            var stored = mcpWriteRequestRepository.findRequest(actor.accountId(), operation, requestRef);
            if (stored.isPresent()) {
                if (!fingerprint.equals(stored.get().fingerprint())) {
                    throw ApiException.createConflict("MCP_REQUEST_CONFLICT",
                            "requestRef đã dùng với nội dung khác");
                }
                return parseResponse(stored.get().response());
            }
            var response = mutation.get();
            String encoded;
            try {
                encoded = json.writeValueAsString(response);
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("Invalid MCP write response", e);
            }
            mcpWriteRequestRepository.insertRequest(actor.accountId(), operation, requestRef, fingerprint, encoded);
            // Return the same JSON types on initial creation and replay.
            return parseResponse(encoded);
        });
    }

    /** Removes retry records older than {@link #RETENTION}; called by the scheduled cleanup. */
    public int deleteExpiredRequests(Instant now) {
        return mcpWriteRequestRepository.deleteRequestsOlderThan(now.minus(RETENTION));
    }

    private Map<String, Object> parseResponse(String response) {
        try {
            return json.readValue(response, RESPONSE_TYPE);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Invalid stored MCP response", e);
        }
    }
}
