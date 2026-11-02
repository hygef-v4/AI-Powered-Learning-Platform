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
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Thao tác trên mục của tài liệu nhóm (F9–F11; BR-U12-31–39; P1): nhận, tiếp quản, giao, nhả, lưu nháp, Xong, chốt,
 * lịch sử. Khóa dòng mục trước khi đổi; mọi sửa chỉ khi tài liệu còn mở và bài còn nhận bài. Đẩy SSE cho người đang
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

    /** Thành viên tự nhận mục {@code OPEN} hoặc {@code DONE} (BR-U12-33). */
    @Transactional
    public SectionView claim(Actor actor, UUID documentId, UUID sectionId) {
        GroupDocContext ctx = access.read(actor, documentId);
        GroupDocAccess.requireMember(ctx);
        access.requireEditable(ctx, false);
        GroupSection section = lockSection(documentId, sectionId);
        if (actor.accountId().equals(section.claimedBy())) {
            return SectionView.of(section, actor.accountId());
        }
        refuse(SectionRules.canClaim(section, actor.accountId()));
        GroupSection claimed = save(section.claimedBy(actor.accountId(), actor.accountId(), clock.instant()));
        push(documentId, "section.claimed", claimed);
        return SectionView.of(claimed, actor.accountId());
    }

    /**
     * Trưởng nhóm giao mục cho một thành viên (BR-U12-33). Mục đang do người khác giữ chỉ giao được khi
     * {@code replaceHolder} (giao diện đã cảnh báo bản nháp của người đó sẽ bị bỏ).
     */
    @Transactional
    public SectionView assign(Actor actor, UUID documentId, UUID sectionId, UUID assigneeId, boolean replaceHolder) {
        GroupDocContext ctx = access.read(actor, documentId);
        if (!ctx.leader()) {
            throw ApiException.forbidden("GROUP_DOC_LEADER_ONLY");
        }
        access.requireEditable(ctx, false);
        if (assigneeId == null || !ctx.group().isMember(assigneeId)) {
            throw ApiException.badRequest("SECTION_ASSIGNEE_INVALID", "Chỉ giao được cho thành viên của nhóm");
        }
        GroupSection section = lockSection(documentId, sectionId);
        refuse(SectionRules.canAssign(section));
        if (assigneeId.equals(section.claimedBy())) {
            return SectionView.of(section, actor.accountId());
        }
        Instant now = clock.instant();
        if (section.status() == GroupSection.Status.CLAIMED) {
            if (!replaceHolder) {
                throw ApiException.conflict("SECTION_CLAIMED_BY_OTHER", "Mục đang do người khác giữ, cần nhả trước");
            }
            UUID previous = section.claimedBy();
            section = save(section.released());
            record(actor, "GROUP_SECTION_RELEASED", section, Map.of("holder", previous.toString(), "cause", "REASSIGNED"));
        }
        GroupSection assigned = save(section.claimedBy(assigneeId, actor.accountId(), now));
        record(actor, "GROUP_SECTION_ASSIGNED", assigned, Map.of("assignee", assigneeId.toString()));
        push(documentId, "section.assigned", assigned);
        return SectionView.of(assigned, actor.accountId());
    }

    /** Người giữ, trưởng nhóm hoặc giảng viên nhả mục; bản nháp bị bỏ (BR-U12-36). */
    @Transactional
    public SectionView release(Actor actor, UUID documentId, UUID sectionId) {
        GroupDocContext ctx = access.read(actor, documentId);
        access.requireEditable(ctx, false);
        GroupSection section = lockSection(documentId, sectionId);
        refuse(SectionRules.canRelease(section, actor.accountId(), ctx.leader(), ctx.teacher()));
        UUID holder = section.claimedBy();
        GroupSection released = save(section.released());
        if (!actor.accountId().equals(holder)) {
            record(actor, "GROUP_SECTION_RELEASED", released,
                    Map.of("holder", holder.toString(), "cause", ctx.teacher() ? "TEACHER" : "LEADER"));
        }
        push(documentId, "section.released", released);
        return SectionView.of(released, actor.accountId());
    }

    /** Tự lưu bản nháp của người giữ kèm {@code version} của mục; lệch thì {@code 409} (BR-U12-34). */
    @Transactional
    public SectionView saveDraft(Actor actor, UUID documentId, UUID sectionId, Long version, List<DocumentBlock> blocks) {
        GroupDocContext ctx = access.read(actor, documentId);
        GroupDocAccess.requireMember(ctx);
        access.requireEditable(ctx, true);
        GroupSection section = lockSection(documentId, sectionId);
        refuse(SectionRules.canEdit(section, actor.accountId()));
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
        refuse(SectionRules.canEdit(section, actor.accountId()));
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

    /**
     * Tiếp quản mục người khác đang giữ (BR-U12-33): bản nháp đã lưu của người trước được giữ và ghi vào lịch sử,
     * người mới làm tiếp từ đó. Người trước lưu tiếp sẽ nhận {@code 409 SECTION_NOT_HELD}.
     */
    @Transactional
    public SectionView takeOver(Actor actor, UUID documentId, UUID sectionId) {
        GroupDocContext ctx = access.read(actor, documentId);
        GroupDocAccess.requireMember(ctx);
        access.requireEditable(ctx, false);
        GroupSection section = lockSection(documentId, sectionId);
        if (actor.accountId().equals(section.claimedBy())) {
            return SectionView.of(section, actor.accountId());
        }
        refuse(SectionRules.canTakeOver(section, actor.accountId()));
        Instant now = clock.instant();
        UUID previous = section.claimedBy();
        if (section.draftBlocks() != null) {
            documents.insertRevision(new SectionRevision(UUID.randomUUID(), section.id(), previous, section.draftBlocks(),
                    now, SectionRevision.Kind.TAKEOVER));
        }
        GroupSection taken = save(section.takenOverBy(actor.accountId(), now));
        record(actor, "GROUP_SECTION_TAKEN_OVER", taken, Map.of("holder", previous.toString()));
        push(documentId, "section.taken", taken);
        return SectionView.of(taken, actor.accountId());
    }

    /** Trưởng nhóm chốt mục đã Xong (BR-U12-39): không ai nhận, giao, tiếp quản được tới khi mở chốt. */
    @Transactional
    public SectionView lock(Actor actor, UUID documentId, UUID sectionId) {
        GroupDocContext ctx = access.read(actor, documentId);
        requireLeader(ctx);
        access.requireEditable(ctx, false);
        GroupSection section = lockSection(documentId, sectionId);
        if (section.locked()) {
            return SectionView.of(section, actor.accountId());
        }
        refuse(SectionRules.canLock(section));
        GroupSection locked = save(section.lockedBy(actor.accountId(), clock.instant()));
        record(actor, "GROUP_SECTION_LOCKED", locked, Map.of());
        push(documentId, "section.locked", locked);
        return SectionView.of(locked, actor.accountId());
    }

    @Transactional
    public SectionView unlock(Actor actor, UUID documentId, UUID sectionId) {
        GroupDocContext ctx = access.read(actor, documentId);
        requireLeader(ctx);
        access.requireEditable(ctx, false);
        GroupSection section = lockSection(documentId, sectionId);
        if (!section.locked()) {
            return SectionView.of(section, actor.accountId());
        }
        GroupSection unlocked = save(section.unlocked());
        record(actor, "GROUP_SECTION_UNLOCKED", unlocked, Map.of());
        push(documentId, "section.unlocked", unlocked);
        return SectionView.of(unlocked, actor.accountId());
    }

    /** Lịch sử mục, mới nhất trước (BR-U12-35): thành viên và giảng viên của lớp đọc được. */
    @Transactional(readOnly = true)
    public List<RevisionView> revisions(Actor actor, UUID documentId, UUID sectionId) {
        access.read(actor, documentId);
        GroupSection section = documents.listSections(documentId).stream().filter(s -> s.id().equals(sectionId))
                .findFirst().orElseThrow(() -> ApiException.notFound("SECTION_NOT_FOUND"));
        return documents.listRevisions(section.id(), MAX_REVISIONS).stream().map(RevisionView::of).toList();
    }

    /** Người giữ đưa một bản lịch sử vào bản nháp (BR-U12-35); bấm Xong mới thành nội dung đã đăng. */
    @Transactional
    public SectionView restore(Actor actor, UUID documentId, UUID sectionId, UUID revisionId, Long version) {
        GroupDocContext ctx = access.read(actor, documentId);
        GroupDocAccess.requireMember(ctx);
        access.requireEditable(ctx, false);
        GroupSection section = lockSection(documentId, sectionId);
        refuse(SectionRules.canEdit(section, actor.accountId()));
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
                section.draftSavedAt(), section.lastAuthorId(), section.version() + 1, section.lockedBy(),
                section.lockedAt());
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
            case "SECTION_LOCKED" -> throw ApiException.conflict(code, "Mục đã được trưởng nhóm chốt");
            case "SECTION_NOT_DONE" -> throw ApiException.conflict(code, "Chỉ chốt được mục đã Xong");
            default -> throw ApiException.conflict(code, "Mục đang do người khác giữ");
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
                              Instant draftSavedAt, UUID lastAuthorId, long version, UUID lockedBy, Instant lockedAt) {

        public static SectionView of(GroupSection s, UUID viewer) {
            boolean holder = viewer != null && viewer.equals(s.claimedBy());
            return new SectionView(s.id(), s.partId(), s.title(), s.orderNo(), s.status(), s.claimedBy(), s.assignedBy(),
                    hideFiles(s.publishedBlocks()), holder ? hideFiles(s.draftBlocks()) : null,
                    holder ? s.draftSavedAt() : null, s.lastAuthorId(), s.version(), s.lockedBy(), s.lockedAt());
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
