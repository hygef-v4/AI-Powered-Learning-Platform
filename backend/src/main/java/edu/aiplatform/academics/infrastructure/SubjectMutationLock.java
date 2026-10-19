package edu.aiplatform.academics.infrastructure;

import edu.aiplatform.academics.domain.ISubjectMutationLock;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/** Mọi command ghi danh/vòng đời lớp phải lấy khóa này trước khi đọc kiểm tra và khóa hàng. */
@Repository
public class SubjectMutationLock implements ISubjectMutationLock {
    private final NamedParameterJdbcTemplate jdbc;
    public SubjectMutationLock(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override public void acquire(UUID subjectId) {
        PersistenceChecks.writableTransaction();
        Objects.requireNonNull(subjectId);
        // SET LOCAL: chỉ ảnh hưởng transaction hiện tại, không rò sang request dùng lại connection.
        jdbc.getJdbcTemplate().execute("set local lock_timeout = '3s'");
        // Collision chỉ làm hai môn chờ chung khóa; không làm mất tính loại trừ.
        jdbc.query("select pg_advisory_xact_lock(hashtextextended(:key, 0))",
                Map.of("key", "u05:subject:" + subjectId), (ResultSetExtractor<Void>) rs -> null);
    }
}
