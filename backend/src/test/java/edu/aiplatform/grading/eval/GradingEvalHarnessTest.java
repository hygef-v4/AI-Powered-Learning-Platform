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
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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

    @Test
    void fabricatingProviderIsCaughtByHallucinationOffTopicAndInjectionMetrics() {
        var runs = runner.runCases(createOracleCall(true), 1, Set.of());

        assertThat(runs).allMatch(run -> run.status() == RunStatus.VALID);
        var summary = GradingEvalMetrics.calculateSummary(runs);
        assertThat(summary.isAllPassed()).isFalse();
        assertThat(summary.requireMetric("fabricated_credit").value()).isEqualTo(1.0);
        assertThat(summary.requireMetric("ungrounded_quotes").value()).isEqualTo(1.0);
        assertThat(summary.requireMetric("wrong_location").value()).isEqualTo(1.0);
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
