package edu.aiplatform.assignments.application;

import edu.aiplatform.academics.contract.ClassRef;
import edu.aiplatform.academics.contract.ClassStatus;
import edu.aiplatform.ai.contract.IReferenceMaterial;
import edu.aiplatform.ai.contract.SelectableMaterial;
import edu.aiplatform.assignments.contract.AssignmentScope;
import edu.aiplatform.assignments.contract.AssignmentStatus;
import edu.aiplatform.assignments.contract.BlockType;
import edu.aiplatform.assignments.contract.DiagramType;
import edu.aiplatform.assignments.contract.DocumentBlock;
import edu.aiplatform.assignments.contract.DocumentFiles;
import edu.aiplatform.assignments.contract.DocumentModel;
import edu.aiplatform.assignments.contract.DocumentPart;
import edu.aiplatform.assignments.contract.GradingMode;
import edu.aiplatform.assignments.contract.SubmissionMode;
import edu.aiplatform.assignments.contract.ValidationIssue;
import edu.aiplatform.assignments.contract.ValidationResult;
import edu.aiplatform.assignments.domain.Assignment;
import edu.aiplatform.assignments.domain.AssignmentOrigin;
import edu.aiplatform.assignments.domain.AssignmentRepository;
import edu.aiplatform.assignments.domain.AssignmentRubric;
import edu.aiplatform.assignments.domain.PartCalculator;
import edu.aiplatform.files.contract.ArtifactPurpose;
import edu.aiplatform.files.contract.FileRef;
import edu.aiplatform.files.contract.IArtifact;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.ResourceRef;
import edu.aiplatform.shared.error.ApiException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Danh sách, tạo, soạn đề và khung, rubric, duyệt, xóa (F1–F5, F8, F14). */
@Service
public class AssignmentAuthoringService {

    static final String DEFAULT_TITLE = "Bài tập chưa đặt tên";
    private static final Set<AssignmentStatus> HIDDEN_BY_DEFAULT = Set.of(AssignmentStatus.CLOSED,
            AssignmentStatus.RETIRED);

    private final AssignmentRepository assignments;
    private final RubricService rubrics;
    private final DocumentModelService documents;
    private final AssignmentAccess access;
    private final AssignmentSupport support;
    private final AssignmentAudit audit;
    private final IArtifact artifacts;
    private final IReferenceMaterial materials;
    private final Clock clock;

    public AssignmentAuthoringService(AssignmentRepository assignments, RubricService rubrics,
                                      DocumentModelService documents, AssignmentAccess access,
                                      AssignmentSupport support, AssignmentAudit audit, IArtifact artifacts,
                                      IReferenceMaterial materials, Clock clock) {
        this.assignments = assignments;
        this.rubrics = rubrics;
        this.documents = documents;
        this.access = access;
        this.support = support;
        this.audit = audit;
        this.artifacts = artifacts;
        this.materials = materials;
        this.clock = clock;
    }

    /** F1: bài của lớp và bài của môn chứa lớp (chỉ đọc); mặc định ẩn bài đã đóng, ngưng giao. */
    @Transactional(readOnly = true)
    public AssignmentViews.Page listForClass(Actor actor, UUID classId, Set<AssignmentStatus> statuses,
                                             boolean includeClosed, String query, GradingMode gradingMode) {
        ClassRef ref = access.requireClassManager(actor, classId, "ASSIGNMENT_LIST");
        boolean archived = ref.status() == ClassStatus.ARCHIVED;
        List<Assignment> all = assignments.listForClass(classId, ref.subjectId());
        return page(all, a -> archived || a.scope() == AssignmentScope.SUBJECT, statuses, includeClosed, query,
                gradingMode, support.groupSupported(classId), archived);
    }

