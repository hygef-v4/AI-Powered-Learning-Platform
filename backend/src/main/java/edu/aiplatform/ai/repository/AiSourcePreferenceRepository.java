package edu.aiplatform.ai.repository;

import edu.aiplatform.ai.contract.AiProvider;
import edu.aiplatform.ai.contract.AiSource;
import edu.aiplatform.ai.model.AiSourcePreference;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Nguồn AI mặc định theo tài khoản; chỉ dùng trong U06. */
@Repository
public class AiSourcePreferenceRepository {

    private final JdbcClient jdbc;

    public AiSourcePreferenceRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<AiSourcePreference> find(UUID accountId) {
        return jdbc.sql("select source, provider from ai_source_preferences where account_id = :id")
                .param("id", accountId)
                .query((rs, n) -> new AiSourcePreference(AiSource.valueOf(rs.getString("source")),
                        rs.getString("provider") == null ? null : AiProvider.valueOf(rs.getString("provider"))))
                .optional();
    }

    public void save(UUID accountId, AiSourcePreference preference, Instant now) {
        jdbc.sql("insert into ai_source_preferences (account_id, source, provider, updated_at) values "
                        + "(:id, :source, :provider, :now) on conflict (account_id) do update set source = :source, "
                        + "provider = :provider, updated_at = :now")
                .param("id", accountId).param("source", preference.source().name())
                .param("provider", preference.provider() == null ? null : preference.provider().name())
                .param("now", Timestamp.from(now)).update();
    }

    public void delete(UUID accountId) {
        jdbc.sql("delete from ai_source_preferences where account_id = :id").param("id", accountId).update();
    }
}
