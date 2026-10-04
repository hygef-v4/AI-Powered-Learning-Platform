package edu.aiplatform.aiexecution.port;

import java.util.List;
import java.util.Map;

/** Judge0 qua mạng sandbox; `Judge0Adapter` cài. */
public interface CodeRunnerPort {
    List<TestOutcome> run(String language, Map<String, String> files, String entryPoint, List<CodeTestCase> tests, RunLimits limits);
}
