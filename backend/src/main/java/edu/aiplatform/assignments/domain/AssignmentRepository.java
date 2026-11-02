package edu.aiplatform.assignments.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.assignments.contract.AssignmentDayCount;
import edu.aiplatform.assignments.contract.AssignmentScope;
import edu.aiplatform.assignments.contract.AssignmentStatus;
import edu.aiplatform.assignments.contract.DiagramType;
import edu.aiplatform.assignments.contract.DocumentModel;
import edu.aiplatform.assignments.contract.DocumentPart;
import edu.aiplatform.assignments.contract.GradingMode;
import edu.aiplatform.assignments.contract.SubmissionMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Bảng {@code assignments}; lưu có điều kiện {@code version}, mọi lần ghi tăng {@code version} (NFR-U07-10). */
@Repository
public class AssignmentRepository {

    private static final String COLUMNS = "id, scope, class_id, subject_id, source_assignment_id, origin, version_no, "
            + "submission_mode, diagram_enabled, grading_mode, title, instructions, skeleton, parts, required_diagrams, "
            + "reference_material_ids, activity_monitoring_enabled, activity_notice, status, opens_at, closes_at, "
            + "late_until, max_attempts, retired_reason, created_by, created_at, updated_at, version";

    private final JdbcClient jdbc;
    private final ObjectMapper json;

