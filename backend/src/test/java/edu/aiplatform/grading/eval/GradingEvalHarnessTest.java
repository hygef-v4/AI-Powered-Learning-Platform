package edu.aiplatform.grading.eval;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.aiplatform.ai.service.IProviderGateway;
import edu.aiplatform.assignments.contract.BlockType;
import edu.aiplatform.assignments.contract.DocumentBlock;
import edu.aiplatform.assignments.contract.DocumentModel;
import edu.aiplatform.assignments.service.DocumentModelService;
import edu.aiplatform.grading.eval.GradingEvalDataset.EvalCase;
import edu.aiplatform.grading.eval.GradingEvalDataset.EvalItem;
import edu.aiplatform.grading.eval.GradingEvalPipeline.CaseRun;
import edu.aiplatform.grading.eval.GradingEvalPipeline.RunStatus;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Kiểm harness đánh giá AI chấm không cần provider thật: dữ liệu hợp lệ, chỉ số bắt được AI tốt/xấu. */
class GradingEvalHarnessTest {

    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();
    private final GradingEvalDataset dataset = GradingEvalDataset.loadDataset(json,
            GradingEvalDataset.DEFAULT_DATASET);
    private final GradingEvalRunner runner = new GradingEvalRunner(json, dataset);

    @Test
    void datasetHasCompleteBoundedLecturerScoresForEveryRubricItem() {
        var assignment = dataset.assignment();
        assertThat(dataset.cases()).hasSize(8);
        assertThat(assignment.itemsById()).hasSize(13);
        assertThat(assignment.maxScore()).isEqualByComparingTo("4.00");
        for (EvalCase evalCase : dataset.cases()) {
            assertThat(evalCase.expectedScores().keySet()).as(evalCase.caseKey())
                    .isEqualTo(assignment.itemsById().keySet());
            evalCase.expectedScores().forEach((itemId, expected) -> {
                var item = assignment.itemsById().get(itemId);
                assertThat(expected).as(evalCase.caseKey() + " " + item.itemKey())
                        .isBetween(BigDecimal.ZERO, item.maxPoints());
            });
            evalCase.absentItemIds().forEach(itemId -> assertThat(evalCase.expectedScores().get(itemId))
                    .as(evalCase.caseKey() + " absent item").isEqualByComparingTo(BigDecimal.ZERO));
            if (evalCase.offTopic()) {
                assertThat(GradingEvalMetrics.ratio(evalCase.expectedTotal(), assignment.maxScore()))
                        .isLessThanOrEqualTo(GradingEvalMetrics.OFF_TOPIC_MAX_RATIO);
            }
        }
    }

    @ParameterizedTest
    @CsvSource({
        "swr302-lab-equipment, 3",
        "swr302-lab-equipment-relations, 1",
        "swr302-room-scheduling, 2",
        "swd392-workshop-registration, 3",
        "swd392-internship-portal, 3"
    })
    void peFixturesBuildGradingRequestsWithCompleteSyntheticReferences(String datasetKey, int caseCount)
            throws Exception {
        var peDataset = GradingEvalDataset.loadDataset(json, datasetKey);
        var assignment = peDataset.assignment();
        assertThat(assignment.assignmentKey()).isEqualTo(datasetKey);
        assertThat(assignment.labelSource()).isEqualTo("SYNTHETIC_TEAM");
        assertThat(assignment.maxScore()).isPositive();
        assertThat(peDataset.cases()).hasSize(caseCount).extracting(EvalCase::caseKey).doesNotHaveDuplicates();
        var pipeline = new GradingEvalRunner(json, peDataset).pipeline();
        var documents = new DocumentModelService(json);
        for (var fixture : peDataset.cases()) {
            var validation = documents.validateForSave(assignment.content().skeleton(), fixture.document());
            assertThat(validation.errors()).as(fixture.caseKey()).isEmpty();
            assertThat(fixture.labelSource()).isEqualTo("SYNTHETIC_TEAM");
            assertThat(fixture.expectedScores()).containsOnlyKeys(assignment.itemsById().keySet());
            fixture.expectedScores().forEach((itemId, expected) -> {
                var rubricItem = assignment.itemsById().get(itemId);
                assertThat(expected).as(fixture.caseKey() + " " + rubricItem.itemKey())
                        .isBetween(BigDecimal.ZERO, rubricItem.maxPoints());
            });
            for (var itemId : fixture.absentItemIds()) {
                assertThat(fixture.expectedScores().get(itemId)).as(fixture.caseKey() + " absent item").isZero();
            }
            var request = pipeline.buildProviderRequest(fixture);
            assertThat(request).as(fixture.caseKey()).isNotNull();
            var input = json.readTree(request.user());
            assertThat(input.path("rubrics").isEmpty()).isFalse();
            assertThat(input.has("expected")).isFalse();
            assertThat(input.has("expectedBehavior")).isFalse();
            assertThat(request.outputSchema()).contains("itemScoresByRubric");
        }
    }

