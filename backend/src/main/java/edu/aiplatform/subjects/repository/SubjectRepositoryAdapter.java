package edu.aiplatform.subjects.repository;

import edu.aiplatform.shared.error.ApiException;
import edu.aiplatform.subjects.contract.SubjectStatus;
import edu.aiplatform.subjects.model.Subject;
import edu.aiplatform.subjects.model.SubjectCode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Chỉ đọc/ghi bảng subjects, trả domain snapshot bất biến và không cache scope. Cột version lưu
 * public version − 1 (môn mới lưu 0).
 */
@Repository
@Transactional(readOnly = true)
public class SubjectRepositoryAdapter implements ISubjectRepository {

    private static final String COLUMNS =
            "id,code,name,description,status,manager_id,created_at,updated_at,version";
    // Giữ đúng mệnh đề Hibernate 6 sinh cho PESSIMISTIC_WRITE trên PostgreSQL: không chặn
    // FOR KEY SHARE của insert bảng con có FK tới subjects.
    private static final String SELECT_FOR_UPDATE =
            "select " + COLUMNS + " from subjects where id = :id for no key update";
    private static final String SEARCH_FILTER =
            " where (cast(:status as varchar) is null or status = cast(:status as varchar))"
                    + " and (lower(code) like :pattern escape '!'"
                    + " or lower(name) like :pattern escape '!')";

    private final JdbcClient jdbcClient;

    public SubjectRepositoryAdapter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public Optional<Subject> findById(UUID subjectId) {
        return jdbcClient
                .sql("select " + COLUMNS + " from subjects where id = :id")
                .param("id", subjectId)
                .query(SubjectRepositoryAdapter::mapSubject)
                .optional();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<Subject> findByIdForUpdate(UUID subjectId) {
        return lockSubject(subjectId);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<Subject> findByIdForUpdate(UUID subjectId, java.time.Duration timeout) {
        if (timeout == null
                || timeout.isNegative()
                || timeout.isZero()
                || timeout.compareTo(java.time.Duration.ofSeconds(1)) > 0) {
            throw new IllegalArgumentException("Lock timeout phải trong (0,1s]");
        }
        long started = System.nanoTime();
        Map<String, Object> previous =
                jdbcClient
                        .sql(
                                """
                select current_setting('transaction_isolation') as isolation,
                       current_setting('lock_timeout') as lock_timeout,
                       current_setting('statement_timeout') as statement_timeout,
                       (extract(epoch from current_setting('lock_timeout')::interval) * 1000)::bigint
                           as lock_millis,
                       (extract(epoch from current_setting('statement_timeout')::interval) * 1000)::bigint
                           as statement_millis
                """)
                        // Không đọc view pg_settings: view dựng mọi tham số (~4 ms mỗi lần), ghi danh 200 dòng
                        // khóa môn 200 lần (TC-PERF-013).
                        .query()
                        .singleRow();
        if (!"read committed".equals(previous.get("isolation"))) {
            throw ApiException.createUnavailable(
                    "SUBJECT_ISOLATION_UNSUPPORTED", "Thao tác môn cần transaction READ_COMMITTED");
        }
        long millis =
                timeout.toMillis()
                        - java.time.Duration.ofNanos(System.nanoTime() - started).toMillis();
        if (millis <= 0) {
            throw ApiException.createUnavailable(
                    "SUBJECT_LIFECYCLE_TIMEOUT", "Kiểm tra môn quá thời gian, vui lòng thử lại");
        }
        long oldLock = Long.parseLong(previous.get("lock_millis").toString()),
                oldStatement = Long.parseLong(previous.get("statement_millis").toString());
        setTimeouts(
                (oldLock > 0 ? Math.min(oldLock, millis) : millis) + "ms",
                (oldStatement > 0 ? Math.min(oldStatement, millis) : millis) + "ms");
        // Khi query lỗi PostgreSQL transaction có thể đã aborted; không chạy SQL restore che lỗi
        // gốc.
        Optional<Subject> result = lockSubject(subjectId);
        setTimeouts(previous.get("lock_timeout").toString(),
                previous.get("statement_timeout").toString());
        return result;
    }

    private Optional<Subject> lockSubject(UUID subjectId) {
        return jdbcClient
                .sql(SELECT_FOR_UPDATE)
                .param("id", subjectId)
                .query(SubjectRepositoryAdapter::mapSubject)
                .optional();
    }

    private void setTimeouts(String lock, String statement) {
        jdbcClient
                .sql(
                        "select set_config('lock_timeout', :lock, true), set_config('statement_timeout', :statement, true)")
                .param("lock", lock)
                .param("statement", statement)
                .query()
                .singleRow();
    }

    @Override
    public boolean hasSubjectWithCode(SubjectCode code) {
        return jdbcClient
                .sql("select exists (select 1 from subjects where code = :code)")
                .param("code", code.value())
                .query(Boolean.class)
                .single();
    }

    @Override
    public boolean isManagedBy(UUID accountId, UUID subjectId) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(subjectId, "subjectId");
        return jdbcClient
                .sql("select exists (select 1 from subjects "
                        + "where id = :subjectId and manager_id = :accountId)")
                .param("subjectId", subjectId)
                .param("accountId", accountId)
                .query(Boolean.class)
                .single();
    }

    @Override
    public List<Subject> findSubjectPage(String query, SubjectStatus status, int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw ApiException.createBadRequest(
                    "SUBJECT_PAGE_INVALID", "Trang phải từ 0, số dòng mỗi trang từ 1 đến 100");
        }
        return jdbcClient
                .sql("select " + COLUMNS + " from subjects" + SEARCH_FILTER
                        + " order by code asc offset :offset limit :size")
                .param("pattern", buildLikePattern(query))
                .param("status", status == null ? null : status.name())
                .param("offset", (long) page * size)
                .param("size", size)
                .query(SubjectRepositoryAdapter::mapSubject)
                .list();
    }

