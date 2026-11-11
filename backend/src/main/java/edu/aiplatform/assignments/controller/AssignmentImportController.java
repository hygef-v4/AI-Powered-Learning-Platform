package edu.aiplatform.assignments.controller;

import edu.aiplatform.ai.contract.AiProvider;
import edu.aiplatform.ai.contract.AiSource;
import edu.aiplatform.assignments.dto.AssignmentViews;
import edu.aiplatform.assignments.service.AssignmentImportService;
import edu.aiplatform.assignments.service.ImportFileReader.ImportFile;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.shared.error.ApiException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Nhập đề cũ từ ảnh/PDF thành bản nháp xem trước (BR-U07-87); ghi đề qua {@code ai-drafts/{draftId}:apply}. */
@RestController
@RequestMapping("/api/v1/assignments/{id}/imports")
public class AssignmentImportController {

    /** Đọc tối đa 5 tệp × 10 MB; U03 kiểm lại theo Settings. */
    static final long MAX_FILE_BYTES = 10L * 1024 * 1024;

    private final AssignmentImportService assignmentImportService;

    public AssignmentImportController(AssignmentImportService assignmentImportService) {
        this.assignmentImportService = assignmentImportService;
    }

    @PostMapping(consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public AssignmentViews.AiDraft startImport(@AuthenticationPrincipal Actor actor, @PathVariable UUID id,
                                               @RequestPart(value = "files", required = false) List<MultipartFile> files,
                                               @RequestParam("requestRef") String requestRef,
                                               @RequestParam(value = "source", required = false) AiSource source,
                                               @RequestParam(value = "provider", required = false) AiProvider provider) {
        return assignmentImportService.startImport(actor, id, readFiles(files),
                new AssignmentImportService.StartImport(requestRef, source, provider));
    }

    private static List<ImportFile> readFiles(List<MultipartFile> files) {
        List<ImportFile> read = new ArrayList<>();
        if (files == null) {
            return read;
        }
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            if (file.getSize() > MAX_FILE_BYTES) {
                throw ApiException.badRequest("ASSIGNMENT_IMPORT_TOO_LARGE", "Mỗi tệp tối đa 10 MB");
            }
            try {
                read.add(new ImportFile(file.getOriginalFilename(), file.getBytes()));
            } catch (IOException e) {
                throw ApiException.badRequest("ASSIGNMENT_IMPORT_UNREADABLE", "Không đọc được tệp đã gửi");
            }
        }
        return read;
    }
}
