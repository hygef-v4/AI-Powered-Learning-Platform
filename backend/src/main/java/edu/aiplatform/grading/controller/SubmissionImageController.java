package edu.aiplatform.grading.controller;

import edu.aiplatform.files.contract.DownloadToken;
import edu.aiplatform.grading.service.SubmissionImageService;
import edu.aiplatform.identity.contract.Actor;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/evaluations/{evaluationId}/images")
public class SubmissionImageController {
    private final SubmissionImageService submissionImageService;

    public SubmissionImageController(SubmissionImageService submissionImageService) {
        this.submissionImageService = submissionImageService;
    }

    @GetMapping("/{fileId}")
    public DownloadToken issueDownloadToken(@AuthenticationPrincipal Actor actor,
            @PathVariable UUID evaluationId, @PathVariable String fileId) {
        return submissionImageService.issueDownloadToken(actor, evaluationId, fileId);
    }
}
