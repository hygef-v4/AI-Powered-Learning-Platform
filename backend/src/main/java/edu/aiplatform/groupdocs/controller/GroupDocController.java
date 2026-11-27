package edu.aiplatform.groupdocs.controller;

import edu.aiplatform.assignments.contract.DiagramPngs;
import edu.aiplatform.assignments.contract.DocumentBlock;
import edu.aiplatform.assignmentwork.contract.ActivityEventInput;
import edu.aiplatform.groupdocs.service.GroupDocAccess;
import edu.aiplatform.groupdocs.service.GroupDocImportService;
import edu.aiplatform.groupdocs.service.GroupDocImportService.SectionImport;
import edu.aiplatform.groupdocs.service.GroupSectionService;
import edu.aiplatform.groupdocs.service.GroupSectionService.RevisionView;
import edu.aiplatform.groupdocs.service.GroupSectionService.SectionView;
import edu.aiplatform.groupdocs.service.GroupSubmissionService;
import edu.aiplatform.groupdocs.service.GroupSubmissionService.Export;
import edu.aiplatform.groupdocs.service.GroupSubmissionService.Receipt;
import edu.aiplatform.groupdocs.service.GroupWorkQueryService;
import edu.aiplatform.groupdocs.service.GroupWorkspaceService;
import edu.aiplatform.groupdocs.service.GroupWorkspaceService.WorkspaceView;
import edu.aiplatform.groupdocs.service.IGroupDocStreams;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.shared.error.ApiException;
import java.io.IOException;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** UC 56: workspace tài liệu nhóm, mục, lịch sử mục, nộp, DOCX, kênh SSE và dữ liệu hoạt động. */
@RestController
public class GroupDocController {

    static final MediaType DOCX = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
    static final long MAX_DOCX_BYTES = 20L * 1024 * 1024;

    private final GroupWorkspaceService groupWorkspaceService;
    private final GroupSectionService groupSectionService;
    private final GroupSubmissionService groupSubmissionService;
    private final GroupWorkQueryService groupWorkQueryService;
    private final GroupDocAccess access;
    private final IGroupDocStreams streams;
    private final GroupDocImportService groupDocImportService;

    public GroupDocController(GroupWorkspaceService groupWorkspaceService, GroupSectionService groupSectionService,
                              GroupSubmissionService groupSubmissionService, GroupWorkQueryService groupWorkQueryService, GroupDocAccess access,
                              IGroupDocStreams streams, GroupDocImportService groupDocImportService) {
        this.groupWorkspaceService = groupWorkspaceService;
        this.groupSectionService = groupSectionService;
        this.groupSubmissionService = groupSubmissionService;
        this.groupWorkQueryService = groupWorkQueryService;
        this.access = access;
        this.streams = streams;
        this.groupDocImportService = groupDocImportService;
    }

    public record AssignRequest(UUID assigneeId, boolean replaceHolder) {
    }

    public record DraftRequest(Long version, List<DocumentBlock> blocks) {
    }

    public record SubmitRequest(Long version) {
    }

    public record ActivityRequest(List<ActivityEventInput> events) {
    }

    @GetMapping("/api/v1/assignments/{assignmentId}/my-group-doc")
    public WorkspaceView loadMyGroupDoc(@AuthenticationPrincipal Actor actor, @PathVariable UUID assignmentId) {
        return groupWorkspaceService.loadMyGroupDocument(actor, assignmentId);
    }

    @GetMapping("/api/v1/group-docs/{id}")
    public WorkspaceView loadWorkspace(@AuthenticationPrincipal Actor actor, @PathVariable UUID id) {
        return groupWorkspaceService.loadWorkspace(actor, id);
    }

    /** Kênh SSE chỉ mở cho thành viên hiệu lực và giảng viên của lớp; Nginx không đệm (BR-U12-37). */
    @GetMapping(value = "/api/v1/group-docs/{id}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeEvents(@AuthenticationPrincipal Actor actor, @PathVariable UUID id, HttpServletResponse response) {
        access.loadContext(actor, id);
        response.setHeader("X-Accel-Buffering", "no");
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache");
        return streams.open(id, actor.accountId());
    }

    @PostMapping("/api/v1/group-docs/{id}/activity")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void recordActivity(@AuthenticationPrincipal Actor actor, @PathVariable UUID id, @RequestBody ActivityRequest request) {
        groupWorkQueryService.recordActivity(actor, id, request.events());
    }

