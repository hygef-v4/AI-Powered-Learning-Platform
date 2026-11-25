package edu.aiplatform.assignmentwork.service;

import edu.aiplatform.assignmentwork.dto.WorkViews;

import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.aiplatform.academics.contract.ClassRef;
import edu.aiplatform.assignments.contract.AssignmentContent;
import edu.aiplatform.assignments.contract.AssignmentScope;
import edu.aiplatform.assignments.contract.AssignmentView;
import edu.aiplatform.assignments.contract.BlockOrigin;
import edu.aiplatform.assignments.contract.BlockType;
import edu.aiplatform.assignments.contract.DocumentBlock;
import edu.aiplatform.assignments.contract.DocumentFiles;
import edu.aiplatform.assignments.contract.DocumentModel;
import edu.aiplatform.assignments.contract.DocxImportPreview;
import edu.aiplatform.assignments.contract.GradingMode;
import edu.aiplatform.assignments.contract.IAssignmentQuery;
import edu.aiplatform.assignments.contract.IDiagramLinker;
import edu.aiplatform.assignments.contract.IDocumentModel;
import edu.aiplatform.assignments.contract.IDocx;
import edu.aiplatform.assignments.contract.RubricView;
import edu.aiplatform.assignments.contract.SubmissionMode;
import edu.aiplatform.assignments.contract.SubmissionWindow;
import edu.aiplatform.assignments.contract.ValidationIssue;
import edu.aiplatform.assignments.contract.ValidationResult;
import edu.aiplatform.assignmentwork.contract.AttemptStatus;
import edu.aiplatform.assignmentwork.contract.SubmitMode;
import edu.aiplatform.assignmentwork.model.AssignmentAttempt;
import edu.aiplatform.assignmentwork.repository.AttemptRepository;
import edu.aiplatform.assignmentwork.model.AttemptSnapshot;
import edu.aiplatform.files.contract.ArtifactPurpose;
import edu.aiplatform.files.contract.FileRef;
import edu.aiplatform.files.contract.IArtifact;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.shared.error.ApiException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Bắt đầu lượt, làm và tự lưu, nhập/xuất DOCX, nộp (F3–F6; BR-U08-10…16, 20…22). */
@Service
public class AttemptService {

    private final WorkAccess access;
    private final AttemptRepository attempts;
    private final AttemptSubmitter submitter;
    private final IAssignmentQuery assignments;
    private final IDocumentModel documents;
    private final IDocx docx;
    private final IArtifact artifacts;
    private final Clock clock;
    private final String publicUrl;

    public AttemptService(WorkAccess access, AttemptRepository attempts, AttemptSubmitter submitter,
                          IAssignmentQuery assignments, IDocumentModel documents, IDocx docx, IArtifact artifacts, Clock clock,
                          @Value("${platform.assignments.public-url:http://localhost:8088}") String publicUrl) {
        this.access = access;
        this.attempts = attempts;
        this.submitter = submitter;
        this.assignments = assignments;
        this.documents = documents;
        this.docx = docx;
        this.artifacts = artifacts;
        this.clock = clock;
        this.publicUrl = publicUrl.endsWith("/") ? publicUrl.substring(0, publicUrl.length() - 1) : publicUrl;
    }

