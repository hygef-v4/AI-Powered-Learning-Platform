package edu.aiplatform.ai.repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Bảng {@code mcp_write_requests}: kết quả của lời gọi ghi MCP theo requestRef, để agent gửi lại không ghi trùng. */
@Repository
public class McpWriteRequestRepository {

    private final JdbcClient jdbc;

    public McpWriteRequestRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Khóa theo khóa yêu cầu đến hết transaction, để các lần gửi lại chạy nối tiếp nhau. */
    public void lockRequest(String requestKey) {
        jdbc.sql("select pg_advisory_xact_lock(hashtextextended(:key, 0))").param("key", requestKey)
                .query((rs, n) -> 1).list();
    }

    public Optional<StoredRequest> findRequest(UUID accountId, String operation, String requestRef) {
        return jdbc.sql("select fingerprint, response::text as response from mcp_write_requests "
                        + "where account_id = :accountId and operation = :operation and request_ref = :requestRef")
                .param("accountId", accountId).param("operation", operation).param("requestRef", requestRef)
                .query((rs, n) -> new StoredRequest(rs.getString("fingerprint"), rs.getString("response")))
                .optional();
    }

    public void insertRequest(UUID accountId, String operation, String requestRef, String fingerprint,
                              String response) {
        jdbc.sql("insert into mcp_write_requests (account_id, operation, request_ref, fingerprint, response) "
                        + "values (:accountId, :operation, :requestRef, :fingerprint, cast(:response as jsonb))")
                .param("accountId", accountId).param("operation", operation).param("requestRef", requestRef)
                .param("fingerprint", fingerprint).param("response", response)
                .update();
    }

    public int deleteRequestsOlderThan(Instant cutoff) {
        return jdbc.sql("delete from mcp_write_requests where created_at < :cutoff")
                .param("cutoff", Timestamp.from(cutoff))
                .update();
    }

    public record StoredRequest(String fingerprint, String response) {
    }
}
