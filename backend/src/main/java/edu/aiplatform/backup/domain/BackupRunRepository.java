package edu.aiplatform.backup.domain;

import edu.aiplatform.backup.contract.BackupState;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Bảng {@code backup_runs}; mọi chuyển trạng thái là cập nhật có điều kiện. */
@Repository
public class BackupRunRepository {

    private final JdbcClient jdbc;

    public BackupRunRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Tạo lần chạy cho ngày; ngày đã có thì không tạo (B1, khóa duy nhất {@code scheduled_for}). */
    public Optional<UUID> createIfAbsent(LocalDate scheduledFor, Instant now) {
        UUID id = UUID.randomUUID();
        int inserted = jdbc.sql("insert into backup_runs (id, scheduled_for, state, created_at) "
                        + "values (:id, :date, 'CREATED', :now) on conflict (scheduled_for) do nothing")
                .param("id", id).param("date", scheduledFor).param("now", ts(now)).update();
        return inserted == 1 ? Optional.of(id) : Optional.empty();
    }

    public Optional<BackupRun> find(UUID id) {
        return jdbc.sql("select * from backup_runs where id = :id").param("id", id)
                .query(BackupRunRepository::map).optional();
    }

    /** {@code CREATED} (hoặc {@code RUNNING} khi thử lại) → {@code RUNNING}; trả 0 nếu lần chạy đã kết thúc. */
    public int markRunning(UUID id, Instant now) {
        return jdbc.sql("update backup_runs set state = 'RUNNING', started_at = :now "
                        + "where id = :id and state in ('CREATED', 'RUNNING')")
                .param("id", id).param("now", ts(now)).update();
    }

    public int markSuccess(UUID id, Instant snapshotAt, String schemaVersion, String appVersion, String archiveRef,
                           String sha256, long byteSize, Instant now) {
        return jdbc.sql("update backup_runs set state = 'SUCCESS', snapshot_at = :snapshot, schema_version = :schema, "
                        + "app_version = :app, archive_ref = :ref, archive_sha256 = :sha, byte_size = :size, "
                        + "completed_at = :now, safe_error = null where id = :id and state = 'RUNNING'")
                .param("id", id).param("snapshot", ts(snapshotAt)).param("schema", schemaVersion)
                .param("app", appVersion).param("ref", archiveRef).param("sha", sha256).param("size", byteSize)
                .param("now", ts(now)).update();
    }

    public int markFailed(UUID id, String safeError, Instant now) {
        return jdbc.sql("update backup_runs set state = 'FAILED', safe_error = :error, completed_at = :now "
                        + "where id = :id and state <> 'SUCCESS'")
                .param("id", id).param("error", safeError).param("now", ts(now)).update();
    }

    public List<BackupRun> findCreatedBefore(Instant olderThan) {
        return jdbc.sql("select * from backup_runs where state = 'CREATED' and created_at < :t")
                .param("t", ts(olderThan)).query(BackupRunRepository::map).list();
    }

    /** Bản {@code SUCCESS} chụp trước {@code cutoff}, chưa xóa, trừ bản thành công mới nhất (BR-U03-96). */
    public List<BackupRun> findPurgeCandidates(Instant cutoff) {
        return jdbc.sql("select * from backup_runs where state = 'SUCCESS' and purged_at is null "
                        + "and snapshot_at < :cutoff and id <> (select id from backup_runs where state = 'SUCCESS' "
                        + "order by snapshot_at desc limit 1)")
                .param("cutoff", ts(cutoff)).query(BackupRunRepository::map).list();
    }

    public int markPurged(UUID id, Instant now) {
        return jdbc.sql("update backup_runs set purged_at = :now where id = :id and purged_at is null")
                .param("id", id).param("now", ts(now)).update();
    }

    public Optional<BackupRun> latest() {
        return jdbc.sql("select * from backup_runs order by scheduled_for desc limit 1")
                .query(BackupRunRepository::map).optional();
    }

    public Page<BackupRun> page(Pageable page) {
        List<BackupRun> items = jdbc.sql("select * from backup_runs order by scheduled_for desc limit :limit offset :offset")
                .param("limit", page.getPageSize()).param("offset", page.getOffset())
                .query(BackupRunRepository::map).list();
        long total = jdbc.sql("select count(*) from backup_runs").query(Long.class).single();
        return new PageImpl<>(items, page, total);
    }

    public long databaseSize() {
        return jdbc.sql("select pg_database_size(current_database())").query(Long.class).single();
    }

    /** Phiên bản schema Flyway mới nhất đã chạy thành công. */
    public String schemaVersion() {
        try {
            return jdbc.sql("select version from flyway_schema_history where success order by installed_rank desc "
                    + "limit 1").query(String.class).optional().orElse("none");
        } catch (DataAccessException e) {
            return "unknown";
        }
    }

    private static Timestamp ts(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }

    static BackupRun map(ResultSet rs, int row) throws SQLException {
        Long size = rs.getObject("byte_size", Long.class);
        return new BackupRun(rs.getObject("id", UUID.class), rs.getObject("scheduled_for", LocalDate.class),
                BackupState.valueOf(rs.getString("state")), instant(rs, "created_at"), instant(rs, "snapshot_at"),
                rs.getString("schema_version"), rs.getString("app_version"), rs.getString("archive_ref"),
                rs.getString("archive_sha256"), size, instant(rs, "started_at"),
                instant(rs, "completed_at"), instant(rs, "purged_at"), rs.getString("safe_error"));
    }
}