    @Test
    void peNotationComparisonUsesSameSubmissionAndChangesOnlyTheStyleCriterion() {
        var required = GradingEvalDataset.loadDataset(json, "swr302-lab-equipment");
        var semantic = GradingEvalDataset.loadDataset(json, "swr302-lab-equipment-relations");
        var withStyle = required.cases().stream().filter(fixture -> fixture.caseKey().equals("swr-lab-solid-required"))
                .findFirst().orElseThrow();
        var withoutStyle = semantic.cases().get(0);
        assertThat(withoutStyle.caseKey()).isEqualTo("swr-lab-solid-semantic");
        assertThat(required.assignment().maxScore()).isEqualByComparingTo(semantic.assignment().maxScore());
        assertThat(required.assignment().content().instructions()).isEqualTo(semantic.assignment().content().instructions());
        assertThat(withStyle.document().blocks()).extracting(DocumentBlock::content)
                .containsExactlyElementsOf(withoutStyle.document().blocks().stream().map(DocumentBlock::content).toList());

        var requiredScores = collectReferenceScores(required, withStyle);
        var semanticScores = collectReferenceScores(semantic, withoutStyle);
        assertThat(requiredScores.remove("d-notation")).isZero();
        assertThat(semanticScores.remove("d-extension-condition")).isEqualByComparingTo("0.50");
        assertThat(requiredScores).isEqualTo(semanticScores);
        assertThat(new GradingEvalRunner(json, required).pipeline().buildProviderRequest(withStyle).user())
                .contains("(nét liền): <<include>>");
    }

    private static Map<String, BigDecimal> collectReferenceScores(GradingEvalDataset dataset, EvalCase fixture) {
        return fixture.expectedScores().entrySet().stream().collect(Collectors.toMap(
                entry -> dataset.assignment().itemsById().get(entry.getKey()).itemKey(), Map.Entry::getValue));
    }

    @Test
    void providerRequestUsesAppPromptAndOnlyRubricsOfNonEmptyParts() throws Exception {
        var complete = requireCase("01-complete");
        var request = runner.pipeline().buildProviderRequest(complete);
        JsonNode user = json.readTree(request.user());
        assertThat(user.path("rubrics")).hasSize(2);
        assertThat(user.path("instructions").asText()).contains("2 normal flow");
        assertThat(request.system()).contains("locked rubric checklist");
        assertThat(request.model()).isNull();
        assertThat(request.apiKey()).isNull();

        var emptyDiagram = runner.pipeline().buildProviderRequest(requireCase("07-empty-diagram"));
        JsonNode emptyUser = json.readTree(emptyDiagram.user());
        assertThat(emptyUser.path("rubrics")).hasSize(1);
        assertThat(emptyUser.path("rubrics").get(0).path("partId").asText())
                .isEqualTo(partIdOf("s-normal"));
    }

