package edu.aiplatform.academics.controller;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import edu.aiplatform.academics.dto.*;
import edu.aiplatform.academics.service.*;
import edu.aiplatform.academics.dto.AcademicsDtos.*;
import edu.aiplatform.academics.contract.ClassStatus;
import edu.aiplatform.academics.model.*;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.shared.error.ApiException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class AcademicsController {
    private final AcademicsService academicsService;
    public AcademicsController(AcademicsService academicsService) {this.academicsService=academicsService;}

    @GetMapping("/me/classes")
    public List<ClassView> listStudentClasses(@AuthenticationPrincipal Actor actor,@RequestParam(required=false) String q,
        @RequestParam(required=false) ClassStatus status,@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int size) {
        return academicsService.listClasses(actor,"student",null,q,status,page,size);
    }
    @GetMapping("/teaching/classes")
    public List<ClassView> listTeacherClasses(@AuthenticationPrincipal Actor actor,@RequestParam(required=false) String q,
        @RequestParam(required=false) ClassStatus status,@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int size) {
        return academicsService.listClasses(actor,"teacher",null,q,status,page,size);
    }
    @GetMapping("/manager/classes")
    public List<ClassView> listManagedClasses(@AuthenticationPrincipal Actor actor,@RequestParam(required=false) UUID subjectId,
        @RequestParam(required=false) String q,@RequestParam(required=false) ClassStatus status,
        @RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int size) {
        return academicsService.listClasses(actor,"manager",subjectId,q,status,page,size);
    }
    @GetMapping("/me/classes/{id}")
    public ClassView loadStudentClass(@AuthenticationPrincipal Actor actor,@PathVariable UUID id) {return academicsService.loadClass(actor,"student",id);}
    @GetMapping("/teaching/classes/{id}")
    public ClassView loadTeacherClass(@AuthenticationPrincipal Actor actor,@PathVariable UUID id) {return academicsService.loadClass(actor,"teacher",id);}
    @GetMapping("/manager/classes/{id}")
    public ClassView loadManagedClass(@AuthenticationPrincipal Actor actor,@PathVariable UUID id) {return academicsService.loadClass(actor,"manager",id);}
    @GetMapping("/teaching/classes/{id}/students")
    public List<Student> listTeacherStudents(@AuthenticationPrincipal Actor actor,@PathVariable UUID id,
        @RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int size) {return academicsService.listStudents(actor,"teacher",id,page,size);}
    @GetMapping("/manager/classes/{id}/students")
    public List<Student> listManagedStudents(@AuthenticationPrincipal Actor actor,@PathVariable UUID id,
        @RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int size) {return academicsService.listStudents(actor,"manager",id,page,size);}
    @GetMapping("/teaching/classes/{id}/students/{accountId}")
    public Student loadTeacherStudent(@AuthenticationPrincipal Actor actor,@PathVariable UUID id,@PathVariable UUID accountId) {return academicsService.loadStudent(actor,id,accountId);}
    @GetMapping("/manager/subjects")
    public List<SubjectSummary> listManagedSubjects(@AuthenticationPrincipal Actor actor) {return academicsService.listManagedSubjects(actor);}
    @GetMapping("/manager/subjects/{id}")
    public SubjectSummary loadManagedSubject(@AuthenticationPrincipal Actor actor,@PathVariable UUID id) {return academicsService.loadManagedSubject(actor,id);}
    @GetMapping("/manager/teacher-candidates")
    public List<Person> listTeacherCandidates(@AuthenticationPrincipal Actor actor,@RequestParam(defaultValue="") String q) {return academicsService.listTeacherCandidates(actor,q);}
    @GetMapping("/manager/classes/{id}/student-candidates")
    public List<Person> listStudentCandidates(@AuthenticationPrincipal Actor actor,@PathVariable UUID id,@RequestParam(defaultValue="") String q) {return academicsService.listStudentCandidates(actor,id,q);}
    @PostMapping("/manager/classes")
    @ResponseStatus(HttpStatus.CREATED)
    public ClassView createClass(@AuthenticationPrincipal Actor actor,@Valid @RequestBody CreateInput input) {
        return academicsService.createClass(actor,input.subjectId(),input.code(),input.name(),input.description(),input.term(),input.teacherId());
    }
    @PatchMapping("/manager/classes/{id}")
    public ClassView updateClassInfo(@AuthenticationPrincipal Actor actor,@PathVariable UUID id,@Valid @RequestBody InfoInput input) {
        return academicsService.updateClassInfo(actor,id,input.name(),input.description(),input.term(),input.version());
    }
    @PutMapping("/manager/classes/{id}/teacher")
    public ClassView assignClassTeacher(@AuthenticationPrincipal Actor actor,@PathVariable UUID id,@Valid @RequestBody TeacherInput input) {return academicsService.assignClassTeacher(actor,id,input.teacherId(),input.version());}
    @PutMapping("/manager/classes/{id}/status")
    public ClassView changeClassStatus(@AuthenticationPrincipal Actor actor,@PathVariable UUID id,@Valid @RequestBody StatusInput input) {return academicsService.changeClassStatus(actor,id,input.status(),input.version());}
    @PostMapping("/manager/classes/{id}/enrollments")
    public EnrollmentOutcome enrollStudent(@AuthenticationPrincipal Actor actor,@PathVariable UUID id,@Valid @RequestBody EnrollmentInput input) {return new EnrollmentOutcome(academicsService.enrollStudent(actor,id,input.accountId()));}
    @PostMapping("/manager/classes/{id}/enrollments:batch")
    public List<RowResult> enrollStudents(@AuthenticationPrincipal Actor actor,@PathVariable UUID id,@Valid @RequestBody BatchInput input) {return academicsService.enrollStudents(actor,id,input.text());}
    @PutMapping("/manager/classes/{id}/enrollments/{accountId}/status")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changeEnrollmentStatus(@AuthenticationPrincipal Actor actor,@PathVariable UUID id,@PathVariable UUID accountId,@Valid @RequestBody EnrollmentStatusInput input) {academicsService.changeEnrollmentStatus(actor,id,accountId,input.status());}

    public interface IStrictInput {
        @JsonAnySetter default void unexpected(String key,Object value) {throw ApiException.badRequest("INVALID_REQUEST","Trường không được phép thay đổi");}
    }
    public record CreateInput(@NotNull UUID subjectId,@NotBlank @Size(max=30) String code,@NotBlank @Size(max=200) String name,
                              @Size(max=2000) String description,@Size(max=20) String term,UUID teacherId) implements IStrictInput { }
    public record InfoInput(@NotBlank @Size(max=200) String name,@Size(max=2000) String description,@Size(max=20) String term,
                            @NotNull @PositiveOrZero Long version) implements IStrictInput { }
    public record TeacherInput(UUID teacherId,@NotNull @PositiveOrZero Long version) implements IStrictInput { }
    public record StatusInput(@NotNull ClassStatus status,@NotNull @PositiveOrZero Long version) implements IStrictInput { }
    public record EnrollmentInput(@NotNull UUID accountId) implements IStrictInput { }
    public record BatchInput(@NotNull String text) implements IStrictInput { }
    public record EnrollmentStatusInput(@NotNull EnrollmentStatus status) implements IStrictInput { }
    public record EnrollmentOutcome(EnrollmentResult result) { }
}
