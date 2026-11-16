package edu.aiplatform.grading.eval;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.ai.service.IProviderGateway;
import edu.aiplatform.ai.service.JsonSchemaChecker;
import edu.aiplatform.assignments.contract.BlockType;
import edu.aiplatform.assignments.contract.DocumentBlock;
import edu.aiplatform.assignments.contract.DocumentModel;
import edu.aiplatform.assignments.contract.DocumentPart;
import edu.aiplatform.assignments.service.DocumentModelService;
import edu.aiplatform.grading.eval.GradingEvalDataset.EvalAssignment;
import edu.aiplatform.grading.eval.GradingEvalDataset.EvalCase;
import edu.aiplatform.grading.eval.GradingEvalDataset.EvalItem;
import edu.aiplatform.grading.model.Evaluation;
import edu.aiplatform.grading.model.GradingFeedback;
import edu.aiplatform.grading.service.GradingFeedbackValidator;
import edu.aiplatform.grading.service.GradingPromptBuilder;
import edu.aiplatform.grading.service.GradingProposalValidator;
import edu.aiplatform.shared.error.ApiException;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Chạy một bài qua đúng các bước chấm AI của app: chọn rubric có bài làm, dựng prompt và schema provider, kiểm schema
 * output, gộp điểm 0 cho phần trống, chuẩn hóa rồi validate proposal. Không chạm database, quota hay kết nối AI.
 */
final class GradingEvalPipeline {

    static final Duration PROVIDER_TIMEOUT = Duration.ofSeconds(120);
    private static final Pattern QUOTE = Pattern.compile("[\"“]([^\"“”]{4,300})[\"”]");

    /** Nhận request không có model/key; bên gọi live tự gắn thông tin kết nối. */
    @FunctionalInterface
    interface ProviderCall {
        IProviderGateway.Response callProvider(String caseKey, int runIndex, IProviderGateway.Request request);
    }

    enum RunStatus {
        VALID, INVALID_OUTPUT, PROVIDER_ERROR
    }

    /** {@code gradedByAi=false}: mục thuộc phần trống, app tự cho 0 mà không gửi AI. */
    record ItemOutcome(UUID itemId, String itemKey, String itemText, String partId, boolean gradedByAi,
                       BigDecimal maxPoints, BigDecimal expected, BigDecimal awarded, boolean absent, String evidence,
                       String evidenceBlockId, String evidencePartId, boolean requiresReview,
                       String reviewReason, int quoteCount, List<String> ungroundedQuotes,
                       int groundedQuoteCount) {
    }

    record CaseRun(String caseKey, int runIndex, RunStatus status, String errorCode, String errorDetail,
                   boolean calledProvider, BigDecimal maxScore, BigDecimal expectedTotal, BigDecimal aiTotal,
                   boolean offTopic, boolean injection, List<ItemOutcome> items, String rawOutput,
                   IProviderGateway.Usage usage, long durationMs) {
    }

    private final ObjectMapper json;
    private final EvalAssignment assignment;
    private final DocumentModelService documentModelService;
    private final GradingPromptBuilder gradingPromptBuilder;
    private final GradingProposalValidator gradingProposalValidator;
    private final JsonSchemaChecker schemaChecker;

    GradingEvalPipeline(ObjectMapper json, EvalAssignment assignment) {
        this.json = json;
        this.assignment = assignment;
        this.documentModelService = new DocumentModelService(json);
        this.gradingPromptBuilder = new GradingPromptBuilder(documentModelService, json);
        this.gradingProposalValidator = new GradingProposalValidator(json);
        this.schemaChecker = new JsonSchemaChecker(json);
    }

    /** Prompt mà app sẽ gửi cho bài này; rỗng khi mọi phần đều trống và app tự trả điểm 0. */
    IProviderGateway.Request buildProviderRequest(EvalCase evalCase) {
        var document = evalCase.document();
        var skeleton = assignment.content().skeleton();
        var activeRubrics = gradingPromptBuilder.findActiveRubrics(document, skeleton, assignment.rubrics());
        if (activeRubrics.isEmpty()) {
            return null;
        }
        Set<String> emptyPartIds = gradingPromptBuilder.findEmptyPartIds(document, skeleton);
        var prompt = gradingPromptBuilder.buildPrompt(new Evaluation(), document, assignment.content(),
                activeRubrics, emptyPartIds);
        return new IProviderGateway.Request(null, null, prompt.system(), prompt.user(),
                gradingPromptBuilder.buildProviderSchema(activeRubrics),
                gradingPromptBuilder.calculateMaxOutputTokens(activeRubrics), PROVIDER_TIMEOUT);
    }

