package edu.aiplatform.assignments.service;

import edu.aiplatform.assignments.dto.AssignmentViews;

import edu.aiplatform.academics.contract.ClassStatus;
import edu.aiplatform.assignments.contract.AssignmentScope;
import edu.aiplatform.assignments.contract.AssignmentStatus;
import edu.aiplatform.assignments.contract.DocumentFiles;
import edu.aiplatform.assignments.contract.DocumentPart;
import edu.aiplatform.assignments.contract.IGroupAssignmentSupport;
import edu.aiplatform.assignments.contract.ReadinessResult;
import edu.aiplatform.assignments.contract.ValidationIssue;
import edu.aiplatform.assignments.model.Assignment;
import edu.aiplatform.assignments.repository.AssignmentRepository;
import edu.aiplatform.assignments.model.AssignmentRubric;
import edu.aiplatform.assignments.repository.RubricRepository;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.shared.error.ApiException;
import edu.aiplatform.subjects.contract.ISubjectQuery;
import edu.aiplatform.subjects.contract.SubjectStatus;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Phần dùng chung của các service U07: tìm, khóa bài để sửa, dựng dữ liệu trả về, hỏi U03, U12. */
@Component
class AssignmentSupport {

    private final AssignmentRepository assignments;
    private final RubricRepository rubrics;
    private final AssignmentAccess access;
    private final IGroupAssignmentSupport groups;
    private final ISubjectQuery subjects;

    AssignmentSupport(AssignmentRepository assignments, RubricRepository rubrics, AssignmentAccess access,
                      IGroupAssignmentSupport groups, ISubjectQuery subjects) {
        this.assignments = assignments;
        this.rubrics = rubrics;
        this.access = access;
        this.groups = groups;
        this.subjects = subjects;
    }

    /** Bài tồn tại và người gọi đọc được; không thì 404 kèm audit. */
    Assignment readable(Actor actor, UUID id, String action) {
        Assignment assignment = assignments.find(id).orElseThrow(() -> access.deny(actor, action,
                AssignmentAudit.OBJECT_ASSIGNMENT, id, "NOT_FOUND"));
        access.requireRead(actor, assignment, action);
        return assignment;
    }

    Assignment manageable(Actor actor, UUID id, String action) {
        Assignment assignment = assignments.find(id).orElseThrow(() -> access.deny(actor, action,
                AssignmentAudit.OBJECT_ASSIGNMENT, id, "NOT_FOUND"));
        access.requireManage(actor, assignment, action);
        return assignment;
    }

    /** Khóa dòng, kiểm quyền soạn, lớp hoặc môn chưa lưu trữ, {@code version} khớp (BR-U07-04, 70). */
    Assignment lock(Actor actor, UUID id, long version, String action) {
        Assignment assignment = assignments.findForUpdate(id).orElseThrow(() -> access.deny(actor, action,
                AssignmentAudit.OBJECT_ASSIGNMENT, id, "NOT_FOUND"));
        access.requireManage(actor, assignment, action);
        access.requireOwnerWritable(assignment);
        requireVersion(assignment, version);
        return assignment;
    }

    /** Khóa bài còn sửa được nội dung (DRAFT, REVIEWED). */
    Assignment lockEditable(Actor actor, UUID id, long version, String action) {
        Assignment assignment = lock(actor, id, version, action);
        if (!assignment.editable()) {
            throw ApiException.conflict("ASSIGNMENT_NOT_EDITABLE",
                    "Bài đã phát hành, không sửa nội dung được. Hãy nhân bản hoặc tạo version mới");
        }
        return assignment;
    }

    static void requireVersion(Assignment assignment, long version) {
        if (assignment.version() != version) {
            throw versionConflict();
        }
    }

    static ApiException versionConflict() {
        return ApiException.conflict("ASSIGNMENT_VERSION_CONFLICT", "Bài đã thay đổi ở nơi khác. Hãy tải lại");
    }

    /** U12 chưa cài hoặc lỗi thì không có bài nhóm (BR-U07-03, 72). */
    boolean groupSupported(UUID classId) {
        if (classId == null) {
            return false;
        }
        try {
            ReadinessResult result = groups.checkReadiness(classId);
            return result != null && !result.errors().contains(IGroupAssignmentSupport.UNAVAILABLE);
        } catch (RuntimeException e) {
            return false;
        }
    }

