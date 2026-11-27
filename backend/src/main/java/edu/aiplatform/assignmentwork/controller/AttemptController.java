package edu.aiplatform.assignmentwork.controller;

import edu.aiplatform.assignments.contract.DiagramPngs;
import edu.aiplatform.ai.contract.AiProvider;
import edu.aiplatform.assignments.contract.DocumentModel;
import edu.aiplatform.assignmentwork.service.ActivityService;
import edu.aiplatform.assignmentwork.service.AttemptService;
import edu.aiplatform.assignmentwork.service.PracticeFeedbackService;
import edu.aiplatform.assignmentwork.service.StudentAssignmentService;
import edu.aiplatform.assignmentwork.dto.WorkViews;
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

    private final AttemptService attemptService;
    private final StudentAssignmentService studentAssignmentService;
    private final PracticeFeedbackService practiceFeedbackService;
    private final ActivityService activityService;

    public AttemptController(AttemptService attemptService, StudentAssignmentService studentAssignmentService,
                             PracticeFeedbackService practiceFeedbackService, ActivityService activityService) {
        this.attemptService = attemptService;
        this.studentAssignmentService = studentAssignmentService;
        this.practiceFeedbackService = practiceFeedbackService;
        this.activityService = activityService;
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
        AttemptService.Started started = attemptService.start(actor, assignmentId, body.classId());
        if (started.created()) {
            return ResponseEntity.created(URI.create("/api/v1/attempts/" + started.attempt().attemptId()))
                    .body(started.attempt());
        }
        return ResponseEntity.ok(started.attempt());
    }

    @GetMapping("/api/v1/assignments/{assignmentId}/attempts/mine")
    public List<WorkViews.AttemptSummary> listMyAttempts(@AuthenticationPrincipal Actor actor, @PathVariable UUID assignmentId,
                                               @RequestParam UUID classId) {
        return studentAssignmentService.listHistory(actor, assignmentId, classId);
    }

    @GetMapping("/api/v1/attempts/{attemptId}")
    public WorkViews.Attempt loadAttempt(@AuthenticationPrincipal Actor actor, @PathVariable UUID attemptId) {
        return attemptService.loadAttempt(actor, attemptId);
    }

    @PutMapping(value = "/api/v1/attempts/{attemptId}/content", consumes = "application/json")
    public WorkViews.Saved saveAttempt(@AuthenticationPrincipal Actor actor, @PathVariable UUID attemptId,
                                @RequestBody SaveRequest body) {
        return attemptService.saveAttempt(actor, attemptId, resolveContentVersion(body.contentVersion()), body.content());
    }

    @PostMapping(value = "/api/v1/attempts/{attemptId}/docx:preview", consumes = "multipart/form-data")
    public WorkViews.ImportPreview importDocx(@AuthenticationPrincipal Actor actor, @PathVariable UUID attemptId,
                                              @RequestPart("file") MultipartFile file) {
        return attemptService.importPreview(actor, attemptId, readBytes(file));
    }

    @PostMapping(value = "/api/v1/attempts/{attemptId}/submit", consumes = "application/json")
    public WorkViews.Receipt submit(@AuthenticationPrincipal Actor actor, @PathVariable UUID attemptId,
                                    @RequestBody SubmitRequest body) {
        return attemptService.submit(actor, attemptId, resolveContentVersion(body.contentVersion()));
    }

    @GetMapping("/api/v1/attempts/{attemptId}/receipt")
    public WorkViews.Receipt loadReceipt(@AuthenticationPrincipal Actor actor, @PathVariable UUID attemptId) {
        return attemptService.loadReceipt(actor, attemptId);
    }

    @GetMapping("/api/v1/attempts/{attemptId}/export.docx")
    public ResponseEntity<byte[]> export(@AuthenticationPrincipal Actor actor, @PathVariable UUID attemptId) {
        return exportWithDiagrams(actor, attemptId, null);
    }

    /** Như GET, kèm ảnh PNG của sơ đồ chưa có ảnh lưu sẵn do trình duyệt vẽ (yêu cầu 2026-11-27). */
    @PostMapping("/api/v1/attempts/{attemptId}/export.docx")
    public ResponseEntity<byte[]> exportWithDiagrams(@AuthenticationPrincipal Actor actor, @PathVariable UUID attemptId,
            @RequestBody(required = false) DiagramPngs.Body body) {
        AttemptService.Export export = attemptService.export(actor, attemptId,
                DiagramPngs.Body.read(body));
        return ResponseEntity.ok().contentType(DOCX).cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(export.fileName(), StandardCharsets.UTF_8).build().toString())
                .body(export.bytes());
    }

    @GetMapping("/api/v1/attempts/{attemptId}/blocks/{blockId}/image")
    public ResponseEntity<Void> redirectImage(@AuthenticationPrincipal Actor actor, @PathVariable UUID attemptId,
                                      @PathVariable UUID blockId) {
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(attemptService.resolveImageUrl(actor, attemptId, blockId)))
                .cacheControl(CacheControl.noStore()).build();
    }

    @GetMapping("/api/v1/attempts/{attemptId}/diagrams/{blockId}")
    public WorkViews.Diagram loadDiagram(@AuthenticationPrincipal Actor actor, @PathVariable UUID attemptId,
                                     @PathVariable UUID blockId) {
        return attemptService.loadDiagram(actor, attemptId, blockId);
    }

    @PostMapping(value = "/api/v1/attempts/{attemptId}/feedback", consumes = "application/json")
    public WorkViews.Feedback requestFeedback(@AuthenticationPrincipal Actor actor, @PathVariable UUID attemptId,
                                              @RequestBody FeedbackRequest body) {
        return practiceFeedbackService.request(actor, attemptId, body.provider(), body.requestRef());
    }

    @GetMapping("/api/v1/attempts/{attemptId}/feedback")
    public List<WorkViews.Feedback> listFeedback(@AuthenticationPrincipal Actor actor, @PathVariable UUID attemptId) {
        return practiceFeedbackService.listFeedback(actor, attemptId);
    }

    @PostMapping(value = "/api/v1/attempts/{attemptId}/activity", consumes = "application/json")
    public Map<String, Integer> recordActivity(@AuthenticationPrincipal Actor actor, @PathVariable UUID attemptId,
                                         @RequestBody ActivityRequest body) {
        return Map.of("accepted", activityService.recordForAttempt(actor, attemptId, body.events()));
    }

    static long resolveContentVersion(Long value) {
        if (value == null || value < 0) {
            throw ApiException.createBadRequest("CONTENT_VERSION_REQUIRED", "Thiếu phiên bản của bài làm");
        }
        return value;
    }

    static byte[] readBytes(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw ApiException.createBadRequest("DOCX_INVALID", "Chưa chọn tệp DOCX");
        }
        if (file.getSize() > 20L * 1024 * 1024) {
            throw ApiException.createPayloadTooLarge("DOCX_TOO_LARGE", "Tệp DOCX tối đa 20 MB");
        }
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw ApiException.createBadRequest("DOCX_INVALID", "Không đọc được tệp DOCX");
        }
    }
}
