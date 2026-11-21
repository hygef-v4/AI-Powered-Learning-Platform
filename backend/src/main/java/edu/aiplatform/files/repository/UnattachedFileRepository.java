package edu.aiplatform.files.repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class UnattachedFileRepository {

    private final JdbcClient jdbcClient;

    public UnattachedFileRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public void saveFile(String fileId, Instant cleanupAt) {
        jdbcClient.sql("""
                insert into unattached_files (file_id, cleanup_at) values (:fileId, :cleanupAt)
                on conflict (file_id) do update set cleanup_at = excluded.cleanup_at
                """)
                .param("fileId", fileId)
                .param("cleanupAt", Timestamp.from(cleanupAt))
                .update();
    }

    public boolean removeFile(String fileId) {
        return jdbcClient.sql("delete from unattached_files where file_id = :fileId")
                .param("fileId", fileId)
                .update() != 0;
    }

    public List<String> listDueFiles(Instant now, int limit) {
        return jdbcClient.sql("""
                select file_id from unattached_files where cleanup_at <= :now
                order by cleanup_at, file_id limit :limit
                """)
                .param("now", Timestamp.from(now))
                .param("limit", limit)
                .query(String.class)
                .list();
    }
}
