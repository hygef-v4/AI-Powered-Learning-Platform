package edu.aiplatform.grading.eval;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.aiplatform.grading.eval.GradingEvalDataset.EvalAssignment;
import edu.aiplatform.grading.eval.GradingEvalMetrics.Direction;
import edu.aiplatform.grading.eval.GradingEvalMetrics.Metric;
import edu.aiplatform.grading.eval.GradingEvalMetrics.Summary;
import edu.aiplatform.grading.eval.GradingEvalPipeline.CaseRun;
import edu.aiplatform.grading.eval.GradingEvalPipeline.ItemOutcome;
import edu.aiplatform.grading.eval.GradingEvalPipeline.RunStatus;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/** Báo cáo Markdown (tiếng Việt) và JSON cho một lượt đánh giá. */
final class GradingEvalReport {

    record RunInfo(String mode, String provider, String model, int repeats, String startedAt) {
    }

    private GradingEvalReport() {
    }

    static void writeReport(ObjectMapper json, Path directory, RunInfo info, EvalAssignment assignment,
            List<CaseRun> runs, Summary summary) {
        try {
            Files.createDirectories(directory);
            Files.writeString(directory.resolve("report.md"), renderMarkdown(info, assignment, runs, summary),
                    StandardCharsets.UTF_8);
            json.writerWithDefaultPrettyPrinter().writeValue(directory.resolve("results.json").toFile(),
                    createResultsJson(json, info, assignment, runs, summary));
        } catch (IOException ex) {
            throw new UncheckedIOException("Cannot write eval report to " + directory, ex);
        }
    }

