package edu.aiplatform.grading.eval;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.ai.contract.AiProvider;
import edu.aiplatform.ai.integration.HttpProviderGateway;
import edu.aiplatform.ai.service.IProviderGateway;
import edu.aiplatform.grading.eval.GradingEvalDataset.EvalCase;
import edu.aiplatform.grading.eval.GradingEvalPipeline.CaseRun;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Chạy toàn bộ bộ dữ liệu qua pipeline với một provider (live, replay hoặc giả lập trong test). */
final class GradingEvalRunner {

    private final GradingEvalDataset dataset;
    private final GradingEvalPipeline pipeline;

    GradingEvalRunner(ObjectMapper json, GradingEvalDataset dataset) {
        this.dataset = dataset;
        this.pipeline = new GradingEvalPipeline(json, dataset.assignment());
    }

    GradingEvalPipeline pipeline() {
        return pipeline;
    }

    List<CaseRun> runCases(GradingEvalPipeline.ProviderCall providerCall, int repeats, Set<String> caseFilter) {
        var runs = new ArrayList<CaseRun>();
        for (EvalCase evalCase : dataset.cases()) {
            if (!caseFilter.isEmpty() && !caseFilter.contains(evalCase.caseKey())) {
                continue;
            }
            for (int runIndex = 1; runIndex <= repeats; runIndex++) {
                var run = pipeline.gradeCase(evalCase, runIndex, providerCall);
                runs.add(run);
                // Key sai/không có quyền thì mọi lần gọi sau cũng bị từ chối: dừng sớm thay vì gọi tiếp.
                if (GradingEvalPipeline.AUTH_REJECTED.equals(run.errorCode())) {
                    return List.copyOf(runs);
                }
            }
        }
        return List.copyOf(runs);
    }

    /** Gọi provider thật bằng key của người chạy và ghi lại output (kể cả lỗi) để replay. */
    static GradingEvalPipeline.ProviderCall createLiveCall(ObjectMapper json, AiProvider provider, String model,
            String apiKey, GradingEvalRecordings recordings) {
        var gateway = new HttpProviderGateway(json);
        return (caseKey, runIndex, request) -> {
            var withCredentials = new IProviderGateway.Request(model, apiKey, request.system(), request.user(),
                    request.outputSchema(), request.maxOutputTokens(), request.timeout(), request.images());
            try {
                var response = gateway.call(provider, withCredentials);
                recordings.saveRecording(new GradingEvalRecordings.Recording(caseKey, runIndex, provider.name(),
                        model, response.output(), null, 0, null, response.usage()));
                return response;
            } catch (IProviderGateway.ProviderException ex) {
                recordings.saveRecording(new GradingEvalRecordings.Recording(caseKey, runIndex, provider.name(),
                        model, null, ex.readKind().name(), ex.readStatus(), ex.getMessage(), ex.readUsage()));
                throw ex;
            }
        };
    }
}
