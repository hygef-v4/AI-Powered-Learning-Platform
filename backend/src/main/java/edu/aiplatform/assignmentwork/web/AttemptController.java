package edu.aiplatform.assignmentwork.web;

import edu.aiplatform.ai.contract.AiProvider;
import edu.aiplatform.assignments.contract.DocumentModel;
import edu.aiplatform.assignmentwork.application.ActivityService;
import edu.aiplatform.assignmentwork.application.AttemptService;
import edu.aiplatform.assignmentwork.application.PracticeFeedbackService;
import edu.aiplatform.assignmentwork.application.StudentAssignmentService;
import edu.aiplatform.assignmentwork.application.WorkViews;
import edu.aiplatform.assignmentwork.contract.ActivityEventInput;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.shared.error.ApiException;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Lượt làm bài (UC 39–41): bắt đầu, làm và tự lưu, nhập/xuất DOCX, nộp, lịch sử, góp ý Practice, hoạt động. */
@RestController
public class AttemptController {

    static final MediaType DOCX = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

    private final AttemptService attempts;
    private final StudentAssignmentService assignments;
    private final PracticeFeedbackService feedback;
    private final ActivityService activity;

    public AttemptController(AttemptService attempts, StudentAssignmentService assignments,
                             PracticeFeedbackService feedback, ActivityService activity) {
        this.attempts = attempts;
        this.assignments = assignments;
        this.feedback = feedback;
        this.activity = activity;
    }

    record StartRequest(UUID classId) {
    }

    record SaveRequest(Long contentVersion, DocumentModel content) {
    }

    record SubmitRequest(Long contentVersion) {
    }

    record FeedbackRequest(AiProvider provider, String requestRef) {
    }

    record ActivityRequest(List<ActivityEventInput> events) {
    }

    @PostMapping(value = "/api/v1/assignments/{assignmentId}/attempts", consumes = "application/json")
    public ResponseEntity<WorkViews.Attempt> start(@AuthenticationPrincipal Actor actor,
                                                   @PathVariable UUID assignmentId,
                                                   @RequestBody StartRequest body) {
        AttemptService.Started started = attempts.start(actor, assignmentId, body.classId());
        if (started.created()) {
            return ResponseEntity.created(URI.create("/api/v1/attempts/" + started.attempt().attemptId()))
                    .body(started.attempt());
        }
        return ResponseEntity.ok(started.attempt());
    }

    @GetMapping("/api/v1/assignments/{assignmentId}/attempts/mine")
    public List<WorkViews.AttemptSummary> mine(@AuthenticationPrincipal Actor actor, @PathVariable UUID assignmentId,
                                               @RequestParam UUID classId) {
        return assignments.history(actor, assignmentId, classId);
    }

    @GetMapping("/api/v1/attempts/{attemptId}")
    public WorkViews.Attempt get(@AuthenticationPrincipal Actor actor, @PathVariable UUID attemptId) {
        return attempts.get(actor, attemptId);
    }

    @PutMapping(value = "/api/v1/attempts/{attemptId}/content", consumes = "application/json")
    public WorkViews.Saved save(@AuthenticationPrincipal Actor actor, @PathVariable UUID attemptId,
                                @RequestBody SaveRequest body) {
        return attempts.save(actor, attemptId, contentVersion(body.contentVersion()), body.content());
    }

    @PostMapping(value = "/api/v1/attempts/{attemptId}/docx:preview", consumes = "multipart/form-data")
    public WorkViews.ImportPreview importDocx(@AuthenticationPrincipal Actor actor, @PathVariable UUID attemptId,
                                              @RequestPart("file") MultipartFile file) {
        return attempts.importPreview(actor, attemptId, bytes(file));
    }

    @PostMapping(value = "/api/v1/attempts/{attemptId}/submit", consumes = "application/json")
    public WorkViews.Receipt submit(@AuthenticationPrincipal Actor actor, @PathVariable UUID attemptId,
                                    @RequestBody SubmitRequest body) {
        return attempts.submit(actor, attemptId, contentVersion(body.contentVersion()));
    }

    @GetMapping("/api/v1/attempts/{attemptId}/receipt")
    public WorkViews.Receipt receipt(@AuthenticationPrincipal Actor actor, @PathVariable UUID attemptId) {
        return attempts.receipt(actor, attemptId);
    }

    @GetMapping("/api/v1/attempts/{attemptId}/export.docx")
    public ResponseEntity<byte[]> export(@AuthenticationPrincipal Actor actor, @PathVariable UUID attemptId) {
        AttemptService.Export export = attempts.export(actor, attemptId);
        return ResponseEntity.ok().contentType(DOCX).cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(export.fileName(), StandardCharsets.UTF_8).build().toString())
                .body(export.bytes());
    }

    @GetMapping("/api/v1/attempts/{attemptId}/blocks/{blockId}/image")
    public ResponseEntity<Void> image(@AuthenticationPrincipal Actor actor, @PathVariable UUID attemptId,
                                      @PathVariable UUID blockId) {
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(attempts.imageUrl(actor, attemptId, blockId)))
                .cacheControl(CacheControl.noStore()).build();
    }

    @GetMapping("/api/v1/attempts/{attemptId}/diagrams/{blockId}")
    public WorkViews.Diagram diagram(@AuthenticationPrincipal Actor actor, @PathVariable UUID attemptId,
                                     @PathVariable UUID blockId) {
        return attempts.diagram(actor, attemptId, blockId);
    }

    @PostMapping(value = "/api/v1/attempts/{attemptId}/feedback", consumes = "application/json")
    public WorkViews.Feedback requestFeedback(@AuthenticationPrincipal Actor actor, @PathVariable UUID attemptId,
                                              @RequestBody FeedbackRequest body) {
        return feedback.request(actor, attemptId, body.provider(), body.requestRef());
    }

    @GetMapping("/api/v1/attempts/{attemptId}/feedback")
    public List<WorkViews.Feedback> listFeedback(@AuthenticationPrincipal Actor actor, @PathVariable UUID attemptId) {
        return feedback.list(actor, attemptId);
    }

    @PostMapping(value = "/api/v1/attempts/{attemptId}/activity", consumes = "application/json")
    public Map<String, Integer> activity(@AuthenticationPrincipal Actor actor, @PathVariable UUID attemptId,
                                         @RequestBody ActivityRequest body) {
        return Map.of("accepted", activity.recordForAttempt(actor, attemptId, body.events()));
    }

    static long contentVersion(Long value) {
        if (value == null || value < 0) {
            throw ApiException.badRequest("CONTENT_VERSION_REQUIRED", "Thiếu phiên bản của bài làm");
        }
        return value;
    }

    static byte[] bytes(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("DOCX_INVALID", "Chưa chọn tệp DOCX");
        }
        if (file.getSize() > 20L * 1024 * 1024) {
            throw ApiException.payloadTooLarge("DOCX_TOO_LARGE", "Tệp DOCX tối đa 20 MB");
        }
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw ApiException.badRequest("DOCX_INVALID", "Không đọc được tệp DOCX");
        }
    }
}
