package edu.aiplatform.groupdocs.controller;

import edu.aiplatform.assignments.contract.DocumentBlock;
import edu.aiplatform.assignmentwork.contract.ActivityEventInput;
import edu.aiplatform.groupdocs.service.GroupDocAccess;
import edu.aiplatform.groupdocs.service.GroupSectionService;
import edu.aiplatform.groupdocs.service.GroupSectionService.SectionView;
import edu.aiplatform.groupdocs.service.GroupSubmissionService;
import edu.aiplatform.groupdocs.service.GroupSubmissionService.Export;
import edu.aiplatform.groupdocs.service.GroupSubmissionService.Receipt;
import edu.aiplatform.groupdocs.service.GroupWorkQueryService;
import edu.aiplatform.groupdocs.service.GroupWorkspaceService;
import edu.aiplatform.groupdocs.service.GroupWorkspaceService.WorkspaceView;
import edu.aiplatform.groupdocs.service.IGroupDocStreams;
import edu.aiplatform.identity.contract.Actor;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** UC 56: workspace tài liệu nhóm, mục, nộp, DOCX, kênh SSE và dữ liệu hoạt động. */
@RestController
public class GroupDocController {

    static final MediaType DOCX = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

    private final GroupWorkspaceService workspace;
    private final GroupSectionService sections;
    private final GroupSubmissionService submissions;
    private final GroupWorkQueryService work;
    private final GroupDocAccess access;
    private final IGroupDocStreams streams;

    public GroupDocController(GroupWorkspaceService workspace, GroupSectionService sections,
                              GroupSubmissionService submissions, GroupWorkQueryService work, GroupDocAccess access,
                              IGroupDocStreams streams) {
        this.workspace = workspace;
        this.sections = sections;
        this.submissions = submissions;
        this.work = work;
        this.access = access;
        this.streams = streams;
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
    public WorkspaceView myGroupDoc(@AuthenticationPrincipal Actor actor, @PathVariable UUID assignmentId) {
        return workspace.myGroupDocument(actor, assignmentId);
    }

    @GetMapping("/api/v1/group-docs/{id}")
    public WorkspaceView get(@AuthenticationPrincipal Actor actor, @PathVariable UUID id) {
        return workspace.workspace(actor, id);
    }

    /** Kênh SSE chỉ mở cho thành viên hiệu lực và giảng viên của lớp; Nginx không đệm (BR-U12-37). */
    @GetMapping(value = "/api/v1/group-docs/{id}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter events(@AuthenticationPrincipal Actor actor, @PathVariable UUID id, HttpServletResponse response) {
        access.read(actor, id);
        response.setHeader("X-Accel-Buffering", "no");
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache");
        return streams.open(id, actor.accountId());
    }

    @PostMapping("/api/v1/group-docs/{id}/activity")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void activity(@AuthenticationPrincipal Actor actor, @PathVariable UUID id, @RequestBody ActivityRequest request) {
        work.recordActivity(actor, id, request.events());
    }

    @PostMapping("/api/v1/group-docs/{id}/sections/{sectionId}:claim")
    public SectionView claim(@AuthenticationPrincipal Actor actor, @PathVariable UUID id, @PathVariable UUID sectionId) {
        return sections.claim(actor, id, sectionId);
    }

    @PostMapping("/api/v1/group-docs/{id}/sections/{sectionId}:assign")
    public SectionView assign(@AuthenticationPrincipal Actor actor, @PathVariable UUID id, @PathVariable UUID sectionId,
                              @RequestBody AssignRequest request) {
        return sections.assign(actor, id, sectionId, request.assigneeId(), request.replaceHolder());
    }

    @PostMapping("/api/v1/group-docs/{id}/sections/{sectionId}:release")
    public SectionView release(@AuthenticationPrincipal Actor actor, @PathVariable UUID id, @PathVariable UUID sectionId) {
        return sections.release(actor, id, sectionId);
    }

    @PostMapping("/api/v1/group-docs/{id}/sections/{sectionId}:done")
    public SectionView done(@AuthenticationPrincipal Actor actor, @PathVariable UUID id, @PathVariable UUID sectionId) {
        return sections.done(actor, id, sectionId);
    }

    @PutMapping("/api/v1/group-docs/{id}/sections/{sectionId}/draft")
    public SectionView draft(@AuthenticationPrincipal Actor actor, @PathVariable UUID id, @PathVariable UUID sectionId,
                             @RequestBody DraftRequest request) {
        return sections.saveDraft(actor, id, sectionId, request.version(), request.blocks());
    }

    @PostMapping("/api/v1/group-docs/{id}/submit")
    public Receipt submit(@AuthenticationPrincipal Actor actor, @PathVariable UUID id, @RequestBody SubmitRequest request) {
        return submissions.submit(actor, id, request.version());
    }

    @GetMapping("/api/v1/group-docs/{id}/export.docx")
    public ResponseEntity<byte[]> export(@AuthenticationPrincipal Actor actor, @PathVariable UUID id,
                                         @RequestParam(defaultValue = "false") boolean submitted) {
        Export export = submissions.export(actor, id, submitted);
        return ResponseEntity.ok().contentType(DOCX)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(export.title() + ".docx", StandardCharsets.UTF_8).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(export.content());
    }
}
