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
    private final MaterialService commands;private final MaterialQueryService queries;
    public SubjectMaterialController(MaterialService commands,MaterialQueryService queries){this.commands=commands;this.queries=queries;}
    @GetMapping("/materials") public List<MaterialViews.Detail> list(@AuthenticationPrincipal Actor actor,@PathVariable UUID subjectId) {
        return queries.subjectList(actor,subjectId);
    }
    @PostMapping("/materials") @ResponseStatus(HttpStatus.CREATED)
    public MaterialViews.Detail create(@AuthenticationPrincipal Actor actor,@PathVariable UUID subjectId,@Valid @RequestBody MaterialCommands.Create input) {
        var m=commands.create(actor,subjectId,false,input);return queries.detail(actor,m.id(),null);
    }
    @PutMapping("/materials:reorder") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reorder(@AuthenticationPrincipal Actor actor,@PathVariable UUID subjectId,@Valid @RequestBody MaterialCommands.Reorder input) {
        commands.reorder(actor,subjectId,false,input);
    }
}
