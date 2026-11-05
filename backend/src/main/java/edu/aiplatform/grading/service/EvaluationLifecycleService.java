package edu.aiplatform.grading.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.academics.contract.IClassAccess;
import edu.aiplatform.assignments.contract.GradingMode;
import edu.aiplatform.assignments.contract.IAssignmentQuery;
import edu.aiplatform.assignments.contract.IRubric;
import edu.aiplatform.assignments.contract.SubmissionMode;
import edu.aiplatform.assignmentwork.contract.ISubmissionListener;
import edu.aiplatform.assignmentwork.contract.ISubmissionQuery;
import edu.aiplatform.grading.contract.IGroupEvaluation;
import edu.aiplatform.grading.model.Evaluation;
import edu.aiplatform.grading.model.EvaluationKind;
import edu.aiplatform.grading.model.EvaluationStatus;
import edu.aiplatform.grading.repository.IEvaluationRepository;
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
    private final IClassAccess classAccess;
    private final IEvaluationRepository evaluationRepository;
    private final ISubmissionQuery submissionQuery;
    private final IAssignmentQuery assignmentQuery;
    private final IRubric rubric;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public EvaluationLifecycleService(IEvaluationRepository evaluationRepository, ISubmissionQuery submissionQuery,
            IAssignmentQuery assignmentQuery, IRubric rubric, ObjectMapper objectMapper, Clock clock, IClassAccess classAccess) {
        this.classAccess = classAccess;
        this.evaluationRepository = evaluationRepository;
        this.submissionQuery = submissionQuery;
        this.assignmentQuery = assignmentQuery;
        this.rubric = rubric;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void onSubmitted(UUID attemptId) {
        var submission = submissionQuery.getSubmission(attemptId).orElseThrow(
                () -> ApiException.unavailable("SUBMISSION_UNAVAILABLE", "Submitted snapshot unavailable"));
        var assignment = assignmentQuery.get(submission.assignmentId())
                .orElseThrow(() -> ApiException.unavailable("ASSIGNMENT_UNAVAILABLE", "Assignment unavailable"));
        if (assignment.gradingMode() != GradingMode.GRADED)
            return;
        if (assignment.submissionMode() == SubmissionMode.GROUP)
            throw ApiException.badRequest("INVALID_SUBMISSION_KIND", "Group submission requires group listener");
        evaluationRepository.lockIdentity(submission.classId(), submission.assignmentId(), submission.accountId());
        if (evaluationRepository.findByAttemptId(attemptId).isPresent())
            return;
        for (var previous : evaluationRepository.findForAssignmentAndClass(submission.assignmentId(), submission.classId()))
            if (previous.kind == EvaluationKind.ATTEMPT && submission.accountId().equals(previous.accountId)) {
                var locked = evaluationRepository.lockById(previous.id).orElseThrow();
                locked.supersede();
                evaluationRepository.save(locked);
            }
        evaluationRepository.save(Evaluation.createPendingEvaluation(attemptId, submission.assignmentId(), submission.classId(),
                submission.accountId(), submission.receiptHash(), rubric.maxPoints(submission.assignmentId()),
                GradingJson.writeSnapshot(objectMapper, rubric.listRubrics(submission.assignmentId())), clock.instant()));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void onGroupSubmitted(UUID documentId, String hash, UUID assignmentId, UUID classId, List<UUID> members) {
        var assignment = assignmentQuery.get(assignmentId)
                .orElseThrow(() -> ApiException.unavailable("ASSIGNMENT_UNAVAILABLE", "Assignment unavailable"));
        if (assignment.gradingMode() != GradingMode.GRADED || assignment.submissionMode() != SubmissionMode.GROUP)
            throw ApiException.badRequest("INVALID_GROUP_ASSIGNMENT", "Invalid group assignment");
        SubmissionReviewService.requireAssignment(assignment, classAccess.getClassRef(classId)
                .orElseThrow(() -> ApiException.unavailable("CLASS_UNAVAILABLE", "Class unavailable")));
        if (hash == null || hash.isBlank() || members == null || members.isEmpty()
                || members.stream().anyMatch(Objects::isNull) || new HashSet<>(members).size() != members.size())
            throw ApiException.badRequest("INVALID_GROUP_SUBMISSION", "Invalid group snapshot");
        evaluationRepository.lockIdentity(classId, assignmentId, documentId);
        var previousEvaluations = evaluationRepository.findForAssignmentAndClass(assignmentId, classId).stream()
                .filter(evaluation -> documentId.equals(evaluation.groupDocumentId))
                .sorted(Comparator.comparing(evaluation -> evaluation.id)).toList();
        var root = previousEvaluations.stream().filter(evaluation -> evaluation.kind == EvaluationKind.GROUP_DOCUMENT)
                .findFirst();
        if (root.isPresent() && hash.equals(root.get().submissionHash))
            return;
        var rubricSnapshot = GradingJson.writeSnapshot(objectMapper, rubric.listRubrics(assignmentId));
        var maxScore = rubric.maxPoints(assignmentId);
        // Root first, then members: same lock order as grading/publishing.
        var group = root.isPresent() ? evaluationRepository.lockById(root.get().id).orElseThrow()
                : Evaluation.createPendingEvaluation(null, assignmentId, classId, null, hash, maxScore, rubricSnapshot,
                        clock.instant());
        group.kind = EvaluationKind.GROUP_DOCUMENT;
        group.groupDocumentId = documentId;
        resetEvaluation(group, hash, maxScore, rubricSnapshot);
        evaluationRepository.save(group);
        Set<UUID> retained = new HashSet<>();
        for (var previous : previousEvaluations)
            if (previous.kind == EvaluationKind.MEMBER) {
                var evaluation = evaluationRepository.lockById(previous.id).orElseThrow();
                if (members.contains(evaluation.accountId)) {
                    resetEvaluation(evaluation, hash, maxScore, rubricSnapshot);
                    retained.add(evaluation.accountId);
                } else
                    evaluation.state = EvaluationStatus.SUPERSEDED;
                evaluationRepository.save(evaluation);
            }
        for (var member : members)
            if (!retained.contains(member)) {
                var evaluation = Evaluation.createPendingEvaluation(null, assignmentId, classId, member, hash, maxScore, rubricSnapshot,
                        clock.instant());
                evaluation.kind = EvaluationKind.MEMBER;
                evaluation.groupDocumentId = documentId;
                evaluationRepository.save(evaluation);
            }
    }

    private void resetEvaluation(Evaluation evaluation, String hash, BigDecimal maxScore, String rubricSnapshot) {
        evaluation.resetForGroupSubmission(hash, maxScore, rubricSnapshot, clock.instant());
    }
}
