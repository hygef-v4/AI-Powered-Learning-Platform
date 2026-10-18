package edu.aiplatform.academics.web;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import edu.aiplatform.academics.application.*;
import edu.aiplatform.academics.application.AcademicsDtos.*;
import edu.aiplatform.academics.contract.ClassStatus;
import edu.aiplatform.academics.domain.*;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.shared.error.ApiException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@ConditionalOnProperty(name="platform.academics.persistence-enabled",havingValue="true")
public class AcademicsController {
    private final AcademicsService service;
    public AcademicsController(AcademicsService service) {this.service=service;}

    @GetMapping("/me/classes")
    public List<ClassView> studentClasses(@AuthenticationPrincipal Actor actor,@RequestParam(required=false) String q,
        @RequestParam(required=false) ClassStatus status,@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int size) {
        return service.list(actor,"student",null,q,status,page,size);
    }
    @GetMapping("/teaching/classes")
    public List<ClassView> teacherClasses(@AuthenticationPrincipal Actor actor,@RequestParam(required=false) String q,
        @RequestParam(required=false) ClassStatus status,@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int size) {
        return service.list(actor,"teacher",null,q,status,page,size);
    }
    @GetMapping("/manager/classes")
    public List<ClassView> managerClasses(@AuthenticationPrincipal Actor actor,@RequestParam(required=false) UUID subjectId,
        @RequestParam(required=false) String q,@RequestParam(required=false) ClassStatus status,
        @RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int size) {
        return service.list(actor,"manager",subjectId,q,status,page,size);
    }
    @GetMapping("/me/classes/{id}")
    public ClassView studentClass(@AuthenticationPrincipal Actor actor,@PathVariable UUID id) {return service.detail(actor,"student",id);}
    @GetMapping("/teaching/classes/{id}")
    public ClassView teacherClass(@AuthenticationPrincipal Actor actor,@PathVariable UUID id) {return service.detail(actor,"teacher",id);}
    @GetMapping("/manager/classes/{id}")
    public ClassView managerClass(@AuthenticationPrincipal Actor actor,@PathVariable UUID id) {return service.detail(actor,"manager",id);}
    @GetMapping("/teaching/classes/{id}/students")
    public List<Student> teacherStudents(@AuthenticationPrincipal Actor actor,@PathVariable UUID id,
        @RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int size) {return service.students(actor,"teacher",id,page,size);}
    @GetMapping("/manager/classes/{id}/students")
    public List<Student> managerStudents(@AuthenticationPrincipal Actor actor,@PathVariable UUID id,
        @RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int size) {return service.students(actor,"manager",id,page,size);}
    @GetMapping("/teaching/classes/{id}/students/{accountId}")
    public Student teacherStudent(@AuthenticationPrincipal Actor actor,@PathVariable UUID id,@PathVariable UUID accountId) {return service.student(actor,id,accountId);}
    @GetMapping("/manager/subjects")
    public List<SubjectSummary> subjects(@AuthenticationPrincipal Actor actor) {return service.managedSubjects(actor);}
    @GetMapping("/manager/subjects/{id}")
    public SubjectSummary subject(@AuthenticationPrincipal Actor actor,@PathVariable UUID id) {return service.managedSubject(actor,id);}
    @GetMapping("/manager/teacher-candidates")
    public List<Person> teachers(@AuthenticationPrincipal Actor actor,@RequestParam(defaultValue="") String q) {return service.teacherCandidates(actor,q);}
    @GetMapping("/manager/classes/{id}/student-candidates")
    public List<Person> students(@AuthenticationPrincipal Actor actor,@PathVariable UUID id,@RequestParam(defaultValue="") String q) {return service.studentCandidates(actor,id,q);}
    @PostMapping("/manager/classes")
    @ResponseStatus(HttpStatus.CREATED)
    public ClassView create(@AuthenticationPrincipal Actor actor,@Valid @RequestBody CreateInput input) {
        return service.create(actor,input.subjectId(),input.code(),input.name(),input.description(),input.term(),input.teacherId());
    }
    @PatchMapping("/manager/classes/{id}")
    public ClassView info(@AuthenticationPrincipal Actor actor,@PathVariable UUID id,@Valid @RequestBody InfoInput input) {
        return service.updateInfo(actor,id,input.name(),input.description(),input.term(),input.version());
    }
    @PutMapping("/manager/classes/{id}/teacher")
    public ClassView teacher(@AuthenticationPrincipal Actor actor,@PathVariable UUID id,@Valid @RequestBody TeacherInput input) {return service.assignTeacher(actor,id,input.teacherId(),input.version());}
    @PutMapping("/manager/classes/{id}/grade-distribution")
    public ClassView distribution(@AuthenticationPrincipal Actor actor,@PathVariable UUID id,@Valid @RequestBody DistributionInput input) {return service.gradeDistribution(actor,id,input.enabled(),input.version());}
    @PutMapping("/manager/classes/{id}/status")
    public ClassView status(@AuthenticationPrincipal Actor actor,@PathVariable UUID id,@Valid @RequestBody StatusInput input) {return service.changeStatus(actor,id,input.status(),input.version());}
    @PostMapping("/manager/classes/{id}/enrollments")
    public EnrollmentOutcome enroll(@AuthenticationPrincipal Actor actor,@PathVariable UUID id,@Valid @RequestBody EnrollmentInput input) {return new EnrollmentOutcome(service.enroll(actor,id,input.accountId()));}
    @PostMapping("/manager/classes/{id}/enrollments:batch")
    public List<RowResult> batch(@AuthenticationPrincipal Actor actor,@PathVariable UUID id,@Valid @RequestBody BatchInput input) {return service.enrollBatch(actor,id,input.text());}
    @PutMapping("/manager/classes/{id}/enrollments/{accountId}/status")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void enrollmentStatus(@AuthenticationPrincipal Actor actor,@PathVariable UUID id,@PathVariable UUID accountId,@Valid @RequestBody EnrollmentStatusInput input) {service.enrollmentStatus(actor,id,accountId,input.status());}

    public interface IStrictInput {
        @JsonAnySetter default void unexpected(String key,Object value) {throw ApiException.badRequest("INVALID_REQUEST","Trường không được phép thay đổi");}
    }
    public record CreateInput(@NotNull UUID subjectId,@NotBlank @Size(max=30) String code,@NotBlank @Size(max=200) String name,
                              @Size(max=2000) String description,@Size(max=20) String term,UUID teacherId) implements IStrictInput { }
    public record InfoInput(@NotBlank @Size(max=200) String name,@Size(max=2000) String description,@Size(max=20) String term,
                            @NotNull @PositiveOrZero Long version) implements IStrictInput { }
    public record TeacherInput(UUID teacherId,@NotNull @PositiveOrZero Long version) implements IStrictInput { }
    public record DistributionInput(@NotNull Boolean enabled,@NotNull @PositiveOrZero Long version) implements IStrictInput { }
    public record StatusInput(@NotNull ClassStatus status,@NotNull @PositiveOrZero Long version) implements IStrictInput { }
    public record EnrollmentInput(@NotNull UUID accountId) implements IStrictInput { }
    public record BatchInput(@NotNull String text) implements IStrictInput { }
    public record EnrollmentStatusInput(@NotNull EnrollmentStatus status) implements IStrictInput { }
    public record EnrollmentOutcome(EnrollmentResult result) { }
}
