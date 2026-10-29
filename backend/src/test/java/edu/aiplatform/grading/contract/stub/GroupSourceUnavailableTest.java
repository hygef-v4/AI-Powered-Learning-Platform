package edu.aiplatform.grading.contract.stub;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import java.util.UUID;
import edu.aiplatform.shared.error.ApiException;

class GroupSourceUnavailableTest {
    @Test
    void missingGroupProviderCannotLookLikeSuccessfulEmptyData() {
        var source = new GradingContractStubs().groupSubmissionSourceStub();
        assertThatThrownBy(() -> source.listGroupSubmissions(UUID.randomUUID(), UUID.randomUUID()))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.status().value()).isEqualTo(503));
        assertThatThrownBy(() -> source.getGroupSubmission(UUID.randomUUID()))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.status().value()).isEqualTo(503));
    }
}
