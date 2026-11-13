package edu.aiplatform.grading.contract.stub;

import edu.aiplatform.grading.contract.GroupSubmissionStatus;
import edu.aiplatform.grading.contract.GroupSubmissionView;
import edu.aiplatform.grading.contract.IGroupEvaluation;
import edu.aiplatform.grading.contract.IGroupSubmissionSource;
import edu.aiplatform.shared.stub.Stubs;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * Bản tạm của interface U09 (và {@code IGroupSubmissionSource} do U12 cài):
 * không tạo đánh giá; nguồn bài nhóm thiếu thì báo unavailable.
 */
@AutoConfiguration
@ConditionalOnProperty(name = "platform.contract-stubs.enabled", havingValue = "true", matchIfMissing = true)
public class GradingContractStubs {

    /** Supplies a no-op group evaluation contract when its provider is unavailable. */
    @Bean
    @ConditionalOnMissingBean
    IGroupEvaluation groupEvaluationStub() {
        return Stubs.announce(IGroupEvaluation.class,
                (groupDocumentId, submissionHash, assignmentId, classId, memberIds) -> Stubs
                        .called(IGroupEvaluation.class, "bỏ qua nộp bài nhóm " + groupDocumentId));
    }

    /** Supplies a group submission source that reports unavailable until implemented. */
    @Bean
    @ConditionalOnMissingBean
    IGroupSubmissionSource groupSubmissionSourceStub() {
        return Stubs.announce(IGroupSubmissionSource.class, new IGroupSubmissionSource() {
            /** Reports that group submission lookup is unavailable. */
            @Override
            public Optional<GroupSubmissionView> getGroupSubmission(UUID groupDocumentId) {
                throw edu.aiplatform.shared.error.ApiException.unavailable(
                        "GROUP_SUBMISSIONS_UNAVAILABLE",
                        "Group submission provider is not implemented");
            }

            /** Reports that group submission listing is unavailable. */
            @Override
            public List<GroupSubmissionStatus> listGroupSubmissions(UUID assignmentId,
                    UUID classId) {
                throw edu.aiplatform.shared.error.ApiException.unavailable(
                        "GROUP_SUBMISSIONS_UNAVAILABLE",
                        "Group submission provider is not implemented");
            }
        });
    }
}
