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
    private final MaterialService commands;private final MaterialQueryService queries;
    public ClassMaterialController(MaterialService commands,MaterialQueryService queries){this.commands=commands;this.queries=queries;}
    @GetMapping("/materials") public List<MaterialViews.Detail> list(@AuthenticationPrincipal Actor actor,@PathVariable UUID classId) {
        return queries.classList(actor,classId,false,null);
    }
    @PostMapping("/materials") @ResponseStatus(HttpStatus.CREATED)
    public MaterialViews.Detail create(@AuthenticationPrincipal Actor actor,@PathVariable UUID classId,@Valid @RequestBody MaterialCommands.Create input) {
        var m=commands.create(actor,classId,true,input);return queries.detail(actor,m.id(),classId);
    }
    @PutMapping("/materials:reorder") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reorder(@AuthenticationPrincipal Actor actor,@PathVariable UUID classId,@Valid @RequestBody MaterialCommands.Reorder input) {
        commands.reorder(actor,classId,true,input);
    }
}
