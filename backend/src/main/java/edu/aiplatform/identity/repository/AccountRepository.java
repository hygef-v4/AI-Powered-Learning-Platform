package edu.aiplatform.identity.repository;

import edu.aiplatform.identity.model.Account;
import edu.aiplatform.identity.model.AccountSearch;

import edu.aiplatform.identity.contract.AccountStatus;
import edu.aiplatform.identity.contract.Role;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Bảng {@code accounts}; chỉ dùng trong U01, unit khác đọc qua {@code IAccountLookup}. Hàm sửa có
 * {@code expectedVersion} trả số dòng cập nhật (0 là xung đột phiên bản hoặc không tồn tại).
 * Đếm sai mật khẩu không tăng {@code version} để không làm Admin đang sửa bị 409 oan.
 */
@Repository
public class AccountRepository {

    private static final String COLUMNS = "id, school_email, role, status, password_hash, password_changed_at, "
            + "display_name, phone_number, failed_login_count, locked_until, created_at, updated_at, version";

    private final JdbcClient jdbc;

    public AccountRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public void insert(Account account) {
        jdbc.sql("insert into accounts (id, school_email, role, status, password_hash, password_changed_at, "
                        + "display_name, phone_number, created_at, updated_at, version) values (:id, :email, :role, "
                        + ":status, :hash, :changedAt, :name, :phone, :createdAt, :updatedAt, :version)")
                .param("id", account.id()).param("email", account.schoolEmail())
                .param("role", account.role().name()).param("status", account.status().name())
                .param("hash", account.passwordHash()).param("changedAt", timestamp(account.passwordChangedAt()))
                .param("name", account.displayName()).param("phone", account.phoneNumber())
                .param("createdAt", timestamp(account.createdAt())).param("updatedAt", timestamp(account.updatedAt()))
                .param("version", account.version())
                .update();
    }

    public Optional<Account> findById(UUID id) {
        return jdbc.sql("select " + COLUMNS + " from accounts where id = :id")
                .param("id", id).query(AccountRepository::map).optional();
    }

    public List<Account> findByIds(Collection<UUID> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return jdbc.sql("select " + COLUMNS + " from accounts where id in (:ids)")
                .param("ids", List.copyOf(ids)).query(AccountRepository::map).list();
    }

    /** Đọc và khóa chia sẻ tới hết transaction: bên gán phân công giữ để role, trạng thái không đổi giữa chừng. */
    public Optional<Account> findByIdForShare(UUID id) {
        return jdbc.sql("select " + COLUMNS + " from accounts where id = :id for share")
                .param("id", id).query(AccountRepository::map).optional();
    }

    /** Khóa dòng tới hết transaction trước khi kiểm phân công rồi đổi role (BR-U01-64). */
    public void lockById(UUID id) {
        jdbc.sql("select id from accounts where id = :id for update").param("id", id).query(UUID.class).optional();
    }

    public List<Account> listActiveAdmins(UUID afterId,int limit) {
        var query=jdbc.sql("select "+COLUMNS+" from accounts where role='ADMIN' and status='ACTIVE'"
                +(afterId==null?"":" and id>:after")+" order by id limit :limit").param("limit",limit);
        if(afterId!=null)query.param("after",afterId);
        return query.query(AccountRepository::map).list();
    }

    /** {@code schoolEmail} đã chuẩn hóa (BR-U01-02). */
    public Optional<Account> findByEmail(String schoolEmail) {
        return jdbc.sql("select " + COLUMNS + " from accounts where school_email = :email")
                .param("email", schoolEmail).query(AccountRepository::map).optional();
    }

    public List<Account> findByEmails(Collection<String> schoolEmails) {
        if (schoolEmails.isEmpty()) {
            return List.of();
        }
        return jdbc.sql("select " + COLUMNS + " from accounts where school_email in (:emails)")
                .param("emails", schoolEmails).query(AccountRepository::map).list();
    }

