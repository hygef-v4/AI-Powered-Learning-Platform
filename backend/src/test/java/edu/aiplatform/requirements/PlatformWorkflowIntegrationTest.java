package edu.aiplatform.requirements;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.academics.contract.*;
import edu.aiplatform.academics.service.AcademicsService;
import edu.aiplatform.academics.model.EnrollmentStatus;
import edu.aiplatform.admindashboard.service.AdminDashboardService;
import edu.aiplatform.ai.contract.AiProvider;
import edu.aiplatform.ai.contract.AiSource;
import edu.aiplatform.ai.contract.AiTaskRequest;
import edu.aiplatform.ai.contract.IAiTask;
import edu.aiplatform.ai.model.McpCredential.Access;
import edu.aiplatform.ai.service.AiConnectionService;
import edu.aiplatform.ai.service.McpCredentialService;
import edu.aiplatform.audit.contract.*;
import edu.aiplatform.identity.contract.*;
import edu.aiplatform.identity.integration.JwtService;
import edu.aiplatform.identity.integration.SessionStore;
import edu.aiplatform.jobs.contract.*;
import edu.aiplatform.settings.service.SettingsService;
import edu.aiplatform.shared.error.ApiException;
import edu.aiplatform.assignments.contract.*;
import edu.aiplatform.assignments.service.AssignmentAuthoringService;
import edu.aiplatform.assignments.service.AssignmentPublicationService;
import edu.aiplatform.assignments.service.AssignmentCommands;
import edu.aiplatform.assignments.dto.AssignmentViews;
import edu.aiplatform.assignmentwork.service.AttemptService;
import edu.aiplatform.groups.service.ClassGroupService;
import edu.aiplatform.groups.model.GroupSetRules.Draft;
import edu.aiplatform.quizzes.service.QuizAuthoringService;
import edu.aiplatform.quizzes.service.QuizCopyService;
import edu.aiplatform.quizzes.service.QuizPublicationService;
import edu.aiplatform.quizzes.model.*;
import edu.aiplatform.quizzes.contract.QuestionType;
import edu.aiplatform.quizpractice.service.QuizAttemptService;
import edu.aiplatform.settings.contract.ISettings;
import edu.aiplatform.files.model.PurposePolicy;
import jakarta.servlet.http.Cookie;
import java.nio.file.*;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.math.BigDecimal;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import edu.aiplatform.ai.service.QuotaPolicyService;
import edu.aiplatform.ai.service.QuotaReservationService;
import edu.aiplatform.grading.model.GradingProposal;
import edu.aiplatform.grading.service.*;
import edu.aiplatform.grading.repository.IEvaluationRepository;
import edu.aiplatform.groupdocs.service.*;
import edu.aiplatform.notifications.service.NotificationEventService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;
import org.springframework.amqp.core.AcknowledgeMode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.utility.DockerImageName;

