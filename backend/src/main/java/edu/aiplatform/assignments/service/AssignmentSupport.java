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
import edu.aiplatform.assignments.model.AssignmentOrigin;
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
    Assignment loadReadable(Actor actor, UUID id, String action) {
        Assignment assignment = assignments.findAssignment(id).orElseThrow(() -> access.notFound());
        access.requireRead(actor, assignment, action);
        return assignment;
    }

    Assignment loadManageable(Actor actor, UUID id, String action) {
        Assignment assignment = assignments.findAssignment(id).orElseThrow(() -> access.notFound());
        access.requireManage(actor, assignment, action);
        return assignment;
    }

    /** Khóa dòng, kiểm quyền soạn, lớp hoặc môn chưa lưu trữ, {@code version} khớp (BR-U07-04, 70). */
    Assignment lock(Actor actor, UUID id, long version, String action) {
        Assignment assignment = assignments.findForUpdate(id).orElseThrow(() -> access.notFound());
        access.requireManage(actor, assignment, action);
        access.requireOwnerWritable(assignment);
        requireVersion(assignment, version);
        return assignment;
    }

    /** Khóa bài còn sửa được nội dung (DRAFT, REVIEWED). */
    Assignment lockEditable(Actor actor, UUID id, long version, String action) {
        Assignment assignment = lock(actor, id, version, action);
        if (!assignment.canEdit()) {
            throw ApiException.createConflict("ASSIGNMENT_NOT_EDITABLE",
                    "Bài đã phát hành, không sửa nội dung được. Hãy nhân bản hoặc tạo version mới");
        }
        return assignment;
    }

    /**
     * Tên đề mẫu không trùng trong môn (yêu cầu 2026-11-27): so với các bài của môn chưa ngưng giao, không phân biệt hoa
     * thường và khoảng trắng thừa. Các version cùng chuỗi (NEW_VERSION) được giữ chung tên. Chỉ kiểm khi tên đổi, để
     * bài cùng tên có từ trước vẫn lưu được các thay đổi khác.
     */
    void requireUniqueTemplateTitle(Assignment current, String nextTitle) {
        if (current.scope() != AssignmentScope.SUBJECT || current.status() == AssignmentStatus.RETIRED
                || matchesTitle(current.title(), nextTitle)) {
            return;
        }
        requireUniqueTemplateTitle(current.subjectId(), nextTitle, current);
    }

    /** Tên do người dùng nhập cho bài mới của môn; {@code lineage}: bài cùng chuỗi version, null nếu không có. */
    void requireUniqueTemplateTitle(UUID subjectId, String title, Assignment lineage) {
        assignments.lockSubjectTitles(subjectId);
        if (isTemplateTitleTaken(subjectId, title, lineage)) {
            throw ApiException.createConflict("TEMPLATE_TITLE_DUPLICATE",
                    "Môn đã có đề mẫu tên này. Hãy đặt tên khác");
        }
    }

    /**
     * Tên tự đặt cho bài mới của môn (tên mặc định, nhân bản, copy không nhập tên): trùng thì thêm " (2)", " (3)"…,
     * cắt bớt tên gốc để không quá {@link Assignment#TITLE_MAX} ký tự.
     */
    String resolveTemplateTitle(UUID subjectId, String base) {
        assignments.lockSubjectTitles(subjectId);
        if (!isTemplateTitleTaken(subjectId, base, null)) {
            return base;
        }
        for (int n = 2; ; n++) {
            String suffix = " (" + n + ")";
            String head = base.length() + suffix.length() > Assignment.TITLE_MAX
                    ? base.substring(0, Assignment.TITLE_MAX - suffix.length()).strip() : base;
            String candidate = head + suffix;
            if (!isTemplateTitleTaken(subjectId, candidate, null)) {
                return candidate;
            }
        }
    }

    private boolean isTemplateTitleTaken(UUID subjectId, String title, Assignment self) {
        List<Assignment> all = assignments.listOfSubject(subjectId);
        Map<UUID, Assignment> byId = new HashMap<>();
        all.forEach(a -> byId.put(a.id(), a));
        UUID selfRoot = self == null ? null : findVersionRoot(self, byId);
        return all.stream().anyMatch(a -> a.status() != AssignmentStatus.RETIRED
                && (self == null || !a.id().equals(self.id()))
                && (selfRoot == null || !selfRoot.equals(findVersionRoot(a, byId)))
                && matchesTitle(a.title(), title));
    }

    private static UUID findVersionRoot(Assignment assignment, Map<UUID, Assignment> byId) {
        Assignment at = assignment;
        while (at.origin() == AssignmentOrigin.NEW_VERSION && at.sourceAssignmentId() != null
                && byId.containsKey(at.sourceAssignmentId())) {
            at = byId.get(at.sourceAssignmentId());
        }
        return at.id();
    }

    private static boolean matchesTitle(String left, String right) {
        return comparableTitle(left).equals(comparableTitle(right));
    }

    private static String comparableTitle(String title) {
        return title == null ? "" : title.strip().replaceAll("\\s+", " ").toLowerCase(java.util.Locale.ROOT);
    }

    static void requireVersion(Assignment assignment, long version) {
        if (assignment.version() != version) {
            throw createVersionConflictError();
        }
    }

    static ApiException createVersionConflictError() {
        return ApiException.createConflict("ASSIGNMENT_VERSION_CONFLICT", "Bài đã thay đổi ở nơi khác. Hãy tải lại");
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

    AssignmentViews.Row buildRow(Assignment a, boolean readOnly) {
        return new AssignmentViews.Row(a.id(), a.scope(), a.classId(), a.subjectId(), a.title(), a.submissionMode(),
                a.gradingMode(), a.diagramEnabled(), a.status(), a.versionNo(), a.origin(), a.opensAt(), a.closesAt(),
                a.lateUntil(), a.maxAttempts(), a.createdAt(), a.updatedAt(), readOnly);
    }

    AssignmentViews.Detail buildDetail(Actor actor, Assignment a, List<ValidationIssue> warnings) {
        boolean manage = access.canManage(actor, a);
        boolean archived = access.isOwnerArchived(a);
        boolean write = manage && !archived;
        List<AssignmentViews.Part> parts = buildDetailParts(a);
        BigDecimal total = parts.stream().map(AssignmentViews.Part::totalPoints).reduce(BigDecimal.ZERO, BigDecimal::add);
        AssignmentStatus status = a.status();
        AssignmentViews.Permissions permissions = new AssignmentViews.Permissions(manage,
                write && a.canEdit(),
                write && (status == AssignmentStatus.SCHEDULED || status == AssignmentStatus.OPEN),
                write && (status == AssignmentStatus.SCHEDULED || status == AssignmentStatus.OPEN),
                write && a.canEdit(),
                write && status == AssignmentStatus.DRAFT,
                // Phải hoàn tất bài (REVIEWED) rồi mới xếp lịch giao (yêu cầu 2026-11-11); bài của môn là đề mẫu,
                // không phát hành (yêu cầu 2026-11-09).
                write && status == AssignmentStatus.REVIEWED && a.scope() == AssignmentScope.CLASS,
                write,
                write && (status == AssignmentStatus.CLOSED || status == AssignmentStatus.RETIRED),
                archived);
        return new AssignmentViews.Detail(a.id(), a.scope(), a.classId(), a.subjectId(), resolveOwnerName(a),
                a.sourceAssignmentId(), a.origin(), a.versionNo(), a.submissionMode(), a.diagramEnabled(),
                a.gradingMode(), a.title(), a.instructions(), DocumentFiles.hideFiles(a.skeleton()), parts, total,
                a.requiredDiagrams(), a.referenceMaterialIds(), a.activityMonitoringEnabled(), a.activityNotice(),
                status, a.opensAt(), a.closesAt(), a.lateUntil(), a.maxAttempts(),
                a.retiredReason(), a.createdAt(), a.updatedAt(), a.version(), permissions,
                a.scope() == AssignmentScope.CLASS && groupSupported(a.classId()), List.copyOf(warnings));
    }

    private List<AssignmentViews.Part> buildDetailParts(Assignment a) {
        Map<String, AssignmentRubric> byPart = new HashMap<>();
        rubrics.listByAssignment(a.id()).forEach(r -> byPart.put(r.partId(), r));
        List<AssignmentViews.Part> parts = new ArrayList<>();
        AssignmentRubric whole = byPart.get(RubricService.WHOLE);
        if (whole != null && RubricService.hasWhole(a.parts())
                && (!whole.criteria().isEmpty() || a.parts().isEmpty())) {
            parts.add(new AssignmentViews.Part(RubricService.WHOLE, null, List.of(), RubricService.WHOLE_TITLE,
                    whole.id(), whole.criteria().size(), whole.totalPoints(), whole.complete(), whole.hasLocked()));
        }
        for (DocumentPart part : a.parts()) {
            AssignmentRubric rubric = byPart.get(part.partId());
            BigDecimal points = rubric == null ? BigDecimal.ZERO : rubric.totalPoints();
            parts.add(new AssignmentViews.Part(part.partId(), part.headingBlockId(), part.ancestorHeadingIds(),
                    part.title(), rubric == null ? null : rubric.id(), rubric == null ? 0 : rubric.criteria().size(),
                    points, rubric != null && rubric.complete(), rubric != null && rubric.hasLocked()));
        }
        return parts;
    }

    String resolveOwnerName(Assignment a) {
        try {
            if (a.scope() == AssignmentScope.CLASS) {
                var ref = access.loadClassRef(a.classId());
                // Lớp không còn tên (yêu cầu 2026-11-02): mã môn rồi mã lớp.
                return ref.subjectCode() == null ? ref.code() : ref.subjectCode() + " · " + ref.code();
            }
            return subjects.getSubject(a.subjectId()).map(s -> s.code() + " · " + s.name()).orElse("");
        } catch (RuntimeException e) {
            return "";
        }
    }

    boolean isClassArchived(UUID classId) {
        return access.loadClassRef(classId).status() == ClassStatus.ARCHIVED;
    }

    boolean isSubjectArchived(UUID subjectId) {
        return access.loadSubjectStatus(subjectId) == SubjectStatus.ARCHIVED;
    }
}
