package edu.aiplatform.grading.service;

import edu.aiplatform.academics.contract.ClassStatus;
import edu.aiplatform.academics.contract.IClassAccess;
import edu.aiplatform.assignments.contract.AssignmentScope;
import edu.aiplatform.assignments.contract.AssignmentStatus;
import edu.aiplatform.assignments.contract.IAssignmentQuery;
import edu.aiplatform.audit.contract.AuditEvent;
import edu.aiplatform.audit.contract.AuditResult;
import edu.aiplatform.audit.contract.IAudit;
import edu.aiplatform.grading.repository.GradeDistributionVisibilityRepository;
import edu.aiplatform.identity.contract.Actions;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.shared.error.ApiException;
import java.time.Clock;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class GradeDistributionVisibilityService {
    public record View(boolean enabled, long version, boolean canEdit) { }

    private final GradeDistributionVisibilityRepository repository;
    private final GradingAccess access;
    private final IClassAccess classes;
    private final IAssignmentQuery assignments;
    private final IAudit audit;
    private final Clock clock;

    public GradeDistributionVisibilityService(GradeDistributionVisibilityRepository repository, GradingAccess access,
            IClassAccess classes, IAssignmentQuery assignments, IAudit audit, Clock clock) {
        this.repository = repository;
        this.access = access;
        this.classes = classes;
        this.assignments = assignments;
        this.audit = audit;
        this.clock = clock;
    }

    public View get(Actor actor, UUID classId, UUID assignmentId) {
        boolean canEdit = requireAssignedTeacher(actor, classId, assignmentId);
        var current = repository.find(classId, assignmentId);
        return new View(current.enabled(), current.version(), canEdit);
    }

    @Transactional
    public View update(Actor actor, UUID classId, UUID assignmentId, boolean enabled, long version) {
        if (!requireAssignedTeacher(actor, classId, assignmentId))
            throw ApiException.conflict("GRADE_DISTRIBUTION_READ_ONLY", "Archived class or retired assignment");
        if (version < 0)
            throw ApiException.badRequest("INVALID_VERSION", "Version must be nonnegative");
        if (!repository.update(classId, assignmentId, enabled, version))
            throw ApiException.conflict("GRADE_DISTRIBUTION_VERSION_CONFLICT", "Visibility changed; reload and retry");
        audit.record(new AuditEvent(UUID.randomUUID(), actor.accountId(), "GRADE_DISTRIBUTION_VISIBILITY_CHANGED",
                "ASSIGNMENT", assignmentId.toString(), AuditResult.SUCCESS, null,
                Map.of("classId", classId.toString(), "enabled", enabled), clock.instant()));
        return new View(enabled, version + 1, true);
    }

    private boolean requireAssignedTeacher(Actor actor, UUID classId, UUID assignmentId) {
        access.requireTeacher(actor, classId, Actions.GRADING_WRITE);
        var courseClass = classes.getClassRef(classId).orElseThrow(() -> ApiException.notFound("CLASS_NOT_FOUND"));
        if (actor == null || !actor.accountId().equals(courseClass.teacherId()))
            throw ApiException.notFound("GRADE_NOT_FOUND");
        var assignment = assignments.get(assignmentId).orElseThrow(() -> ApiException.notFound("ASSIGNMENT_NOT_FOUND"));
        SubmissionReviewService.requireAssignment(assignment, courseClass);
        if (assignment.scope() == AssignmentScope.SUBJECT
                && (assignment.status() == AssignmentStatus.DRAFT || assignment.status() == AssignmentStatus.REVIEWED))
            throw ApiException.notFound("ASSIGNMENT_NOT_FOUND");
        return courseClass.status() != ClassStatus.ARCHIVED && assignment.status() != AssignmentStatus.RETIRED;
    }
}