    @PostMapping("/api/v1/group-docs/{id}/sections/{sectionId}:claim")
    public SectionView claim(@AuthenticationPrincipal Actor actor, @PathVariable UUID id, @PathVariable UUID sectionId) {
        return groupSectionService.claim(actor, id, sectionId);
    }

    @PostMapping("/api/v1/group-docs/{id}/sections/{sectionId}:assign")
    public SectionView assign(@AuthenticationPrincipal Actor actor, @PathVariable UUID id, @PathVariable UUID sectionId,
                              @RequestBody AssignRequest request) {
        return groupSectionService.assign(actor, id, sectionId, request.assigneeId(), request.replaceHolder());
    }

    @PostMapping("/api/v1/group-docs/{id}/sections/{sectionId}:release")
    public SectionView release(@AuthenticationPrincipal Actor actor, @PathVariable UUID id, @PathVariable UUID sectionId) {
        return groupSectionService.release(actor, id, sectionId);
    }

    @PostMapping("/api/v1/group-docs/{id}/sections/{sectionId}:done")
    public SectionView markSectionDone(@AuthenticationPrincipal Actor actor, @PathVariable UUID id, @PathVariable UUID sectionId) {
        return groupSectionService.markSectionDone(actor, id, sectionId);
    }

    @GetMapping("/api/v1/group-docs/{id}/sections/{sectionId}/revisions")
    public List<RevisionView> listRevisions(@AuthenticationPrincipal Actor actor, @PathVariable UUID id,
                                        @PathVariable UUID sectionId) {
        return groupSectionService.listRevisions(actor, id, sectionId);
    }

    /** Người giữ xem trước DOCX để chèn vào mục đang soạn (BR-U12-34, như trình soạn bài cá nhân). */
    @PostMapping(value = "/api/v1/group-docs/{id}/sections/{sectionId}/docx:preview", consumes = "multipart/form-data")
    public SectionImport previewSectionDocx(@AuthenticationPrincipal Actor actor, @PathVariable UUID id,
                                            @PathVariable UUID sectionId, @RequestPart("file") MultipartFile file) {
        return groupDocImportService.previewSection(actor, id, sectionId, readDocx(file));
    }

    @PutMapping("/api/v1/group-docs/{id}/sections/{sectionId}/draft")
    public SectionView draft(@AuthenticationPrincipal Actor actor, @PathVariable UUID id, @PathVariable UUID sectionId,
                             @RequestBody DraftRequest request) {
        return groupSectionService.saveDraft(actor, id, sectionId, request.version(), request.blocks());
    }

    @PostMapping("/api/v1/group-docs/{id}/submit")
    public Receipt submit(@AuthenticationPrincipal Actor actor, @PathVariable UUID id, @RequestBody SubmitRequest request) {
        return groupSubmissionService.submit(actor, id, request.version());
    }

    @GetMapping("/api/v1/group-docs/{id}/export.docx")
    public ResponseEntity<byte[]> export(@AuthenticationPrincipal Actor actor, @PathVariable UUID id,
                                         @RequestParam(defaultValue = "false") boolean submitted) {
        return exportWithDiagrams(actor, id, submitted, null);
    }

    /** Như GET, kèm ảnh PNG của sơ đồ chưa có ảnh lưu sẵn do trình duyệt vẽ (yêu cầu 2026-11-27). */
    @PostMapping("/api/v1/group-docs/{id}/export.docx")
    public ResponseEntity<byte[]> exportWithDiagrams(@AuthenticationPrincipal Actor actor, @PathVariable UUID id,
            @RequestParam(defaultValue = "false") boolean submitted,
            @RequestBody(required = false) DiagramPngs.Body body) {
        Export export = groupSubmissionService.export(actor, id, submitted,
                DiagramPngs.Body.read(body));
        return ResponseEntity.ok().contentType(DOCX)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(export.title() + ".docx", StandardCharsets.UTF_8).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(export.content());
    }

    private static byte[] readDocx(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw ApiException.createBadRequest("IMPORT_FILE_INVALID", "Chưa chọn tệp DOCX");
        }
        if (file.getSize() > MAX_DOCX_BYTES) {
            throw ApiException.createPayloadTooLarge("IMPORT_FILE_TOO_LARGE", "Tệp DOCX tối đa 20 MB");
        }
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw ApiException.createBadRequest("IMPORT_FILE_INVALID", "Không đọc được tệp DOCX");
        }
    }
}