    @Override
    public long countSubjects(String query, SubjectStatus status) {
        return jdbcClient
                .sql("select count(*) from subjects" + SEARCH_FILTER)
                .param("pattern", buildLikePattern(query))
                .param("status", status == null ? null : status.name())
                .query(Long.class)
                .single();
    }

    @Override
    public Map<SubjectStatus, Long> countSubjectsByStatus(String query) {
        Map<SubjectStatus, Long> counts = new EnumMap<>(SubjectStatus.class);
        for (SubjectStatus status : SubjectStatus.values()) {
            counts.put(status, countSubjects(query, status));
        }
        return Map.copyOf(counts);
    }

    @Override
    public List<Subject> findSubjectsManagedBy(UUID accountId) {
        Objects.requireNonNull(accountId, "accountId");
        return jdbcClient
                .sql("select " + COLUMNS + " from subjects where manager_id = :accountId "
                        + "order by code asc")
                .param("accountId", accountId)
                .query(SubjectRepositoryAdapter::mapSubject)
                .list();
    }

    @Override
    @Transactional
    public void insertSubject(Subject subject) {
        if (subject.version() != 1) {
            throw new IllegalArgumentException("Môn mới phải có public version 1");
        }
        jdbcClient
                .sql("insert into subjects(" + COLUMNS + ") values (:id,:code,:name,:description,"
                        + ":status,:managerId,:createdAt,:updatedAt,0)")
                .param("id", subject.id())
                .param("code", subject.code().value())
                .param("name", subject.name())
                .param("description", subject.description())
                .param("status", subject.status().name())
                .param("managerId", subject.managerId())
                .param("createdAt", Timestamp.from(subject.createdAt()))
                .param("updatedAt", Timestamp.from(subject.updatedAt()))
                .update();
    }

    /** Compare-and-set nguyên tử: không ghi đè snapshot cũ, không cập nhật code/created_at. */
    @Override
    @Transactional
    public boolean updateSubject(Subject subject, long expectedVersion) {
        if (expectedVersion < 1 || subject.version() != Math.addExact(expectedVersion, 1)) {
            throw new IllegalArgumentException(
                    "Snapshot cập nhật phải có public version bằng expectedVersion + 1");
        }
        return jdbcClient
                        .sql("update subjects set name = :name, description = :description, "
                                + "status = :status, manager_id = :managerId, "
                                + "updated_at = :updatedAt, version = :nextVersion "
                                + "where id = :id and version = :expectedVersion and code = :code")
                        .param("name", subject.name())
                        .param("description", subject.description())
                        .param("status", subject.status().name())
                        .param("managerId", subject.managerId())
                        .param("updatedAt", Timestamp.from(subject.updatedAt()))
                        .param("nextVersion", subject.version() - 1)
                        .param("id", subject.id())
                        .param("expectedVersion", expectedVersion - 1)
                        .param("code", subject.code().value())
                        .update()
                == 1;
    }

    private static Subject mapSubject(ResultSet row, int rowNumber) throws SQLException {
        return new Subject(
                row.getObject("id", UUID.class),
                new SubjectCode(row.getString("code")),
                row.getString("name"),
                row.getString("description"),
                SubjectStatus.valueOf(row.getString("status")),
                row.getObject("manager_id", UUID.class),
                row.getTimestamp("created_at").toInstant(),
                row.getTimestamp("updated_at").toInstant(),
                Math.addExact(row.getLong("version"), 1));
    }

    private static String buildLikePattern(String query) {
        String literal = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        return "%" + literal.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
    }
}