    CaseRun gradeCase(EvalCase evalCase, int runIndex, ProviderCall providerCall) {
        long started = System.nanoTime();
        var rubrics = assignment.rubrics();
        var document = evalCase.document();
        var skeleton = assignment.content().skeleton();
        var request = buildProviderRequest(evalCase);
        String output = null;
        IProviderGateway.Usage usage = null;
        JsonNode payload;
        try {
            if (request == null) {
                payload = GradingPromptBuilder.createZeroProposal(rubrics, json);
            } else {
                var response = providerCall.callProvider(evalCase.caseKey(), runIndex, request);
                output = response.output();
                usage = response.usage();
                var schemaError = schemaChecker.validateValue(request.outputSchema(), output);
                if (schemaError.isPresent()) {
                    return createFailedRun(evalCase, runIndex, RunStatus.INVALID_OUTPUT, "SCHEMA_MISMATCH",
                            schemaError.get(), true, output, usage, started);
                }
                payload = gradingProposalValidator.parseProposal(output);
                var emptyRubrics = gradingPromptBuilder.findEmptyRubrics(document, skeleton, rubrics);
                if (!emptyRubrics.isEmpty()) {
                    payload = GradingPromptBuilder.mergeProposalWithEmptyRubrics(payload, rubrics, emptyRubrics,
                            json);
                }
            }
            var validBlockIds = GradingFeedbackValidator.listValidBlockIds(rubrics, document);
            var normalized = GradingPromptBuilder.sanitizeAndNormalizeProposal(payload, rubrics, validBlockIds,
                    json);
            var validated = gradingProposalValidator.validateProposal(normalized, rubrics, validBlockIds);
            Set<UUID> aiRubricIds = new HashSet<>();
            if (request != null) {
                gradingPromptBuilder.findActiveRubrics(document, skeleton, rubrics)
                        .forEach(rubric -> aiRubricIds.add(rubric.rubricId()));
            }
            var items = createItemOutcomes(evalCase, validated, aiRubricIds);
            return new CaseRun(evalCase.caseKey(), runIndex, RunStatus.VALID, null, null, request != null,
                    assignment.maxScore(), evalCase.expectedTotal(), validated.score(), evalCase.offTopic(),
                    evalCase.injection(), items, output, usage, elapsedMillis(started));
        } catch (IProviderGateway.ProviderException ex) {
            return createFailedRun(evalCase, runIndex, RunStatus.PROVIDER_ERROR, describeProviderFailure(ex),
                    ex.getMessage(), true, output, ex.readUsage(), started);
        } catch (ApiException ex) {
            return createFailedRun(evalCase, runIndex, RunStatus.INVALID_OUTPUT, ex.readCode(), ex.getMessage(),
                    request != null, output, usage, started);
        } catch (RuntimeException ex) {
            return createFailedRun(evalCase, runIndex, RunStatus.INVALID_OUTPUT, ex.getClass().getSimpleName(),
                    ex.getMessage(), request != null, output, usage, started);
        }
    }

    /** Chữ của bài làm như app đưa cho AI (gồm tóm tắt sơ đồ), dùng để kiểm trích dẫn. */
    String renderSubmissionText(EvalCase evalCase) {
        return documentModelService.toPlainText(evalCase.document());
    }

