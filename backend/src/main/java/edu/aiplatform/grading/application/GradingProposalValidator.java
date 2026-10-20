package edu.aiplatform.grading.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.assignments.contract.RubricView;
import edu.aiplatform.shared.error.ApiException;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class GradingProposalValidator {
    private static final int MAX_FEEDBACK_LENGTH = 10000;
    private static final int MAX_PAYLOAD_LENGTH = 262144;
    private static final Set<String> PROPOSAL_FIELDS = Set.of("checks", "score", "feedback", "sections");
    private final ObjectMapper json;

    public GradingProposalValidator(ObjectMapper json) {
        this.json = json;
    }

    public record Validated(Map<UUID, Set<UUID>> checks, BigDecimal score, String feedback, JsonNode sections) {
    }

    public Validated validate(JsonNode payload, List<RubricView> rubrics) {
        try {
            requireProposalObject(payload);
            var checks = readChecks(payload);
            var score = GradingJson.score(rubrics, checks);
            requireMatchingScore(payload.get("score"), score);
            var feedback = readFeedback(payload.get("feedback"));
            var sections = payload.get("sections");
            validateSections(sections, rubrics);
            return new Validated(checks, score, feedback, sections);
        } catch (RuntimeException ex) {
            throw ApiException.badRequest("INVALID_AI_PROPOSAL", "AI proposal does not match the rubric schema");
        }
    }

    private void requireProposalObject(JsonNode payload) {
        if (payload == null || !payload.isObject()) {
            throw new IllegalArgumentException();
        }
        payload.fieldNames().forEachRemaining(field -> {
            if (!PROPOSAL_FIELDS.contains(field)) {
                throw new IllegalArgumentException();
            }
        });
    }

    private Map<UUID, Set<UUID>> readChecks(JsonNode payload) {
        var node = payload.get("checks");
        if (node == null || !node.isObject() || !payload.hasNonNull("score")) {
            throw new IllegalArgumentException();
        }
        Map<UUID, Set<UUID>> checks = new LinkedHashMap<>();
        var fields = node.fields();
        while (fields.hasNext()) {
            var field = fields.next();
            if (!field.getValue().isArray()) {
                throw new IllegalArgumentException();
            }
            Set<UUID> selected = new HashSet<>();
            for (var item : field.getValue()) {
                if (!item.isTextual() || !selected.add(UUID.fromString(item.textValue()))) {
                    throw new IllegalArgumentException();
                }
            }
            checks.put(UUID.fromString(field.getKey()), selected);
        }
        return checks;
    }

    private void requireMatchingScore(JsonNode scoreNode, BigDecimal computedScore) {
        var suppliedScore = new BigDecimal(scoreNode.asText());
        if (suppliedScore.scale() > 2 || computedScore.compareTo(suppliedScore) != 0) {
            throw new IllegalArgumentException();
        }
    }

    private String readFeedback(JsonNode node) {
        if (node != null && !node.isNull() && !node.isTextual()) {
            throw new IllegalArgumentException();
        }
        String feedback = node == null || node.isNull() ? null : node.textValue();
        if (feedback != null && feedback.length() > MAX_FEEDBACK_LENGTH) {
            throw new IllegalArgumentException();
        }
        return feedback;
    }

    private void validateSections(JsonNode sections, List<RubricView> rubrics) {
        if (sections == null) {
            return;
        }
        if (!sections.isArray()) {
            throw new IllegalArgumentException();
        }
        Set<String> knownPartIds = new HashSet<>();
        Set<String> seenPartIds = new HashSet<>();
        rubrics.forEach(rubric -> knownPartIds.add(rubric.partId()));
        for (var section : sections) {
            if (!section.isObject() || !section.path("partId").isTextual()
                    || !knownPartIds.contains(section.path("partId").asText())
                    || !seenPartIds.add(section.path("partId").asText()) || !section.path("feedback").isTextual()
                    || section.path("feedback").asText().length() > MAX_FEEDBACK_LENGTH) {
                throw new IllegalArgumentException();
            }
        }
    }

    public JsonNode parse(String value) {
        if (value == null || value.length() > MAX_PAYLOAD_LENGTH)
            throw ApiException.badRequest("INVALID_AI_PROPOSAL", "AI proposal too large");
        try {
            return json.reader().with(com.fasterxml.jackson.core.JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
                    .readTree(value);
        } catch (Exception ex) {
            throw ApiException.badRequest("INVALID_AI_PROPOSAL", "Invalid AI proposal JSON");
        }
    }
}