    /** F3: trả lượt đang làm nếu có; không thì kiểm điều kiện và tạo lượt mới từ khung (BR-U08-10…12). */
    @Transactional
    public Started start(Actor actor, UUID assignmentId, UUID classId) {
        ClassRef ref = access.requireStudentOfClass(actor, classId, "ATTEMPT_STARTED");
        WorkAccess.call(() -> {
            assignments.lockSubmission(assignmentId);
            return null;
        });
        AssignmentView view = access.requireVisible(actor, ref, assignmentId, "ATTEMPT_STARTED");
        attempts.lockStart(assignmentId, actor.accountId());
        Optional<AssignmentAttempt> existing = attempts.findInProgress(assignmentId, actor.accountId());
        if (existing.isPresent()) {
            AssignmentAttempt draft = existing.get();
            if (!draft.classId().equals(ref.classId())) {
                draft = resumeDraftInClass(actor, view, ref.classId(), draft.id());
            }
            return new Started(attemptView(draft, view), false);
        }
        if (view.submissionMode() == SubmissionMode.GROUP) {
            throw ApiException.createConflict("GROUP_ASSIGNMENT", "Bài nhóm làm trong tài liệu nhóm");
        }
        Instant now = clock.instant();
        SubmissionWindow window = WorkAccess.call(() -> assignments.submissionWindow(assignmentId, now));
        if (window == SubmissionWindow.CLOSED) {
            throw ApiException.createConflict("SUBMISSION_CLOSED", "Bài đã hết hạn nhận bài");
        }
        int used = attempts.findMaxAttemptNo(assignmentId, actor.accountId());
        // Bài có chấm nộp bao nhiêu lần cũng được khi còn hạn; chỉ bài Luyện tập giới hạn lượt (yêu cầu 2026-11-10).
        int max = view.maxAttempts() == null ? 1 : view.maxAttempts();
        if (view.gradingMode() == GradingMode.PRACTICE && used >= max) {
            throw ApiException.createConflict("ATTEMPTS_EXHAUSTED", "Bạn đã dùng hết số lượt nộp");
        }
        AssignmentContent content = WorkAccess.call(() -> assignments.getContent(assignmentId))
                .orElseThrow(() -> ApiException.createNotFound("NOT_FOUND"));
        Instant deadline = view.lateUntil() != null ? view.lateUntil() : view.closesAt();
        // Bài có chấm nộp lại: bắt đầu từ bài nộp gần nhất để sửa tiếp, không phải làm lại từ đề trống (yêu cầu 2026-11-10);
        // bài nộp cũ không còn khớp khung đề thì bắt đầu từ đề. Bài Luyện tập mỗi lượt làm lại từ đề.
        DocumentModel startFrom = view.gradingMode() == GradingMode.PRACTICE ? content.skeleton()
                : attempts.listMine(assignmentId, actor.accountId()).stream()
                        .filter(a -> a.status() == AttemptStatus.SUBMITTED)
                        .max(java.util.Comparator.comparingInt(AssignmentAttempt::attemptNo))
                        // Danh sách lượt không mang nội dung bài: tải lại đủ bài nộp gần nhất.
                        .flatMap(last -> attempts.findAttempt(last.id())).map(AssignmentAttempt::content)
                        .filter(previous -> documents.validateForSave(content.skeleton(), previous).errors().isEmpty())
                        .orElse(content.skeleton());
        AssignmentAttempt attempt = new AssignmentAttempt(UUID.randomUUID(), assignmentId, ref.classId(),
                actor.accountId(), used + 1, AttemptSnapshot.createSnapshot(view, content.requiredDiagrams()), startFrom,
                0, AttemptStatus.IN_PROGRESS, null, false, now, deadline, null, null, null, List.of());
        attempts.insert(attempt);
        return new Started(attemptView(attempt, view), true);
    }

    private AssignmentAttempt resumeDraftInClass(Actor actor, AssignmentView view, UUID classId, UUID attemptId) {
        AssignmentAttempt draft = attempts.findForUpdate(attemptId)
                .orElseThrow(() -> ApiException.createNotFound("ATTEMPT_NOT_FOUND"));
        if (!draft.hasInProgressStatus()) {
            throw ApiException.createConflict("ATTEMPT_SUBMITTED", "Lượt đã nộp, không sửa được");
        }
        if (view.scope() != AssignmentScope.SUBJECT || view.submissionMode() != SubmissionMode.INDIVIDUAL) {
            throw access.notFound();
        }
        if (access.isActiveStudent(actor.accountId(), draft.classId())) {
            access.requireStudentOfClass(actor, draft.classId(), "ATTEMPT_STARTED");
            return draft;
        }
        access.requireUniqueDraftDestination(actor, classId, view.subjectId());
        if (WorkAccess.call(() -> assignments.submissionWindow(view.assignmentId(), clock.instant())) == SubmissionWindow.CLOSED) {
            throw ApiException.createConflict("SUBMISSION_CLOSED", "Bài đã hết hạn nhận bài");
        }
        return attempts.transferDraft(attemptId, draft.classId(), classId)
                .orElseThrow(AttemptService::createVersionConflictError);
    }

    @Transactional(readOnly = true)
    public WorkViews.Attempt loadAttempt(Actor actor, UUID attemptId) {
        AssignmentAttempt attempt = access.requireOwnAttempt(actor, attemptId, "ATTEMPT_VIEW");
        AssignmentView view = WorkAccess.call(() -> assignments.loadAssignment(attempt.assignmentId()))
                .orElseThrow(() -> ApiException.createNotFound("NOT_FOUND"));
        return attemptView(attempt, view);
    }