/** Real production composition, migrations, PostgreSQL, Redis and Rabbit; no remote provider calls. */
@SpringBootTest(classes = PlatformWorkflowIntegrationTest.Application.class, properties = {
        "platform.identity.bcrypt-cost=4", "platform.identity.mail-from=integration@example.test",
        "platform.identity.bootstrap-admin-email=", "platform.identity.mail-transport=smtp",
        "platform.files.require-drive=false", "platform.files.drive-service-account-key=",
        "platform.files.shared-drive-id=", "platform.backup.folder-id=",
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "spring.rabbitmq.listener.direct.auto-startup=false",
        "spring.main.allow-bean-definition-overriding=false"
})
@AutoConfigureMockMvc
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PlatformWorkflowIntegrationTest {
    static final String JWT_SECRET = UUID.randomUUID().toString() + UUID.randomUUID();
    static final String FILE_SECRET = UUID.randomUUID().toString() + UUID.randomUUID();
    static final String ENCRYPTION_KEY = Base64.getEncoder().encodeToString(
            UUID.randomUUID().toString().replace("-", "").getBytes(java.nio.charset.StandardCharsets.UTF_8));
    @Configuration(proxyBeanMethods = false)
    @AutoConfigurationPackage(basePackages = "edu.aiplatform")
    @EnableAutoConfiguration(exclude = UserDetailsServiceAutoConfiguration.class)
    @ComponentScan(basePackages = "edu.aiplatform", excludeFilters = {
            @ComponentScan.Filter(type = FilterType.REGEX, pattern = ".*Test(?:Config|Configuration)?(?:\\$.*)?$"),
            @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = edu.aiplatform.PlatformApplication.class)
    })
    static class Application {
        // Scan production beans while excluding other suites' manually registered test configurations.
        @Bean @Primary
        RecordingMail recordingMail() { return new RecordingMail(); }
        // Quota tests freeze one business day; other scenarios keep live time aligned with their fixtures.
        @Bean @Primary
        WorkflowClock workflowClock() { return new WorkflowClock(); }
    }

    static class WorkflowClock extends Clock {
        final AtomicReference<Instant> frozen;
        final ZoneId zone;
        WorkflowClock() { this(new AtomicReference<>(), ZoneOffset.UTC); }
        private WorkflowClock(AtomicReference<Instant> frozen, ZoneId zone) {
            this.frozen = frozen;
            this.zone = zone;
        }
        @Override public ZoneId getZone() { return zone; }
        @Override public Clock withZone(ZoneId zone) { return new WorkflowClock(frozen, zone); }
        @Override public Instant instant() {
            Instant fixed = frozen.get();
            return fixed == null ? Instant.now() : fixed;
        }
        void freeze() { frozen.set(Instant.now()); }
        void reset() { frozen.set(null); }
    }

    /** Only the external mail provider is controlled; delivery state and queue processing stay real. */
    static class RecordingMail implements IMail {
        final AtomicInteger attempts = new AtomicInteger();
        final AtomicInteger failuresRemaining = new AtomicInteger();
        final List<MailMessage> accepted = new CopyOnWriteArrayList<>();
        public void send(MailMessage message) {
            attempts.incrementAndGet();
            if (failuresRemaining.getAndUpdate(n -> Math.max(0, n - 1)) > 0)
                throw new IllegalStateException("INTEGRATION_PROVIDER_UNAVAILABLE");
            accepted.add(message);
        }
        void reset() { attempts.set(0); failuresRemaining.set(0); accepted.clear(); }
    }
    @Container
    static final PostgreSQLContainer<?> DATABASE = new PostgreSQLContainer<>("postgres:16-alpine")
            .withInitScript("requirements/platform-roles.sql")
            .withPassword(UUID.randomUUID().toString());
    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7.4-alpine"))
            .withExposedPorts(6379);
    @Container
    static final GenericContainer<?> BROKER = new GenericContainer<>(
            DockerImageName.parse("rabbitmq:4.1-management-alpine"))
            .withEnv("RABBITMQ_DEFAULT_USER", "integration")
            .withEnv("RABBITMQ_DEFAULT_PASS", UUID.randomUUID().toString())
            .withExposedPorts(5672);
    static final Path FILES = createDirectory();

    static Path createDirectory() {
        try {
            return Files.createTempDirectory("platform-integration-");
        } catch (java.io.IOException exception) {
            throw new java.io.UncheckedIOException(exception);
        }
    }

    @DynamicPropertySource
    static void configureServices(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", DATABASE::getJdbcUrl);
        properties.add("spring.datasource.username", DATABASE::getUsername);
        properties.add("spring.datasource.password", DATABASE::getPassword);
        properties.add("spring.flyway.url", DATABASE::getJdbcUrl);
        properties.add("spring.flyway.user", DATABASE::getUsername);
        properties.add("spring.flyway.password", DATABASE::getPassword);
        properties.add("spring.data.redis.host", REDIS::getHost);
        properties.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        properties.add("spring.data.redis.password", () -> "");
        properties.add("spring.rabbitmq.host", BROKER::getHost);
        properties.add("spring.rabbitmq.port", () -> BROKER.getMappedPort(5672));
        properties.add("spring.rabbitmq.username", () -> BROKER.getEnvMap().get("RABBITMQ_DEFAULT_USER"));
        properties.add("spring.rabbitmq.password", () -> BROKER.getEnvMap().get("RABBITMQ_DEFAULT_PASS"));
        properties.add("spring.rabbitmq.virtual-host", () -> "/");
        properties.add("platform.identity.jwt-secret", () -> JWT_SECRET);
        properties.add("platform.ai.key-encryption-key", () -> ENCRYPTION_KEY);
        properties.add("platform.files.fileref-secret", () -> FILE_SECRET);
        properties.add("platform.files.local-storage-dir", FILES::toString);
        properties.add("platform.backup.local-dir", () -> FILES.resolve("backups").toString());
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;
    @Autowired TransactionTemplate transaction;
    @Autowired IAudit audit;
    @Autowired SessionStore sessionStore;
    @Autowired JwtService jwtService;
    @Autowired MockMvc mvc;
    @Autowired AcademicsService academicsService;
    @Autowired SettingsService settingsService;
    @Autowired IJob job;
    @Autowired RabbitTemplate rabbitTemplate;
    @Autowired edu.aiplatform.jobs.service.JobDispatcher jobDispatcher;
    @Autowired AiConnectionService aiConnectionService;
    @Autowired McpCredentialService mcpCredentialService;
    @Autowired AdminDashboardService adminDashboardService;
    @Autowired ISettings settings;
    @Autowired AssignmentAuthoringService assignmentAuthoringService;
    @Autowired AssignmentPublicationService assignmentPublicationService;
    @Autowired AttemptService attemptService;
    @Autowired QuizAuthoringService quizAuthoringService;
    @Autowired QuizCopyService quizCopyService;
    @Autowired QuizPublicationService quizPublicationService;
    @Autowired QuizAttemptService quizAttemptService;
    @Autowired edu.aiplatform.quizzes.service.QuizAiDraftService quizDrafts;
    @Autowired edu.aiplatform.quizzes.service.QuizDraftResultHandler quizDraftResults;
    @Autowired ClassGroupService classGroupService;
    @Autowired GradingService gradingService;
    @Autowired GradingProposalService gradingProposalService;
    @Autowired GradingProposalTransactions gradingProposalTransactions;
    @Autowired IAiTask aiTask;
    @Autowired GradebookService gradebookService;
    @Autowired IEvaluationRepository evaluationRepository;
    @Autowired GroupWorkspaceService groupWorkspaceService;
    @Autowired GroupSectionService groupSectionService;
    @Autowired GroupSubmissionService groupSubmissionService;
    @Autowired NotificationEventService notificationEventService;
    @Autowired RecordingMail recordingMail;
    @Autowired QuotaPolicyService quotaPolicyService;
    @Autowired QuotaReservationService quotaReservationService;
    @Autowired WorkflowClock workflowClock;
    final AtomicInteger handledEmailDeliveries = new AtomicInteger();
    final Map<Role, Actor> actors = new EnumMap<>(Role.class);
    UUID subjectId;
    UUID classId;

    @BeforeEach
    void seedIndependentFixture() {
        workflowClock.reset();
        recordingMail.reset();
        handledEmailDeliveries.set(0);
        String tables = jdbc.queryForObject("select string_agg(quote_ident(tablename), ',') from pg_tables "
                + "where schemaname='public' and tablename not in ('flyway_schema_history','system_settings')",
                String.class);
        jdbc.execute("truncate " + tables + " cascade");
        actors.clear();
        Instant now = Instant.now();
        for (Role role : Role.values()) {
            Actor actor = new Actor(UUID.randomUUID(), role);
            actors.put(role, actor);
            jdbc.update("""
                    insert into accounts(id,school_email,role,status,password_hash,display_name,created_at,updated_at,version)
                    values (?,?,?,'ACTIVE','fixture-hash',?,?,?,0)
                    """, actor.accountId(), actor.accountId() + "@example.test", role.name(), role.name(),
                    Timestamp.from(now), Timestamp.from(now));
        }
        subjectId = UUID.randomUUID();
        classId = UUID.randomUUID();
        jdbc.update("""
                insert into subjects(id,code,name,status,manager_id,created_at,updated_at)
                values (?,'INTEGRATION','Integration subject','ACTIVE',?,?,?)
                """, subjectId, actor(Role.SUBJECT_MANAGER).accountId(), Timestamp.from(now), Timestamp.from(now));
        jdbc.update("""
                insert into course_classes(id,subject_id,code,name,status,teacher_id,starts_on,created_at,updated_at)
                values (?,?,'INTEGRATION','Integration class','OPEN',?,DATE '2026-11-14',?,?)
                """, classId, subjectId, actor(Role.TEACHER).accountId(), Timestamp.from(now), Timestamp.from(now));
        jdbc.update("""
                insert into enrollments(class_id,account_id,status,source,enrolled_at)
                values (?,?,'ACTIVE','MANUAL',?)
                """, classId, actor(Role.STUDENT).accountId(), Timestamp.from(now));
        while (rabbitTemplate.receive("jobs.email") != null) {
            // No worker consumes this disposable queue; discard only this suite's prior fixture messages.
        }
    }

    Actor actor(Role role) {
        return actors.get(role);
    }

    long count(String table) {
        return jdbc.queryForObject("select count(*) from " + table, Long.class);
    }

    AuditEvent event(UUID id, AuditResult result) {
        return new AuditEvent(id, actor(Role.ADMIN).accountId(), "INTEGRATION_CHECK", "SUBJECT",
                subjectId.toString(), result, null, Map.of("description", "Integration fixture"), Instant.now());
    }

    @Test
    void auditSuccessCommitsOnceAndRollsBackWithCaller() {
        UUID committed = UUID.randomUUID();
        transaction.executeWithoutResult(status -> {
            audit.record(event(committed, AuditResult.SUCCESS));
            audit.record(event(committed, AuditResult.SUCCESS));
        });
        assertThat(count("audit_logs")).isEqualTo(1);
        transaction.executeWithoutResult(status -> {
            audit.record(event(UUID.randomUUID(), AuditResult.SUCCESS));
            status.setRollbackOnly();
        });
        assertThat(count("audit_logs")).isEqualTo(1);
    }

    @Test
    void deniedAuditSurvivesCallerRollbackAndSensitiveDetailsAreRejected() {
        transaction.executeWithoutResult(status -> {
            audit.record(event(UUID.randomUUID(), AuditResult.DENIED));
            status.setRollbackOnly();
        });
        assertThat(count("audit_logs")).isEqualTo(1);
        assertThatThrownBy(() -> audit.record(new AuditEvent(UUID.randomUUID(), actor(Role.ADMIN).accountId(),
                "INTEGRATION_CHECK", "SUBJECT", subjectId.toString(), AuditResult.SUCCESS, null,
                Map.of("password", "must-not-store"), Instant.now()))).isInstanceOf(IllegalArgumentException.class);
        assertThat(count("audit_logs")).isEqualTo(1);
    }

    @Test
    void redisSessionAndCurrentAccountStatusProtectRealSecurityFilter() throws Exception {
        Actor admin = actor(Role.ADMIN);
        var session = sessionStore.createSession(admin.accountId(), null);
        Cookie cookie = new Cookie("access_token", jwtService.issue(admin.accountId(), admin.role(), session.sid()));
        mvc.perform(get("/api/v1/admin/subjects").cookie(cookie)).andExpect(status().isOk());
        jdbc.update("update accounts set status='DISABLED',version=version+1 where id=?", admin.accountId());
        mvc.perform(get("/api/v1/admin/subjects").cookie(cookie)).andExpect(status().isUnauthorized());
        assertThat(jdbc.queryForObject("select status from subjects where id=?", String.class, subjectId))
                .isEqualTo("ACTIVE");
    }

    @Test
    void classCreationEnrollmentRemovalAndReactivationUseActualOwners() {
        var manager = actor(Role.SUBJECT_MANAGER);
        var created = academicsService.createClass(manager, subjectId, "SECOND", "Second class", null,
                LocalDate.of(2026, 11, 14), actor(Role.TEACHER).accountId());
        assertThat(created.status()).isEqualTo(ClassStatus.DRAFT);
        academicsService.changeEnrollmentStatus(manager, classId, actor(Role.STUDENT).accountId(),
                EnrollmentStatus.REMOVED);
        assertThatThrownBy(() -> academicsService.loadClass(actor(Role.STUDENT), "student", classId))
                .isInstanceOf(ApiException.class);
        academicsService.enrollStudent(manager, classId, actor(Role.STUDENT).accountId());
        assertThat(academicsService.loadClass(actor(Role.STUDENT), "student", classId).id()).isEqualTo(classId);
        assertThat(jdbc.queryForObject("select count(*) from enrollments where class_id=? and account_id=?",
                Integer.class, classId, actor(Role.STUDENT).accountId())).isEqualTo(1);
    }

    @Test
    void staleClassWriteAndUnauthorizedEnrollmentLeaveCommittedStateUnchanged() {
        var manager = actor(Role.SUBJECT_MANAGER);
        var current = academicsService.loadClass(manager, "manager", classId);
        academicsService.updateClassInfo(manager, classId, "Renamed", null, current.startsOn(), current.version());
        assertThatThrownBy(() -> academicsService.updateClassInfo(manager, classId, "Stale", null,
                current.startsOn(), current.version()))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> academicsService.changeEnrollmentStatus(actor(Role.TEACHER), classId,
                actor(Role.STUDENT).accountId(), EnrollmentStatus.REMOVED)).isInstanceOf(ApiException.class);
        assertThat(jdbc.queryForObject("select name from course_classes where id=?", String.class, classId))
                .isEqualTo("Renamed");
        assertThat(jdbc.queryForObject("select status from enrollments where class_id=?", String.class, classId))
                .isEqualTo("ACTIVE");
    }

    @Test
    void personalAiConnectionEncryptsAtRestAndRejectsForeignOwner() {
        String key = "integration-only-key-never-sent";
        var saved = aiConnectionService.addPersonal(actor(Role.TEACHER), AiProvider.OPENAI,
                new AiConnectionService.SaveCommand(key, "gpt-6.1-sol", null));
        assertThatThrownBy(() -> aiConnectionService.setPersonalEnabled(actor(Role.STUDENT), saved.id(), false,
                saved.version())).isInstanceOf(ApiException.class);
        var disabled = aiConnectionService.setPersonalEnabled(actor(Role.TEACHER), saved.id(), false, saved.version());
        assertThat(disabled.status().name()).isEqualTo("DISABLED");
        assertThat(jdbc.queryForObject("select row_to_json(c)::text from ai_connections c where id=?",
                String.class, saved.id())).doesNotContain(key);
    }

    @Test
    void concurrentCredentialCreationOnlyFillsOneRemainingSlot() throws Exception {
        var teacher = actor(Role.TEACHER);
        for (int i = 0; i < 4; i++) {
            mcpCredentialService.createCredential(teacher, "Synthetic agent " + i, Access.READ_ONLY, 7);
        }
        Callable<String> create = () -> {
            try {
                mcpCredentialService.createCredential(teacher, "Concurrent agent", Access.READ_ONLY, 7);
                return "CREATED";
            } catch (ApiException error) {
                return error.readCode();
            }
        };
        var executor = Executors.newFixedThreadPool(2);
        try {
            var pending = transaction.execute(status -> {
                // Hold both requests before either INSERT can commit.
                jdbc.execute("lock table mcp_credentials in share mode");
                var requests = List.of(executor.submit(create), executor.submit(create));
                org.awaitility.Awaitility.await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                        assertThat(jdbc.queryForObject("""
                                select count(*) from pg_locks where not granted
                                and (relation = 'mcp_credentials'::regclass or locktype = 'advisory')
                                """, Long.class)).isEqualTo(2));
                return requests;
            });

            assertThat(List.of(pending.get(0).get(10, TimeUnit.SECONDS),
                    pending.get(1).get(10, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder("CREATED", "MCP_LIMIT_REACHED");
            assertThat(mcpCredentialService.listCredentials(teacher)).hasSize(5);
        } finally {
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test
    void sourceMismatchCommitsEmptyDraftThenQueuesOneSourceOnlyRetry() {
        aiConnectionService.addPersonal(actor(Role.TEACHER), AiProvider.OPENAI,
                new AiConnectionService.SaveCommand("integration-key-never-sent", "gpt-6.1-sol", null));
        var quiz = quizAuthoringService.createForClass(actor(Role.TEACHER), classId, "Source quiz");
        UUID material = seedQuizSource();
        var pending = quizDrafts.requestForQuiz(actor(Role.TEACHER), quiz.quiz().id(),
                new edu.aiplatform.quizzes.service.QuizAiDraftService.DraftRequest("Liên Quân", 2, QuestionType.MCQ_SINGLE,
                        Difficulty.EASY, List.of(material), edu.aiplatform.ai.contract.AiSource.PERSONAL, AiProvider.OPENAI, "source-guard"),
                quiz.quiz().version());
        var result = new edu.aiplatform.ai.contract.AiTaskResult(pending.taskId(), "QUIZ_QUESTION_DRAFT", actor(Role.TEACHER),
                "quiz-ai-draft:" + pending.id(), quiz.quiz().version(), """
                {"requestMatchesSources":false,"questionsMatchSources":false,
                 "questions":[{"type":"MCQ_SINGLE","stem":"Wrong topic","options":["A","B"],"correct":[1]}]}
                """);
        transaction.executeWithoutResult(status -> {
            assertThat(quizDraftResults.onReady(result)).isEqualTo(edu.aiplatform.ai.contract.ApplyOutcome.APPLIED);
            assertThat(count("ai_tasks")).isEqualTo(1);
        });
        var retry = quizDrafts.getForQuiz(actor(Role.TEACHER), quiz.quiz().id(), pending.id());
        assertThat(retry.status()).isEqualTo(DraftStatus.PENDING);
        assertThat(retry.questions()).isEmpty();
        assertThat(retry.taskId()).isNotEqualTo(pending.taskId());
        assertThat(count("ai_tasks")).isEqualTo(2);
        assertThat(jdbc.queryForObject("select input_payload::text from ai_tasks where id=?", String.class, retry.taskId()))
                .contains("Tạo quiz theo nội dung tài liệu đã chọn", "Quang Trung").doesNotContain("Liên Quân");
        transaction.executeWithoutResult(status -> assertThat(quizDraftResults.onReady(result))
                .isEqualTo(edu.aiplatform.ai.contract.ApplyOutcome.STALE));
        assertThat(count("ai_tasks")).isEqualTo(2);
    }

    @Test
    void failedSourceRetrySubmissionRollsBackBeforeRecordingDraftFailure() {
        var connection = aiConnectionService.addPersonal(actor(Role.TEACHER), AiProvider.OPENAI,
                new AiConnectionService.SaveCommand("integration-key-never-sent", "gpt-6.1-sol", null));
        var quiz = quizAuthoringService.createForClass(actor(Role.TEACHER), classId, "Source quiz");
        var pending = quizDrafts.requestForQuiz(actor(Role.TEACHER), quiz.quiz().id(),
                new edu.aiplatform.quizzes.service.QuizAiDraftService.DraftRequest("Other topic", 2, null, null,
                        List.of(seedQuizSource()), edu.aiplatform.ai.contract.AiSource.PERSONAL, AiProvider.OPENAI, "retry-failure"),
                quiz.quiz().version());
        aiConnectionService.setPersonalEnabled(actor(Role.TEACHER), connection.id(), false, connection.version());
        transaction.executeWithoutResult(status -> quizDraftResults.onReady(new edu.aiplatform.ai.contract.AiTaskResult(
                pending.taskId(), "QUIZ_QUESTION_DRAFT", actor(Role.TEACHER), "quiz-ai-draft:" + pending.id(), quiz.quiz().version(), """
                {"requestMatchesSources":false,"questions":[{"type":"MCQ_SINGLE","stem":"x","options":["A","B"],"correct":[1]}]}
                """)));
        var failed = quizDrafts.getForQuiz(actor(Role.TEACHER), quiz.quiz().id(), pending.id());
        assertThat(failed.status()).isEqualTo(DraftStatus.FAILED);
        assertThat(failed.questions()).isEmpty();
        assertThat(failed.sourceWarnings()).hasSize(1);
        assertThat(count("ai_tasks")).isEqualTo(1);
    }

    private UUID seedQuizSource() {
        UUID module = UUID.randomUUID(), material = UUID.randomUUID();
        jdbc.update("insert into material_modules(id,subject_id,title,order_no,created_by,created_at,updated_at) "
                + "values (?,?,'Module',0,?,now(),now())", module, subjectId, actor(Role.TEACHER).accountId());
        jdbc.update("insert into materials(id,scope,subject_id,class_id,module_id,kind,title,order_no,status,current_version_no,"
                + "created_by,created_at,updated_at) values (?,'CLASS',?,?,?,'FILE','Lịch sử',0,'ACTIVE',1,?,now(),now())",
                material, subjectId, classId, module, actor(Role.TEACHER).accountId());
        jdbc.update("insert into material_versions(material_id,version_no,file_id,file_name,mime_type,byte_size,text_status,"
                + "extracted_text,created_by,created_at) values (?,1,'test-only-file','source.pdf','application/pdf',100,'EXTRACTED',"
                + "'Quang Trung đại phá quân Thanh năm 1789.',?,now())", material, actor(Role.TEACHER).accountId());
        return material;
    }

    @Test
    void settingUpdateInvalidatesConsumerCacheAndRejectsStaleVersion() {
        String key = PurposePolicy.MATERIAL_MAX_MB.key();
        var before = settingsService.loadSetting(actor(Role.ADMIN), key);
        settings.get(PurposePolicy.MATERIAL_MAX_MB);
        int changedValue = before.value() != null && before.value().asInt() == 20 ? 25 : 20;
        var changed = settingsService.updateSetting(actor(Role.ADMIN), key, json.valueToTree(changedValue), before.version());
        assertThat(settings.get(PurposePolicy.MATERIAL_MAX_MB)).contains(changedValue);
        assertThatThrownBy(() -> settingsService.updateSetting(actor(Role.ADMIN), key, json.valueToTree(30), before.version()))
                .isInstanceOf(ApiException.class);
        assertThat(settingsService.loadSetting(actor(Role.ADMIN), key).version()).isEqualTo(changed.version());
        assertThat(jdbc.queryForObject("select count(*) from audit_logs where action='SETTING_UPDATED'", Long.class))
                .isEqualTo(1);
    }

    @Test
    void actualRabbitJobAppearsOnlyAfterCommitAndRollbackPublishesNothing() {
        String key = "integration:" + UUID.randomUUID();
        transaction.executeWithoutResult(status -> {
            job.enqueue(JobTypes.EMAIL_SEND, Map.of("notificationId", UUID.randomUUID().toString()), key);
            assertThat(rabbitTemplate.receive("jobs.email")).isNull();
            status.setRollbackOnly();
        });
        assertThat(rabbitTemplate.receive("jobs.email", 300)).isNull();
        transaction.executeWithoutResult(status -> {
            job.enqueue(JobTypes.EMAIL_SEND, Map.of("notificationId", UUID.randomUUID().toString()), key);
            assertThat(rabbitTemplate.receive("jobs.email")).isNull();
        });
        var message = rabbitTemplate.receive("jobs.email", 5000);
        assertThat(message).isNotNull();
        assertThat(new String(message.getBody(), java.nio.charset.StandardCharsets.UTF_8)).contains(key, "EMAIL_SEND");
        assertThat(rabbitTemplate.receive("jobs.email", 300)).isNull();
    }

    @Test
    void rabbitDeliveryPassesRealListenerDispatcherAndAcknowledgesMissingTerminalWork() {
        String key = "missing-delivery:" + UUID.randomUUID();
        job.enqueue(JobTypes.EMAIL_SEND, Map.of("deliveryId", UUID.randomUUID().toString()), key);
        rabbitTemplate.execute(channel -> {
            var delivery = org.awaitility.Awaitility.await().atMost(Duration.ofSeconds(5))
                    .until(() -> channel.basicGet("jobs.email", false), Objects::nonNull);
            var properties = new org.springframework.amqp.core.MessageProperties();
            properties.setDeliveryTag(delivery.getEnvelope().getDeliveryTag());
            new edu.aiplatform.jobs.integration.JobListener(jobDispatcher, json)
                    .receiveEmail(new org.springframework.amqp.core.Message(delivery.getBody(), properties), channel);
            channel.basicRecover(true);
            assertThat(channel.basicGet("jobs.email", true)).isNull();
            return null;
        });
        assertThat(count("email_deliveries")).isZero();
    }

    AssignmentViews.Detail publishAssignment(GradingMode mode) throws Exception {
        return publishAssignment(mode, SubmissionMode.INDIVIDUAL);
    }

    AssignmentViews.Detail publishAssignment(GradingMode mode, SubmissionMode submissionMode) throws Exception {
        var teacher = actor(Role.TEACHER);
        var created = assignmentAuthoringService.createForClass(teacher, classId,
                new AssignmentCommands.Create(submissionMode, mode, false, "Integration assignment"));
        var skeleton = new DocumentModel(List.of(new DocumentBlock(UUID.randomUUID(), BlockOrigin.TEACHER,
                BlockType.HEADING, json.readTree("{\"style\":\"H1\",\"text\":\"Answer\"}"))));
        var prepared = assignmentAuthoringService.saveSkeleton(teacher, created.id(),
                new AssignmentCommands.Skeleton(created.version(), skeleton, "Provide an answer", false));
        var rubric = assignmentAuthoringService.updateRubric(teacher, prepared.id(), prepared.parts().get(0).partId(),
                new AssignmentCommands.RubricUpdate(prepared.version(), List.of(new RubricCriterion(UUID.randomUUID(),
                        "Correctness", "", CriterionKind.TEXT,
                        List.of(new RubricItem(UUID.randomUUID(), "Correct answer", BigDecimal.TEN))))));
        prepared = assignmentAuthoringService.loadAssignment(teacher, prepared.id());
        assertThat(rubric.complete()).isTrue();
        // Hoàn tất bài rồi mới xếp lịch giao (yêu cầu 2026-11-11).
        prepared = assignmentAuthoringService.review(teacher, prepared.id(), prepared.version());
        return assignmentPublicationService.publish(teacher, prepared.id(),
                new AssignmentCommands.Publish(prepared.version(), null, true, Instant.now().plusSeconds(3600),
                        null, 2, false));
    }

    DocumentModel answer(DocumentModel original) {
        var blocks = new ArrayList<>(original.blocks());
        var content = json.createObjectNode();
        content.putArray("runs").addObject().put("text", "Integration answer");
        blocks.add(new DocumentBlock(UUID.randomUUID(), BlockOrigin.STUDENT, BlockType.PARAGRAPH, content));
        return new DocumentModel(blocks);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void legacySubjectDraftTransfersOnlyToTheUniqueEligibleClass(boolean ambiguous) throws Exception {
        var assignment = publishAssignment(GradingMode.GRADED);
        // Legacy data from when whole-subject assignments could be published.
        jdbc.update("update assignments set scope = 'SUBJECT', class_id = null where id = ?", assignment.id());
        var student = actor(Role.STUDENT);
        var manager = actor(Role.SUBJECT_MANAGER);
        UUID previousSubmission = submitIndividual(assignment);
        var previousReceipt = attemptService.loadReceipt(student, previousSubmission);
        var draft = attemptService.start(student, assignment.id(), classId).attempt();
        var saved = attemptService.saveAttempt(student, draft.attemptId(), draft.contentVersion(), answer(draft.content()));

        var destination = academicsService.createClass(manager, subjectId, "TRANSFER", "Transfer class", null,
                LocalDate.of(2026, 11, 14), actor(Role.TEACHER).accountId());
        academicsService.changeClassStatus(manager, destination.id(), ClassStatus.OPEN, destination.version());
        assertThatThrownBy(() -> attemptService.start(student, assignment.id(), destination.id()))
                .isInstanceOfSatisfying(ApiException.class, failure -> assertThat(failure.readCode()).isEqualTo("NOT_FOUND"));
        academicsService.changeEnrollmentStatus(manager, classId, student.accountId(), EnrollmentStatus.REMOVED);
        academicsService.enrollStudent(manager, destination.id(), student.accountId());

        if (ambiguous) {
            var other = academicsService.createClass(manager, subjectId, "AMBIGUOUS", "Other class", null,
                    LocalDate.of(2026, 11, 14), actor(Role.TEACHER).accountId());
            academicsService.changeClassStatus(manager, other.id(), ClassStatus.OPEN, other.version());
            jdbc.update("insert into enrollments(class_id, account_id, status, source, enrolled_at) "
                    + "values (?, ?, 'ACTIVE', 'MANUAL', now())", other.id(), student.accountId());
            assertThatThrownBy(() -> attemptService.start(student, assignment.id(), destination.id()))
                    .isInstanceOfSatisfying(ApiException.class,
                            failure -> assertThat(failure.readCode()).isEqualTo("NOT_FOUND"));
            assertThat(jdbc.queryForObject("select class_id from assignment_attempts where id = ?", UUID.class,
                    draft.attemptId())).isEqualTo(classId);
        } else {
            var resumed = attemptService.start(student, assignment.id(), destination.id());
            assertThat(resumed.created()).isFalse();
            assertThat(resumed.attempt().attemptId()).isEqualTo(draft.attemptId());
            assertThat(resumed.attempt().classId()).isEqualTo(destination.id());
            assertThat(resumed.attempt().content()).isEqualTo(saved.content());
            assertThat(resumed.attempt().contentVersion()).isEqualTo(saved.contentVersion());
            assertThat(attemptService.loadAttempt(student, draft.attemptId())).usingRecursiveComparison()
                    .ignoringFields("serverTime").isEqualTo(resumed.attempt());
            assertThat(attemptService.start(student, assignment.id(), destination.id()).attempt().attemptId())
                    .isEqualTo(draft.attemptId());
        }
        assertThat(jdbc.queryForMap("select class_id, receipt_hash from assignment_attempts where id = ?",
                previousSubmission)).containsEntry("class_id", classId).containsEntry("receipt_hash", previousReceipt.receiptHash());
        assertThatThrownBy(() -> attemptService.loadReceipt(student, previousSubmission))
                .isInstanceOfSatisfying(ApiException.class, failure -> assertThat(failure.readCode()).isEqualTo("NOT_FOUND"));
    }

    @Test
    void gradedAssignmentPublicationSubmissionAndEvaluationUseRealContracts() throws Exception {
        var published = publishAssignment(GradingMode.GRADED);
        assertThat(published.status()).isEqualTo(AssignmentStatus.OPEN);
        assertThat(published.parts()).allMatch(AssignmentViews.Part::locked);
        var started = attemptService.start(actor(Role.STUDENT), published.id(), classId);
        var attempt = started.attempt();
        assertThat(attemptService.start(actor(Role.STUDENT), published.id(), classId).created()).isFalse();
        var saved = attemptService.saveAttempt(actor(Role.STUDENT), attempt.attemptId(), attempt.contentVersion(), answer(attempt.content()));
        attemptService.submit(actor(Role.STUDENT), attempt.attemptId(), saved.contentVersion());
        assertThat(jdbc.queryForObject("select status from assignment_attempts where id=?", String.class, attempt.attemptId()))
                .isEqualTo("SUBMITTED");
        assertThat(jdbc.queryForObject("select status from evaluations where attempt_id=?", String.class, attempt.attemptId()))
                .isEqualTo("PENDING");
        assertThat(count("evaluations")).isEqualTo(1);
        assertThatThrownBy(() -> attemptService.loadAttempt(actor(Role.TEACHER), attempt.attemptId())).isInstanceOf(ApiException.class);
    }

    @Test
    void practiceSaveRejectsStaleContentAndRemovedEnrollmentWithoutChangingAnswer() throws Exception {
        var published = publishAssignment(GradingMode.PRACTICE);
        var student = actor(Role.STUDENT);
        var attempt = attemptService.start(student, published.id(), classId).attempt();
        var content = answer(attempt.content());
        var saved = attemptService.saveAttempt(student, attempt.attemptId(), attempt.contentVersion(), content);
        assertThatThrownBy(() -> attemptService.saveAttempt(student, attempt.attemptId(), attempt.contentVersion(), content))
                .isInstanceOf(ApiException.class);
        assertThat(attemptService.loadAttempt(student, attempt.attemptId()).contentVersion()).isEqualTo(saved.contentVersion());
        academicsService.changeEnrollmentStatus(actor(Role.SUBJECT_MANAGER), classId, student.accountId(), EnrollmentStatus.REMOVED);
        assertThatThrownBy(() -> attemptService.loadAttempt(student, attempt.attemptId())).isInstanceOf(ApiException.class);
        assertThat(jdbc.queryForObject("select content_version from assignment_attempts where id=?", Long.class,
                attempt.attemptId())).isEqualTo(saved.contentVersion());
        assertThat(count("evaluations")).isZero();
    }

    @Test
    void invalidAssignmentPublicationLeavesDraftAndNoRubricLock() {
        var created = assignmentAuthoringService.createForClass(actor(Role.TEACHER), classId,
                new AssignmentCommands.Create(SubmissionMode.INDIVIDUAL, GradingMode.GRADED, false, "Incomplete"));
        assertThatThrownBy(() -> assignmentPublicationService.publish(actor(Role.TEACHER), created.id(),
                new AssignmentCommands.Publish(created.version(), null, true, Instant.now().plusSeconds(3600),
                        null, 1, false))).isInstanceOf(ApiException.class);
        assertThat(jdbc.queryForObject("select status from assignments where id=?", String.class, created.id()))
                .isEqualTo("DRAFT");
        assertThat(jdbc.queryForObject("select count(*) from audit_logs where action='ASSIGNMENT_PUBLISHED'", Long.class))
                .isZero();
    }

    @Test
    void quizPublicationPinsQuestionsAndRealAttemptScoresWithoutGradebookEntry() {
        var teacher = actor(Role.TEACHER);
        var created = quizAuthoringService.createForClass(teacher, classId, "Integration quiz");
        var content = QuestionContent.createContent(QuestionType.MCQ_SINGLE, "Which option is correct?",
                List.of(new QuestionOption("A", "Correct"), new QuestionOption("B", "Incorrect")),
                List.of("A"), "Explanation", BigDecimal.TEN, Difficulty.EASY, List.of());
        var prepared = quizAuthoringService.addInline(teacher, created.quiz().id(), content, created.quiz().version());
        var published = quizPublicationService.publish(teacher, prepared.quiz().id(), prepared.quiz().version());
        var taking = quizAttemptService.start(actor(Role.STUDENT), published.quiz().id(), classId);
        assertThat(json.valueToTree(taking).toString()).doesNotContain("correctOptionIds", "Explanation");
        UUID questionId = taking.questions().get(0).questionId();
        quizAttemptService.saveAnswers(actor(Role.STUDENT), taking.attemptId(), Map.of(questionId, List.of("A")),
                taking.answerVersion());
        quizAttemptService.submit(actor(Role.STUDENT), taking.attemptId());
        assertThat(jdbc.queryForObject("select score from quiz_attempts where id=?", BigDecimal.class,
                taking.attemptId())).isEqualByComparingTo(BigDecimal.TEN);
        assertThat(count("evaluations")).isZero();
        assertThatThrownBy(() -> quizAttemptService.loadResult(teacher, taking.attemptId())).isInstanceOf(ApiException.class);
    }

    @Test
    void simultaneousReopensSerializeEvenWhenNoVersionIsOpen() {
        var teacher = actor(Role.TEACHER);
        var created = quizAuthoringService.createForClass(teacher, classId, "Concurrent quiz");
        var content = QuestionContent.createContent(QuestionType.MCQ_SINGLE, "Which option is correct?",
                List.of(new QuestionOption("A", "Correct"), new QuestionOption("B", "Incorrect")),
                List.of("A"), "Explanation", BigDecimal.TEN, Difficulty.EASY, List.of());
        var prepared = quizAuthoringService.addInline(teacher, created.quiz().id(), content, created.quiz().version());
        var first = quizPublicationService.publish(teacher, prepared.quiz().id(), prepared.quiz().version()).quiz();
        var nextDraft = quizCopyService.createNewVersion(teacher, first.id()).quiz();
        var next = quizPublicationService.publish(teacher, nextDraft.id(), nextDraft.version()).quiz();
        var retiredNext = quizPublicationService.retire(teacher, next.id(), "Pause", next.version()).quiz();
        var retiredFirst = quizAuthoringService.loadQuiz(teacher, first.id(), null).quiz();
        var competing = new TransactionTemplate(transaction.getTransactionManager());
        competing.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        transaction.executeWithoutResult(status -> {
            quizPublicationService.reopen(teacher, retiredNext.id(), retiredNext.version());
            assertThatThrownBy(() -> competing.executeWithoutResult(other -> {
                jdbc.execute("set local lock_timeout = '200ms'");
                quizPublicationService.reopen(teacher, retiredFirst.id(), retiredFirst.version());
            })).isInstanceOfSatisfying(DataAccessException.class, conflict -> assertThat(
                    conflict.getMostSpecificCause()).isInstanceOfSatisfying(java.sql.SQLException.class,
                    failure -> assertThat(failure.getSQLState()).isEqualTo("55P03")));
        });

        assertThatThrownBy(() -> quizPublicationService.reopen(teacher, retiredFirst.id(), retiredFirst.version()))
                .isInstanceOfSatisfying(ApiException.class,
                        conflict -> assertThat(conflict.readCode()).isEqualTo("QUIZ_OTHER_VERSION_OPEN"));
        assertThat(jdbc.queryForList("select id from quizzes where id in (?, ?) and status = 'OPEN'", UUID.class,
                retiredFirst.id(), retiredNext.id())).containsExactly(retiredNext.id());
    }

    @Test
    void emptyQuizCannotPublishOrCreateStudentAttempt() {
        var created = quizAuthoringService.createForClass(actor(Role.TEACHER), classId, "Empty quiz");
        assertThatThrownBy(() -> quizPublicationService.publish(actor(Role.TEACHER), created.quiz().id(),
                created.quiz().version())).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> quizAttemptService.start(actor(Role.STUDENT), created.quiz().id(), classId))
                .isInstanceOf(ApiException.class);
        assertThat(count("quiz_attempts")).isZero();
    }

    @Test
    void groupSetUsesEnrollmentAndRejectsStaleWholeSetWrite() {
        var teacher = actor(Role.TEACHER);
        var before = classGroupService.view(teacher, classId);
        UUID student = actor(Role.STUDENT).accountId();
        var saved = classGroupService.saveGroups(teacher, classId, new ClassGroupService.SaveCommand(before.version(),
                List.of(new Draft("new", null, "Integration group", student, List.of(student)))));
        assertThat(saved.groups()).hasSize(1);
        assertThat(saved.groups().get(0).leaderId()).isEqualTo(student);
        assertThatThrownBy(() -> classGroupService.saveGroups(teacher, classId,
                new ClassGroupService.SaveCommand(before.version(), List.of()))).isInstanceOf(ApiException.class);
        assertThat(classGroupService.view(teacher, classId).groups()).hasSize(1);
        assertThatThrownBy(() -> classGroupService.view(actor(Role.STUDENT), classId)).isInstanceOf(ApiException.class);
    }

    UUID submitIndividual(AssignmentViews.Detail assignment) {
        var attempt = attemptService.start(actor(Role.STUDENT), assignment.id(), classId).attempt();
        var saved = attemptService.saveAttempt(actor(Role.STUDENT), attempt.attemptId(), attempt.contentVersion(), answer(attempt.content()));
        attemptService.submit(actor(Role.STUDENT), attempt.attemptId(), saved.contentVersion());
        return attempt.attemptId();
    }

    void publishGrade(UUID evaluationId, AssignmentViews.Detail assignment) {
        var current = assignmentAuthoringService.loadAssignment(actor(Role.TEACHER), assignment.id());
        assignmentPublicationService.closeAssignment(actor(Role.TEACHER), assignment.id(), current.version());
        var rubric = assignmentAuthoringService.loadRubric(actor(Role.TEACHER), assignment.id(), assignment.parts().get(0).partId());
        var checks = Map.of(rubric.rubricId(), Set.of(rubric.criteria().get(0).items().get(0).itemId()));
        gradingService.saveEvaluation(actor(Role.TEACHER), evaluationId, new GradingCommands.Save(
                evaluationRepository.findById(evaluationId).orElseThrow().version, checks, BigDecimal.ZERO,
                null, "Correct answer", null, null));
        assertThat(gradebookService.listMyGrades(actor(Role.STUDENT), classId)).isEmpty();
        gradingService.finalizeEvaluation(actor(Role.TEACHER), new GradingCommands.ItemVersion(evaluationId,
                evaluationRepository.findById(evaluationId).orElseThrow().version));
        assertGradeEditable(evaluationId, checks);
        assertThat(gradebookService.listMyGrades(actor(Role.STUDENT), classId)).isEmpty();
        gradingService.publishEvaluation(actor(Role.TEACHER), new GradingCommands.ItemVersion(evaluationId,
                evaluationRepository.findById(evaluationId).orElseThrow().version));
        assertGradeEditable(evaluationId, checks);
    }

    void assertGradeEditable(UUID evaluationId, Map<UUID, Set<UUID>> checks) {
        var before = evaluationRepository.findById(evaluationId).orElseThrow();
        gradingService.saveEvaluation(actor(Role.TEACHER), evaluationId,
                new GradingCommands.Save(before.version, checks, BigDecimal.ZERO, null,
                        "Corrected teacher feedback", null, null));
        var after = evaluationRepository.findById(evaluationId).orElseThrow();
        assertThat(after.version).isEqualTo(before.version + 1);
        assertThat(after.state).isEqualTo(before.state);
        assertThat(after.score).isEqualByComparingTo(before.score);
        assertThat(after.feedback).isEqualTo("Corrected teacher feedback");
        if (after.publishedAt != null) {
            assertThat(gradebookService.listMyGrades(actor(Role.STUDENT), classId))
                    .singleElement().satisfies(grade -> {
                        assertThat(grade.score()).isEqualByComparingTo(after.score);
                        assertThat(grade.feedback()).isEqualTo(after.feedback);
                    });
        }
    }

    @Test
    void individualResubmissionBeforeCloseSupersedesPriorEvaluationThenAllowsPublication() throws Exception {
        var assignment = publishAssignment(GradingMode.GRADED);
        UUID firstAttempt = submitIndividual(assignment);
        UUID firstGrade = evaluationRepository.findByAttemptId(firstAttempt).orElseThrow().id;
        assertThatThrownBy(() -> gradingService.saveEvaluation(actor(Role.TEACHER), firstGrade,
                new GradingCommands.Save(evaluationRepository.findById(firstGrade).orElseThrow().version,
                        Map.of(), BigDecimal.ZERO, null, "Too early", null, null)))
                .isInstanceOf(ApiException.class).hasMessageContaining("đóng nhận bài");
        UUID secondAttempt = submitIndividual(assignment);
        assertThat(secondAttempt).isNotEqualTo(firstAttempt);
        assertThat(evaluationRepository.findById(firstGrade).orElseThrow().state.name()).isEqualTo("SUPERSEDED");
        assertThat(evaluationRepository.findByAttemptId(secondAttempt).orElseThrow().state.name()).isEqualTo("PENDING");
        assertThat(gradebookService.listMyGrades(actor(Role.STUDENT), classId)).isEmpty();
        assertThat(count("evaluations")).isEqualTo(2);
        publishGrade(evaluationRepository.findByAttemptId(secondAttempt).orElseThrow().id, assignment);
        assertThat(gradebookService.listMyGrades(actor(Role.STUDENT), classId)).singleElement()
                .satisfies(grade -> assertThat(grade.score()).isEqualByComparingTo(BigDecimal.TEN));
        assertThatThrownBy(() -> attemptService.start(actor(Role.STUDENT), assignment.id(), classId))
                .isInstanceOf(ApiException.class);
        assertThat(jdbc.queryForObject("select count(*) from audit_logs where action='GRADE_PUBLISHED'", Long.class)).isEqualTo(1);
    }

    @Test
    void batchProposalRejectsGradeVersionChangedBeforeTheEvaluationLock() throws Exception {
        var teacher = actor(Role.TEACHER);
        var assignment = publishAssignment(GradingMode.GRADED);
        var evaluation = evaluationRepository.findByAttemptId(submitIndividual(assignment)).orElseThrow();
        assignmentPublicationService.closeAssignment(teacher, assignment.id(), assignment.version());
        configureQuota(100_000, 200_000);
        aiConnectionService.addInstitutional(actor(Role.ADMIN), AiProvider.OPENAI,
                new AiConnectionService.SaveCommand("integration-only-key-never-sent", "gpt-6.1-sol", null));
        var input = new GradingCommands.AiBatch(
                List.of(new GradingCommands.ItemVersion(evaluation.id, evaluation.version)),
                new GradingCommands.AiRequest(AiSource.SHARED, null, UUID.randomUUID().toString()));
        var executor = Executors.newSingleThreadExecutor();
        try {
            var pending = transaction.execute(status -> {
                gradingService.saveEvaluation(teacher, evaluation.id, new GradingCommands.Save(
                        evaluation.version, Map.of(), BigDecimal.ZERO, null, "Concurrent feedback", null, null));
                var response = executor.submit(() -> gradingProposalService.requestProposals(teacher, input));
                org.awaitility.Awaitility.await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                        assertThat(jdbc.queryForObject("""
                                select count(*) from pg_stat_activity
                                where wait_event_type = 'Lock' and query like '%from evaluations%for update'
                                """, Long.class)).isEqualTo(1));
                return response;
            });

            assertThat(pending.get(10, TimeUnit.SECONDS).items()).singleElement().satisfies(item -> {
                assertThat(item.outcome()).isEqualTo("FAILED");
                assertThat(item.code()).isEqualTo("GRADE_VERSION_CONFLICT");
            });
            assertThat(count("grading_proposals")).isZero();
            assertThat(count("ai_tasks")).isZero();
            assertThat(quotaCounter(teacher.accountId().toString(), "reserved_tokens")).isZero();
        } finally {
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void retriesInterruptedAiGradingWithOneProposalTaskAndReservation(boolean taskAlreadySubmitted)
            throws Exception {
        while (rabbitTemplate.receive("jobs.ai") != null) {
            // Discard messages from earlier tests in this suite's isolated broker.
        }
        var teacher = actor(Role.TEACHER);
        var assignment = publishAssignment(GradingMode.GRADED);
        var attemptId = submitIndividual(assignment);
        var evaluation = evaluationRepository.findByAttemptId(attemptId).orElseThrow();
        assignmentPublicationService.closeAssignment(teacher, assignment.id(), assignment.version());
        configureQuota(100_000, 200_000);
        aiConnectionService.addInstitutional(actor(Role.ADMIN), AiProvider.OPENAI,
                new AiConnectionService.SaveCommand("integration-only-key-never-sent", "gpt-6.1-sol", null));
        var request = new GradingCommands.AiRequest(AiSource.SHARED, null, UUID.randomUUID().toString());
        var prepared = gradingProposalTransactions.prepareProposal(teacher, evaluation.id, request);
        var taskRequest = new AiTaskRequest(
                teacher, request.source(), request.provider(), "GRADING_PROPOSAL",
                "evaluation:" + evaluation.id, prepared.proposal().versionNo, request.requestRef(),
                prepared.prompt(), prepared.outputSchema(), prepared.maxOutputTokens());
        UUID priorTaskId = taskAlreadySubmitted ? aiTask.submit(taskRequest).taskId() : null;
        assertThat(prepared.proposal().taskId).isNull();
        if (!taskAlreadySubmitted) {
            assertThatThrownBy(() -> gradingProposalTransactions.dispatchPreparedProposal(
                    prepared.proposal().id, () -> {
                        aiTask.submit(taskRequest);
                        throw new IllegalStateException("Integration fixture interrupted before attachment");
                    })).isInstanceOf(IllegalStateException.class).hasMessageContaining("interrupted");
            assertThat(count("ai_tasks")).isZero();
            assertThat(quotaCounter(teacher.accountId().toString(), "reserved_tokens")).isZero();
        }

        var workers = Executors.newFixedThreadPool(2);
        var start = new CountDownLatch(1);
        try {
            Callable<GradingProposal> retry = () -> {
                assertThat(start.await(5, TimeUnit.SECONDS)).isTrue();
                return gradingProposalService.requestProposal(teacher, evaluation.id, request);
            };
            var first = workers.submit(retry);
            var second = workers.submit(retry);
            start.countDown();
            var recovered = first.get(10, TimeUnit.SECONDS);
            var replayed = second.get(10, TimeUnit.SECONDS);
            assertThat(recovered.id).isEqualTo(prepared.proposal().id);
            assertThat(recovered.taskId).isNotNull();
            assertThat(replayed.id).isEqualTo(recovered.id);
            assertThat(replayed.taskId).isEqualTo(recovered.taskId);
            if (priorTaskId != null) {
                assertThat(recovered.taskId).isEqualTo(priorTaskId);
            }
            var changedSource = new GradingCommands.AiRequest(AiSource.PERSONAL, AiProvider.OPENAI,
                    request.requestRef());
            assertThatThrownBy(() -> gradingProposalService.requestProposal(teacher, evaluation.id,
                    changedSource)).isInstanceOfSatisfying(ApiException.class,
                            ex -> assertThat(ex.readCode()).isEqualTo("REQUEST_REF_CONFLICT"));
            assertThat(count("grading_proposals")).isEqualTo(1);
            assertThat(count("ai_tasks")).isEqualTo(1);
            long reserved = jdbc.queryForObject(
                    "select reserved_tokens from ai_tasks where id=?", Long.class, recovered.taskId);
            assertThat(reserved).isPositive();
            assertThat(quotaCounter(teacher.accountId().toString(), "reserved_tokens")).isEqualTo(reserved);
            assertThat(quotaCounter("GLOBAL", "reserved_tokens")).isEqualTo(reserved);
            assertThat(gradebookService.listMyGrades(actor(Role.STUDENT), classId)).isEmpty();
            assertThat(jdbc.queryForObject("select count(*) from audit_logs where action='AI_GRADING_REQUESTED'",
                    Long.class)).isEqualTo(1);
            var queued = rabbitTemplate.receive("jobs.ai", 5_000);
            assertThat(queued).isNotNull();
            assertThat(json.readTree(queued.getBody()).path("idempotencyKey").asText())
                    .isEqualTo(recovered.taskId.toString());
            assertThat(rabbitTemplate.receive("jobs.ai", 200)).isNull();
        } finally {
            start.countDown();
            workers.shutdownNow();
            assertThat(workers.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
    }

    void rejectEvaluationInsert(String kind) {
        assertThat(Set.of("ATTEMPT", "MEMBER")).contains(kind);
        jdbc.execute("create function integration_reject_evaluation() returns trigger language plpgsql as $$ "
                + "begin if NEW.kind='" + kind + "' then raise exception 'INTEGRATION_EVALUATION_FAILURE'; end if; return NEW; end $$");
        jdbc.execute("create trigger integration_reject_evaluation before insert on evaluations for each row execute function integration_reject_evaluation()");
    }

    void restoreEvaluationInsert() {
        jdbc.execute("drop trigger if exists integration_reject_evaluation on evaluations");
        jdbc.execute("drop function if exists integration_reject_evaluation()");
    }

    @Test
    void individualEvaluationFailureRollsBackSubmissionAndRetriesOnce() throws Exception {
        var assignment = publishAssignment(GradingMode.GRADED);
        var attempt = attemptService.start(actor(Role.STUDENT), assignment.id(), classId).attempt();
        var saved = attemptService.saveAttempt(actor(Role.STUDENT), attempt.attemptId(), attempt.contentVersion(), answer(attempt.content()));
        try {
            rejectEvaluationInsert("ATTEMPT");
            assertThatThrownBy(() -> attemptService.submit(actor(Role.STUDENT), attempt.attemptId(), saved.contentVersion()))
                    .hasStackTraceContaining("INTEGRATION_EVALUATION_FAILURE");
            assertThat(jdbc.queryForObject("select status from assignment_attempts where id=?", String.class, attempt.attemptId())).isEqualTo("IN_PROGRESS");
            assertThat(jdbc.queryForObject("select receipt_hash from assignment_attempts where id=?", String.class, attempt.attemptId())).isNull();
            assertThat(count("evaluations")).isZero();
            assertThat(jdbc.queryForObject("select count(*) from audit_logs where action='ASSIGNMENT_SUBMITTED' and result='SUCCESS'", Long.class)).isZero();
        } finally { restoreEvaluationInsert(); }
        attemptService.submit(actor(Role.STUDENT), attempt.attemptId(), saved.contentVersion());
        attemptService.submit(actor(Role.STUDENT), attempt.attemptId(), saved.contentVersion());
        assertThat(count("evaluations")).isEqualTo(1);
        assertThat(jdbc.queryForObject("select status from assignment_attempts where id=?", String.class, attempt.attemptId())).isEqualTo("SUBMITTED");
    }

    Actor addGroupMember() {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        jdbc.update("insert into accounts(id,school_email,display_name,password_hash,role,status,created_at,updated_at) values (?,?,?,'fixture-hash','STUDENT','ACTIVE',?,?)",
                id, id + "@example.test", "Second student", Timestamp.from(now), Timestamp.from(now));
        academicsService.enrollStudent(actor(Role.SUBJECT_MANAGER), classId, id);
        UUID leader = actor(Role.STUDENT).accountId();
        var before = classGroupService.view(actor(Role.TEACHER), classId);
        classGroupService.saveGroups(actor(Role.TEACHER), classId, new ClassGroupService.SaveCommand(before.version(),
                List.of(new Draft("new", null, "Integration group", leader, List.of(leader, id)))));
        return new Actor(id, Role.STUDENT);
    }

    @Test
    void earlyCloseRollsBackSavedAttemptOnEvaluationFailureThenRetriesAtomically() throws Exception {
        var teacher = actor(Role.TEACHER);
        var assignment = publishAssignment(GradingMode.GRADED);
        var attempt = attemptService.start(actor(Role.STUDENT), assignment.id(), classId).attempt();
        var saved = attemptService.saveAttempt(actor(Role.STUDENT), attempt.attemptId(), attempt.contentVersion(),
                answer(attempt.content()));
        try {
            rejectEvaluationInsert("ATTEMPT");
            assertThatThrownBy(() -> assignmentPublicationService.closeAssignment(teacher, assignment.id(),
                    assignment.version())).hasStackTraceContaining("INTEGRATION_EVALUATION_FAILURE");
            assertThat(jdbc.queryForObject("select status from assignments where id=?", String.class,
                    assignment.id())).isEqualTo("OPEN");
            assertThat(jdbc.queryForObject("select status from assignment_attempts where id=?", String.class,
                    attempt.attemptId())).isEqualTo("IN_PROGRESS");
            assertThat(count("evaluations")).isZero();
            assertThat(jdbc.queryForObject("select count(*) from audit_logs where action='ASSIGNMENT_CLOSED'",
                    Long.class)).isZero();
        } finally {
            restoreEvaluationInsert();
        }
        var closed = assignmentPublicationService.closeAssignment(teacher, assignment.id(), assignment.version());
        assertThat(closed.status()).isEqualTo(AssignmentStatus.CLOSED);
        assertThat(closed.closesAt()).isEqualTo(assignment.closesAt());
        assertThat(jdbc.queryForObject("select status from assignment_attempts where id=?", String.class,
                attempt.attemptId())).isEqualTo("SUBMITTED");
        assertThat(attemptService.loadAttempt(actor(Role.STUDENT), attempt.attemptId()).content()).isEqualTo(saved.content());
        assertThat(count("evaluations")).isEqualTo(1);
        assertThatThrownBy(() -> attemptService.saveAttempt(actor(Role.STUDENT), attempt.attemptId(),
                saved.contentVersion(), saved.content())).isInstanceOf(ApiException.class);
    }

    GroupWorkspaceService.WorkspaceView prepareGroupSection(UUID assignmentId) {
        var workspace = groupWorkspaceService.loadMyGroupDocument(actor(Role.STUDENT), assignmentId);
        UUID sectionId = workspace.sections().get(0).id();
        var claimed = groupSectionService.claim(actor(Role.STUDENT), workspace.documentId(), sectionId);
        var paragraph = json.createObjectNode();
        paragraph.putArray("runs").addObject().put("text", "Integration answer revision " + workspace.version());
        var content = List.of(new DocumentBlock(UUID.randomUUID(), BlockOrigin.STUDENT, BlockType.PARAGRAPH, paragraph));
        groupSectionService.saveDraft(actor(Role.STUDENT), workspace.documentId(), sectionId, claimed.version(), content);
        groupSectionService.markSectionDone(actor(Role.STUDENT), workspace.documentId(), sectionId);
        return groupWorkspaceService.loadWorkspace(actor(Role.STUDENT), workspace.documentId());
    }

    @Test
    void draftWaitingForTheGroupDocumentCannotWriteAfterClosure() throws Exception {
        addGroupMember();
        var assignment = publishAssignment(GradingMode.GRADED, SubmissionMode.GROUP);
        var student = actor(Role.STUDENT);
        var workspace = groupWorkspaceService.loadMyGroupDocument(student, assignment.id());
        UUID documentId = workspace.documentId();
        UUID sectionId = workspace.sections().get(0).id();
        var claimed = groupSectionService.claim(student, documentId, sectionId);
        var worker = Executors.newSingleThreadExecutor();
        var draftConnection = new CompletableFuture<Integer>();
        try {
            var pending = transaction.execute(status -> {
                jdbc.queryForObject("select id from assignments where id = ? for share", UUID.class, assignment.id());
                jdbc.queryForObject("select id from group_documents where id = ? for update", UUID.class, documentId);
                var draft = worker.submit(() -> catchThrowable(() -> transaction.executeWithoutResult(save -> {
                    draftConnection.complete(jdbc.queryForObject("select pg_backend_pid()", Integer.class));
                    groupSectionService.saveDraft(student, documentId, sectionId, claimed.version(), List.of());
                })));
                int backend = draftConnection.orTimeout(5, TimeUnit.SECONDS).join();
                org.awaitility.Awaitility.await().atMost(Duration.ofSeconds(5)).until(() -> Boolean.TRUE.equals(
                        jdbc.queryForObject("select cardinality(pg_blocking_pids(?)) > 0", Boolean.class, backend)));
                jdbc.execute("set local lock_timeout = '5s'");
                jdbc.queryForList("select id from group_document_sections where group_document_id = ? for update",
                        UUID.class, documentId);
                jdbc.update("update group_documents set status = 'CLOSED' where id = ?", documentId);
                return draft;
            });

            assertThat(pending.get(5, TimeUnit.SECONDS)).isInstanceOfSatisfying(ApiException.class,
                    failure -> assertThat(failure.readCode()).isEqualTo("GROUP_DOC_CLOSED"));
            assertThat(jdbc.queryForObject("select version from group_document_sections where id = ?",
                    Long.class, sectionId)).isEqualTo(claimed.version());
        } finally {
            worker.shutdownNow();
        }
    }

    @Test
    void groupResubmissionBeforeCloseCreatesMemberGradesThenLocksStudentWork() throws Exception {
        Actor member = addGroupMember();
        var assignment = publishAssignment(GradingMode.GRADED, SubmissionMode.GROUP);
        var workspace = prepareGroupSection(assignment.id());
        var firstReceipt = groupSubmissionService.submit(actor(Role.STUDENT), workspace.documentId(), workspace.version());
        var evaluations = evaluationRepository.findForAssignmentAndClass(assignment.id(), classId);
        assertThat(evaluations).hasSize(3);
        UUID root = evaluations.stream().filter(e -> e.kind.name().equals("GROUP_DOCUMENT")).findFirst().orElseThrow().id;
        Set<UUID> identities = new HashSet<>(evaluations.stream().map(e -> e.id).toList());
        assertThat(evaluations.stream().filter(e -> e.kind.name().equals("MEMBER")).map(e -> e.accountId))
                .containsExactlyInAnyOrder(actor(Role.STUDENT).accountId(), member.accountId());
        assertThatThrownBy(() -> gradingService.saveEvaluation(actor(Role.TEACHER), root, new GradingCommands.Save(
                evaluationRepository.findById(root).orElseThrow().version, Map.of(), BigDecimal.ZERO,
                null, "Draft grade", null, null)))
                .isInstanceOf(ApiException.class).hasMessageContaining("đóng nhận bài");
        workspace = prepareGroupSection(assignment.id());
        var draftReceipt = groupSubmissionService.submit(actor(Role.STUDENT), workspace.documentId(), workspace.version());
        assertThat(draftReceipt.receiptHash()).isNotEqualTo(firstReceipt.receiptHash());
        assertThat(evaluationRepository.findForAssignmentAndClass(assignment.id(), classId)).allSatisfy(e -> {
            assertThat(e.state.name()).isEqualTo("PENDING");
            assertThat(e.score).isNull();
        });
        publishGrade(root, assignment);
        assertThat(gradebookService.listMyGrades(actor(Role.STUDENT), classId)).hasSize(1);
        assertThat(gradebookService.listMyGrades(member, classId)).hasSize(1);
        assertThat(evaluationRepository.findForAssignmentAndClass(assignment.id(), classId)).allSatisfy(e -> {
            assertThat(e.state.name()).isEqualTo("PUBLISHED");
            assertThat(e.score).isEqualByComparingTo(BigDecimal.TEN);
        });
        UUID documentId = workspace.documentId();
        long documentVersion = workspace.version();
        assertThatThrownBy(() -> prepareGroupSection(assignment.id())).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> groupSubmissionService.submit(actor(Role.STUDENT), documentId, documentVersion))
                .isInstanceOf(ApiException.class);
        var reset = evaluationRepository.findForAssignmentAndClass(assignment.id(), classId);
        assertThat(reset.stream().map(e -> e.id)).containsExactlyInAnyOrderElementsOf(identities);
        assertThat(reset).allSatisfy(e -> {
            assertThat(e.state.name()).isEqualTo("PUBLISHED");
            assertThat(e.score).isEqualByComparingTo(BigDecimal.TEN);
            assertThat(e.submissionHash).isEqualTo(draftReceipt.receiptHash());
            assertThat(e.publishedAt).isNotNull();
        });
        assertThat(gradebookService.listMyGrades(actor(Role.STUDENT), classId)).hasSize(1);
        assertThat(gradebookService.listMyGrades(member, classId)).hasSize(1);
    }

    @Test
    void groupEvaluationFailureRollsBackSnapshotAndRetriesOnce() throws Exception {
        addGroupMember();
        var assignment = publishAssignment(GradingMode.GRADED, SubmissionMode.GROUP);
        var workspace = prepareGroupSection(assignment.id());
        try {
            rejectEvaluationInsert("MEMBER");
            assertThatThrownBy(() -> groupSubmissionService.submit(actor(Role.STUDENT), workspace.documentId(), workspace.version()))
                    .hasStackTraceContaining("INTEGRATION_EVALUATION_FAILURE");
            assertThat(count("evaluations")).isZero();
            assertThat(jdbc.queryForObject("select receipt_hash from group_documents where id=?", String.class, workspace.documentId())).isNull();
            assertThat(jdbc.queryForObject("select submitted_document::text from group_documents where id=?", String.class, workspace.documentId())).isNull();
            assertThat(jdbc.queryForObject("select count(*) from audit_logs where action='GROUP_SUBMITTED' and result='SUCCESS'", Long.class)).isZero();
        } finally { restoreEvaluationInsert(); }
        groupSubmissionService.submit(actor(Role.STUDENT), workspace.documentId(), workspace.version());
        assertThat(count("evaluations")).isEqualTo(3);
        assertThat(jdbc.queryForObject("select receipt_hash from group_documents where id=?", String.class, workspace.documentId())).isNotBlank();
    }

    SimpleMessageListenerContainer emailConsumer() {
        var listener = new edu.aiplatform.jobs.integration.JobListener(jobDispatcher, json);
        var consumer = new SimpleMessageListenerContainer(rabbitTemplate.getConnectionFactory());
        consumer.setQueueNames("jobs.email");
        consumer.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        consumer.setPrefetchCount(1);
        consumer.setMessageListener((ChannelAwareMessageListener) (message, channel) -> {
            listener.receiveEmail(message, channel);
            handledEmailDeliveries.incrementAndGet();
        });
        consumer.start();
        return consumer;
    }

    UUID enqueueEnrollmentEmail() {
        notificationEventService.handleEvent(
                new DomainEvent(
                        1,
                        UUID.randomUUID(),
                        EventTypes.ENROLLMENT_ACTIVATED,
                        "U05",
                        Instant.now(),
                        UUID.randomUUID().toString(),
                        Map.of(
                                "accountId",
                                actor(Role.STUDENT).accountId().toString(),
                                "classId",
                                classId.toString())));
        return jdbc.queryForObject("select id from email_deliveries", UUID.class);
    }

    String deliveryStatus(UUID deliveryId) {
        return jdbc.queryForObject("select status from email_deliveries where id=?", String.class, deliveryId);
    }

    @Test
    void queuedNotificationSendsOnceAndDuplicateDeliveryIsAcknowledged() {
        UUID id = enqueueEnrollmentEmail();
        var consumer = emailConsumer();
        try {
            org.awaitility.Awaitility.await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> assertThat(deliveryStatus(id)).isEqualTo("SENT"));
            job.enqueue(JobTypes.EMAIL_SEND, Map.of("deliveryId", id.toString()), "duplicate:" + UUID.randomUUID());
            org.awaitility.Awaitility.await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
                assertThat(handledEmailDeliveries).hasValue(2);
                assertThat(new org.springframework.amqp.rabbit.core.RabbitAdmin(rabbitTemplate).getQueueInfo("jobs.email").getMessageCount()).isZero();
                assertThat(consumer.getActiveConsumerCount()).isEqualTo(1);
            });
        } finally { consumer.stop(); consumer.destroy(); }
        assertThat(recordingMail.accepted).hasSize(1);
        assertThat(recordingMail.attempts).hasValue(1);
        assertThat(deliveryStatus(id)).isEqualTo("SENT");
        assertThat(rabbitTemplate.receive("jobs.email")).isNull();
    }

    @Test
    void queuedNotificationRetriesThroughRealRabbitDelayThenSends() {
        recordingMail.failuresRemaining.set(1);
        UUID id = enqueueEnrollmentEmail();
        var consumer = emailConsumer();
        try {
            org.awaitility.Awaitility.await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
                assertThat(recordingMail.attempts).hasValue(1);
                assertThat(deliveryStatus(id)).isEqualTo("PENDING");
                assertThat(new org.springframework.amqp.rabbit.core.RabbitAdmin(rabbitTemplate).getQueueInfo("jobs.retry.30s").getMessageCount()).isEqualTo(1);
            });
            org.awaitility.Awaitility.await().atMost(Duration.ofSeconds(45)).untilAsserted(() -> assertThat(deliveryStatus(id)).isEqualTo("SENT"));
        } finally { consumer.stop(); consumer.destroy(); }
        assertThat(recordingMail.attempts).hasValue(2);
        assertThat(recordingMail.accepted).hasSize(1);
    }

    List<String> raceReservations(List<UUID> accounts, long tokens) throws Exception {
        var pool = Executors.newFixedThreadPool(2);
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        try {
            List<Future<String>> results = new ArrayList<>();
            for (UUID account : accounts) results.add(pool.submit(() -> {
                ready.countDown();
                if (!start.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Concurrent start timed out");
                try {
                    transaction.executeWithoutResult(status -> quotaReservationService.reserve(account, tokens));
                    return "RESERVED";
                } catch (ApiException exception) { return exception.readCode(); }
            }));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            return List.of(results.get(0).get(10, TimeUnit.SECONDS), results.get(1).get(10, TimeUnit.SECONDS));
        } finally { start.countDown(); pool.shutdownNow(); assertThat(pool.awaitTermination(5, TimeUnit.SECONDS)).isTrue(); }
    }

    void configureQuota(long own, long global) {
        workflowClock.freeze();
        quotaPolicyService.savePolicy(actor(Role.ADMIN), own, global, quotaPolicyService.loadPolicy(actor(Role.ADMIN)).version());
    }

    long quotaCounter(String scope, String column) {
        assertThat(Set.of("reserved_tokens", "used_tokens")).contains(column);
        return jdbc.queryForObject("select coalesce(sum(" + column + "),0) from ai_quota_windows where scope_key=?", Long.class, scope);
    }

    @Test
    void sameAccountQuotaRaceAllowsOneReservationThenSettles() throws Exception {
        configureQuota(100, 1000);
        UUID account = actor(Role.TEACHER).accountId();
        assertThat(raceReservations(List.of(account, account), 70)).containsExactlyInAnyOrder("RESERVED", "AI_QUOTA_EXCEEDED");
        assertThat(quotaCounter(account.toString(), "reserved_tokens")).isEqualTo(70);
        assertThat(quotaCounter("GLOBAL", "reserved_tokens")).isEqualTo(70);
        transaction.executeWithoutResult(status -> quotaReservationService.settle(account, quotaReservationService.resolveToday(), 70, 40));
        assertThat(quotaReservationService.calculateRemaining(account).used()).isEqualTo(40);
        assertThat(quotaReservationService.calculateRemaining(account).left()).isEqualTo(60);
        transaction.executeWithoutResult(status -> {
            var reservation = quotaReservationService.reserve(account, 60);
            quotaReservationService.release(account, reservation.day(), reservation.tokens());
        });
        for (String scope : List.of("GLOBAL", account.toString())) {
            assertThat(quotaCounter(scope, "reserved_tokens")).isZero();
            assertThat(quotaCounter(scope, "used_tokens")).isEqualTo(40);
        }
    }

    @Test
    void globalQuotaRaceAcrossAccountsHonorsCapAndRollbackReleases() throws Exception {
        configureQuota(100, 100);
        List<UUID> accounts = List.of(actor(Role.TEACHER).accountId(), actor(Role.SUBJECT_MANAGER).accountId());
        var results = raceReservations(accounts, 70);
        assertThat(results).containsExactlyInAnyOrder("RESERVED", "AI_QUOTA_GLOBAL_EXCEEDED");
        UUID winner = accounts.get(results.indexOf("RESERVED"));
        UUID loser = accounts.get(results.indexOf("AI_QUOTA_GLOBAL_EXCEEDED"));
        assertThat(quotaCounter("GLOBAL", "reserved_tokens")).isEqualTo(70);
        assertThat(quotaCounter(winner.toString(), "reserved_tokens")).isEqualTo(70);
        assertThat(quotaCounter(loser.toString(), "reserved_tokens")).isZero();
        transaction.executeWithoutResult(status -> quotaReservationService.release(winner, quotaReservationService.resolveToday(), 70));
        transaction.executeWithoutResult(status -> { quotaReservationService.reserve(loser, 80); status.setRollbackOnly(); });
        for (String scope : List.of("GLOBAL", winner.toString(), loser.toString())) {
            assertThat(quotaCounter(scope, "reserved_tokens")).isZero();
            assertThat(quotaCounter(scope, "used_tokens")).isZero();
        }
        transaction.executeWithoutResult(status -> quotaReservationService.reserve(loser, 100));
        assertThat(quotaCounter("GLOBAL", "reserved_tokens")).isEqualTo(100);
        assertThat(quotaReservationService.calculateRemaining(winner).left()).isZero();
    }

    @Test
    void dashboardReadsActualOwnersAndRejectsNonAdmin() {
        var dashboard = json.valueToTree(adminDashboardService.loadDashboard(actor(Role.ADMIN)));
        assertThat(dashboard.toString()).contains("AVAILABLE");
        assertThat(dashboard.toString()).doesNotContain("@example.test", "fixture-hash", subjectId.toString());
        assertThatThrownBy(() -> adminDashboardService.loadDashboard(actor(Role.TEACHER)))
                .isInstanceOf(ApiException.class);
        assertThat(count("accounts")).isEqualTo(Role.values().length);
        var view = adminDashboardService.loadDashboard(actor(Role.ADMIN));
        assertThat(view.accounts().data().total()).isEqualTo(Role.values().length);
        assertThat(view.subjects().data().get(edu.aiplatform.subjects.contract.SubjectStatus.ACTIVE)).isEqualTo(1L);
        assertThat(view.classes().data().get(ClassStatus.OPEN)).isEqualTo(1L);
    }
}
