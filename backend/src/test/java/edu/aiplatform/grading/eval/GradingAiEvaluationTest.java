package edu.aiplatform.grading.eval;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.ai.contract.AiProvider;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * Đánh giá AI chấm trên bộ dữ liệu có điểm giảng viên. Mặc định bỏ qua; bật bằng {@code -Dai.eval.mode=live|replay}.
 * Hướng dẫn: aidlc-docs/construction/build-and-test/ai-grading-evaluation-instructions.md.
 */
@EnabledIfSystemProperty(named = "ai.eval.mode", matches = "live|replay")
class GradingAiEvaluationTest {

    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();

    @Test
    void evaluatesAiGradingQualityAgainstLecturerScores() throws Exception {
        String mode = System.getProperty("ai.eval.mode");
        String datasetKey = readSetting("ai.eval.dataset", "AI_EVAL_DATASET", GradingEvalDataset.DEFAULT_DATASET);
        Set<String> caseFilter = Arrays.stream(readSetting("ai.eval.cases", "AI_EVAL_CASES", "").split(","))
                .map(String::strip).filter(value -> !value.isEmpty()).collect(Collectors.toSet());
        String startedAt = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneOffset.UTC)
                .format(Instant.now());
        Path outputDirectory = Path.of(readSetting("ai.eval.outputDir", "AI_EVAL_OUTPUT_DIR", "target/ai-eval"))
                .resolve(datasetKey + "-" + mode + "-" + startedAt);

        var dataset = GradingEvalDataset.loadDataset(json, datasetKey);
        var runner = new GradingEvalRunner(json, dataset);
        GradingEvalPipeline.ProviderCall providerCall;
        GradingEvalReport.RunInfo info;
        int repeats;
        if ("live".equals(mode)) {
            var provider = AiProvider.valueOf(requireSetting("ai.eval.provider", "AI_EVAL_PROVIDER")
                    .toUpperCase(Locale.ROOT));
            String model = requireSetting("ai.eval.model", "AI_EVAL_MODEL");
            repeats = Integer.parseInt(readSetting("ai.eval.runs", "AI_EVAL_RUNS", "3"));
            var recordings = new GradingEvalRecordings(json, outputDirectory.resolve("recordings"));
            providerCall = GradingEvalRunner.createLiveCall(json, provider, model,
                    requireSetting("ai.eval.apiKey", "AI_EVAL_API_KEY"), recordings);
            info = new GradingEvalReport.RunInfo(mode, provider.name(), model, repeats, startedAt);
        } else {
            Path replayDirectory = Path.of(requireSetting("ai.eval.replayDir", "AI_EVAL_REPLAY_DIR"));
            var recordings = new GradingEvalRecordings(json, replayDirectory);
            repeats = countRecordedRuns(recordings, dataset);
            var sample = recordings.loadRecording(dataset.cases().stream()
                    .filter(evalCase -> recordings.hasRecording(evalCase.caseKey(), 1)).findFirst()
                    .orElseThrow(() -> new IllegalStateException("No recordings in " + replayDirectory))
                    .caseKey(), 1);
            providerCall = recordings.createReplayCall();
            info = new GradingEvalReport.RunInfo(mode, sample.provider(), sample.model(), repeats, startedAt);
        }

        var runs = runner.runCases(providerCall, repeats, caseFilter);
        var summary = GradingEvalMetrics.calculateSummary(runs);
        GradingEvalReport.writeReport(json, outputDirectory, info, dataset.assignment(), runs, summary);
        System.out.println("AI grading evaluation report: " + outputDirectory.resolve("report.md").toAbsolutePath());

        assertThat(Files.exists(outputDirectory.resolve("report.md"))).isTrue();
        assertThat(runs).as("provider rejected the request (wrong key, model or permission); see report.md")
                .noneMatch(run -> GradingEvalPipeline.AUTH_REJECTED.equals(run.errorCode()));
        if (Boolean.parseBoolean(readSetting("ai.eval.enforce", "AI_EVAL_ENFORCE", "false"))) {
            assertThat(summary.metrics()).as("metrics below threshold")
                    .allMatch(GradingEvalMetrics.Metric::isPassed);
        }
    }

    private static int countRecordedRuns(GradingEvalRecordings recordings, GradingEvalDataset dataset) {
        // Lấy số lần chạy lớn nhất có bản ghi; lần thiếu (live bị dừng giữa chừng) được tính là lỗi provider.
        int runs = 1;
        for (var evalCase : dataset.cases()) {
            int recorded = 0;
            while (recordings.hasRecording(evalCase.caseKey(), recorded + 1)) {
                recorded++;
            }
            runs = Math.max(runs, recorded);
        }
        return runs;
    }

    private static String readSetting(String property, String environment, String fallback) {
        String value = System.getProperty(property);
        if (value == null || value.isBlank()) {
            value = System.getenv(environment);
        }
        return value == null || value.isBlank() ? fallback : value.strip();
    }

    private static String requireSetting(String property, String environment) {
        String value = readSetting(property, environment, null);
        if (value == null) {
            throw new IllegalStateException("Missing -D" + property + " or env " + environment);
        }
        return value;
    }
}
