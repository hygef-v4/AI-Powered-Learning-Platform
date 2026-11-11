package edu.aiplatform.grading.service;


import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.academics.contract.ClassRef;
import edu.aiplatform.academics.contract.IClassAccess;
import edu.aiplatform.assignments.contract.AssignmentScope;
import edu.aiplatform.assignments.contract.AssignmentView;
import edu.aiplatform.assignments.contract.DocumentModel;
import edu.aiplatform.assignments.contract.GradingMode;
import edu.aiplatform.assignments.contract.IAssignmentQuery;
import edu.aiplatform.assignments.contract.SubmissionMode;
import edu.aiplatform.assignmentwork.contract.ActivitySummary;
import edu.aiplatform.assignmentwork.contract.AttemptStatus;
import edu.aiplatform.assignmentwork.contract.IActivityQuery;
import edu.aiplatform.assignmentwork.contract.ISubmissionQuery;
import edu.aiplatform.assignmentwork.contract.StudentSubmissionStatus;
import edu.aiplatform.assignmentwork.contract.WorkKind;
import edu.aiplatform.assignmentwork.contract.WorkRef;
import edu.aiplatform.grading.dto.GradingViews.EvaluationDetail;
import edu.aiplatform.grading.dto.GradingViews.MemberActivity;
import edu.aiplatform.grading.dto.GradingViews.Person;
import edu.aiplatform.grading.dto.GradingViews.SubmissionPage;
import edu.aiplatform.grading.dto.GradingViews.SubmissionRow;
import edu.aiplatform.grading.contract.GroupSectionAuthor;
import edu.aiplatform.grading.contract.IGroupSubmissionSource;
import edu.aiplatform.grading.model.EvaluationKind;
import edu.aiplatform.grading.model.EvaluationStatus;
import edu.aiplatform.grading.repository.IEvaluationRepository;
import edu.aiplatform.identity.contract.Actions;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.IAccountLookup;
import edu.aiplatform.shared.error.ApiException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SubmissionReviewService {
    private final IEvaluationRepository evaluationRepository;
    private final GradingAccess gradingAccess;
    private final IAssignmentQuery assignmentQuery;
    private final ISubmissionQuery submissionQuery;
    private final IClassAccess classAccess;
    private final IAccountLookup accountLookup;
    private final IActivityQuery activityQuery;
    private final ObjectProvider<IGroupSubmissionSource> groupSubmissionSourceProvider;
    private final ObjectMapper objectMapper;

    /** Creates the review service with evaluation, assignment, submission, roster, and activity services. */
    public SubmissionReviewService(IEvaluationRepository evaluationRepository,
            GradingAccess gradingAccess, IAssignmentQuery assignmentQuery,
            ISubmissionQuery submissionQuery, IClassAccess classAccess,
            IAccountLookup accountLookup, IActivityQuery activityQuery,
            ObjectProvider<IGroupSubmissionSource> groupSubmissionSourceProvider,
            ObjectMapper objectMapper) {
        this.evaluationRepository = evaluationRepository;
        this.gradingAccess = gradingAccess;
        this.assignmentQuery = assignmentQuery;
        this.submissionQuery = submissionQuery;
        this.classAccess = classAccess;
        this.accountLookup = accountLookup;
        this.activityQuery = activityQuery;
        this.groupSubmissionSourceProvider = groupSubmissionSourceProvider;
        this.objectMapper = objectMapper;
    }

    /**
     * Asserts that the assignment is graded and belongs to the given class or subject scope.
     */
    public static void requireAssignment(AssignmentView assignment, ClassRef courseClass) {
        if (assignment.gradingMode() != GradingMode.GRADED
                || !(assignment.scope() == AssignmentScope.CLASS
                        ? courseClass.classId().equals(assignment.classId())
                        : courseClass.subjectId().equals(assignment.subjectId())))
            throw ApiException.notFound("ASSIGNMENT_NOT_FOUND");
    }

    /**
     * Validates that pagination page and size numbers are positive and within upper bounds.
     */
    public static void validatePage(int page, int size) {
        if (page < 1 || size < 1 || size > 100 || page > 100000)
            throw ApiException.badRequest("INVALID_PAGE", "Invalid page or size");
    }

    /**
     * Resolves an account ID to a displayable Person record with name and email.
     */
    Person mapPerson(UUID id) {
        return accountLookup.getContact(id)
                .map(contact -> new Person(id, contact.displayName(), contact.email()))
                .orElse(new Person(id, "Unavailable", ""));
    }

    /**
     * Returns a paginated list of submissions for an assignment with status filter and summary counts.
     */
    public SubmissionPage listSubmissions(Actor actor, UUID classId, UUID assignmentId,
            String filter, Boolean late, int page, int size) {
        validatePage(page, size);
        var listing = listRows(actor, classId, assignmentId, filter, late);
        return new SubmissionPage(
                listing.rows().stream().skip((long) (page - 1) * size).limit(size).toList(),
                listing.rows().size(), page, size, listing.summary(), listing.closesAt());
    }

    record Listing(List<SubmissionRow> rows,
            List<edu.aiplatform.grading.model.Evaluation> evaluations, Map<String, Long> summary,
            java.time.Instant closesAt) {
    }

    /**
     * Queries and maps all submission rows and evaluation states for either individual or group assignments.
     */
    Listing listRows(Actor actor, UUID classId, UUID assignmentId, String filter, Boolean late) {
        gradingAccess.requireTeacher(actor, classId, Actions.GRADING_REVIEW);
        var courseClass = classAccess.getClassRef(classId)
                .orElseThrow(() -> ApiException.notFound("CLASS_NOT_FOUND"));
        var assignment = assignmentQuery.get(assignmentId)
                .orElseThrow(() -> ApiException.notFound("ASSIGNMENT_NOT_FOUND"));
        requireAssignment(assignment, courseClass);
        var evals = evaluationRepository.findForAssignmentAndClass(assignmentId, classId);
        var students = classAccess.listActiveStudents(classId);
        return buildListing(classId, assignmentId, assignment, evals, filter, late, students,
                accountLookup.getContacts(students));
    }

    public Map<UUID, SubmissionPage> loadProgress(Actor actor, UUID classId) {
        gradingAccess.requireTeacher(actor, classId, Actions.GRADEBOOK_READ);
        gradingAccess.requireTeacher(actor, classId, Actions.GRADING_REVIEW);
        var courseClass = classAccess.getClassRef(classId)
                .orElseThrow(() -> ApiException.notFound("CLASS_NOT_FOUND"));
        var students = classAccess.listActiveStudents(classId);
        var contacts = accountLookup.getContacts(students);
        var all = evaluationRepository.findForClass(classId).stream()
                .collect(java.util.stream.Collectors.groupingBy(e -> e.assignmentId));
        var result = new LinkedHashMap<UUID, SubmissionPage>();
        for (var assignment : assignmentQuery.listForClass(classId)) {
            if (!GradebookService.isGradable(assignment)) continue;
            requireAssignment(assignment, courseClass);
            var listing = buildListing(classId, assignment.assignmentId(), assignment,
                    all.getOrDefault(assignment.assignmentId(), List.of()), null, null, students, contacts);
            result.put(assignment.assignmentId(), new SubmissionPage(listing.rows().stream().limit(1).toList(),
                    listing.rows().size(), 1, 1, listing.summary(), listing.closesAt()));
        }
        return result;
    }

    private Listing buildListing(UUID classId, UUID assignmentId, AssignmentView assignment,
            List<edu.aiplatform.grading.model.Evaluation> evals, String filter, Boolean late, List<UUID> students,
            Map<UUID, edu.aiplatform.identity.contract.AccountContact> contacts) {
        List<SubmissionRow> rows = new ArrayList<>();
        if (assignment.submissionMode() == SubmissionMode.GROUP) {
            var provider = groupSubmissionSourceProvider.getIfAvailable();
            if (provider == null)
                throw ApiException.unavailable("GROUP_SUBMISSIONS_UNAVAILABLE",
                        "Group submission source unavailable");
            for (var submission : provider.listGroupSubmissions(assignmentId, classId)) {
                var evaluation = evals.stream()
                        .filter(candidate -> candidate.kind == EvaluationKind.GROUP_DOCUMENT
                                && submission.groupDocumentId().equals(candidate.groupDocumentId)
                                && candidate.state != EvaluationStatus.SUPERSEDED)
                        .findFirst().orElse(null);
                rows.add(new SubmissionRow(
                        new Person(submission.groupId(), submission.groupName(), ""),
                        evaluation == null ? null : evaluation.id, null,
                        evaluation == null ? (submission.submitted() ? "PENDING" : "NOT_SUBMITTED")
                                : evaluation.state.name(),
                        evaluation == null ? null : evaluation.score,
                        evaluation == null ? null : evaluation.maxScore, submission.late(),
                        submission.submittedAt(), submission.submitted() ? 1 : 0,
                        evaluation == null ? 0 : evaluation.version));
            }
        } else {
            Map<UUID, StudentSubmissionStatus> source = new HashMap<>();
            for (var submission : submissionQuery.listSubmissions(assignmentId, classId))
                source.put(submission.accountId(), submission);
            for (var sid : students) {
                var submission = source.get(sid);
                var evaluation = submission == null ? null
                        : evals.stream().filter(
                                candidate -> candidate.kind == EvaluationKind.ATTEMPT && Objects
                                        .equals(candidate.attemptId, submission.gradedAttemptId()))
                                .findFirst().orElse(null);
                String status = evaluation != null ? evaluation.state.name()
                        : submission == null || submission.latestStatus() == null ? "NOT_SUBMITTED"
                                : submission.latestStatus() == AttemptStatus.IN_PROGRESS
                                        ? "IN_PROGRESS"
                                        : "PENDING";
                var contact = contacts.get(sid);
                var student = contact == null ? new Person(sid, "Unavailable", "")
                        : new Person(sid, contact.displayName(), contact.email());
                rows.add(new SubmissionRow(student, evaluation == null ? null : evaluation.id,
                        submission == null ? null : submission.gradedAttemptId(), status,
                        evaluation == null ? null : evaluation.score,
                        evaluation == null ? null : evaluation.maxScore,
                        submission != null && submission.late(),
                        submission == null ? null : submission.submittedAt(),
                        submission == null ? 0 : submission.attemptCount(),
                        evaluation == null ? 0 : evaluation.version));
            }
        }
        Map<String, Long> summary = new LinkedHashMap<>();
        summary.put("total", (long) rows.size());
        summary.put("submitted", rows.stream().filter(r -> r.submittedAt() != null).count());
        summary.put("inProgress",
                rows.stream().filter(r -> r.status().equals("IN_PROGRESS")).count());
        summary.put("notSubmitted",
                rows.stream().filter(r -> r.status().equals("NOT_SUBMITTED")).count());
        summary.put("late", rows.stream().filter(SubmissionRow::late).count());
        for (var state : EvaluationStatus.values())
            summary.put(state.name(),
                    rows.stream().filter(r -> r.status().equals(state.name())).count());
        if (filter != null && !filter.isBlank() && !filter.equals("ALL") && !Set
                .of("PENDING", "DRAFT", "FINALIZED", "PUBLISHED", "NOT_SUBMITTED", "IN_PROGRESS")
                .contains(filter))
            throw ApiException.badRequest("INVALID_FILTER", "Unknown status filter");
        var filtered = rows.stream()
                .filter(r -> filter == null || filter.isBlank() || filter.equals("ALL")
                        || r.status().equals(filter))
                .filter(r -> late == null || r.late() == late).toList();
        return new Listing(filtered, evals, summary, assignment.closesAt());
    }

    /**
     * Loads full evaluation details without filtering adjacent navigation links.
     */
    public EvaluationDetail loadEvaluation(Actor actor, UUID id) {
        return loadEvaluation(actor, id, null, null);
    }

    /**
     * Loads comprehensive evaluation details including rubric breakdown, submitted document, activity monitoring, and navigation.
     */
    public EvaluationDetail loadEvaluation(Actor actor, UUID id, String status, Boolean late) {
        var evaluation = evaluationRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("GRADE_NOT_FOUND"));
        gradingAccess.requireTeacher(actor, evaluation.classId, Actions.GRADING_REVIEW);
        var assignment = assignmentQuery.get(evaluation.assignmentId).orElseThrow(
                () -> ApiException.unavailable("ASSIGNMENT_UNAVAILABLE", "Assignment unavailable"));
        var courseClass = classAccess.getClassRef(evaluation.classId)
                .orElseThrow(() -> ApiException.notFound("CLASS_NOT_FOUND"));
        requireAssignment(assignment, courseClass);
        DocumentModel document;
        UUID activityOwner = evaluation.accountId;
        WorkRef ref;
        List<GroupSectionAuthor> authors = List.of();
        if (evaluation.kind == EvaluationKind.ATTEMPT) {
            var submission = submissionQuery.getSubmission(evaluation.attemptId)
                    .orElseThrow(() -> ApiException.unavailable("SUBMISSION_UNAVAILABLE",
                            "Submission unavailable"));
            if (!evaluation.submissionHash.equals(submission.receiptHash()))
                throw ApiException.conflict("SUBMISSION_CHANGED", "Submitted snapshot changed");
            document = submission.content();
            ref = new WorkRef(WorkKind.ASSIGNMENT_ATTEMPT, evaluation.attemptId,
                    evaluation.classId);
        } else {
            var provider = groupSubmissionSourceProvider.getIfAvailable();
            if (provider == null)
                throw ApiException.unavailable("GROUP_SUBMISSIONS_UNAVAILABLE",
                        "Group submission unavailable");
            var submission = provider.getGroupSubmission(evaluation.groupDocumentId)
                    .orElseThrow(() -> ApiException.unavailable("GROUP_SUBMISSIONS_UNAVAILABLE",
                            "Group submission unavailable"));
            if (!evaluation.submissionHash.equals(submission.submissionHash())
                    || !evaluation.classId.equals(submission.classId())
                    || !evaluation.assignmentId.equals(submission.assignmentId()))
                throw ApiException.conflict("SUBMISSION_CHANGED", "Group submission changed");
            document = submission.content();
            authors = submission.sectionAuthors();
            ref = new WorkRef(WorkKind.GROUP_DOCUMENT, evaluation.groupDocumentId,
                    evaluation.classId);
        }
        ActivitySummary activityValue = null;
        String activityState = "DISABLED";
        if (assignment.activityMonitoringEnabled() && activityOwner != null) {
            try {
                activityValue = activityQuery.summary(ref, activityOwner);
                activityState = activityValue == null ? "UNAVAILABLE"
                        : activityValue.policyState().name();
            } catch (RuntimeException ex) {
                activityState = "UNAVAILABLE";
            }
        }
        var all = evaluationRepository.findForAssignmentAndClass(evaluation.assignmentId,
                evaluation.classId);
        Set<UUID> latestAttempts = new HashSet<>();
        if (evaluation.kind == EvaluationKind.ATTEMPT)
            for (var row : submissionQuery.listSubmissions(evaluation.assignmentId,
                    evaluation.classId))
                if (row.gradedAttemptId() != null)
                    latestAttempts.add(row.gradedAttemptId());
        boolean current = evaluation.state != EvaluationStatus.SUPERSEDED
                && (evaluation.kind != EvaluationKind.ATTEMPT
                        || latestAttempts.contains(evaluation.attemptId));
        var navigationIds = all.stream()
                .filter(candidate -> candidate.kind != EvaluationKind.MEMBER
                        && candidate.state != EvaluationStatus.SUPERSEDED
                        && (candidate.kind != EvaluationKind.ATTEMPT
                                || latestAttempts.contains(candidate.attemptId)))
                .sorted(Comparator.comparing(candidate -> candidate.id))
                .map(candidate -> candidate.id).toList();
        if (status != null || late != null) {
            navigationIds = listRows(actor, evaluation.classId, evaluation.assignmentId, status,
                    late).rows().stream()
                    .filter(row -> row.evaluationId() != null && !row.status().equals("SUPERSEDED"))
                    .map(SubmissionRow::evaluationId).toList();
        }
        var members = all.stream()
                .filter(candidate -> candidate.kind == EvaluationKind.MEMBER
                        && Objects.equals(evaluation.groupDocumentId, candidate.groupDocumentId)
                        && candidate.state != EvaluationStatus.SUPERSEDED)
                .toList();
        List<MemberActivity> memberActivities = new ArrayList<>();
        if (evaluation.kind == EvaluationKind.GROUP_DOCUMENT) {
            activityState = assignment.activityMonitoringEnabled() ? "PER_MEMBER" : "DISABLED";
            for (var member : members) {
                ActivitySummary summary = null;
                String state = "DISABLED";
                if (assignment.activityMonitoringEnabled()) {
                    try {
                        summary = activityQuery.summary(ref, member.accountId);
                        state = summary == null ? "UNAVAILABLE" : summary.policyState().name();
                    } catch (RuntimeException ex) {
                        state = "UNAVAILABLE";
                    }
                }
                memberActivities
                        .add(new MemberActivity(mapPerson(member.accountId), summary, state));
            }
        }
        UUID groupEvaluationId = evaluation.kind == EvaluationKind.MEMBER
                ? all.stream()
                        .filter(candidate -> candidate.kind == EvaluationKind.GROUP_DOCUMENT
                                && candidate.state != EvaluationStatus.SUPERSEDED
                                && evaluation.isCurrentMemberOf(candidate))
                        .map(candidate -> candidate.id).findFirst().orElse(null)
                : null;
        return new EvaluationDetail(evaluation, document,
                GradingJson.readRubrics(objectMapper, evaluation.rubricSnapshot), navigationIds,
                activityValue, activityState, members, memberActivities, authors, current,
                groupEvaluationId, current
                        && courseClass.status() == edu.aiplatform.academics.contract.ClassStatus.OPEN);
    }
}