    static String renderMarkdown(RunInfo info, EvalAssignment assignment, List<CaseRun> runs, Summary summary) {
        var out = new StringBuilder();
        out.append("# Đánh giá chất lượng AI chấm bài\n\n");
        out.append("- **Thời điểm:** ").append(info.startedAt()).append('\n');
        out.append("- **Chế độ:** ").append(info.mode()).append(" · **Provider/model:** ")
                .append(nullToDash(info.provider())).append(" / ").append(nullToDash(info.model())).append('\n');
        out.append("- **Bộ dữ liệu:** ").append(assignment.title()).append(" (").append(summary.caseCount())
                .append(" bài × ").append(info.repeats()).append(" lần chạy)\n");
        out.append("- **Nguồn điểm kỳ vọng:** `").append(assignment.labelSource()).append("`");
        if (!"LECTURER_VERIFIED".equals(assignment.labelSource())) {
            out.append(" — ").append(assignment.labelNote().isBlank()
                    ? "chưa được giảng viên xác nhận" : assignment.labelNote());
        }
        out.append("\n- **Lần chạy:** ").append(summary.runCount()).append(" (hợp lệ ")
                .append(summary.validRunCount()).append(", output lỗi ").append(summary.invalidRunCount())
                .append(", lỗi provider ").append(summary.providerErrorCount()).append(")\n");
        out.append("- **Tỷ lệ mục AI tự đánh dấu cần giảng viên xem:** ")
                .append(formatPercent(summary.reviewRate())).append("\n\n");

        out.append("## Điểm tổng hợp\n\n");
        out.append("**").append(summary.compositeScore() == null ? "—"
                : String.format(Locale.ROOT, "%.2f", summary.compositeScore())).append(" / 10** — ")
                .append(summary.compositeScore() == null ? "không đo được chỉ số nào (xem phần lần chạy lỗi)"
                        : summary.isAllPassed() ? "mọi chỉ số đạt ngưỡng" : "có chỉ số chưa đạt ngưỡng")
                .append(".\n\n| Thành phần | Trọng số | Điểm thành phần |\n|---|---:|---:|\n");
        summary.components().forEach(component -> out.append("| ").append(component.title()).append(" | ")
                .append((int) component.weight()).append("% | ")
                .append(component.score() == null ? "không đo được" : formatPercent(component.score()))
                .append(" |\n"));

        out.append("\n## Chỉ số chi tiết\n\n| Nhóm | Chỉ số | Giá trị | Ngưỡng | Mẫu | Kết quả |\n")
                .append("|---|---|---:|---:|---:|---|\n");
        for (Metric metric : summary.metrics()) {
            out.append("| ").append(metric.group()).append(" | ").append(metric.title()).append(" | ")
                    .append(metric.isApplicable() ? formatMetric(metric, metric.value()) : "—").append(" | ")
                    .append(metric.direction() == Direction.HIGHER_IS_BETTER ? "≥ " : "≤ ")
                    .append(formatMetric(metric, metric.threshold())).append(" | ").append(metric.sampleSize())
                    .append(" | ").append(!metric.isApplicable() ? "Không áp dụng"
                            : metric.isPassed() ? "Đạt" : "**Không đạt**").append(" |\n");
        }
        out.append("\nĐịnh nghĩa:\n\n");
        summary.metrics().forEach(metric -> out.append("- **").append(metric.title()).append(":** ")
                .append(metric.definition()).append(".\n"));

        out.append("\n## Theo từng bài\n\n| Bài | Điểm GV | Điểm AI (các lần) | Lệch TB | Ghi chú |\n")
                .append("|---|---:|---|---:|---|\n");
        var runsByCase = runs.stream().collect(Collectors.groupingBy(CaseRun::caseKey, LinkedHashMap::new,
                Collectors.toList()));
        runsByCase.forEach((caseKey, caseRuns) -> {
            var first = caseRuns.get(0);
            var validRuns = caseRuns.stream().filter(run -> run.status() == RunStatus.VALID).toList();
            String scores = caseRuns.stream().map(run -> run.status() == RunStatus.VALID
                    ? formatScore(run.aiTotal()) : run.status() + "(" + run.errorCode() + ")")
                    .collect(Collectors.joining(", "));
            double meanError = validRuns.stream().mapToDouble(run -> run.aiTotal().subtract(run.expectedTotal())
                    .doubleValue()).average().orElse(Double.NaN);
            String note = (first.offTopic() ? "lạc đề " : "") + (first.injection() ? "chèn lệnh " : "")
                    + (!first.calledProvider() && first.status() == RunStatus.VALID ? "app tự cho 0, không gọi AI"
                    : "");
            out.append("| ").append(caseKey).append(" | ").append(formatScore(first.expectedTotal())).append("/")
                    .append(formatScore(first.maxScore())).append(" | ").append(scores).append(" | ")
                    .append(Double.isNaN(meanError) ? "—" : String.format(Locale.ROOT, "%+.2f", meanError))
                    .append(" | ").append(note.strip()).append(" |\n");
        });

        out.append("\n## Mục rubric AI chấm lệch giảng viên\n\n");
        var disagreements = runs.stream().filter(run -> run.status() == RunStatus.VALID)
                .flatMap(run -> run.items().stream().filter(GradingEvalReport::isDisagreement)
                        .map(item -> new Object[] {run, item})).toList();
        if (disagreements.isEmpty()) {
            out.append("Không có mục nào lệch quá 25% điểm mục.\n");
        } else {
            out.append("| Bài/lần | Mục | GV | AI | Cần xem | Bằng chứng AI nêu |\n|---|---|---:|---:|---|---|\n");
            for (Object[] row : disagreements) {
                var run = (CaseRun) row[0];
                var item = (ItemOutcome) row[1];
                out.append("| ").append(run.caseKey()).append(" #").append(run.runIndex()).append(" | ")
                        .append(item.itemKey()).append(item.absent() ? " (vắng)" : "").append(" | ")
                        .append(formatScore(item.expected())).append(" | ").append(formatScore(item.awarded()))
                        .append(" | ").append(item.requiresReview() ? "có" : "").append(" | ")
                        .append(escapeCell(shorten(item.evidence(), 160))).append(" |\n");
            }
        }

        var ungrounded = runs.stream().flatMap(run -> run.items().stream()
                .filter(item -> !item.ungroundedQuotes().isEmpty())
                .map(item -> run.caseKey() + " #" + run.runIndex() + " · " + item.itemKey() + ": "
                        + String.join(" | ", item.ungroundedQuotes()))).toList();
        if (!ungrounded.isEmpty()) {
            out.append("\n## Trích dẫn không tìm thấy trong bài\n\n");
            ungrounded.forEach(line -> out.append("- ").append(escapeCell(line)).append('\n'));
        }

        var failures = runs.stream().filter(run -> run.status() != RunStatus.VALID).toList();
        if (!failures.isEmpty()) {
            out.append("\n## Lần chạy lỗi\n\n");
            failures.forEach(run -> out.append("- ").append(run.caseKey()).append(" #").append(run.runIndex())
                    .append(": ").append(run.status()).append(" `").append(run.errorCode()).append("` ")
                    .append(escapeCell(shorten(run.errorDetail(), 200))).append('\n'));
        }
        return out.toString();
    }

