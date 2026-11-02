package edu.aiplatform.groupdocs.controller;

import edu.aiplatform.groupdocs.service.GroupDocImportService;
import edu.aiplatform.groupdocs.service.GroupDocImportService.ApplyItem;
import edu.aiplatform.groupdocs.service.GroupDocImportService.ApplyResult;
import edu.aiplatform.groupdocs.service.GroupDocImportService.DocumentImport;
import edu.aiplatform.groupdocs.service.GroupDocImportService.SectionImport;
import edu.aiplatform.groupdocs.service.GroupPeerReviewService;
import edu.aiplatform.groupdocs.service.GroupPeerReviewService.Given;
import edu.aiplatform.groupdocs.service.GroupPeerReviewService.PeerReviewView;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.shared.error.ApiException;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Nhập DOCX/Draw.io vào tài liệu nhóm (BR-U12-46) và đánh giá chéo giữa thành viên (BR-U12-47). */
@RestController
public class GroupDocCollaborationController {

    static final long MAX_FILE_BYTES = 20L * 1024 * 1024;

    private final GroupDocImportService imports;
    private final GroupPeerReviewService peerReviews;

    public GroupDocCollaborationController(GroupDocImportService imports, GroupPeerReviewService peerReviews) {
        this.imports = imports;
        this.peerReviews = peerReviews;
    }

    public record ApplyRequest(List<ApplyItem> sections) {
    }

    public record PeerReviewRequest(List<Given> reviews) {
    }

    @PostMapping(value = "/api/v1/group-docs/{id}/sections/{sectionId}/docx:preview", consumes = "multipart/form-data")
    public SectionImport previewSection(@AuthenticationPrincipal Actor actor, @PathVariable UUID id,
                                        @PathVariable UUID sectionId, @RequestPart("file") MultipartFile file) {
        return imports.previewSection(actor, id, sectionId, bytes(file, "DOCX"));
    }

    @PostMapping(value = "/api/v1/group-docs/{id}/import:preview", consumes = "multipart/form-data")
    public DocumentImport previewDocument(@AuthenticationPrincipal Actor actor, @PathVariable UUID id,
                                          @RequestPart(value = "docx", required = false) MultipartFile docx,
                                          @RequestPart(value = "drawio", required = false) MultipartFile drawio) {
        return imports.previewDocument(actor, id, docx == null ? null : bytes(docx, "DOCX"),
                drawio == null ? null : bytes(drawio, "Draw.io"));
    }

    @PostMapping("/api/v1/group-docs/{id}/import:apply")
    public ApplyResult applyDocument(@AuthenticationPrincipal Actor actor, @PathVariable UUID id,
                                     @RequestBody ApplyRequest request) {
        return imports.applyDocument(actor, id, request.sections());
    }

    @GetMapping("/api/v1/group-docs/{id}/peer-reviews")
    public PeerReviewView peerReviews(@AuthenticationPrincipal Actor actor, @PathVariable UUID id) {
        return peerReviews.view(actor, id);
    }

    @PutMapping("/api/v1/group-docs/{id}/peer-reviews")
    public PeerReviewView savePeerReviews(@AuthenticationPrincipal Actor actor, @PathVariable UUID id,
                                          @RequestBody PeerReviewRequest request) {
        return peerReviews.save(actor, id, request.reviews());
    }

    static byte[] bytes(MultipartFile file, String kind) {
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("IMPORT_FILE_INVALID", "Chưa chọn tệp " + kind);
        }
        if (file.getSize() > MAX_FILE_BYTES) {
            throw ApiException.payloadTooLarge("IMPORT_FILE_TOO_LARGE", "Tệp " + kind + " tối đa 20 MB");
        }
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw ApiException.badRequest("IMPORT_FILE_INVALID", "Không đọc được tệp " + kind);
        }
    }
}
