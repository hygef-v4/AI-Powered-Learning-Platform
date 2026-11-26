package edu.aiplatform.grading.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.ai.contract.*;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.shared.error.ApiException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.*;

/** No batch-wide transaction: each proposal uses recordMcp's own transaction and idempotency checks. */
@Component
public class GradingMcpBatchService implements IMcpToolProvider {
    static final int BATCH_CONTENT_BYTES = 180 * 1024;
    static final int ENVELOPE_BYTES = 240 * 1024;
    /** Room for an outcome's index, ids and keys around its result. */
    static final int ITEM_OVERHEAD_BYTES = 512;
    private final GradingMcpQueryService gradingMcpQueryService;
    private final GradingMcpTools gradingMcpTools;
    private final ObjectMapper objectMapper;

    /** Creates the batch tool provider with query, write, and JSON services. */
    public GradingMcpBatchService(GradingMcpQueryService gradingMcpQueryService,
            GradingMcpTools gradingMcpTools, ObjectMapper objectMapper) {
        this.gradingMcpQueryService = gradingMcpQueryService;
        this.gradingMcpTools = gradingMcpTools;
        this.objectMapper = objectMapper;
    }

    /** Defines the MCP tools for reading and recording grading batches. */
    @Override
    public List<McpToolDefinition> tools() {
        try {
            String item = gradingMcpTools.tools().get(0).parameterSchema();
            return List.of(new McpToolDefinition("read_grading_batch",
                    "Đọc tối đa 20 snapshot, lỗi từng item độc lập; dùng continuation để đọc đủ.",
                    gradeBatch(McpSchemas.schema(GradingMcpQueryService.CONTEXT_INPUT,
                            "evaluationId")),
                    McpToolKind.READ, McpSchemas.STAFF),
                    new McpToolDefinition("record_grading_proposals_batch",
                            "Lưu tối đa 20 đề xuất độc lập; không lưu hay công bố điểm chính thức.",
                            gradeBatch(item), McpToolKind.WRITE, McpSchemas.STAFF));
        } catch (RuntimeException e) {
            throw new IllegalStateException("Invalid batch schema", e);
        }
    }

    /** Builds a bounded batch schema around the supplied item schema. */
    private static String gradeBatch(String item) {
        return McpSchemas
                .schema("\"items\":{\"type\":\"array\",\"minItems\":1,\"maxItems\":20,\"items\":"
                        + item + "}", "items");
    }

    /** Processes batch items independently and returns each item's outcome. */
    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public McpToolResult call(Actor actor, String name, Map<String, Object> args) {
        // Step 1: Validate teacher authentication and MCP batch tool name
        McpInput.requireTeaching(actor);
        boolean write = "record_grading_proposals_batch".equals(name);
        if (!write && !"read_grading_batch".equals(name))
            throw McpInput.createInvalidArguments();
        if (args == null || !(args.get("items") instanceof List<?> items) || items.isEmpty()
                || items.size() > 20)
            throw McpInput.createInvalidArguments();

        // Step 2: Iterate and process each batch item independently (max 20 items)
        List<Map<String, Object>> results = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (int n = 0; n < items.size(); n++) {
            Map<String, Object> outcome = new LinkedHashMap<>();
            outcome.put("index", n);
            try {
                if (!(items.get(n) instanceof Map<?, ?> raw))
                    throw McpInput.createInvalidArguments();
                var input = objectMapper.convertValue(raw,
                        new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {
                        });
                String key = write ? Objects.toString(input.get("requestRef"), "")
                        : Objects.toString(input.get("evaluationId"), "") + ":"
                                + Objects.toString(input.get("offset"), "0");
                if (!seen.add(key))
                    throw ApiException.createBadRequest("DUPLICATE_ITEM", "Repeated item in batch");
                if (input.containsKey("evaluationId"))
                    outcome.put("evaluationId", input.get("evaluationId"));
                if (input.containsKey("requestRef"))
                    outcome.put("requestRef", input.get("requestRef"));

                // Step 3: Dispatch single item to read context or record proposal; a read page fits what is left
                var result = write
                        ? gradingMcpTools.call(actor, "record_grading_proposal", input).content()
                        : loadContextWithinBudget(actor, input, results);
                outcome.put("outcome", "OK");
                outcome.put("result", result);

                // Step 4: Ensure output stays within safe MCP response payload limits (180KB / 240KB)
                if (!isWithinBatchLimit(results, outcome)) {
                    outcome.remove("result");
                    outcome.put("outcome", "ERROR");
                    outcome.put("code", "MCP_BATCH_RESULT_LIMIT");
                } else if (!write)
                    gradingMcpQueryService.recordRead(actor, (Map<String, Object>) result);
            } catch (ApiException e) {
                // Item level failure isolation
                outcome.remove("result");
                outcome.put("outcome", "ERROR");
                outcome.put("code", e.readCode());
            } catch (RuntimeException e) {
                outcome.remove("result");
                outcome.put("outcome", "ERROR");
                outcome.put("code", "MCP_ITEM_UNAVAILABLE");
            }
            results.add(outcome);
        }
        return new McpToolResult(false, Map.of("items", results, "partial",
                results.stream().anyMatch(r -> !"OK".equals(r.get("outcome")))));
    }

