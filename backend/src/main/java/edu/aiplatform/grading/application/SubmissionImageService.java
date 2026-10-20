package edu.aiplatform.grading.application;

import edu.aiplatform.assignments.contract.BlockType;
import edu.aiplatform.files.contract.DownloadToken;
import edu.aiplatform.files.contract.IArtifact;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.shared.error.ApiException;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class SubmissionImageService {
    private final SubmissionReviewService review;
    private final IArtifact artifacts;

    public SubmissionImageService(SubmissionReviewService review, IArtifact artifacts) {
        this.review = review;
        this.artifacts = artifacts;
    }

    public DownloadToken download(Actor actor, UUID evaluationId, String fileId) {
        var detail = review.detail(actor, evaluationId);
        boolean attached = detail.document().blocks().stream()
                .anyMatch(b -> b.type() == BlockType.IMAGE && fileId.equals(b.content().path("fileId").asText()));
        if (!attached)
            throw ApiException.notFound("SUBMISSION_IMAGE_NOT_FOUND");
        return artifacts.issueDownloadToken(fileId, actor.accountId());
    }
}
