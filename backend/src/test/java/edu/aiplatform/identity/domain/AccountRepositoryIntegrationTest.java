package edu.aiplatform.identity.domain;

import static edu.aiplatform.identity.IdentityTestData.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import com.zaxxer.hikari.HikariDataSource;
import edu.aiplatform.identity.contract.AccountStatus;
import edu.aiplatform.identity.contract.Role;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * SQL viết tay của {@link AccountRepository} chạy trên PostgreSQL 16 thật. Container chạy chính script tạo role của
 * {@code infra/postgres/init}: Flyway chạy bằng {@code migrator}, repository bằng {@code app} như khi triển khai.
 */
@Testcontainers(disabledWithoutDocker = true)
class AccountRepositoryIntegrationTest {

    // Sinh mỗi lần chạy: container chỉ sống trong test, không để chuỗi mật khẩu cố định trong mã.
    private static final String APP_PASSWORD = UUID.randomUUID().toString();
    private static final String MIGRATOR_PASSWORD = UUID.randomUUID().toString();

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withEnv("POSTGRES_APP_PASSWORD", APP_PASSWORD)
            .withEnv("POSTGRES_MIGRATOR_PASSWORD", MIGRATOR_PASSWORD)
            .withCopyToContainer(rolesScript(), "/docker-entrypoint-initdb.d/01-roles.sh");

    private static HikariDataSource appDataSource;
    private static HikariDataSource adminDataSource;
    private static JdbcClient jdbc;
    private static JdbcClient admin;
    private static AccountRepository repository;
    private static TransactionTemplate tx;