    /** Loads a context page sized to the batch bytes still free, so later items shrink instead of failing. */
    private Map<String, Object> loadContextWithinBudget(Actor actor, Map<String, Object> input,
            List<Map<String, Object>> results) {
        String text = writeJson(Map.of("items", results, "partial", true));
        int raw = text.getBytes(StandardCharsets.UTF_8).length;
        int escaped = measureEnvelopeBytes(text);
        // Escaping the page into the envelope text inflates it about as much as the items so far; the 240 KB
        // check in isWithinBatchLimit stays the backstop when a page escapes worse than this estimate.
        double inflation = (double) (escaped - measureEnvelopeBytes("")) / raw;
        int left = Math.min(BATCH_CONTENT_BYTES - raw, (int) ((ENVELOPE_BYTES - escaped) / inflation))
                - ITEM_OVERHEAD_BYTES;
        int budget = Math.min(GradingMcpQueryService.CONTEXT_BYTES, left);
        if (budget <= 0)
            throw createBatchLimitError();
        try {
            return gradingMcpQueryService.loadGradingContext(actor, input, false, budget);
        } catch (ApiException e) {
            // A page that fits alone but not in the remaining batch space is retryable, not too large.
            if (budget < GradingMcpQueryService.CONTEXT_BYTES && "MCP_CONTEXT_TOO_LARGE".equals(e.readCode()))
                throw createBatchLimitError();
            throw e;
        }
    }

    // Báo giới hạn dung lượng của lượt đọc nhóm để client dời mục sang lần gọi sau.
    private static ApiException createBatchLimitError() {
        return ApiException.createBadRequest("MCP_BATCH_RESULT_LIMIT",
                "Batch result is full; read this item in a later call");
    }

    /** Checks whether the next outcome keeps both the result and MCP envelope within size limits. */
    private boolean isWithinBatchLimit(List<Map<String, Object>> results,
            Map<String, Object> next) {
        var candidate = new ArrayList<>(results);
        candidate.add(next);
        String text = writeJson(Map.of("items", candidate, "partial", true));
        // Account for text escaping in the MCP envelope; reserve room for remaining bounded outcomes.
        return text.getBytes(StandardCharsets.UTF_8).length <= BATCH_CONTENT_BYTES
                && measureEnvelopeBytes(text) <= ENVELOPE_BYTES;
    }

    /** Measures the MCP tool-result envelope that carries {@code text} as one escaped string. */
    private int measureEnvelopeBytes(String text) {
        return writeJson(Map.of("content", List.of(Map.of("type", "text", "text", text)), "isError", false))
                .getBytes(StandardCharsets.UTF_8).length;
    }

    /** Serializes a batch result value to JSON. */
    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}