    @Test
    void diagramSummarySentToAiShowsIncludeLabelAndLineStyle() throws Exception {
        // F2 (2026-11-13): trước đây nhãn <<include>> bị xóa và nét đứt, nét liền trông giống hệt nhau.
        String dashed = runner.pipeline().buildProviderRequest(requireCase("01-complete")).user();
        String solid = runner.pipeline().buildProviderRequest(requireCase("04-solid-include")).user();
        assertThat(dashed).contains("nối uc-book -> uc-pay (nét đứt): <<include>>",
                "nối uc-cancel -> uc-pay (nét đứt): <<extend>>");
        assertThat(solid).contains("nối uc-book -> uc-pay (nét liền): <<include>>").doesNotContain("uc-pay (nét đứt)");
        // Kế hoạch 2026-11-20: AI thấy khung system boundary, mã block, và sơ đồ chỉ được gửi một lần.
        assertThat(dashed).contains("hình sys (chữ nhật): Hệ thống đặt sân cầu lông",
                "hình uc-book (ellipse) trong sys: Đặt sân", "[block ");
        assertThat(dashed).doesNotContain("actor-customer (umlActor) trong");
        assertThat(dashed.split("nối uc-book -> uc-pay", -1)).hasSize(2);
    }

    @Test
    void perfectProviderScoresTenWithEveryMetricPassed() {
        var runs = runner.runCases(createOracleCall(false), 2, Set.of());

        assertThat(runs).hasSize(16).allMatch(run -> run.status() == RunStatus.VALID);
        assertThat(runs.stream().flatMap(run -> run.items().stream()))
                .noneMatch(GradingEvalPipeline.ItemOutcome::requiresReview);
        var summary = GradingEvalMetrics.calculateSummary(runs);
        assertThat(summary.isAllPassed()).isTrue();
        assertThat(summary.compositeScore()).isEqualTo(10.0);
        assertThat(summary.requireMetric("score_mae").value()).isZero();
        assertThat(summary.requireMetric("item_kappa").value()).isEqualTo(1.0);
        assertThat(summary.requireMetric("fabricated_credit").value()).isZero();
        assertThat(summary.requireMetric("ungrounded_quotes").value()).isZero();
        assertThat(summary.requireMetric("wrong_location").value()).isZero();
        assertThat(summary.requireMetric("citation_rate").value()).isEqualTo(1.0);
        assertThat(summary.requireMetric("off_topic_detected").sampleSize()).isEqualTo(2);
        assertThat(summary.requireMetric("injection_resisted").sampleSize()).isEqualTo(2);
        assertThat(summary.requireMetric("score_stability").value()).isZero();
    }

    @ParameterizedTest
    @CsvSource({"evidence, false, false", "evidence, true, false",
            "reviewReason, false, true", "reviewReason, true, true"})
    void providerFeedbackWithMissingEvidenceOrReviewReasonIsFlaggedWithoutChangingPoints(
            String field, boolean omitted, boolean providerMarkedForReview) {
        var result = gradeWithEditedProviderFeedback(feedback -> {
            if (omitted) {
                feedback.remove(field);
            } else {
                feedback.putNull(field);
            }
            feedback.put("requiresReview", providerMarkedForReview);
        });

        assertThat(result.status()).as("provider diagnostic: %s", result.errorDetail())
                .isEqualTo(RunStatus.VALID);
        assertThat(result.items()).filteredOn(GradingEvalPipeline.ItemOutcome::requiresReview)
                .singleElement().satisfies(item -> {
                    assertThat(item.reviewReason()).isNotBlank();
                    assertThat(item.awarded()).isEqualByComparingTo(item.expected());
                    if (!providerMarkedForReview) {
                        assertThat(item.evidence()).isNull();
                    }
                });
    }

    @Test
    void providerMayOmitOptionalSuggestionAndEvidenceBlock() {
        var result = gradeWithEditedProviderFeedback(feedback ->
                feedback.remove(List.of("suggestion", "evidenceBlockId")));

        assertThat(result.status()).as("provider diagnostic: %s", result.errorDetail())
                .isEqualTo(RunStatus.VALID);
        assertThat(result.items()).noneMatch(GradingEvalPipeline.ItemOutcome::requiresReview);
    }

    @Test
    void providerFeedbackOfTheWrongTypeStillFailsSchemaValidation() {
        var result = gradeWithEditedProviderFeedback(feedback ->
                feedback.putObject("evidence").put("text", "Invalid type"));

        assertThat(result.status()).isEqualTo(RunStatus.INVALID_OUTPUT);
        assertThat(result.errorCode()).isEqualTo("SCHEMA_MISMATCH");
    }

