package edu.aiplatform.materials.controller;

import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.files.contract.DownloadToken;
import edu.aiplatform.materials.service.*;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/materials/{id}")
public class MaterialController {
    private final MaterialService materialService;
    private final MaterialQueryService materialQueryService;

    public MaterialController(
            MaterialService materialService, MaterialQueryService materialQueryService) {
        this.materialService = materialService;
        this.materialQueryService = materialQueryService;
    }

    @GetMapping({"", "/view"})
    public MaterialViews.Detail loadMaterial(
            @AuthenticationPrincipal Actor actor,
            @PathVariable UUID id,
            @RequestParam(required = false) UUID classId) {
        return materialQueryService.loadMaterial(actor, id, classId);
    }

    @PatchMapping
    public MaterialViews.Detail updateMaterial(
            @AuthenticationPrincipal Actor actor,
            @PathVariable UUID id,
            @Valid @RequestBody MaterialCommands.Update input) {
        materialService.updateMaterial(actor, id, input);
        return materialQueryService.loadMaterial(actor, id, null);
    }

    @PostMapping("/versions")
    public MaterialViews.Detail replaceMaterialVersion(
            @AuthenticationPrincipal Actor actor,
            @PathVariable UUID id,
            @Valid @RequestBody MaterialCommands.Replace input) {
        materialService.replaceMaterialVersion(actor, id, input);
        return materialQueryService.loadMaterial(actor, id, null);
    }

    @PostMapping("/archive")
    public MaterialViews.Detail archiveMaterial(
            @AuthenticationPrincipal Actor actor,
            @PathVariable UUID id,
            @Valid @RequestBody MaterialCommands.Expected input) {
        materialService.archiveMaterial(actor, id, input.version());
        return materialQueryService.loadMaterial(actor, id, null);
    }

    @PostMapping("/restore")
    public MaterialViews.Detail restoreMaterial(
            @AuthenticationPrincipal Actor actor,
            @PathVariable UUID id,
            @Valid @RequestBody MaterialCommands.Expected input) {
        materialService.restoreMaterial(actor, id, input.version());
        return materialQueryService.loadMaterial(actor, id, null);
    }

    @PostMapping("/download-token")
    public DownloadToken issueDownloadToken(
            @AuthenticationPrincipal Actor actor,
            @PathVariable UUID id,
            @RequestParam(required = false) UUID classId,
            @RequestParam(required = false) Integer versionNo) {
        return materialQueryService.issueDownloadToken(actor, id, classId, versionNo);
    }
}
