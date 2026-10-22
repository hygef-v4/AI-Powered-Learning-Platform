package edu.aiplatform.assignmentwork.application;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.aiplatform.academics.contract.ClassRef;
import edu.aiplatform.assignments.contract.AssignmentContent;
import edu.aiplatform.assignments.contract.AssignmentView;
import edu.aiplatform.assignments.contract.BlockOrigin;
import edu.aiplatform.assignments.contract.BlockType;
import edu.aiplatform.assignments.contract.DocumentBlock;
import edu.aiplatform.assignments.contract.DocumentFiles;
import edu.aiplatform.assignments.contract.DocumentModel;
import edu.aiplatform.assignments.contract.DocxImportPreview;
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
import edu.aiplatform.assignmentwork.domain.AssignmentAttempt;
import edu.aiplatform.assignmentwork.domain.AttemptRepository;
import edu.aiplatform.assignmentwork.domain.AttemptSnapshot;
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
    private final WorkAudit audit;
    private final Clock clock;
    private final String publicUrl;

    public AttemptService(WorkAccess access, AttemptRepository attempts, AttemptSubmitter submitter,
                          IAssignmentQuery assignments, IDocumentModel documents, IDocx docx, IArtifact artifacts,
                          WorkAudit audit, Clock clock,
                          @Value("${platform.assignments.public-url:http://localhost:8088}") String publicUrl) {
        this.access = access;
        this.attempts = attempts;
        this.submitter = submitter;
        this.assignments = assignments;
        this.documents = documents;
        this.docx = docx;
        this.artifacts = artifacts;
        this.audit = audit;
        this.clock = clock;
        this.publicUrl = publicUrl.endsWith("/") ? publicUrl.substring(0, publicUrl.length() - 1) : publicUrl;
    }

    /** F3: trả lượt đang làm nếu có; không thì kiểm điều kiện và tạo lượt mới từ khung (BR-U08-10…12). */
    @Transactional
    public Started start(Actor actor, UUID assignmentId, UUID classId) {
        ClassRef ref = access.requireStudentOfClass(actor, classId, "ATTEMPT_STARTED");
        AssignmentView view = access.requireVisible(actor, ref, assignmentId, "ATTEMPT_STARTED");
        attempts.lockStart(assignmentId, actor.accountId());
        Optional<AssignmentAttempt> existing = attempts.inProgress(assignmentId, actor.accountId());
        if (existing.isPresent()) {
            return new Started(attemptView(existing.get(), view), false);
        }
        if (view.submissionMode() == SubmissionMode.GROUP) {
            throw ApiException.conflict("GROUP_ASSIGNMENT", "Bài nhóm làm trong tài liệu nhóm");
        }
        Instant now = clock.instant();
        SubmissionWindow window = WorkAccess.call(() -> assignments.submissionWindow(assignmentId, now));
        if (window == SubmissionWindow.CLOSED) {
            throw reject(actor, assignmentId, "SUBMISSION_CLOSED", "Bài đã hết hạn nhận bài");
        }
        int used = attempts.maxAttemptNo(assignmentId, actor.accountId());
        int max = view.maxAttempts() == null ? 1 : view.maxAttempts();
        if (used >= max) {
            throw reject(actor, assignmentId, "ATTEMPTS_EXHAUSTED", "Bạn đã dùng hết số lượt nộp");
        }
        AssignmentContent content = WorkAccess.call(() -> assignments.getContent(assignmentId))
                .orElseThrow(() -> ApiException.notFound("NOT_FOUND"));
        Instant deadline = view.lateUntil() != null ? view.lateUntil() : view.closesAt();
        AssignmentAttempt attempt = new AssignmentAttempt(UUID.randomUUID(), assignmentId, ref.classId(),
                actor.accountId(), used + 1, AttemptSnapshot.of(view, content.requiredDiagrams()), content.skeleton(),
                0, AttemptStatus.IN_PROGRESS, null, false, now, deadline, null, null, null, List.of());
        attempts.insert(attempt);
        return new Started(attemptView(attempt, view), true);
    }

    @Transactional(readOnly = true)
    public WorkViews.Attempt get(Actor actor, UUID attemptId) {
        AssignmentAttempt attempt = access.requireOwnAttempt(actor, attemptId, "ATTEMPT_VIEW");
        AssignmentView view = WorkAccess.call(() -> assignments.get(attempt.assignmentId()))
                .orElseThrow(() -> ApiException.notFound("NOT_FOUND"));
        return attemptView(attempt, view);
    }

    /** F4 (P1): chủ lượt, còn làm, chưa quá hạn cộng ân hạn, {@code contentVersion} khớp, không đụng khung. */
    @Transactional
    public WorkViews.Saved save(Actor actor, UUID attemptId, long contentVersion, DocumentModel content) {
        AssignmentAttempt attempt = writable(actor, attemptId, "ATTEMPT_SAVED");
        if (attempt.contentVersion() != contentVersion) {
            throw versionConflict();
        }
        AssignmentContent assignment = WorkAccess.call(() -> assignments.getContent(attempt.assignmentId()))
                .orElseThrow(() -> ApiException.notFound("NOT_FOUND"));
        DocumentModel stored = prepare(actor, attempt, assignment.skeleton(), content);
        Instant now = clock.instant();
        if (attempts.saveContent(attempt.id(), stored, contentVersion, now) != 1) {
            throw versionConflict();
        }
        return new WorkViews.Saved(attempt.id(), contentVersion + 1, now, DocumentFiles.hideFiles(stored));
    }

    /** F6: nộp tay (BR-U08-20…22); nộp lặp trả cùng biên nhận. */
    @Transactional
    public WorkViews.Receipt submit(Actor actor, UUID attemptId, long contentVersion) {
        AssignmentAttempt current = access.requireOwnAttemptForUpdate(actor, attemptId, "ASSIGNMENT_SUBMITTED");
        if (current.status() == AttemptStatus.SUBMITTED) {
            return receipt(current);
        }
        Instant now = clock.instant();
        AssignmentView view = WorkAccess.call(() -> assignments.get(current.assignmentId()))
                .orElseThrow(() -> ApiException.notFound("NOT_FOUND"));
        if (!now.isBefore(effectiveDeadline(current, view).plus(AssignmentAttempt.GRACE))) {
            throw reject(actor, current.assignmentId(), "DEADLINE_PASSED", "Đã quá hạn nộp bài");
        }
        if (current.contentVersion() != contentVersion) {
            throw versionConflict();
        }
        AssignmentContent content = WorkAccess.call(() -> assignments.getContent(current.assignmentId()))
                .orElseThrow(() -> ApiException.notFound("NOT_FOUND"));
        ValidationResult result = documents.validateForSubmit(content.skeleton(), current.content(),
                current.snapshot().requiredDiagrams());
        List<ValidationIssue> errors = new ArrayList<>(result.errors());
        errors.addAll(disabledDiagrams(current, current.content()));
        if (!errors.isEmpty()) {
            throw WorkProblem.issues(HttpStatus.CONFLICT, "SUBMISSION_INVALID", "Bài chưa đủ điều kiện nộp", errors);
        }
        submitter.submit(current, SubmitMode.MANUAL, actor.accountId(), now, view.closesAt(), List.of());
        return receipt(attempts.find(current.id()).orElseThrow());
    }

    @Transactional(readOnly = true)
    public WorkViews.Receipt receipt(Actor actor, UUID attemptId) {
        AssignmentAttempt attempt = access.requireOwnAttempt(actor, attemptId, "ATTEMPT_RECEIPT_VIEW");
        if (attempt.status() != AttemptStatus.SUBMITTED) {
            throw ApiException.conflict("ATTEMPT_NOT_SUBMITTED", "Lượt chưa nộp");
        }
        return receipt(attempt);
    }

    /**
     * F5 nhập: block của người học, xem trước; mã sơ đồ chỉ nhận đề của bài hoặc lượt của chính mình. Không đánh dấu
     * chỉ đọc: {@code writable} khóa dòng ({@code FOR UPDATE}) và có thể nhận hạn mới của lượt.
     */
    @Transactional
    public WorkViews.ImportPreview importPreview(Actor actor, UUID attemptId, byte[] file) {
        AssignmentAttempt attempt = writable(actor, attemptId, "ATTEMPT_DOCX_IMPORTED");
        DocxImportPreview preview = docx.importPreview(actor, file, BlockOrigin.STUDENT,
                attempt.snapshot().diagramEnabled(), ref -> resolve(actor, attempt, ref));
        List<DocumentBlock> blocks = new ArrayList<>();
        for (DocumentBlock block : preview.document().blocks()) {
            blocks.add(block.type() == BlockType.HEADING ? headingAsParagraph(block) : block);
        }
        int images = (int) blocks.stream().filter(b -> b.type() == BlockType.IMAGE).count();
        int diagrams = (int) blocks.stream().filter(b -> b.type() == BlockType.DIAGRAM).count();
        return new WorkViews.ImportPreview(new DocumentModel(List.copyOf(blocks)), preview.warnings(), images, diagrams);
    }

    /** F5 xuất: chủ lượt hoặc giảng viên chính của lớp; sơ đồ kèm mã {@code attempt/attemptId/blockId}. */
    @Transactional(readOnly = true)
    public Export export(Actor actor, UUID attemptId) {
        AssignmentAttempt attempt = access.requireReadableAttempt(actor, attemptId, "ATTEMPT_DOCX_EXPORTED");
        String title = attempt.snapshot().title() + " – Lượt " + attempt.attemptNo();
        byte[] bytes = docx.export(attempt.content(), title, linker(attempt.id()));
        return new Export(fileName(title), bytes);
    }

    @Transactional(readOnly = true)
    public String imageUrl(Actor actor, UUID attemptId, UUID blockId) {
        AssignmentAttempt attempt = access.requireReadableAttempt(actor, attemptId, "ATTEMPT_IMAGE_VIEW");
        String fileId = DocumentFiles.fileId(attempt.content(), blockId)
                .orElseThrow(() -> ApiException.notFound("IMAGE_NOT_FOUND"));
        return artifacts.issueDownloadToken(fileId, actor.accountId()).url();
    }

    @Transactional(readOnly = true)
    public WorkViews.Diagram diagram(Actor actor, UUID attemptId, UUID blockId) {
        AssignmentAttempt attempt = access.requireReadableAttempt(actor, attemptId, "ATTEMPT_DIAGRAM_VIEW");
        DocumentBlock block = attempt.content().blocks().stream()
                .filter(b -> blockId.equals(b.id()) && b.type() == BlockType.DIAGRAM).findFirst()
                .orElseThrow(() -> ApiException.notFound("DIAGRAM_NOT_FOUND"));
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
            throw ApiException.badRequest("DOCUMENT_INVALID", "Bài làm không hợp lệ");
        }
        DocumentModel pending = DocumentFiles.resolveFiles(attempt.content(), documents.sanitize(content), null);
        ValidationResult result = documents.validateForSave(skeleton, pending);
        List<ValidationIssue> errors = new ArrayList<>(result.errors());
        errors.addAll(disabledDiagrams(attempt, pending));
        if (!errors.isEmpty()) {
            throw WorkProblem.issues(HttpStatus.BAD_REQUEST, "DOCUMENT_INVALID", "Bài làm có lỗi", errors);
        }
        DocumentModel attached = DocumentFiles.resolveFiles(attempt.content(), pending,
                ref -> artifacts.attach(new FileRef(ref), actor, ArtifactPurpose.DOCUMENT_IMAGE).fileId());
        Map<UUID, DocumentBlock> teacher = new HashMap<>();
        skeleton.blocks().forEach(b -> teacher.put(b.id(), b));
        List<DocumentBlock> canonical = new ArrayList<>(attached.blocks().size());
        for (DocumentBlock block : attached.blocks()) {
            DocumentBlock original = block.origin() == BlockOrigin.TEACHER ? teacher.get(block.id()) : null;
            canonical.add(original == null ? block : switch (original.type()) {
                case DIAGRAM -> drawn(original, block);
                case TABLE -> filled(original, block);
                default -> original;
            });
        }
        return new DocumentModel(List.copyOf(canonical));
    }

    /**
     * Sơ đồ của người soạn với XML, loại (suy từ hình vẽ) và ảnh xem trước người học vừa vẽ; mô tả giữ theo đề. XML chỉ
     * khác đề khi sơ đồ nằm trong phần (validateForSave đã kiểm), không thì giữ nguyên bản của đề.
     */
    private static DocumentBlock drawn(DocumentBlock original, DocumentBlock block) {
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
    private static DocumentBlock filled(DocumentBlock original, DocumentBlock block) {
        if (original.content().path("rows").equals(block.content().path("rows"))) {
            return original;
        }
        ObjectNode content = original.content().deepCopy();
        content.set("rows", block.content().path("rows").deepCopy());
        return new DocumentBlock(original.id(), original.origin(), original.type(), content);
    }

    private static List<ValidationIssue> disabledDiagrams(AssignmentAttempt attempt, DocumentModel content) {
        if (attempt.snapshot().diagramEnabled()) {
            return List.of();
        }
        return content.blocks().stream().filter(b -> b.origin() == BlockOrigin.STUDENT && b.type() == BlockType.DIAGRAM)
                .map(b -> new ValidationIssue("DIAGRAM_DISABLED", b.id(), "Bài không dùng sơ đồ")).toList();
    }

    private AssignmentAttempt writable(Actor actor, UUID attemptId, String action) {
        AssignmentAttempt attempt = access.requireOwnAttemptForUpdate(actor, attemptId, action);
        if (!attempt.inProgress()) {
            throw ApiException.conflict("ATTEMPT_SUBMITTED", "Lượt đã nộp, không sửa được");
        }
        AssignmentView view = WorkAccess.call(() -> assignments.get(attempt.assignmentId()))
                .orElseThrow(() -> ApiException.notFound("NOT_FOUND"));
        Instant deadline = effectiveDeadline(attempt, view);
        if (deadline.isAfter(attempt.deadlineAt())) {
            attempts.extendDeadline(attempt.id(), deadline);
        }
        if (clock.instant().isAfter(deadline.plus(AssignmentAttempt.GRACE))) {
            throw ApiException.conflict("DEADLINE_PASSED", "Đã quá hạn, không lưu được nữa");
        }
        return attempt;
    }

    /** Hạn của lượt; bài được kéo dài hạn sau khi bắt đầu thì lượt nhận hạn mới (BR-U07-35). */
    static Instant effectiveDeadline(AssignmentAttempt attempt, AssignmentView view) {
        Instant current = view.lateUntil() != null ? view.lateUntil() : view.closesAt();
        return current != null && current.isAfter(attempt.deadlineAt()) ? current : attempt.deadlineAt();
    }

    private Optional<String> resolve(Actor actor, AssignmentAttempt attempt, String ref) {
        String[] parts = ref.split("/");
        try {
            if (parts.length == 2 && UUID.fromString(parts[0]).equals(attempt.assignmentId())) {
                UUID blockId = UUID.fromString(parts[1]);
                return assignments.getContent(attempt.assignmentId()).flatMap(c -> diagramXml(c.skeleton(), blockId));
            }
            if (parts.length == 3 && parts[0].equals("attempt")) {
                UUID otherId = UUID.fromString(parts[1]);
                UUID blockId = UUID.fromString(parts[2]);
                return attempts.find(otherId).filter(a -> a.accountId().equals(actor.accountId()))
                        .flatMap(a -> diagramXml(a.content(), blockId));
            }
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
        return Optional.empty();
    }

    private static Optional<String> diagramXml(DocumentModel document, UUID blockId) {
        return document.blocks().stream().filter(b -> blockId.equals(b.id()) && b.type() == BlockType.DIAGRAM)
                .findFirst().map(b -> b.content().path("xml").asText(null));
    }

    private IDiagramLinker linker(UUID attemptId) {
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
                .orElseThrow(() -> ApiException.notFound("NOT_FOUND"));
        BigDecimal maxPoints = content.rubrics().stream().map(RubricView::maxPoints)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Instant now = clock.instant();
        Instant deadline = effectiveDeadline(a, view);
        boolean editable = a.inProgress() && !now.isAfter(deadline.plus(AssignmentAttempt.GRACE));
        return new WorkViews.Attempt(a.id(), a.assignmentId(), a.classId(), a.snapshot().title(), a.attemptNo(),
                a.snapshot().maxAttempts(), a.status(), DocumentFiles.hideFiles(a.content()), a.contentVersion(),
                a.startedAt(), deadline, a.lastSavedAt(), a.submittedAt(), a.late(), a.submitMode(), a.receiptHash(),
                a.submitWarnings(), content.instructions(), a.snapshot().gradingMode(), view.submissionMode(),
                a.snapshot().diagramEnabled(), a.snapshot().requiredDiagrams(), content.parts(),
                maxPoints, a.snapshot().activityMonitoringEnabled(),
                a.snapshot().activityMonitoringEnabled() ? a.snapshot().activityNotice() : null, view.closesAt(),
                view.lateUntil(), editable, now);
    }

    private static WorkViews.Receipt receipt(AssignmentAttempt a) {
        int diagrams = (int) a.content().blocks().stream()
                .filter(b -> b.origin() == BlockOrigin.STUDENT && b.type() == BlockType.DIAGRAM).count();
        int blocks = (int) a.content().blocks().stream().filter(b -> b.origin() == BlockOrigin.STUDENT).count();
        return new WorkViews.Receipt(a.id(), a.assignmentId(), a.classId(), a.snapshot().title(), a.attemptNo(),
                a.snapshot().maxAttempts(), a.submittedAt(), a.late(), a.submitMode(), a.receiptHash(), blocks, diagrams);
    }

    /** Người học không thêm tiêu đề mục (BR-U07-15): tiêu đề trong DOCX thành đoạn chữ đậm. */
    private static DocumentBlock headingAsParagraph(DocumentBlock block) {
        ObjectNode content = JsonNodeFactory.instance.objectNode();
        content.putArray("runs").addObject().put("text", block.content().path("text").asText("")).put("bold", true);
        return new DocumentBlock(block.id(), block.origin(), BlockType.PARAGRAPH, content);
    }

    private ApiException reject(Actor actor, UUID assignmentId, String code, String message) {
        try {
            audit.rejected(actor, "SUBMIT_REJECTED", WorkAudit.OBJECT_ASSIGNMENT, assignmentId, code, Map.of());
        } catch (RuntimeException e) {
            return ApiException.unavailable("AUDIT_UNAVAILABLE", "Chưa ghi nhận được yêu cầu, vui lòng thử lại");
        }
        return ApiException.conflict(code, message);
    }

    private static ApiException versionConflict() {
        return ApiException.conflict("CONTENT_VERSION_CONFLICT", "Bài đang mở ở nơi khác. Hãy tải lại");
    }

    static String fileName(String title) {
        String base = title.replaceAll("[\\\\/:*?\"<>|\\r\\n]+", " ").strip();
        return (base.isEmpty() ? "bai-lam" : base.length() > 120 ? base.substring(0, 120) : base) + ".docx";
    }

    public record Started(WorkViews.Attempt attempt, boolean created) {
    }

    public record Export(String fileName, byte[] bytes) {
    }
}
