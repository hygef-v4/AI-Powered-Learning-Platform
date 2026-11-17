package edu.aiplatform.groupdocs.controller;

import edu.aiplatform.groupdocs.service.GroupDocImageService;
import edu.aiplatform.identity.contract.Actor;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** Ảnh trong tài liệu nhóm: chuyển tới link tải ngắn hạn của U03, như ảnh trong bài cá nhân. */
@RestController
public class GroupDocImageController {

    private final GroupDocImageService groupDocImageService;

    public GroupDocImageController(GroupDocImageService groupDocImageService) {
        this.groupDocImageService = groupDocImageService;
    }

    @GetMapping("/api/v1/group-docs/{id}/blocks/{blockId}/image")
    public ResponseEntity<Void> redirectImage(@AuthenticationPrincipal Actor actor, @PathVariable UUID id,
                                      @PathVariable UUID blockId) {
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(groupDocImageService.resolveImageUrl(actor, id, blockId)))
                .cacheControl(CacheControl.noStore()).build();
    }
}