    private CaseRun gradeWithEditedProviderFeedback(Consumer<ObjectNode> editFeedback) {
        EvalCase evalCase = requireCase("01-complete");
        return runner.pipeline().gradeCase(evalCase, 1, (key, runIndex, request) -> {
            var output = createOracleOutput(evalCase, readPromptRubrics(request.user()), false);
            editFeedback.accept((ObjectNode) output.at("/structuredFeedback/items/0"));
            return new IProviderGateway.Response(output.toString(), null, null);
        });
    }

    @ParameterizedTest
    @CsvSource({"08-wrong-actors, 0.25, true", "08-wrong-actors, 0.10, false", "01-complete, 0.25, false"})
    void flagsFullCreditWhenSecondaryActorsInTheSpecificationDoNotMatch(
            String caseKey, BigDecimal awarded, boolean requiresReview) {
        EvalCase evalCase = requireCase(caseKey);
        UUID itemId = GradingEvalDataset.createStableId(dataset.assignment().assignmentKey(), "s-name-actors");
        var actorItem = dataset.assignment().itemsById().get(itemId);
        var result = runner.pipeline().gradeCase(evalCase, 1, (key, runIndex, request) -> {
            var output = createOracleOutput(evalCase, readPromptRubrics(request.user()), false);
            ((ObjectNode) output.path("itemScoresByRubric").path(actorItem.rubricId().toString()))
                    .put(itemId.toString(), awarded);
            return new IProviderGateway.Response(output.toString(), null, null);
        });

        assertThat(result.status()).isEqualTo(RunStatus.VALID);
        assertThat(result.items()).filteredOn(item -> item.itemId().equals(itemId)).singleElement()
                .satisfies(item -> {
                    assertThat(item.awarded()).isEqualByComparingTo(awarded);
                    assertThat(item.requiresReview()).isEqualTo(requiresReview);
                    if (requiresReview) {
                        assertThat(item.reviewReason()).contains("actor phụ");
                    }
                });
    }

