package edu.aiplatform.grading.contract.stub;

import edu.aiplatform.assignments.contract.DocumentModel;
import edu.aiplatform.grading.contract.GroupSubmissionStatus;
import edu.aiplatform.grading.contract.GroupSubmissionView;
import edu.aiplatform.grading.contract.IGroupEvaluation;
import edu.aiplatform.grading.contract.IGroupSubmissionSource;
import edu.aiplatform.shared.stub.Stubs;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/** Bản tạm của interface U09 (và {@code IGroupSubmissionSource} do U12 cài): không tạo đánh giá, bài nhóm mẫu rỗng. */
@AutoConfiguration
@ConditionalOnProperty(name = "platform.contract-stubs.enabled", havingValue = "true", matchIfMissing = true)
public class GradingContractStubs {

    @Bean
    @ConditionalOnMissingBean
    IGroupEvaluation groupEvaluationStub() {
        return Stubs.announce(IGroupEvaluation.class, (groupDocumentId, submissionHash, assignmentId, classId, memberIds) ->
                Stubs.called(IGroupEvaluation.class, "bỏ qua nộp bài nhóm " + groupDocumentId));
    }

    @Bean
    @ConditionalOnMissingBean
    IGroupSubmissionSource groupSubmissionSourceStub() {
        return Stubs.announce(IGroupSubmissionSource.class, new IGroupSubmissionSource() {
            @Override
            public Optional<GroupSubmissionView> getGroupSubmission(UUID groupDocumentId) {
                return Optional.of(new GroupSubmissionView(groupDocumentId, Stubs.derivedId("group", groupDocumentId),
                        Stubs.derivedId("assignment", groupDocumentId), Stubs.derivedId("class", groupDocumentId),
                        new DocumentModel(List.of()), List.of(), List.of(), Instant.now(), false, ""));
            }

            @Override
            public List<GroupSubmissionStatus> listGroupSubmissions(UUID assignmentId, UUID classId) {
                return List.of();
            }
        });
    }
}
