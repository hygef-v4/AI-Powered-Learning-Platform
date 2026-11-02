package edu.aiplatform.grading.service;

import edu.aiplatform.grading.dto.GradingViews;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.*;
import edu.aiplatform.assignments.contract.*;
import edu.aiplatform.files.contract.*;
import edu.aiplatform.identity.contract.*;
import com.fasterxml.jackson.databind.ObjectMapper;

class SubmissionImageServiceTest {
    @Test
    void onlyFilesInAuthorizedSubmittedSnapshotReceiveDownloadTokens() throws Exception {
        var review = mock(SubmissionReviewService.class);
        var files = mock(IArtifact.class);
        var eid = UUID.randomUUID();
        var actor = new Actor(UUID.randomUUID(), Role.TEACHER);
        var block = new DocumentBlock(UUID.randomUUID(), BlockOrigin.values()[0], BlockType.IMAGE,
                new ObjectMapper().readTree("{\"fileId\":\"attached\"}"));
        when(review.detail(actor, eid))
                .thenReturn(new GradingViews.EvaluationDetail(null, new DocumentModel(List.of(block)), List.of(),
                        List.of(), null, "DISABLED", List.of(), List.of(), List.of(), true));
        var service = new SubmissionImageService(review, files);
        assertThatThrownBy(() -> service.download(actor, eid, "other")).isInstanceOf(RuntimeException.class);
        verifyNoInteractions(files);
        service.download(actor, eid, "attached");
        verify(files).issueDownloadToken("attached", actor.accountId());
    }
}
