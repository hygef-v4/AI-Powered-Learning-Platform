package edu.aiplatform.grading.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.assignmentwork.contract.ISubmissionQuery;
import edu.aiplatform.audit.contract.AuditEvent;
import edu.aiplatform.audit.contract.AuditResult;
import edu.aiplatform.audit.contract.IAudit;
import edu.aiplatform.grading.application.GradingCommands.ItemVersion;
import edu.aiplatform.grading.application.GradingCommands.MemberSave;
import edu.aiplatform.grading.application.GradingCommands.Save;
import edu.aiplatform.grading.domain.Evaluation;
import edu.aiplatform.grading.domain.EvaluationKind;
import edu.aiplatform.grading.domain.EvaluationStatus;
import edu.aiplatform.grading.domain.IEvaluationRepository;
import edu.aiplatform.identity.contract.Actions;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.jobs.contract.DomainEvent;
import edu.aiplatform.jobs.contract.EventTypes;
import edu.aiplatform.jobs.contract.IEventPublisher;
import edu.aiplatform.shared.error.ApiException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GradingService {
    private final IEvaluationRepository evaluations;
    private final GradingAccess access;
    private final IAudit audit;
    private final IEventPublisher events;
    private final ISubmissionQuery submissions;
    private final ObjectMapper json;
    private final Clock clock;

    public GradingService(IEvaluationRepository evaluations, GradingAccess access, IAudit audit, IEventPublisher events,
            ISubmissionQuery submissions, ObjectMapper json, Clock clock) {
        this.evaluations = evaluations;
        this.access = access;
        this.audit = audit;
        this.events = events;
        this.submissions = submissions;
        this.json = json;
        this.clock = clock;
    }

    public Evaluation requireCurrent(Actor actor, UUID id, long version) {
        var evaluation = evaluations.lockById(id).orElseThrow(() -> ApiException.notFound("GRADE_NOT_FOUND"));
        access.requireTeacher(actor, evaluation.classId, Actions.GRADING_WRITE);
        evaluation.checkVersion(version);
        evaluation.editable();
        if (evaluation.kind == EvaluationKind.ATTEMPT) {
            requireLatestSubmission(evaluation);
        }
        return evaluation;
    }

    private void requireLatestSubmission(Evaluation evaluation) {

        var submission = submissions.getSubmission(evaluation.attemptId)
                .orElseThrow(() -> ApiException.unavailable("SUBMISSION_UNAVAILABLE", "Submission unavailable"));
        if (!evaluation.submissionHash.equals(submission.receiptHash())
                || !evaluation.assignmentId.equals(submission.assignmentId())
                || !evaluation.classId.equals(submission.classId()))
            throw ApiException.conflict("SUBMISSION_CHANGED", "Submission snapshot changed");
        boolean latest = submissions.listSubmissions(evaluation.assignmentId, evaluation.classId).stream()
                .anyMatch(row -> evaluation.accountId.equals(row.accountId())
                        && evaluation.attemptId.equals(row.gradedAttemptId()));
        if (!latest)
            throw ApiException.conflict("NOT_LATEST_ATTEMPT", "Only the latest submitted attempt can be graded");
    }

    @Transactional
    public Evaluation save(Actor actor, UUID id, Save input) {
        if (input.usedProposalId() != null)
            throw ApiException.badRequest("PROPOSAL_REQUIRES_VALIDATION",
                    "Apply proposal through proposal review endpoint");
        return saveChecked(actor, id, input);
    }

    Evaluation saveChecked(Actor actor, UUID id, Save input) {
        var evaluation = requireCurrent(actor, id, input.version());
        if (evaluation.kind == EvaluationKind.MEMBER)
            throw ApiException.badRequest("MEMBER_ENDPOINT_REQUIRED", "Use member grading endpoint");
        var points = GradingJson.score(GradingJson.rubrics(json, evaluation.rubricSnapshot),
                input.checkedItemsByRubric());
        boolean changingOfficialGrade = evaluation.official();
        evaluation.save(GradingJson.write(json, input.checkedItemsByRubric()), points, input.adjustment(),
                input.adjustmentReason(), input.feedback(), input.changeReason(), actor.accountId(), clock.instant());
        propagateGroup(evaluation, actor, GroupOperation.SAVE, input.changeReason());
        evaluations.save(evaluation);
        record(actor, evaluation, changingOfficialGrade ? "GRADE_CHANGED" : "GRADE_SAVED", input.changeReason());
        return evaluation;
    }

    @Transactional
    public Evaluation finalizeOne(Actor actor, ItemVersion item) {
        var evaluation = requireCurrent(actor, item.evaluationId(), item.version());
        if (evaluation.kind == EvaluationKind.MEMBER)
            throw ApiException.badRequest("GROUP_ROOT_REQUIRED", "Finalize the group document");
        evaluation.finalizeGrade(actor.accountId(), clock.instant());
        propagateGroup(evaluation, actor, GroupOperation.FINALIZE, null);
        evaluations.save(evaluation);
        record(actor, evaluation, "GRADE_FINALIZED", null);
        return evaluation;
    }

    @Transactional
    public Evaluation publishOne(Actor actor, ItemVersion item) {
        var evaluation = requireCurrent(actor, item.evaluationId(), item.version());
        if (evaluation.kind == EvaluationKind.MEMBER)
            throw ApiException.badRequest("GROUP_ROOT_REQUIRED", "Publish the group document");
        evaluation.publish(actor.accountId(), clock.instant());
        var recipients = propagateGroup(evaluation, actor, GroupOperation.PUBLISH, null);
        if (evaluation.kind == EvaluationKind.ATTEMPT)
            recipients = List.of(evaluation.accountId);
        evaluations.save(evaluation);
        record(actor, evaluation, "GRADE_PUBLISHED", null);
        events.publish(new DomainEvent(1, UUID.randomUUID(), EventTypes.GRADE_PUBLISHED, "U09", clock.instant(),
                evaluation.id.toString(),
                Map.of("assignmentId", evaluation.assignmentId.toString(), "classId", evaluation.classId.toString(),
                        "studentIds", recipients.stream().map(UUID::toString).toList())));
        return evaluation;
    }

    @Transactional
    public Evaluation saveMember(Actor actor, UUID rootId, UUID accountId, MemberSave input) {
        var root = evaluations.findById(rootId).orElseThrow(() -> ApiException.notFound("GRADE_NOT_FOUND"));
        access.requireTeacher(actor, root.classId, Actions.GRADING_WRITE);
        root = evaluations.lockById(rootId).orElseThrow();
        root.editable();
        if (root.kind != EvaluationKind.GROUP_DOCUMENT || root.score == null)
            throw ApiException.conflict("GROUP_NOT_GRADED", "Grade group document first");
        final UUID documentId = root.groupDocumentId;
        final String hash = root.submissionHash;
        var candidate = evaluations.findForAssignmentAndClass(root.assignmentId, root.classId).stream()
                .filter(evaluation -> evaluation.kind == EvaluationKind.MEMBER
                        && documentId.equals(evaluation.groupDocumentId) && hash.equals(evaluation.submissionHash)
                        && accountId.equals(evaluation.accountId) && evaluation.state != EvaluationStatus.SUPERSEDED)
                .findFirst().orElseThrow(() -> ApiException.notFound("GRADE_NOT_FOUND"));
        var member = evaluations.lockById(candidate.id).orElseThrow();
        member.checkVersion(input.version());
        member.overrideMemberScore(input.score(), input.reason(), actor.accountId(), clock.instant());
        evaluations.save(member);
        record(actor, member, "GRADE_CHANGED", input.reason());
        return member;
    }

    private enum GroupOperation {
        SAVE, FINALIZE, PUBLISH
    }

    private List<UUID> propagateGroup(Evaluation root, Actor actor, GroupOperation operation, String reason) {
        if (root.kind != EvaluationKind.GROUP_DOCUMENT)
            return List.of();
        List<UUID> recipients = new ArrayList<>();
        var members = evaluations.findForAssignmentAndClass(root.assignmentId, root.classId).stream()
                .filter(member -> member.currentMemberOf(root))
                .sorted(Comparator.comparing(evaluation -> evaluation.id)).toList();
        for (var candidate : members) {
            var member = evaluations.lockById(candidate.id).orElseThrow();
            switch (operation) {
            case SAVE -> member.inheritGroupGrade(root, reason, actor.accountId(), clock.instant());
            case FINALIZE -> member.finalizeGrade(actor.accountId(), clock.instant());
            case PUBLISH -> member.publish(actor.accountId(), clock.instant());
            }
            evaluations.save(member);
            recipients.add(member.accountId);
        }
        if (recipients.isEmpty())
            throw ApiException.conflict("GROUP_MEMBERS_UNAVAILABLE", "Group member snapshot unavailable");
        return recipients;
    }

    void record(Actor actor, Evaluation evaluation, String action, String reason) {
        audit.record(new AuditEvent(UUID.randomUUID(), actor.accountId(), action, "EVALUATION",
                evaluation.id.toString(), AuditResult.SUCCESS, reason, Map.of("classId", evaluation.classId.toString()),
                clock.instant()));
    }
}
