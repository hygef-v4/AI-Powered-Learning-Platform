package edu.aiplatform.materials.web;

import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.files.contract.DownloadToken;
import edu.aiplatform.materials.application.*;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/materials/{id}")
public class MaterialController {
    private final MaterialService commands;private final MaterialQueryService queries;
    public MaterialController(MaterialService commands,MaterialQueryService queries){this.commands=commands;this.queries=queries;}
    @GetMapping({"","/view"}) public MaterialViews.Detail detail(@AuthenticationPrincipal Actor actor,@PathVariable UUID id,
                                                               @RequestParam(required=false) UUID classId) {return queries.detail(actor,id,classId);}
    @PatchMapping public MaterialViews.Detail update(@AuthenticationPrincipal Actor actor,@PathVariable UUID id,
                                                    @Valid @RequestBody MaterialCommands.Update input) {
        commands.update(actor,id,input);return queries.detail(actor,id,null);
    }
    @PostMapping("/versions") public MaterialViews.Detail replace(@AuthenticationPrincipal Actor actor,@PathVariable UUID id,
                                                                 @Valid @RequestBody MaterialCommands.Replace input) {
        commands.replace(actor,id,input);return queries.detail(actor,id,null);
    }
    @PostMapping("/archive") public MaterialViews.Detail archive(@AuthenticationPrincipal Actor actor,@PathVariable UUID id,
                                                                @Valid @RequestBody MaterialCommands.Expected input) {
        commands.archive(actor,id,input.version());return queries.detail(actor,id,null);
    }
    @PostMapping("/download-token") public DownloadToken download(@AuthenticationPrincipal Actor actor,@PathVariable UUID id,
            @RequestParam(required=false) UUID classId,@RequestParam(required=false) Integer versionNo) {
        return queries.download(actor,id,classId,versionNo);
    }
}