    /** F4 (P1): chủ lượt, còn làm, chưa quá hạn cộng ân hạn, {@code contentVersion} khớp, không đụng khung. */
    @Transactional
    public WorkViews.Saved saveAttempt(Actor actor, UUID attemptId, long contentVersion, DocumentModel content) {
        AssignmentAttempt attempt = loadWritable(actor, attemptId, "ATTEMPT_SAVED");
        if (attempt.contentVersion() != contentVersion) {
            throw createVersionConflictError();
        }
        AssignmentContent assignment = WorkAccess.call(() -> assignments.getContent(attempt.assignmentId()))
                .orElseThrow(() -> ApiException.createNotFound("NOT_FOUND"));
        DocumentModel stored = prepare(actor, attempt, assignment.skeleton(), content);
        Instant now = clock.instant();
        if (attempts.saveContent(attempt.id(), stored, contentVersion, now) != 1) {
            throw createVersionConflictError();
        }
        return new WorkViews.Saved(attempt.id(), contentVersion + 1, now, DocumentFiles.hideFiles(stored),
                submitIssues(attempt, assignment.skeleton(), stored));
    }

    /** F6: nộp tay (BR-U08-20…22); nộp lặp trả cùng biên nhận. */
    @Transactional
    public WorkViews.Receipt submit(Actor actor, UUID attemptId, long contentVersion) {
        AssignmentAttempt current = access.requireOwnAttemptForUpdate(actor, attemptId, "ASSIGNMENT_SUBMITTED");
        if (current.status() == AttemptStatus.SUBMITTED) {
            return buildReceipt(current);
        }
        Instant now = clock.instant();
        AssignmentView view = WorkAccess.call(() -> assignments.loadAssignment(current.assignmentId()))
                .orElseThrow(() -> ApiException.createNotFound("NOT_FOUND"));
        if (!WorkAccess.openForStudents(view)) {
            throw ApiException.createConflict("SUBMISSION_CLOSED", "Bài đã đóng nhận bài");
        }
        if (!now.isBefore(calculateEffectiveDeadline(current, view).plus(AssignmentAttempt.GRACE))) {
            throw ApiException.createConflict("DEADLINE_PASSED", "Đã quá hạn nộp bài");
        }
        if (current.contentVersion() != contentVersion) {
            throw createVersionConflictError();
        }
        submitter.submit(current, SubmitMode.MANUAL, actor.accountId(), now, view.closesAt(), List.of());
        return buildReceipt(attempts.findAttempt(current.id()).orElseThrow());
    }

    @Transactional(readOnly = true)
    public WorkViews.Receipt loadReceipt(Actor actor, UUID attemptId) {
        AssignmentAttempt attempt = access.requireOwnAttempt(actor, attemptId, "ATTEMPT_RECEIPT_VIEW");
        if (attempt.status() != AttemptStatus.SUBMITTED) {
            throw ApiException.createConflict("ATTEMPT_NOT_SUBMITTED", "Lượt chưa nộp");
        }
        return buildReceipt(attempt);
    }

    /**
     * F5 nhập: block của người học, xem trước; mã sơ đồ chỉ nhận đề của bài hoặc lượt của chính mình. Không đánh dấu
     * chỉ đọc: {@code writable} khóa dòng ({@code FOR UPDATE}) và có thể nhận hạn mới của lượt.
     */
    @Transactional
    public WorkViews.ImportPreview importPreview(Actor actor, UUID attemptId, byte[] file) {
        AssignmentAttempt attempt = loadWritable(actor, attemptId, "ATTEMPT_DOCX_IMPORTED");
        DocxImportPreview preview = docx.importPreview(actor, file, BlockOrigin.STUDENT,
                attempt.snapshot().diagramEnabled(), ref -> resolve(actor, attempt, ref));
        // Giữ tiêu đề (yêu cầu 2026-11-25): trang làm bài so các tiêu đề với mục của đề để ghép bài vào đúng phần; tiêu đề
        // không khớp mục nào thì trang đổi thành đoạn in đậm.
        List<DocumentBlock> blocks = preview.document().blocks();
        int images = (int) blocks.stream().filter(b -> b.type() == BlockType.IMAGE).count();
        int diagrams = (int) blocks.stream().filter(b -> b.type() == BlockType.DIAGRAM).count();
        return new WorkViews.ImportPreview(new DocumentModel(List.copyOf(blocks)), preview.warnings(), images, diagrams);
    }