    /** Các email trong danh sách đã có tài khoản; dùng khi kiểm cả file CSV bằng một truy vấn (BR-U01-82). */
    public Set<String> existingEmails(Collection<String> schoolEmails) {
        if (schoolEmails.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(jdbc.sql("select school_email from accounts where school_email in (:emails)")
                .param("emails", schoolEmails).query(String.class).list());
    }

    /** Account List: lọc role, trạng thái, tìm email/tên không phân biệt hoa thường; mới cập nhật trước. */
    public AccountSearch.Result search(AccountSearch filter) {
        StringBuilder where = new StringBuilder(" where 1 = 1");
        Map<String, Object> params = new HashMap<>();
        if (filter.role() != null) {
            where.append(" and role = :role");
            params.put("role", filter.role().name());
        }
        if (filter.status() != null) {
            where.append(" and status = :status");
            params.put("status", filter.status().name());
        }
        if (filter.query() != null) {
            where.append(" and (school_email ilike :q escape '\\' or display_name ilike :q escape '\\')");
            params.put("q", "%" + escapeLike(filter.query()) + "%");
        }
        long total = jdbc.sql("select count(*) from accounts" + where).params(params).query(Long.class).single();
        List<Account> items = jdbc.sql("select " + COLUMNS + " from accounts" + where
                        + " order by updated_at desc, id limit :limit offset :offset")
                .params(params).param("limit", filter.size()).param("offset", (long) filter.page() * filter.size())
                .query(AccountRepository::map).list();
        return new AccountSearch.Result(items, total);
    }

    /**
     * Tìm tài khoản cho unit khác chọn người (U04 Chủ nhiệm môn, U05 giảng viên/sinh viên): theo email hoặc tên,
     * lọc role và (nếu có) trạng thái, sắp theo tên, tối đa {@code limit} dòng.
     */
    public List<Account> findForLookup(String query, Set<Role> roles, AccountStatus status, int limit) {
        if (roles.isEmpty()) {
            return List.of();
        }
        StringBuilder sql = new StringBuilder("select " + COLUMNS + " from accounts where role in (:roles)");
        Map<String, Object> params = new HashMap<>();
        params.put("roles", roles.stream().map(Role::name).toList());
        if (status != null) {
            sql.append(" and status = :status");
            params.put("status", status.name());
        }
        if (query != null && !query.isBlank()) {
            sql.append(" and (school_email ilike :q escape '\\' or display_name ilike :q escape '\\')");
            params.put("q", "%" + escapeLike(query.strip()) + "%");
        }
        sql.append(" order by display_name, school_email limit :limit");
        return jdbc.sql(sql.toString()).params(params).param("limit", limit).query(AccountRepository::map).list();
    }

    /** Sửa tên, số điện thoại; {@code expectedVersion} rỗng là không kiểm phiên bản (hồ sơ của chính mình). */
    public int updateProfile(UUID id, String displayName, String phoneNumber, Long expectedVersion, Instant now) {
        return jdbc.sql("update accounts set display_name = :name, phone_number = :phone, updated_at = :now, "
                        + "version = version + 1 where id = :id"
                        + (expectedVersion == null ? "" : " and version = :version"))
                .param("name", displayName).param("phone", phoneNumber).param("now", timestamp(now))
                .param("id", id).params(expectedVersion == null ? Map.of() : Map.of("version", expectedVersion))
                .update();
    }

    /** Đặt mật khẩu mới (đổi hoặc đặt lại) và xóa khóa tạm (F5 bước 6). */
    public int updatePassword(UUID id, String passwordHash, Instant now) {
        return jdbc.sql("update accounts set password_hash = :hash, password_changed_at = :now, "
                        + "failed_login_count = 0, locked_until = null, updated_at = :now, version = version + 1 "
                        + "where id = :id")
                .param("hash", passwordHash).param("now", timestamp(now)).param("id", id).update();
    }

    /** Kích hoạt: chỉ thành công khi tài khoản vẫn {@code PENDING} (BR-U01-13). */
    public int activate(UUID id, String passwordHash, Instant now) {
        return jdbc.sql("update accounts set password_hash = :hash, password_changed_at = :now, status = 'ACTIVE', "
                        + "failed_login_count = 0, locked_until = null, updated_at = :now, version = version + 1 "
                        + "where id = :id and status = 'PENDING'")
                .param("hash", passwordHash).param("now", timestamp(now)).param("id", id).update();
    }

    public int updateRole(UUID id, Role role, long expectedVersion, Instant now) {
        return jdbc.sql("update accounts set role = :role, updated_at = :now, version = version + 1 "
                        + "where id = :id and version = :version")
                .param("role", role.name()).param("now", timestamp(now)).param("id", id)
                .param("version", expectedVersion).update();
    }

    /** Đổi trạng thái; mở lại thì cũng xóa khóa tạm (F12). */
    public int updateStatus(UUID id, AccountStatus status, long expectedVersion, Instant now) {
        return jdbc.sql("update accounts set status = :status, failed_login_count = 0, locked_until = null, "
                        + "updated_at = :now, version = version + 1 where id = :id and version = :version")
                .param("status", status.name()).param("now", timestamp(now)).param("id", id)
                .param("version", expectedVersion).update();
    }

    /**
     * Tăng đếm sai mật khẩu trong một câu lệnh. Chạm ngưỡng thì đặt {@code locked_until} và đưa đếm về 0 để hết
     * khóa không bị khóa lại ngay ở lần sai kế tiếp (BR-U01-41). Trả {@code true} nếu lần này vừa khóa.
     */
    public boolean recordFailedLogin(UUID id, int threshold, Instant lockUntil) {
        return jdbc.sql("update accounts set "
                        + "locked_until = case when failed_login_count + 1 >= :threshold then :lockUntil "
                        + "else locked_until end, "
                        + "failed_login_count = case when failed_login_count + 1 >= :threshold then 0 "
                        + "else failed_login_count + 1 end "
                        + "where id = :id returning locked_until = :lockUntil as locked")
                .param("threshold", threshold).param("lockUntil", timestamp(lockUntil)).param("id", id)
                .query(Boolean.class).optional().orElse(false);
    }

    /** Đăng nhập đúng xóa đếm sai và khóa tạm (BR-U01-42); không tăng {@code version}. */
    public void resetLoginThrottle(UUID id) {
        jdbc.sql("update accounts set failed_login_count = 0, locked_until = null "
                        + "where id = :id and (failed_login_count <> 0 or locked_until is not null)")
                .param("id", id).update();
    }

    /**
     * Khóa các dòng Admin đang hoạt động tới hết transaction và trả ID của chúng; hai Admin không thể đồng thời
     * hạ quyền nhau làm mất Admin cuối cùng (BR-U01-66).
     */
    public List<UUID> lockActiveAdmins() {
        return jdbc.sql("select id from accounts where role = 'ADMIN' and status = 'ACTIVE' order by id for update")
                .query(UUID.class).list();
    }

    public boolean anyAdminExists() {
        return jdbc.sql("select exists (select 1 from accounts where role = 'ADMIN')").query(Boolean.class).single();
    }

    /** Số tài khoản theo role và trạng thái (U15 qua {@code IAccountLookup}). */
    public Map<Role, Map<AccountStatus, Long>> countByRoleAndStatus() {
        record Count(Role role, AccountStatus status, long total) {
        }
        List<Count> rows = jdbc.sql("select role, status, count(*) as total from accounts group by role, status")
                .query((rs, row) -> new Count(Role.valueOf(rs.getString("role")),
                        AccountStatus.valueOf(rs.getString("status")), rs.getLong("total")))
                .list();
        Map<Role, Map<AccountStatus, Long>> counts = new EnumMap<>(Role.class);
        for (Count count : rows) {
            counts.computeIfAbsent(count.role(), r -> new EnumMap<>(AccountStatus.class))
                    .put(count.status(), count.total());
        }
        return counts;
    }

    static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private static Timestamp timestamp(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }

    private static Account map(ResultSet rs, int row) throws SQLException {
        return new Account(rs.getObject("id", UUID.class), rs.getString("school_email"),
                Role.valueOf(rs.getString("role")), AccountStatus.valueOf(rs.getString("status")),
                rs.getString("password_hash"), instant(rs, "password_changed_at"), rs.getString("display_name"),
                rs.getString("phone_number"), rs.getInt("failed_login_count"), instant(rs, "locked_until"),
                instant(rs, "created_at"), instant(rs, "updated_at"), rs.getLong("version"));
    }
}
