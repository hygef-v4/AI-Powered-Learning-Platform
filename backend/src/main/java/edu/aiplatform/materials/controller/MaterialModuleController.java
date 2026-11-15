package edu.aiplatform.materials.controller;

import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.materials.service.*;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Module học liệu của môn: Chủ nhiệm môn quản lý; giảng viên và sinh viên của lớp xem module của môn. */
@RestController
@RequestMapping("/api/v1")
public class MaterialModuleController {
    private final MaterialModuleService materialModuleService;

    public MaterialModuleController(MaterialModuleService materialModuleService) {
        this.materialModuleService = materialModuleService;
    }

    @GetMapping("/subjects/{subjectId}/modules")
    public List<MaterialViews.Module> listSubjectModules(
            @AuthenticationPrincipal Actor actor, @PathVariable UUID subjectId) {
        return materialModuleService.listSubjectModules(actor, subjectId);
    }

    @PostMapping("/subjects/{subjectId}/modules")
    @ResponseStatus(HttpStatus.CREATED)
    public MaterialViews.Module createModule(
            @AuthenticationPrincipal Actor actor,
            @PathVariable UUID subjectId,
            @Valid @RequestBody MaterialCommands.ModuleInput input) {
        return materialModuleService.createModule(actor, subjectId, input);
    }

    @PutMapping("/subjects/{subjectId}/modules:reorder")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reorderModules(
            @AuthenticationPrincipal Actor actor,
            @PathVariable UUID subjectId,
            @Valid @RequestBody MaterialCommands.Reorder input) {
        materialModuleService.reorderModules(actor, subjectId, input);
    }

    @GetMapping("/classes/{classId}/modules")
    public List<MaterialViews.Module> listClassModules(
            @AuthenticationPrincipal Actor actor, @PathVariable UUID classId) {
        return materialModuleService.listClassModules(actor, classId, false);
    }

    @GetMapping("/me/classes/{classId}/modules")
    public List<MaterialViews.Module> listMyClassModules(
            @AuthenticationPrincipal Actor actor, @PathVariable UUID classId) {
        return materialModuleService.listClassModules(actor, classId, true);
    }

    @PatchMapping("/material-modules/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void renameModule(
            @AuthenticationPrincipal Actor actor,
            @PathVariable UUID id,
            @Valid @RequestBody MaterialCommands.ModuleUpdate input) {
        materialModuleService.renameModule(actor, id, input);
    }

    @DeleteMapping("/material-modules/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteModule(
            @AuthenticationPrincipal Actor actor,
            @PathVariable UUID id,
            @RequestParam long version) {
        materialModuleService.deleteModule(actor, id, version);
    }

    @PostMapping("/material-modules/{id}/quizzes")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void attachQuiz(
            @AuthenticationPrincipal Actor actor,
            @PathVariable UUID id,
            @Valid @RequestBody MaterialCommands.QuizAttach input) {
        materialModuleService.attachQuiz(actor, id, input.quizId());
    }

    @DeleteMapping("/material-modules/{id}/quizzes/{quizId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void detachQuiz(
            @AuthenticationPrincipal Actor actor,
            @PathVariable UUID id,
            @PathVariable UUID quizId) {
        materialModuleService.detachQuiz(actor, id, quizId);
    }
}
