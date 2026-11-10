package edu.aiplatform.grading.service;

import edu.aiplatform.assignments.contract.BlockType;
import edu.aiplatform.files.contract.DownloadToken;
import edu.aiplatform.files.contract.IArtifact;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.shared.error.ApiException;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class SubmissionImageService {
    private final SubmissionReviewService submissionReviewService;
    private final IArtifact artifacts;

    /** Creates the image access service with review and artifact dependencies. */
    public SubmissionImageService(SubmissionReviewService submissionReviewService,
            IArtifact artifacts) {
        this.submissionReviewService = submissionReviewService;
        this.artifacts = artifacts;
    }

    /** Issues a download token after confirming the image belongs to the evaluation submission. */
    public DownloadToken issueDownloadToken(Actor actor, UUID evaluationId, String fileId) {
        // Step 1: Load evaluation details and verify teacher access
        var detail = submissionReviewService.loadEvaluation(actor, evaluationId);

        // Step 2: Ensure fileId is actually attached as an IMAGE block in the student's submission document
        boolean attached = detail.document().blocks().stream()
                .anyMatch(b -> b.type() == BlockType.IMAGE
                        && fileId.equals(b.content().path("fileId").asText()));
        if (!attached)
            throw ApiException.notFound("SUBMISSION_IMAGE_NOT_FOUND");

        // Step 3: Issue authorized temporary download token from artifact storage
        return artifacts.issueDownloadToken(fileId, actor.accountId());
    }
}
