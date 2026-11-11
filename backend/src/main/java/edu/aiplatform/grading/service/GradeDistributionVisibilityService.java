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
    public record View(boolean enabled, long version, boolean canEdit) {
    }

    private final GradeDistributionVisibilityRepository gradeDistributionVisibilityRepository;
    private final GradingAccess gradingAccess;
    private final IClassAccess classAccess;
    private final IAssignmentQuery assignmentQuery;
    private final IAudit audit;
    private final Clock clock;

    /** Creates the visibility service with its access and persistence dependencies. */
    public GradeDistributionVisibilityService(
            GradeDistributionVisibilityRepository gradeDistributionVisibilityRepository,
            GradingAccess gradingAccess, IClassAccess classAccess, IAssignmentQuery assignmentQuery,
            IAudit audit, Clock clock) {
        this.gradeDistributionVisibilityRepository = gradeDistributionVisibilityRepository;
        this.gradingAccess = gradingAccess;
        this.classAccess = classAccess;
        this.assignmentQuery = assignmentQuery;
        this.audit = audit;
        this.clock = clock;
    }

    /** Loads grade distribution visibility and whether the actor can edit it. */
    public View loadVisibility(Actor actor, UUID classId, UUID assignmentId) {
        // Step 1: Check teacher access and class/assignment mutability
        boolean canEdit = requireAssignedTeacher(actor, classId, assignmentId);

        // Step 2: Fetch current visibility flag and optimistic version from database
        var current = gradeDistributionVisibilityRepository.findVisibility(classId, assignmentId);
        return new View(current.enabled(), current.version(), canEdit);
    }

    /** Updates visibility when the actor is the assigned teacher and the version matches. */
    @Transactional
    public View updateVisibility(Actor actor, UUID classId, UUID assignmentId, boolean enabled,
            long version) {
        // Step 1: Ensure class is active and assignment is not retired
        if (!requireAssignedTeacher(actor, classId, assignmentId))
            throw ApiException.conflict("GRADE_DISTRIBUTION_READ_ONLY",
                    "Archived class or retired assignment");
        gradingAccess.requireTeacher(actor, classId, Actions.GRADING_WRITE);
        if (version < 0)
            throw ApiException.badRequest("INVALID_VERSION", "Version must be nonnegative");

        // Step 2: Update visibility in DB using optimistic locking
        if (!gradeDistributionVisibilityRepository.updateVisibility(classId, assignmentId, enabled,
                version))
            throw ApiException.conflict("GRADE_DISTRIBUTION_VERSION_CONFLICT",
                    "Visibility changed; reload and retry");

        // Step 3: Record audit trail event for visibility toggle
        audit.record(new AuditEvent(UUID.randomUUID(), actor.accountId(),
                "GRADE_DISTRIBUTION_VISIBILITY_CHANGED", "ASSIGNMENT", assignmentId.toString(),
                AuditResult.SUCCESS, null,
                Map.of("classId", classId.toString(), "enabled", enabled), clock.instant()));
        return new View(enabled, version + 1, true);
    }

    /** Checks whether the actor is the teacher assigned to the requested class assignment. */
    private boolean requireAssignedTeacher(Actor actor, UUID classId, UUID assignmentId) {
        // Step 1: Verify teacher permission and course class existence
        gradingAccess.requireTeacher(actor, classId, Actions.GRADING_REVIEW);
        var courseClass = classAccess.getClassRef(classId)
                .orElseThrow(() -> ApiException.notFound("CLASS_NOT_FOUND"));
        if (actor == null || !actor.accountId().equals(courseClass.teacherId()))
            throw ApiException.notFound("GRADE_NOT_FOUND");

        // Step 2: Verify assignment belongs to class/subject and is published
        var assignment = assignmentQuery.get(assignmentId)
                .orElseThrow(() -> ApiException.notFound("ASSIGNMENT_NOT_FOUND"));
        SubmissionReviewService.requireAssignment(assignment, courseClass);
        if (assignment.scope() == AssignmentScope.SUBJECT
                && (assignment.status() == AssignmentStatus.DRAFT
                        || assignment.status() == AssignmentStatus.REVIEWED))
            throw ApiException.notFound("ASSIGNMENT_NOT_FOUND");

        // Step 3: Teacher can edit only if class is unarchived and assignment is not retired
        return courseClass.status() != ClassStatus.ARCHIVED
                && assignment.status() != AssignmentStatus.RETIRED;
    }
}
