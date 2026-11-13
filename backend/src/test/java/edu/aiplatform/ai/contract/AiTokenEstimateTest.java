package edu.aiplatform.ai.contract;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AiTokenEstimateTest {
    @Test
    void estimatesHalfPromptLengthRoundedUpPlusOutputAllowance() {
        assertThat(AiTokenEstimate.estimate(101, 4096)).isEqualTo(4147);
        assertThat(AiTokenEstimate.estimate(100, 4096)).isEqualTo(4146);
        assertThat(AiTokenEstimate.estimate(-4, 4096)).isEqualTo(4096);
    }
}