    /** F2: bài của môn cho Chủ nhiệm môn. */
    @Transactional(readOnly = true)
    public AssignmentViews.Page listForSubject(Actor actor, UUID subjectId, Set<AssignmentStatus> statuses,
                                               boolean includeClosed, String query, GradingMode gradingMode) {
        access.requireSubjectManager(actor, subjectId, "ASSIGNMENT_LIST");
        boolean archived = support.subjectArchived(subjectId);
        return page(assignments.listOfSubject(subjectId), a -> archived, statuses, includeClosed, query, gradingMode,
                false, archived);
    }

    /** F3 bài của lớp (BR-U07-02, 03, 04). */
    @Transactional
    public AssignmentViews.Detail createForClass(Actor actor, UUID classId, AssignmentCommands.Create command) {
        ClassRef ref = access.requireClassManager(actor, classId, "ASSIGNMENT_CREATED");
        if (ref.status() == ClassStatus.ARCHIVED) {
            throw ApiException.conflict("ASSIGNMENT_READ_ONLY", "Lớp đã lưu trữ, không tạo bài được");
        }
        modes(command.submissionMode(), command.gradingMode());
        if (command.submissionMode() == SubmissionMode.GROUP && !support.groupSupported(classId)) {
            throw ApiException.badRequest("GROUP_NOT_SUPPORTED", "Chưa dùng được bài nhóm cho lớp này");
        }
        return create(actor, AssignmentScope.CLASS, classId, ref.subjectId(), command);
    }

    /** F3 bài của môn: chỉ cá nhân, Chủ nhiệm môn soạn (BR-U07-02). */
    @Transactional
    public AssignmentViews.Detail createForSubject(Actor actor, UUID subjectId, AssignmentCommands.Create command) {
        access.requireSubjectManager(actor, subjectId, "ASSIGNMENT_CREATED");
        if (support.subjectArchived(subjectId)) {
            throw ApiException.conflict("ASSIGNMENT_READ_ONLY", "Môn đã lưu trữ, không tạo bài được");
        }
        if (command.submissionMode() == SubmissionMode.GROUP) {
            throw ApiException.badRequest("GROUP_NOT_ALLOWED", "Bài của môn chỉ có hình thức cá nhân");
        }
        modes(command.submissionMode(), command.gradingMode());
        return create(actor, AssignmentScope.SUBJECT, null, subjectId, command);
    }

    @Transactional(readOnly = true)
    public AssignmentViews.Detail detail(Actor actor, UUID id) {
        return support.detail(actor, support.readable(actor, id, "ASSIGNMENT_VIEW"), List.of());
    }

    /**
     * F4 bước 2: tiêu đề, hướng dẫn, cài đặt (BR-U07-13, 16, 17). Sơ đồ luôn bật nên {@code diagramEnabled} gửi lên
     * bị bỏ qua (yêu cầu 2026-10-21 thay BR-U07-14).
     */
    @Transactional
    public AssignmentViews.Detail updateDetails(Actor actor, UUID id, AssignmentCommands.Details command) {
        Assignment current = support.lockEditable(actor, id, command.version(), "ASSIGNMENT_UPDATED");
        String title = command.title() == null ? current.title() : title(command.title());
        String instructions = command.instructions() == null ? current.instructions()
                : instructions(command.instructions());
        GradingMode grading = command.gradingMode() == null ? current.gradingMode() : command.gradingMode();
        modes(current.submissionMode(), grading);
        Map<DiagramType, Integer> required = command.requiredDiagrams() == null ? current.requiredDiagrams()
                : requiredDiagrams(command.requiredDiagrams());
        List<UUID> chosen = command.referenceMaterialIds() == null ? current.referenceMaterialIds()
                : materials(actor, current, command.referenceMaterialIds());
        boolean activity = command.activityMonitoringEnabled() == null ? current.activityMonitoringEnabled()
                : command.activityMonitoringEnabled();
        String notice = command.activityNotice() == null ? current.activityNotice() : notice(command.activityNotice());
        Assignment saved = assignments.save(current.withDetails(title, instructions, true, grading, required,
                chosen, activity, notice, clock.instant()));
        return support.detail(actor, saved, List.of());
    }