    /** F5 xuất: chủ lượt hoặc giảng viên chính của lớp; sơ đồ kèm mã {@code attempt/attemptId/blockId}. */
    @Transactional(readOnly = true)
    public Export export(Actor actor, UUID attemptId) {
        AssignmentAttempt attempt = access.requireReadableAttempt(actor, attemptId, "ATTEMPT_DOCX_EXPORTED");
        // Bài có chấm không còn khái niệm lượt (yêu cầu 2026-11-10): tên tệp chỉ có tên bài; bài Luyện tập vẫn ghi lượt.
        String title = attempt.snapshot().gradingMode() == GradingMode.PRACTICE
                ? attempt.snapshot().title() + " – Lượt " + attempt.attemptNo() : attempt.snapshot().title();
        byte[] bytes = docx.export(attempt.content(), title, createLinker(attempt.id()));
        return new Export(buildFileName(title), bytes);
    }

    @Transactional(readOnly = true)
    public String resolveImageUrl(Actor actor, UUID attemptId, UUID blockId) {
        AssignmentAttempt attempt = access.requireReadableAttempt(actor, attemptId, "ATTEMPT_IMAGE_VIEW");
        String fileId = DocumentFiles.fileId(attempt.content(), blockId)
                .orElseThrow(() -> ApiException.createNotFound("IMAGE_NOT_FOUND"));
        return artifacts.issueDownloadToken(fileId, actor.accountId()).url();
    }

    @Transactional(readOnly = true)
    public WorkViews.Diagram loadDiagram(Actor actor, UUID attemptId, UUID blockId) {
        AssignmentAttempt attempt = access.requireReadableAttempt(actor, attemptId, "ATTEMPT_DIAGRAM_VIEW");
        DocumentBlock block = attempt.content().blocks().stream()
                .filter(b -> blockId.equals(b.id()) && b.type() == BlockType.DIAGRAM).findFirst()
                .orElseThrow(() -> ApiException.createNotFound("DIAGRAM_NOT_FOUND"));
        return new WorkViews.Diagram(attempt.id(), blockId, attempt.snapshot().title(),
                block.content().path("diagramType").asText(), block.content().path("alt").asText(""),
                block.content().path("xml").asText(""), block.content().path("svgPreview").asText(null));
    }

    /**
     * Làm sạch, gắn ảnh mới, kiểm không đụng khung (BR-U08-14); block người soạn lấy đúng bản của đề, trừ nét vẽ của
     * người học trên sơ đồ của người soạn nằm trong phần (yêu cầu 2026-10-21).
     */
    private DocumentModel prepare(Actor actor, AssignmentAttempt attempt, DocumentModel skeleton, DocumentModel content) {
        if (content == null || content.blocks() == null) {
            throw ApiException.createBadRequest("DOCUMENT_INVALID", "Bài làm không hợp lệ");
        }
        DocumentModel pending = DocumentFiles.resolveFiles(attempt.content(), documents.sanitize(content), null);
        ValidationResult result = documents.validateForSave(skeleton, pending);
        List<ValidationIssue> errors = new ArrayList<>(result.errors());
        errors.addAll(listDisabledDiagrams(attempt, pending));
        if (!errors.isEmpty()) {
            throw WorkProblem.createIssuesProblem(HttpStatus.BAD_REQUEST, "DOCUMENT_INVALID", "Bài làm có lỗi", errors);
        }
        DocumentModel attached = DocumentFiles.resolveFiles(attempt.content(), pending,
                ref -> artifacts.attach(new FileRef(ref), actor, ArtifactPurpose.DOCUMENT_IMAGE).fileId());
        Map<UUID, DocumentBlock> teacher = new HashMap<>();
        skeleton.blocks().forEach(b -> teacher.put(b.id(), b));
        List<DocumentBlock> canonical = new ArrayList<>(attached.blocks().size());
        for (DocumentBlock block : attached.blocks()) {
            DocumentBlock original = block.origin() == BlockOrigin.TEACHER ? teacher.get(block.id()) : null;
            canonical.add(original == null ? block : switch (original.type()) {
                case DIAGRAM -> mergeDrawnBlock(original, block);
                case TABLE -> mergeFilledBlock(original, block);
                default -> original;
            });
        }
        return new DocumentModel(List.copyOf(canonical));
    }

