package edu.aiplatform.grading.application;

import edu.aiplatform.academics.contract.ClassRef;
import edu.aiplatform.academics.contract.IClassAccess;
import edu.aiplatform.assignments.contract.GradingMode;
import edu.aiplatform.assignments.contract.IAssignmentQuery;
import edu.aiplatform.assignments.contract.IRubric;
import edu.aiplatform.assignments.contract.SubmissionMode;
import edu.aiplatform.assignmentwork.contract.AttemptStatus;
import edu.aiplatform.assignmentwork.contract.ISubmissionQuery;
import edu.aiplatform.assignmentwork.contract.StudentSubmissionStatus;
import edu.aiplatform.audit.contract.AuditEvent;
import edu.aiplatform.audit.contract.AuditResult;
import edu.aiplatform.audit.contract.IAudit;
import edu.aiplatform.grading.application.GradingViews.AssignmentColumn;
import edu.aiplatform.grading.application.GradingViews.GradeCell;
import edu.aiplatform.grading.application.GradingViews.GradebookPage;
import edu.aiplatform.grading.application.GradingViews.GradebookRow;
import edu.aiplatform.grading.application.GradingViews.Person;
import edu.aiplatform.grading.application.GradingViews.StudentGradeView;
import edu.aiplatform.grading.contract.GroupSubmissionStatus;
import edu.aiplatform.grading.contract.IGroupSubmissionSource;
import edu.aiplatform.grading.domain.Evaluation;
import edu.aiplatform.grading.domain.EvaluationKind;
import edu.aiplatform.grading.domain.EvaluationStatus;
import edu.aiplatform.grading.domain.GradeDistributionCalculator;
import edu.aiplatform.grading.domain.IEvaluationRepository;
import edu.aiplatform.identity.contract.AccountContact;
import edu.aiplatform.identity.contract.Actions;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.IAccountLookup;
import edu.aiplatform.shared.error.ApiException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class GradebookService {
    private static final int EXPORT_PAGE_SIZE = 100;
    private final ObjectProvider<IGroupSubmissionSource> groups;
    private final IEvaluationRepository evaluations;
    private final GradingAccess access;
    private final IAssignmentQuery assignments;
    private final ISubmissionQuery submissions;
    private final IClassAccess classes;
    private final IAccountLookup accounts;
    private final IRubric rubric;
    private final IAudit audit;
    private final Clock clock;

    public GradebookService(IEvaluationRepository evaluations, GradingAccess access, IAssignmentQuery assignments,
            ISubmissionQuery submissions, IClassAccess classes, IAccountLookup accounts, IRubric rubric, IAudit audit,
            Clock clock, ObjectProvider<IGroupSubmissionSource> groups) {
        this.groups = groups;
        this.evaluations = evaluations;
        this.access = access;
        this.assignments = assignments;
        this.submissions = submissions;
        this.classes = classes;
        this.accounts = accounts;
        this.rubric = rubric;
        this.audit = audit;
        this.clock = clock;
    }

    public GradebookPage list(Actor actor, UUID classId, String query, int page, int size) {
        SubmissionReviewService.page(page, size);
        access.requireTeacher(actor, classId, Actions.GRADEBOOK_READ);
        var courseClass = classes.getClassRef(classId).orElseThrow(() -> ApiException.notFound("CLASS_NOT_FOUND"));
        var columns = assignmentColumns(courseClass);
        var classEvaluations = evaluations.findForClass(classId);
        var progress = submissionProgress(classId, columns);

        List<GradebookRow> rows = new ArrayList<>();
        for (var studentId : classes.listActiveStudents(classId)) {
            var student = studentPerson(studentId);
            if (!matchesQuery(student, query)) {
                continue;
            }
            var grades = columns.stream().map(column -> gradeCell(studentId, column, classEvaluations, progress))
                    .toList();
            rows.add(new GradebookRow(student, grades));
        }
        var pageRows = rows.stream().skip((long) (page - 1) * size).limit(size).toList();
        return new GradebookPage(pageRows, columns, rows.size(), page, size);
    }

    private List<AssignmentColumn> assignmentColumns(ClassRef courseClass) {
        return assignments.listForClass(courseClass.classId()).stream()
                .filter(assignment -> assignment.gradingMode() == GradingMode.GRADED)
                .peek(assignment -> SubmissionReviewService.requireAssignment(assignment, courseClass))
                .map(assignment -> new AssignmentColumn(assignment.assignmentId(), assignment.title(),
                        rubric.maxPoints(assignment.assignmentId()), assignment.submissionMode()))
                .toList();
    }

    private record SubmissionProgress(Map<UUID, List<StudentSubmissionStatus>> individual,
            Map<UUID, GroupSubmissionStatus> groups) {
    }

    private SubmissionProgress submissionProgress(UUID classId, List<AssignmentColumn> columns) {
        Map<UUID, List<StudentSubmissionStatus>> individual = new HashMap<>();
        Map<UUID, GroupSubmissionStatus> groupProgress = new HashMap<>();
        for (var column : columns) {
            var assignment = assignments.get(column.id())
                    .orElseThrow(() -> ApiException.unavailable("ASSIGNMENT_UNAVAILABLE", "Assignment unavailable"));
            if (assignment.submissionMode() == SubmissionMode.GROUP) {
                var provider = groups.getIfAvailable();
                if (provider == null) {
                    throw ApiException.unavailable("GROUP_SUBMISSIONS_UNAVAILABLE",
                            "Group submission source unavailable");
                }
                for (var status : provider.listGroupSubmissions(column.id(), classId)) {
                    groupProgress.put(status.groupDocumentId(), status);
                }
                individual.put(column.id(), List.of());
            } else {
                individual.put(column.id(), submissions.listSubmissions(column.id(), classId));
            }
        }
        return new SubmissionProgress(individual, groupProgress);
    }

    private Person studentPerson(UUID studentId) {
        var contact = accounts.getContact(studentId);
        return new Person(studentId, contact.map(AccountContact::displayName).orElse("Unavailable"),
                contact.map(AccountContact::email).orElse(""));
    }

    private boolean matchesQuery(Person student, String query) {
        return query == null || query.isBlank() || (student.name() + " " + student.email()).toLowerCase(Locale.ROOT)
                .contains(query.toLowerCase(Locale.ROOT));
    }

    private GradeCell gradeCell(UUID studentId, AssignmentColumn column, List<Evaluation> evaluations,
            SubmissionProgress progress) {
        var submission = progress.individual().get(column.id()).stream()
                .filter(status -> studentId.equals(status.accountId())).findFirst();
        var evaluation = currentEvaluation(studentId, column.id(), evaluations, submission).orElse(null);
        boolean late;
        Instant submittedAt;
        if (evaluation != null && evaluation.kind == EvaluationKind.MEMBER) {
            var group = Optional.ofNullable(progress.groups().get(evaluation.groupDocumentId));
            late = group.map(GroupSubmissionStatus::late).orElse(false);
            submittedAt = group.map(GroupSubmissionStatus::submittedAt).orElse(null);
        } else {
            late = submission.map(StudentSubmissionStatus::late).orElse(false);
            submittedAt = submission.map(StudentSubmissionStatus::submittedAt).orElse(null);
        }
        String status = evaluation == null ? ungradedStatus(submission) : evaluation.state.name();
        return new GradeCell(column.id(), evaluation == null ? null : evaluation.id, status,
                evaluation == null ? null : evaluation.score,
                evaluation == null ? column.maxScore() : evaluation.maxScore, late, submittedAt,
                evaluation == null ? null : evaluation.feedback);
    }

    private Optional<Evaluation> currentEvaluation(UUID studentId, UUID assignmentId, List<Evaluation> evaluations,
            Optional<StudentSubmissionStatus> submission) {
        return evaluations.stream()
                .filter(evaluation -> assignmentId.equals(evaluation.assignmentId)
                        && studentId.equals(evaluation.accountId) && evaluation.kind != EvaluationKind.GROUP_DOCUMENT
                        && evaluation.state != EvaluationStatus.SUPERSEDED)
                .filter(evaluation -> evaluation.kind == EvaluationKind.MEMBER || submission.isPresent()
                        && Objects.equals(evaluation.attemptId, submission.get().gradedAttemptId()))
                .max(Comparator.comparing(evaluation -> evaluation.createdAt));
    }

    private String ungradedStatus(Optional<StudentSubmissionStatus> submission) {
        if (submission.isEmpty() || submission.get().latestStatus() == null)
            return "NOT_SUBMITTED";
        return submission.get().latestStatus() == AttemptStatus.IN_PROGRESS ? "IN_PROGRESS" : "PENDING";
    }

    public List<StudentGradeView> myGrades(Actor actor, UUID classId) {
        access.requireStudent(actor, classId);
        Map<UUID, Evaluation> latest = new HashMap<>();
        for (var evaluation : evaluations.findForClass(classId))
            if (actor.accountId().equals(evaluation.accountId) && evaluation.state != EvaluationStatus.SUPERSEDED) {
                var previous = latest.get(evaluation.assignmentId);
                if (previous == null || evaluation.createdAt.isAfter(previous.createdAt))
                    latest.put(evaluation.assignmentId, evaluation);
            }
        return latest.values().stream().filter(evaluation -> evaluation.state == EvaluationStatus.PUBLISHED)
                .map(this::studentView).toList();
    }

    public StudentGradeView forAttempt(Actor actor, UUID attemptId) {
        var evaluation = evaluations.findByAttemptId(attemptId)
                .orElseThrow(() -> ApiException.notFound("GRADE_NOT_FOUND"));
        access.requireStudent(actor, evaluation.classId);
        if (!actor.accountId().equals(evaluation.accountId))
            throw ApiException.notFound("GRADE_NOT_FOUND");
        if (evaluation.state != EvaluationStatus.PUBLISHED)
            return new StudentGradeView(evaluation.assignmentId, evaluation.attemptId, null, null, null, null, false,
                    null);
        return studentView(evaluation);
    }

    private StudentGradeView studentView(Evaluation evaluation) {
        return new StudentGradeView(evaluation.assignmentId, evaluation.attemptId, evaluation.score,
                evaluation.maxScore, evaluation.feedback, evaluation.publishedAt, !evaluation.changes.isEmpty(),
                evaluation.rubricChecks);
    }

    public List<GradeDistributionCalculator.Bucket> distribution(Actor actor, UUID classId, UUID assignmentId,
            boolean student) {
        if (student)
            access.requireStudent(actor, classId);
        else
            access.requireTeacher(actor, classId, Actions.GRADING_REVIEW);
        var courseClass = classes.getClassRef(classId).orElseThrow(() -> ApiException.notFound("CLASS_NOT_FOUND"));
        var assignment = assignments.get(assignmentId).orElseThrow(() -> ApiException.notFound("ASSIGNMENT_NOT_FOUND"));
        SubmissionReviewService.requireAssignment(assignment, courseClass);
        if (student && !courseClass.showGradeDistribution())
            return List.of();
        // One published value per active learner, never one value per attempt.
        var active = new HashSet<>(classes.listActiveStudents(classId));
        Map<UUID, Evaluation> latest = new HashMap<>();
        for (var evaluation : evaluations.findForAssignmentAndClass(assignmentId, classId))
            if (evaluation.state == EvaluationStatus.PUBLISHED && active.contains(evaluation.accountId)) {
                var previous = latest.get(evaluation.accountId);
                if (previous == null || evaluation.createdAt.isAfter(previous.createdAt))
                    latest.put(evaluation.accountId, evaluation);
            }
        return new GradeDistributionCalculator().calculate(
                latest.values().stream().map(evaluation -> evaluation.score).toList(),
                latest.values().stream().map(evaluation -> evaluation.maxScore).toList());
    }

    // A successful export also writes its audit event in this transaction.
    @Transactional
    public List<List<String>> exportRows(Actor actor, UUID classId, UUID assignmentId, String format) {
        access.requireTeacher(actor, classId, Actions.GRADEBOOK_EXPORT);
        if (!Set.of("csv", "xlsx").contains(format))
            throw ApiException.badRequest("INVALID_EXPORT_FORMAT", "Choose csv or xlsx");
        List<List<String>> output = new ArrayList<>();
        output.add(List.of("Student", "Email", "Assignment", "Status", "Submitted at", "Late", "Score", "Maximum",
                "Feedback"));
        var first = list(actor, classId, null, 1, EXPORT_PAGE_SIZE);
        if (assignmentId != null
                && first.assignments().stream().noneMatch(assignment -> assignmentId.equals(assignment.id())))
            throw ApiException.notFound("ASSIGNMENT_NOT_FOUND");
        int pageCount = Math.max(1, (first.total() + EXPORT_PAGE_SIZE - 1) / EXPORT_PAGE_SIZE);
        for (int page = 1; page <= pageCount; page++) {
            var data = page == 1 ? first : list(actor, classId, null, page, EXPORT_PAGE_SIZE);
            appendExportRows(output, data, assignmentId);
        }
        audit.record(new AuditEvent(UUID.randomUUID(), actor.accountId(), "GRADEBOOK_EXPORTED", "CLASS",
                classId.toString(), AuditResult.SUCCESS, null, Map.of("format", format), clock.instant()));
        return output;
    }

    private void appendExportRows(List<List<String>> output, GradebookPage data, UUID assignmentId) {
        for (var row : data.rows()) {
            for (var grade : row.grades()) {
                if (assignmentId == null || assignmentId.equals(grade.assignmentId())) {
                    String title = data.assignments().stream()
                            .filter(assignment -> assignment.id().equals(grade.assignmentId())).findFirst()
                            .orElseThrow().title();
                    output.add(exportRow(row.student(), grade, title));
                }
            }
        }
    }

    private List<String> exportRow(Person student, GradeCell grade, String assignmentTitle) {
        boolean official = grade.status().equals("FINALIZED") || grade.status().equals("PUBLISHED");
        String score = official && grade.score() != null ? grade.score().toPlainString() : "";
        String feedback = official && grade.feedback() != null ? grade.feedback() : "";
        return List.of(student.name(), student.email(), assignmentTitle, grade.status(),
                grade.submittedAt() == null ? "" : grade.submittedAt().toString(), Boolean.toString(grade.late()),
                score, grade.maxScore() == null ? "" : grade.maxScore().toPlainString(), feedback);
    }
}
