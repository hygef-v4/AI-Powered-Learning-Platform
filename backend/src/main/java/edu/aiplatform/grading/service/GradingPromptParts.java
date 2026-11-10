package edu.aiplatform.grading.service;

import com.fasterxml.jackson.databind.JsonNode;
import edu.aiplatform.assignments.contract.BlockOrigin;
import edu.aiplatform.assignments.contract.BlockType;
import edu.aiplatform.assignments.contract.DocumentBlock;
import edu.aiplatform.assignments.contract.DocumentModel;
import edu.aiplatform.assignments.contract.DocumentPart;
import edu.aiplatform.assignments.contract.IDocumentModel;
import edu.aiplatform.assignments.contract.RubricView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Helper methods for partitioning submission documents and identifying empty parts for AI grading. */
final class GradingPromptParts {
    private GradingPromptParts() {
    }

    static DocumentModel filterDocumentForActiveParts(IDocumentModel documentModel,
            DocumentModel document, Set<String> emptyPartIds) {
        if (document == null || document.blocks() == null || emptyPartIds.isEmpty()) {
            return document != null ? document : new DocumentModel(List.of());
        }
        List<DocumentPart> parts = computeParts(documentModel, document);
        boolean whole = parts.size() == 1 && parts.get(0).headingBlockId() == null;
        Set<UUID> leaves = collectLeafHeadingIds(parts);
        List<DocumentBlock> filtered = new ArrayList<>();
        String currentPartId = whole ? parts.get(0).partId() : null;
        for (DocumentBlock block : document.blocks()) {
            currentPartId = resolveCurrentPartId(block, whole, leaves, currentPartId);
            if (currentPartId == null || !emptyPartIds.contains(currentPartId)) {
                filtered.add(block);
            }
        }
        return new DocumentModel(List.copyOf(filtered));
    }

    static List<RubricView> findActiveRubrics(IDocumentModel documentModel,
            DocumentModel document, DocumentModel skeleton, List<RubricView> rubrics) {
        List<DocumentPart> parts = computeParts(documentModel, skeleton != null ? skeleton : document);
        Set<String> emptyPartIds = findEmptyPartIds(documentModel, document, skeleton, parts);
        boolean allPartsEmpty = parts.isEmpty()
                || parts.stream().allMatch(p -> emptyPartIds.contains(p.partId()));
        if (allPartsEmpty) {
            return List.of();
        }
        return rubrics.stream()
                .filter(r -> r.partId() == null || !emptyPartIds.contains(r.partId()))
                .toList();
    }

    static List<RubricView> findEmptyRubrics(IDocumentModel documentModel,
            DocumentModel document, DocumentModel skeleton, List<RubricView> rubrics) {
        List<DocumentPart> parts = computeParts(documentModel, skeleton != null ? skeleton : document);
        Set<String> emptyPartIds = findEmptyPartIds(documentModel, document, skeleton, parts);
        boolean allPartsEmpty = parts.isEmpty()
                || parts.stream().allMatch(p -> emptyPartIds.contains(p.partId()));
        if (allPartsEmpty) {
            return rubrics;
        }
        return rubrics.stream()
                .filter(r -> r.partId() != null && emptyPartIds.contains(r.partId()))
                .toList();
    }

    static Set<String> findEmptyPartIds(IDocumentModel documentModel,
            DocumentModel document, DocumentModel skeleton) {
        List<DocumentPart> parts = computeParts(documentModel, skeleton != null ? skeleton : document);
        return findEmptyPartIds(documentModel, document, skeleton, parts);
    }

    static Set<String> findEmptyPartIds(IDocumentModel documentModel, DocumentModel document, DocumentModel skeleton,
            List<DocumentPart> parts) {
        if (parts == null || parts.isEmpty()) {
            return Set.of();
        }
        Map<String, List<DocumentBlock>> blocksByPart = groupBlocksByPart(document, parts);
        Set<String> emptyParts = new HashSet<>();
        for (var part : parts) {
            List<DocumentBlock> partBlocks = blocksByPart.getOrDefault(part.partId(), List.of());
            if (!hasStudentWork(documentModel, partBlocks, skeleton)) {
                emptyParts.add(part.partId());
            }
        }
        return emptyParts;
    }

    private static List<DocumentPart> computeParts(IDocumentModel documentModel, DocumentModel doc) {
        if (documentModel == null || doc == null) {
            return List.of();
        }
        return documentModel.computeParts(doc);
    }

    private static Map<String, List<DocumentBlock>> groupBlocksByPart(DocumentModel document,
            List<DocumentPart> parts) {
        Map<String, List<DocumentBlock>> blocksByPart = new LinkedHashMap<>();
        for (var part : parts) {
            blocksByPart.put(part.partId(), new ArrayList<>());
        }
        if (document == null || document.blocks() == null) {
            return blocksByPart;
        }
        boolean whole = parts.size() == 1 && parts.get(0).headingBlockId() == null;
        String currentPartId = whole ? parts.get(0).partId() : null;
        Set<UUID> leaves = collectLeafHeadingIds(parts);
        for (DocumentBlock block : document.blocks()) {
            currentPartId = resolveCurrentPartId(block, whole, leaves, currentPartId);
            if (currentPartId != null && blocksByPart.containsKey(currentPartId)) {
                blocksByPart.get(currentPartId).add(block);
            }
        }
        return blocksByPart;
    }