    /**
     * Sơ đồ của người soạn với XML, loại (suy từ hình vẽ) và ảnh xem trước người học vừa vẽ; mô tả giữ theo đề. XML chỉ
     * khác đề khi sơ đồ nằm trong phần (validateForSave đã kiểm), không thì giữ nguyên bản của đề.
     */
    private static DocumentBlock mergeDrawnBlock(DocumentBlock original, DocumentBlock block) {
        if (original.content().path("xml").equals(block.content().path("xml"))) {
            return original;
        }
        ObjectNode content = original.content().deepCopy();
        content.set("xml", block.content().path("xml").deepCopy());
        content.set("diagramType", block.content().path("diagramType").deepCopy());
        if (block.content().hasNonNull("svgPreview")) {
            content.set("svgPreview", block.content().get("svgPreview").deepCopy());
        } else {
            content.remove("svgPreview");
        }
        return new DocumentBlock(original.id(), original.origin(), original.type(), content);
    }

    /** Bảng của người soạn với các ô người học điền (chỉ khác đề khi bảng nằm trong phần, validateForSave đã kiểm). */
    private static DocumentBlock mergeFilledBlock(DocumentBlock original, DocumentBlock block) {
        if (original.content().path("rows").equals(block.content().path("rows"))) {
            return original;
        }
        ObjectNode content = original.content().deepCopy();
        content.set("rows", block.content().path("rows").deepCopy());
        return new DocumentBlock(original.id(), original.origin(), original.type(), content);
    }

    private static List<ValidationIssue> listDisabledDiagrams(AssignmentAttempt attempt, DocumentModel content) {
        if (attempt.snapshot().diagramEnabled()) {
            return List.of();
        }
        return content.blocks().stream().filter(b -> b.origin() == BlockOrigin.STUDENT && b.type() == BlockType.DIAGRAM)
                .map(b -> new ValidationIssue("DIAGRAM_DISABLED", b.id(), "Bài không dùng sơ đồ")).toList();
    }

    private AssignmentAttempt loadWritable(Actor actor, UUID attemptId, String action) {
        AssignmentAttempt attempt = access.requireOwnAttemptForUpdate(actor, attemptId, action);
        if (!attempt.hasInProgressStatus()) {
            throw ApiException.createConflict("ATTEMPT_SUBMITTED", "Lượt đã nộp, không sửa được");
        }
        AssignmentView view = WorkAccess.call(() -> assignments.loadAssignment(attempt.assignmentId()))
                .orElseThrow(() -> ApiException.createNotFound("NOT_FOUND"));
        Instant deadline = calculateEffectiveDeadline(attempt, view);
        if (!WorkAccess.openForStudents(view)) {
            throw ApiException.createConflict("SUBMISSION_CLOSED", "Bài đã đóng nhận bài");
        }
        if (deadline.isAfter(attempt.deadlineAt())) {
            attempts.extendDeadline(attempt.id(), deadline);
        }
        if (clock.instant().isAfter(deadline.plus(AssignmentAttempt.GRACE))) {
            throw ApiException.createConflict("DEADLINE_PASSED", "Đã quá hạn, không lưu được nữa");
        }
        return attempt;
    }

    /** Hạn của lượt; bài được kéo dài hạn sau khi bắt đầu thì lượt nhận hạn mới (BR-U07-35). */
    static Instant calculateEffectiveDeadline(AssignmentAttempt attempt, AssignmentView view) {
        Instant current = view.lateUntil() != null ? view.lateUntil() : view.closesAt();
        return current != null && current.isAfter(attempt.deadlineAt()) ? current : attempt.deadlineAt();
    }

