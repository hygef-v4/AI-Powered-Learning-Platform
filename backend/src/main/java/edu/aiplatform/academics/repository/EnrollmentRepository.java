package edu.aiplatform.academics.repository;

import edu.aiplatform.academics.model.*;
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
public class EnrollmentRepository implements IEnrollmentRepository {
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public EnrollmentRepository(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    /** Finds an enrollment record by class ID and account ID. */
    @Override
    public Optional<Enrollment> find(UUID classId, UUID accountId) {
        return namedParameterJdbcTemplate.query(
                "select * from enrollments where class_id = :classId and account_id = :accountId",
                Map.of("classId", classId, "accountId", accountId),
                EnrollmentRepository::mapEnrollment).stream().findFirst();
    }

    /** Lists paginated enrollments for a class, optionally filtering for active students only. */
    @Override
    public List<Enrollment> listByClass(UUID classId, boolean activeOnly, int limit, int offset) {
        PersistenceChecks.validatePage(limit, offset);
        return namedParameterJdbcTemplate.query(
                "select * from enrollments where class_id = :classId and (:allRows or status = 'ACTIVE') "
                        + "order by enrolled_at, account_id limit :limit offset :offset",
                Map.of("classId", classId, "allRows", !activeOnly, "limit", limit, "offset",
                        offset),
                EnrollmentRepository::mapEnrollment);
    }

    /** Checks if a student is already actively enrolled in another open class of the same subject. */
    @Override
    public boolean activeInOtherClass(UUID accountId, UUID subjectId, UUID excludedClassId) {
        Boolean result = namedParameterJdbcTemplate.queryForObject(
                """
                        select exists (select 1 from enrollments e join course_classes c on c.id = e.class_id
                            where e.account_id = :accountId and e.status = 'ACTIVE' and c.status <> 'ARCHIVED'
                            and c.subject_id = :subjectId and c.id <> :classId)
                        """,
                Map.of("accountId", accountId, "subjectId", subjectId, "classId", excludedClassId),
                Boolean.class);
        return Boolean.TRUE.equals(result);
    }

    /** Finds student IDs in this class who are concurrently active in another class of the same subject. */
    @Override
    public List<UUID> conflictingStudents(UUID classId, UUID subjectId) {
        return namedParameterJdbcTemplate.queryForList(
                """
                        select e.account_id from enrollments e where e.class_id = :classId and e.status = 'ACTIVE'
                            and exists (select 1 from enrollments other join course_classes c on c.id = other.class_id
                                where other.account_id = e.account_id and other.status = 'ACTIVE'
                                and c.subject_id = :subjectId and c.status <> 'ARCHIVED' and c.id <> :classId)
                        order by e.account_id
                        """,
                Map.of("classId", classId, "subjectId", subjectId), UUID.class);
    }

    /** Inserts a new student enrollment record into the database. */
    @Override
    public void insert(Enrollment value) {
        PersistenceChecks.requireWritableTransaction();
        try {
            namedParameterJdbcTemplate.update(
                    """
                            insert into enrollments (class_id, account_id, status, source, enrolled_at, removed_at)
                            values (:classId, :accountId, :status, :source, :enrolledAt, :removedAt)
                            """,
                    params(value));
        } catch (DuplicateKeyException ex) {
            throw new AcademicRuleException("ENROLLMENT_EXISTS",
                    "Ghi danh đã tồn tại; tải lại dữ liệu trước khi thêm lại");
        }
    }

    /** Updates an enrollment record checking expected previous status for concurrency safety. */
    @Override
    public void update(Enrollment value, EnrollmentStatus expectedStatus) {
        PersistenceChecks.requireWritableTransaction();
        var args = params(value);
        args.put("expectedStatus", expectedStatus.name());
        int count = namedParameterJdbcTemplate.update(
                """
                        update enrollments set status = :status, source = :source, enrolled_at = :enrolledAt, removed_at = :removedAt
                        where class_id = :classId and account_id = :accountId and status = :expectedStatus
                        """,
                args);
        if (count != 1)
            throw new AcademicRuleException("ENROLLMENT_CONFLICT",
                    "Ghi danh đã thay đổi. Hãy tải lại");
    }

    private static Map<String, Object> params(Enrollment value) {
        var args = new HashMap<String, Object>();
        args.put("classId", value.classId());
        args.put("accountId", value.accountId());
        args.put("status", value.status().name());
        args.put("source", value.source().name());
        args.put("enrolledAt", Timestamp.from(value.enrolledAt()));
        args.put("removedAt", value.removedAt() == null ? null : Timestamp.from(value.removedAt()));
        return args;
    }

    private static Enrollment mapEnrollment(ResultSet rs, int row) throws SQLException {
        Timestamp removed = rs.getTimestamp("removed_at");
        return new Enrollment(rs.getObject("class_id", UUID.class),
                rs.getObject("account_id", UUID.class),
                EnrollmentStatus.valueOf(rs.getString("status")),
                EnrollmentSource.valueOf(rs.getString("source")),
                rs.getTimestamp("enrolled_at").toInstant(),
                removed == null ? null : removed.toInstant());
    }
}
