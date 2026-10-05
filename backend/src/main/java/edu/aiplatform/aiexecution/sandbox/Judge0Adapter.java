package edu.aiplatform.aiexecution.sandbox;

import edu.aiplatform.aiexecution.port.*;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Khung cài đặt của U13: thân hàm tạm trả giá trị rỗng/an toàn hoặc báo chưa cài.
 * Plan U13: Adapter Judge0.
 */
@Component
public class Judge0Adapter implements CodeRunnerPort {

    @Override
    public List<TestOutcome> run(String language, Map<String, String> files, String entryPoint, List<CodeTestCase> tests, RunLimits limits) {
        return List.of();
    }
}
