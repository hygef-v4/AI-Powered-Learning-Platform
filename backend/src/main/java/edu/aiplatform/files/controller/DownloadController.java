package edu.aiplatform.files.controller;

import edu.aiplatform.files.service.DownloadService;
import edu.aiplatform.identity.contract.Actor;
import java.io.InputStream;
import java.io.OutputStream;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/** Tải về (F3): stream từng phần, bộ đệm 64 KB (NFR-U03-05). */
@RestController
public class DownloadController {

    private final DownloadService downloads;

    public DownloadController(DownloadService downloads) {
        this.downloads = downloads;
    }

    @GetMapping("/api/v1/files/download/{token}")
    public ResponseEntity<StreamingResponseBody> download(@AuthenticationPrincipal Actor actor,
                                                          @PathVariable String token) {
        DownloadService.Download download = downloads.open(actor, token);
        StreamingResponseBody body = (OutputStream out) -> {
            try (InputStream in = download.content()) {
                byte[] buffer = new byte[64 * 1024];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
            }
        };
        return ResponseEntity.ok().headers(download.headers()).body(body);
    }
}
