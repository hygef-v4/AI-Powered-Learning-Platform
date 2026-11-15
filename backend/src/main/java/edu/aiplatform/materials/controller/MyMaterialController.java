package edu.aiplatform.materials.controller;

import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.materials.service.*;
import java.util.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/me/classes/{classId}/materials")
public class MyMaterialController {
    private final MaterialQueryService materialQueryService;

    public MyMaterialController(MaterialQueryService materialQueryService) {
        this.materialQueryService = materialQueryService;
    }

    @GetMapping
    public List<MaterialViews.Detail> listMyMaterials(
            @AuthenticationPrincipal Actor actor,
            @PathVariable UUID classId,
            @RequestParam(required = false) List<UUID> ids) {
        return materialQueryService.listClassMaterials(actor, classId, true, ids);
    }
}
