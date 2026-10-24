package edu.aiplatform.grading.application;

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
import edu.aiplatform.grading.application.GradingViews.EvaluationDetail;
import edu.aiplatform.grading.application.GradingViews.MemberActivity;
import edu.aiplatform.grading.application.GradingViews.Person;
import edu.aiplatform.grading.application.GradingViews.SubmissionPage;
import edu.aiplatform.grading.application.GradingViews.SubmissionRow;
import edu.aiplatform.grading.contract.GroupSectionAuthor;
import edu.aiplatform.grading.contract.IGroupSubmissionSource;
import edu.aiplatform.grading.domain.EvaluationKind;
import edu.aiplatform.grading.domain.EvaluationStatus;
import edu.aiplatform.grading.domain.IEvaluationRepository;
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
    private final IEvaluationRepository evaluations;
    private final GradingAccess access;
    private final IAssignmentQuery assignments;
    private final ISubmissionQuery submissions;
    private final IClassAccess classes;
    private final IAccountLookup accounts;
    private final IActivityQuery activity;
    private final ObjectProvider<IGroupSubmissionSource> groups;
    private final ObjectMapper json;

    public SubmissionReviewService(IEvaluationRepository evaluations, GradingAccess access,
            IAssignmentQuery assignments, ISubmissionQuery submissions, IClassAccess classes, IAccountLookup accounts,
            IActivityQuery activity, ObjectProvider<IGroupSubmissionSource> groups, ObjectMapper json) {
        this.evaluations = evaluations;
        this.access = access;
        this.assignments = assignments;
        this.submissions = submissions;
        this.classes = classes;
        this.accounts = accounts;
        this.activity = activity;
        this.groups = groups;
        this.json = json;
    }

    public static void requireAssignment(AssignmentView assignment, ClassRef courseClass) {
        if (assignment.gradingMode() != GradingMode.GRADED
                || !(assignment.scope() == AssignmentScope.CLASS ? courseClass.classId().equals(assignment.classId())
                        : courseClass.subjectId().equals(assignment.subjectId())))
            throw ApiException.notFound("ASSIGNMENT_NOT_FOUND");
    }

    public static void page(int page, int size) {
        if (page < 1 || size < 1 || size > 100 || page > 100000)
            throw ApiException.badRequest("INVALID_PAGE", "Invalid page or size");
    }

    Person person(UUID id) {
        return accounts.getContact(id).map(contact -> new Person(id, contact.displayName(), contact.email()))
                .orElse(new Person(id, "Unavailable", ""));
    }

    public SubmissionPage list(Actor actor, UUID classId, UUID assignmentId, String filter, Boolean late, int page,
            int size) {
        page(page, size);
        access.requireTeacher(actor, classId, Actions.GRADING_REVIEW);
        var courseClass = classes.getClassRef(classId).orElseThrow(() -> ApiException.notFound("CLASS_NOT_FOUND"));
        var assignment = assignments.get(assignmentId).orElseThrow(() -> ApiException.notFound("ASSIGNMENT_NOT_FOUND"));
        requireAssignment(assignment, courseClass);
        var evals = evaluations.findForAssignmentAndClass(assignmentId, classId);
        List<SubmissionRow> rows = new ArrayList<>();
        if (assignment.submissionMode() == SubmissionMode.GROUP) {
            var provider = groups.getIfAvailable();
            if (provider == null)
                throw ApiException.unavailable("GROUP_SUBMISSIONS_UNAVAILABLE", "Group submission source unavailable");
            for (var submission : provider.listGroupSubmissions(assignmentId, classId)) {
                var evaluation = evals.stream()
                        .filter(candidate -> candidate.kind == EvaluationKind.GROUP_DOCUMENT
                                && submission.groupDocumentId().equals(candidate.groupDocumentId)
                                && candidate.state != EvaluationStatus.SUPERSEDED)
                        .findFirst().orElse(null);
                rows.add(new SubmissionRow(new Person(submission.groupId(), submission.groupName(), ""),
                        evaluation == null ? null : evaluation.id, null,
                        evaluation == null ? (submission.submitted() ? "PENDING" : "NOT_SUBMITTED")
                                : evaluation.state.name(),
                        evaluation == null ? null : evaluation.score, evaluation == null ? null : evaluation.maxScore,
                        submission.late(), submission.submittedAt(), submission.submitted() ? 1 : 0,
                        evaluation == null ? 0 : evaluation.version));
            }
        } else {
            Map<UUID, StudentSubmissionStatus> source = new HashMap<>();
            for (var submission : submissions.listSubmissions(assignmentId, classId))
                source.put(submission.accountId(), submission);
            for (var sid : classes.listActiveStudents(classId)) {
                var submission = source.get(sid);
                var evaluation = submission == null ? null
                        : evals.stream()
                                .filter(candidate -> candidate.kind == EvaluationKind.ATTEMPT
                                        && Objects.equals(candidate.attemptId, submission.gradedAttemptId()))
                                .findFirst().orElse(null);
                String status = evaluation != null ? evaluation.state.name()
                        : submission == null || submission.latestStatus() == null ? "NOT_SUBMITTED"
                                : submission.latestStatus() == AttemptStatus.IN_PROGRESS ? "IN_PROGRESS" : "PENDING";
                rows.add(new SubmissionRow(person(sid), evaluation == null ? null : evaluation.id,
                        submission == null ? null : submission.gradedAttemptId(), status,
                        evaluation == null ? null : evaluation.score, evaluation == null ? null : evaluation.maxScore,
                        submission != null && submission.late(), submission == null ? null : submission.submittedAt(),
                        submission == null ? 0 : submission.attemptCount(),
                        evaluation == null ? 0 : evaluation.version));
            }
        }
        Map<String, Long> summary = new LinkedHashMap<>();
        summary.put("total", (long) rows.size());
        summary.put("submitted", rows.stream().filter(r -> r.submittedAt() != null).count());
        summary.put("inProgress", rows.stream().filter(r -> r.status().equals("IN_PROGRESS")).count());
        summary.put("notSubmitted", rows.stream().filter(r -> r.status().equals("NOT_SUBMITTED")).count());
        summary.put("late", rows.stream().filter(SubmissionRow::late).count());
        for (var state : EvaluationStatus.values())
            summary.put(state.name(), rows.stream().filter(r -> r.status().equals(state.name())).count());
        if (filter != null && !filter.isBlank() && !filter.equals("ALL") && !Set
                .of("PENDING", "DRAFT", "FINALIZED", "PUBLISHED", "NOT_SUBMITTED", "IN_PROGRESS").contains(filter))
            throw ApiException.badRequest("INVALID_FILTER", "Unknown status filter");
        var filtered = rows.stream()
                .filter(r -> filter == null || filter.isBlank() || filter.equals("ALL") || r.status().equals(filter))
                .filter(r -> late == null || r.late() == late).toList();
        return new SubmissionPage(filtered.stream().skip((long) (page - 1) * size).limit(size).toList(),
                filtered.size(), page, size, summary, assignment.closesAt());
    }

    public EvaluationDetail detail(Actor actor, UUID id) {
        var evaluation = evaluations.findById(id).orElseThrow(() -> ApiException.notFound("GRADE_NOT_FOUND"));
        access.requireTeacher(actor, evaluation.classId, Actions.GRADING_REVIEW);
        var assignment = assignments.get(evaluation.assignmentId)
                .orElseThrow(() -> ApiException.unavailable("ASSIGNMENT_UNAVAILABLE", "Assignment unavailable"));
        var courseClass = classes.getClassRef(evaluation.classId)
                .orElseThrow(() -> ApiException.notFound("CLASS_NOT_FOUND"));
        requireAssignment(assignment, courseClass);
        DocumentModel document;
        UUID activityOwner = evaluation.accountId;
        WorkRef ref;
        List<GroupSectionAuthor> authors = List.of();
        if (evaluation.kind == EvaluationKind.ATTEMPT) {
            var submission = submissions.getSubmission(evaluation.attemptId)
                    .orElseThrow(() -> ApiException.unavailable("SUBMISSION_UNAVAILABLE", "Submission unavailable"));
            if (!evaluation.submissionHash.equals(submission.receiptHash()))
                throw ApiException.conflict("SUBMISSION_CHANGED", "Submitted snapshot changed");
            document = submission.content();
            ref = new WorkRef(WorkKind.ASSIGNMENT_ATTEMPT, evaluation.attemptId, evaluation.classId);
        } else {
            var provider = groups.getIfAvailable();
            if (provider == null)
                throw ApiException.unavailable("GROUP_SUBMISSIONS_UNAVAILABLE", "Group submission unavailable");
            var submission = provider.getGroupSubmission(evaluation.groupDocumentId).orElseThrow(
                    () -> ApiException.unavailable("GROUP_SUBMISSIONS_UNAVAILABLE", "Group submission unavailable"));
            if (!evaluation.submissionHash.equals(submission.submissionHash())
                    || !evaluation.classId.equals(submission.classId())
                    || !evaluation.assignmentId.equals(submission.assignmentId()))
                throw ApiException.conflict("SUBMISSION_CHANGED", "Group submission changed");
            document = submission.content();
            authors = submission.sectionAuthors();
            ref = new WorkRef(WorkKind.GROUP_DOCUMENT, evaluation.groupDocumentId, evaluation.classId);
        }
        ActivitySummary activityValue = null;
        String activityState = "DISABLED";
        if (assignment.activityMonitoringEnabled() && activityOwner != null) {
            try {
                activityValue = activity.summary(ref, activityOwner);
                activityState = activityValue == null ? "UNAVAILABLE" : activityValue.policyState().name();
            } catch (RuntimeException ex) {
                activityState = "UNAVAILABLE";
            }
        }
        var all = evaluations.findForAssignmentAndClass(evaluation.assignmentId, evaluation.classId);
        Set<UUID> latestAttempts = new HashSet<>();
        if (evaluation.kind == EvaluationKind.ATTEMPT)
            for (var row : submissions.listSubmissions(evaluation.assignmentId, evaluation.classId))
                if (row.gradedAttemptId() != null)
                    latestAttempts.add(row.gradedAttemptId());
        boolean current = evaluation.state != EvaluationStatus.SUPERSEDED
                && (evaluation.kind != EvaluationKind.ATTEMPT || latestAttempts.contains(evaluation.attemptId));
        var navigationIds = all.stream()
                .filter(candidate -> candidate.kind != EvaluationKind.MEMBER
                        && candidate.state != EvaluationStatus.SUPERSEDED
                        && (candidate.kind != EvaluationKind.ATTEMPT || latestAttempts.contains(candidate.attemptId)))
                .sorted(Comparator.comparing(candidate -> candidate.id)).map(candidate -> candidate.id).toList();
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
                        summary = activity.summary(ref, member.accountId);
                        state = summary == null ? "UNAVAILABLE" : summary.policyState().name();
                    } catch (RuntimeException ex) {
                        state = "UNAVAILABLE";
                    }
                }
                memberActivities.add(new MemberActivity(person(member.accountId), summary, state));
            }
        }
        return new EvaluationDetail(evaluation, document, GradingJson.rubrics(json, evaluation.rubricSnapshot),
                navigationIds, activityValue, activityState, members, memberActivities, authors, current);
    }
}
