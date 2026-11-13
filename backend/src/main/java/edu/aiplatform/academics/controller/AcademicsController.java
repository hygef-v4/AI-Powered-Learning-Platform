package edu.aiplatform.academics.controller;

import com.fasterxml.jackson.annotation.JsonAnySetter;
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

    /** Creates the academics API controller with its application service. */
    public AcademicsController(AcademicsService academicsService) {
        this.academicsService = academicsService;
    }

    /** Lists enrolled classes for the authenticated student. */
    @GetMapping("/me/classes")
    public List<ClassView> listStudentClasses(@AuthenticationPrincipal Actor actor,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) ClassStatus status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return academicsService.listClasses(actor, "student", null, q, status, page, size);
    }

    /** Lists classes assigned to the authenticated teacher. */
    @GetMapping("/teaching/classes")
    public List<ClassView> listTeacherClasses(@AuthenticationPrincipal Actor actor,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) ClassStatus status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return academicsService.listClasses(actor, "teacher", null, q, status, page, size);
    }

    /** Lists classes belonging to subjects managed by the subject manager. */
    @GetMapping("/manager/classes")
    public List<ClassView> listManagedClasses(@AuthenticationPrincipal Actor actor,
            @RequestParam(required = false) UUID subjectId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) ClassStatus status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return academicsService.listClasses(actor, "manager", subjectId, q, status, page, size);
    }

    /** Loads class details for an enrolled student. */
    @GetMapping("/me/classes/{id}")
    public ClassView loadStudentClass(@AuthenticationPrincipal Actor actor, @PathVariable UUID id) {
        return academicsService.loadClass(actor, "student", id);
    }

    /** Loads class details for the assigned teacher. */
    @GetMapping("/teaching/classes/{id}")
    public ClassView loadTeacherClass(@AuthenticationPrincipal Actor actor, @PathVariable UUID id) {
        return academicsService.loadClass(actor, "teacher", id);
    }

    /** Loads class details for the subject manager. */
    @GetMapping("/manager/classes/{id}")
    public ClassView loadManagedClass(@AuthenticationPrincipal Actor actor, @PathVariable UUID id) {
        return academicsService.loadClass(actor, "manager", id);
    }

    /** Lists enrolled students in a class for the teacher. */
    @GetMapping("/teaching/classes/{id}/students")
    public List<Student> listTeacherStudents(@AuthenticationPrincipal Actor actor,
            @PathVariable UUID id, @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return academicsService.listStudents(actor, "teacher", id, page, size);
    }

    /** Lists enrolled students in a class for the subject manager. */
    @GetMapping("/manager/classes/{id}/students")
    public List<Student> listManagedStudents(@AuthenticationPrincipal Actor actor,
            @PathVariable UUID id, @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return academicsService.listStudents(actor, "manager", id, page, size);
    }

    /** Loads a specific student's profile within a class. */
    @GetMapping("/teaching/classes/{id}/students/{accountId}")
    public Student loadTeacherStudent(@AuthenticationPrincipal Actor actor, @PathVariable UUID id,
            @PathVariable UUID accountId) {
        return academicsService.loadStudent(actor, id, accountId);
    }

    /** Lists subjects assigned to the subject manager. */
    @GetMapping("/manager/subjects")
    public List<SubjectSummary> listManagedSubjects(@AuthenticationPrincipal Actor actor) {
        return academicsService.listManagedSubjects(actor);
    }

    /** Loads details of an assigned subject for the subject manager. */
    @GetMapping("/manager/subjects/{id}")
    public SubjectSummary loadManagedSubject(@AuthenticationPrincipal Actor actor,
            @PathVariable UUID id) {
        return academicsService.loadManagedSubject(actor, id);
    }

    /** Searches available teacher accounts for class assignment. */
    @GetMapping("/manager/teacher-candidates")
    public List<Person> listTeacherCandidates(@AuthenticationPrincipal Actor actor,
            @RequestParam(defaultValue = "") String q) {
        return academicsService.listTeacherCandidates(actor, q);
    }

    /** Searches candidate students available for class enrollment. */
    @GetMapping("/manager/classes/{id}/student-candidates")
    public List<Person> listStudentCandidates(@AuthenticationPrincipal Actor actor,
            @PathVariable UUID id, @RequestParam(defaultValue = "") String q) {
        return academicsService.listStudentCandidates(actor, id, q);
    }

    /** Creates a new class within a managed subject. */
    @PostMapping("/manager/classes")
    @ResponseStatus(HttpStatus.CREATED)
    public ClassView createClass(@AuthenticationPrincipal Actor actor,
            @Valid @RequestBody CreateInput input) {
        return academicsService.createClass(actor, input.subjectId(), input.code(), input.name(),
                input.description(), input.teacherId());
    }

    /** Updates the class name and description using the supplied version. */
    @PatchMapping("/manager/classes/{id}")
    public ClassView updateClassInfo(@AuthenticationPrincipal Actor actor, @PathVariable UUID id,
            @Valid @RequestBody InfoInput input) {
        return academicsService.updateClassInfo(actor, id, input.name(), input.description(), input.version());
    }

    /** Assigns or reassigns the responsible teacher to a class. */
    @PutMapping("/manager/classes/{id}/teacher")
    public ClassView assignClassTeacher(@AuthenticationPrincipal Actor actor, @PathVariable UUID id,
            @Valid @RequestBody TeacherInput input) {
        return academicsService.assignClassTeacher(actor, id, input.teacherId(), input.version());
    }

    /** Updates class lifecycle status (e.g. OPEN, ARCHIVED). */
    @PutMapping("/manager/classes/{id}/status")
    public ClassView changeClassStatus(@AuthenticationPrincipal Actor actor, @PathVariable UUID id,
            @Valid @RequestBody StatusInput input) {
        return academicsService.changeClassStatus(actor, id, input.status(), input.version());
    }

    /** Enrolls an individual student into a class. */
    @PostMapping("/manager/classes/{id}/enrollments")
    public EnrollmentOutcome enrollStudent(@AuthenticationPrincipal Actor actor,
            @PathVariable UUID id, @Valid @RequestBody EnrollmentInput input) {
        return new EnrollmentOutcome(academicsService.enrollStudent(actor, id, input.accountId()));
    }

    /** Batch enrolls students from a raw list or CSV text. */
    @PostMapping("/manager/classes/{id}/enrollments:batch")
    public List<RowResult> enrollStudents(@AuthenticationPrincipal Actor actor,
            @PathVariable UUID id, @Valid @RequestBody BatchInput input) {
        return academicsService.enrollStudents(actor, id, input.text());
    }

    /** Changes enrollment status of a student in a class (e.g. ACTIVE, DROPPED). */
    @PutMapping("/manager/classes/{id}/enrollments/{accountId}/status")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changeEnrollmentStatus(@AuthenticationPrincipal Actor actor, @PathVariable UUID id,
            @PathVariable UUID accountId, @Valid @RequestBody EnrollmentStatusInput input) {
        academicsService.changeEnrollmentStatus(actor, id, accountId, input.status());
    }

    public interface IStrictInput {
        /** Rejects input fields that are not declared by the request record. */
        @JsonAnySetter
        default void unexpected(String key, Object value) {
            throw ApiException.badRequest("INVALID_REQUEST", "Trường không được phép thay đổi");
        }
    }

    public record CreateInput(@NotNull UUID subjectId, @NotBlank @Size(max = 30) String code,
            @NotBlank @Size(max = 200) String name, @Size(max = 2000) String description,
            UUID teacherId) implements IStrictInput {
    }

    public record InfoInput(@NotBlank @Size(max = 200) String name,
            @Size(max = 2000) String description, @NotNull @PositiveOrZero Long version) implements IStrictInput {
    }

    public record TeacherInput(UUID teacherId, @NotNull @PositiveOrZero Long version)
            implements IStrictInput {
    }

    public record StatusInput(@NotNull ClassStatus status, @NotNull @PositiveOrZero Long version)
            implements IStrictInput {
    }

    public record EnrollmentInput(@NotNull UUID accountId) implements IStrictInput {
    }

    public record BatchInput(@NotNull String text) implements IStrictInput {
    }

    public record EnrollmentStatusInput(@NotNull EnrollmentStatus status) implements IStrictInput {
    }

    public record EnrollmentOutcome(EnrollmentResult result) {
    }
}
