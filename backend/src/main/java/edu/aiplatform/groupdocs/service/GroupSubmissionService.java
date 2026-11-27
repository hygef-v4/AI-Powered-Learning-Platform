package edu.aiplatform.groupdocs.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.assignments.contract.AssignmentContent;
import edu.aiplatform.assignments.contract.AssignmentStatus;
import edu.aiplatform.assignments.contract.AssignmentView;
import edu.aiplatform.assignments.contract.DocumentModel;
import edu.aiplatform.assignments.contract.DiagramPngs;
import edu.aiplatform.assignments.contract.IAssignmentQuery;
import edu.aiplatform.assignments.contract.IDocumentModel;
import edu.aiplatform.assignments.contract.IDocx;
import edu.aiplatform.assignments.contract.ValidationIssue;
import edu.aiplatform.assignments.contract.ValidationResult;
import edu.aiplatform.grading.contract.IGroupEvaluation;
import edu.aiplatform.groupdocs.contract.ISseHub;
import edu.aiplatform.groupdocs.model.GroupDocument;
import edu.aiplatform.groupdocs.model.GroupDocument.SubmitMode;
import edu.aiplatform.groupdocs.repository.GroupDocumentRepository;
import edu.aiplatform.groupdocs.model.GroupSection;
import edu.aiplatform.groupdocs.model.SectionRevision;
import edu.aiplatform.groupdocs.model.SectionRules;
import edu.aiplatform.groups.service.GroupAccess;
import edu.aiplatform.groups.model.ClassGroup;
import edu.aiplatform.groups.repository.GroupRepository;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.jobs.contract.DomainEvent;
import edu.aiplatform.jobs.contract.EventTypes;
import edu.aiplatform.jobs.contract.IEventPublisher;
import edu.aiplatform.shared.error.ApiException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nộp bài nhóm (F12, F13; BR-U12-40–44; P5): trưởng nhóm nộp tay khi bài còn nhận bài, hệ thống tự nộp khi quá hạn
 * 30 giây hoặc bài ngưng giao. Bản nộp là ảnh chụp bất biến kèm mã biên nhận; U09 được báo trong cùng transaction.
 */
@Service
public class GroupSubmissionService {

    private final GroupDocumentRepository documents;
    private final GroupRepository groups;
    private final GroupDocAccess access;
    private final IAssignmentQuery assignments;
    private final IDocumentModel documentModel;
    private final IDocx docx;
    private final IGroupEvaluation evaluation;
    private final ISseHub sse;
    private final IEventPublisher events;
    private final ObjectMapper json;
    private final Clock clock;

    public GroupSubmissionService(GroupDocumentRepository documents, GroupRepository groups, GroupDocAccess access,
                                  IAssignmentQuery assignments, IDocumentModel documentModel, IDocx docx,
                                  IGroupEvaluation evaluation, ISseHub sse, IEventPublisher events,
                                  ObjectMapper json, Clock clock) {
        this.documents = documents;
        this.groups = groups;
        this.access = access;
        this.assignments = assignments;
        this.documentModel = documentModel;
        this.docx = docx;
        this.evaluation = evaluation;
        this.sse = sse;
        this.events = events;
        this.clock = clock;
        this.json = json;
    }

    /** Trưởng nhóm hiện tại nộp; còn mục chưa Xong thì cảnh báo nhưng vẫn nộp (BR-U12-40). */
    @Transactional
    public Receipt submit(Actor actor, UUID documentId, Long version) {
        GroupDocContext ctx = access.lock(actor, documentId);
        if (!ctx.leader()) {
            throw ApiException.createForbidden("GROUP_DOC_LEADER_ONLY");
        }
        access.requireEditable(ctx, false);
        GroupDocument document = ctx.document();
        if (version == null || version != document.version()) {
            throw ApiException.createConflict("GROUP_DOC_VERSION_CONFLICT", "Tài liệu vừa được thay đổi, hãy tải lại");
        }
        AssignmentView assignment = assignments.loadAssignment(document.assignmentId()).orElseThrow(GroupAccess::createNotFoundError);
        GroupSnapshot.Result snapshot = GroupSnapshot.buildSnapshot(document.sharedBlocks(), documents.listSections(documentId));
        ValidationResult result = validate(document, snapshot.document());
        if (!result.ok()) {
            throw new SubmitInvalidException(result.errors());
        }
        return store(document, ctx.group(), assignment, snapshot, actor.accountId(), SubmitMode.MANUAL,
                GroupDocument.Status.IN_PROGRESS, collectWarnings(snapshot, result));
    }

