package edu.aiplatform.assignments.web;

import edu.aiplatform.assignments.application.AssignmentAuthoringService;
import edu.aiplatform.assignments.application.AssignmentCopyService;
import edu.aiplatform.assignments.application.AssignmentPublicationService;
import edu.aiplatform.assignments.application.AssignmentViews;
import edu.aiplatform.assignments.application.DocxService;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.shared.error.ApiException;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Assignment Form (UC 35): đọc, sửa, khung, DOCX, sơ đồ, ảnh, duyệt, phát hành, lịch, ngưng giao, nhân bản. */
@RestController
@RequestMapping("/api/v1/assignments/{id}")
public class AssignmentController {

    static final MediaType DOCX = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

    private final AssignmentAuthoringService authoring;
    private final AssignmentPublicationService publication;
    private final AssignmentCopyService copies;
    private final DocxService docx;

    public AssignmentController(AssignmentAuthoringService authoring, AssignmentPublicationService publication,
                                AssignmentCopyService copies, DocxService docx) {
        this.authoring = authoring;
        this.publication = publication;
        this.copies = copies;
        this.docx = docx;
    }

    @GetMapping
    public AssignmentViews.Detail detail(@AuthenticationPrincipal Actor actor, @PathVariable UUID id) {
        return authoring.detail(actor, id);
    }

    @PatchMapping(consumes = "application/json")
    public AssignmentViews.Detail update(@AuthenticationPrincipal Actor actor, @PathVariable UUID id,
                                         @RequestBody AssignmentRequests.Details body) {
        return authoring.updateDetails(actor, id, body.command());
    }

    @DeleteMapping
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Actor actor, @PathVariable UUID id,
                                       @RequestParam Long version) {
        authoring.delete(actor, id, AssignmentRequests.requireVersion(version));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/selectable-materials")
    public List<AssignmentViews.Material> materials(@AuthenticationPrincipal Actor actor, @PathVariable UUID id) {
        return authoring.selectableMaterials(actor, id);
    }

    @PutMapping(value = "/skeleton", consumes = "application/json")
    public AssignmentViews.Detail skeleton(@AuthenticationPrincipal Actor actor, @PathVariable UUID id,
                                           @RequestBody AssignmentRequests.Skeleton body) {
        return authoring.saveSkeleton(actor, id, body.command());
    }

    @PostMapping(value = "/skeleton:import-docx", consumes = "multipart/form-data")
    public AssignmentViews.ImportPreview importDocx(@AuthenticationPrincipal Actor actor, @PathVariable UUID id,
                                                    @RequestPart("file") MultipartFile file) {
        return docx.importAssignment(actor, id, bytes(file));
    }

    @GetMapping("/docx")
    public ResponseEntity<byte[]> exportDocx(@AuthenticationPrincipal Actor actor, @PathVariable UUID id) {
        return download(docx.exportAssignment(actor, id));
    }

    /** Rubric các phần thành tệp DOCX riêng, tách khỏi tệp đề (yêu cầu 2026-10-22). */
    @GetMapping("/rubric/docx")
    public ResponseEntity<byte[]> exportRubric(@AuthenticationPrincipal Actor actor, @PathVariable UUID id) {
        return download(docx.exportRubric(actor, id));
    }

    private static ResponseEntity<byte[]> download(DocxService.Export export) {
        return ResponseEntity.ok().contentType(DOCX).cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(export.fileName(), StandardCharsets.UTF_8).build().toString())
                .body(export.bytes());
    }

    @GetMapping("/diagrams/{blockId}")
    public AssignmentViews.Diagram diagram(@AuthenticationPrincipal Actor actor, @PathVariable UUID id,
                                           @PathVariable UUID blockId) {
        return authoring.diagram(actor, id, blockId);
    }

    /** Ảnh trong khung: kiểm quyền rồi chuyển tới link tải 5 phút của U03. */
    @GetMapping("/blocks/{blockId}/image")
    public ResponseEntity<Void> image(@AuthenticationPrincipal Actor actor, @PathVariable UUID id,
                                      @PathVariable UUID blockId) {
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(authoring.imageUrl(actor, id, blockId)))
                .cacheControl(CacheControl.noStore()).build();
    }

    @PostMapping(value = "/review", consumes = "application/json")
    public AssignmentViews.Detail review(@AuthenticationPrincipal Actor actor, @PathVariable UUID id,
                                         @RequestBody AssignmentRequests.Version body) {
        return authoring.review(actor, id, body.value());
    }

    @PostMapping(value = "/publish", consumes = "application/json")
    public AssignmentViews.Detail publish(@AuthenticationPrincipal Actor actor, @PathVariable UUID id,
                                          @RequestBody AssignmentRequests.Publish body) {
        return publication.publish(actor, id, body.command());
    }

    @PatchMapping(value = "/schedule", consumes = "application/json")
    public AssignmentViews.Detail schedule(@AuthenticationPrincipal Actor actor, @PathVariable UUID id,
                                           @RequestBody AssignmentRequests.Schedule body) {
        return publication.changeSchedule(actor, id, body.command());
    }

    @PostMapping(value = "/retire", consumes = "application/json")
    public AssignmentViews.Detail retire(@AuthenticationPrincipal Actor actor, @PathVariable UUID id,
                                         @RequestBody AssignmentRequests.Retire body) {
        return publication.retire(actor, id, body.command());
    }

    @PostMapping("/clone")
    public ResponseEntity<AssignmentViews.Detail> cloneAssignment(@AuthenticationPrincipal Actor actor,
                                                                  @PathVariable UUID id) {
        AssignmentViews.Detail created = copies.cloneAssignment(actor, id);
        return ResponseEntity.created(URI.create("/api/v1/assignments/" + created.id())).body(created);
    }

    @PostMapping("/versions")
    public ResponseEntity<AssignmentViews.Detail> newVersion(@AuthenticationPrincipal Actor actor,
                                                             @PathVariable UUID id) {
        AssignmentViews.Detail created = copies.newVersion(actor, id);
        return ResponseEntity.created(URI.create("/api/v1/assignments/" + created.id())).body(created);
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
