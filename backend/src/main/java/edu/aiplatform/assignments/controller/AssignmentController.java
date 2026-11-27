package edu.aiplatform.assignments.controller;

import edu.aiplatform.assignments.contract.DiagramPngs;
import edu.aiplatform.assignments.service.AssignmentAuthoringService;
import edu.aiplatform.assignments.service.AssignmentCopyService;
import edu.aiplatform.assignments.service.AssignmentPublicationService;
import edu.aiplatform.assignments.dto.AssignmentViews;
import edu.aiplatform.assignments.service.DocxService;
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

    private final AssignmentAuthoringService assignmentAuthoringService;
    private final AssignmentPublicationService assignmentPublicationService;
    private final AssignmentCopyService assignmentCopyService;
    private final DocxService docxService;

    public AssignmentController(AssignmentAuthoringService assignmentAuthoringService, AssignmentPublicationService assignmentPublicationService,
                                AssignmentCopyService assignmentCopyService, DocxService docxService) {
        this.assignmentAuthoringService = assignmentAuthoringService;
        this.assignmentPublicationService = assignmentPublicationService;
        this.assignmentCopyService = assignmentCopyService;
        this.docxService = docxService;
    }

    @GetMapping
    public AssignmentViews.Detail loadAssignment(@AuthenticationPrincipal Actor actor, @PathVariable UUID id) {
        return assignmentAuthoringService.loadAssignment(actor, id);
    }

    @PatchMapping(consumes = "application/json")
    public AssignmentViews.Detail updateDetails(@AuthenticationPrincipal Actor actor, @PathVariable UUID id,
                                         @RequestBody AssignmentRequests.Details body) {
        return assignmentAuthoringService.updateDetails(actor, id, body.buildCommand());
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteAssignment(@AuthenticationPrincipal Actor actor, @PathVariable UUID id,
                                       @RequestParam Long version) {
        assignmentAuthoringService.deleteAssignment(actor, id, AssignmentRequests.requireVersion(version));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/selectable-materials")
    public List<AssignmentViews.Material> listMaterials(@AuthenticationPrincipal Actor actor, @PathVariable UUID id) {
        return assignmentAuthoringService.listSelectableMaterials(actor, id);
    }

    @PutMapping(value = "/skeleton", consumes = "application/json")
    public AssignmentViews.Detail applySkeleton(@AuthenticationPrincipal Actor actor, @PathVariable UUID id,
                                           @RequestBody AssignmentRequests.Skeleton body) {
        return assignmentAuthoringService.saveSkeleton(actor, id, body.buildCommand());
    }

    @PostMapping(value = "/skeleton:import-docx", consumes = "multipart/form-data")
    public AssignmentViews.ImportPreview importDocx(@AuthenticationPrincipal Actor actor, @PathVariable UUID id,
                                                    @RequestPart("file") MultipartFile file) {
        return docxService.importAssignment(actor, id, readBytes(file));
    }

    @GetMapping("/docx")
    public ResponseEntity<byte[]> exportDocx(@AuthenticationPrincipal Actor actor, @PathVariable UUID id) {
        return download(docxService.exportAssignment(actor, id));
    }

    /** Như GET, kèm ảnh PNG của sơ đồ chưa có ảnh lưu sẵn do trình duyệt vẽ (yêu cầu 2026-11-27). */
    @PostMapping("/docx")
    public ResponseEntity<byte[]> exportDocxWithDiagrams(@AuthenticationPrincipal Actor actor, @PathVariable UUID id,
                                                         @RequestBody(required = false) DiagramPngs.Body body) {
        return download(docxService.exportAssignment(actor, id, DiagramPngs.Body.read(body)));
    }


    /** Rubric các phần thành tệp DOCX riêng, tách khỏi tệp đề (yêu cầu 2026-10-22). */
    @GetMapping("/rubric/docx")
    public ResponseEntity<byte[]> exportRubric(@AuthenticationPrincipal Actor actor, @PathVariable UUID id) {
        return download(docxService.exportRubric(actor, id));
    }

    private static ResponseEntity<byte[]> download(DocxService.Export export) {
        return ResponseEntity.ok().contentType(DOCX).cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(export.fileName(), StandardCharsets.UTF_8).build().toString())
                .body(export.bytes());
    }

    @GetMapping("/diagrams/{blockId}")
    public AssignmentViews.Diagram loadDiagram(@AuthenticationPrincipal Actor actor, @PathVariable UUID id,
                                           @PathVariable UUID blockId) {
        return assignmentAuthoringService.loadDiagram(actor, id, blockId);
    }

    /** Ảnh trong khung: kiểm quyền rồi chuyển tới link tải 5 phút của U03. */
    @GetMapping("/blocks/{blockId}/image")
    public ResponseEntity<Void> redirectImage(@AuthenticationPrincipal Actor actor, @PathVariable UUID id,
                                      @PathVariable UUID blockId) {
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(assignmentAuthoringService.resolveImageUrl(actor, id, blockId)))
                .cacheControl(CacheControl.noStore()).build();
    }

    @PostMapping(value = "/review", consumes = "application/json")
    public AssignmentViews.Detail review(@AuthenticationPrincipal Actor actor, @PathVariable UUID id,
                                         @RequestBody AssignmentRequests.Version body) {
        return assignmentAuthoringService.review(actor, id, body.readVersion());
    }

    @PostMapping(value = "/publish", consumes = "application/json")
    public AssignmentViews.Detail publish(@AuthenticationPrincipal Actor actor, @PathVariable UUID id,
                                          @RequestBody AssignmentRequests.Publish body) {
        return assignmentPublicationService.publish(actor, id, body.buildCommand());
    }

    @PatchMapping(value = "/schedule", consumes = "application/json")
    public AssignmentViews.Detail schedule(@AuthenticationPrincipal Actor actor, @PathVariable UUID id,
                                           @RequestBody AssignmentRequests.Schedule body) {
        return assignmentPublicationService.changeSchedule(actor, id, body.buildCommand());
    }

    @PostMapping(value = "/retire", consumes = "application/json")
    public AssignmentViews.Detail retire(@AuthenticationPrincipal Actor actor, @PathVariable UUID id,
                                         @RequestBody AssignmentRequests.Retire body) {
        return assignmentPublicationService.retire(actor, id, body.buildCommand());
    }

    @PostMapping(value = "/close", consumes = "application/json")
    public AssignmentViews.Detail closeAssignment(@AuthenticationPrincipal Actor actor,
            @PathVariable UUID id, @RequestBody AssignmentRequests.Close body) {
        return assignmentPublicationService.closeAssignment(actor, id, body.readVersion());
    }

    @PostMapping("/clone")
    public ResponseEntity<AssignmentViews.Detail> cloneAssignment(@AuthenticationPrincipal Actor actor,
                                                                  @PathVariable UUID id) {
        AssignmentViews.Detail created = assignmentCopyService.cloneAssignment(actor, id);
        return ResponseEntity.created(URI.create("/api/v1/assignments/" + created.id())).body(created);
    }

    @PostMapping("/versions")
    public ResponseEntity<AssignmentViews.Detail> createNewVersion(@AuthenticationPrincipal Actor actor,
                                                             @PathVariable UUID id) {
        AssignmentViews.Detail created = assignmentCopyService.createNewVersion(actor, id);
        return ResponseEntity.created(URI.create("/api/v1/assignments/" + created.id())).body(created);
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