    /** F4 bước 4–6: lưu khung, tính lại phần, đồng bộ rubric (BR-U07-10…12, 20; P1). */
    @Transactional
    public AssignmentViews.Detail saveSkeleton(Actor actor, UUID id, AssignmentCommands.Skeleton command) {
        Assignment current = support.lockEditable(actor, id, command.version(), "ASSIGNMENT_SKELETON_SAVED");
        if (command.skeleton() == null || command.skeleton().blocks() == null) {
            throw ApiException.badRequest("SKELETON_INVALID", "Khung tài liệu không hợp lệ");
        }
        String instructions = command.instructions() == null ? current.instructions()
                : instructions(command.instructions());
        DocumentModel pending = DocumentFiles.resolveFiles(current.skeleton(), documents.sanitize(command.skeleton()),
                null);
        ValidationResult result = documents.validateSkeleton(pending);
        List<ValidationIssue> errors = new ArrayList<>(result.errors());
        if (!current.diagramEnabled()) {
            pending.blocks().stream().filter(b -> b != null && b.type() == BlockType.DIAGRAM).forEach(b -> errors.add(
                    new ValidationIssue("DIAGRAM_DISABLED", b.id(), "Bài chưa bật sơ đồ")));
        }
        if (!errors.isEmpty()) {
            throw AssignmentProblem.issues(HttpStatus.BAD_REQUEST, "SKELETON_INVALID", "Khung tài liệu có lỗi", errors);
        }
        DocumentModel stored = DocumentFiles.resolveFiles(current.skeleton(), pending,
                ref -> artifacts.attach(new FileRef(ref), actor, ArtifactPurpose.DOCUMENT_IMAGE).fileId());
        Instant now = clock.instant();
        List<DocumentPart> parts = PartCalculator.compute(stored);
        Assignment saved = assignments.save(current.withSkeleton(instructions, stored, parts, now));
        rubrics.syncParts(saved.id(), parts, now);
        if (command.fromDocx()) {
            audit.success(actor, "ASSIGNMENT_DOCX_IMPORTED", saved.id(), AssignmentAudit.details(
                    "blockCount", stored.blocks().size(), "partCount", parts.size()));
        }
        return support.detail(actor, saved, result.warnings());
    }

    /** F14 (BR-U07-31): chỉ bài chưa phát hành; rubric, đề xuất AI xóa theo. */
    @Transactional
    public void delete(Actor actor, UUID id, long version) {
        Assignment current = support.lockEditable(actor, id, version, "ASSIGNMENT_DELETED");
        if (assignments.delete(current.id(), version) != 1) {
            throw AssignmentSupport.versionConflict();
        }
        audit.success(actor, "ASSIGNMENT_DELETED", current.id(), AssignmentAudit.details("scope", current.scope(),
                "classId", current.classId(), "subjectId", current.subjectId(), "status", current.status()));
    }

    /** F8 (BR-U07-24, 32): kiểm đủ rồi chuyển REVIEWED. */
    @Transactional
    public AssignmentViews.Detail review(Actor actor, UUID id, long version) {
        Assignment current = support.lockEditable(actor, id, version, "ASSIGNMENT_REVIEWED");
        if (current.status() != AssignmentStatus.DRAFT) {
            throw ApiException.conflict("ASSIGNMENT_STATUS_INVALID", "Chỉ duyệt bài đang nháp");
        }
        List<ValidationIssue> issues = readinessIssues(current);
        if (!issues.isEmpty()) {
            throw AssignmentProblem.issues(HttpStatus.CONFLICT, "ASSIGNMENT_NOT_READY", "Bài chưa đủ để duyệt", issues);
        }
        Assignment saved = assignments.save(current.withStatus(AssignmentStatus.REVIEWED, clock.instant()));
        audit.success(actor, "ASSIGNMENT_REVIEWED", saved.id(), AssignmentAudit.details("versionNo", saved.versionNo()));
        return support.detail(actor, saved, List.of());
    }

