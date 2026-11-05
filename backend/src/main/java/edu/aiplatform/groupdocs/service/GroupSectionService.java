package edu.aiplatform.groupdocs.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.assignments.contract.DocumentBlock;
import edu.aiplatform.assignments.contract.DocumentFiles;
import edu.aiplatform.assignments.contract.DocumentModel;
import edu.aiplatform.assignments.contract.IDocumentModel;
import edu.aiplatform.assignments.contract.ValidationIssue;
import edu.aiplatform.assignments.contract.ValidationResult;
import edu.aiplatform.audit.contract.AuditEvent;
import edu.aiplatform.audit.contract.AuditResult;
import edu.aiplatform.audit.contract.IAudit;
import edu.aiplatform.files.contract.ArtifactPurpose;
import edu.aiplatform.files.contract.FileRef;
import edu.aiplatform.files.contract.IArtifact;
import edu.aiplatform.groupdocs.contract.ISseHub;
import edu.aiplatform.groupdocs.repository.GroupDocumentRepository;
import edu.aiplatform.groupdocs.model.AssignmentLock;
import edu.aiplatform.groupdocs.model.GroupSection;
import edu.aiplatform.groupdocs.model.SectionRevision;
import edu.aiplatform.groupdocs.model.SectionRules;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.shared.error.ApiException;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Thao tác trên mục của tài liệu nhóm (F9–F11; BR-U12-31–38; P1): nhận, giao, nhả, lưu nháp, Xong; phân công rồi chốt
 * và lịch sử theo góp ý 2026-11-02 (FB-U12-01, 02). Khóa dòng mục trước khi đổi; mọi sửa chỉ khi tài liệu còn mở và bài còn nhận bài. Đẩy SSE cho người đang
 * mở tài liệu.
 */
@Service
public class GroupSectionService {

    public static final int MAX_BLOCKS = 2000;
    public static final int MAX_DRAFT_BYTES = 2 * 1024 * 1024;
    public static final int MAX_REVISIONS = 50;

    private final GroupDocumentRepository documents;
    private final GroupDocAccess access;
    private final IDocumentModel documentModel;
    private final ISseHub sse;
    private final IAudit audit;
    private final IArtifact artifacts;
    private final ObjectMapper json;
    private final Clock clock;

    public GroupSectionService(GroupDocumentRepository documents, GroupDocAccess access, IDocumentModel documentModel,
                               ISseHub sse, IAudit audit, IArtifact artifacts, ObjectMapper json, Clock clock) {
        this.documents = documents;
        this.access = access;
        this.documentModel = documentModel;
        this.sse = sse;
        this.audit = audit;
        this.artifacts = artifacts;
        this.json = json;
        this.clock = clock;
    }

    /**
     * Nhận mục (BR-U12-33, FB-U12-01). Chưa chốt phân công: thành viên nhận làm người phụ trách mục chưa có ai, chưa soạn.
     * Đã chốt: người phụ trách bắt đầu soạn mục của mình ({@code CLAIMED}, bản nháp từ nội dung đã đăng).
     */
    @Transactional
    public SectionView claim(Actor actor, UUID documentId, UUID sectionId) {
        GroupDocContext ctx = access.read(actor, documentId);
        GroupDocAccess.requireMember(ctx);
        access.requireEditable(ctx, false);
        boolean working = working(documentId);
        GroupSection section = lockSection(documentId, sectionId);
        refuse(SectionRules.canClaim(section, actor.accountId(), working));
        if (!working) {
            if (section.assignedTo(actor.accountId())) {
                return SectionView.of(section, actor.accountId());
            }
            GroupSection reserved = save(section.withAssignee(actor.accountId()));
            push(documentId, "section.claimed", reserved);
            return SectionView.of(reserved, actor.accountId());
        }
        if (actor.accountId().equals(section.claimedBy())) {
            return SectionView.of(section, actor.accountId());
        }
        GroupSection claimed = save(section.claimedBy(actor.accountId(), actor.accountId(), clock.instant()));
        push(documentId, "section.claimed", claimed);
        return SectionView.of(claimed, actor.accountId());
    }

