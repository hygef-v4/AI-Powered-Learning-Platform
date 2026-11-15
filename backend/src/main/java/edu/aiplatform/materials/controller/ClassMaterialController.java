package edu.aiplatform.materials.controller;

import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.materials.service.*;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/classes/{classId}")
public class ClassMaterialController {
    private final MaterialService materialService;
    private final MaterialQueryService materialQueryService;

    public ClassMaterialController(
            MaterialService materialService, MaterialQueryService materialQueryService) {
        this.materialService = materialService;
        this.materialQueryService = materialQueryService;
    }

    @GetMapping("/materials")
    public List<MaterialViews.Detail> listClassMaterials(
            @AuthenticationPrincipal Actor actor, @PathVariable UUID classId) {
        return materialQueryService.listClassMaterials(actor, classId, false, null);
    }

    @PostMapping("/materials")
    @ResponseStatus(HttpStatus.CREATED)
    public MaterialViews.Detail createClassMaterial(
            @AuthenticationPrincipal Actor actor,
            @PathVariable UUID classId,
            @Valid @RequestBody MaterialCommands.Create input) {
        var m = materialService.createMaterial(actor, classId, true, input);
        return materialQueryService.loadMaterial(actor, m.id(), classId);
    }

    @PutMapping("/materials:reorder")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reorderClassMaterials(
            @AuthenticationPrincipal Actor actor,
            @PathVariable UUID classId,
            @Valid @RequestBody MaterialCommands.Reorder input) {
        materialService.reorderMaterials(actor, classId, true, input);
    }
}
