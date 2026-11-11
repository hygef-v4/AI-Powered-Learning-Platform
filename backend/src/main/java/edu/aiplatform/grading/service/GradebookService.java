package edu.aiplatform.grading.service;

import edu.aiplatform.grading.model.RubricScoring;

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
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
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
    private static final DateTimeFormatter EXPORT_DATE_TIME = DateTimeFormatter
            .ofPattern("dd/MM/yyyy HH:mm:ss").withZone(ZoneId.of("Asia/Ho_Chi_Minh"));
    private static final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final ObjectProvider<IGroupSubmissionSource> groupSubmissionSourceProvider;
    private final IEvaluationRepository evaluationRepository;
    private final GradeDistributionVisibilityRepository gradeDistributionVisibilityRepository;
    private final GradingAccess gradingAccess;
    private final IAssignmentQuery assignmentQuery;
    private final ISubmissionQuery submissionQuery;
    private final IClassAccess classAccess;
    private final IAccountLookup accountLookup;
    private final IRubric rubric;
    private final IAudit audit;
    private final Clock clock;

    /** Connects gradebook queries to evaluation, submission, access, and audit services. */
    public GradebookService(IEvaluationRepository evaluationRepository, GradingAccess gradingAccess,
            IAssignmentQuery assignmentQuery, ISubmissionQuery submissionQuery,
            IClassAccess classAccess, IAccountLookup accountLookup, IRubric rubric, IAudit audit,
            Clock clock, ObjectProvider<IGroupSubmissionSource> groupSubmissionSourceProvider,
            GradeDistributionVisibilityRepository gradeDistributionVisibilityRepository) {
        this.gradeDistributionVisibilityRepository = gradeDistributionVisibilityRepository;
        this.groupSubmissionSourceProvider = groupSubmissionSourceProvider;
        this.evaluationRepository = evaluationRepository;
        this.gradingAccess = gradingAccess;
        this.assignmentQuery = assignmentQuery;
        this.submissionQuery = submissionQuery;
        this.classAccess = classAccess;
        this.accountLookup = accountLookup;
        this.rubric = rubric;
        this.audit = audit;
        this.clock = clock;
    }

    /** Returns a page of gradebook rows after filtering active students by name or email. */
    public GradebookPage listGrades(Actor actor, UUID classId, String query, int page, int size) {
        // Step 1: Validate page arguments and verify teacher gradebook access
        SubmissionReviewService.validatePage(page, size);
        gradingAccess.requireTeacher(actor, classId, Actions.GRADEBOOK_READ);

        // Step 2: Load complete gradebook data (columns, students, latest evaluations)
        var data = loadGradebook(classId, null);

        // Step 3: Filter active students by optional search query (name or email)
        var students = data.students().stream().filter(student -> matchesQuery(student, query))
                .toList();

        // Step 4: Apply pagination and map student grades for each assignment column
        var rows = students.stream().skip((long) (page - 1) * size).limit(size)
                .map(student -> mapGradebookRow(student, data)).toList();
        return new GradebookPage(rows, data.columns(), students.size(), page, size);
    }

    /** Returns the gradebook row for one active student in the class. */
    public GradebookRow loadStudentGrades(Actor actor, UUID classId, UUID accountId) {
        // Step 1: Verify teacher access and load gradebook scoped to this single student
        gradingAccess.requireTeacher(actor, classId, Actions.GRADEBOOK_READ);
        var data = loadGradebook(classId, accountId);

        // Step 2: Find the matching student and map their gradebook row
        var student = data.students().stream().findFirst()
                .orElseThrow(() -> ApiException.notFound("STUDENT_NOT_FOUND"));
        return mapGradebookRow(student, data);
    }

    private record GradeKey(UUID assignmentId, UUID studentId) {
    }

    private record GradebookData(List<AssignmentColumn> columns, List<Person> students,
            Map<GradeKey, Evaluation> evaluations, SubmissionProgress progress) {
    }

    /** Assembles assignment columns, active students, and evaluations for current submissions. */
    private GradebookData loadGradebook(UUID classId, UUID accountId) {
        // Step 1: Verify course class exists and load gradable assignment columns
        var courseClass = classAccess.getClassRef(classId)
                .orElseThrow(() -> ApiException.notFound("CLASS_NOT_FOUND"));
        var columns = listAssignmentColumns(courseClass);

        // Step 2: Index current submission progress (individual and group submissions)
        var progress = loadSubmissionProgress(classId, columns);
        Map<GradeKey, Evaluation> current = new HashMap<>();

        // Step 3: Match evaluations against latest student attempts or group documents
        for (var evaluation : evaluationRepository.findForClass(classId)) {
            if (evaluation.kind == EvaluationKind.GROUP_DOCUMENT
                    || evaluation.state == EvaluationStatus.SUPERSEDED)
                continue;
            var key = new GradeKey(evaluation.assignmentId, evaluation.accountId);
            var submission = progress.individual().get(key);
            if (evaluation.kind == EvaluationKind.MEMBER || submission != null
                    && Objects.equals(evaluation.attemptId, submission.gradedAttemptId())) {
                var previous = current.get(key);
                if (previous == null || gradeTime(evaluation).isAfter(gradeTime(previous)))
                    current.put(key, evaluation);
            }
        }

        // Step 4: Resolve active student profiles and contact information
        var studentIds = classAccess.listActiveStudents(classId).stream()
                .filter(id -> accountId == null || id.equals(accountId)).toList();
        var contacts = accountLookup.getContacts(studentIds);
        var students = studentIds.stream().map(id -> {
            var contact = contacts.get(id);
            return new Person(id, contact == null ? "Unavailable" : contact.displayName(),
                    contact == null ? "" : contact.email());
        }).toList();
        return new GradebookData(columns, students, current, progress);
    }

    /** Builds one grade cell per assignment column for the student. */
    private GradebookRow mapGradebookRow(Person student, GradebookData data) {
        return new GradebookRow(student,
                data.columns().stream().map(column -> mapGradeCell(student.accountId(), column,
                        data.evaluations(), data.progress())).toList());
    }

    /** Includes only graded assignments that have opened, excluding drafts and scheduled work. */
    public static boolean isGradable(AssignmentView assignment) {
        return assignment.gradingMode() == GradingMode.GRADED
                && (assignment.status() == AssignmentStatus.OPEN
                        || assignment.status() == AssignmentStatus.CLOSED
                        || assignment.status() == AssignmentStatus.RETIRED);
    }

    /** Builds columns for gradable class assignments with their rubric totals. */
    private List<AssignmentColumn> listAssignmentColumns(ClassRef courseClass) {
        return assignmentQuery.listForClass(courseClass.classId()).stream()
                .filter(GradebookService::isGradable)
                .peek(assignment -> SubmissionReviewService.requireAssignment(assignment,
                        courseClass))
                .map(assignment -> new AssignmentColumn(assignment.assignmentId(),
                        assignment.title(), rubric.maxPoints(assignment.assignmentId()),
                        assignment.submissionMode()))
                .toList();
    }

    private record SubmissionProgress(Map<GradeKey, StudentSubmissionStatus> individual,
            Map<UUID, GroupSubmissionStatus> groups) {
    }

    /** Indexes individual submissions by learner and group submissions by document. */
    private SubmissionProgress loadSubmissionProgress(UUID classId,
            List<AssignmentColumn> columns) {
        Map<GradeKey, StudentSubmissionStatus> individual = new HashMap<>();
        Map<UUID, GroupSubmissionStatus> groupProgress = new HashMap<>();
        for (var column : columns) {
            if (column.submissionMode() == SubmissionMode.GROUP) {
                var provider = groupSubmissionSourceProvider.getIfAvailable();
                if (provider == null) {
                    throw ApiException.unavailable("GROUP_SUBMISSIONS_UNAVAILABLE",
                            "Group submission source unavailable");
                }
                for (var status : provider.listGroupSubmissions(column.id(), classId)) {
                    groupProgress.put(status.groupDocumentId(), status);
                }
            } else {
                for (var status : submissionQuery.listSubmissions(column.id(), classId))
                    individual.putIfAbsent(new GradeKey(column.id(), status.accountId()), status);
            }
        }
        return new SubmissionProgress(individual, groupProgress);
    }

    /** Matches a case-insensitive name or email search, with blank queries matching all. */
    private boolean matchesQuery(Person student, String query) {
        return query == null || query.isBlank() || (student.name() + " " + student.email())
                .toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT));
    }

    /** Combines evaluation scores and status with individual or group submission timing. */
    private GradeCell mapGradeCell(UUID studentId, AssignmentColumn column,
            Map<GradeKey, Evaluation> evaluations, SubmissionProgress progress) {
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
        String status = evaluation == null ? resolveUngradedStatus(submission)
                : evaluation.state.name();
        return new GradeCell(column.id(), evaluation == null ? null : evaluation.id, status,
                evaluation == null ? null : evaluation.score,
                evaluation == null ? column.maxScore() : evaluation.maxScore, late, submittedAt,
                evaluation == null ? null : evaluation.feedback);
    }

    /** Distinguishes missing, in-progress, and pending submissions before grading. */
    private String resolveUngradedStatus(Optional<StudentSubmissionStatus> submission) {
        if (submission.isEmpty() || submission.get().latestStatus() == null)
            return "NOT_SUBMITTED";
        return submission.get().latestStatus() == AttemptStatus.IN_PROGRESS ? "IN_PROGRESS"
                : "PENDING";
    }

    /** Returns the student's latest non-superseded grade per assignment if published. */
    public List<StudentGradeView> listMyGrades(Actor actor, UUID classId) {
        // Step 1: Verify student enrollment in the course class
        gradingAccess.requireStudent(actor, classId);
        Map<UUID, Evaluation> latest = new HashMap<>();

        // Step 2: Filter the student's latest non-superseded evaluation for each assignment
        for (var evaluation : evaluationRepository.findForClass(classId))
            if (actor.accountId().equals(evaluation.accountId)
                    && evaluation.state != EvaluationStatus.SUPERSEDED) {
                var previous = latest.get(evaluation.assignmentId);
                if (previous == null || gradeTime(evaluation).isAfter(gradeTime(previous)))
                    latest.put(evaluation.assignmentId, evaluation);
            }

        // Step 3: Only expose published evaluations to students
        return latest.values().stream()
                .filter(evaluation -> evaluation.state == EvaluationStatus.PUBLISHED)
                .map(this::mapStudentView).toList();
    }

    private static Instant gradeTime(Evaluation evaluation) {
        return evaluation.gradedAt == null ? evaluation.createdAt : evaluation.gradedAt;
    }

    /** Returns the caller's attempt grade and hides its details until publication. */
    public StudentGradeView loadAttemptGrade(Actor actor, UUID attemptId) {
        // Step 1: Locate evaluation and verify that the caller is the student owner
        var evaluation = evaluationRepository.findByAttemptId(attemptId)
                .orElseThrow(() -> ApiException.notFound("GRADE_NOT_FOUND"));
        gradingAccess.requireStudent(actor, evaluation.classId);
        if (!actor.accountId().equals(evaluation.accountId))
            throw ApiException.notFound("GRADE_NOT_FOUND");

        // Step 2: Hide score and feedback breakdown if grade is still DRAFT/FINALIZED (not yet PUBLISHED)
        if (evaluation.state != EvaluationStatus.PUBLISHED)
            return new StudentGradeView(evaluation.assignmentId, evaluation.attemptId, null, null,
                    null, null, false, null);
        return mapStudentView(evaluation);
    }

    /** Shows published scores, rubric item evidence, and feedback from the saved evaluation. */
    private StudentGradeView mapStudentView(Evaluation evaluation) {
        List<RubricView> rubrics = List.of();
        if (evaluation.rubricSnapshot != null && !evaluation.rubricSnapshot.isBlank()) {
            try {
                rubrics = GradingJson.readRubrics(objectMapper, evaluation.rubricSnapshot);
            } catch (RuntimeException ignored) {
                rubrics = List.of();
            }
        }
        var checks = GradingJson.readChecks(objectMapper, evaluation.rubricChecks);
        var effectiveScores = RubricScoring.calculateEffectiveItemScores(rubrics, checks,
                evaluation.rubricItemScores);
        Map<UUID, GradingFeedback.Item> feedbackByItem = new HashMap<>();
        if (evaluation.structuredFeedback != null
                && evaluation.structuredFeedback.items() != null) {
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
                    if (item.points() == null) continue;
                    BigDecimal awarded = awardedForRubric.getOrDefault(item.itemId(),
                            BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
                    rubricScore = rubricScore.add(awarded);
                    var fb = feedbackByItem.get(item.itemId());
                    rubricItems.add(new StudentRubricItemView(rubricView.rubricId(),
                            rubricView.partId(), criterion.title(), item.itemId(), item.text(),
                            awarded, item.points().setScale(2, RoundingMode.HALF_UP),
                            fb == null ? null : fb.evidence(), fb == null ? null : fb.reason(),
                            fb == null ? null : fb.suggestion()));
                }
            }
        }
        GradingFeedback.Summary summary = evaluation.structuredFeedback != null
                && evaluation.structuredFeedback.summary() != null
                        ? evaluation.structuredFeedback.summary().copy()
                        : null;
        return new StudentGradeView(evaluation.assignmentId, evaluation.attemptId, evaluation.score,
                evaluation.maxScore, evaluation.feedback, evaluation.publishedAt,
                evaluation.publishedAt != null && evaluation.gradedAt != null
                        && evaluation.gradedAt.isAfter(evaluation.publishedAt), evaluation.rubricChecks, summary,
                List.copyOf(rubricItems), rubrics.isEmpty() ? evaluation.score : rubricScore,
                evaluation.memberOverride);
    }

    /** Buckets one published score per active student, subject to student visibility. */
    public List<GradeDistributionCalculator.Bucket> loadDistribution(Actor actor, UUID classId,
            UUID assignmentId, boolean student) {
        // Step 1: Verify role access (student or teacher)
        if (student)
            gradingAccess.requireStudent(actor, classId);
        else
            gradingAccess.requireTeacher(actor, classId, Actions.GRADING_REVIEW);

        // Step 2: Validate class and assignment scope
        var courseClass = classAccess.getClassRef(classId)
                .orElseThrow(() -> ApiException.notFound("CLASS_NOT_FOUND"));
        var assignment = assignmentQuery.get(assignmentId)
                .orElseThrow(() -> ApiException.notFound("ASSIGNMENT_NOT_FOUND"));
        SubmissionReviewService.requireAssignment(assignment, courseClass);
        if (assignment.scope() == AssignmentScope.SUBJECT
                && (assignment.status() == AssignmentStatus.DRAFT
                        || assignment.status() == AssignmentStatus.REVIEWED))
            throw ApiException.notFound("ASSIGNMENT_NOT_FOUND");

        // Step 3: For students, check whether the teacher enabled grade distribution visibility
        if (student && !gradeDistributionVisibilityRepository.findVisibility(classId, assignmentId)
                .enabled())
            return List.of();

        // Step 4: Collect latest published scores across active learners and compute histogram buckets
        var active = new HashSet<>(classAccess.listActiveStudents(classId));
        Map<UUID, Evaluation> latest = new HashMap<>();
        for (var evaluation : evaluationRepository.findForAssignmentAndClass(assignmentId, classId))
            if (evaluation.state == EvaluationStatus.PUBLISHED
                    && active.contains(evaluation.accountId)) {
                var previous = latest.get(evaluation.accountId);
                if (previous == null || gradeTime(evaluation).isAfter(gradeTime(previous)))
                    latest.put(evaluation.accountId, evaluation);
            }
        return new GradeDistributionCalculator().calculate(
                latest.values().stream().map(evaluation -> evaluation.score).toList(),
                latest.values().stream().map(evaluation -> evaluation.maxScore).toList());
    }

    /**
     * Builds CSV or Excel gradebook rows and records the successful export in the same transaction.
     */
    @Transactional
    public List<List<String>> buildExportRows(Actor actor, UUID classId, UUID assignmentId,
            String format) {
        return buildExport(actor, classId, assignmentId, format).rows();
    }

    public record GradebookExport(List<List<String>> rows, String assignmentTitle) {}

    @Transactional
    public GradebookExport buildExport(Actor actor, UUID classId, UUID assignmentId, String format) {
        // Step 1: Check teacher export permission and format parameter (csv or xlsx)
        gradingAccess.requireTeacher(actor, classId, Actions.GRADEBOOK_EXPORT);
        if (!Set.of("csv", "xlsx").contains(format))
            throw ApiException.badRequest("INVALID_EXPORT_FORMAT", "Choose csv or xlsx");

        // Step 2: Write standard header row
        List<List<String>> output = new ArrayList<>();
        output.add(List.of("Sinh viên", "Email", "Bài tập", "Trạng thái", "Thời gian nộp", "Nộp muộn",
                "Điểm", "Điểm tối đa", "Nhận xét"));

        // Step 3: Load gradebook data and format export rows for each student and assignment
        gradingAccess.requireTeacher(actor, classId, Actions.GRADEBOOK_READ);
        var data = loadGradebook(classId, null);
        var selectedColumns = data.columns().stream()
                .filter(column -> assignmentId == null || assignmentId.equals(column.id())).toList();
        if (assignmentId != null && selectedColumns.isEmpty())
            throw ApiException.notFound("ASSIGNMENT_NOT_FOUND");
        for (var student : data.students()) {
            for (var column : selectedColumns) {
                output.add(buildExportRow(student, mapGradeCell(student.accountId(), column,
                        data.evaluations(), data.progress()), column.title()));
            }
        }

        // Step 4: Record audit log event for gradebook export
        audit.record(new AuditEvent(UUID.randomUUID(), actor.accountId(), "GRADEBOOK_EXPORTED",
                "CLASS", classId.toString(), AuditResult.SUCCESS, null, Map.of("format", format),
                clock.instant()));
        String title = selectedColumns.size() == 1 ? selectedColumns.get(0).title() : null;
        return new GradebookExport(output, title);
    }

    /** Staff exports include saved drafts; student endpoints still require publication. */
    private List<String> buildExportRow(Person student, GradeCell grade, String assignmentTitle) {
        boolean saved = grade.status().equals("DRAFT") || grade.status().equals("FINALIZED")
                || grade.status().equals("PUBLISHED");
        String score = saved && grade.score() != null ? grade.score().toPlainString() : "";
        String feedback = saved && grade.feedback() != null ? grade.feedback() : "";
        String status = switch (grade.status()) {
            case "NOT_SUBMITTED" -> "Chưa nộp";
            case "IN_PROGRESS" -> "Đang làm bài";
            case "PENDING" -> "Chờ chấm";
            case "DRAFT" -> "Đã chấm";
            case "FINALIZED" -> "Đã lưu";
            case "PUBLISHED" -> "Đã công bố";
            case "SUPERSEDED" -> "Bài nộp cũ";
            default -> "Chưa xác định";
        };
        return List.of(student.name(), student.email(), assignmentTitle, status,
                grade.submittedAt() == null ? "" : EXPORT_DATE_TIME.format(grade.submittedAt()),
                grade.late() ? "Có" : "Không", score,
                grade.maxScore() == null ? "" : grade.maxScore().toPlainString(), feedback);
    }
}