    /**
     * Trưởng nhóm giao hoặc đổi người phụ trách khi chưa chốt phân công (BR-U12-33, FB-U12-01). Mục còn bản nháp đang
     * giữ (từ trước khi mở chốt) chỉ giao được khi {@code replaceHolder}; bản nháp đó bị bỏ (BR-U12-36).
     */
    @Transactional
    public SectionView assign(Actor actor, UUID documentId, UUID sectionId, UUID assigneeId, boolean replaceHolder) {
        GroupDocContext ctx = access.read(actor, documentId);
        requireLeader(ctx);
        access.requireEditable(ctx, false);
        if (assigneeId == null || !ctx.group().isMember(assigneeId)) {
            throw ApiException.badRequest("SECTION_ASSIGNEE_INVALID", "Chỉ giao được cho thành viên của nhóm");
        }
        refuse(SectionRules.canAssign(working(documentId)));
        GroupSection section = lockSection(documentId, sectionId);
        if (section.assignedTo(assigneeId)) {
            return SectionView.of(section, actor.accountId());
        }
        if (section.status() == GroupSection.Status.CLAIMED && !assigneeId.equals(section.claimedBy())) {
            if (!replaceHolder) {
                throw ApiException.conflict("SECTION_CLAIMED_BY_OTHER", "Mục còn bản nháp của người khác, cần nhả trước");
            }
            UUID previous = section.claimedBy();
            section = save(section.released());
            record(actor, "GROUP_SECTION_RELEASED", section, Map.of("holder", previous.toString(), "cause", "REASSIGNED"));
        }
        GroupSection assigned = save(section.withAssignee(assigneeId));
        record(actor, "GROUP_SECTION_ASSIGNED", assigned, Map.of("assignee", assigneeId.toString()));
        push(documentId, "section.assigned", assigned);
        return SectionView.of(assigned, actor.accountId());
    }

    /**
     * Nhả mục (BR-U12-36, FB-U12-01). Chưa chốt phân công: bỏ người phụ trách (và bản nháp nếu còn). Đã chốt: dừng soạn,
     * bản nháp bị bỏ, người phụ trách giữ nguyên.
     */
    @Transactional
    public SectionView release(Actor actor, UUID documentId, UUID sectionId) {
        GroupDocContext ctx = access.read(actor, documentId);
        access.requireEditable(ctx, false);
        boolean working = working(documentId);
        GroupSection section = lockSection(documentId, sectionId);
        refuse(SectionRules.canRelease(section, actor.accountId(), ctx.leader(), ctx.teacher(), working));
        UUID holder = section.claimedBy() != null ? section.claimedBy() : section.assigneeId();
        GroupSection next = section.status() == GroupSection.Status.CLAIMED ? section.released() : section;
        GroupSection released = save(working ? next : next.withAssignee(null));
        if (!actor.accountId().equals(holder)) {
            record(actor, "GROUP_SECTION_RELEASED", released,
                    Map.of("holder", holder.toString(), "cause", ctx.teacher() ? "TEACHER" : "LEADER"));
        }
        push(documentId, "section.released", released);
        return SectionView.of(released, actor.accountId());
    }

    /**
     * Trưởng nhóm chốt phân công cho cả tài liệu (FB-U12-01): mọi mục phải có người phụ trách là thành viên hiện tại;
     * từ đó mỗi người soạn mục của mình, không nhận hay giao thêm.
     */
    @Transactional
    public AssignmentLock lockAssignment(Actor actor, UUID documentId) {
        GroupDocContext ctx = access.read(actor, documentId);
        requireLeader(ctx);
        access.requireEditable(ctx, false);
        Optional<AssignmentLock> current = documents.assignmentLock(documentId);
        if (current.isPresent()) {
            return current.get();
        }
        long missing = documents.lockSections(documentId).stream()
                .filter(s -> s.assigneeId() == null || !ctx.group().isMember(s.assigneeId())).count();
        if (missing > 0) {
            throw ApiException.conflict("ASSIGNMENT_INCOMPLETE", "Còn " + missing + " mục chưa có người phụ trách");
        }
        AssignmentLock lock = new AssignmentLock(actor.accountId(), clock.instant());
        documents.setAssignmentLock(documentId, lock);
        recordDocument(actor, "GROUP_ASSIGNMENT_LOCKED", documentId);
        pushDocument(documentId, "assignment.locked");
        return lock;
    }

