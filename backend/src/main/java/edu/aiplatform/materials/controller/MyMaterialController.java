package edu.aiplatform.materials.controller;

import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.materials.service.*;
import java.util.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/me/classes/{classId}/materials")
public class MyMaterialController {
    private final MaterialQueryService queries;
    public MyMaterialController(MaterialQueryService queries){this.queries=queries;}
    @GetMapping public List<MaterialViews.Detail> list(@AuthenticationPrincipal Actor actor,@PathVariable UUID classId,
                                                     @RequestParam(required=false) List<UUID> ids) {
        return queries.classList(actor,classId,true,ids);
    }
}
