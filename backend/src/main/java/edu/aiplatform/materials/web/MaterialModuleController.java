package edu.aiplatform.materials.web;

import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.materials.application.*;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Module học liệu của môn: Chủ nhiệm môn quản lý; giảng viên và sinh viên của lớp xem module của môn. */
@RestController
@RequestMapping("/api/v1")
public class MaterialModuleController {
    private final MaterialModuleService modules;
    public MaterialModuleController(MaterialModuleService modules){this.modules=modules;}

    @GetMapping("/subjects/{subjectId}/modules")
    public List<MaterialViews.Module> subject(@AuthenticationPrincipal Actor actor,@PathVariable UUID subjectId) {
        return modules.forSubject(actor,subjectId);
    }
    @PostMapping("/subjects/{subjectId}/modules") @ResponseStatus(HttpStatus.CREATED)
    public MaterialViews.Module create(@AuthenticationPrincipal Actor actor,@PathVariable UUID subjectId,@Valid @RequestBody MaterialCommands.ModuleInput input) {
        return modules.create(actor,subjectId,input);
    }
    @PutMapping("/subjects/{subjectId}/modules:reorder") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reorder(@AuthenticationPrincipal Actor actor,@PathVariable UUID subjectId,@Valid @RequestBody MaterialCommands.Reorder input) {
        modules.reorder(actor,subjectId,input);
    }
    @GetMapping("/classes/{classId}/modules")
    public List<MaterialViews.Module> teaching(@AuthenticationPrincipal Actor actor,@PathVariable UUID classId) {
        return modules.forClass(actor,classId,false);
    }
    @GetMapping("/me/classes/{classId}/modules")
    public List<MaterialViews.Module> mine(@AuthenticationPrincipal Actor actor,@PathVariable UUID classId) {
        return modules.forClass(actor,classId,true);
    }
    @PatchMapping("/material-modules/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void rename(@AuthenticationPrincipal Actor actor,@PathVariable UUID id,@Valid @RequestBody MaterialCommands.ModuleUpdate input) {
        modules.rename(actor,id,input);
    }
    @DeleteMapping("/material-modules/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Actor actor,@PathVariable UUID id,@RequestParam long version) {
        modules.delete(actor,id,version);
    }
    @PostMapping("/material-modules/{id}/quizzes") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void attach(@AuthenticationPrincipal Actor actor,@PathVariable UUID id,@Valid @RequestBody MaterialCommands.QuizAttach input) {
        modules.attach(actor,id,input.quizId());
    }
    @DeleteMapping("/material-modules/{id}/quizzes/{quizId}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void detach(@AuthenticationPrincipal Actor actor,@PathVariable UUID id,@PathVariable UUID quizId) {
        modules.detach(actor,id,quizId);
    }
}
