package edu.aiplatform.groupdocs.repository;

import edu.aiplatform.groupdocs.model.GroupDocument;
import edu.aiplatform.groupdocs.model.GroupSection;
import edu.aiplatform.groupdocs.model.SectionRevision;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.assignments.contract.DocumentBlock;
import edu.aiplatform.assignments.contract.DocumentModel;
import edu.aiplatform.groupdocs.model.GroupDocument.SectionAuthor;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Bảng tài liệu nhóm, mục và lịch sử; chỉ dùng trong U12. Thao tác trên mục khóa dòng mục ({@code FOR UPDATE}),
 * nộp khóa dòng tài liệu (P1, NFR-U12-10); tạo tài liệu idempotent (P3).
 */
@Repository
public class GroupDocumentRepository {

    private static final TypeReference<List<DocumentBlock>> BLOCKS = new TypeReference<>() {
    };
    private static final TypeReference<List<UUID>> IDS = new TypeReference<>() {
    };
    private static final TypeReference<List<SectionAuthor>> AUTHORS = new TypeReference<>() {
    };
    private static final TypeReference<List<String>> STRINGS = new TypeReference<>() {
    };
    private static final String DOC_COLUMNS = "id, group_id, assignment_id, class_id, status, shared_blocks::text as shared, "
            + "activity_monitoring, auto_submit_reason, submitted_document::text as submitted, "
            + "section_authors::text as authors, submitted_by, submit_mode, submitted_at, late, receipt_hash, "
            + "warnings::text as warnings, last_changed_at, version, created_at";
    private static final String SECTION_COLUMNS = "id, group_document_id, part_id, heading_block_id, "
            + "ancestor_heading_ids::text as ancestors, order_no, title, status, claimed_by, claimed_at, assigned_by, "
            + "published_blocks::text as published, draft_blocks::text as draft, draft_saved_at, last_author_id, version";

    private final JdbcClient jdbc;
    private final ObjectMapper json;