    private Optional<String> resolve(Actor actor, AssignmentAttempt attempt, String ref) {
        String[] parts = ref.split("/");
        try {
            if (parts.length == 2 && UUID.fromString(parts[0]).equals(attempt.assignmentId())) {
                UUID blockId = UUID.fromString(parts[1]);
                return assignments.getContent(attempt.assignmentId()).flatMap(c -> findDiagramXml(c.skeleton(), blockId));
            }
            if (parts.length == 3 && parts[0].equals("attempt")) {
                UUID otherId = UUID.fromString(parts[1]);
                UUID blockId = UUID.fromString(parts[2]);
                return attempts.findAttempt(otherId).filter(a -> a.accountId().equals(actor.accountId()))
                        .flatMap(a -> findDiagramXml(a.content(), blockId));
            }
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
        return Optional.empty();
    }

    private static Optional<String> findDiagramXml(DocumentModel document, UUID blockId) {
        return document.blocks().stream().filter(b -> blockId.equals(b.id()) && b.type() == BlockType.DIAGRAM)
                .findFirst().map(b -> b.content().path("xml").asText(null));
    }

    private IDiagramLinker createLinker(UUID attemptId) {
        return new IDiagramLinker() {
            @Override
            public String linkFor(UUID blockId) {
                return "attempt/" + attemptId + "/" + blockId;
            }

            @Override
            public String viewUrlFor(UUID blockId) {
                return publicUrl + "/attempts/" + attemptId + "/diagrams/" + blockId;
            }
        };
    }

    private WorkViews.Attempt attemptView(AssignmentAttempt a, AssignmentView view) {
        AssignmentContent content = WorkAccess.call(() -> assignments.getContent(a.assignmentId()))
                .orElseThrow(() -> ApiException.createNotFound("NOT_FOUND"));
        BigDecimal maxPoints = content.rubrics().stream().map(RubricView::maxPoints)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Instant now = clock.instant();
        Instant deadline = calculateEffectiveDeadline(a, view);
        boolean editable = WorkAccess.openForStudents(view) && a.hasInProgressStatus()
                && !now.isAfter(deadline.plus(AssignmentAttempt.GRACE));
        return new WorkViews.Attempt(a.id(), a.assignmentId(), a.classId(), a.snapshot().title(), a.attemptNo(),
                a.snapshot().maxAttempts(), a.status(), DocumentFiles.hideFiles(a.content()), a.contentVersion(),
                a.startedAt(), deadline, a.lastSavedAt(), a.submittedAt(), a.late(), a.submitMode(), a.receiptHash(),
                a.submitWarnings(), content.instructions(), a.snapshot().gradingMode(), view.submissionMode(),
                a.snapshot().diagramEnabled(), a.snapshot().requiredDiagrams(), content.parts(),
                maxPoints, a.snapshot().activityMonitoringEnabled(),
                a.snapshot().activityMonitoringEnabled() ? a.snapshot().activityNotice() : null, view.closesAt(),
                view.lateUntil(), editable, now, submitIssues(a, content.skeleton(), a.content()));
    }

    /**
     * Kiểm nộp (F6): cùng luật với lúc nộp, trả về cùng lượt để trang báo đúng trước khi bấm Nộp. Máy khách không có bản
     * đề gốc nên không tự biết sơ đồ, bảng của đề đã được vẽ, điền khác đề hay chưa. Lượt đã nộp thì rỗng.
     */
    private List<ValidationIssue> submitIssues(AssignmentAttempt attempt, DocumentModel skeleton, DocumentModel content) {
        if (!attempt.hasInProgressStatus()) {
            return List.of();
        }
        ValidationResult result = documents.validateForSubmit(skeleton, content, attempt.snapshot().requiredDiagrams());
        List<ValidationIssue> errors = new ArrayList<>(result.errors());
        errors.addAll(listDisabledDiagrams(attempt, content));
        return List.copyOf(errors);
    }

    private static WorkViews.Receipt buildReceipt(AssignmentAttempt a) {
        int diagrams = (int) a.content().blocks().stream()
                .filter(b -> b.origin() == BlockOrigin.STUDENT && b.type() == BlockType.DIAGRAM).count();
        int blocks = (int) a.content().blocks().stream().filter(b -> b.origin() == BlockOrigin.STUDENT).count();
        return new WorkViews.Receipt(a.id(), a.assignmentId(), a.classId(), a.snapshot().title(), a.attemptNo(),
                a.snapshot().maxAttempts(), a.submittedAt(), a.late(), a.submitMode(), a.receiptHash(), blocks, diagrams);
    }

    /** Người học không thêm tiêu đề mục (BR-U07-15): tiêu đề trong DOCX thành đoạn chữ đậm. */
    private static ApiException createVersionConflictError() {
        return ApiException.createConflict("CONTENT_VERSION_CONFLICT", "Bài đang mở ở nơi khác. Hãy tải lại");
    }

    static String buildFileName(String title) {
        String base = title.replaceAll("[\\\\/:*?\"<>|\\r\\n]+", " ").strip();
        return (base.isEmpty() ? "bai-lam" : base.length() > 120 ? base.substring(0, 120) : base) + ".docx";
    }

    public record Started(WorkViews.Attempt attempt, boolean created) {
    }

    public record Export(String fileName, byte[] bytes) {
    }
}