    private static ObjectNode createResultsJson(ObjectMapper json, RunInfo info, EvalAssignment assignment,
            List<CaseRun> runs, Summary summary) {
        ObjectNode root = json.createObjectNode();
        root.set("run", json.valueToTree(info));
        root.put("assignment", assignment.assignmentKey()).put("labelSource", assignment.labelSource());
        root.put("compositeScore", summary.compositeScore());
        ArrayNode metrics = root.putArray("metrics");
        summary.metrics().forEach(metric -> metrics.addObject().put("key", metric.key())
                .put("value", metric.value()).put("threshold", metric.threshold())
                .put("sampleSize", metric.sampleSize()).put("applicable", metric.isApplicable())
                .put("passed", metric.isPassed()));
        ArrayNode cases = root.putArray("runs");
        for (CaseRun run : runs) {
            ObjectNode node = cases.addObject().put("caseKey", run.caseKey()).put("runIndex", run.runIndex())
                    .put("status", run.status().name()).put("errorCode", run.errorCode())
                    .put("expectedTotal", run.expectedTotal()).put("aiTotal", run.aiTotal())
                    .put("durationMs", run.durationMs());
            ArrayNode items = node.putArray("items");
            run.items().forEach(item -> items.addObject().put("itemKey", item.itemKey())
                    .put("expected", item.expected()).put("awarded", item.awarded()).put("absent", item.absent())
                    .put("requiresReview", item.requiresReview()).put("evidence", item.evidence())
                    .put("evidencePartMatches", item.evidencePartId() == null ? null
                            : item.evidencePartId().equals(item.partId())));
        }
        return root;
    }

    private static boolean isDisagreement(ItemOutcome item) {
        return GradingEvalMetrics.ratio(item.awarded().subtract(item.expected()).abs(), item.maxPoints())
                > GradingEvalMetrics.ITEM_TOLERANCE + 1e-9;
    }

    private static String formatMetric(Metric metric, double value) {
        if ("item_kappa".equals(metric.key())) {
            return String.format(Locale.ROOT, "%.2f", value);
        }
        return formatPercent(value);
    }

    private static String formatPercent(double value) {
        return String.format(Locale.ROOT, "%.1f%%", value * 100);
    }

    private static String formatScore(BigDecimal value) {
        return value == null ? "—" : value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static String shorten(String text, int max) {
        if (text == null) {
            return "";
        }
        String flat = text.replaceAll("\\s+", " ").strip();
        return flat.length() <= max ? flat : flat.substring(0, max) + "…";
    }

    private static String escapeCell(String text) {
        return text.replace("|", "\\|");
    }

    private static String nullToDash(String text) {
        return text == null || text.isBlank() ? "—" : text;
    }
}
