package edu.aiplatform.settings.repository;

import edu.aiplatform.settings.model.SettingRow;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Bảng {@code system_settings}; chỉ dùng trong U03. */
@Repository
public class SystemSettingRepository {

    private static final String COLUMNS = "key, setting_group, value::text as value, version, updated_by, updated_at";

    private final JdbcClient jdbc;

    public SystemSettingRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public List<SettingRow> findAll() {
        return jdbc.sql("select " + COLUMNS + " from system_settings").query(SystemSettingRepository::map).list();
    }

    public Optional<SettingRow> find(String key) {
        return jdbc.sql("select " + COLUMNS + " from system_settings where key = :key")
                .param("key", key).query(SystemSettingRepository::map).optional();
    }

    /** Khởi tạo mục khi khởi động; mục đã có giữ nguyên (P14, BR-U03-84). */
    public int insertIfAbsent(String key, String group, String valueJson, Instant now) {
        return jdbc.sql("insert into system_settings (key, setting_group, value, version, updated_by, updated_at) "
                        + "values (:key, :group, cast(:value as jsonb), 1, null, :now) on conflict (key) do nothing")
                .param("key", key).param("group", group).param("value", valueJson)
                .param("now", Timestamp.from(now)).update();
    }

    /** Ghi mục chưa có dòng (bị xóa sau khi khởi động); trả 0 nếu nơi khác vừa tạo dòng (xung đột). */
    public int insert(String key, String group, String valueJson, UUID updatedBy, Instant now) {
        return jdbc.sql("insert into system_settings (key, setting_group, value, version, updated_by, updated_at) "
                        + "values (:key, :group, cast(:value as jsonb), 1, :by, :now) on conflict (key) do nothing")
                .param("key", key).param("group", group).param("value", valueJson).param("by", updatedBy)
                .param("now", Timestamp.from(now)).update();
    }

    /** Cập nhật có điều kiện {@code version} khớp; trả số dòng cập nhật (0 là xung đột). */
    public int update(String key, String valueJson, long expectedVersion, UUID updatedBy, Instant now) {
        return jdbc.sql("update system_settings set value = cast(:value as jsonb), version = version + 1, "
                        + "updated_by = :by, updated_at = :now where key = :key and version = :version")
                .param("value", valueJson).param("by", updatedBy).param("now", Timestamp.from(now))
                .param("key", key).param("version", expectedVersion).update();
    }

    private static SettingRow map(java.sql.ResultSet rs, int row) throws java.sql.SQLException {
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        return new SettingRow(rs.getString("key"), rs.getString("setting_group"), rs.getString("value"),
                rs.getLong("version"), rs.getObject("updated_by", UUID.class),
                updatedAt == null ? null : updatedAt.toInstant());
    }
}