    /** F5 bước 1: rubric của một phần; bài đã phát hành chỉ xem. */
    @Transactional(readOnly = true)
    public AssignmentViews.Rubric rubric(Actor actor, UUID id, String partId) {
        Assignment assignment = support.readable(actor, id, "RUBRIC_VIEW");
        return rubricView(actor, assignment, partId);
    }

    /** F5 bước 3–4: ghi đè rubric; bài đã duyệt về nháp (BR-U07-21, 23). */
    @Transactional
    public AssignmentViews.Rubric updateRubric(Actor actor, UUID id, String partId,
                                               AssignmentCommands.RubricUpdate command) {
        Assignment current = support.lockEditable(actor, id, command.version(), "RUBRIC_UPDATED");
        DocumentPart part = part(current, partId);
        AssignmentRubric rubric = rubrics.forPart(current, part.partId())
                .orElseThrow(() -> ApiException.notFound("RUBRIC_NOT_FOUND"));
        if (rubric.locked()) {
            throw ApiException.conflict("RUBRIC_LOCKED", "Rubric đã khóa khi phát hành");
        }
        Instant now = clock.instant();
        AssignmentRubric next = rubrics.save(rubric.withCriteria(
                RubricService.normalize(command.criteria(), current.diagramEnabled()), now));
        Assignment saved = assignments.save(current.touched(now));
        audit.success(actor, "RUBRIC_UPDATED", saved.id(), AssignmentAudit.details("partId", part.partId(),
                "criteriaCount", next.criteria().size(), "totalPoints", next.totalPoints()));
        return rubricView(actor, saved, part.partId());
    }

    /** Tài liệu tham khảo được chọn (BR-U07-17); chưa có U13 thì rỗng. */
    @Transactional(readOnly = true)
    public List<AssignmentViews.Material> selectableMaterials(Actor actor, UUID id) {
        Assignment assignment = support.manageable(actor, id, "ASSIGNMENT_MATERIALS_VIEW");
        return selectable(actor, assignment).stream()
                .map(m -> new AssignmentViews.Material(m.materialId(), m.title(), m.hasText())).toList();
    }

    /** Trang xem sơ đồ (link trong DOCX); trang tự kiểm quyền đọc bài. */
    @Transactional(readOnly = true)
    public AssignmentViews.Diagram diagram(Actor actor, UUID id, UUID blockId) {
        Assignment assignment = support.readable(actor, id, "ASSIGNMENT_DIAGRAM_VIEW");
        DocumentBlock block = assignment.skeleton().blocks().stream()
                .filter(b -> blockId.equals(b.id()) && b.type() == BlockType.DIAGRAM).findFirst()
                .orElseThrow(() -> ApiException.notFound("DIAGRAM_NOT_FOUND"));
        return new AssignmentViews.Diagram(assignment.id(), blockId, assignment.title(),
                block.content().path("diagramType").asText(), block.content().path("alt").asText(""),
                block.content().path("xml").asText(""), block.content().path("svgPreview").asText(null));
    }

    /** Ảnh trong khung: kiểm quyền đọc bài rồi cấp link tải 5 phút của U03. */
    @Transactional(readOnly = true)
    public String imageUrl(Actor actor, UUID id, UUID blockId) {
        Assignment assignment = support.readable(actor, id, "ASSIGNMENT_IMAGE_VIEW");
        String fileId = DocumentFiles.fileId(assignment.skeleton(), blockId)
                .orElseThrow(() -> ApiException.notFound("IMAGE_NOT_FOUND"));
        return artifacts.issueDownloadToken(fileId, actor.accountId()).url();
    }

