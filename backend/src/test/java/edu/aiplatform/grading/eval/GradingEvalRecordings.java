package edu.aiplatform.grading.eval;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.aiplatform.ai.service.IProviderGateway;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Bản ghi output provider theo bài/lần chạy: {@code <dir>/<caseKey>/run-<n>.json}. Chạy live ghi bản ghi; chạy replay đọc
 * lại để tính chỉ số mà không gọi provider. Không lưu key, prompt hay header.
 */
final class GradingEvalRecordings {

    record Recording(String caseKey, int runIndex, String provider, String model, String output,
                     String errorKind, int httpStatus, String errorMessage, IProviderGateway.Usage usage) {
    }

    private final ObjectMapper json;
    private final Path directory;

    GradingEvalRecordings(ObjectMapper json, Path directory) {
        this.json = json;
        this.directory = directory;
    }

    void saveRecording(Recording recording) {
        ObjectNode node = json.createObjectNode().put("caseKey", recording.caseKey())
                .put("runIndex", recording.runIndex()).put("provider", recording.provider())
                .put("model", recording.model()).put("output", recording.output())
                .put("errorKind", recording.errorKind()).put("httpStatus", recording.httpStatus())
                .put("errorMessage", recording.errorMessage());
        if (recording.usage() != null) {
            node.set("usage", json.valueToTree(recording.usage()));
        }
        Path file = resolveFile(recording.caseKey(), recording.runIndex());
        try {
            Files.createDirectories(file.getParent());
            json.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), node);
        } catch (IOException ex) {
            throw new UncheckedIOException("Cannot write recording " + file, ex);
        }
    }

    boolean hasRecording(String caseKey, int runIndex) {
        return Files.isRegularFile(resolveFile(caseKey, runIndex));
    }

    Recording loadRecording(String caseKey, int runIndex) {
        Path file = resolveFile(caseKey, runIndex);
        try {
            JsonNode node = json.readTree(file.toFile());
            JsonNode usage = node.path("usage");
            return new Recording(caseKey, runIndex, node.path("provider").asText(null),
                    node.path("model").asText(null), node.path("output").isNull() ? null : node.path("output")
                    .asText(null), node.path("errorKind").asText(null), node.path("httpStatus").asInt(0),
                    node.path("errorMessage").asText(null),
                    usage.isObject() ? json.treeToValue(usage, IProviderGateway.Usage.class) : null);
        } catch (IOException ex) {
            throw new UncheckedIOException("Cannot read recording " + file, ex);
        }
    }

    /** Phát lại bản ghi như một provider; lỗi đã ghi được ném lại đúng loại. */
    GradingEvalPipeline.ProviderCall createReplayCall() {
        return (caseKey, runIndex, request) -> {
            if (!hasRecording(caseKey, runIndex)) {
                // Lượt live bị dừng giữa chừng: tính là lỗi provider, không phải output sai định dạng.
                throw new IProviderGateway.ProviderException(IProviderGateway.FailureKind.UNKNOWN, 0,
                        "Không có bản ghi cho lần chạy này", null);
            }
            var recording = loadRecording(caseKey, runIndex);
            if (recording.errorKind() != null) {
                throw new IProviderGateway.ProviderException(
                        IProviderGateway.FailureKind.valueOf(recording.errorKind()), recording.httpStatus(),
                        recording.errorMessage(), recording.usage());
            }
            return new IProviderGateway.Response(recording.output(), null, recording.usage());
        };
    }

    private Path resolveFile(String caseKey, int runIndex) {
        return directory.resolve(caseKey).resolve("run-" + runIndex + ".json");
    }
}