    /** Trưởng nhóm mở chốt để phân lại (FB-U12-01); bản nháp và nội dung đã Xong giữ nguyên, tạm dừng soạn. */
    @Transactional
    public void unlockAssignment(Actor actor, UUID documentId) {
        GroupDocContext ctx = access.read(actor, documentId);
        requireLeader(ctx);
        access.requireEditable(ctx, false);
        if (documents.assignmentLock(documentId).isEmpty()) {
            return;
        }
        documents.setAssignmentLock(documentId, null);
        recordDocument(actor, "GROUP_ASSIGNMENT_UNLOCKED", documentId);
        pushDocument(documentId, "assignment.unlocked");
    }

    /** Tự lưu bản nháp của người giữ kèm {@code version} của mục; lệch thì {@code 409} (BR-U12-34). */
    @Transactional
    public SectionView saveDraft(Actor actor, UUID documentId, UUID sectionId, Long version, List<DocumentBlock> blocks) {
        GroupDocContext ctx = access.read(actor, documentId);
        GroupDocAccess.requireMember(ctx);
        access.requireEditable(ctx, true);
        GroupSection section = lockSection(documentId, sectionId);
        refuse(SectionRules.canEdit(section, actor.accountId(), working(documentId)));
        if (version == null || version != section.version()) {
            throw ApiException.conflict("SECTION_VERSION_CONFLICT", "Mục vừa được thay đổi ở nơi khác, hãy tải lại");
        }
        List<DocumentBlock> content = clean(section, checkBlocks(blocks));
        rejectInvalid(validate(ctx, section, content));
        Instant now = clock.instant();
        GroupSection saved = save(section.withDraft(attach(actor, section, content), now));
        documents.touch(documentId, now);
        return SectionView.of(saved, actor.accountId());
    }

    /** Xong: kiểm block, nháp thành nội dung đã đăng, thêm lịch sử, bỏ khóa (BR-U12-35). */
    @Transactional
    public SectionView done(Actor actor, UUID documentId, UUID sectionId) {
        GroupDocContext ctx = access.read(actor, documentId);
        GroupDocAccess.requireMember(ctx);
        access.requireEditable(ctx, false);
        GroupSection section = lockSection(documentId, sectionId);
        refuse(SectionRules.canEdit(section, actor.accountId(), working(documentId)));
        List<DocumentBlock> content = section.draftBlocks() != null ? section.draftBlocks()
                : section.publishedBlocks() == null ? List.of() : section.publishedBlocks();
        rejectInvalid(validate(ctx, section, content));
        Instant now = clock.instant();
        GroupSection finished = save(section.withDraft(content, section.draftSavedAt()).done(now));
        documents.insertRevision(new SectionRevision(UUID.randomUUID(), section.id(), actor.accountId(), content, now));
        documents.touch(documentId, now);
        push(documentId, "section.done", finished);
        return SectionView.of(finished, actor.accountId());
    }

    /** Lịch sử mục, mới nhất trước (BR-U12-35, FB-U12-02): thành viên và giảng viên của lớp đọc được. */
    @Transactional(readOnly = true)
    public List<RevisionView> revisions(Actor actor, UUID documentId, UUID sectionId) {
        access.read(actor, documentId);
        GroupSection section = documents.listSections(documentId).stream().filter(s -> s.id().equals(sectionId))
                .findFirst().orElseThrow(() -> ApiException.notFound("SECTION_NOT_FOUND"));
        return documents.listRevisions(section.id(), MAX_REVISIONS).stream().map(RevisionView::of).toList();
    }

    /** Người giữ đưa một bản lịch sử vào bản nháp (FB-U12-02); bấm Xong mới thành nội dung đã đăng. */
    @Transactional
    public SectionView restore(Actor actor, UUID documentId, UUID sectionId, UUID revisionId, Long version) {
        GroupDocContext ctx = access.read(actor, documentId);
        GroupDocAccess.requireMember(ctx);
        access.requireEditable(ctx, false);
        GroupSection section = lockSection(documentId, sectionId);
        refuse(SectionRules.canEdit(section, actor.accountId(), working(documentId)));
        if (version == null || version != section.version()) {
            throw ApiException.conflict("SECTION_VERSION_CONFLICT", "Mục vừa được thay đổi ở nơi khác, hãy tải lại");
        }
        SectionRevision revision = documents.findRevision(section.id(), revisionId)
                .orElseThrow(() -> ApiException.notFound("REVISION_NOT_FOUND"));
        rejectInvalid(validate(ctx, section, revision.blocks()));
        Instant now = clock.instant();
        GroupSection restored = save(section.withDraft(revision.blocks(), now));
        documents.touch(documentId, now);
        record(actor, "GROUP_SECTION_RESTORED", restored, Map.of("revisionId", revisionId.toString()));
        return SectionView.of(restored, actor.accountId());
    }

