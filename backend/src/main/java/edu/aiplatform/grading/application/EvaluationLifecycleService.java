package edu.aiplatform.grading.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.academics.contract.IClassAccess;
import edu.aiplatform.assignments.contract.GradingMode;
import edu.aiplatform.assignments.contract.IAssignmentQuery;
import edu.aiplatform.assignments.contract.IRubric;
import edu.aiplatform.assignments.contract.SubmissionMode;
import edu.aiplatform.assignmentwork.contract.ISubmissionListener;
import edu.aiplatform.assignmentwork.contract.ISubmissionQuery;
import edu.aiplatform.grading.contract.IGroupEvaluation;
import edu.aiplatform.grading.domain.Evaluation;
import edu.aiplatform.grading.domain.EvaluationKind;
import edu.aiplatform.grading.domain.EvaluationStatus;
import edu.aiplatform.grading.domain.IEvaluationRepository;
import edu.aiplatform.shared.error.ApiException;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EvaluationLifecycleService implements ISubmissionListener, IGroupEvaluation {
    private final IClassAccess classes;
    private final IEvaluationRepository evaluations;
    private final ISubmissionQuery submissions;
    private final IAssignmentQuery assignments;
    private final IRubric rubric;
    private final ObjectMapper json;
    private final Clock clock;

    public EvaluationLifecycleService(IEvaluationRepository evaluations, ISubmissionQuery submissions,
            IAssignmentQuery assignments, IRubric rubric, ObjectMapper json, Clock clock, IClassAccess classes) {
        this.classes = classes;
        this.evaluations = evaluations;
        this.submissions = submissions;
        this.assignments = assignments;
        this.rubric = rubric;
        this.json = json;
        this.clock = clock;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void onSubmitted(UUID attemptId) {
        var submission = submissions.getSubmission(attemptId).orElseThrow(
                () -> ApiException.unavailable("SUBMISSION_UNAVAILABLE", "Submitted snapshot unavailable"));
        var assignment = assignments.get(submission.assignmentId())
                .orElseThrow(() -> ApiException.unavailable("ASSIGNMENT_UNAVAILABLE", "Assignment unavailable"));
        if (assignment.gradingMode() != GradingMode.GRADED)
            return;
        if (assignment.submissionMode() == SubmissionMode.GROUP)
            throw ApiException.badRequest("INVALID_SUBMISSION_KIND", "Group submission requires group listener");
        evaluations.lockIdentity(submission.classId(), submission.assignmentId(), submission.accountId());
        if (evaluations.findByAttemptId(attemptId).isPresent())
            return;
        for (var previous : evaluations.findForAssignmentAndClass(submission.assignmentId(), submission.classId()))
            if (previous.kind == EvaluationKind.ATTEMPT && submission.accountId().equals(previous.accountId)) {
                var locked = evaluations.lockById(previous.id).orElseThrow();
                locked.supersede();
                evaluations.save(locked);
            }
        evaluations.save(Evaluation.pending(attemptId, submission.assignmentId(), submission.classId(),
                submission.accountId(), submission.receiptHash(), rubric.maxPoints(submission.assignmentId()),
                GradingJson.write(json, rubric.listRubrics(submission.assignmentId())), clock.instant()));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void onGroupSubmitted(UUID documentId, String hash, UUID assignmentId, UUID classId, List<UUID> members) {
        var assignment = assignments.get(assignmentId)
                .orElseThrow(() -> ApiException.unavailable("ASSIGNMENT_UNAVAILABLE", "Assignment unavailable"));
        if (assignment.gradingMode() != GradingMode.GRADED || assignment.submissionMode() != SubmissionMode.GROUP)
            throw ApiException.badRequest("INVALID_GROUP_ASSIGNMENT", "Invalid group assignment");
        SubmissionReviewService.requireAssignment(assignment, classes.getClassRef(classId)
                .orElseThrow(() -> ApiException.unavailable("CLASS_UNAVAILABLE", "Class unavailable")));
        if (hash == null || hash.isBlank() || members == null || members.isEmpty()
                || members.stream().anyMatch(Objects::isNull) || new HashSet<>(members).size() != members.size())
            throw ApiException.badRequest("INVALID_GROUP_SUBMISSION", "Invalid group snapshot");
        evaluations.lockIdentity(classId, assignmentId, documentId);
        var previousEvaluations = evaluations.findForAssignmentAndClass(assignmentId, classId).stream()
                .filter(evaluation -> documentId.equals(evaluation.groupDocumentId))
                .sorted(Comparator.comparing(evaluation -> evaluation.id)).toList();
        var root = previousEvaluations.stream().filter(evaluation -> evaluation.kind == EvaluationKind.GROUP_DOCUMENT)
                .findFirst();
        if (root.isPresent() && hash.equals(root.get().submissionHash))
            return;
        var rubricSnapshot = GradingJson.write(json, rubric.listRubrics(assignmentId));
        var maxScore = rubric.maxPoints(assignmentId);
        // Root first, then members: same lock order as grading/publishing.
        var group = root.isPresent() ? evaluations.lockById(root.get().id).orElseThrow()
                : Evaluation.pending(null, assignmentId, classId, null, hash, maxScore, rubricSnapshot,
                        clock.instant());
        group.kind = EvaluationKind.GROUP_DOCUMENT;
        group.groupDocumentId = documentId;
        reset(group, hash, maxScore, rubricSnapshot);
        evaluations.save(group);
        Set<UUID> retained = new HashSet<>();
        for (var previous : previousEvaluations)
            if (previous.kind == EvaluationKind.MEMBER) {
                var evaluation = evaluations.lockById(previous.id).orElseThrow();
                if (members.contains(evaluation.accountId)) {
                    reset(evaluation, hash, maxScore, rubricSnapshot);
                    retained.add(evaluation.accountId);
                } else
                    evaluation.state = EvaluationStatus.SUPERSEDED;
                evaluations.save(evaluation);
            }
        for (var member : members)
            if (!retained.contains(member)) {
                var evaluation = Evaluation.pending(null, assignmentId, classId, member, hash, maxScore, rubricSnapshot,
                        clock.instant());
                evaluation.kind = EvaluationKind.MEMBER;
                evaluation.groupDocumentId = documentId;
                evaluations.save(evaluation);
            }
    }

    private void reset(Evaluation evaluation, String hash, BigDecimal maxScore, String rubricSnapshot) {
        evaluation.resetForGroupSubmission(hash, maxScore, rubricSnapshot, clock.instant());
    }
}