    public GroupDocumentRepository(JdbcClient jdbc, ObjectMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    // ---- tài liệu --------------------------------------------------------------------------------------------

    /** Tạo nếu chưa có cặp (nhóm, bài); trả {@code true} khi vừa tạo. */
    public boolean insertIfAbsent(GroupDocument d) {
        return jdbc.sql("insert into group_documents (id, group_id, assignment_id, class_id, status, shared_blocks, "
                        + "activity_monitoring, last_changed_at, version, created_at) values (:id, :groupId, :assignmentId, "
                        + ":classId, :status, cast(:shared as jsonb), :activity, :lastChangedAt, 0, :createdAt) "
                        + "on conflict (group_id, assignment_id) do nothing")
                .param("id", d.id()).param("groupId", d.groupId()).param("assignmentId", d.assignmentId())
                .param("classId", d.classId()).param("status", d.status().name()).param("shared", write(d.sharedBlocks()))
                .param("activity", d.activityMonitoring()).param("lastChangedAt", ts(d.lastChangedAt()))
                .param("createdAt", ts(d.createdAt())).update() == 1;
    }

    public Optional<GroupDocument> findById(UUID id) {
        return jdbc.sql("select " + DOC_COLUMNS + " from group_documents where id = :id").param("id", id)
                .query(this::document).optional();
    }

    public Optional<GroupDocument> lockDocument(UUID id) {
        return jdbc.sql("select " + DOC_COLUMNS + " from group_documents where id = :id for update").param("id", id)
                .query(this::document).optional();
    }

    public Optional<GroupDocument> findByGroupAndAssignment(UUID groupId, UUID assignmentId) {
        return jdbc.sql("select " + DOC_COLUMNS + " from group_documents where group_id = :groupId "
                        + "and assignment_id = :assignmentId")
                .param("groupId", groupId).param("assignmentId", assignmentId).query(this::document).optional();
    }

    public List<GroupDocument> listByAssignmentAndClass(UUID assignmentId, UUID classId) {
        return jdbc.sql("select " + DOC_COLUMNS + " from group_documents where assignment_id = :assignmentId "
                        + "and class_id = :classId order by created_at")
                .param("assignmentId", assignmentId).param("classId", classId).query(this::document).list();
    }

    public List<GroupDocument> listByAssignment(UUID assignmentId) {
        return jdbc.sql("select " + DOC_COLUMNS + " from group_documents where assignment_id = :assignmentId")
                .param("assignmentId", assignmentId).query(this::document).list();
    }

    public List<GroupDocument> listByGroup(UUID groupId) {
        return jdbc.sql("select " + DOC_COLUMNS + " from group_documents where group_id = :groupId order by created_at")
                .param("groupId", groupId).query(this::document).list();
    }

    public List<GroupDocument> listByGroups(Collection<UUID> groupIds, Collection<UUID> assignmentIds) {
        if (groupIds.isEmpty() || assignmentIds.isEmpty()) {
            return List.of();
        }
        return jdbc.sql("select " + DOC_COLUMNS + " from group_documents where group_id in (:groups) "
                        + "and assignment_id in (:assignments)")
                .param("groups", groupIds).param("assignments", assignmentIds).query(this::document).list();
    }

    /** Tài liệu đang làm cần tự nộp: bài ngưng giao đã đánh dấu, hoặc các bài truyền vào (đã quá hạn). */
    public List<UUID> listDueForAutoSubmit(Collection<UUID> overdueAssignments) {
        String overdue = overdueAssignments.isEmpty() ? "" : " or assignment_id in (:assignments)";
        var spec = jdbc.sql("select id from group_documents where status = 'IN_PROGRESS' and (auto_submit_reason is not null"
                + overdue + ")");
        if (!overdueAssignments.isEmpty()) {
            spec = spec.param("assignments", overdueAssignments);
        }
        return spec.query(UUID.class).list();
    }

    public List<UUID> listInProgressAssignments() {
        return jdbc.sql("select distinct assignment_id from group_documents where status = 'IN_PROGRESS'")
                .query(UUID.class).list();
    }

    /** Đánh dấu tự nộp vì ngưng giao cho mọi tài liệu đang làm của bài (BR-U12-43). */
    public int markRetired(UUID assignmentId) {
        return jdbc.sql("update group_documents set auto_submit_reason = 'AUTO_RETIRED' where assignment_id = :assignmentId "
                        + "and status = 'IN_PROGRESS'")
                .param("assignmentId", assignmentId).update();
    }

    public boolean update(GroupDocument d) {
        Map<String, Object> p = new HashMap<>();
        p.put("id", d.id());
        p.put("version", d.version());
        p.put("status", d.status().name());
        p.put("autoReason", d.autoSubmitReason() == null ? null : d.autoSubmitReason().name());
        p.put("submitted", d.submittedDocument() == null ? null : write(d.submittedDocument()));
        p.put("authors", write(d.sectionAuthors()));
        p.put("submittedBy", d.submittedBy());
        p.put("mode", d.submitMode() == null ? null : d.submitMode().name());
        p.put("submittedAt", ts(d.submittedAt()));
        p.put("late", d.late());
        p.put("hash", d.receiptHash());
        p.put("warnings", write(d.warnings()));
        p.put("lastChangedAt", ts(d.lastChangedAt()));
        return jdbc.sql("update group_documents set status = :status, auto_submit_reason = :autoReason, "
                        + "submitted_document = cast(:submitted as jsonb), section_authors = cast(:authors as jsonb), "
                        + "submitted_by = :submittedBy, submit_mode = :mode, submitted_at = :submittedAt, late = :late, "
                        + "receipt_hash = :hash, warnings = cast(:warnings as jsonb), last_changed_at = :lastChangedAt, "
                        + "version = version + 1 where id = :id and version = :version")
                .params(p).update() == 1;
    }

    public void touch(UUID documentId, Instant now) {
        jdbc.sql("update group_documents set last_changed_at = :now where id = :id").param("id", documentId)
                .param("now", ts(now)).update();
    }

    /** Nhóm có tài liệu có nội dung hoặc bản nộp thì không xóa được (BR-U12-05). */
    public boolean groupHasContent(UUID groupId) {
        return jdbc.sql("select exists (select 1 from group_documents d where d.group_id = :groupId and "
                        + "(d.submitted_at is not null or exists (select 1 from group_document_sections s "
                        + "where s.group_document_id = d.id and (s.published_blocks is not null or s.draft_blocks is not null))))")
                .param("groupId", groupId).query(Boolean.class).single();
    }

    // ---- mục -------------------------------------------------------------------------------------------------

    public void insertSection(GroupSection s) {
        jdbc.sql("insert into group_document_sections (id, group_document_id, part_id, heading_block_id, "
                        + "ancestor_heading_ids, order_no, title, status, version) values (:id, :documentId, :partId, "
                        + ":heading, cast(:ancestors as jsonb), :orderNo, :title, :status, 0) "
                        + "on conflict (group_document_id, part_id) do nothing")
                .param("id", s.id()).param("documentId", s.documentId()).param("partId", s.partId())
                .param("heading", s.headingBlockId()).param("ancestors", write(s.ancestorHeadingIds()))
                .param("orderNo", s.orderNo()).param("title", s.title()).param("status", s.status().name()).update();
    }

    public List<GroupSection> listSections(UUID documentId) {
        return jdbc.sql("select " + SECTION_COLUMNS + " from group_document_sections where group_document_id = :id "
                        + "order by order_no")
                .param("id", documentId).query(this::section).list();
    }

    public List<GroupSection> lockSections(UUID documentId) {
        return jdbc.sql("select " + SECTION_COLUMNS + " from group_document_sections where group_document_id = :id "
                        + "order by order_no for update")
                .param("id", documentId).query(this::section).list();
    }

    public Optional<GroupSection> lockSection(UUID documentId, UUID sectionId) {
        return jdbc.sql("select " + SECTION_COLUMNS + " from group_document_sections where id = :id "
                        + "and group_document_id = :documentId for update")
                .param("id", sectionId).param("documentId", documentId).query(this::section).optional();
    }

    /** Mục đang do một người giữ trong các tài liệu của nhóm (để nhả khi người đó rời nhóm). */
    public List<GroupSection> lockSectionsHeldBy(UUID groupId, UUID accountId) {
        return jdbc.sql("select s.id, s.group_document_id, s.part_id, s.heading_block_id, s.ancestor_heading_ids::text as ancestors, "
                        + "s.order_no, s.title, s.status, s.claimed_by, s.claimed_at, s.assigned_by, "
                        + "s.published_blocks::text as published, s.draft_blocks::text as draft, s.draft_saved_at, "
                        + "s.last_author_id, s.version from group_document_sections s join group_documents d "
                        + "on d.id = s.group_document_id where d.group_id = :groupId and s.claimed_by = :accountId for update of s")
                .param("groupId", groupId).param("accountId", accountId).query(this::section).list();
    }

    /** Ghi khi {@code version} khớp, tăng {@code version}; lệch trả {@code false}. */
    public boolean updateSection(GroupSection s) {
        Map<String, Object> p = new HashMap<>();
        p.put("id", s.id());
        p.put("version", s.version());
        p.put("status", s.status().name());
        p.put("claimedBy", s.claimedBy());
        p.put("claimedAt", ts(s.claimedAt()));
        p.put("assignedBy", s.assignedBy());
        p.put("published", s.publishedBlocks() == null ? null : write(s.publishedBlocks()));
        p.put("draft", s.draftBlocks() == null ? null : write(s.draftBlocks()));
        p.put("draftSavedAt", ts(s.draftSavedAt()));
        p.put("lastAuthor", s.lastAuthorId());
        return jdbc.sql("update group_document_sections set status = :status, claimed_by = :claimedBy, "
                        + "claimed_at = :claimedAt, assigned_by = :assignedBy, published_blocks = cast(:published as jsonb), "
                        + "draft_blocks = cast(:draft as jsonb), draft_saved_at = :draftSavedAt, last_author_id = :lastAuthor, "
                        + "version = version + 1 where id = :id and version = :version")
                .params(p).update() == 1;
    }

    public void insertRevision(SectionRevision r) {
        jdbc.sql("insert into group_document_revisions (id, section_id, author_id, blocks, created_at) values "
                        + "(:id, :sectionId, :authorId, cast(:blocks as jsonb), :createdAt)")
                .param("id", r.id()).param("sectionId", r.sectionId()).param("authorId", r.authorId())
                .param("blocks", write(r.blocks())).param("createdAt", ts(r.createdAt())).update();
    }

    // ---- ánh xạ ----------------------------------------------------------------------------------------------

    private GroupDocument document(ResultSet rs, int n) throws SQLException {
        String auto = rs.getString("auto_submit_reason");
        String mode = rs.getString("submit_mode");
        String submitted = rs.getString("submitted");
        return new GroupDocument(rs.getObject("id", UUID.class), rs.getObject("group_id", UUID.class),
                rs.getObject("assignment_id", UUID.class), rs.getObject("class_id", UUID.class),
                GroupDocument.Status.valueOf(rs.getString("status")), read(rs.getString("shared"), DocumentModel.class),
                rs.getBoolean("activity_monitoring"), auto == null ? null : GroupDocument.SubmitMode.valueOf(auto),
                submitted == null ? null : read(submitted, DocumentModel.class), readList(rs.getString("authors"), AUTHORS),
                rs.getObject("submitted_by", UUID.class), mode == null ? null : GroupDocument.SubmitMode.valueOf(mode),
                instant(rs.getTimestamp("submitted_at")), rs.getBoolean("late"), rs.getString("receipt_hash"),
                readList(rs.getString("warnings"), STRINGS), instant(rs.getTimestamp("last_changed_at")),
                rs.getLong("version"), instant(rs.getTimestamp("created_at")));
    }

    private GroupSection section(ResultSet rs, int n) throws SQLException {
        String published = rs.getString("published");
        String draft = rs.getString("draft");
        return new GroupSection(rs.getObject("id", UUID.class), rs.getObject("group_document_id", UUID.class),
                rs.getString("part_id"), rs.getObject("heading_block_id", UUID.class),
                readList(rs.getString("ancestors"), IDS), rs.getInt("order_no"), rs.getString("title"),
                GroupSection.Status.valueOf(rs.getString("status")), rs.getObject("claimed_by", UUID.class),
                instant(rs.getTimestamp("claimed_at")), rs.getObject("assigned_by", UUID.class),
                published == null ? null : readList(published, BLOCKS), draft == null ? null : readList(draft, BLOCKS),
                instant(rs.getTimestamp("draft_saved_at")), rs.getObject("last_author_id", UUID.class),
                rs.getLong("version"));
    }

    private String write(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Không ghi được JSON tài liệu nhóm", e);
        }
    }

    private <T> T read(String value, Class<T> type) {
        try {
            return json.readValue(value, type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Không đọc được JSON tài liệu nhóm", e);
        }
    }

    private <T> List<T> readList(String value, TypeReference<List<T>> type) {
        try {
            return value == null ? List.of() : json.readValue(value, type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Không đọc được JSON tài liệu nhóm", e);
        }
    }

    private static Timestamp ts(Instant value) {
        return value == null ? null : Timestamp.from(value);
    }

    private static Instant instant(Timestamp value) {
        return value == null ? null : value.toInstant();
    }
}
