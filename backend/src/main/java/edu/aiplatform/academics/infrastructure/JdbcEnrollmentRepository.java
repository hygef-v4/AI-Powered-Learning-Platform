package edu.aiplatform.academics.infrastructure;

import edu.aiplatform.academics.domain.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcEnrollmentRepository implements IEnrollmentRepository {
    private final NamedParameterJdbcTemplate jdbc;
    public JdbcEnrollmentRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override public Optional<Enrollment> find(UUID classId, UUID accountId) {
        return jdbc.query("select * from enrollments where class_id = :classId and account_id = :accountId",
                Map.of("classId", classId, "accountId", accountId), JdbcEnrollmentRepository::map).stream().findFirst();
    }

    @Override public List<Enrollment> listByClass(UUID classId, boolean activeOnly, int limit, int offset) {
        PersistenceChecks.page(limit, offset);
        return jdbc.query("select * from enrollments where class_id = :classId and (:allRows or status = 'ACTIVE') " +
                        "order by enrolled_at, account_id limit :limit offset :offset",
                Map.of("classId", classId, "allRows", !activeOnly, "limit", limit, "offset", offset), JdbcEnrollmentRepository::map);
    }

    @Override public boolean activeInOtherClass(UUID accountId, UUID subjectId, UUID excludedClassId) {
        Boolean result = jdbc.queryForObject("""
                select exists (select 1 from enrollments e join course_classes c on c.id = e.class_id
                    where e.account_id = :accountId and e.status = 'ACTIVE' and c.status <> 'ARCHIVED'
                    and c.subject_id = :subjectId and c.id <> :classId)
                """, Map.of("accountId", accountId, "subjectId", subjectId, "classId", excludedClassId), Boolean.class);
        return Boolean.TRUE.equals(result);
    }

    @Override public List<UUID> conflictingStudents(UUID classId, UUID subjectId) {
        return jdbc.queryForList("""
                select e.account_id from enrollments e where e.class_id = :classId and e.status = 'ACTIVE'
                    and exists (select 1 from enrollments other join course_classes c on c.id = other.class_id
                        where other.account_id = e.account_id and other.status = 'ACTIVE'
                        and c.subject_id = :subjectId and c.status <> 'ARCHIVED' and c.id <> :classId)
                order by e.account_id
                """, Map.of("classId", classId, "subjectId", subjectId), UUID.class);
    }

    @Override public void insert(Enrollment value) {
        PersistenceChecks.writableTransaction();
        try {
            jdbc.update("""
                    insert into enrollments (class_id, account_id, status, source, enrolled_at, removed_at)
                    values (:classId, :accountId, :status, :source, :enrolledAt, :removedAt)
                    """, params(value));
        } catch (DuplicateKeyException ex) {
            throw new AcademicRuleException("ENROLLMENT_EXISTS", "Ghi danh đã tồn tại; tải lại dữ liệu trước khi thêm lại");
        }
    }

    @Override public void update(Enrollment value, EnrollmentStatus expectedStatus) {
        PersistenceChecks.writableTransaction();
        var args = params(value);
        args.put("expectedStatus", expectedStatus.name());
        int count = jdbc.update("""
                update enrollments set status = :status, source = :source, enrolled_at = :enrolledAt, removed_at = :removedAt
                where class_id = :classId and account_id = :accountId and status = :expectedStatus
                """, args);
        if (count != 1) throw new AcademicRuleException("ENROLLMENT_CONFLICT", "Ghi danh đã thay đổi. Hãy tải lại");
    }

    private static Map<String, Object> params(Enrollment value) {
        var args = new HashMap<String, Object>();
        args.put("classId", value.classId()); args.put("accountId", value.accountId());
        args.put("status", value.status().name()); args.put("source", value.source().name());
        args.put("enrolledAt", Timestamp.from(value.enrolledAt()));
        args.put("removedAt", value.removedAt() == null ? null : Timestamp.from(value.removedAt()));
        return args;
    }

    private static Enrollment map(ResultSet rs, int row) throws SQLException {
        Timestamp removed = rs.getTimestamp("removed_at");
        return new Enrollment(rs.getObject("class_id", UUID.class), rs.getObject("account_id", UUID.class),
                EnrollmentStatus.valueOf(rs.getString("status")), EnrollmentSource.valueOf(rs.getString("source")),
                rs.getTimestamp("enrolled_at").toInstant(), removed == null ? null : removed.toInstant());
    }
}