    private JsonNode readPromptRubrics(String prompt) {
        try {
            return json.readTree(prompt).path("rubrics");
        } catch (java.io.IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Test
    void fabricatingProviderIsCaughtByHallucinationOffTopicAndInjectionMetrics() {
        var runs = runner.runCases(createOracleCall(true), 1, Set.of());

        assertThat(runs).allMatch(run -> run.status() == RunStatus.VALID);
        var summary = GradingEvalMetrics.calculateSummary(runs);
        assertThat(summary.isAllPassed()).isFalse();
        assertThat(summary.requireMetric("fabricated_credit").value()).isEqualTo(1.0);
        assertThat(summary.requireMetric("ungrounded_quotes").value()).isEqualTo(1.0);
        assertThat(summary.requireMetric("wrong_location").value()).isEqualTo(1.0);
        assertThat(runs.stream().flatMap(run -> run.items().stream())
                .filter(item -> item.evidencePartId() != null && !item.partId().equals(item.evidencePartId())))
                .isNotEmpty().allMatch(item -> item.requiresReview() && !item.reviewReason().isBlank());
        assertThat(runs.stream().filter(run -> run.caseKey().equals("07-empty-diagram"))
                .flatMap(run -> run.items().stream()).filter(item -> !item.gradedByAi()))
                .as("empty diagram part is zeroed by the app, not by AI").hasSize(6)
                .allMatch(item -> item.awarded().signum() == 0);
        assertThat(summary.requireMetric("off_topic_detected").value()).isZero();
        assertThat(summary.requireMetric("injection_resisted").value()).isZero();
        assertThat(summary.requireMetric("score_stability").isApplicable()).isFalse();
        assertThat(summary.compositeScore()).isLessThan(5.0);
    }

    @Test
    void malformedProviderOutputCountsAsFormatFailureNotAsScore() {
        var runs = runner.runCases((caseKey, runIndex, request) ->
                new IProviderGateway.Response("Điểm của bài là 10/10", null, null), 1, Set.of());

        assertThat(runs).allMatch(run -> run.status() == RunStatus.INVALID_OUTPUT && run.aiTotal() == null);
        var summary = GradingEvalMetrics.calculateSummary(runs);
        assertThat(summary.requireMetric("valid_output_rate").value()).isZero();
        assertThat(summary.requireMetric("score_mae").isApplicable()).isFalse();
        assertThat(GradingEvalReport.renderMarkdown(new GradingEvalReport.RunInfo("test", null, null, 1, "now"),
                dataset.assignment(), runs, summary)).contains("Lần chạy lỗi").contains("SCHEMA_MISMATCH");
    }

    @Test
    void rejectedKeyStopsTheRunAfterTheFirstCall() {
        int[] calls = {0};
        var runs = runner.runCases((caseKey, runIndex, request) -> {
            calls[0]++;
            throw new IProviderGateway.ProviderException(IProviderGateway.FailureKind.REJECTED, 401,
                    "Key API không hợp lệ", null);
        }, 3, Set.of());

        assertThat(calls[0]).isEqualTo(1);
        assertThat(runs).singleElement().extracting(CaseRun::errorCode).isEqualTo(GradingEvalPipeline.AUTH_REJECTED);
        var summary = GradingEvalMetrics.calculateSummary(runs);
        assertThat(GradingEvalReport.renderMarkdown(new GradingEvalReport.RunInfo("live", "CLAUDE", "m", 3, "now"),
                dataset.assignment(), runs, summary)).contains("không đo được chỉ số nào")
                .doesNotContain("mọi chỉ số đạt ngưỡng");
    }

    @Test
    void overloadedProviderDoesNotStopTheRemainingCases() {
        int[] calls = {0};
        var runs = runner.runCases((caseKey, runIndex, request) -> {
            calls[0]++;
            throw new IProviderGateway.ProviderException(IProviderGateway.FailureKind.REJECTED, 503,
                    "Nhà cung cấp AI đang quá tải (HTTP 503)", null);
        }, 1, Set.of());

        assertThat(calls[0]).isEqualTo(8);
        assertThat(runs).hasSize(8).allMatch(run -> "REJECTED_503".equals(run.errorCode()));
    }

    @Test
    void recordedRunsReplayToTheSameScoresAndReport(@TempDir Path directory) throws Exception {
        var recordings = new GradingEvalRecordings(json, directory.resolve("recordings"));
        var oracle = createOracleCall(false);
        GradingEvalPipeline.ProviderCall recordingCall = (caseKey, runIndex, request) -> {
            var response = oracle.callProvider(caseKey, runIndex, request);
            recordings.saveRecording(new GradingEvalRecordings.Recording(caseKey, runIndex, "CLAUDE", "test-model",
                    response.output(), null, 0, null, new IProviderGateway.Usage(10L, 20L, null, null)));
            return response;
        };
        var live = runner.runCases(recordingCall, 1, Set.of("03-one-normal-flow", "05-off-topic"));
        var replayed = runner.runCases(recordings.createReplayCall(), 1, Set.of("03-one-normal-flow",
                "05-off-topic"));

        assertThat(replayed).extracting(CaseRun::aiTotal).containsExactlyElementsOf(
                live.stream().map(CaseRun::aiTotal).toList());
        var summary = GradingEvalMetrics.calculateSummary(replayed);
        GradingEvalReport.writeReport(json, directory, new GradingEvalReport.RunInfo("replay", "CLAUDE",
                "test-model", 1, "now"), dataset.assignment(), replayed, summary);
        assertThat(Files.readString(directory.resolve("report.md"))).contains("05-off-topic")
                .contains("SYNTHETIC_TEAM");
        assertThat(json.readTree(directory.resolve("results.json").toFile()).path("runs")).hasSize(2);
    }

    /**
     * Provider giả: {@code fabricate=false} trả đúng điểm giảng viên kèm trích dẫn có thật; {@code true} cho điểm tối
     * đa mọi mục, trích một câu không có trong bài và chỉ sai phần bài.
     */
    private GradingEvalPipeline.ProviderCall createOracleCall(boolean fabricate) {
        var casesByKey = dataset.cases().stream().collect(Collectors.toMap(EvalCase::caseKey, Function.identity()));
        return (caseKey, runIndex, request) -> {
            var evalCase = casesByKey.get(caseKey);
            try {
                JsonNode user = json.readTree(request.user());
                return new IProviderGateway.Response(json.writeValueAsString(
                        createOracleOutput(evalCase, user.path("rubrics"), fabricate)), null, null);
            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            }
        };
    }

    private ObjectNode createOracleOutput(EvalCase evalCase, JsonNode activeRubrics, boolean fabricate) {
        var assignment = dataset.assignment();
        var documentModelService = new DocumentModelService(json);
        var partsByBlock = runner.pipeline().mapBlocksToParts(evalCase.document());
        ObjectNode output = json.createObjectNode().put("feedback", "Nhận xét chung.");
        ArrayNode sections = output.putArray("sections");
        ObjectNode scores = output.putObject("itemScoresByRubric");
        ObjectNode structured = output.putObject("structuredFeedback").put("schemaVersion", 1);
        ObjectNode summary = structured.putObject("summary");
        summary.putArray("strengths");
        summary.putArray("improvements");
        summary.putArray("nextSteps");
        ArrayNode items = structured.putArray("items");
        for (JsonNode rubric : activeRubrics) {
            String rubricId = rubric.path("rubricId").asText();
            String partId = rubric.path("partId").asText();
            sections.addObject().put("partId", partId).put("feedback", "Nhận xét phần.");
            ObjectNode rubricScores = scores.putObject(rubricId);
            DocumentBlock evidenceBlock = findFirstContentBlock(evalCase.document(), partsByBlock, partId,
                    fabricate);
            String blockText = documentModelService.toPlainText(new DocumentModel(List.of(evidenceBlock)))
                    .replaceAll("\\s+", " ").strip();
            String quote = blockText.substring(0, Math.min(24, blockText.length())).strip();
            for (EvalItem item : assignment.itemsById().values()) {
                if (!item.rubricId().toString().equals(rubricId)) {
                    continue;
                }
                BigDecimal score = fabricate ? item.maxPoints() : evalCase.expectedScores().get(item.itemId());
                rubricScores.put(item.itemId().toString(), score);
                items.addObject().put("rubricId", rubricId).put("itemId", item.itemId().toString())
                        .put("evidence", fabricate ? "Bài viết \"Hệ thống quản lý thư viện trực tuyến\""
                                : "Trong bài: \"" + quote + "\"")
                        .put("reason", "Đối chiếu với tiêu chí.")
                        .put("suggestion", "Bổ sung theo tiêu chí.")
                        .put("evidenceBlockId", evidenceBlock.id().toString())
                        .put("requiresReview", false).putNull("reviewReason");
            }
        }
        return output;
    }

    /** Block có nội dung đầu tiên trong phần; {@code otherPart=true} lấy block của phần khác để giả lập chỉ sai chỗ. */
    private static DocumentBlock findFirstContentBlock(DocumentModel document, Map<String, String> partsByBlock,
            String partId, boolean otherPart) {
        var candidates = document.blocks().stream()
                .filter(block -> {
                    String blockPart = partsByBlock.get(block.id().toString());
                    return blockPart != null && (otherPart != blockPart.equals(partId));
                }).toList();
        return candidates.stream().filter(block -> block.type() != BlockType.HEADING).findFirst()
                .or(() -> candidates.stream().findFirst())
                .orElseThrow(() -> new IllegalStateException("No block for part " + partId));
    }

    private EvalCase requireCase(String caseKey) {
        return dataset.cases().stream().filter(evalCase -> evalCase.caseKey().equals(caseKey)).findFirst()
                .orElseThrow();
    }

    private String partIdOf(String itemKey) {
        UUID itemId = GradingEvalDataset.createStableId(dataset.assignment().assignmentKey(), itemKey);
        return dataset.assignment().itemsById().get(itemId).partId();
    }
}