    ReadinessResult groupReadiness(UUID classId) {
        try {
            ReadinessResult result = groups.checkReadiness(classId);
            return result == null ? new ReadinessResult(List.of(IGroupAssignmentSupport.UNAVAILABLE), List.of()) : result;
        } catch (RuntimeException e) {
            return new ReadinessResult(List.of(IGroupAssignmentSupport.UNAVAILABLE), List.of());
        }
    }

    AssignmentViews.Row row(Assignment a, boolean readOnly) {
        return new AssignmentViews.Row(a.id(), a.scope(), a.classId(), a.subjectId(), a.title(), a.submissionMode(),
                a.gradingMode(), a.diagramEnabled(), a.status(), a.versionNo(), a.origin(), a.opensAt(), a.closesAt(),
                a.lateUntil(), a.maxAttempts(), a.updatedAt(), readOnly);
    }

    AssignmentViews.Detail detail(Actor actor, Assignment a, List<ValidationIssue> warnings) {
        boolean manage = access.canManage(actor, a);
        boolean archived = access.ownerArchived(a);
        boolean write = manage && !archived;
        Map<String, AssignmentRubric> byPart = new HashMap<>();
        rubrics.listByAssignment(a.id()).forEach(r -> byPart.put(r.partId(), r));
        List<AssignmentViews.Part> parts = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (DocumentPart part : a.parts()) {
            AssignmentRubric rubric = byPart.get(part.partId());
            BigDecimal points = rubric == null ? BigDecimal.ZERO : rubric.totalPoints();
            total = total.add(points);
            parts.add(new AssignmentViews.Part(part.partId(), part.headingBlockId(), part.ancestorHeadingIds(),
                    part.title(), rubric == null ? null : rubric.id(), rubric == null ? 0 : rubric.criteria().size(),
                    points, rubric != null && rubric.complete(), rubric != null && rubric.locked()));
        }
        AssignmentStatus status = a.status();
        AssignmentViews.Permissions permissions = new AssignmentViews.Permissions(manage,
                write && a.editable(),
                write && (status == AssignmentStatus.SCHEDULED || status == AssignmentStatus.OPEN),
                write && (status == AssignmentStatus.SCHEDULED || status == AssignmentStatus.OPEN),
                write && a.editable(),
                write && status == AssignmentStatus.DRAFT,
                // Phát hành thẳng từ bài nháp (yêu cầu 2026-10-22 bỏ trạng thái Đã duyệt).
                write && a.editable(),
                write,
                write && (status == AssignmentStatus.CLOSED || status == AssignmentStatus.RETIRED),
                archived);
        return new AssignmentViews.Detail(a.id(), a.scope(), a.classId(), a.subjectId(), ownerName(a),
                a.sourceAssignmentId(), a.origin(), a.versionNo(), a.submissionMode(), a.diagramEnabled(),
                a.gradingMode(), a.title(), a.instructions(), DocumentFiles.hideFiles(a.skeleton()), parts, total,
                a.requiredDiagrams(), a.referenceMaterialIds(), a.activityMonitoringEnabled(), a.activityNotice(),
                status, a.opensAt(), a.closesAt(), a.lateUntil(), a.maxAttempts(),
                a.retiredReason(), a.createdAt(), a.updatedAt(), a.version(), permissions,
                a.scope() == AssignmentScope.CLASS && groupSupported(a.classId()), List.copyOf(warnings));
    }

    String ownerName(Assignment a) {
        try {
            if (a.scope() == AssignmentScope.CLASS) {
                var ref = access.classRef(a.classId());
                return ref.code() + " · " + ref.name();
            }
            return subjects.getSubject(a.subjectId()).map(s -> s.code() + " · " + s.name()).orElse("");
        } catch (RuntimeException e) {
            return "";
        }
    }

    boolean classArchived(UUID classId) {
        return access.classRef(classId).status() == ClassStatus.ARCHIVED;
    }

    boolean subjectArchived(UUID subjectId) {
        return access.subjectStatus(subjectId) == SubjectStatus.ARCHIVED;
    }
}
