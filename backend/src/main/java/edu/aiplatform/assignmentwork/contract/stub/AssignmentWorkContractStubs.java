package edu.aiplatform.assignmentwork.contract.stub;

import edu.aiplatform.assignments.contract.DocumentModel;
import edu.aiplatform.assignmentwork.contract.ActivityPolicyState;
import edu.aiplatform.assignmentwork.contract.ActivitySummary;
import edu.aiplatform.assignmentwork.contract.IActivityQuery;
import edu.aiplatform.assignmentwork.contract.IActivityWrite;
import edu.aiplatform.assignmentwork.contract.IGroupWorkStatus;
import edu.aiplatform.assignmentwork.contract.ISubmissionQuery;
import edu.aiplatform.assignmentwork.contract.StudentSubmissionStatus;
import edu.aiplatform.assignmentwork.contract.SubmissionView;
import edu.aiplatform.assignmentwork.contract.SubmitMode;
import edu.aiplatform.shared.stub.Stubs;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * Bản tạm của interface U08 (và {@code IGroupWorkStatus} do U12 cài): chưa có ai nộp, bài nộp mẫu rỗng theo ID,
 * chưa có dữ liệu rời trang, chưa có bài nhóm.
 */
@AutoConfiguration
@ConditionalOnProperty(name = "platform.contract-stubs.enabled", havingValue = "true", matchIfMissing = true)
public class AssignmentWorkContractStubs {

    @Bean
    @ConditionalOnMissingBean
    ISubmissionQuery submissionQueryStub() {
        return Stubs.announce(ISubmissionQuery.class, new ISubmissionQuery() {
            @Override
            public List<StudentSubmissionStatus> listSubmissions(UUID assignmentId, UUID classId) {
                return List.of();
            }

            @Override
            public Optional<SubmissionView> getSubmission(UUID attemptId) {
                return Optional.of(new SubmissionView(attemptId, Stubs.derivedId("assignment", attemptId),
                        Stubs.derivedId("class", attemptId), Stubs.derivedId("account", attemptId),
                        new DocumentModel(List.of()), "", Instant.now(), false, SubmitMode.MANUAL));
            }

            @Override
            public List<UUID> listStudentsWithoutSubmission(UUID assignmentId, UUID classId) {
                return List.of();
            }
        });
    }

    @Bean
    @ConditionalOnMissingBean
    IActivityWrite activityWriteStub() {
        return Stubs.announce(IActivityWrite.class, (actor, workRef, events) -> {
        });
    }

    @Bean
    @ConditionalOnMissingBean
    IActivityQuery activityQueryStub() {
        return Stubs.announce(IActivityQuery.class, (workRef, accountId) -> new ActivitySummary(
                ActivityPolicyState.NO_DATA, 0, Duration.ZERO, List.of(), 0, "Chưa có dữ liệu"));
    }

    @Bean
    @ConditionalOnMissingBean
    IGroupWorkStatus groupWorkStatusStub() {
        return Stubs.announce(IGroupWorkStatus.class, (accountId, assignmentIds) -> Map.of());
    }
}
