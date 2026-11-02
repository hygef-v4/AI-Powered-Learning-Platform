package edu.aiplatform.ai.repository;

import edu.aiplatform.ai.model.QuotaPolicy;

import edu.aiplatform.ai.model.QuotaPolicy.TeacherQuota;
import edu.aiplatform.ai.model.QuotaPolicy.Window;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Hạn mức và bộ đếm cửa sổ ngày; chỉ dùng trong U06. Giữ chỗ: tạo dòng cửa sổ còn thiếu rồi khóa theo thứ tự cố
 * định (toàn hệ thống trước, người gọi sau) để tránh deadlock (P1, NFR-U06-01).
 */
@Repository
public class QuotaRepository {

    public static final String GLOBAL = "GLOBAL";

    private final JdbcClient jdbc;

    public QuotaRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    // ---- hạn mức ---------------------------------------------------------------------------------------------

    public QuotaPolicy policy() {
        return jdbc.sql("select default_day_tokens, global_day_tokens, version, updated_by, updated_at from ai_quota_policy "
                        + "where id = 1")
                .query((rs, n) -> new QuotaPolicy(rs.getObject("default_day_tokens", Long.class),
                        rs.getObject("global_day_tokens", Long.class), rs.getLong("version"),
                        rs.getObject("updated_by", UUID.class), rs.getTimestamp("updated_at").toInstant()))
                .optional().orElse(QuotaPolicy.empty());
    }

    /** Lưu hạn mức khi {@code version} khớp; chưa có dòng thì tạo khi {@code expected} = 0. */
    public boolean savePolicy(long defaultDay, long globalDay, long expected, UUID by, Instant now) {
        if (expected == 0) {
            int inserted = jdbc.sql("insert into ai_quota_policy (id, default_day_tokens, global_day_tokens, version, "
                            + "updated_by, updated_at) values (1, :d, :g, 1, :by, :now) on conflict (id) do nothing")
                    .param("d", defaultDay).param("g", globalDay).param("by", by).param("now", Timestamp.from(now)).update();
            if (inserted == 1) {
                return true;
            }
        }
        return jdbc.sql("update ai_quota_policy set default_day_tokens = :d, global_day_tokens = :g, version = version + 1, "
                        + "updated_by = :by, updated_at = :now where id = 1 and version = :expected")
                .param("d", defaultDay).param("g", globalDay).param("by", by).param("now", Timestamp.from(now))
                .param("expected", expected).update() == 1;
    }

    public Optional<TeacherQuota> teacherQuota(UUID accountId) {
        return jdbc.sql("select account_id, day_tokens, version, updated_by, updated_at from ai_teacher_quotas "
                        + "where account_id = :id")
                .param("id", accountId).query(QuotaRepository::teacher).optional();
    }

    public List<TeacherQuota> listTeacherQuotas() {
        return jdbc.sql("select account_id, day_tokens, version, updated_by, updated_at from ai_teacher_quotas "
                        + "order by updated_at desc")
                .query(QuotaRepository::teacher).list();
    }

    public void upsertTeacherQuota(UUID accountId, long dayTokens, UUID by, Instant now) {
        jdbc.sql("insert into ai_teacher_quotas (account_id, day_tokens, version, updated_by, updated_at) values "
                        + "(:id, :tokens, 0, :by, :now) on conflict (account_id) do update set day_tokens = :tokens, "
                        + "version = ai_teacher_quotas.version + 1, updated_by = :by, updated_at = :now")
                .param("id", accountId).param("tokens", dayTokens).param("by", by).param("now", Timestamp.from(now))
                .update();
    }

    public int deleteTeacherQuota(UUID accountId) {
        return jdbc.sql("delete from ai_teacher_quotas where account_id = :id").param("id", accountId).update();
    }

    // ---- cửa sổ ngày ----------------------------------------------------------------------------------------

    public void ensureWindow(String scopeKey, LocalDate day) {
        jdbc.sql("insert into ai_quota_windows (scope_key, day) values (:key, :day) on conflict do nothing")
                .param("key", scopeKey).param("day", Date.valueOf(day)).update();
    }

    public Window lockWindow(String scopeKey, LocalDate day) {
        return jdbc.sql("select scope_key, day, reserved_tokens, used_tokens from ai_quota_windows "
                        + "where scope_key = :key and day = :day for update")
                .param("key", scopeKey).param("day", Date.valueOf(day)).query(QuotaRepository::window).single();
    }

    public Optional<Window> findWindow(String scopeKey, LocalDate day) {
        return jdbc.sql("select scope_key, day, reserved_tokens, used_tokens from ai_quota_windows "
                        + "where scope_key = :key and day = :day")
                .param("key", scopeKey).param("day", Date.valueOf(day)).query(QuotaRepository::window).optional();
    }

    public List<Window> listWindows(Collection<String> scopeKeys, LocalDate day) {
        if (scopeKeys.isEmpty()) {
            return List.of();
        }
        return jdbc.sql("select scope_key, day, reserved_tokens, used_tokens from ai_quota_windows "
                        + "where scope_key in (:keys) and day = :day")
                .param("keys", scopeKeys).param("day", Date.valueOf(day)).query(QuotaRepository::window).list();
    }

    /** Cộng hoặc trừ phần giữ chỗ và phần đã dùng; không xuống dưới 0. */
    public void adjust(String scopeKey, LocalDate day, long reservedDelta, long usedDelta) {
        jdbc.sql("update ai_quota_windows set reserved_tokens = greatest(reserved_tokens + :r, 0), "
                        + "used_tokens = greatest(used_tokens + :u, 0) where scope_key = :key and day = :day")
                .param("r", reservedDelta).param("u", usedDelta).param("key", scopeKey).param("day", Date.valueOf(day))
                .update();
    }

    private static Window window(ResultSet rs, int n) throws SQLException {
        return new Window(rs.getString("scope_key"), rs.getDate("day").toLocalDate(), rs.getLong("reserved_tokens"),
                rs.getLong("used_tokens"));
    }

    private static TeacherQuota teacher(ResultSet rs, int n) throws SQLException {
        return new TeacherQuota(rs.getObject("account_id", UUID.class), rs.getLong("day_tokens"), rs.getLong("version"),
                rs.getObject("updated_by", UUID.class), rs.getTimestamp("updated_at").toInstant());
    }
}