    /**
     * Tự nộp một tài liệu (F13), gọi trong transaction riêng của bộ quét. Mục đang giữ được chốt bằng bản nháp; đã có
     * bản nộp tay hoặc chưa có thay đổi nào thì chỉ nhả khóa và đóng. Câu lệnh có điều kiện nên chạy lại không nộp hai lần.
     */
    @Transactional
    public Optional<Receipt> submitAutomatically(UUID documentId) {
        GroupDocument initialDocument = documents.findById(documentId).orElse(null);
        if (initialDocument == null) {
            return Optional.empty();
        }
        assignments.lockSubmission(initialDocument.assignmentId());
        GroupDocument document = documents.lockDocument(documentId).orElse(null);
        if (document == null || document.status() != GroupDocument.Status.IN_PROGRESS) {
            return Optional.empty();
        }
        AssignmentView assignment = assignments.loadAssignment(document.assignmentId()).orElse(null);
        Instant now = clock.instant();
        if (assignment == null || !isReadyForAutoSubmit(assignment, now)) {
            return Optional.empty();
        }
        boolean finalized = closeSections(documentId, now);
        if (document.hasSubmitted() || (!finalized && !document.hasChangedSinceSubmit())) {
            if (documents.updateDocument(document.markClosed())) {
                push(document, "document.closed", null);
            }
            return Optional.empty();
        }
        SubmitMode mode = document.autoSubmitReason() != null ? document.autoSubmitReason() : SubmitMode.AUTO_DEADLINE;
        ClassGroup group = groups.findGroup(document.groupId()).orElse(null);
        if (group == null) {
            documents.updateDocument(document.markClosed());
            return Optional.empty();
        }
        GroupSnapshot.Result snapshot = GroupSnapshot.buildSnapshot(document.sharedBlocks(), documents.listSections(documentId));
        ValidationResult result = validate(document, snapshot.document());
        return Optional.of(store(document, group, assignment, snapshot, null, mode, GroupDocument.Status.CLOSED,
                collectWarnings(snapshot, result)));
    }

    private static boolean isReadyForAutoSubmit(AssignmentView assignment, Instant now) {
        if (assignment.status() == AssignmentStatus.CLOSED || assignment.status() == AssignmentStatus.RETIRED) {
            return true;
        }
        Instant deadline = assignment.lateUntil() != null ? assignment.lateUntil() : assignment.closesAt();
        return assignment.status() == AssignmentStatus.OPEN && deadline != null
                && now.isAfter(deadline.plus(SectionRules.GRACE));
    }

    private boolean closeSections(UUID documentId, Instant now) {
        boolean finalized = false;
        for (GroupSection section : documents.lockSections(documentId)) {
            if (section.status() == GroupSection.Status.CLAIMED && section.draftSavedAt() != null
                    && section.draftBlocks() != null) {
                GroupSection done = section.markDone(now);
                if (documents.updateSection(done)) {
                    documents.insertRevision(new SectionRevision(UUID.randomUUID(), section.id(), section.claimedBy(),
                            done.publishedBlocks(), now));
                    finalized = true;
                }
            } else if (section.status() == GroupSection.Status.CLAIMED) {
                documents.updateSection(section.markReleased());
            }
        }
        return finalized;
    }

    /** DOCX: thành viên tải bản hiện tại hoặc bản nộp; giảng viên chỉ bản nộp (BR-U12-44). */
    @Transactional(readOnly = true)
    public Export export(Actor actor, UUID documentId, boolean submitted) {
        return export(actor, documentId, submitted, Map.of());
    }

    /** {@code diagramPngs}: ảnh sơ đồ trình duyệt vẽ cho sơ đồ chưa có ảnh lưu sẵn (yêu cầu 2026-11-27). */
    @Transactional(readOnly = true)
    public Export export(Actor actor, UUID documentId, boolean submitted, Map<String, String> diagramPngs) {
        GroupDocContext ctx = access.loadContext(actor, documentId);
        GroupDocument document = ctx.document();
        if (ctx.teacher() && !submitted) {
            throw ApiException.createForbidden("GROUP_DOC_EXPORT_SUBMITTED_ONLY");
        }
        DocumentModel model;
        if (submitted) {
            if (document.submittedDocument() == null) {
                throw ApiException.createNotFound("GROUP_DOC_NOT_SUBMITTED");
            }
            model = document.submittedDocument();
        } else {
            model = GroupSnapshot.buildSnapshot(document.sharedBlocks(), documents.listSections(documentId)).document();
        }
        String title = assignments.loadAssignment(document.assignmentId()).map(AssignmentView::title).orElse("Bai nhom") + " - "
                + ctx.group().name();
        return new Export(title, docx.export(DiagramPngs.fillMissing(model, diagramPngs), title, blockId -> null));
    }

