package edu.aiplatform.subjects.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import edu.aiplatform.subjects.contract.SubjectStatus;
import edu.aiplatform.subjects.contract.ISubjectQuery;
import edu.aiplatform.identity.contract.ISubjectScope;
import edu.aiplatform.subjects.service.SubjectQueryService;
import edu.aiplatform.subjects.service.SubjectScopeService;
import edu.aiplatform.subjects.service.SubjectCommandService;
import edu.aiplatform.subjects.service.SubjectAuditRecorder;
import edu.aiplatform.subjects.service.SubjectStatusService;
import edu.aiplatform.subjects.service.SubjectMutationGuard;
import edu.aiplatform.subjects.contract.ISubjectMutationGuard;
import edu.aiplatform.subjects.contract.ISubjectClasses;
import edu.aiplatform.subjects.contract.SubjectClassSummary;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.Role;
import edu.aiplatform.identity.contract.AccountContact;
import edu.aiplatform.identity.contract.AccountStatus;
import edu.aiplatform.identity.contract.IAccountLookup;
import edu.aiplatform.identity.contract.IAuthorization;
import edu.aiplatform.identity.contract.AuthorizationDecision;
import edu.aiplatform.audit.contract.IAudit;
import edu.aiplatform.shared.error.ApiException;
import edu.aiplatform.subjects.model.Subject;
import edu.aiplatform.subjects.model.SubjectCode;
import java.sql.Connection;
import java.sql.Statement;
import java.time.Instant;
import java.time.Clock;
import java.time.ZoneOffset;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Developer persistence checks trên PostgreSQL thật, không dùng H2 hoặc môi trường ứng dụng.
 * Container PostgreSQL cô lập tự tạo khi chạy test; mỗi lần tạo schema riêng.
 * Không thay bằng chứng integration độc lập với schema/accounts thật từ U01.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = SubjectRepositoryIntegrationTest.PersistenceConfig.class)