    @BeforeAll
    static void migrate() {
        Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), "migrator", MIGRATOR_PASSWORD)
                .locations("classpath:db/migration").load().migrate();
        appDataSource = dataSource("app", APP_PASSWORD);
        adminDataSource = dataSource(POSTGRES.getUsername(), POSTGRES.getPassword());
        jdbc = JdbcClient.create(appDataSource);
        admin = JdbcClient.create(adminDataSource);
        repository = new AccountRepository(jdbc);
        tx = new TransactionTemplate(new DataSourceTransactionManager(appDataSource));
    }

    @AfterAll
    static void close() {
        if (appDataSource != null) {
            appDataSource.close();
        }
        if (adminDataSource != null) {
            adminDataSource.close();
        }
    }

    @BeforeEach
    void clean() {
        admin.sql("truncate accounts cascade").update();
    }

    /** Script tạo role của môi trường thật; bỏ CR phòng khi checkout trên Windows đổi xuống dòng. */
    private static Transferable rolesScript() {
        try {
            String script = Files.readString(Path.of("..", "infra", "postgres", "init", "01-roles.sh"));
            return Transferable.of(script.replace("\r", ""), 0755);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static HikariDataSource dataSource(String user, String password) {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(POSTGRES.getJdbcUrl());
        dataSource.setUsername(user);
        dataSource.setPassword(password);
        dataSource.setMaximumPoolSize(4);
        return dataSource;
    }

    @Test
    void insertedAccountReadsBackByIdAndEmail() {
        Account account = pending("taivm@fpt.edu.vn", Role.STUDENT, "Vũ Minh Tài");
        repository.insert(account);

        assertThat(repository.findById(account.id())).hasValue(account);
        assertThat(repository.findByEmail("taivm@fpt.edu.vn")).hasValue(account);
        assertThat(repository.findByEmail("khac@fpt.edu.vn")).isEmpty();
        assertThat(repository.findByEmails(List.of("taivm@fpt.edu.vn", "khac@fpt.edu.vn")))
                .extracting(Account::id).containsExactly(account.id());
        assertThat(repository.findByEmails(List.of())).isEmpty();
    }

    @Test
    void failedLoginsLockAtThresholdThenResetCounterWithoutBumpingVersion() {
        Account account = active("taivm@fpt.edu.vn", Role.STUDENT);
        repository.insert(account);
        // Có phần nano: kiểm so sánh locked_until = :lockUntil trong RETURNING vẫn đúng sau khi PostgreSQL làm tròn micro giây.
        Instant lockUntil = NOW.plus(Duration.ofMinutes(15)).plusNanos(123_456_789);

        assertThat(repository.recordFailedLogin(account.id(), 3, lockUntil)).isFalse();
        assertThat(repository.recordFailedLogin(account.id(), 3, lockUntil)).isFalse();
        assertThat(reload(account).failedLoginCount()).isEqualTo(2);
        assertThat(reload(account).lockedUntil()).isNull();

        assertThat(repository.recordFailedLogin(account.id(), 3, lockUntil)).isTrue();
        Account locked = reload(account);
        assertThat(locked.failedLoginCount()).isZero();
        assertThat(locked.lockedUntil()).isCloseTo(lockUntil, within(1, ChronoUnit.MICROS));
        assertThat(locked.version()).isEqualTo(account.version());

        // Lần sai kế tiếp không khóa lại ngay và giữ nguyên thời điểm khóa đang có.
        assertThat(repository.recordFailedLogin(account.id(), 3, lockUntil.plusSeconds(60))).isFalse();
        assertThat(reload(account).lockedUntil()).isEqualTo(locked.lockedUntil());
        assertThat(reload(account).failedLoginCount()).isEqualTo(1);

        repository.resetLoginThrottle(account.id());
        Account reset = reload(account);
        assertThat(reset.failedLoginCount()).isZero();
        assertThat(reset.lockedUntil()).isNull();
        assertThat(reset.version()).isEqualTo(account.version());
    }

    @Test
    void failedLoginOnUnknownAccountReturnsFalse() {
        assertThat(repository.recordFailedLogin(UUID.randomUUID(), 3, NOW)).isFalse();
    }

    @Test
    void activateSucceedsOnlyWhilePending() {
        Account pending = pending("taivm@fpt.edu.vn", Role.STUDENT, "Vũ Minh Tài");
        Account disabled = withStatus(pending("khoa@fpt.edu.vn", Role.STUDENT, "Khoa"), AccountStatus.DISABLED);
        repository.insert(pending);
        repository.insert(disabled);
        Instant later = NOW.plusSeconds(60);

        assertThat(repository.activate(pending.id(), "$2a$hash", later)).isEqualTo(1);
        Account activated = reload(pending);
        assertThat(activated.status()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(activated.passwordHash()).isEqualTo("$2a$hash");
        assertThat(activated.passwordChangedAt()).isEqualTo(later);
        assertThat(activated.version()).isEqualTo(pending.version() + 1);

        assertThat(repository.activate(pending.id(), "$2a$other", later)).isZero();
        assertThat(reload(pending).passwordHash()).isEqualTo("$2a$hash");
        assertThat(repository.activate(disabled.id(), "$2a$hash", later)).isZero();
        assertThat(reload(disabled).status()).isEqualTo(AccountStatus.DISABLED);
    }

    @Test
    void versionedUpdatesTouchNothingOnStaleVersion() {
        Account account = active("taivm@fpt.edu.vn", Role.STUDENT);
        repository.insert(account);
        long version = account.version();
        Instant later = NOW.plusSeconds(60);

        assertThat(repository.updateRole(account.id(), Role.TEACHER, version - 1, later)).isZero();
        assertThat(repository.updateStatus(account.id(), AccountStatus.DISABLED, version + 1, later)).isZero();
        assertThat(repository.updateProfile(account.id(), "Tên mới", null, version + 1, later)).isZero();
        assertThat(reload(account)).isEqualTo(account);

        assertThat(repository.updateRole(account.id(), Role.TEACHER, version, later)).isEqualTo(1);
        assertThat(reload(account).role()).isEqualTo(Role.TEACHER);
        assertThat(reload(account).updatedAt()).isEqualTo(later);
        assertThat(repository.updateRole(account.id(), Role.ADMIN, version, later)).isZero();

        assertThat(repository.updateProfile(account.id(), "Tên mới", "0912345678", version + 1, later)).isEqualTo(1);
        assertThat(reload(account).displayName()).isEqualTo("Tên mới");
        assertThat(reload(account).phoneNumber()).isEqualTo("0912345678");
        // Hồ sơ của chính mình: không kiểm phiên bản nhưng vẫn tăng.
        assertThat(repository.updateProfile(account.id(), "Tự sửa", null, null, later)).isEqualTo(1);
        assertThat(reload(account).version()).isEqualTo(version + 3);

        assertThat(repository.updateRole(UUID.randomUUID(), Role.TEACHER, 1, later)).isZero();
    }

    @Test
    void statusChangeClearsTemporaryLock() {
        Account account = active("taivm@fpt.edu.vn", Role.STUDENT);
        repository.insert(account);
        repository.recordFailedLogin(account.id(), 1, NOW.plus(Duration.ofMinutes(15)));
        repository.recordFailedLogin(account.id(), 5, NOW.plus(Duration.ofMinutes(15)));
        assertThat(reload(account).lockedUntil()).isNotNull();

        assertThat(repository.updateStatus(account.id(), AccountStatus.DISABLED, account.version(), NOW)).isEqualTo(1);
        Account disabled = reload(account);
        assertThat(disabled.status()).isEqualTo(AccountStatus.DISABLED);
        assertThat(disabled.lockedUntil()).isNull();
        assertThat(disabled.failedLoginCount()).isZero();
    }

    @Test
    void passwordUpdateClearsLockAndBumpsVersion() {
        Account account = active("taivm@fpt.edu.vn", Role.STUDENT);
        repository.insert(account);
        repository.recordFailedLogin(account.id(), 1, NOW.plus(Duration.ofMinutes(15)));

        assertThat(repository.updatePassword(account.id(), "$2a$new", NOW.plusSeconds(5))).isEqualTo(1);
        Account updated = reload(account);
        assertThat(updated.passwordHash()).isEqualTo("$2a$new");
        assertThat(updated.lockedUntil()).isNull();
        assertThat(updated.version()).isEqualTo(account.version() + 1);
    }

    @Test
    void searchEscapesLikeWildcardsAndIgnoresCase() {
        insertAll(pending("phantram@fpt.edu.vn", Role.STUDENT, "Đạt 100% chăm"),
                pending("gach@fpt.edu.vn", Role.STUDENT, "a_b"),
                pending("cheo@fpt.edu.vn", Role.STUDENT, "back\\slash"),
                pending("thuong@fpt.edu.vn", Role.STUDENT, "a1b 1000 chăm"));

        assertThat(names(search(null, null, "%"))).containsExactly("Đạt 100% chăm");
        assertThat(names(search(null, null, "_"))).containsExactly("a_b");
        assertThat(names(search(null, null, "\\"))).containsExactly("back\\slash");
        assertThat(names(search(null, null, "100%"))).containsExactly("Đạt 100% chăm");
        assertThat(names(search(null, null, "PHANTRAM@"))).containsExactly("Đạt 100% chăm");
        assertThat(names(search(null, null, "CHĂM"))).containsExactlyInAnyOrder("Đạt 100% chăm", "a1b 1000 chăm");
        assertThat(search(null, null, "khongco").total()).isZero();
    }

    @Test
    void searchFiltersByRoleAndStatusAndPagesNewestFirst() {
        for (int i = 0; i < 5; i++) {
            Account a = pending("sv" + i + "@fpt.edu.vn", Role.STUDENT, "Sinh viên " + i);
            repository.insert(touched(a, NOW.plusSeconds(i)));
        }
        repository.insert(active("gv@fpt.edu.vn", Role.TEACHER));
        repository.insert(withStatus(pending("tat@fpt.edu.vn", Role.STUDENT, "Đã tắt"), AccountStatus.DISABLED));

        AccountSearch.Result page = repository.search(new AccountSearch(Role.STUDENT, AccountStatus.PENDING, null, 1, 2));
        assertThat(page.total()).isEqualTo(5);
        assertThat(names(page)).containsExactly("Sinh viên 2", "Sinh viên 1");
        assertThat(names(repository.search(new AccountSearch(Role.STUDENT, AccountStatus.PENDING, null, 2, 2))))
                .containsExactly("Sinh viên 0");
        assertThat(repository.search(new AccountSearch(Role.STUDENT, AccountStatus.PENDING, null, 3, 2)).items())
                .isEmpty();

        assertThat(search(Role.TEACHER, null, null).total()).isEqualTo(1);
        assertThat(search(null, AccountStatus.DISABLED, null).total()).isEqualTo(1);
        assertThat(search(null, null, null).total()).isEqualTo(7);
    }

    @Test
    void findForLookupFiltersRolesStatusAndQuery() {
        insertAll(pending("b@fpt.edu.vn", Role.TEACHER, "Bình"), active("a@fpt.edu.vn", Role.TEACHER),
                pending("c@fpt.edu.vn", Role.STUDENT, "Chi"));

        assertThat(repository.findForLookup(null, Set.of(Role.TEACHER), null, 10))
                .extracting(Account::schoolEmail).containsExactlyInAnyOrder("a@fpt.edu.vn", "b@fpt.edu.vn");
        assertThat(repository.findForLookup("bìn", Set.of(Role.TEACHER, Role.STUDENT),
                AccountStatus.PENDING, 10)).extracting(Account::schoolEmail).containsExactly("b@fpt.edu.vn");
        assertThat(repository.findForLookup(null, Set.of(Role.TEACHER), null, 1)).hasSize(1);
        assertThat(repository.findForLookup(null, Set.of(), null, 10)).isEmpty();
    }

    @Test
    void existingEmailsAndCountsAndAdminPresence() {
        assertThat(repository.anyAdminExists()).isFalse();
        insertAll(active("ad@fpt.edu.vn", Role.ADMIN), active("gv@fpt.edu.vn", Role.TEACHER),
                pending("sv1@fpt.edu.vn", Role.STUDENT, "SV 1"), pending("sv2@fpt.edu.vn", Role.STUDENT, "SV 2"),
                withStatus(pending("sv3@fpt.edu.vn", Role.STUDENT, "SV 3"), AccountStatus.DISABLED));

        assertThat(repository.anyAdminExists()).isTrue();
        assertThat(repository.existingEmails(List.of("sv1@fpt.edu.vn", "moi@fpt.edu.vn", "ad@fpt.edu.vn")))
                .containsExactlyInAnyOrder("sv1@fpt.edu.vn", "ad@fpt.edu.vn");
        assertThat(repository.existingEmails(List.of())).isEmpty();
        assertThat(repository.countByRoleAndStatus()).isEqualTo(Map.of(
                Role.ADMIN, Map.of(AccountStatus.ACTIVE, 1L),
                Role.TEACHER, Map.of(AccountStatus.ACTIVE, 1L),
                Role.STUDENT, Map.of(AccountStatus.PENDING, 2L, AccountStatus.DISABLED, 1L)));
    }

    /**
     * BR-U01-66: hai Admin cuối cùng hạ quyền nhau cùng lúc. Transaction thứ hai phải chờ khóa dòng của transaction
     * đầu, rồi thấy chỉ còn một Admin nên từ chối; kết quả luôn còn ít nhất một Admin.
     */
    @Test
    void lockActiveAdminsSerializesCrossDemotion() throws Exception {
        Account first = active("ad1@fpt.edu.vn", Role.ADMIN);
        Account second = active("ad2@fpt.edu.vn", Role.ADMIN);
        insertAll(first, second, active("gv@fpt.edu.vn", Role.TEACHER));
        assertThat(repository.lockActiveAdmins()).containsExactlyInAnyOrder(first.id(), second.id());

        CountDownLatch firstLocked = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> demoteSecond = pool.submit(() -> demote(second, () -> {
                firstLocked.countDown();
                await(releaseFirst);
            }));
            assertThat(firstLocked.await(10, TimeUnit.SECONDS)).isTrue();
            Future<Boolean> demoteFirst = pool.submit(() -> demote(first, () -> { }));

            awaitBlockedOnRowLock();
            assertThat(demoteFirst.isDone()).isFalse();
            releaseFirst.countDown();

            assertThat(demoteSecond.get(10, TimeUnit.SECONDS)).isTrue();
            assertThat(demoteFirst.get(10, TimeUnit.SECONDS)).isFalse();
        } finally {
            releaseFirst.countDown();
            pool.shutdownNow();
        }
        assertThat(reload(first).role()).isEqualTo(Role.ADMIN);
        assertThat(reload(second).role()).isEqualTo(Role.TEACHER);
        assertThat(repository.lockActiveAdmins()).containsExactly(first.id());
    }

    @Test
    void lockActiveAdminsSkipsDisabledAndOtherRoles() {
        Account admin = active("ad@fpt.edu.vn", Role.ADMIN);
        insertAll(admin, withStatus(active("ad2@fpt.edu.vn", Role.ADMIN), AccountStatus.DISABLED),
                pending("ad3@fpt.edu.vn", Role.ADMIN, "Admin chờ"), active("gv@fpt.edu.vn", Role.TEACHER));

        List<UUID> locked = tx.execute(s -> repository.lockActiveAdmins());
        assertThat(locked).containsExactly(admin.id());
    }

    @Test
    void schemaRejectsDuplicateEmailAndInvalidRows() {
        repository.insert(pending("taivm@fpt.edu.vn", Role.STUDENT, "Vũ Minh Tài"));

        assertThatThrownBy(() -> repository.insert(pending("taivm@fpt.edu.vn", Role.TEACHER, "Trùng")))
                .isInstanceOf(DuplicateKeyException.class);
        assertThatThrownBy(() -> repository.insert(pending("TaiVM2@fpt.edu.vn", Role.STUDENT, "Hoa")))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("accounts_school_email_normalized");
        assertThatThrownBy(() -> repository.insert(pending(" khoa@fpt.edu.vn", Role.STUDENT, "Cách")))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("accounts_school_email_normalized");
        assertThatThrownBy(() -> repository.insert(withStatus(pending("nopw@fpt.edu.vn", Role.STUDENT, "Không mk"),
                AccountStatus.ACTIVE)))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("accounts_active_has_password");
        assertThatThrownBy(() -> rawInsert("role@fpt.edu.vn", "ROOT", "PENDING", 0))
                .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("accounts_role_check");
        assertThatThrownBy(() -> rawInsert("status@fpt.edu.vn", "STUDENT", "LOCKED", 0))
                .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("accounts_status_check");
        assertThatThrownBy(() -> rawInsert("count@fpt.edu.vn", "STUDENT", "PENDING", -1))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("accounts_failed_login_count_check");
        assertThat(search(null, null, null).total()).isEqualTo(1);
    }

    /** Giống {@code AccountAdminService.protectLastAdmin} rồi đổi role, trong một transaction. */
    private static boolean demote(Account target, Runnable afterLock) {
        return Boolean.TRUE.equals(tx.execute(status -> {
            List<UUID> admins = repository.lockActiveAdmins();
            afterLock.run();
            if (admins.contains(target.id()) && admins.size() <= 1) {
                return false;
            }
            return repository.updateRole(target.id(), Role.TEACHER, target.version(), NOW) == 1;
        }));
    }

    /** Chờ tới khi có một phiên PostgreSQL đang đợi khóa dòng (transaction thứ hai bị chặn ở FOR UPDATE). */
    private static void awaitBlockedOnRowLock() throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline) {
            long waiting = admin.sql("select count(*) from pg_stat_activity where wait_event_type = 'Lock' "
                    + "and query ilike '%for update%'").query(Long.class).single();
            if (waiting > 0) {
                return;
            }
            Thread.sleep(50);
        }
        throw new AssertionError("Transaction thứ hai không chờ khóa dòng Admin");
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Hết thời gian chờ");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private static void rawInsert(String email, String role, String status, int failedLoginCount) {
        jdbc.sql("insert into accounts (id, school_email, role, status, display_name, failed_login_count) "
                        + "values (:id, :email, :role, :status, 'Thô', :count)")
                .param("id", UUID.randomUUID()).param("email", email).param("role", role).param("status", status)
                .param("count", failedLoginCount).update();
    }

    private static AccountSearch.Result search(Role role, AccountStatus status, String query) {
        return repository.search(new AccountSearch(role, status, query, 0, 50));
    }

    private static List<String> names(AccountSearch.Result result) {
        return result.items().stream().map(Account::displayName).toList();
    }

    private static void insertAll(Account... accounts) {
        for (Account account : accounts) {
            repository.insert(account);
        }
    }

    private static Account reload(Account account) {
        return repository.findById(account.id()).orElseThrow();
    }

    private static Account pending(String email, Role role, String name) {
        return Account.newPending(UUID.randomUUID(), email, role, name, null, NOW);
    }

    private static Account active(String email, Role role) {
        Account a = pending(email, role, email.substring(0, email.indexOf('@')));
        return new Account(a.id(), a.schoolEmail(), a.role(), AccountStatus.ACTIVE, "$2a$12$hash", NOW,
                a.displayName(), a.phoneNumber(), 0, null, NOW, NOW, a.version());
    }

    private static Account withStatus(Account a, AccountStatus status) {
        return new Account(a.id(), a.schoolEmail(), a.role(), status, a.passwordHash(), a.passwordChangedAt(),
                a.displayName(), a.phoneNumber(), a.failedLoginCount(), a.lockedUntil(), a.createdAt(),
                a.updatedAt(), a.version());
    }

    private static Account touched(Account a, Instant updatedAt) {
        return new Account(a.id(), a.schoolEmail(), a.role(), a.status(), a.passwordHash(), a.passwordChangedAt(),
                a.displayName(), a.phoneNumber(), a.failedLoginCount(), a.lockedUntil(), a.createdAt(), updatedAt,
                a.version());
    }
}