    private static Set<UUID> collectLeafHeadingIds(List<DocumentPart> parts) {
        Set<UUID> leaves = new HashSet<>();
        for (DocumentPart part : parts) {
            if (part.headingBlockId() != null) {
                leaves.add(part.headingBlockId());
            }
        }
        return leaves;
    }

    private static String resolveCurrentPartId(DocumentBlock block, boolean whole,
            Set<UUID> leaves, String currentPartId) {
        if (!whole && headingLevel(block) > 0) {
            return leaves.contains(block.id()) ? block.id().toString() : null;
        }
        if (!whole && under(block) >= 0) {
            return null;
        }
        return currentPartId;
    }

    private static boolean hasStudentWork(IDocumentModel documentModel, List<DocumentBlock> blocks, DocumentModel skeleton) {
        if (blocks == null || blocks.isEmpty()) {
            return false;
        }
        Map<UUID, String> skeletonXml = new HashMap<>();
        Map<UUID, JsonNode> skeletonRows = new HashMap<>();
        indexSkeletonBlocks(skeleton, skeletonXml, skeletonRows);
        for (DocumentBlock block : blocks) {
            if (block.origin() == BlockOrigin.TEACHER
                    && hasModifiedTeacherBlock(documentModel, block, skeletonXml, skeletonRows)) {
                return true;
            }
            if (block.origin() == BlockOrigin.STUDENT && hasStudentBlockContent(documentModel, block)) {
                return true;
            }
        }
        return false;
    }

    private static void indexSkeletonBlocks(DocumentModel skeleton,
            Map<UUID, String> skeletonXml, Map<UUID, JsonNode> skeletonRows) {
        if (skeleton == null || skeleton.blocks() == null) {
            return;
        }
        for (DocumentBlock block : skeleton.blocks()) {
            if (block.type() == BlockType.DIAGRAM) {
                skeletonXml.put(block.id(), block.content().path("xml").asText(""));
            } else if (block.type() == BlockType.TABLE) {
                skeletonRows.put(block.id(), block.content().path("rows"));
            }
        }
    }

    private static boolean hasModifiedTeacherBlock(IDocumentModel documentModel, DocumentBlock block,
            Map<UUID, String> skeletonXml, Map<UUID, JsonNode> skeletonRows) {
        if (block.type() == BlockType.DIAGRAM) {
            String xml = block.content().path("xml").asText("");
            String orig = skeletonXml.getOrDefault(block.id(), "");
            return documentModel != null && documentModel.hasDiagramContent(xml) && !xml.equals(orig);
        }
        if (block.type() == BlockType.TABLE) {
            JsonNode origRows = skeletonRows.get(block.id());
            JsonNode currentRows = block.content().path("rows");
            return (origRows != null && !currentRows.equals(origRows))
                    || (origRows == null && hasTableText(currentRows));
        }
        return false;
    }

    private static boolean hasStudentBlockContent(IDocumentModel documentModel, DocumentBlock block) {
        if (block.type() == BlockType.DIAGRAM) {
            return documentModel != null && documentModel.hasDiagramContent(block.content().path("xml").asText(""));
        }
        if (block.type() == BlockType.IMAGE) {
            return block.content().path("hasFile").asBoolean(false)
                    || !block.content().path("fileRef").asText("").isBlank();
        }
        return hasNonEmptyText(block);
    }

    private static int headingLevel(DocumentBlock block) {
        if (block.type() != BlockType.HEADING || block.content() == null) {
            return 0;
        }
        String style = block.content().path("style").asText("");
        if (style.length() == 2 && style.charAt(0) == 'H'
                && style.charAt(1) >= '1' && style.charAt(1) <= '6') {
            return style.charAt(1) - '0';
        }
        return 0;
    }

    private static boolean hasNonEmptyText(DocumentBlock block) {
        JsonNode content = block.content();
        if (content == null) {
            return false;
        }
        return switch (block.type()) {
            case HEADING -> !isBlankText(content.path("text").asText(""));
            case PARAGRAPH -> !isBlankText(runs(content.path("runs")));
            case LIST -> {
                for (JsonNode item : content.path("items")) {
                    if (!isBlankText(runs(item.path("runs")))) {
                        yield true;
                    }
                }
                yield false;
            }
            case TABLE -> hasTableText(content.path("rows"));
            default -> false;
        };
    }

    private static boolean hasTableText(JsonNode rows) {
        if (rows == null || !rows.isArray()) {
            return false;
        }
        for (JsonNode row : rows) {
            for (JsonNode cell : row) {
                if (!isBlankText(runs(cell.path("runs")))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isBlankText(String text) {
        return text == null || text.isBlank();
    }

    private static String runs(JsonNode runs) {
        if (runs == null || !runs.isArray()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (JsonNode run : runs) {
            sb.append(run.path("text").asText(""));
        }
        return sb.toString();
    }

    private static int under(DocumentBlock block) {
        if (block.type() == BlockType.HEADING || block.content() == null) {
            return -1;
        }
        JsonNode under = block.content().get("under");
        return under != null && under.canConvertToInt() ? under.asInt() : -1;
    }
}
