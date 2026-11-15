package edu.aiplatform.grading.eval;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.aiplatform.assignments.contract.AssignmentContent;
import edu.aiplatform.assignments.contract.BlockOrigin;
import edu.aiplatform.assignments.contract.BlockType;
import edu.aiplatform.assignments.contract.CriterionKind;
import edu.aiplatform.assignments.contract.DocumentBlock;
import edu.aiplatform.assignments.contract.DocumentModel;
import edu.aiplatform.assignments.contract.RubricCriterion;
import edu.aiplatform.assignments.contract.RubricItem;
import edu.aiplatform.assignments.contract.RubricView;
import edu.aiplatform.assignments.service.DocumentModelService;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Bộ dữ liệu đánh giá AI chấm: một đề (khung, rubric) và các bài làm có điểm kỳ vọng từng mục rubric. Dữ liệu viết
 * bằng khóa ngắn ({@code d-boundary}, {@code h-spec}); khóa được đổi thành UUID cố định để bản ghi live chạy lại được.
 */
record GradingEvalDataset(EvalAssignment assignment, List<EvalCase> cases) {

    static final String RESOURCE_ROOT = "ai-eval/grading/";
    static final String DEFAULT_DATASET = "badminton-booking";

    record EvalItem(String itemKey, UUID itemId, UUID rubricId, String partId, String text,
                    BigDecimal maxPoints) {
    }

