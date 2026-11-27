package edu.aiplatform.backup.repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Bảng {@code wal_segments}: tệp WAL đã tải lên Drive (U03, 2026-11-26). */
@Repository
public class WalSegmentRepository {

    public record WalSegment(String fileName, String archiveRef) {
    }

    private final JdbcClient jdbc;

    public WalSegmentRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Ghi tệp vừa tải lên; tệp đã có (tải lại sau lỗi) thì cập nhật chỗ lưu mới. */
    public void recordUploaded(String fileName, String archiveRef, long byteSize, Instant uploadedAt) {
        jdbc.sql("insert into wal_segments (file_name, archive_ref, byte_size, uploaded_at) "
                        + "values (:name, :ref, :size, :at) on conflict (file_name) do update set "
                        + "archive_ref = excluded.archive_ref, byte_size = excluded.byte_size, "
                        + "uploaded_at = excluded.uploaded_at, purged_at = null")
                .param("name", fileName).param("ref", archiveRef).param("size", byteSize)
                .param("at", Timestamp.from(uploadedAt)).update();
    }

    public Optional<Instant> findLatestUploadedAt() {
        return jdbc.sql("select max(uploaded_at) from wal_segments").query(Timestamp.class).optional()
                .map(Timestamp::toInstant);
    }

    /** Tệp chưa dọn tải lên trước {@code cutoff}. */
    public List<WalSegment> findPurgeCandidates(Instant cutoff) {
        return jdbc.sql("select file_name, archive_ref from wal_segments where purged_at is null "
                        + "and uploaded_at < :cutoff order by file_name")
                .param("cutoff", Timestamp.from(cutoff))
                .query((rs, row) -> new WalSegment(rs.getString("file_name"), rs.getString("archive_ref"))).list();
    }

    public void markPurged(String fileName, Instant now) {
        jdbc.sql("update wal_segments set purged_at = :now where file_name = :name and purged_at is null")
                .param("name", fileName).param("now", Timestamp.from(now)).update();
    }

    /** Khóa theo transaction để chỉ một worker đẩy WAL tại một thời điểm. */
    public boolean tryLockShipping() {
        return Boolean.TRUE.equals(jdbc.sql("select pg_try_advisory_xact_lock(hashtext('u03-wal-ship'))")
                .query(Boolean.class).single());
    }
}
