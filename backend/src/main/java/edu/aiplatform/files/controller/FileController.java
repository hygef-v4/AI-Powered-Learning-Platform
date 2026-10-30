package edu.aiplatform.files.controller;

import edu.aiplatform.files.service.FilePolicyService;
import edu.aiplatform.files.service.UploadResult;
import edu.aiplatform.files.service.UploadService;
import edu.aiplatform.files.contract.ArtifactPurpose;
import edu.aiplatform.files.contract.FileUpload;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.shared.error.ApiException;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Upload (F1) và giới hạn tải lên (F5). */
@RestController
@RequestMapping("/api/v1/files")
public class FileController {

    private final UploadService uploads;
    private final FilePolicyService policies;

    public FileController(UploadService uploads, FilePolicyService policies) {
        this.uploads = uploads;
        this.policies = policies;
    }

    @PostMapping(consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public UploadResult upload(@AuthenticationPrincipal Actor actor, @RequestParam("purpose") ArtifactPurpose purpose,
                               @RequestParam("file") MultipartFile file) {
        try {
            return uploads.upload(actor, purpose,
                    new FileUpload(file.getOriginalFilename(), file.getSize(), file.getInputStream()));
        } catch (IOException e) {
            throw ApiException.unavailable("UPLOAD_FAILED", "Không nhận được tệp, thử lại sau");
        }
    }

    @GetMapping("/policies")
    public List<FilePolicyService.PurposePolicyView> policies() {
        return policies.policies();
    }
}