    // ---- tiện ích -----------------------------------------------------------------------------------------------

    private GroupSection lockSection(UUID documentId, UUID sectionId) {
        return documents.lockSection(documentId, sectionId)
                .orElseThrow(() -> ApiException.notFound("SECTION_NOT_FOUND"));
    }

    /** Ghi có điều kiện theo {@code version}; trả bản đã tăng {@code version}. */
    private GroupSection save(GroupSection section) {
        if (!documents.updateSection(section)) {
            throw ApiException.conflict("SECTION_VERSION_CONFLICT", "Mục vừa được thay đổi ở nơi khác, hãy tải lại");
        }
        return new GroupSection(section.id(), section.documentId(), section.partId(), section.headingBlockId(),
                section.ancestorHeadingIds(), section.orderNo(), section.title(), section.status(), section.claimedBy(),
                section.claimedAt(), section.assignedBy(), section.publishedBlocks(), section.draftBlocks(),
                section.draftSavedAt(), section.lastAuthorId(), section.version() + 1, section.assigneeId());
    }

    /**
     * Gắn ảnh vừa tải ({@code fileRef}) vào U03 như bài cá nhân (BR-U12-35); ảnh đã có giữ {@code fileId} của bản
     * trước.
     */
    private List<DocumentBlock> attach(Actor actor, GroupSection section, List<DocumentBlock> content) {
        List<DocumentBlock> previous = section.draftBlocks() != null ? section.draftBlocks()
                : section.publishedBlocks() == null ? List.of() : section.publishedBlocks();
        return DocumentFiles.resolveFiles(new DocumentModel(previous), new DocumentModel(content),
                ref -> artifacts.attach(new FileRef(ref), actor, ArtifactPurpose.DOCUMENT_IMAGE).fileId()).blocks();
    }

    private boolean working(UUID documentId) {
        return documents.assignmentLock(documentId).isPresent();
    }

    private void recordDocument(Actor actor, String action, UUID documentId) {
        audit.record(new AuditEvent(UUID.randomUUID(), actor.accountId(), action, "GROUP_DOCUMENT", documentId.toString(),
                AuditResult.SUCCESS, null, Map.of(), clock.instant()));
    }

