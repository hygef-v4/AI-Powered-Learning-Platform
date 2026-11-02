package edu.aiplatform.grading.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.assignments.contract.DocumentModel;
import edu.aiplatform.assignments.contract.RubricItem;
import edu.aiplatform.assignments.contract.RubricView;
import edu.aiplatform.grading.domain.GradingFeedback;
import edu.aiplatform.shared.error.ApiException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class GradingFeedbackValidator {
    public static final int MAX_TOTAL_JSON_CHARS = 60_000;
    public static final int MAX_SUMMARY_ITEMS = 5;
    public static final int MAX_SUMMARY_ENTRY_LENGTH = 500;
    public static final int MAX_ITEM_TEXT_LENGTH = 1_000;
    public static final int MAX_REVIEW_REASON_LENGTH = 500;
    public static final int MAX_BLOCK_ID_LENGTH = 200;

    private static final Set<String> ROOT_FIELDS = Set.of("schemaVersion", "summary", "items");
    private static final Set<String> SUMMARY_FIELDS = Set.of("strengths", "improvements", "nextSteps");
    private static final Set<String> ITEM_FIELDS = Set.of("rubricId", "itemId", "evidence", "reason", "suggestion",
            "evidenceBlockId", "requiresReview", "reviewReason");

    private final ObjectMapper json;

    public GradingFeedbackValidator(ObjectMapper json) {
        this.json = json;
    }

    public static Set<String> validBlockIds(List<RubricView> rubrics, DocumentModel document) {
        Set<String> ids = new HashSet<>();
        if (rubrics != null) {
            for (var rubric : rubrics) {
                if (rubric.partId() != null && !rubric.partId().isBlank()) {
                    ids.add(rubric.partId());
                }
            }
        }
        if (document != null && document.blocks() != null) {
            for (var block : document.blocks()) {
                if (block != null && block.id() != null) {
                    ids.add(block.id().toString());
                }
            }
        }
        return ids;
    }

    public GradingFeedback validateNode(JsonNode node, List<RubricView> rubrics, Set<String> validBlockIds,
            boolean requireAllItems) {
        if (node == null || node.isNull()) {
            if (requireAllItems) {
                throw invalid("Missing structuredFeedback");
            }
            return null;
        }
        if (!node.isObject()) {
            throw invalid("structuredFeedback must be a JSON object");
        }
        String raw = GradingJson.write(json, node);
        if (raw.length() > MAX_TOTAL_JSON_CHARS) {
            throw invalid("structuredFeedback exceeds maximum length of 60000 characters");
        }
        requireFields(node, ROOT_FIELDS, Set.of("schemaVersion", "summary", "items"));
        if (!node.get("schemaVersion").isIntegralNumber() || node.get("schemaVersion").asInt() != 1) {
            throw invalid("Unsupported schemaVersion");
        }
        var summaryNode = node.get("summary");
        if (summaryNode == null || !summaryNode.isObject()) {
            throw invalid("summary must be an object");
        }
        requireFields(summaryNode, SUMMARY_FIELDS, SUMMARY_FIELDS);
        var summary = new GradingFeedback.Summary(
                readSummaryList(summaryNode.get("strengths"), "strengths"),
                readSummaryList(summaryNode.get("improvements"), "improvements"),
                readSummaryList(summaryNode.get("nextSteps"), "nextSteps"));

        var itemsNode = node.get("items");
        if (itemsNode == null || !itemsNode.isArray()) {
            throw invalid("items must be an array");
        }
        List<GradingFeedback.Item> items = new ArrayList<>();
        for (var itemNode : itemsNode) {
            if (itemNode == null || !itemNode.isObject()) {
                throw invalid("Each item in structuredFeedback.items must be an object");
            }
            requireFields(itemNode, ITEM_FIELDS, Set.of("rubricId", "itemId", "reason", "requiresReview"));
            UUID rubricId = readUuid(itemNode.get("rubricId"), "rubricId");
            UUID itemId = readUuid(itemNode.get("itemId"), "itemId");
            String evidence = readOptionalText(itemNode.get("evidence"), MAX_ITEM_TEXT_LENGTH, "evidence");
            String reason = readRequiredText(itemNode.get("reason"), MAX_ITEM_TEXT_LENGTH, "reason");
            String suggestion = readOptionalText(itemNode.get("suggestion"), MAX_ITEM_TEXT_LENGTH, "suggestion");
            String evidenceBlockId = readOptionalText(itemNode.get("evidenceBlockId"), MAX_BLOCK_ID_LENGTH,
                    "evidenceBlockId");
            var reqReviewNode = itemNode.get("requiresReview");
            if (reqReviewNode == null || !reqReviewNode.isBoolean()) {
                throw invalid("requiresReview must be a boolean");
            }
            boolean requiresReview = reqReviewNode.booleanValue();
            String reviewReason = readOptionalText(itemNode.get("reviewReason"), MAX_REVIEW_REASON_LENGTH,
                    "reviewReason");
            items.add(new GradingFeedback.Item(rubricId, itemId, evidence, reason, suggestion, evidenceBlockId,
                    requiresReview, reviewReason));
        }

        var feedback = new GradingFeedback(1, summary, List.copyOf(items));
        return validate(feedback, rubrics, validBlockIds, requireAllItems);
    }

    public GradingFeedback validate(GradingFeedback feedback, List<RubricView> rubrics, Set<String> validBlockIds,
            boolean requireAllItems) {
        if (feedback == null) {
            if (requireAllItems) {
                throw invalid("Missing structuredFeedback");
            }
            return null;
        }
        String serialized = GradingJson.write(json, feedback);
        if (serialized.length() > MAX_TOTAL_JSON_CHARS) {
            throw invalid("structuredFeedback exceeds maximum length of 60000 characters");
        }
        if (feedback.schemaVersion() != 1) {
            throw invalid("Unsupported structuredFeedback schemaVersion");
        }
        if (feedback.summary() == null) {
            throw invalid("Missing structuredFeedback summary");
        }
        validateSummaryList(feedback.summary().strengths(), "strengths");
        validateSummaryList(feedback.summary().improvements(), "improvements");
        validateSummaryList(feedback.summary().nextSteps(), "nextSteps");

        if (feedback.items() == null) {
            throw invalid("Missing structuredFeedback items");
        }
        Map<UUID, Map<UUID, RubricItem>> knownItems = indexRubrics(rubrics);
        int totalRubricItems = knownItems.values().stream().mapToInt(Map::size).sum();
        if (feedback.items().size() > totalRubricItems) {
            throw invalid("structuredFeedback item count exceeds rubric item count");
        }
        if (requireAllItems && feedback.items().size() != totalRubricItems) {
            throw invalid("AI structuredFeedback must provide feedback for every rubric item");
        }

        Set<String> allowedBlocks = validBlockIds != null ? validBlockIds : validBlockIds(rubrics, null);
        Set<UUID> seenItems = new HashSet<>();
        for (var item : feedback.items()) {
            if (item == null || item.rubricId() == null || item.itemId() == null) {
                throw invalid("structuredFeedback item requires rubricId and itemId");
            }
            var rubricMap = knownItems.get(item.rubricId());
            if (rubricMap == null || !rubricMap.containsKey(item.itemId())) {
                throw invalid("Unknown rubricId or itemId in structuredFeedback");
            }
            if (!seenItems.add(item.itemId())) {
                throw invalid("Duplicate rubric item in structuredFeedback");
            }
            validateRequiredText(item.reason(), MAX_ITEM_TEXT_LENGTH, "reason");
            validateOptionalText(item.evidence(), MAX_ITEM_TEXT_LENGTH, "evidence");
            validateOptionalText(item.suggestion(), MAX_ITEM_TEXT_LENGTH, "suggestion");
            validateOptionalText(item.reviewReason(), MAX_REVIEW_REASON_LENGTH, "reviewReason");

            boolean hasEvidence = item.evidence() != null && !item.evidence().isBlank();
            boolean hasReviewReason = item.reviewReason() != null && !item.reviewReason().isBlank();
            if (item.requiresReview() && !hasReviewReason) {
                throw invalid("requiresReview requires a non-blank reviewReason");
            }
            if (!hasEvidence && !(item.requiresReview() && hasReviewReason)) {
                throw invalid("Each item requires observed evidence or an explicit review flag with reason");
            }
            if (item.evidenceBlockId() != null) {
                if (item.evidenceBlockId().isBlank() || item.evidenceBlockId().length() > MAX_BLOCK_ID_LENGTH) {
                    throw invalid("Invalid evidenceBlockId");
                }
                if (!allowedBlocks.contains(item.evidenceBlockId())) {
                    throw invalid("Unknown evidenceBlockId: " + item.evidenceBlockId());
                }
            }
        }
        return feedback.copy();
    }

    private static Map<UUID, Map<UUID, RubricItem>> indexRubrics(List<RubricView> rubrics) {
        Map<UUID, Map<UUID, RubricItem>> map = new HashMap<>();
        if (rubrics == null) {
            return map;
        }
        for (var rubric : rubrics) {
            Map<UUID, RubricItem> items = new HashMap<>();
            if (rubric.criteria() != null) {
                for (var criterion : rubric.criteria()) {
                    if (criterion.items() != null) {
                        for (var item : criterion.items()) {
                            items.put(item.itemId(), item);
                        }
                    }
                }
            }
            map.put(rubric.rubricId(), items);
        }
        return map;
    }

    private static void requireFields(JsonNode node, Set<String> allowed, Set<String> required) {
        var names = new HashSet<String>();
        node.fieldNames().forEachRemaining(field -> {
            if (!allowed.contains(field)) {
                throw invalid("Unknown field in structuredFeedback: " + field);
            }
            names.add(field);
        });
        if (!names.containsAll(required)) {
            throw invalid("Missing required field in structuredFeedback");
        }
    }

    private static List<String> readSummaryList(JsonNode node, String field) {
        if (node == null || !node.isArray() || node.size() > MAX_SUMMARY_ITEMS) {
            throw invalid("Invalid summary list: " + field);
        }
        List<String> values = new ArrayList<>(node.size());
        for (var entry : node) {
            if (entry == null || !entry.isTextual()) {
                throw invalid("Summary entry must be a string in " + field);
            }
            String text = entry.textValue();
            if (text == null || text.isBlank() || text.length() > MAX_SUMMARY_ENTRY_LENGTH) {
                throw invalid("Summary entry out of bounds in " + field);
            }
            values.add(text);
        }
        return List.copyOf(values);
    }

    private static void validateSummaryList(List<String> list, String field) {
        if (list == null || list.size() > MAX_SUMMARY_ITEMS) {
            throw invalid("Invalid summary list: " + field);
        }
        for (var entry : list) {
            if (entry == null || entry.isBlank() || entry.length() > MAX_SUMMARY_ENTRY_LENGTH) {
                throw invalid("Summary entry out of bounds in " + field);
            }
        }
    }

    private static UUID readUuid(JsonNode node, String field) {
        if (node == null || !node.isTextual()) {
            throw invalid("Expected UUID string for " + field);
        }
        try {
            return UUID.fromString(node.textValue());
        } catch (IllegalArgumentException ex) {
            throw invalid("Invalid UUID for " + field);
        }
    }

    private static String readRequiredText(JsonNode node, int maxLength, String field) {
        if (node == null || !node.isTextual()) {
            throw invalid("Expected string for " + field);
        }
        String text = node.textValue();
        validateRequiredText(text, maxLength, field);
        return text;
    }

    private static String readOptionalText(JsonNode node, int maxLength, String field) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (!node.isTextual()) {
            throw invalid("Expected string or null for " + field);
        }
        String text = node.textValue();
        validateOptionalText(text, maxLength, field);
        return text;
    }

    private static void validateRequiredText(String value, int maxLength, String field) {
        if (value == null || value.isBlank() || value.length() > maxLength) {
            throw invalid("Field " + field + " is required and must be at most " + maxLength + " characters");
        }
    }

    private static void validateOptionalText(String value, int maxLength, String field) {
        if (value != null && value.length() > maxLength) {
            throw invalid("Field " + field + " exceeds " + maxLength + " characters");
        }
    }

    private static ApiException invalid(String detail) {
        return ApiException.badRequest("INVALID_STRUCTURED_FEEDBACK", detail);
    }
}