    /** Lỗi chặn duyệt, phát hành (BR-U07-24, 32). */
    List<ValidationIssue> readinessIssues(Assignment assignment) {
        List<ValidationIssue> issues = new ArrayList<>();
        if (assignment.title() == null || assignment.title().isBlank()) {
            issues.add(new ValidationIssue("TITLE_REQUIRED", null, "Bài cần tiêu đề"));
        }
        ValidationResult skeleton = documents.validateSkeleton(assignment.skeleton());
        issues.addAll(skeleton.errors());
        skeleton.warnings().stream().filter(w -> w.code().equals("HEADING_EMPTY")).forEach(issues::add);
        if (!assignment.diagramEnabled()) {
            assignment.skeleton().blocks().stream().filter(b -> b.type() == BlockType.DIAGRAM).forEach(b -> issues.add(
                    new ValidationIssue("DIAGRAM_DISABLED", b.id(), "Bài chưa bật sơ đồ")));
        }
        issues.addAll(rubrics.readinessIssues(assignment));
        if (assignment.submissionMode() == SubmissionMode.GROUP && !support.groupSupported(assignment.classId())) {
            issues.add(new ValidationIssue("GROUP_NOT_SUPPORTED", null, "Chưa dùng được bài nhóm cho lớp này"));
        }
        if (assignment.activityMonitoringEnabled()) {
            if (assignment.activityNotice() == null || assignment.activityNotice().isBlank()) {
                issues.add(new ValidationIssue("ACTIVITY_NOTICE_REQUIRED", null,
                        "Cần lời báo cho sinh viên khi bật ghi nhận hoạt động"));
            }
        }
        return issues;
    }

    private AssignmentViews.Detail create(Actor actor, AssignmentScope scope, UUID classId, UUID subjectId,
                                          AssignmentCommands.Create command) {
        Instant now = clock.instant();
        DocumentModel skeleton = new DocumentModel(List.of());
        List<DocumentPart> parts = PartCalculator.compute(skeleton);
        String title = command.title() == null || command.title().isBlank() ? DEFAULT_TITLE : title(command.title());
        Assignment assignment = new Assignment(UUID.randomUUID(), scope, classId, subjectId, null,
                AssignmentOrigin.MANUAL, 1, command.submissionMode(), true, command.gradingMode(),
                title, "", skeleton, parts, Map.of(), List.of(), false, null, AssignmentStatus.DRAFT, null, null, null,
                null, null, actor.accountId(), now, now, 0);
        assignments.insert(assignment);
        rubrics.syncParts(assignment.id(), parts, now);
        audit.success(actor, "ASSIGNMENT_CREATED", assignment.id(), AssignmentAudit.details("scope", scope,
                "classId", classId, "subjectId", subjectId, "submissionMode", command.submissionMode(),
                "gradingMode", command.gradingMode()));
        return support.detail(actor, assignment, List.of());
    }

    private AssignmentViews.Page page(List<Assignment> all, Predicate<Assignment> readOnly,
                                      Set<AssignmentStatus> statuses, boolean includeClosed, String query,
                                      GradingMode gradingMode, boolean groupSupported, boolean archived) {
        if (query != null && query.length() > 200) {
            throw ApiException.badRequest("INVALID_QUERY", "Từ khóa tối đa 200 ký tự");
        }
        Map<AssignmentStatus, Long> counts = new EnumMap<>(AssignmentStatus.class);
        all.forEach(a -> counts.merge(a.status(), 1L, Long::sum));
        String needle = query == null ? "" : query.strip().toLowerCase(Locale.ROOT);
        boolean explicit = statuses != null && !statuses.isEmpty();
        int hidden = 0;
        List<AssignmentViews.Row> rows = new ArrayList<>();
        for (Assignment a : all) {
            if (gradingMode != null && a.gradingMode() != gradingMode) {
                continue;
            }
            if (!needle.isEmpty() && !a.title().toLowerCase(Locale.ROOT).contains(needle)) {
                continue;
            }
            if (explicit && !statuses.contains(a.status())) {
                continue;
            }
            if (!explicit && !includeClosed && HIDDEN_BY_DEFAULT.contains(a.status())) {
                hidden++;
                continue;
            }
            rows.add(support.row(a, readOnly.test(a)));
        }
        return new AssignmentViews.Page(List.copyOf(rows), hidden, counts, groupSupported, archived);
    }