    public AssignmentRepository(JdbcClient jdbc, ObjectMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    public void insert(Assignment a) {
        jdbc.sql("insert into assignments (" + COLUMNS + ") values (:id, :scope, :classId, :subjectId, :source, "
                        + ":origin, :versionNo, :submissionMode, :diagramEnabled, :gradingMode, :title, :instructions, "
                        + "cast(:skeleton as jsonb), cast(:parts as jsonb), cast(:required as jsonb), "
                        + "cast(:materials as jsonb), :activity, :notice, :status, :opensAt, :closesAt, :lateUntil, "
                        + ":maxAttempts, :reason, :createdBy, :createdAt, :updatedAt, :version)")
                .params(params(a)).param("source", a.sourceAssignmentId()).param("origin", a.origin().name())
                .param("versionNo", a.versionNo()).param("createdBy", a.createdBy())
                .param("createdAt", ts(a.createdAt())).param("version", a.version())
                .update();
    }

    public Optional<Assignment> find(UUID id) {
        return jdbc.sql("select " + COLUMNS + " from assignments where id = :id").param("id", id)
                .query(this::map).optional();
    }

    /** Khóa dòng tới cuối transaction: phát hành, ngưng giao, áp dụng đề xuất chạy lần lượt với scanner. */
    public Optional<Assignment> findForUpdate(UUID id) {
        return jdbc.sql("select " + COLUMNS + " from assignments where id = :id for update").param("id", id)
                .query(this::map).optional();
    }

    /** Ghi mọi cột đổi được khi {@code version} còn khớp; trả bản đã tăng {@code version}. */
    public Assignment save(Assignment a) {
        int updated = jdbc.sql("update assignments set diagram_enabled = :diagramEnabled, grading_mode = :gradingMode, "
                        + "title = :title, instructions = :instructions, skeleton = cast(:skeleton as jsonb), "
                        + "parts = cast(:parts as jsonb), required_diagrams = cast(:required as jsonb), "
                        + "reference_material_ids = cast(:materials as jsonb), activity_monitoring_enabled = :activity, "
                        + "activity_notice = :notice, status = :status, opens_at = :opensAt, closes_at = :closesAt, "
                        + "late_until = :lateUntil, max_attempts = :maxAttempts, retired_reason = :reason, "
                        + "updated_at = :updatedAt, version = version + 1 where id = :id and version = :version")
                .params(params(a)).param("version", a.version()).update();
        if (updated != 1) {
            throw new OptimisticLockingFailureException("assignment " + a.id());
        }
        return a.withVersion(a.version() + 1);
    }

    /** Bài lớp của lớp và bài môn đã phát hành của môn chứa lớp (F1). */
    public List<Assignment> listForClass(UUID classId, UUID subjectId) {
        return jdbc.sql("select " + COLUMNS + " from assignments where (scope = 'CLASS' and class_id = :classId) "
                        + "or (scope = 'SUBJECT' and subject_id = :subjectId "
                        + "and status in ('SCHEDULED', 'OPEN', 'CLOSED', 'RETIRED')) order by created_at desc")
                .param("classId", classId).param("subjectId", subjectId).query(this::map).list();
    }

    /** Số bài đã phát hành theo ngày giao và loại (luyện tập, cá nhân, nhóm); ngày tính theo {@code zone}. */
    public List<AssignmentDayCount> countGivenByDay(Instant from, Instant to, ZoneId zone) {
        return jdbc.sql("select cast(coalesce(opens_at, created_at) at time zone :zone as date) as day, "
                        + "case when grading_mode = 'PRACTICE' then 'PRACTICE' else submission_mode end as kind, "
                        + "count(*) as total from assignments "
                        + "where status in ('SCHEDULED', 'OPEN', 'CLOSED', 'RETIRED') "
                        + "and coalesce(opens_at, created_at) >= :from and coalesce(opens_at, created_at) < :to "
                        + "group by 1, 2 order by 1, 2")
                .param("zone", zone.getId()).param("from", Timestamp.from(from)).param("to", Timestamp.from(to))
                .query((rs, row) -> new AssignmentDayCount(rs.getDate("day").toLocalDate(), rs.getString("kind"),
                        rs.getLong("total")))
                .list();
    }

    public List<Assignment> listOfClass(UUID classId) {
        return jdbc.sql("select " + COLUMNS + " from assignments where scope = 'CLASS' and class_id = :classId "
                        + "order by created_at desc")
                .param("classId", classId).query(this::map).list();
    }

    public List<Assignment> listOfSubject(UUID subjectId) {
        return jdbc.sql("select " + COLUMNS + " from assignments where scope = 'SUBJECT' and subject_id = :subjectId "
                        + "order by created_at desc")
                .param("subjectId", subjectId).query(this::map).list();
    }

    public List<UUID> dueToOpen(Instant now, int limit) {
        return jdbc.sql("select id from assignments where status = 'SCHEDULED' and opens_at <= :now "
                        + "order by opens_at limit :limit")
                .param("now", ts(now)).param("limit", limit).query(UUID.class).list();
    }

    /** {@code SCHEDULED → OPEN} có điều kiện; chạy lại không mở hai lần (P3). */
    public int open(UUID id, Instant now) {
        return jdbc.sql("update assignments set status = 'OPEN', updated_at = :now, version = version + 1 "
                        + "where id = :id and status = 'SCHEDULED' and opens_at <= :now")
                .param("id", id).param("now", ts(now)).update();
    }

    /** {@code OPEN → CLOSED} khi qua {@code lateUntil}, hoặc {@code closesAt} nếu không có (BR-U07-34). */
    public List<UUID> closeDue(Instant now) {
        return jdbc.sql("update assignments set status = 'CLOSED', updated_at = :now, version = version + 1 "
                        + "where status = 'OPEN' and coalesce(late_until, closes_at) <= :now returning id")
                .param("now", ts(now)).query(UUID.class).list();
    }

    /** Chỉ xóa bài chưa từng phát hành (BR-U07-31); rubric và đề xuất AI xóa theo khóa ngoại. */
    public int delete(UUID id, long version) {
        return jdbc.sql("delete from assignments where id = :id and version = :version "
                        + "and status in ('DRAFT', 'REVIEWED')")
                .param("id", id).param("version", version).update();
    }

    public boolean hasOpenVersion(UUID sourceId) {
        return jdbc.sql("select count(*) from assignments where source_assignment_id = :id and origin = 'NEW_VERSION' "
                        + "and status in ('DRAFT', 'REVIEWED')")
                .param("id", sourceId).query(Long.class).single() > 0;
    }

    public List<Assignment> listClosingBetween(Instant from, Instant to) {
        return jdbc.sql("select " + COLUMNS + " from assignments where status = 'OPEN' and closes_at >= :from "
                        + "and closes_at < :to order by closes_at")
                .param("from", ts(from)).param("to", ts(to)).query(this::map).list();
    }

    private Map<String, Object> params(Assignment a) {
        Map<String, Object> values = new java.util.HashMap<>();
        values.put("id", a.id());
        values.put("scope", a.scope().name());
        values.put("classId", a.classId());
        values.put("subjectId", a.subjectId());
        values.put("submissionMode", a.submissionMode().name());
        values.put("diagramEnabled", a.diagramEnabled());
        values.put("gradingMode", a.gradingMode().name());
        values.put("title", a.title());
        values.put("instructions", a.instructions());
        values.put("skeleton", write(a.skeleton()));
        values.put("parts", write(a.parts()));
        values.put("required", write(a.requiredDiagrams()));
        values.put("materials", write(a.referenceMaterialIds()));
        values.put("activity", a.activityMonitoringEnabled());
        values.put("notice", a.activityNotice());
        values.put("status", a.status().name());
        values.put("opensAt", ts(a.opensAt()));
        values.put("closesAt", ts(a.closesAt()));
        values.put("lateUntil", ts(a.lateUntil()));
        values.put("maxAttempts", a.maxAttempts());
        values.put("reason", a.retiredReason());
        values.put("updatedAt", ts(a.updatedAt()));
        return values;
    }

    private Assignment map(ResultSet rs, int row) throws SQLException {
        Map<DiagramType, Integer> required = new EnumMap<>(DiagramType.class);
        required.putAll(read(rs.getString("required_diagrams"), new TypeReference<Map<DiagramType, Integer>>() { }));
        return new Assignment(rs.getObject("id", UUID.class), AssignmentScope.valueOf(rs.getString("scope")),
                rs.getObject("class_id", UUID.class), rs.getObject("subject_id", UUID.class),
                rs.getObject("source_assignment_id", UUID.class), AssignmentOrigin.valueOf(rs.getString("origin")),
                rs.getInt("version_no"), SubmissionMode.valueOf(rs.getString("submission_mode")),
                rs.getBoolean("diagram_enabled"), GradingMode.valueOf(rs.getString("grading_mode")),
                rs.getString("title"), rs.getString("instructions"),
                read(rs.getString("skeleton"), new TypeReference<DocumentModel>() { }),
                read(rs.getString("parts"), new TypeReference<List<DocumentPart>>() { }), required,
                read(rs.getString("reference_material_ids"), new TypeReference<List<UUID>>() { }),
                rs.getBoolean("activity_monitoring_enabled"), rs.getString("activity_notice"),
                AssignmentStatus.valueOf(rs.getString("status")), instant(rs, "opens_at"), instant(rs, "closes_at"),
                instant(rs, "late_until"), (Integer) rs.getObject("max_attempts"), rs.getString("retired_reason"),
                rs.getObject("created_by", UUID.class), instant(rs, "created_at"), instant(rs, "updated_at"),
                rs.getLong("version"));
    }

    private String write(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Không ghi được JSON của bài", e);
        }
    }

    private <T> T read(String value, TypeReference<T> type) {
        try {
            return json.readValue(value, type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Không đọc được JSON của bài", e);
        }
    }

    static Timestamp ts(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }

    static Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }
}
