package edu.aiplatform.assignments.repository;

import edu.aiplatform.assignments.model.AssignmentRubric;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.assignments.contract.RubricCriterion;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Bảng {@code assignment_rubrics}; rubric đã khóa không sửa, không xóa (BR-U07-23). */
@Repository
public class RubricRepository {

    private final JdbcClient jdbc;
    private final ObjectMapper json;

    public RubricRepository(JdbcClient jdbc, ObjectMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    public List<AssignmentRubric> listByAssignment(UUID assignmentId) {
        return jdbc.sql("select * from assignment_rubrics where assignment_id = :id order by created_at, part_id")
                .param("id", assignmentId).query(this::map).list();
    }

    public Optional<AssignmentRubric> find(UUID rubricId) {
        return jdbc.sql("select * from assignment_rubrics where id = :id").param("id", rubricId)
                .query(this::map).optional();
    }

    public void insert(AssignmentRubric r) {
        jdbc.sql("insert into assignment_rubrics (id, assignment_id, part_id, criteria, total_points, locked_at, "
                        + "created_at, updated_at, version) values (:id, :assignmentId, :partId, cast(:criteria as jsonb), "
                        + ":total, :lockedAt, :createdAt, :updatedAt, :version)")
                .param("id", r.id()).param("assignmentId", r.assignmentId()).param("partId", r.partId())
                .param("criteria", write(r.criteria())).param("total", r.totalPoints())
                .param("lockedAt", AssignmentRepository.ts(r.lockedAt()))
                .param("createdAt", AssignmentRepository.ts(r.createdAt()))
                .param("updatedAt", AssignmentRepository.ts(r.updatedAt())).param("version", r.version())
                .update();
    }

    public AssignmentRubric save(AssignmentRubric r) {
        int updated = jdbc.sql("update assignment_rubrics set criteria = cast(:criteria as jsonb), total_points = :total, "
                        + "updated_at = :now, version = version + 1 where id = :id and version = :version "
                        + "and locked_at is null")
                .param("id", r.id()).param("criteria", write(r.criteria())).param("total", r.totalPoints())
                .param("now", AssignmentRepository.ts(r.updatedAt())).param("version", r.version()).update();
        if (updated != 1) {
            throw new OptimisticLockingFailureException("rubric " + r.id());
        }
        return new AssignmentRubric(r.id(), r.assignmentId(), r.partId(), r.criteria(), r.totalPoints(), r.lockedAt(),
                r.createdAt(), r.updatedAt(), r.version() + 1);
    }

    /** Phần không còn trong khung thì xóa rubric (BR-U07-20); chỉ khi chưa khóa. */
    public int deleteParts(UUID assignmentId, Collection<String> partIds) {
        if (partIds.isEmpty()) {
            return 0;
        }
        return jdbc.sql("delete from assignment_rubrics where assignment_id = :id and part_id in (:parts) "
                        + "and locked_at is null")
                .param("id", assignmentId).param("parts", partIds).update();
    }

    /** Khóa mọi rubric trong transaction phát hành (P2). */
    public int lockAll(UUID assignmentId, Instant now) {
        return jdbc.sql("update assignment_rubrics set locked_at = :now, version = version + 1 "
                        + "where assignment_id = :id and locked_at is null")
                .param("id", assignmentId).param("now", AssignmentRepository.ts(now)).update();
    }

    private AssignmentRubric map(ResultSet rs, int row) throws SQLException {
        return new AssignmentRubric(rs.getObject("id", UUID.class), rs.getObject("assignment_id", UUID.class),
                rs.getString("part_id"), read(rs.getString("criteria")), rs.getBigDecimal("total_points"),
                AssignmentRepository.instant(rs, "locked_at"), AssignmentRepository.instant(rs, "created_at"),
                AssignmentRepository.instant(rs, "updated_at"), rs.getLong("version"));
    }

    private String write(List<RubricCriterion> criteria) {
        try {
            return json.writeValueAsString(criteria);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Không ghi được JSON của rubric", e);
        }
    }

    private List<RubricCriterion> read(String value) {
        try {
            return json.readValue(value, new TypeReference<List<RubricCriterion>>() { });
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Không đọc được JSON của rubric", e);
        }
    }
}