@Testcontainers(disabledWithoutDocker = true)
class SubjectRepositoryIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("u04_subjects_test")
                    .withPassword(UUID.randomUUID().toString());

    private static final String SCHEMA =
            "u04_subjects_" + UUID.randomUUID().toString().replace("-", "");
    private static final UUID MANAGER = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID OTHER_MANAGER =
            UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final Instant CREATED = Instant.parse("2026-10-18T00:00:00.123456Z");
    private static final Instant LATER = CREATED.plusSeconds(60);
    private static DriverManagerDataSource testSource;

    @Autowired private ISubjectRepository subjectRepository;
    @Autowired private DataSource dataSource;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private ISubjectQuery subjectQuery;
    @Autowired private ISubjectScope subjectScope;
    @Autowired private SubjectCommandService subjectCommandService;
    @Autowired private IAudit audit;
    @Autowired private IAccountLookup accountLookup;
    @Autowired private SubjectStatusService subjectStatusService;
    @Autowired private ISubjectMutationGuard mutationGuard;
    @Autowired private ISubjectClasses archiveChecker;

    @TestConfiguration
    @EnableTransactionManagement
    @Import({
        SubjectRepositoryAdapter.class,
        SubjectQueryService.class,
        SubjectScopeService.class,
        SubjectCommandService.class,
        SubjectAuditRecorder.class,
        SubjectStatusService.class,
        SubjectMutationGuard.class
    })
    static class PersistenceConfig {

        @Bean
        IAuthorization authorization() {
            return (actor, action, resource) -> AuthorizationDecision.allow();
        }

        @Bean
        IAccountLookup accountLookup() {
            return mock(IAccountLookup.class);
        }

        @Bean
        IAudit audit() {
            return mock(IAudit.class);
        }

        @Bean
        ISubjectClasses archiveChecker() {
            return mock(ISubjectClasses.class);
        }

        @Bean
        Clock clock() {
            return Clock.fixed(LATER, ZoneOffset.UTC);
        }

        @Bean
        DataSource dataSource() throws Exception {
            DriverManagerDataSource source =
                    new DriverManagerDataSource(
                            POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
            try (Connection connection = source.getConnection();
                    Statement sql = connection.createStatement()) {
                sql.execute(
                        "do $$ begin if not exists (select from pg_roles where rolname = 'app') "
                                + "then create role app nologin; end if; end $$");
                sql.execute("create schema " + SCHEMA);
                sql.execute("grant usage on schema " + SCHEMA + " to app");
                // Mô phỏng default grant của infra: migration subjects phải thật sự REVOKE DELETE.
                sql.execute(
                        "alter default privileges in schema "
                                + SCHEMA
                                + " grant select, insert, update, delete on tables to app");
            }
            source.setSchema(SCHEMA);
            testSource = source;
            new ResourceDatabasePopulator(
                            new ClassPathResource("subjects/identity-prerequisite.sql"))
                    .execute(source);
            flyway(source).migrate();
            new JdbcTemplate(source)
                    .execute(
                            "create table u04_lifecycle_classes (id uuid primary key, subject_id uuid not null references subjects(id), status varchar(20) not null)");
            new JdbcTemplate(source)
                    .execute(
                            "create table u04_audit_events (id uuid primary key, action varchar(100) not null, result varchar(20) not null, object_id varchar(100))");
            return source;
        }

        @Bean
        JdbcClient jdbcClient(DataSource source) {
            return JdbcClient.create(source);
        }

        @Bean
        PlatformTransactionManager transactionManager(DataSource source) {
            return new DataSourceTransactionManager(source);
        }
    }

    private static Flyway flyway(DataSource source) {
        return Flyway.configure()
                .dataSource(source)
                .defaultSchema(SCHEMA)
                .locations("classpath:db/migration/subjects")
                // Fixture accounts tạo schema không rỗng; baseline0 chỉ dùng trong test cô lập này.
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .load();
    }

    @BeforeEach
    void resetFixture() {
        reset(audit, accountLookup, archiveChecker);
        when(archiveChecker.archiveClassesOfSubject(any(), any()))
                .thenAnswer(call -> fixtureArchiveAll(call.getArgument(0)));
        for (UUID id : List.of(MANAGER, OTHER_MANAGER)) {
            when(accountLookup.getContact(id))
                    .thenReturn(
                            Optional.of(
                                    new AccountContact(
                                            id,
                                            "fixture@example.test",
                                            "Fixture",
                                            Role.SUBJECT_MANAGER,
                                            AccountStatus.ACTIVE)));
            when(accountLookup.lockContact(id))
                    .thenReturn(
                            Optional.of(
                                    new AccountContact(
                                            id,
                                            "fixture@example.test",
                                            "Fixture",
                                            Role.SUBJECT_MANAGER,
                                            AccountStatus.ACTIVE)));
        }
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.update("delete from u04_audit_events");
        jdbc.update("delete from u04_lifecycle_classes");
        jdbc.update("delete from subjects");
        jdbc.update("delete from accounts");
        jdbc.update("insert into accounts (id) values (?), (?)", MANAGER, OTHER_MANAGER);
    }

    @AfterAll
    static void dropOnlyTheSchemaCreatedByThisTest() throws Exception {
        if (testSource != null) {
            try (Connection connection = testSource.getConnection();
                    Statement sql = connection.createStatement()) {
                sql.execute("drop schema " + SCHEMA + " cascade");
            }
        }
    }

    private Subject subject(String code, String name, UUID managerId) {
        return Subject.createSubject(UUID.randomUUID(), code, name, "Description", managerId, CREATED);
    }

    @Test
    void insertRoundTripsNullableManagerTimestampsAndPublicVersionOne() {
        Subject subject = subject("swr302", "Requirements", null);
        subjectRepository.insertSubject(subject);
        assertThat(subjectRepository.findById(subject.id())).contains(subject);
        assertThat(subjectRepository.hasSubjectWithCode(new SubjectCode(" swr302 "))).isTrue();
        assertThat(
                        new JdbcTemplate(dataSource)
                                .queryForObject(
                                        "select version from subjects where id = ?",
                                        Long.class,
                                        subject.id()))
                .isZero();
        assertThat(subjectRepository.findById(UUID.randomUUID())).isEmpty();
    }

    @Test
    void updateComparesVersionsAtomicallyAndReadAgainInSameTransactionIsFresh() {
        Subject original = subject("SWR302", "Original", MANAGER);
        subjectRepository.insertSubject(original);
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.executeWithoutResult(
                ignored -> {
                    Subject firstRead = subjectRepository.findById(original.id()).orElseThrow();
                    Subject edited = firstRead.updateInformation("Changed", null, LATER);
                    assertThat(
                            subjectRepository.updateSubject(edited, firstRead.version())).isTrue();
                    assertThat(subjectRepository.findById(original.id())).contains(edited);
                    assertThat(
                            subjectRepository.updateSubject(
                                    firstRead.archive(LATER), firstRead.version()))
                            .isFalse();
                });
        Subject saved = subjectRepository.findById(original.id()).orElseThrow();
        assertThat(saved.version()).isEqualTo(2);
        assertThat(saved.status()).isEqualTo(SubjectStatus.ACTIVE);
        assertThat(
                        new JdbcTemplate(dataSource)
                                .queryForObject("select version from subjects", Long.class))
                .isEqualTo(1);
    }

    @Test
    void managerArchiveAndReopenEachAdvanceExactlyOneVersion() {
        Subject original = subject("SWR302", "Requirements", MANAGER);
        subjectRepository.insertSubject(original);
        Subject archived = original.archive(LATER);
        assertThat(subjectRepository.updateSubject(archived, 1)).isTrue();
        assertThat(archived.managerId()).isNull();
        assertThat(archived.version()).isEqualTo(2);
        assertThat(archived.changeManager(null, LATER)).isSameAs(archived);
        Subject reopened = archived.reopen(LATER);
        assertThat(subjectRepository.updateSubject(reopened, 2)).isTrue();
        assertThat(subjectRepository.findById(original.id())).contains(reopened);
        assertThat(reopened.version()).isEqualTo(3);
        assertThat(reopened.managerId()).isNull();
    }

    @Test
    void updateCannotChangeCodeOrCreatedAtAndRejectsAnUnchangedSnapshot() {
        Subject original = subject("SWR302", "Requirements", null);
        subjectRepository.insertSubject(original);
        Subject changedCode =
                new Subject(
                        original.id(),
                        new SubjectCode("OTHER"),
                        original.name(),
                        null,
                        original.status(),
                        null,
                        CREATED,
                        LATER,
                        2);
        assertThat(subjectRepository.updateSubject(changedCode, 1)).isFalse();
        assertThat(subjectRepository.findById(original.id())).contains(original);
        // Production còn EntityManagerFactory nên @Repository bọc lỗi thành
        // InvalidDataAccessApiUsageException; test này không dựng JPA nên nhận lỗi gốc.
        assertThatThrownBy(() -> subjectRepository.updateSubject(original, 1))
                .satisfies(
                        failure ->
                                assertThat(NestedExceptionUtils.getMostSpecificCause(failure))
                                        .isInstanceOf(IllegalArgumentException.class));
        Subject alteredCreation =
                new Subject(
                        original.id(),
                        original.code(),
                        "New name",
                        null,
                        original.status(),
                        null,
                        LATER,
                        LATER,
                        2);
        assertThat(subjectRepository.updateSubject(alteredCreation, 1)).isTrue();
        assertThat(
                subjectRepository
                        .findById(original.id())
                        .orElseThrow()
                        .createdAt()).isEqualTo(CREATED);
    }

    @Test
    void searchFiltersAndPagesInCodeOrderWithBothStatusCounts() {
        Subject a = subject("AAA", "Requirements A", MANAGER);
        Subject b = subject("BBB", "Requirements B", MANAGER);
        Subject c = subject("CCC", "Other", OTHER_MANAGER);
        subjectRepository.insertSubject(c);
        subjectRepository.insertSubject(b);
        subjectRepository.insertSubject(a);
        assertThat(subjectRepository.updateSubject(b.archive(LATER), 1)).isTrue();
        assertThat(subjectRepository.findSubjectPage(" requirements ", null, 0, 1))
                .extracting(s -> s.code().value())
                .containsExactly("AAA");
        assertThat(subjectRepository.findSubjectPage("REQUIREMENTS", null, 1, 1))
                .extracting(s -> s.code().value())
                .containsExactly("BBB");
        assertThat(subjectRepository.findSubjectPage("requirements", SubjectStatus.ACTIVE, 0, 100))
                .containsExactly(a);
        assertThat(subjectRepository.findSubjectPage("bbb", null, 0, 20)).hasSize(1);
        assertThat(subjectRepository.findSubjectPage("requirements", null, 2, 1)).isEmpty();
        assertThat(subjectRepository.countSubjects("requirements", null)).isEqualTo(2);
        assertThat(subjectRepository.countSubjectsByStatus("requirements"))
                .containsExactlyInAnyOrderEntriesOf(
                        Map.of(SubjectStatus.ACTIVE, 1L, SubjectStatus.ARCHIVED, 1L));
        assertThat(subjectRepository.countSubjectsByStatus("missing"))
                .containsExactlyInAnyOrderEntriesOf(
                        Map.of(SubjectStatus.ACTIVE, 0L, SubjectStatus.ARCHIVED, 0L));
    }

    @Test
    void searchTreatsPercentUnderscoreExclamationAndQuotesAsLiteralText() {
        Subject literal = subject("LITERAL", "100% rate_A! 'quoted'", null);
        subjectRepository.insertSubject(literal);
        subjectRepository.insertSubject(subject("OTHER", "100 rateBA", null));
        for (String query : List.of("%", "_", "!", "'quoted'")) {
            assertThat(
                    subjectRepository.findSubjectPage(query, null, 0, 20)).containsExactly(literal);
        }
        assertThat(subjectRepository.findSubjectPage("' or 1=1 --", null, 0, 20)).isEmpty();
    }

    @Test
    void rejectsInvalidPagesBeforeQuerying() {
        for (int[] page : List.of(new int[] {-1, 20}, new int[] {0, 0}, new int[] {0, 101})) {
            assertThatThrownBy(
                    () -> subjectRepository.findSubjectPage(null, null, page[0], page[1]))
                    .hasMessageContaining("Trang phải từ 0");
        }
    }

    @Test
    void managedSubjectsReflectActiveReassignmentAndArchiveClearsManagerWithoutCaching() {
        assertThatThrownBy(() -> subjectRepository.findSubjectsManagedBy(null))
                .isInstanceOf(NullPointerException.class);
        Subject a = subject("AAA", "A", MANAGER);
        Subject b = subject("BBB", "B", MANAGER);
        subjectRepository.insertSubject(a);
        subjectRepository.insertSubject(b);
        assertThat(subjectRepository.findSubjectsManagedBy(MANAGER)).containsExactly(a, b);
        Subject reassigned = b.changeManager(OTHER_MANAGER, LATER);
        assertThat(subjectRepository.updateSubject(reassigned, 1)).isTrue();
        assertThat(subjectRepository.findSubjectsManagedBy(MANAGER)).containsExactly(a);
        assertThat(
                subjectRepository.findSubjectsManagedBy(OTHER_MANAGER)).containsExactly(reassigned);
        Subject archived = reassigned.archive(LATER);
        assertThat(subjectRepository.updateSubject(archived, 2)).isTrue();
        assertThat(subjectRepository.findById(b.id())).contains(archived);
        assertThat(archived.managerId()).isNull();
        assertThat(subjectRepository.findSubjectsManagedBy(OTHER_MANAGER)).isEmpty();
    }

    @Test
    void publicQueryAndScopeReadCurrentAssignmentsThroughTheRealDatabase() {
        Subject original = subject("SWR302", "Requirements", MANAGER);
        subjectRepository.insertSubject(original);
        subjectRepository.insertSubject(subject("SWR303", "Unassigned", null));
        assertThat(subjectQuery.getSubject(original.id()).orElseThrow().managerId())
                .isEqualTo(MANAGER);
        assertThat(subjectQuery.isActive(original.id())).isTrue();
        assertThat(subjectScope.isSubjectManager(MANAGER, original.id())).isTrue();
        assertThat(subjectScope.isSubjectManager(OTHER_MANAGER, original.id())).isFalse();
        assertThat(subjectScope.isSubjectManager(MANAGER, UUID.randomUUID())).isFalse();
        assertThat(subjectQuery.getSubject(UUID.randomUUID())).isEmpty();
        assertThat(subjectQuery.countSubjectsByStatus())
                .containsExactlyInAnyOrderEntriesOf(
                        Map.of(SubjectStatus.ACTIVE, 2L, SubjectStatus.ARCHIVED, 0L));

        Subject reassigned = original.changeManager(OTHER_MANAGER, LATER);
        assertThat(subjectRepository.updateSubject(reassigned, 1)).isTrue();
        assertThat(subjectScope.isSubjectManager(MANAGER, original.id())).isFalse();
        assertThat(subjectScope.isSubjectManager(OTHER_MANAGER, original.id())).isTrue();
        assertThat(subjectQuery.listManagedSubjects(MANAGER)).isEmpty();
        assertThat(subjectScope.listManagedSubjects(MANAGER)).isEmpty();
        assertThat(subjectQuery.listManagedSubjects(OTHER_MANAGER))
                .singleElement()
                .extracting(ref -> ref.managerId())
                .isEqualTo(OTHER_MANAGER);
        assertThat(subjectScope.listManagedSubjects(OTHER_MANAGER)).containsExactly(original.id());

        Subject unassigned = reassigned.changeManager(null, LATER);
        assertThat(subjectRepository.updateSubject(unassigned, 2)).isTrue();
        assertThat(subjectScope.isSubjectManager(OTHER_MANAGER, original.id())).isFalse();
        assertThat(subjectQuery.getSubject(original.id()).orElseThrow().managerId()).isNull();
        assertThat(subjectScope.listManagedSubjects(OTHER_MANAGER)).isEmpty();
        Subject assignedAgain = unassigned.changeManager(MANAGER, LATER);
        assertThat(subjectRepository.updateSubject(assignedAgain, 3)).isTrue();
        assertThat(subjectScope.isSubjectManager(MANAGER, original.id())).isTrue();
        assertThat(subjectRepository.updateSubject(assignedAgain.archive(LATER), 4)).isTrue();
        assertThat(subjectQuery.isActive(original.id())).isFalse();
        assertThat(subjectQuery.getSubject(original.id()).orElseThrow().managerId()).isNull();
        assertThat(subjectScope.isSubjectManager(MANAGER, original.id())).isFalse();
        assertThat(subjectQuery.listManagedSubjects(MANAGER)).isEmpty();
        assertThat(subjectScope.listManagedSubjects(MANAGER)).isEmpty();
        assertThat(subjectQuery.countSubjectsByStatus())
                .containsExactlyInAnyOrderEntriesOf(
                        Map.of(SubjectStatus.ACTIVE, 1L, SubjectStatus.ARCHIVED, 1L));
    }

    @Test
    void databaseRejectsDuplicateCodeAndUnknownManager() {
        subjectRepository.insertSubject(subject("SWR302", "First", null));
        assertThatThrownBy(
                () -> subjectRepository.insertSubject(subject("SWR302", "Duplicate", null)))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(
                        () -> subjectRepository.insertSubject(
                                subject("MISSING", "Missing", UUID.randomUUID())))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(subjectRepository.countSubjects(null, null)).isEqualTo(1);
    }

    @Test
    void databaseRejectsInvalidRawValuesEvenIfDomainValidationIsBypassed() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        for (String code : List.of("lowercase", "A_B", "A".repeat(21))) {
            assertThatThrownBy(
                            () ->
                                    jdbc.update(
                                            "insert into subjects (id, code, name, created_at, updated_at) "
                                                    + "values (?, ?, 'Name', now(), now())",
                                            UUID.randomUUID(),
                                            code))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }
        assertThatThrownBy(
                        () ->
                                jdbc.update(
                                        "insert into subjects (id, code, name, status, created_at, updated_at) "
                                                + "values (?, 'INVALID', 'Name', 'DELETED', now(), now())",
                                        UUID.randomUUID()))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(
                        () ->
                                jdbc.update(
                                        "insert into subjects (id, code, name, created_at, updated_at) "
                                                + "values (?, 'EMPTY', '   ', now(), now())",
                                        UUID.randomUUID()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void appRoleHasReadWriteButNoDeleteAndFlywayDoesNotRunTwice() throws Exception {
        try (Connection connection = dataSource.getConnection();
                Statement sql = connection.createStatement()) {
            sql.execute("set role app");
            try (var permissions =
                    sql.executeQuery(
                            "select has_table_privilege(current_user, 'subjects', 'SELECT'), "
                                    + "has_table_privilege(current_user, 'subjects', 'INSERT'), "
                                    + "has_table_privilege(current_user, 'subjects', 'UPDATE'), "
                                    + "has_table_privilege(current_user, 'subjects', 'DELETE')")) {
                assertThat(permissions.next()).isTrue();
                assertThat(permissions.getBoolean(1)).isTrue();
                assertThat(permissions.getBoolean(2)).isTrue();
                assertThat(permissions.getBoolean(3)).isTrue();
                assertThat(permissions.getBoolean(4)).isFalse();
            }
            assertThatThrownBy(() -> sql.executeUpdate("delete from subjects"))
                    .hasMessageContaining("permission denied");
        }
        assertThat(flyway(dataSource).migrate().migrationsExecuted).isZero();
    }

    @Test
    void updateInCallersTransactionChangesOnlyTargetRowAndLaterReadsSeeIt() {
        Subject a = subject("AAA", "A", null);
        Subject b = subject("BBB", "B", null);
        subjectRepository.insertSubject(a);
        subjectRepository.insertSubject(b);
        new TransactionTemplate(transactionManager)
                .executeWithoutResult(
                        ignored -> {
                            assertThat(subjectRepository.findById(b.id())).contains(b);
                            Subject archived = a.archive(LATER);
                            assertThat(subjectRepository.updateSubject(archived, 1)).isTrue();
                            assertThat(subjectRepository.findById(a.id())).contains(archived);
                            assertThat(subjectRepository.findById(b.id())).contains(b);
                        });
    }

    @Test
    void callerRollbackAlsoRollsBackRepositoryUpdate() {
        Subject original = subject("SWR302", "Original", null);
        subjectRepository.insertSubject(original);
        new TransactionTemplate(transactionManager)
                .executeWithoutResult(
                        tx -> {
                            assertThat(
                                    subjectRepository.updateSubject(
                                            original.archive(LATER), 1)).isTrue();
                            tx.setRollbackOnly();
                        });
        assertThat(subjectRepository.findById(original.id())).contains(original);
    }

    @Test
    void twoConcurrentUpdatesWithOneVersionHaveExactlyOneWinner() throws Exception {
        Subject original = subject("SWR302", "Original", null);
        subjectRepository.insertSubject(original);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService workers = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> first =
                    workers.submit(
                            () ->
                                    updateWhenStarted(
                                            original.updateInformation("First", null, LATER),
                                            ready,
                                            start));
            Future<Boolean> second =
                    workers.submit(
                            () ->
                                    updateWhenStarted(
                                            original.updateInformation("Second", null, LATER),
                                            ready,
                                            start));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            assertThat(List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(true, false);
            Subject saved = subjectRepository.findById(original.id()).orElseThrow();
            assertThat(saved.version()).isEqualTo(2);
            assertThat(saved.name()).isIn("First", "Second");
        } finally {
            start.countDown();
            workers.shutdownNow();
        }
    }

    @Test
    void twoConcurrentInsertsWithOneCodeHaveExactlyOneWinner() throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService workers = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> first =
                    workers.submit(
                            () -> insertWhenStarted(subject("SAME", "First", null), ready, start));
            Future<Boolean> second =
                    workers.submit(
                            () -> insertWhenStarted(subject("SAME", "Second", null), ready, start));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            assertThat(List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(true, false);
            assertThat(subjectRepository.countSubjects(null, null)).isEqualTo(1);
        } finally {
            start.countDown();
            workers.shutdownNow();
        }
    }

    @Test
    void proxiedCommandsCreateAndEditWithRealTransactionsAndPublicVersions() {
        Actor admin = new Actor(UUID.randomUUID(), Role.ADMIN);
        Subject created =
                subjectCommandService.createSubject(
                        admin, new SubjectCommandService.Create(" abc ", " Name ", null, null));
        assertThat(subjectRepository.findById(created.id()).orElseThrow()).isEqualTo(created);
        Subject edited =
                subjectCommandService.updateInformation(
                        admin,
                        created.id(),
                        new SubjectCommandService.UpdateInformation("New", "Description", 1L));
        assertThat(subjectRepository.findById(created.id()).orElseThrow()).isEqualTo(edited);
        assertThat(edited.version()).isEqualTo(2);
        assertThatThrownBy(
                        () ->
                                subjectCommandService.updateInformation(
                                        admin,
                                        created.id(),
                                        new SubjectCommandService.UpdateInformation(
                                                "New", "Description", 1L)))
                .isInstanceOfSatisfying(
                        ApiException.class,
                        error -> assertThat(error.readCode()).isEqualTo("SUBJECT_VERSION_CONFLICT"));
        verify(audit, times(2)).record(any());
    }

    @Test
    void auditExceptionRollsBackBothInsertedAndUpdatedSubjects() {
        Actor admin = new Actor(UUID.randomUUID(), Role.ADMIN);
        Subject before = subject("BEFORE", "Before", null);
        subjectRepository.insertSubject(before);
        doThrow(new IllegalStateException("test audit failure")).when(audit).record(any());
        assertThatThrownBy(
                        () ->
                                subjectCommandService.createSubject(
                                        admin,
                                        new SubjectCommandService.Create(
                                                "ROLLBACK", "Name", null, null)))
                .isInstanceOf(IllegalStateException.class);
        assertThat(subjectRepository.hasSubjectWithCode(new SubjectCode("ROLLBACK"))).isFalse();
        assertThatThrownBy(
                        () ->
                                subjectCommandService.updateInformation(
                                        admin,
                                        before.id(),
                                        new SubjectCommandService.UpdateInformation(
                                                "After", null, 1L)))
                .isInstanceOf(IllegalStateException.class);
        assertThat(subjectRepository.findById(before.id()).orElseThrow()).isEqualTo(before);
    }

    @Test
    void concurrentCreateCommandsHaveOneSuccessAndOneDuplicateConflict() throws Exception {
        Actor admin = new Actor(UUID.randomUUID(), Role.ADMIN);
        CountDownLatch ready = new CountDownLatch(2), start = new CountDownLatch(1);
        ExecutorService workers = Executors.newFixedThreadPool(2);
        java.util.concurrent.Callable<String> create =
                () -> {
                    ready.countDown();
                    if (!start.await(5, TimeUnit.SECONDS))
                        throw new IllegalStateException("Test barrier timeout");
                    try {
                        subjectCommandService.createSubject(
                                admin,
                                new SubjectCommandService.Create("CMD-RACE", "Name", null, null));
                        return "SUCCESS";
                    } catch (ApiException error) {
                        return error.readCode();
                    }
                };
        try {
            Future<String> first = workers.submit(create), second = workers.submit(create);
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            assertThat(List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder("SUCCESS", "SUBJECT_CODE_DUPLICATE");
            assertThat(subjectRepository.countSubjects(null, null)).isEqualTo(1);
            verify(audit, times(1)).record(any());
        } finally {
            start.countDown();
            workers.shutdownNow();
        }
    }

    @Test
    void managerCommandsUseFixtureAccountLocksAndRollbackAssignmentOnAuditFailure() {
        Actor admin = new Actor(UUID.randomUUID(), Role.ADMIN);
        Subject created =
                subjectCommandService.createSubject(
                        admin, new SubjectCommandService.Create("MANAGED", "Name", null, MANAGER));
        Subject reassigned =
                subjectCommandService.changeManager(
                        admin,
                        created.id(),
                        new SubjectCommandService.ChangeManager(OTHER_MANAGER, 1L));
        assertThat(subjectScope.isSubjectManager(MANAGER, created.id())).isFalse();
        assertThat(subjectScope.isSubjectManager(OTHER_MANAGER, created.id())).isTrue();
        doThrow(new IllegalStateException("test audit failure")).when(audit).record(any());
        assertThatThrownBy(
                        () ->
                                subjectCommandService.changeManager(
                                        admin,
                                        created.id(),
                                        new SubjectCommandService.ChangeManager(null, 2L)))
                .isInstanceOf(IllegalStateException.class);
        assertThat(subjectRepository.findById(created.id()).orElseThrow()).isEqualTo(reassigned);
        reset(audit);
        Subject unassigned =
                subjectCommandService.changeManager(
                        admin, created.id(), new SubjectCommandService.ChangeManager(null, 2L));
        assertThat(unassigned.managerId()).isNull();
        assertThat(unassigned.version()).isEqualTo(3);
        assertThat(subjectScope.isSubjectManager(OTHER_MANAGER, created.id())).isFalse();
    }

    @Test
    void subjectLockRequiresCallerTransactionAndHoldsUntilItEnds() {
        Subject original = subject("LOCKED", "Name", null);
        subjectRepository.insertSubject(original);
        assertThatThrownBy(() -> subjectRepository.findByIdForUpdate(original.id()))
                .isInstanceOf(
                        org.springframework.transaction.IllegalTransactionStateException.class);
        new TransactionTemplate(transactionManager)
                .executeWithoutResult(
                        status -> {
                            assertThat(subjectRepository.findByIdForUpdate(original.id()))
                                    .contains(original);
                            // Kết nối độc lập: khóa PostgreSQL phải còn đến hết
                            // transaction caller.
                            try (Connection contender = dataSource.getConnection();
                                    Statement sql = contender.createStatement()) {
                                contender.setAutoCommit(false);
                                sql.execute("set local lock_timeout = '200ms'");
                                assertThatThrownBy(
                                                () ->
                                                        sql.executeQuery(
                                                                "select id from subjects where id = '"
                                                                        + original.id()
                                                                        + "' for update"))
                                        .isInstanceOfSatisfying(
                                                java.sql.SQLException.class,
                                                error ->
                                                        assertThat(error.getSQLState())
                                                                .isEqualTo("55P03"));
                                contender.rollback();
                            } catch (java.sql.SQLException failure) {
                                throw new IllegalStateException(failure);
                            }
                        });
        new TransactionTemplate(transactionManager)
                .executeWithoutResult(
                        status ->
                                assertThat(subjectRepository.findByIdForUpdate(original.id()))
                                        .contains(original));
    }

    @Test
    void archiveAndReopenKeepClassHistoryWithoutRestoringManager() {
        Subject original = subject("LIFE", "Lifecycle", MANAGER);
        subjectRepository.insertSubject(original);
        insertFixtureClass(original.id(), "ARCHIVED");
        Actor admin = new Actor(UUID.randomUUID(), Role.ADMIN);
        Subject archived =
                subjectStatusService.changeStatus(
                        admin,
                        original.id(),
                        new SubjectStatusService.ChangeStatus(SubjectStatus.ARCHIVED, 1L));
        assertThat(archived.version()).isEqualTo(2);
        assertThat(subjectQuery.isActive(original.id())).isFalse();
        assertThat(subjectScope.isSubjectManager(MANAGER, original.id())).isFalse();
        Subject reopened =
                subjectStatusService.changeStatus(
                        admin,
                        original.id(),
                        new SubjectStatusService.ChangeStatus(SubjectStatus.ACTIVE, 2L));
        assertThat(reopened.version()).isEqualTo(3);
        assertThat(reopened.managerId()).isNull();
        assertThat(
                        new JdbcTemplate(dataSource)
                                .queryForObject(
                                        "select status from u04_lifecycle_classes where subject_id = ?",
                                        String.class,
                                        original.id()))
                .isEqualTo("ARCHIVED");
        verify(archiveChecker, times(1)).archiveClassesOfSubject(eq(original.id()), any());
        verify(audit, times(3)).record(any());
    }

    @Test
    void archiveAlsoArchivesOpenAndDraftFixtures() {
        Subject original = subject("BLOCKED", "Name", null);
        subjectRepository.insertSubject(original);
        insertFixtureClass(original.id(), "OPEN");
        insertFixtureClass(original.id(), "DRAFT");
        Subject archived =
                subjectStatusService.changeStatus(
                        new Actor(UUID.randomUUID(), Role.ADMIN),
                        original.id(),
                        new SubjectStatusService.ChangeStatus(SubjectStatus.ARCHIVED, 1L));
        assertThat(archived.status()).isEqualTo(SubjectStatus.ARCHIVED);
        new TransactionTemplate(transactionManager)
                .executeWithoutResult(tx -> assertThat(fixtureBlockers(original.id())).isEmpty());
        assertThat(
                        new JdbcTemplate(dataSource)
                                .queryForList(
                                        "select status from u04_lifecycle_classes where subject_id = ?",
                                        String.class,
                                        original.id()))
                .containsExactlyInAnyOrder("ARCHIVED", "ARCHIVED");
    }

    @Test
    void statusAuditFailureRollsBackArchiveAndReopen() {
        Subject original = subject("AUDIT-LIFE", "Name", null);
        subjectRepository.insertSubject(original);
        Actor admin = new Actor(UUID.randomUUID(), Role.ADMIN);
        doThrow(new IllegalStateException("audit failure")).when(audit).record(any());
        assertThatThrownBy(
                        () ->
                                subjectStatusService.changeStatus(
                                        admin,
                                        original.id(),
                                        new SubjectStatusService.ChangeStatus(
                                                SubjectStatus.ARCHIVED, 1L)))
                .isInstanceOf(IllegalStateException.class);
        assertThat(subjectRepository.findById(original.id())).contains(original);
        reset(audit);
        Subject archived =
                subjectStatusService.changeStatus(
                        admin,
                        original.id(),
                        new SubjectStatusService.ChangeStatus(SubjectStatus.ARCHIVED, 1L));
        doThrow(new IllegalStateException("audit failure")).when(audit).record(any());
        assertThatThrownBy(
                        () ->
                                subjectStatusService.changeStatus(
                                        admin,
                                        original.id(),
                                        new SubjectStatusService.ChangeStatus(
                                                SubjectStatus.ACTIVE, 2L)))
                .isInstanceOf(IllegalStateException.class);
        assertThat(subjectRepository.findById(original.id())).contains(archived);
    }

    @Test
    void boundedLockRestoresExistingTimeoutsAndRejectsRepeatableRead() {
        Subject original = subject("BOUNDED", "Name", null);
        subjectRepository.insertSubject(original);
        assertThatThrownBy(() -> mutationGuard.lockActive(original.id(), Duration.ofSeconds(1)))
                .isInstanceOf(
                        org.springframework.transaction.IllegalTransactionStateException.class);
        new TransactionTemplate(transactionManager)
                .executeWithoutResult(
                        status -> {
                            JdbcTemplate jdbc = new JdbcTemplate(dataSource);
                            jdbc.execute("set local lock_timeout = '800ms'");
                            jdbc.execute("set local statement_timeout = '900ms'");
                            mutationGuard.lockActive(original.id(), Duration.ofMillis(500));
                            assertThat(
                                            jdbc.queryForObject(
                                                    "select current_setting('lock_timeout')",
                                                    String.class))
                                    .isEqualTo("800ms");
                            assertThat(
                                            jdbc.queryForObject(
                                                    "select current_setting('statement_timeout')",
                                                    String.class))
                                    .isEqualTo("900ms");
                        });
        TransactionTemplate repeatableRead = new TransactionTemplate(transactionManager);
        repeatableRead.setIsolationLevel(
                org.springframework.transaction.TransactionDefinition.ISOLATION_REPEATABLE_READ);
        assertThatThrownBy(
                        () ->
                                repeatableRead.executeWithoutResult(
                                        status ->
                                                mutationGuard.lockActive(
                                                        original.id(), Duration.ofSeconds(1))))
                .isInstanceOfSatisfying(
                        ApiException.class,
                        error ->
                                assertThat(error.readCode())
                                        .isEqualTo("SUBJECT_ISOLATION_UNSUPPORTED"));
    }

    private boolean tryLockFromOtherConnection(UUID subjectId, String lockClause) {
        try (Connection contender = dataSource.getConnection();
                java.sql.PreparedStatement sql =
                        contender.prepareStatement(
                                "select id from subjects where id = ? " + lockClause + " nowait")) {
            contender.setAutoCommit(false);
            sql.setObject(1, subjectId);
            sql.executeQuery().close();
            contender.rollback();
            return true;
        } catch (java.sql.SQLException failure) {
            if ("55P03".equals(failure.getSQLState())) {
                return false;
            }
            throw new IllegalStateException(failure);
        }
    }

    @Test
    void subjectRowLockAllowsKeyShareButBlocksOtherWriters() {
        Subject original = subject("KEYSHARE", "Name", null);
        subjectRepository.insertSubject(original);
        new TransactionTemplate(transactionManager)
                .executeWithoutResult(
                        status -> {
                            assertThat(subjectRepository.findByIdForUpdate(original.id()))
                                    .contains(original);
                            assertThat(tryLockFromOtherConnection(original.id(), "for key share"))
                                    .isTrue();
                            assertThat(
                                            tryLockFromOtherConnection(
                                                    original.id(), "for no key update"))
                                    .isFalse();
                        });
        new TransactionTemplate(transactionManager)
                .executeWithoutResult(
                        status -> {
                            assertThat(
                                            subjectRepository.findByIdForUpdate(
                                                    original.id(), Duration.ofMillis(500)))
                                    .contains(original);
                            assertThat(tryLockFromOtherConnection(original.id(), "for key share"))
                                    .isTrue();
                            assertThat(
                                            tryLockFromOtherConnection(
                                                    original.id(), "for no key update"))
                                    .isFalse();
                        });
    }

    @Test
    void boundedGuardTimesOutOnActualDatabaseRowLock() throws Exception {
        Subject original = subject("BUSY", "Name", null);
        subjectRepository.insertSubject(original);
        ExecutorService worker = Executors.newSingleThreadExecutor();
        try {
            new TransactionTemplate(transactionManager)
                    .executeWithoutResult(
                            status -> {
                                subjectRepository.findByIdForUpdate(original.id());
                                Future<String> contender =
                                        worker.submit(
                                                () -> {
                                                    try {
                                                        new TransactionTemplate(transactionManager)
                                                                .executeWithoutResult(
                                                                        tx ->
                                                                                mutationGuard
                                                                                        .lockActive(
                                                                                                original
                                                                                                        .id(),
                                                                                                Duration
                                                                                                        .ofMillis(
                                                                                                                150)));
                                                        return "UNEXPECTED_SUCCESS";
                                                    } catch (ApiException expected) {
                                                        return expected.readCode();
                                                    }
                                                });
                                try {
                                    assertThat(contender.get(5, TimeUnit.SECONDS))
                                            .isEqualTo("SUBJECT_LIFECYCLE_UNAVAILABLE");
                                } catch (Exception failure) {
                                    throw new IllegalStateException(failure);
                                }
                            });
        } finally {
            worker.shutdownNow();
        }
    }

    @Test
    void providerStatementTimeoutRollsBackArchive() {
        Subject original = subject("SLOW", "Name", null);
        subjectRepository.insertSubject(original);
        doAnswer(
                        call -> {
                            JdbcTemplate jdbc = new JdbcTemplate(dataSource);
                            jdbc.execute("set local statement_timeout = '50ms'");
                            jdbc.execute("select pg_sleep(0.2)");
                            return List.of();
                        })
                .when(archiveChecker)
                .archiveClassesOfSubject(eq(original.id()), any());
        assertThatThrownBy(
                        () ->
                                subjectStatusService.changeStatus(
                                        new Actor(UUID.randomUUID(), Role.ADMIN),
                                        original.id(),
                                        new SubjectStatusService.ChangeStatus(
                                                SubjectStatus.ARCHIVED, 1L)))
                .isInstanceOfSatisfying(
                        ApiException.class,
                        error -> assertThat(error.readCode()).isEqualTo("SUBJECT_CLASSES_UNAVAILABLE"));
        assertThat(subjectRepository.findById(original.id())).contains(original);
        verifyNoInteractions(audit);
    }

    @Test
    void classWriterHoldingGuardFirstMakesArchiveSeeCommittedOpenClass() throws Exception {
        Subject original = subject("WRITER-FIRST", "Name", null);
        subjectRepository.insertSubject(original);
        CountDownLatch writerLocked = new CountDownLatch(1),
                allowWriterCommit = new CountDownLatch(1);
        ExecutorService workers = Executors.newFixedThreadPool(2);
        try {
            Future<?> writer =
                    workers.submit(
                            () ->
                                    new TransactionTemplate(transactionManager)
                                            .executeWithoutResult(
                                                    tx -> {
                                                        mutationGuard.lockActive(
                                                                original.id(),
                                                                Duration.ofSeconds(1));
                                                        insertFixtureClass(original.id(), "OPEN");
                                                        writerLocked.countDown();
                                                        awaitFixture(allowWriterCommit);
                                                    }));
            assertThat(writerLocked.await(5, TimeUnit.SECONDS)).isTrue();
            Future<String> archive = workers.submit(() -> archiveOutcome(original.id()));
            allowWriterCommit.countDown();
            writer.get(5, TimeUnit.SECONDS);
            assertThat(archive.get(5, TimeUnit.SECONDS)).isEqualTo("SUCCESS");
            Subject archived = subjectRepository.findById(original.id()).orElseThrow();
            assertThat(archived.status()).isEqualTo(SubjectStatus.ARCHIVED);
            assertThat(archived.version()).isEqualTo(2);
            assertThat(
                            new JdbcTemplate(dataSource)
                                    .queryForList(
                                            "select status from u04_lifecycle_classes where subject_id = ?",
                                            String.class,
                                            original.id()))
                    .containsExactly("ARCHIVED");
            verify(audit).record(argThat(event -> event.action().equals("SUBJECT_ARCHIVED")));
        } finally {
            allowWriterCommit.countDown();
            workers.shutdownNow();
        }
    }

    @Test
    void archiveHoldingGuardFirstMakesLaterClassWriterRejectArchivedSubject() throws Exception {
        Subject original = subject("ARCHIVE-FIRST", "Name", null);
        subjectRepository.insertSubject(original);
        CountDownLatch archiveLocked = new CountDownLatch(1), allowArchive = new CountDownLatch(1);
        doAnswer(
                        call -> {
                            archiveLocked.countDown();
                            awaitFixture(allowArchive);
                            return fixtureArchiveAll(original.id());
                        })
                .when(archiveChecker)
                .archiveClassesOfSubject(eq(original.id()), any());
        ExecutorService workers = Executors.newFixedThreadPool(2);
        try {
            Future<String> archive = workers.submit(() -> archiveOutcome(original.id()));
            assertThat(archiveLocked.await(5, TimeUnit.SECONDS)).isTrue();
            Future<String> writer =
                    workers.submit(
                            () -> {
                                try {
                                    new TransactionTemplate(transactionManager)
                                            .executeWithoutResult(
                                                    tx -> {
                                                        mutationGuard.lockActive(
                                                                original.id(),
                                                                Duration.ofSeconds(1));
                                                        insertFixtureClass(original.id(), "OPEN");
                                                    });
                                    return "UNEXPECTED_SUCCESS";
                                } catch (ApiException expected) {
                                    return expected.readCode();
                                }
                            });
            allowArchive.countDown();
            assertThat(archive.get(5, TimeUnit.SECONDS)).isEqualTo("SUCCESS");
            assertThat(writer.get(5, TimeUnit.SECONDS)).isEqualTo("SUBJECT_ARCHIVED");
            assertThat(
                            new JdbcTemplate(dataSource)
                                    .queryForObject(
                                            "select count(*) from u04_lifecycle_classes where subject_id = ?",
                                            Integer.class,
                                            original.id()))
                    .isZero();
        } finally {
            allowArchive.countDown();
            workers.shutdownNow();
        }
    }

    private void insertFixtureClass(UUID subjectId, String status) {
        new JdbcTemplate(dataSource)
                .update(
                        "insert into u04_lifecycle_classes (id, subject_id, status) values (?, ?, ?)",
                        UUID.randomUUID(),
                        subjectId,
                        status);
    }

    /** Thay cho ISubjectClasses của U05: đọc lớp chưa lưu trữ cùng transaction; không phải code U05. */
    /** Thay cho U05: lưu trữ mọi lớp chưa lưu trữ của môn trong cùng transaction. */
    private List<UUID> fixtureArchiveAll(UUID subjectId) {
        assertThat(
                        org.springframework.transaction.support.TransactionSynchronizationManager
                                .isActualTransactionActive())
                .isTrue();
        return new JdbcTemplate(dataSource)
                .queryForList(
                        "update u04_lifecycle_classes set status = 'ARCHIVED' where subject_id = ? and status <> 'ARCHIVED' returning id",
                        UUID.class,
                        subjectId);
    }

    private List<SubjectClassSummary> fixtureBlockers(UUID subjectId) {
        assertThat(
                        org.springframework.transaction.support.TransactionSynchronizationManager
                                .isActualTransactionActive())
                .isTrue();
        return new JdbcTemplate(dataSource)
                .query(
                        "select id, status from u04_lifecycle_classes where subject_id = ? and status <> 'ARCHIVED'",
                        (row, index) ->
                                new SubjectClassSummary(
                                        row.getObject("id", UUID.class),
                                        "FIXTURE",
                                        null,
                                        "Fixture class",
                                        row.getString("status"),
                                        null,
                                        0),
                        subjectId);
    }

    private void awaitFixture(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS))
                throw new IllegalStateException("Fixture barrier timeout");
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(interrupted);
        }
    }

    private String archiveOutcome(UUID subjectId) {
        try {
            subjectStatusService.changeStatus(
                    new Actor(UUID.randomUUID(), Role.ADMIN),
                    subjectId,
                    new SubjectStatusService.ChangeStatus(SubjectStatus.ARCHIVED, 1L));
            return "SUCCESS";
        } catch (ApiException expected) {
            return expected.readCode();
        }
    }

    @Test
    void deniedAuditSurvivesCommandRollbackUsingIndependentFixtureTransaction() {
        doAnswer(
                        call -> {
                            var event =
                                    call.getArgument(
                                            0, edu.aiplatform.audit.contract.AuditEvent.class);
                            assertThat(event.result())
                                    .isEqualTo(edu.aiplatform.audit.contract.AuditResult.DENIED);
                            // Recorder NOT_SUPPORTED đã tạm dừng command transaction; provider U02
                            // phải mở tx riêng.
                            assertThat(
                                            org.springframework.transaction.support
                                                    .TransactionSynchronizationManager
                                                    .isActualTransactionActive())
                                    .isFalse();
                            TransactionTemplate independent =
                                    new TransactionTemplate(transactionManager);
                            independent.setPropagationBehavior(
                                    org.springframework.transaction.TransactionDefinition
                                            .PROPAGATION_REQUIRES_NEW);
                            independent.executeWithoutResult(tx -> insertAuditFixture(event));
                            return null;
                        })
                .when(audit)
                .record(any());
        Actor student = new Actor(UUID.randomUUID(), Role.STUDENT);
        assertThatThrownBy(
                        () ->
                                new TransactionTemplate(transactionManager)
                                        .executeWithoutResult(
                                                outer ->
                                                        subjectCommandService.createSubject(
                                                                student,
                                                                new SubjectCommandService.Create(
                                                                        "DENIED", "Name", null,
                                                                        null))))
                .isInstanceOfSatisfying(
                        ApiException.class,
                        error -> assertThat(error.readCode()).isEqualTo("SUBJECT_FORBIDDEN"));
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from u04_audit_events where action = 'ACCESS_DENIED' and result = 'DENIED'",
                                Integer.class))
                .isEqualTo(1);
        assertThat(subjectRepository.countSubjects(null, null)).isZero();
        verify(audit, times(1)).record(any());
    }

    @Test
    void successAuditFixtureCommitsAndRollsBackTogetherWithSubject() {
        doAnswer(
                        call -> {
                            assertThat(
                                            org.springframework.transaction.support
                                                    .TransactionSynchronizationManager
                                                    .isActualTransactionActive())
                                    .isTrue();
                            insertAuditFixture(call.getArgument(0));
                            return null;
                        })
                .when(audit)
                .record(any());
        Actor admin = new Actor(UUID.randomUUID(), Role.ADMIN);
        Subject committed =
                subjectCommandService.createSubject(
                        admin,
                        new SubjectCommandService.Create("AUDIT-COMMIT", "Name", null, null));
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from u04_audit_events where object_id = ?",
                                Integer.class,
                                committed.id().toString()))
                .isEqualTo(1);
        doAnswer(
                        call -> {
                            insertAuditFixture(call.getArgument(0));
                            throw new IllegalStateException("fixture failure after audit insert");
                        })
                .when(audit)
                .record(any());
        assertThatThrownBy(
                        () ->
                                subjectCommandService.createSubject(
                                        admin,
                                        new SubjectCommandService.Create(
                                                "AUDIT-ROLLBACK", "Name", null, null)))
                .isInstanceOf(IllegalStateException.class);
        assertThat(
                subjectRepository.hasSubjectWithCode(new SubjectCode("AUDIT-ROLLBACK"))).isFalse();
        assertThat(jdbc.queryForObject("select count(*) from u04_audit_events", Integer.class))
                .isEqualTo(1);
    }

    private void insertAuditFixture(edu.aiplatform.audit.contract.AuditEvent event) {
        new JdbcTemplate(dataSource)
                .update(
                        "insert into u04_audit_events (id, action, result, object_id) values (?, ?, ?, ?)",
                        event.id(),
                        event.action(),
                        event.result().name(),
                        event.objectId());
    }

    private boolean updateWhenStarted(Subject updated, CountDownLatch ready, CountDownLatch start)
            throws InterruptedException {
        ready.countDown();
        if (!start.await(5, TimeUnit.SECONDS))
            throw new IllegalStateException("Hết thời gian chờ test");
        return subjectRepository.updateSubject(updated, 1);
    }

    private boolean insertWhenStarted(Subject subject, CountDownLatch ready, CountDownLatch start)
            throws InterruptedException {
        ready.countDown();
        if (!start.await(5, TimeUnit.SECONDS))
            throw new IllegalStateException("Hết thời gian chờ test");
        try {
            subjectRepository.insertSubject(subject);
            return true;
        } catch (DataIntegrityViolationException expectedDuplicate) {
            return false;
        }
    }
}