    private Receipt store(GroupDocument document, ClassGroup group, AssignmentView assignment, GroupSnapshot.Result snapshot,
                          UUID by, SubmitMode mode, GroupDocument.Status next, List<String> warnings) {
        Instant now = clock.instant();
        // Tự nộp chạy sau hạn cuối: trễ hay không tính theo lần sửa cuối của nhóm, như bài cá nhân (AttemptSubmitter).
        Instant effective = mode == SubmitMode.MANUAL ? now : document.lastChangedAt() != null
                ? document.lastChangedAt() : (assignment.closesAt() != null ? assignment.closesAt() : now);
        boolean late = assignment.closesAt() != null && effective.isAfter(assignment.closesAt());
        String hash = GroupSnapshot.hashReceipt(json, document.id(), snapshot, now);
        GroupDocument submitted = document.recordSubmission(snapshot.document(), snapshot.authors(), by, mode, now, late, hash,
                warnings, next);
        if (!documents.updateDocument(submitted)) {
            throw ApiException.createConflict("GROUP_DOC_VERSION_CONFLICT", "Tài liệu vừa được thay đổi, hãy tải lại");
        }
        evaluation.onGroupSubmitted(document.id(), hash, document.assignmentId(), document.classId(), group.memberIds());
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("assignmentId", document.assignmentId().toString());
        details.put("groupId", group.id().toString());
        details.put("mode", mode.name());
        details.put("late", late);
        details.put("receiptHash", hash);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("classId", document.classId().toString());
        payload.put("assignmentId", document.assignmentId().toString());
        payload.put("groupId", group.id().toString());
        payload.put("documentId", document.id().toString());
        payload.put("mode", mode.name());
        payload.put("recipientIds", group.memberIds().stream().map(UUID::toString).toList());
        events.publish(new DomainEvent(1, UUID.randomUUID(), EventTypes.GROUP_SUBMITTED, "U12", now, null, payload));
        push(document, next == GroupDocument.Status.CLOSED ? "document.auto-submitted" : "document.submitted", hash);
        return new Receipt(document.id(), mode.name(), now, late, hash, by, warnings);
    }

    private ValidationResult validate(GroupDocument document, DocumentModel snapshot) {
        AssignmentContent content = assignments.getContent(document.assignmentId()).orElse(null);
        ValidationResult result = documentModel.validateForSubmit(document.sharedBlocks(), snapshot,
                content == null ? Map.of() : content.requiredDiagrams());
        return result == null ? new ValidationResult(List.of(), List.of()) : result;
    }

    private static List<String> collectWarnings(GroupSnapshot.Result snapshot, ValidationResult result) {
        List<String> notes = new ArrayList<>();
        snapshot.undone().forEach(title -> notes.add("Mục chưa Xong không vào bản nộp: " + title));
        result.errors().forEach(i -> notes.add(i.message()));
        result.warnings().forEach(i -> notes.add(i.message()));
        return notes;
    }

    private void push(GroupDocument document, String type, String hash) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("type", type);
        event.put("documentId", document.id().toString());
        if (hash != null) {
            event.put("receiptHash", hash);
        }
        sse.publish(GroupSectionService.buildChannel(document.id()), event);
    }

    /** Biên nhận nộp; {@code warnings} gồm mục chưa Xong (không vào bản nộp). */
    public record Receipt(UUID documentId, String mode, Instant submittedAt, boolean late, String receiptHash,
                          UUID submittedBy, List<String> warnings) {
    }

    public record Export(String title, byte[] content) {
    }

    /** Tài liệu chưa qua {@code validateForSubmit}: trả danh sách cần sửa (BR-U12-40). */
    public static class SubmitInvalidException extends ApiException {

        private final transient List<ValidationIssue> issues;

        public SubmitInvalidException(List<ValidationIssue> issues) {
            super(HttpStatus.BAD_REQUEST, "GROUP_DOC_SUBMIT_INVALID", "Tài liệu chưa đủ điều kiện nộp");
            this.issues = List.copyOf(issues);
        }

        public List<ValidationIssue> listIssues() {
            return issues;
        }
    }
}
