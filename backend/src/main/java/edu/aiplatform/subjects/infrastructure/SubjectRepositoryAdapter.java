package edu.aiplatform.subjects.infrastructure;

import edu.aiplatform.shared.error.ApiException;
import edu.aiplatform.subjects.contract.SubjectStatus;
import edu.aiplatform.subjects.domain.ISubjectRepository;
import edu.aiplatform.subjects.domain.Subject;
import edu.aiplatform.subjects.domain.SubjectCode;
import jakarta.persistence.EntityManager;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

/** Chỉ đọc/ghi bảng subjects, trả domain snapshot bất biến và không cache scope. */
@Repository
@Transactional(readOnly = true)
public class SubjectRepositoryAdapter implements ISubjectRepository {

    private final ISubjectJpaRepository rows;
    private final EntityManager entityManager;

    public SubjectRepositoryAdapter(ISubjectJpaRepository rows, EntityManager entityManager) {
        this.rows = rows;
        this.entityManager = entityManager;
    }

    @Override
    public Optional<Subject> findById(UUID subjectId) {
        return rows.findById(subjectId).map(this::snapshot);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<Subject> findByIdForUpdate(UUID subjectId) {
        return rows.findByIdForUpdate(subjectId).map(this::snapshot);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<Subject> findByIdForUpdate(UUID subjectId, java.time.Duration timeout) {
        if (timeout == null || timeout.isNegative() || timeout.isZero() || timeout.compareTo(java.time.Duration.ofSeconds(1)) > 0) {
            throw new IllegalArgumentException("Lock timeout phải trong (0,1s]");
        }
        long started = System.nanoTime();
        Object[] previous = (Object[]) entityManager.createNativeQuery("""
                select current_setting('transaction_isolation'), current_setting('lock_timeout'),
                       current_setting('statement_timeout'),
                       (select setting from pg_settings where name = 'lock_timeout'),
                       (select setting from pg_settings where name = 'statement_timeout')
                """).getSingleResult();
        if (!"read committed".equals(previous[0])) {
            throw ApiException.unavailable("SUBJECT_ISOLATION_UNSUPPORTED", "Thao tác môn cần transaction READ_COMMITTED");
        }
        long millis = timeout.toMillis() - java.time.Duration.ofNanos(System.nanoTime() - started).toMillis();
        if (millis <= 0) {
            throw ApiException.unavailable("SUBJECT_LIFECYCLE_TIMEOUT", "Kiểm tra môn quá thời gian, vui lòng thử lại");
        }
        long oldLock = Long.parseLong(previous[3].toString()), oldStatement = Long.parseLong(previous[4].toString());
        setTimeouts((oldLock > 0 ? Math.min(oldLock, millis) : millis) + "ms",
                (oldStatement > 0 ? Math.min(oldStatement, millis) : millis) + "ms");
        // Khi query lỗi PostgreSQL transaction có thể đã aborted; không chạy SQL restore che lỗi gốc.
        Optional<Subject> result = rows.findByIdForUpdate(subjectId).map(this::snapshot);
        setTimeouts(previous[1].toString(), previous[2].toString());
        return result;
    }

    private void setTimeouts(String lock, String statement) {
        entityManager.createNativeQuery("select set_config('lock_timeout', :lock, true), set_config('statement_timeout', :statement, true)")
                .setParameter("lock", lock).setParameter("statement", statement).getSingleResult();
    }

    @Override
    public boolean existsByCode(SubjectCode code) {
        return rows.existsByCode(code.value());
    }

    @Override
    public boolean isManagedBy(UUID accountId, UUID subjectId) {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(subjectId, "subjectId");
        return rows.existsByIdAndManagerId(subjectId, accountId);
    }

    @Override
    public List<Subject> findPage(String query, SubjectStatus status, int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw ApiException.badRequest("SUBJECT_PAGE_INVALID", "Trang phải từ 0, số dòng mỗi trang từ 1 đến 100");
        }
        return rows.findPage(pattern(query), status, PageRequest.of(page, size, Sort.by("code")))
                .stream().map(this::snapshot).toList();
    }

    @Override
    public long count(String query, SubjectStatus status) {
        return rows.countMatching(pattern(query), status);
    }

    @Override
    public Map<SubjectStatus, Long> countByStatus(String query) {
        Map<SubjectStatus, Long> counts = new EnumMap<>(SubjectStatus.class);
        for (SubjectStatus status : SubjectStatus.values()) {
            counts.put(status, count(query, status));
        }
        return Map.copyOf(counts);
    }

    @Override
    public List<Subject> findManagedBy(UUID accountId) {
        Objects.requireNonNull(accountId, "accountId");
        return rows.findByManagerIdOrderByCodeAsc(accountId).stream().map(this::snapshot).toList();
    }

    @Override
    @Transactional
    public void insert(Subject subject) {
        SubjectJpaEntity row = SubjectJpaEntity.forInsert(subject);
        entityManager.persist(row);
        entityManager.flush();
        entityManager.detach(row);
    }

    @Override
    @Transactional
    public boolean update(Subject subject, long expectedVersion) {
        if (expectedVersion < 1 || subject.version() != Math.addExact(expectedVersion, 1)) {
            throw new IllegalArgumentException("Snapshot cập nhật phải có public version bằng expectedVersion + 1");
        }
        return rows.updateSnapshot(subject.id(), subject.code().value(), subject.name(), subject.description(),
                subject.status(), subject.managerId(), subject.updatedAt(), expectedVersion - 1, subject.version() - 1) == 1;
    }

    private Subject snapshot(SubjectJpaEntity row) {
        Subject subject = row.toDomain();
        // Bulk UPDATE phía trên không đồng bộ managed entity. Detach chỉ row này để lần đọc sau lấy DB mới,
        // không clear/detach dữ liệu unit khác trong transaction dùng chung (ví dụ disable account).
        entityManager.detach(row);
        return subject;
    }

    private static String pattern(String query) {
        String literal = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        return "%" + literal.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
    }
}
