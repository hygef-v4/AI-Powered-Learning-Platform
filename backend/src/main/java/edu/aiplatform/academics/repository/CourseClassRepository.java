package edu.aiplatform.academics.repository;

import edu.aiplatform.academics.contract.ClassStatus;
import edu.aiplatform.academics.model.AcademicRuleException;
import edu.aiplatform.academics.model.CourseClass;
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
public class CourseClassRepository implements ICourseClassRepository {
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    /** Creates the class repository with its named JDBC template. */
    public CourseClassRepository(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    /** Finds a course class by its unique ID. */
    @Override
    public Optional<CourseClass> find(UUID classId) {
        return namedParameterJdbcTemplate.query("select * from course_classes where id = :id",
                Map.of("id", classId), CourseClassRepository::mapCourseClass).stream()
                .findFirst();
    }

    /** Lists paginated course classes belonging to a subject. */
    @Override
    public List<CourseClass> listBySubject(UUID subjectId, int limit, int offset) {
        PersistenceChecks.validatePage(limit, offset);
        return namedParameterJdbcTemplate.query(
                "select * from course_classes where subject_id = :subjectId order by code, id limit :limit offset :offset",
                Map.of("subjectId", subjectId, "limit", limit, "offset", offset),
                CourseClassRepository::mapCourseClass);
    }

    /** Inserts a new course class record with initial version 0. */
    @Override
    public void insert(CourseClass value) {
        PersistenceChecks.requireWritableTransaction();
        if (value.version() != 0)
            throw new IllegalArgumentException("New class version must be zero");
        try {
            namedParameterJdbcTemplate.update(
                    """
                            insert into course_classes (id, subject_id, code, name, description, status, teacher_id,
                                show_grade_distribution, created_at, updated_at, version)
                            values (:id, :subjectId, :code, :name, :description, :status, :teacherId,
                                :distribution, :createdAt, :updatedAt, :version)
                            """,
                    params(value));
        } catch (DuplicateKeyException ex) {
            throw new AcademicRuleException("CLASS_EXISTS",
                    "Mã lớp trong môn hoặc ID lớp đã tồn tại");
        }
    }

    /** Updates course class fields checking for version concurrency conflict. */
    @Override
    public void update(CourseClass value, long expectedVersion) {
        PersistenceChecks.requireWritableTransaction();
        if (expectedVersion < 0 || expectedVersion == Long.MAX_VALUE
                || value.version() != expectedVersion + 1) {
            throw new IllegalArgumentException(
                    "Updated class must advance expected version by one");
        }
        var args = params(value);
        args.put("expectedVersion", expectedVersion);
        int count = namedParameterJdbcTemplate.update(
                """
                        update course_classes set name = :name, description = :description,
                            status = :status, teacher_id = :teacherId, show_grade_distribution = :distribution,
                            updated_at = :updatedAt, version = :version
                        where id = :id and version = :expectedVersion
                        """,
                args);
        if (count != 1)
            throw new AcademicRuleException("CLASS_VERSION_CONFLICT",
                    "Dữ liệu lớp đã thay đổi. Hãy tải lại");
    }

    /** Builds the named SQL parameters for the supplied record. */
    private static Map<String, Object> params(CourseClass value) {
        var args = new HashMap<String, Object>();
        args.put("id", value.id());
        args.put("subjectId", value.subjectId());
        args.put("code", value.code());
        args.put("name", value.name());
        args.put("description", value.description());
        args.put("status", value.status().name());
        args.put("teacherId", value.teacherId());
        args.put("distribution", value.showGradeDistribution());
        args.put("version", value.version());
        args.put("createdAt", Timestamp.from(value.createdAt()));
        args.put("updatedAt", Timestamp.from(value.updatedAt()));
        return args;
    }

    /** Maps a database row to a course class. */
    static CourseClass mapCourseClass(ResultSet rs, int row) throws SQLException {
        return new CourseClass(rs.getObject("id", UUID.class),
                rs.getObject("subject_id", UUID.class), rs.getString("code"), rs.getString("name"),
                rs.getString("description"),
                ClassStatus.valueOf(rs.getString("status")), rs.getObject("teacher_id", UUID.class),
                rs.getBoolean("show_grade_distribution"), rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant(), rs.getLong("version"));
    }
}