    private AssignmentViews.Rubric rubricView(Actor actor, Assignment assignment, String partId) {
        DocumentPart part = part(assignment, partId);
        AssignmentRubric rubric = rubrics.forPart(assignment, part.partId())
                .orElseThrow(() -> ApiException.notFound("RUBRIC_NOT_FOUND"));
        boolean editable = assignment.editable() && !rubric.locked() && access.canManage(actor, assignment)
                && !access.ownerArchived(assignment);
        return new AssignmentViews.Rubric(rubric.id(), assignment.id(), part.partId(), part.title(), rubric.criteria(),
                rubric.totalPoints(), rubric.locked(), rubric.complete(), assignment.diagramEnabled(), editable,
                assignment.version());
    }

    private static DocumentPart part(Assignment assignment, String partId) {
        return assignment.parts().stream().filter(p -> p.partId().equals(partId)).findFirst()
                .orElseThrow(() -> ApiException.notFound("PART_NOT_FOUND"));
    }

    private List<UUID> materials(Actor actor, Assignment assignment, List<UUID> requested) {
        Set<UUID> unique = new LinkedHashSet<>(requested);
        if (unique.size() > Assignment.MATERIALS_MAX) {
            throw ApiException.badRequest("TOO_MANY_MATERIALS", "Gắn tối đa 10 tài liệu tham khảo");
        }
        if (unique.isEmpty()) {
            return List.of();
        }
        Set<UUID> allowed = selectable(actor, assignment).stream().map(SelectableMaterial::materialId)
                .collect(Collectors.toSet());
        if (!allowed.containsAll(unique)) {
            throw ApiException.badRequest("MATERIAL_NOT_ALLOWED", "Có tài liệu ngoài phạm vi của bài");
        }
        return List.copyOf(unique);
    }

    private List<SelectableMaterial> selectable(Actor actor, Assignment assignment) {
        ResourceRef scope = assignment.scope() == AssignmentScope.CLASS
                ? ResourceRef.courseClass(assignment.classId()) : ResourceRef.subject(assignment.subjectId());
        try {
            List<SelectableMaterial> list = materials.listSelectable(actor, scope);
            return list == null ? List.of() : list;
        } catch (RuntimeException e) {
            return List.of();
        }
    }

    static Map<DiagramType, Integer> requiredDiagrams(Map<DiagramType, Integer> requested) {
        Map<DiagramType, Integer> out = new EnumMap<>(DiagramType.class);
        requested.forEach((type, count) -> {
            if (type == null || count == null || count < 1 || count > Assignment.REQUIRED_DIAGRAM_MAX) {
                throw ApiException.badRequest("REQUIRED_DIAGRAMS_INVALID", "Số sơ đồ bắt buộc mỗi loại từ 1 đến 20");
            }
            out.put(type, count);
        });
        return out;
    }

    static void modes(SubmissionMode submissionMode, GradingMode gradingMode) {
        if (submissionMode == null || gradingMode == null) {
            throw ApiException.badRequest("MODE_REQUIRED", "Chọn hình thức và chế độ của bài");
        }
        if (submissionMode == SubmissionMode.GROUP && gradingMode != GradingMode.GRADED) {
            throw ApiException.badRequest("GROUP_MUST_BE_GRADED", "Bài nhóm chỉ có chế độ Graded");
        }
    }

    static String title(String value) {
        String title = value == null ? "" : value.strip();
        if (title.isEmpty() || title.length() > Assignment.TITLE_MAX) {
            throw ApiException.badRequest("TITLE_INVALID", "Tiêu đề từ 1 đến 200 ký tự");
        }
        return title;
    }

    static String instructions(String value) {
        if (value.length() > Assignment.INSTRUCTIONS_MAX) {
            throw ApiException.badRequest("INSTRUCTIONS_TOO_LONG", "Hướng dẫn tối đa 20.000 ký tự");
        }
        return value;
    }

    static String notice(String value) {
        String notice = value.strip();
        if (notice.length() > Assignment.NOTICE_MAX) {
            throw ApiException.badRequest("NOTICE_TOO_LONG", "Lời báo tối đa 1.000 ký tự");
        }
        return notice.isEmpty() ? null : notice;
    }
}