    record EvalAssignment(String assignmentKey, String title, String labelSource, String labelNote,
                          AssignmentContent content, List<RubricView> rubrics,
                          Map<UUID, EvalItem> itemsById) {

        BigDecimal maxScore() {
            return itemsById.values().stream().map(EvalItem::maxPoints)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
    }

    record EvalCase(String caseKey, String description, String labelSource, List<String> tags,
                    boolean offTopic, boolean injection, DocumentModel document,
                    Map<UUID, BigDecimal> expectedScores, Set<UUID> absentItemIds) {

        BigDecimal expectedTotal() {
            return expectedScores.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        }
    }

    static GradingEvalDataset loadDataset(ObjectMapper json, String datasetKey) {
        String base = RESOURCE_ROOT + datasetKey + "/";
        JsonNode root = readJson(json, base + "assignment.json");
        String assignmentKey = root.path("assignmentKey").asText();
        UUID assignmentId = createStableId(assignmentKey, "assignment");
        var skeletonBlocks = new ArrayList<DocumentBlock>();
        var headingsByKey = new HashMap<String, DocumentBlock>();
        for (JsonNode heading : root.path("skeleton")) {
            String key = heading.path("key").asText();
            ObjectNode content = json.createObjectNode()
                    .put("style", heading.path("style").asText("H1"))
                    .put("text", heading.path("text").asText());
            var block = new DocumentBlock(createStableId(assignmentKey, key), BlockOrigin.TEACHER,
                    BlockType.HEADING, content);
            skeletonBlocks.add(block);
            headingsByKey.put(key, block);
        }
        var skeleton = new DocumentModel(List.copyOf(skeletonBlocks));
        var rubrics = new ArrayList<RubricView>();
        var itemsById = new LinkedHashMap<UUID, EvalItem>();
        var itemIdsByKey = new HashMap<String, UUID>();
        for (JsonNode rubricNode : root.path("rubrics")) {
            UUID rubricId = createStableId(assignmentKey, rubricNode.path("key").asText());
            String partId = requireHeading(headingsByKey, rubricNode.path("partHeading").asText())
                    .id().toString();
            var criteria = new ArrayList<RubricCriterion>();
            BigDecimal rubricMax = BigDecimal.ZERO;
            for (JsonNode criterionNode : rubricNode.path("criteria")) {
                var items = new ArrayList<RubricItem>();
                for (JsonNode itemNode : criterionNode.path("items")) {
                    String itemKey = itemNode.path("key").asText();
                    UUID itemId = createStableId(assignmentKey, itemKey);
                    BigDecimal points = itemNode.path("points").decimalValue();
                    String text = itemNode.path("text").asText();
                    items.add(new RubricItem(itemId, text, points));
                    itemsById.put(itemId, new EvalItem(itemKey, itemId, rubricId, partId, text, points));
                    itemIdsByKey.put(itemKey, itemId);
                    rubricMax = rubricMax.add(points);
                }
                criteria.add(new RubricCriterion(
                        createStableId(assignmentKey, criterionNode.path("key").asText()),
                        criterionNode.path("title").asText(), criterionNode.path("description").asText(""),
                        CriterionKind.valueOf(criterionNode.path("kind").asText("TEXT")), items));
            }
            rubrics.add(new RubricView(rubricId, assignmentId, partId, criteria, rubricMax));
        }
        var documentModelService = new DocumentModelService(json);
        var content = new AssignmentContent(assignmentId, 1, root.path("instructions").asText(), skeleton,
                documentModelService.computeParts(skeleton), Map.of(), List.copyOf(rubrics));
        var assignment = new EvalAssignment(assignmentKey, root.path("title").asText(),
                root.path("labelSource").asText("UNKNOWN"), root.path("labelNote").asText(""), content,
                List.copyOf(rubrics), Map.copyOf(itemsById));
        var cases = new ArrayList<EvalCase>();
        for (JsonNode casePath : root.path("cases")) {
            JsonNode caseNode = readJson(json, base + casePath.asText());
            cases.add(createCase(json, base, assignmentKey, caseNode, headingsByKey, itemIdsByKey));
        }
        return new GradingEvalDataset(assignment, List.copyOf(cases));
    }

    private static EvalCase createCase(ObjectMapper json, String base, String assignmentKey, JsonNode caseNode,
            Map<String, DocumentBlock> headingsByKey, Map<String, UUID> itemIdsByKey) {
        String caseKey = caseNode.path("caseKey").asText();
        var blocks = new ArrayList<DocumentBlock>();
        int index = 0;
        for (JsonNode blockNode : caseNode.path("blocks")) {
            if (blockNode.has("heading")) {
                blocks.add(requireHeading(headingsByKey, blockNode.path("heading").asText()));
                continue;
            }
            UUID blockId = createStableId(assignmentKey, caseKey + "#" + index++);
            BlockType type = BlockType.valueOf(blockNode.path("type").asText());
            blocks.add(new DocumentBlock(blockId, BlockOrigin.STUDENT, type,
                    createBlockContent(json, base, type, blockNode)));
        }
        var expected = new LinkedHashMap<UUID, BigDecimal>();
        caseNode.path("expected").properties().forEach(entry -> expected.put(
                requireItem(itemIdsByKey, entry.getKey()), entry.getValue().decimalValue()));
        var absent = new HashSet<UUID>();
        caseNode.path("absentItems").forEach(key -> absent.add(requireItem(itemIdsByKey, key.asText())));
        var tags = new ArrayList<String>();
        caseNode.path("tags").forEach(tag -> tags.add(tag.asText()));
        return new EvalCase(caseKey, caseNode.path("description").asText(),
                caseNode.path("labelSource").asText("UNKNOWN"), List.copyOf(tags),
                caseNode.path("offTopic").asBoolean(false), caseNode.path("injection").asBoolean(false),
                new DocumentModel(List.copyOf(blocks)), Map.copyOf(expected), Set.copyOf(absent));
    }

    private static JsonNode createBlockContent(ObjectMapper json, String base, BlockType type,
            JsonNode blockNode) {
        ObjectNode content = json.createObjectNode();
        switch (type) {
            case PARAGRAPH -> content.set("runs", createRuns(json, blockNode.path("text").asText()));
            case LIST -> {
                ArrayNode items = content.putArray("items");
                blockNode.path("items").forEach(item -> items.addObject().put("level", 0)
                        .set("runs", createRuns(json, item.asText())));
            }
            case TABLE -> {
                ArrayNode rows = content.putArray("rows");
                for (JsonNode row : blockNode.path("rows")) {
                    ArrayNode cells = rows.addArray();
                    row.forEach(cell -> cells.addObject().set("runs", createRuns(json, cell.asText())));
                }
            }
            case DIAGRAM -> content.put("diagramType", blockNode.path("diagramType").asText("USE_CASE"))
                    .put("xml", readText(base + blockNode.path("file").asText()));
            default -> throw new IllegalArgumentException("Unsupported eval block type: " + type);
        }
        return content;
    }

    private static ArrayNode createRuns(ObjectMapper json, String text) {
        ArrayNode runs = json.createArrayNode();
        runs.addObject().put("text", text);
        return runs;
    }

    private static DocumentBlock requireHeading(Map<String, DocumentBlock> headingsByKey, String key) {
        var heading = headingsByKey.get(key);
        if (heading == null) {
            throw new IllegalArgumentException("Unknown skeleton heading key: " + key);
        }
        return heading;
    }

    private static UUID requireItem(Map<String, UUID> itemIdsByKey, String key) {
        var itemId = itemIdsByKey.get(key);
        if (itemId == null) {
            throw new IllegalArgumentException("Unknown rubric item key: " + key);
        }
        return itemId;
    }

    static UUID createStableId(String assignmentKey, String key) {
        return UUID.nameUUIDFromBytes(("ai-eval:" + assignmentKey + ":" + key).getBytes(StandardCharsets.UTF_8));
    }

    private static JsonNode readJson(ObjectMapper json, String resource) {
        try {
            return json.readTree(readText(resource));
        } catch (IOException ex) {
            throw new UncheckedIOException("Cannot parse " + resource, ex);
        }
    }

    private static String readText(String resource) {
        try (InputStream input = GradingEvalDataset.class.getClassLoader().getResourceAsStream(resource)) {
            if (input == null) {
                throw new IllegalArgumentException("Missing eval resource: " + resource);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new UncheckedIOException("Cannot read " + resource, ex);
        }
    }
}
