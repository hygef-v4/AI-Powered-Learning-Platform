package edu.aiplatform.ai.repository;

import edu.aiplatform.ai.model.AiConnection;

import edu.aiplatform.ai.contract.AiProvider;
import edu.aiplatform.ai.model.AiConnection.Scope;
import edu.aiplatform.ai.model.AiConnection.Status;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Bảng {@code ai_connections}; chỉ dùng trong U06. Sửa khi {@code version} khớp (BR-U06-06). */
@Repository
public class AiConnectionRepository {

    private static final String COLUMNS = "id, scope, owner_id, provider, model, key_ciphertext, key_iv, key_version, "
            + "key_hint, status, last_test_at, last_test_result, created_at, updated_at, version";

    private final JdbcClient jdbc;

    public AiConnectionRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public void insert(AiConnection c) {
        jdbc.sql("insert into ai_connections (" + COLUMNS + ") values (:id, :scope, :ownerId, :provider, :model, :cipher, "
                        + ":iv, :keyVersion, :hint, :status, :lastTestAt, :lastTestResult, :createdAt, :updatedAt, 0)")
                .params(params(c)).update();
    }

    public boolean update(AiConnection c) {
        return jdbc.sql("update ai_connections set model = :model, key_ciphertext = :cipher, key_iv = :iv, "
                        + "key_version = :keyVersion, key_hint = :hint, status = :status, last_test_at = :lastTestAt, "
                        + "last_test_result = :lastTestResult, updated_at = :updatedAt, version = version + 1 "
                        + "where id = :id and version = :version")
                .params(params(c)).param("version", c.version()).update() == 1;
    }

    /** Ghi kết quả kiểm tra, không đổi {@code version} (kiểm tra không phải thay đổi cấu hình). */
    public void saveTest(UUID id, String result, Instant at) {
        jdbc.sql("update ai_connections set last_test_at = :at, last_test_result = :result where id = :id")
                .param("id", id).param("at", Timestamp.from(at)).param("result", result).update();
    }

    public Optional<AiConnection> findById(UUID id) {
        return jdbc.sql("select " + COLUMNS + " from ai_connections where id = :id").param("id", id)
                .query(AiConnectionRepository::map).optional();
    }

    /** Kết nối cá nhân chưa thu hồi của một người cho một provider. */
    public Optional<AiConnection> findPersonal(UUID ownerId, AiProvider provider) {
        return jdbc.sql("select " + COLUMNS + " from ai_connections where scope = 'PERSONAL' and owner_id = :ownerId "
                        + "and provider = :provider and status <> 'REVOKED'")
                .param("ownerId", ownerId).param("provider", provider.name()).query(AiConnectionRepository::map).optional();
    }

    public List<AiConnection> listPersonal(UUID ownerId) {
        return jdbc.sql("select " + COLUMNS + " from ai_connections where scope = 'PERSONAL' and owner_id = :ownerId "
                        + "and status <> 'REVOKED' order by provider")
                .param("ownerId", ownerId).query(AiConnectionRepository::map).list();
    }

    /** Khóa chung dùng cho provider: khóa đang bật tạo sớm nhất, không có thì khóa đang tắt tạo sớm nhất. */
    public Optional<AiConnection> findInstitutional(AiProvider provider) {
        return jdbc.sql("select " + COLUMNS + " from ai_connections where scope = 'INSTITUTIONAL' and provider = :provider "
                        + "and status <> 'REVOKED' order by (status = 'ACTIVE') desc, created_at, id limit 1")
                .param("provider", provider.name()).query(AiConnectionRepository::map).optional();
    }

    public List<AiConnection> listInstitutional() {
        return jdbc.sql("select " + COLUMNS + " from ai_connections where scope = 'INSTITUTIONAL' and status <> 'REVOKED' "
                        + "order by provider, created_at, id")
                .query(AiConnectionRepository::map).list();
    }

    private static Map<String, Object> params(AiConnection c) {
        Map<String, Object> p = new HashMap<>();
        p.put("id", c.id());
        p.put("scope", c.scope().name());
        p.put("ownerId", c.ownerId());
        p.put("provider", c.provider().name());
        p.put("model", c.model());
        p.put("cipher", c.keyCiphertext());
        p.put("iv", c.keyIv());
        p.put("keyVersion", c.keyVersion());
        p.put("hint", c.keyHint());
        p.put("status", c.status().name());
        p.put("lastTestAt", c.lastTestAt() == null ? null : Timestamp.from(c.lastTestAt()));
        p.put("lastTestResult", c.lastTestResult());
        p.put("createdAt", Timestamp.from(c.createdAt()));
        p.put("updatedAt", Timestamp.from(c.updatedAt()));
        return p;
    }

    private static AiConnection map(ResultSet rs, int n) throws SQLException {
        Timestamp tested = rs.getTimestamp("last_test_at");
        return new AiConnection(rs.getObject("id", UUID.class), Scope.valueOf(rs.getString("scope")),
                rs.getObject("owner_id", UUID.class), AiProvider.valueOf(rs.getString("provider")), rs.getString("model"),
                rs.getBytes("key_ciphertext"), rs.getBytes("key_iv"), rs.getObject("key_version", Integer.class),
                rs.getString("key_hint"), Status.valueOf(rs.getString("status")),
                tested == null ? null : tested.toInstant(), rs.getString("last_test_result"),
                rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant(), rs.getLong("version"));
    }
}
