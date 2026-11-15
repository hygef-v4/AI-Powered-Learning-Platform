package edu.aiplatform.materials.controller;

import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.materials.service.*;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/subjects/{subjectId}")
public class SubjectMaterialController {
    private final MaterialService materialService;
    private final MaterialQueryService materialQueryService;

    public SubjectMaterialController(
            MaterialService materialService, MaterialQueryService materialQueryService) {
        this.materialService = materialService;
        this.materialQueryService = materialQueryService;
    }

    @GetMapping("/materials")
    public List<MaterialViews.Detail> listSubjectMaterials(
            @AuthenticationPrincipal Actor actor, @PathVariable UUID subjectId) {
        return materialQueryService.listSubjectMaterials(actor, subjectId);
    }

    @PostMapping("/materials")
    @ResponseStatus(HttpStatus.CREATED)
    public MaterialViews.Detail createSubjectMaterial(
            @AuthenticationPrincipal Actor actor,
            @PathVariable UUID subjectId,
            @Valid @RequestBody MaterialCommands.Create input) {
        var m = materialService.createMaterial(actor, subjectId, false, input);
        return materialQueryService.loadMaterial(actor, m.id(), null);
    }

    @PutMapping("/materials:reorder")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reorderSubjectMaterials(
            @AuthenticationPrincipal Actor actor,
            @PathVariable UUID subjectId,
            @Valid @RequestBody MaterialCommands.Reorder input) {
        materialService.reorderMaterials(actor, subjectId, false, input);
    }
}