    private void pushDocument(UUID documentId, String type) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("type", type);
        event.put("documentId", documentId.toString());
        sse.publish(channel(documentId), event);
    }

    private static void requireLeader(GroupDocContext ctx) {
        if (!ctx.leader()) {
            throw ApiException.forbidden("GROUP_DOC_LEADER_ONLY");
        }
    }

    private List<DocumentBlock> checkBlocks(List<DocumentBlock> blocks) {
        List<DocumentBlock> content = blocks == null ? List.of() : blocks;
        if (content.size() > MAX_BLOCKS) {
            throw ApiException.payloadTooLarge("SECTION_TOO_LARGE", "Mục quá dài");
        }
        try {
            if (json.writeValueAsBytes(content).length > MAX_DRAFT_BYTES) {
                throw ApiException.payloadTooLarge("SECTION_TOO_LARGE", "Mục quá dài");
            }
        } catch (JsonProcessingException e) {
            throw ApiException.badRequest("SECTION_CONTENT_INVALID", "Nội dung mục không hợp lệ");
        }
        return content;
    }

    /**
     * Làm sạch như bài cá nhân (NFR-U07-30): bỏ khóa lạ, làm sạch SVG; {@code fileId} client gửi bị bỏ, ảnh chỉ giữ
     * {@code fileId} đã lưu của chính block đó trong mục.
     */
    private List<DocumentBlock> clean(GroupSection section, List<DocumentBlock> content) {
        List<DocumentBlock> previous = section.draftBlocks() != null ? section.draftBlocks()
                : section.publishedBlocks() == null ? List.of() : section.publishedBlocks();
        DocumentModel sanitized = documentModel.sanitize(new DocumentModel(content));
        return DocumentFiles.resolveFiles(new DocumentModel(previous), sanitized, null).blocks();
    }

    /** Kiểm nội dung mục trong khung chung (block của người soạn còn đủ, đúng thứ tự). */
    private ValidationResult validate(GroupDocContext ctx, GroupSection section, List<DocumentBlock> content) {
        DocumentModel shared = ctx.document().sharedBlocks();
        return documentModel.validateForSave(shared, GroupSnapshot.withSection(shared, section.headingBlockId(), content));
    }

    private static void rejectInvalid(ValidationResult result) {
        if (result != null && !result.ok()) {
            ValidationIssue first = result.errors().get(0);
            throw ApiException.badRequest("SECTION_CONTENT_INVALID", first.message());
        }
    }

    private static void refuse(String code) {
        if (code == null) {
            return;
        }
        switch (code) {
            case "SECTION_RELEASE_FORBIDDEN" -> throw ApiException.forbidden(code);
            case "SECTION_NOT_HELD" -> throw ApiException.conflict(code, "Bạn không giữ mục này");
            case "SECTION_NOT_CLAIMED" -> throw ApiException.conflict(code, "Mục không có ai giữ");
            case "ASSIGNMENT_LOCKED" -> throw ApiException.conflict(code, "Phân công đã chốt, trưởng nhóm mở chốt để đổi");
            case "ASSIGNMENT_NOT_LOCKED" -> throw ApiException.conflict(code, "Chờ trưởng nhóm chốt phân công rồi mới soạn");
            case "SECTION_ASSIGNED_TO_OTHER" -> throw ApiException.conflict(code, "Mục do thành viên khác phụ trách");
            default -> throw ApiException.conflict(code, "Mục đã có người nhận");
        }
    }

    private void push(UUID documentId, String type, GroupSection section) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("type", type);
        event.put("documentId", documentId.toString());
        event.put("sectionId", section.id().toString());
        event.put("status", section.status().name());
        event.put("holderId", section.claimedBy() == null ? null : section.claimedBy().toString());
        event.put("version", section.version());
        sse.publish(channel(documentId), event);
    }

    public static String channel(UUID documentId) {
        return "group-doc:" + documentId;
    }

    private void record(Actor actor, String action, GroupSection section, Map<String, Object> extra) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("documentId", section.documentId().toString());
        details.put("partId", section.partId());
        details.putAll(extra);
        audit.record(new AuditEvent(UUID.randomUUID(), actor.accountId(), action, "GROUP_SECTION", section.id().toString(),
                AuditResult.SUCCESS, null, details, clock.instant()));
    }

    /** Mục trả ra ngoài; bản nháp chỉ người giữ thấy (BR-U12-34). */
    public record SectionView(UUID id, String partId, String title, int orderNo, GroupSection.Status status, UUID holderId,
                              UUID assignedBy, List<DocumentBlock> publishedBlocks, List<DocumentBlock> draftBlocks,
                              Instant draftSavedAt, UUID lastAuthorId, long version, UUID assigneeId) {

        public static SectionView of(GroupSection s, UUID viewer) {
            boolean holder = viewer != null && viewer.equals(s.claimedBy());
            return new SectionView(s.id(), s.partId(), s.title(), s.orderNo(), s.status(), s.claimedBy(), s.assignedBy(),
                    hideFiles(s.publishedBlocks()), holder ? hideFiles(s.draftBlocks()) : null,
                    holder ? s.draftSavedAt() : null, s.lastAuthorId(), s.version(), s.assigneeId());
        }

        /** Không trả {@code fileId} của U03 cho trình duyệt; ảnh mở theo id block (như bài cá nhân). */
        private static List<DocumentBlock> hideFiles(List<DocumentBlock> blocks) {
            return blocks == null ? null : DocumentFiles.hideFiles(new DocumentModel(blocks)).blocks();
        }
    }

    /** Một bản lịch sử trả ra ngoài, ảnh không kèm {@code fileId}. */
    public record RevisionView(UUID id, UUID authorId, SectionRevision.Kind kind, Instant createdAt,
                               List<DocumentBlock> blocks) {

        static RevisionView of(SectionRevision r) {
            return new RevisionView(r.id(), r.authorId(), r.kind(), r.createdAt(),
                    DocumentFiles.hideFiles(new DocumentModel(r.blocks())).blocks());
        }
    }
}
