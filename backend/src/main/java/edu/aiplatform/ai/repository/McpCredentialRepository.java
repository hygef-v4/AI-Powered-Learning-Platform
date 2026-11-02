package edu.aiplatform.ai.repository;

import edu.aiplatform.ai.model.AiConnection;
import edu.aiplatform.ai.model.McpCredential;

import edu.aiplatform.ai.model.AiConnection.EncryptedKey;
import edu.aiplatform.ai.model.McpCredential.Access;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Bảng {@code mcp_credentials}; chỉ dùng trong U06. Tìm theo băm token, không lưu token rõ; bản mã của token chỉ để
 * chủ agent xem lại.
 */
@Repository
public class McpCredentialRepository {

    private static final String COLUMNS = "id, account_id, label, access, token_hash, token_hint, expires_at, revoked_at, "
            + "last_used_at, created_at";

    private final JdbcClient jdbc;

    public McpCredentialRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public void insert(McpCredential c) {
        jdbc.sql("insert into mcp_credentials (" + COLUMNS + ") values (:id, :accountId, :label, :access, :hash, :hint, "
                        + ":expiresAt, null, null, :createdAt)")
                .param("id", c.id()).param("accountId", c.accountId()).param("label", c.label())
                .param("access", c.access().name()).param("hash", c.tokenHash()).param("hint", c.tokenHint())
                .param("expiresAt", Timestamp.from(c.expiresAt())).param("createdAt", Timestamp.from(c.createdAt()))
                .update();
    }

    public List<McpCredential> listOwn(UUID accountId) {
        return jdbc.sql("select " + COLUMNS + " from mcp_credentials where account_id = :id order by created_at desc")
                .param("id", accountId).query(McpCredentialRepository::map).list();
    }

    public int countActive(UUID accountId, Instant now) {
        return jdbc.sql("select count(*) from mcp_credentials where account_id = :id and revoked_at is null "
                        + "and expires_at > :now")
                .param("id", accountId).param("now", Timestamp.from(now)).query(Integer.class).single();
    }

    public Optional<McpCredential> findOwn(UUID accountId, UUID id) {
        return jdbc.sql("select " + COLUMNS + " from mcp_credentials where id = :id and account_id = :accountId")
                .param("id", id).param("accountId", accountId).query(McpCredentialRepository::map).optional();
    }

    public Optional<McpCredential> findByHash(String hash) {
        return jdbc.sql("select " + COLUMNS + " from mcp_credentials where token_hash = :hash").param("hash", hash)
                .query(McpCredentialRepository::map).optional();
    }

    public int revoke(UUID id, Instant now) {
        return jdbc.sql("update mcp_credentials set revoked_at = :now where id = :id and revoked_at is null")
                .param("id", id).param("now", Timestamp.from(now)).update();
    }

    /** Lưu bản mã của token để chủ agent xem lại; xác thực vẫn chỉ dùng {@code token_hash}. */
    public void saveSecret(UUID id, EncryptedKey secret) {
        jdbc.sql("update mcp_credentials set token_ciphertext = :cipher, token_iv = :iv, token_key_version = :version "
                        + "where id = :id")
                .param("id", id).param("cipher", secret.ciphertext()).param("iv", secret.iv())
                .param("version", secret.version()).update();
    }

    public Optional<EncryptedKey> findSecret(UUID id) {
        return jdbc.sql("select token_ciphertext, token_iv, token_key_version from mcp_credentials where id = :id "
                        + "and token_ciphertext is not null")
                .param("id", id)
                .query((rs, n) -> new EncryptedKey(rs.getBytes("token_ciphertext"), rs.getBytes("token_iv"),
                        rs.getInt("token_key_version")))
                .optional();
    }

    /** Agent của một người có bản mã token (xem lại được). */
    public Set<UUID> revealableIds(UUID accountId) {
        return Set.copyOf(jdbc.sql("select id from mcp_credentials where account_id = :id and token_ciphertext is not null")
                .param("id", accountId).query(UUID.class).list());
    }

    public int update(UUID id, String label, Access access) {
        return jdbc.sql("update mcp_credentials set label = :label, access = :access where id = :id and revoked_at is null")
                .param("id", id).param("label", label).param("access", access.name()).update();
    }

    public void touch(UUID id, Instant now) {
        jdbc.sql("update mcp_credentials set last_used_at = :now where id = :id").param("id", id)
                .param("now", Timestamp.from(now)).update();
    }

    private static McpCredential map(ResultSet rs, int n) throws SQLException {
        Timestamp revoked = rs.getTimestamp("revoked_at");
        Timestamp used = rs.getTimestamp("last_used_at");
        return new McpCredential(rs.getObject("id", UUID.class), rs.getObject("account_id", UUID.class),
                rs.getString("label"), Access.valueOf(rs.getString("access")), rs.getString("token_hash"),
                rs.getString("token_hint"), rs.getTimestamp("expires_at").toInstant(),
                revoked == null ? null : revoked.toInstant(), used == null ? null : used.toInstant(),
                rs.getTimestamp("created_at").toInstant());
    }
}