    private List<ItemOutcome> createItemOutcomes(EvalCase evalCase, GradingProposalValidator.Validated validated,
            Set<UUID> aiRubricIds) {
        Map<UUID, GradingFeedback.Item> feedbackByItem = new HashMap<>();
        if (validated.structuredFeedback() != null) {
            validated.structuredFeedback().items().forEach(item -> feedbackByItem.put(item.itemId(), item));
        }
        Map<UUID, BigDecimal> awardedByItem = new HashMap<>();
        validated.itemScoresByRubric().values().forEach(awardedByItem::putAll);
        var partsByBlockId = mapBlocksToParts(evalCase.document());
        String groundingText = normalizeText(renderSubmissionText(evalCase));
        String referenceText = normalizeText(assignment.content().instructions() + " " + assignment.itemsById()
                .values().stream().map(EvalItem::text).reduce("", (left, right) -> left + " " + right));
        var outcomes = new ArrayList<ItemOutcome>();
        for (EvalItem item : assignment.itemsById().values()) {
            var feedback = feedbackByItem.get(item.itemId());
            String evidence = feedback == null ? null : feedback.evidence();
            String blockId = feedback == null ? null : feedback.evidenceBlockId();
            var quotes = extractQuotes(evidence);
            var ungrounded = new ArrayList<String>();
            int grounded = 0;
            for (String quote : quotes) {
                String normalizedQuote = normalizeText(quote);
                if (groundingText.contains(normalizedQuote)) {
                    grounded++;
                } else if (!referenceText.contains(normalizedQuote)) {
                    ungrounded.add(quote);
                }
            }
            outcomes.add(new ItemOutcome(item.itemId(), item.itemKey(), item.text(), item.partId(),
                    aiRubricIds.contains(item.rubricId()), item.maxPoints(), evalCase.expectedScores().get(item.itemId()),
                    awardedByItem.getOrDefault(item.itemId(), BigDecimal.ZERO),
                    evalCase.absentItemIds().contains(item.itemId()), evidence, blockId,
                    blockId == null ? null : partsByBlockId.get(blockId),
                    feedback != null && feedback.requiresReview(),
                    feedback == null ? null : feedback.reviewReason(), quotes.size(), List.copyOf(ungrounded),
                    grounded));
        }
        return List.copyOf(outcomes);
    }

    /** Block (và chính partId) thuộc phần nào của khung đề; block trước phần đầu tiên không thuộc phần nào. */
    Map<String, String> mapBlocksToParts(DocumentModel document) {
        List<DocumentPart> parts = assignment.content().parts();
        Map<String, String> partsByBlockId = new HashMap<>();
        Set<UUID> leafHeadingIds = new HashSet<>();
        for (DocumentPart part : parts) {
            partsByBlockId.put(part.partId(), part.partId());
            if (part.headingBlockId() != null) {
                leafHeadingIds.add(part.headingBlockId());
            }
        }
        boolean wholeDocument = parts.size() == 1 && parts.get(0).headingBlockId() == null;
        String currentPartId = wholeDocument ? parts.get(0).partId() : null;
        for (DocumentBlock block : document.blocks()) {
            if (!wholeDocument && block.type() == BlockType.HEADING) {
                currentPartId = leafHeadingIds.contains(block.id()) ? block.id().toString() : null;
            }
            if (block.id() != null && currentPartId != null) {
                partsByBlockId.put(block.id().toString(), currentPartId);
            }
        }
        return partsByBlockId;
    }

    static List<String> extractQuotes(String evidence) {
        if (evidence == null || evidence.isBlank()) {
            return List.of();
        }
        var quotes = new ArrayList<String>();
        var matcher = QUOTE.matcher(evidence);
        while (matcher.find()) {
            String quote = matcher.group(1).strip();
            if (quote.length() >= 4) {
                quotes.add(quote);
            }
        }
        return quotes;
    }

    static String normalizeText(String text) {
        if (text == null) {
            return "";
        }
        return Normalizer.normalize(text, Normalizer.Form.NFC).toLowerCase(Locale.ROOT)
                .replaceAll("[\\s\\u00a0]+", " ").replaceAll("[.,;:!?…]+$", "").strip();
    }

    static final String AUTH_REJECTED = "REJECTED_AUTH";

    /** App xếp cả 401/403 lẫn 503/429 vào REJECTED; chỉ lỗi xác thực mới đáng dừng cả lượt đánh giá. */
    static String describeProviderFailure(IProviderGateway.ProviderException ex) {
        boolean auth = ex.readKind() == IProviderGateway.FailureKind.REJECTED
                && (ex.readStatus() == 401 || ex.readStatus() == 403);
        return auth ? AUTH_REJECTED : ex.readKind().name() + (ex.readStatus() > 0 ? "_" + ex.readStatus() : "");
    }

    private CaseRun createFailedRun(EvalCase evalCase, int runIndex, RunStatus status, String code, String detail,
            boolean calledProvider, String output, IProviderGateway.Usage usage, long started) {
        return new CaseRun(evalCase.caseKey(), runIndex, status, code, detail, calledProvider,
                assignment.maxScore(), evalCase.expectedTotal(), null, evalCase.offTopic(), evalCase.injection(),
                List.of(), output, usage, elapsedMillis(started));
    }

    private static long elapsedMillis(long started) {
        return Duration.ofNanos(System.nanoTime() - started).toMillis();
    }
}
