package edu.aiplatform.grading.service;

import edu.aiplatform.grading.model.RubricScoring;
import edu.aiplatform.grading.dto.GradingViews;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.academics.contract.ClassRef;
import edu.aiplatform.academics.contract.IClassAccess;
import edu.aiplatform.assignments.contract.AssignmentStatus;
import edu.aiplatform.assignments.contract.AssignmentView;
import edu.aiplatform.assignments.contract.GradingMode;
import edu.aiplatform.assignments.contract.IAssignmentQuery;
import edu.aiplatform.assignments.contract.IRubric;
import edu.aiplatform.assignments.contract.RubricView;
import edu.aiplatform.assignments.contract.SubmissionMode;
import edu.aiplatform.assignmentwork.contract.AttemptStatus;
import edu.aiplatform.assignmentwork.contract.ISubmissionQuery;
import edu.aiplatform.assignmentwork.contract.StudentSubmissionStatus;
import edu.aiplatform.audit.contract.AuditEvent;
import edu.aiplatform.audit.contract.AuditResult;
import edu.aiplatform.audit.contract.IAudit;
import edu.aiplatform.grading.dto.GradingViews.AssignmentColumn;
import edu.aiplatform.grading.dto.GradingViews.GradeCell;
import edu.aiplatform.grading.dto.GradingViews.GradebookPage;
import edu.aiplatform.grading.dto.GradingViews.GradebookRow;
import edu.aiplatform.grading.dto.GradingViews.Person;
import edu.aiplatform.grading.dto.GradingViews.StudentGradeView;
import edu.aiplatform.grading.dto.GradingViews.StudentRubricItemView;
import edu.aiplatform.grading.contract.GroupSubmissionStatus;
import edu.aiplatform.grading.contract.IGroupSubmissionSource;
import edu.aiplatform.grading.model.Evaluation;
import edu.aiplatform.grading.model.EvaluationKind;
import edu.aiplatform.grading.model.EvaluationStatus;
import edu.aiplatform.grading.model.GradeDistributionCalculator;
import edu.aiplatform.grading.model.GradingFeedback;
import edu.aiplatform.grading.repository.IEvaluationRepository;
import edu.aiplatform.grading.repository.GradeDistributionVisibilityRepository;
import edu.aiplatform.assignments.contract.AssignmentScope;
import edu.aiplatform.identity.contract.Actions;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.IAccountLookup;
import edu.aiplatform.shared.error.ApiException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
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
    private static final ObjectMapper JSON = new ObjectMapper().findAndRegisterModules()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final ObjectProvider<IGroupSubmissionSource> groups;
    private final IEvaluationRepository evaluations;
    private final GradeDistributionVisibilityRepository visibility;
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
            Clock clock, ObjectProvider<IGroupSubmissionSource> groups, GradeDistributionVisibilityRepository visibility) {
        this.visibility = visibility;
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
        var data = loadGradebook(classId, null);
        var students = data.students().stream().filter(student -> matchesQuery(student, query)).toList();
        var rows = students.stream().skip((long) (page - 1) * size).limit(size)
                .map(student -> gradebookRow(student, data)).toList();
        return new GradebookPage(rows, data.columns(), students.size(), page, size);
    }

    public GradebookRow student(Actor actor, UUID classId, UUID accountId) {
        access.requireTeacher(actor, classId, Actions.GRADEBOOK_READ);
        var data = loadGradebook(classId, accountId);
        var student = data.students().stream().findFirst()
                .orElseThrow(() -> ApiException.notFound("STUDENT_NOT_FOUND"));
        return gradebookRow(student, data);
    }

    private record GradeKey(UUID assignmentId, UUID studentId) { }
    private record GradebookData(List<AssignmentColumn> columns, List<Person> students,
            Map<GradeKey, Evaluation> evaluations, SubmissionProgress progress) { }

    private GradebookData loadGradebook(UUID classId, UUID accountId) {
        var courseClass = classes.getClassRef(classId).orElseThrow(() -> ApiException.notFound("CLASS_NOT_FOUND"));
        var columns = assignmentColumns(courseClass);
        var progress = submissionProgress(classId, columns);
        Map<GradeKey, Evaluation> current = new HashMap<>();
        for (var evaluation : evaluations.findForClass(classId)) {
            if (evaluation.kind == EvaluationKind.GROUP_DOCUMENT || evaluation.state == EvaluationStatus.SUPERSEDED)
                continue;
            var key = new GradeKey(evaluation.assignmentId, evaluation.accountId);
            var submission = progress.individual().get(key);
            if (evaluation.kind == EvaluationKind.MEMBER || submission != null
                    && Objects.equals(evaluation.attemptId, submission.gradedAttemptId())) {
                var previous = current.get(key);
                if (previous == null || evaluation.createdAt.isAfter(previous.createdAt)) current.put(key, evaluation);
            }
        }
        var studentIds = classes.listActiveStudents(classId).stream()
                .filter(id -> accountId == null || id.equals(accountId)).toList();
        var contacts = accounts.getContacts(studentIds);
        var students = studentIds.stream().map(id -> {
            var contact = contacts.get(id);
            return new Person(id, contact == null ? "Unavailable" : contact.displayName(), contact == null ? "" : contact.email());
        }).toList();
        return new GradebookData(columns, students, current, progress);
    }

    private GradebookRow gradebookRow(Person student, GradebookData data) {
        return new GradebookRow(student, data.columns().stream()
                .map(column -> gradeCell(student.accountId(), column, data.evaluations(), data.progress())).toList());
    }

    /**
     * Bài có điểm đã giao cho sinh viên: {@code IAssignmentQuery.listForClass} trả cả bài lớp còn nháp, đã duyệt, chờ mở;
     * những bài đó chưa có ai nộp nên không thành cột bảng điểm.
     */
    public static boolean gradable(AssignmentView assignment) {
        return assignment.gradingMode() == GradingMode.GRADED && (assignment.status() == AssignmentStatus.OPEN
                || assignment.status() == AssignmentStatus.CLOSED || assignment.status() == AssignmentStatus.RETIRED);
    }

    private List<AssignmentColumn> assignmentColumns(ClassRef courseClass) {
        return assignments.listForClass(courseClass.classId()).stream()
                .filter(GradebookService::gradable)
                .peek(assignment -> SubmissionReviewService.requireAssignment(assignment, courseClass))
                .map(assignment -> new AssignmentColumn(assignment.assignmentId(), assignment.title(),
                        rubric.maxPoints(assignment.assignmentId()), assignment.submissionMode()))
                .toList();
    }

    private record SubmissionProgress(Map<GradeKey, StudentSubmissionStatus> individual,
            Map<UUID, GroupSubmissionStatus> groups) {
    }

    private SubmissionProgress submissionProgress(UUID classId, List<AssignmentColumn> columns) {
        Map<GradeKey, StudentSubmissionStatus> individual = new HashMap<>();
        Map<UUID, GroupSubmissionStatus> groupProgress = new HashMap<>();
        for (var column : columns) {
            if (column.submissionMode() == SubmissionMode.GROUP) {
                var provider = groups.getIfAvailable();
                if (provider == null) {
                    throw ApiException.unavailable("GROUP_SUBMISSIONS_UNAVAILABLE",
                            "Group submission source unavailable");
                }
                for (var status : provider.listGroupSubmissions(column.id(), classId)) {
                    groupProgress.put(status.groupDocumentId(), status);
                }
            } else {
                for (var status : submissions.listSubmissions(column.id(), classId))
                    individual.putIfAbsent(new GradeKey(column.id(), status.accountId()), status);
            }
        }
        return new SubmissionProgress(individual, groupProgress);
    }

    private boolean matchesQuery(Person student, String query) {
        return query == null || query.isBlank() || (student.name() + " " + student.email()).toLowerCase(Locale.ROOT)
                .contains(query.toLowerCase(Locale.ROOT));
    }

    private GradeCell gradeCell(UUID studentId, AssignmentColumn column, Map<GradeKey, Evaluation> evaluations,
            SubmissionProgress progress) {
        var key = new GradeKey(column.id(), studentId);
        var submission = Optional.ofNullable(progress.individual().get(key));
        var evaluation = evaluations.get(key);
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
        List<RubricView> rubrics = List.of();
        if (evaluation.rubricSnapshot != null && !evaluation.rubricSnapshot.isBlank()) {
            try {
                rubrics = GradingJson.rubrics(JSON, evaluation.rubricSnapshot);
            } catch (RuntimeException ignored) {
                rubrics = List.of();
            }
        }
        var checks = GradingJson.readChecks(JSON, evaluation.rubricChecks);
        var effectiveScores = RubricScoring.effectiveItemScores(rubrics, checks, evaluation.rubricItemScores);
        Map<UUID, GradingFeedback.Item> feedbackByItem = new HashMap<>();
        if (evaluation.structuredFeedback != null && evaluation.structuredFeedback.items() != null) {
            for (var item : evaluation.structuredFeedback.items()) {
                if (item != null && item.itemId() != null) {
                    feedbackByItem.put(item.itemId(), item);
                }
            }
        }
        List<StudentRubricItemView> rubricItems = new ArrayList<>();
        BigDecimal rubricScore = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        for (var rubricView : rubrics) {
            var awardedForRubric = effectiveScores.getOrDefault(rubricView.rubricId(), Map.of());
            if (rubricView.criteria() == null)
                continue;
            for (var criterion : rubricView.criteria()) {
                if (criterion.items() == null)
                    continue;
                for (var item : criterion.items()) {
                    BigDecimal awarded = awardedForRubric.getOrDefault(item.itemId(),
                            BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
                    rubricScore = rubricScore.add(awarded);
                    var fb = feedbackByItem.get(item.itemId());
                    rubricItems.add(new StudentRubricItemView(
                            rubricView.rubricId(),
                            rubricView.partId(),
                            criterion.title(),
                            item.itemId(),
                            item.text(),
                            awarded,
                            item.points().setScale(2, RoundingMode.HALF_UP),
                            fb == null ? null : fb.evidence(),
                            fb == null ? null : fb.reason(),
                            fb == null ? null : fb.suggestion()));
                }
            }
        }
        GradingFeedback.Summary summary = evaluation.structuredFeedback != null
                && evaluation.structuredFeedback.summary() != null
                        ? evaluation.structuredFeedback.summary().copy()
                        : null;
        return new StudentGradeView(
                evaluation.assignmentId,
                evaluation.attemptId,
                evaluation.score,
                evaluation.maxScore,
                evaluation.feedback,
                evaluation.publishedAt,
                !evaluation.changes.isEmpty(),
                evaluation.rubricChecks,
                summary,
                List.copyOf(rubricItems),
                rubrics.isEmpty() ? evaluation.score : rubricScore,
                evaluation.memberOverride);
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
        if (assignment.scope() == AssignmentScope.SUBJECT
                && (assignment.status() == AssignmentStatus.DRAFT || assignment.status() == AssignmentStatus.REVIEWED))
            throw ApiException.notFound("ASSIGNMENT_NOT_FOUND");
        if (student && !visibility.find(classId, assignmentId).enabled())
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
        access.requireTeacher(actor, classId, Actions.GRADEBOOK_READ);
        var data = loadGradebook(classId, null);
        if (assignmentId != null
                && data.columns().stream().noneMatch(assignment -> assignmentId.equals(assignment.id())))
            throw ApiException.notFound("ASSIGNMENT_NOT_FOUND");
        for (var student : data.students()) {
            for (var column : data.columns()) {
                if (assignmentId == null || assignmentId.equals(column.id()))
                    output.add(exportRow(student, gradeCell(student.accountId(), column, data.evaluations(), data.progress()), column.title()));
            }
        }
        audit.record(new AuditEvent(UUID.randomUUID(), actor.accountId(), "GRADEBOOK_EXPORTED", "CLASS",
                classId.toString(), AuditResult.SUCCESS, null, Map.of("format", format), clock.instant()));
        return output;
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
