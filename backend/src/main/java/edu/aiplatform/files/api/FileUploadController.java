package edu.aiplatform.files.api;

import edu.aiplatform.files.application.UploadResult;
import edu.aiplatform.files.application.UploadService;
import edu.aiplatform.files.port.ArtifactPurpose;
import edu.aiplatform.files.port.UploadedFile;
import edu.aiplatform.shared.security.CurrentActor;
import java.io.IOException;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** `POST /api/v1/files` (contracts/openapi/files.yaml): upload một tệp, trả `FileRef` (không trả `fileId`). */
@RestController
@RequestMapping("/api/v1/files")
public class FileUploadController {

    private final UploadService uploads;

    public FileUploadController(UploadService uploads) {
        this.uploads = uploads;
    }

    public record UploadResponse(String fileRef, Instant expiresAt, String mediaType, long byteSize,
                                 String originalFileName) {
        static UploadResponse of(UploadResult r) {
            return new UploadResponse(r.fileRef().value(), r.expiresAt(), r.mediaType(), r.byteSize(),
                    r.originalFileName());
        }
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public UploadResponse upload(@RequestParam("purpose") ArtifactPurpose purpose,
                                 @RequestPart("file") MultipartFile file) throws IOException {
        UploadedFile uploaded = new UploadedFile(file.getOriginalFilename(), file.getContentType(), file.getSize(),
                file.getInputStream());
        return UploadResponse.of(uploads.upload(CurrentActor.require(), purpose, uploaded));
    }
}
