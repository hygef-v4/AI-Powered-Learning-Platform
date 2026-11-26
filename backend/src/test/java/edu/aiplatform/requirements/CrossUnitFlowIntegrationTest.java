package edu.aiplatform.requirements;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import edu.aiplatform.audit.contract.IAudit;
import edu.aiplatform.materials.repository.MaterialModuleRepositoryAdapter;
import edu.aiplatform.materials.service.MaterialAccessPolicy;
import edu.aiplatform.materials.service.MaterialModuleService;
import edu.aiplatform.quizzes.contract.IQuizQuery;
import edu.aiplatform.subjects.contract.SubjectRef;
import edu.aiplatform.subjects.contract.SubjectStatus;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.academics.model.EnrollmentStatus;
import edu.aiplatform.academics.service.AcademicsService;
import edu.aiplatform.ai.contract.AiProvider;
import edu.aiplatform.ai.contract.AiSource;
import edu.aiplatform.ai.contract.AiTaskResult;
import edu.aiplatform.ai.contract.ApplyOutcome;
import edu.aiplatform.ai.service.AiConnectionService;
import edu.aiplatform.ai.service.AiImageSourceRegistry;
import edu.aiplatform.assignments.contract.*;
import edu.aiplatform.assignments.dto.AssignmentViews;
import edu.aiplatform.assignments.service.AssignmentAuthoringService;
import edu.aiplatform.assignments.service.AssignmentCommands;
import edu.aiplatform.assignments.service.AssignmentImportService;
import edu.aiplatform.assignments.service.AssignmentPublicationService;
import edu.aiplatform.assignments.service.ImportFileReader;
import edu.aiplatform.assignmentwork.service.AttemptService;
import edu.aiplatform.assignmentwork.service.StudentAssignmentService;
import edu.aiplatform.files.contract.ArtifactPurpose;
import edu.aiplatform.files.contract.FileUpload;
import edu.aiplatform.files.service.ArtifactService;
import edu.aiplatform.files.service.IStorage;
import edu.aiplatform.files.service.StoredMetadata;
import edu.aiplatform.files.service.UnattachedFileScanner;
import edu.aiplatform.grading.service.GradingCommands;
import edu.aiplatform.grading.service.GradingProposalResultHandler;
import edu.aiplatform.grading.service.GradingProposalService;
import edu.aiplatform.groups.model.GroupSetRules.Draft;
import edu.aiplatform.groups.service.ClassGroupService;
import edu.aiplatform.groups.service.LeaderRequestService;
import edu.aiplatform.identity.contract.*;
import edu.aiplatform.identity.integration.JwtService;
import edu.aiplatform.identity.integration.SessionStore;
import edu.aiplatform.identity.model.OtpPurpose;
import edu.aiplatform.identity.service.AuthService;
import edu.aiplatform.identity.service.OtpService;
import edu.aiplatform.identity.service.PasswordResetService;
import edu.aiplatform.jobs.integration.JobListener;
import edu.aiplatform.jobs.service.JobDispatcher;
import edu.aiplatform.materials.service.MaterialViews;
import edu.aiplatform.materials.service.MaterialQueryService;
import edu.aiplatform.materials.service.MaterialService;
import edu.aiplatform.notifications.integration.NotificationEventListener;
import edu.aiplatform.notifications.service.NotificationEventService;
import edu.aiplatform.shared.error.ApiException;
import jakarta.servlet.http.Cookie;
import java.awt.Color;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.*;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.utility.DockerImageName;

/**
 * Cross-unit flows that had no integration test: real production beans, migrations, PostgreSQL, Redis,
 * RabbitMQ and local file storage. Only the external mail provider is recorded and AI provider
 * results are delivered through the real result handlers; no remote provider is called.
 */
@SpringBootTest(classes = PlatformWorkflowIntegrationTest.Application.class, properties = {
        "platform.identity.bcrypt-cost=4", "platform.identity.mail-from=integration@example.test",
        "platform.identity.bootstrap-admin-email=", "platform.identity.mail-transport=smtp",
        "platform.files.drive-service-account-key=",
        "platform.files.shared-drive-id=", "platform.backup.folder-id=",
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "spring.rabbitmq.listener.direct.auto-startup=false",
        "spring.main.allow-bean-definition-overriding=false"
})
@AutoConfigureMockMvc
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CrossUnitFlowIntegrationTest {
    static final String JWT_SECRET = UUID.randomUUID().toString() + UUID.randomUUID();
    static final String FILE_SECRET = UUID.randomUUID().toString() + UUID.randomUUID();
    static final String ENCRYPTION_KEY = Base64.getEncoder().encodeToString(
            UUID.randomUUID().toString().replace("-", "").getBytes(StandardCharsets.UTF_8));
    static final String UNATTACHED_KEY = "file:unattached";
    static final String SVG_CSP = "default-src 'none'; style-src 'unsafe-inline'; sandbox";

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
    static final Path FILES = PlatformWorkflowIntegrationTest.createDirectory();

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
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;
    @Autowired TransactionTemplate transaction;
    @Autowired MockMvc mvc;
    @Autowired SessionStore sessionStore;
    @Autowired JwtService jwtService;
    @Autowired RabbitTemplate rabbitTemplate;
    @Autowired StringRedisTemplate redisTemplate;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JobDispatcher jobDispatcher;
    @Autowired PlatformWorkflowIntegrationTest.RecordingMail recordingMail;
    @Autowired PlatformWorkflowIntegrationTest.WorkflowClock workflowClock;
    @Autowired AcademicsService academicsService;
    @Autowired AiConnectionService aiConnectionService;
    @Autowired AiImageSourceRegistry aiImageSourceRegistry;
    @Autowired AssignmentAuthoringService assignmentAuthoringService;
    @Autowired AssignmentPublicationService assignmentPublicationService;
    @Autowired AssignmentImportService assignmentImportService;
    @Autowired AttemptService attemptService;
    @Autowired StudentAssignmentService studentAssignmentService;
    @Autowired GradingProposalService gradingProposalService;
    @Autowired GradingProposalResultHandler gradingProposalResultHandler;
    @Autowired OtpService otpService;
    @Autowired PasswordResetService passwordResetService;
    @Autowired AuthService authService;
    @Autowired ClassGroupService classGroupService;
    @Autowired LeaderRequestService leaderRequestService;
    @Autowired NotificationEventService notificationEventService;
    @Autowired MaterialService materialService;
    @Autowired MaterialQueryService materialQueryService;
    @Autowired ArtifactService artifactService;
    @Autowired IStorage storage;
    @Autowired UnattachedFileScanner unattachedFileScanner;
    final Map<Role, Actor> actors = new EnumMap<>(Role.class);
    UUID subjectId;
    UUID classId;
    UUID moduleId;

    @BeforeEach
    void seedIndependentFixture() {
        workflowClock.reset();
        recordingMail.reset();
        String tables = jdbc.queryForObject("select string_agg(quote_ident(tablename), ',') from pg_tables "
                + "where schemaname='public' and tablename not in ('flyway_schema_history','system_settings')",
                String.class);
        jdbc.execute("truncate " + tables + " cascade");
        redisTemplate.delete(UNATTACHED_KEY);
        actors.clear();
        for (Role role : Role.values()) {
            actors.put(role, createAccount(role));
        }
        Instant now = Instant.now();
        subjectId = UUID.randomUUID();
        classId = UUID.randomUUID();
        moduleId = UUID.randomUUID();
        jdbc.update("""
                insert into subjects(id,code,name,status,manager_id,created_at,updated_at)
                values (?,'INTEGRATION','Integration subject','ACTIVE',?,?,?)
                """, subjectId, actor(Role.SUBJECT_MANAGER).accountId(), Timestamp.from(now), Timestamp.from(now));
        classId = createClass("INTEGRATION");
        enroll(actor(Role.STUDENT).accountId(), classId);
        jdbc.update("insert into material_modules(id,subject_id,title,order_no,created_by,created_at,updated_at) "
                + "values (?,?,'Module',0,?,now(),now())", moduleId, subjectId, actor(Role.TEACHER).accountId());
        for (String queue : List.of("jobs.email", "jobs.notification", "jobs.ai")) {
            while (rabbitTemplate.receive(queue) != null) {
                // No worker consumes these disposable queues; discard the previous scenario's messages.
            }
        }
    }

    Actor actor(Role role) {
        return actors.get(role);
    }

    Actor createAccount(Role role) {
        Actor actor = new Actor(UUID.randomUUID(), role);
        Instant now = Instant.now();
        jdbc.update("""
                insert into accounts(id,school_email,role,status,password_hash,display_name,created_at,updated_at,version)
                values (?,?,?,'ACTIVE','fixture-hash',?,?,?,0)
                """, actor.accountId(), actor.accountId() + "@example.test", role.name(), role.name(),
                Timestamp.from(now), Timestamp.from(now));
        return actor;
    }

    UUID createClass(String code) {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        jdbc.update("""
                insert into course_classes(id,subject_id,code,name,status,teacher_id,starts_on,created_at,updated_at)
                values (?,?,?,?,'OPEN',?,DATE '2026-11-14',?,?)
                """, id, subjectId, code, code + " class", actor(Role.TEACHER).accountId(),
                Timestamp.from(now), Timestamp.from(now));
        return id;
    }

    void enroll(UUID accountId, UUID courseClassId) {
        jdbc.update("insert into enrollments(class_id,account_id,status,source,enrolled_at) values (?,?,'ACTIVE','MANUAL',now() - interval '1 day')",
                courseClassId, accountId);
    }

    long count(String sql, Object... arguments) {
        return jdbc.queryForObject(sql, Long.class, arguments);
    }

    /** Builds a random password per run that meets the password policy (letters and digits). */
    static String generateTestPassword() {
        return "Pw9" + UUID.randomUUID().toString().replace("-", "");
    }

    /** Builds a client address in the TEST-NET-2 documentation range (198.51.100.0/24). */
    static String buildDocumentationIp(int host) {
        return "198.51.100." + host;
    }

    Cookie loginCookie(Actor actor) {
        var session = sessionStore.createSession(actor.accountId(), null);
        return new Cookie("access_token", jwtService.issue(actor.accountId(), actor.role(), session.sid()));
    }

    void addPersonalAiKey() {
        aiConnectionService.addPersonal(actor(Role.TEACHER), AiProvider.OPENAI,
                new AiConnectionService.SaveCommand("integration-key-never-sent", "gpt-6.1-sol", null));
    }

    AssignmentViews.Detail publishAssignment(GradingMode mode, List<UUID> referenceMaterialIds) throws Exception {
        var teacher = actor(Role.TEACHER);
        var created = assignmentAuthoringService.createForClass(teacher, classId,
                new AssignmentCommands.Create(SubmissionMode.INDIVIDUAL, mode, false, "Integration assignment"));
        long version = created.version();
        if (!referenceMaterialIds.isEmpty()) {
            version = assignmentAuthoringService.updateDetails(teacher, created.id(), new AssignmentCommands.Details(
                    version, null, null, null, null, null, referenceMaterialIds, null, null)).version();
        }
        var skeleton = new DocumentModel(List.of(new DocumentBlock(UUID.randomUUID(), BlockOrigin.TEACHER,
                BlockType.HEADING, json.readTree("{\"style\":\"H1\",\"text\":\"Answer\"}"))));
        var prepared = assignmentAuthoringService.saveSkeleton(teacher, created.id(),
                new AssignmentCommands.Skeleton(version, skeleton, "Provide an answer", false));
        assignmentAuthoringService.updateRubric(teacher, prepared.id(), prepared.parts().get(0).partId(),
                new AssignmentCommands.RubricUpdate(prepared.version(), List.of(new RubricCriterion(UUID.randomUUID(),
                        "Correctness", "", CriterionKind.TEXT,
                        List.of(new RubricItem(UUID.randomUUID(), "Correct answer", BigDecimal.TEN))))));
        prepared = assignmentAuthoringService.loadAssignment(teacher, prepared.id());
        prepared = assignmentAuthoringService.review(teacher, prepared.id(), prepared.version());
        return assignmentPublicationService.publish(teacher, prepared.id(),
                new AssignmentCommands.Publish(prepared.version(), null, true, Instant.now().plusSeconds(3600),
                        null, 2, false));
    }

    UUID submitAnswer(AssignmentViews.Detail assignment) {
        var student = actor(Role.STUDENT);
        var attempt = attemptService.start(student, assignment.id(), classId).attempt();
        var blocks = new ArrayList<>(attempt.content().blocks());
        var content = json.createObjectNode();
        content.putArray("runs").addObject().put("text", "Photosynthesis turns light into chemical energy.");
        blocks.add(new DocumentBlock(UUID.randomUUID(), BlockOrigin.STUDENT, BlockType.PARAGRAPH, content));
        var saved = attemptService.saveAttempt(student, attempt.attemptId(), attempt.contentVersion(), new DocumentModel(blocks));
        attemptService.submit(student, attempt.attemptId(), saved.contentVersion());
        return attempt.attemptId();
    }

    UUID insertMaterial(String scope, UUID courseClassId, String title) {
        UUID id = UUID.randomUUID();
        jdbc.update("insert into materials(id,scope,subject_id,class_id,module_id,kind,title,order_no,status,current_version_no,"
                + "created_by,created_at,updated_at) values (?,?,?,?,?,'FILE',?,0,'ACTIVE',1,?,now() - interval '1 day',now() - interval '1 day')",
                id, scope, subjectId, courseClassId, moduleId, title, actor(Role.TEACHER).accountId());
        jdbc.update("insert into material_versions(material_id,version_no,file_id,file_name,mime_type,byte_size,text_status,"
                + "extracted_text,created_by,created_at) values (?,1,'test-only-file','source.pdf','application/pdf',100,'EXTRACTED',"
                + "'Photosynthesis happens in chloroplasts.',?,now())", id, actor(Role.TEACHER).accountId());
        return id;
    }

    /** Takes each waiting message from a queue and hands it to the production listener on the same channel. */
    int deliverQueuedMessages(String queue, QueueConsumer consumer) {
        return rabbitTemplate.execute(channel -> {
            var first = Awaitility.await().atMost(Duration.ofSeconds(5))
                    .until(() -> channel.basicGet(queue, false), Objects::nonNull);
            int delivered = 0;
            for (var delivery = first; delivery != null; delivery = channel.basicGet(queue, false)) {
                var properties = new MessageProperties();
                properties.setDeliveryTag(delivery.getEnvelope().getDeliveryTag());
                properties.setReceivedRoutingKey(delivery.getEnvelope().getRoutingKey());
                if (delivery.getProps().getHeaders() != null) {
                    delivery.getProps().getHeaders().forEach((name, value) -> properties.setHeader(name, value));
                }
                consumer.accept(new Message(delivery.getBody(), properties), channel);
                delivered++;
            }
            return delivered;
        });
    }

    @FunctionalInterface
    interface QueueConsumer {
        void accept(Message message, com.rabbitmq.client.Channel channel) throws IOException;
    }

    int deliverNotificationEvents() {
        var listener = new NotificationEventListener(notificationEventService, json, rabbitTemplate);
        return deliverQueuedMessages("jobs.notification", listener::receiveEventMessage);
    }

    long notificationsFor(UUID recipientId, String type) {
        return count("select count(*) from notifications where recipient_id=? and type=?", recipientId, type);
    }

    byte[] createPagePng(String text) throws IOException {
        var image = new BufferedImage(1000, 1400, BufferedImage.TYPE_INT_RGB);
        var graphics = image.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
            graphics.setColor(Color.BLACK);
            graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 32));
            graphics.drawString(text, 60, 120);
            graphics.drawString("Answer in at least three sentences.", 60, 180);
        } finally {
            graphics.dispose();
        }
        var output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }

    String putStoredFile(UUID ownerId, String fileName, String mediaType, byte[] content) throws Exception {
        Path source = Files.createTempFile(FILES, "download-", ".tmp");
        Files.write(source, content);
        String sha256 = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        return storage.putFile(ArtifactPurpose.DOCUMENT_IMAGE, source,
                new StoredMetadata(ownerId, ArtifactPurpose.DOCUMENT_IMAGE, fileName, mediaType, content.length, sha256));
    }

    @Test
    void aiGradingSuggestionIsStoredAsReadyProposalWithoutChangingTheTeacherGrade() throws Exception {
        addPersonalAiKey();
        var assignment = publishAssignment(GradingMode.GRADED, List.of());
        UUID attemptId = submitAnswer(assignment);
        var teacher = actor(Role.TEACHER);
        var current = assignmentAuthoringService.loadAssignment(teacher, assignment.id());
        assignmentPublicationService.closeAssignment(teacher, assignment.id(), current.version());
        UUID evaluationId = jdbc.queryForObject("select id from evaluations where attempt_id=?", UUID.class, attemptId);

        var proposal = gradingProposalService.requestProposal(teacher, evaluationId,
                new GradingCommands.AiRequest(AiSource.PERSONAL, AiProvider.OPENAI, "grade-ai-1"));
        assertThat(proposal.status).isEqualTo("PENDING");
        assertThat(proposal.taskId).isNotNull();
        assertThat(count("select count(*) from ai_tasks where id=? and task_type='GRADING_PROPOSAL' and source='PERSONAL'",
                proposal.taskId)).isEqualTo(1);
        var repeated = gradingProposalService.requestProposal(teacher, evaluationId,
                new GradingCommands.AiRequest(AiSource.PERSONAL, AiProvider.OPENAI, "grade-ai-1"));
        assertThat(repeated.id).isEqualTo(proposal.id);
        assertThat(count("select count(*) from ai_tasks")).isEqualTo(1);
        assertThatThrownBy(() -> gradingProposalService.requestProposal(actor(Role.STUDENT), evaluationId,
                new GradingCommands.AiRequest(AiSource.PERSONAL, AiProvider.OPENAI, "grade-ai-student")))
                .isInstanceOf(ApiException.class);

        var part = assignment.parts().get(0);
        var rubric = assignmentAuthoringService.loadRubric(teacher, assignment.id(), part.partId());
        UUID rubricId = rubric.rubricId();
        UUID itemId = rubric.criteria().get(0).items().get(0).itemId();
        String output = """
                {"checks":{"%1$s":["%2$s"]},"itemScoresByRubric":{"%1$s":{"%2$s":10.00}},"score":"10.00",
                 "feedback":"Clear explanation","sections":[],
                 "structuredFeedback":{"schemaVersion":1,"summary":{"strengths":["Correct idea"],"improvements":[],"nextSteps":[]},
                  "items":[{"rubricId":"%1$s","itemId":"%2$s","evidence":"Light into chemical energy",
                   "reason":"Matches the rubric item","suggestion":"Add an example","evidenceBlockId":"%3$s",
                   "requiresReview":false,"reviewReason":null}]}}
                """.formatted(rubricId, itemId, part.partId());
        var result = new AiTaskResult(proposal.taskId, "GRADING_PROPOSAL", teacher, "evaluation:" + evaluationId,
                proposal.versionNo, output);
        transaction.executeWithoutResult(status ->
                assertThat(gradingProposalResultHandler.onReady(result)).isEqualTo(ApplyOutcome.APPLIED));

        assertThat(jdbc.queryForObject("select status from grading_proposals where id=?", String.class, proposal.id))
                .isEqualTo("READY");
        assertThat(jdbc.queryForObject("select status from evaluations where id=?", String.class, evaluationId))
                .isEqualTo("PENDING");
        assertThat(jdbc.queryForObject("select score from evaluations where id=?", BigDecimal.class, evaluationId))
                .isNull();
        assertThat(count("select count(*) from notifications")).isZero();
    }

    @Test
    void assignmentImportStoresNormalizedPagesAndQueuesOneVisionTaskPerRequest() throws Exception {
        addPersonalAiKey();
        var teacher = actor(Role.TEACHER);
        var assignment = assignmentAuthoringService.createForClass(teacher, classId,
                new AssignmentCommands.Create(SubmissionMode.INDIVIDUAL, GradingMode.GRADED, false, "Imported exam"));
        long draftsBefore = count("select count(*) from assignment_ai_drafts");
        assertThatThrownBy(() -> assignmentImportService.startImport(teacher, assignment.id(),
                List.of(new ImportFileReader.ImportFile("fake.png", "not an image".getBytes(StandardCharsets.UTF_8))),
                new AssignmentImportService.StartImport("import-fake", AiSource.PERSONAL, AiProvider.OPENAI)))
                .isInstanceOf(ApiException.class);
        assertThat(count("select count(*) from assignment_ai_drafts")).isEqualTo(draftsBefore);
        assertThat(count("select count(*) from ai_tasks")).isZero();

        var page = new ImportFileReader.ImportFile("exam-page.png", createPagePng("Question 1: Explain photosynthesis."));
        var draft = assignmentImportService.startImport(teacher, assignment.id(), List.of(page),
                new AssignmentImportService.StartImport("import-1", AiSource.PERSONAL, AiProvider.OPENAI));
        assertThat(draft.status()).isEqualTo("PENDING");
        assertThat(draft.taskId()).isNotNull();
        assertThat(count("select count(*) from ai_tasks where id=? and task_type='ASSIGNMENT_IMPORT' and source='PERSONAL'",
                draft.taskId())).isEqualTo(1);
        assertThat(count("select count(*) from assignment_import_pages where draft_id=?", draft.draftId())).isEqualTo(1);
        var image = aiImageSourceRegistry.findSource("ASSIGNMENT_IMPORT").orElseThrow().load(draft.draftId() + "/1");
        assertThat(image).isPresent();
        assertThat(image.get().bytes()).isNotEmpty();
        assertThat(image.get().mediaType()).startsWith("image/");
        assertThat(jdbc.queryForObject("select input_payload::text from ai_tasks where id=?", String.class, draft.taskId()))
                .doesNotContain("integration-key-never-sent");

        var repeated = assignmentImportService.startImport(teacher, assignment.id(), List.of(page),
                new AssignmentImportService.StartImport("import-1", AiSource.PERSONAL, AiProvider.OPENAI));
        assertThat(repeated.draftId()).isEqualTo(draft.draftId());
        assertThat(count("select count(*) from ai_tasks")).isEqualTo(1);
        assertThatThrownBy(() -> assignmentImportService.startImport(actor(Role.STUDENT), assignment.id(), List.of(page),
                new AssignmentImportService.StartImport("import-student", AiSource.PERSONAL, AiProvider.OPENAI)))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void passwordResetDeliversOtpThroughQueueRevokesSessionsAndAcceptsOnlyTheNewPassword() throws Exception {
        var student = actor(Role.STUDENT);
        String email = student.accountId() + "@example.test";
        String oldPassword = generateTestPassword();
        String newPassword = generateTestPassword();
        jdbc.update("update accounts set password_hash=? where id=?", passwordEncoder.encode(oldPassword), student.accountId());
        authService.login(email, oldPassword, buildDocumentationIp(10));
        var oldSession = sessionStore.createSession(student.accountId(), null);
        var oldCookie = new Cookie("access_token", jwtService.issue(student.accountId(), student.role(), oldSession.sid()));
        mvc.perform(get("/api/v1/me/classes").cookie(oldCookie)).andExpect(status().isOk());

        otpService.request(email, OtpPurpose.PASSWORD_RESET, buildDocumentationIp(11));
        var jobListener = new JobListener(jobDispatcher, json);
        assertThat(deliverQueuedMessages("jobs.email", jobListener::receiveEmail)).isEqualTo(1);
        assertThat(recordingMail.accepted).hasSize(1);
        var mail = recordingMail.accepted.get(0);
        assertThat(mail.to()).isEqualTo(email);
        var matcher = Pattern.compile("\\b(\\d{6})\\b").matcher(mail.textBody());
        assertThat(matcher.find()).isTrue();
        String code = matcher.group(1);
        assertThatThrownBy(() -> otpService.verify(email, OtpPurpose.PASSWORD_RESET,
                code.equals("000000") ? "111111" : "000000", buildDocumentationIp(12))).isInstanceOf(ApiException.class);

        String ticket = otpService.verify(email, OtpPurpose.PASSWORD_RESET, code, buildDocumentationIp(12));
        passwordResetService.complete(ticket, newPassword);

        assertThat(sessionStore.findSession(oldSession.sid())).isEmpty();
        mvc.perform(get("/api/v1/me/classes").cookie(oldCookie)).andExpect(status().isUnauthorized());
        assertThat(count("select count(*) from audit_logs where action='PASSWORD_RESET' and actor_id=?",
                student.accountId())).isZero();
        assertThatThrownBy(() -> authService.login(email, oldPassword, buildDocumentationIp(13))).isInstanceOf(ApiException.class);
        assertThat(authService.login(email, newPassword, buildDocumentationIp(14)).accessToken()).isNotBlank();
        assertThatThrownBy(() -> passwordResetService.complete(ticket, generateTestPassword()))
                .isInstanceOf(ApiException.class);
        assertThat(mail.textBody() + mail.htmlBody()).doesNotContain(oldPassword, newPassword);
    }

    @Test
    void leaderChangeRequestReachesTeacherAndApprovalNotifiesEveryMemberThroughRabbit() {
        var teacher = actor(Role.TEACHER);
        var leader = actor(Role.STUDENT);
        var member = createAccount(Role.STUDENT);
        enroll(member.accountId(), classId);
        var before = classGroupService.view(teacher, classId);
        var saved = classGroupService.saveGroups(teacher, classId, new ClassGroupService.SaveCommand(before.version(),
                List.of(new Draft("new", null, "Integration group", leader.accountId(),
                        List.of(leader.accountId(), member.accountId())))));
        UUID groupId = saved.groups().get(0).id();
        rabbitTemplate.execute(channel -> channel.queuePurge("jobs.notification"));

        var request = leaderRequestService.submit(member, groupId, "I can coordinate the next milestone",
                member.accountId());
        assertThatThrownBy(() -> leaderRequestService.submit(leader, groupId, "Second request", member.accountId()))
                .isInstanceOf(ApiException.class);
        assertThat(deliverNotificationEvents()).isGreaterThanOrEqualTo(1);
        assertThat(notificationsFor(teacher.accountId(), "LEADER_CHANGE_REQUESTED")).isEqualTo(1);
        assertThat(notificationsFor(leader.accountId(), "LEADER_CHANGE_REQUESTED")).isZero();

        assertThatThrownBy(() -> leaderRequestService.approve(member, request.id(), member.accountId()))
                .isInstanceOf(ApiException.class);
        leaderRequestService.approve(teacher, request.id(), null);
        assertThat(deliverNotificationEvents()).isGreaterThanOrEqualTo(1);
        assertThat(notificationsFor(leader.accountId(), "LEADER_CHANGED")).isEqualTo(1);
        assertThat(notificationsFor(member.accountId(), "LEADER_CHANGED")).isEqualTo(1);
        assertThat(classGroupService.view(teacher, classId).groups().get(0).leaderId()).isEqualTo(member.accountId());
        assertThat(jdbc.queryForObject("select status from leader_change_requests where id=?", String.class, request.id()))
                .isEqualTo("APPROVED");
    }

    @Test
    void attachedMaterialsAreShownToEnrolledStudentOnlyWhileActive() throws Exception {
        UUID otherClassId = createClass("OTHER");
        UUID subjectMaterial = insertMaterial("SUBJECT", null, "Subject handout");
        UUID classMaterial = insertMaterial("CLASS", classId, "Class slides");
        UUID foreignMaterial = insertMaterial("CLASS", otherClassId, "Other class slides");
        var teacher = actor(Role.TEACHER);
        var draft = assignmentAuthoringService.createForClass(teacher, classId,
                new AssignmentCommands.Create(SubmissionMode.INDIVIDUAL, GradingMode.GRADED, false, "Scope check"));
        assertThatThrownBy(() -> assignmentAuthoringService.updateDetails(teacher, draft.id(), new AssignmentCommands.Details(
                draft.version(), null, null, null, null, null, List.of(classMaterial, foreignMaterial), null, null)))
                .isInstanceOfSatisfying(ApiException.class, error -> assertThat(error.readCode()).isEqualTo("MATERIAL_NOT_ALLOWED"));

        var assignment = publishAssignment(GradingMode.GRADED, List.of(subjectMaterial, classMaterial));
        var student = actor(Role.STUDENT);
        var detail = studentAssignmentService.loadAssignment(student, assignment.id(), classId);
        assertThat(detail.referenceMaterialIds()).containsExactlyInAnyOrder(subjectMaterial, classMaterial);
        assertThat(materialQueryService.listClassMaterials(student, classId, true, detail.referenceMaterialIds()))
                .extracting(MaterialViews.Detail::id).containsExactlyInAnyOrder(subjectMaterial, classMaterial);

        long materialVersion = jdbc.queryForObject("select version from materials where id=?", Long.class, classMaterial);
        materialService.archiveMaterial(teacher, classMaterial, materialVersion);
        assertThat(materialQueryService.listClassMaterials(student, classId, true, detail.referenceMaterialIds()))
                .extracting(MaterialViews.Detail::id).containsExactly(subjectMaterial);
        // Lưu trữ không xóa (yêu cầu 2026-11-26): giảng viên vẫn thấy, vẫn khôi phục được với quyền thật; sửa thì bị chặn.
        var archivedView = materialQueryService.listClassMaterials(teacher, classId, false, null).stream()
                .filter(m -> m.id().equals(classMaterial)).findFirst().orElseThrow();
        assertThat(archivedView.status()).isEqualTo(edu.aiplatform.materials.model.Material.Status.ARCHIVED);
        assertThat(archivedView.canManage()).as("nút khôi phục cần canManage").isTrue();
        long archivedVersion = jdbc.queryForObject("select version from materials where id=?", Long.class, classMaterial);
        assertThatThrownBy(() -> materialService.archiveMaterial(teacher, classMaterial, archivedVersion))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.readCode()).isEqualTo("MATERIAL_ARCHIVED"));
        // Học liệu môn đã lưu trữ thì giảng viên không thấy trong danh sách (chỉ Chủ nhiệm môn khôi phục được).
        long subjectVersion = jdbc.queryForObject("select version from materials where id=?", Long.class, subjectMaterial);
        materialService.archiveMaterial(actor(Role.SUBJECT_MANAGER), subjectMaterial, subjectVersion);
        assertThat(materialQueryService.listClassMaterials(teacher, classId, false, null))
                .extracting(MaterialViews.Detail::id).doesNotContain(subjectMaterial).contains(classMaterial);
        materialService.restoreMaterial(actor(Role.SUBJECT_MANAGER), subjectMaterial, subjectVersion + 1);
        materialService.restoreMaterial(teacher, classMaterial, archivedVersion);
        assertThat(materialQueryService.listClassMaterials(student, classId, true, detail.referenceMaterialIds()))
                .extracting(MaterialViews.Detail::id).containsExactlyInAnyOrder(subjectMaterial, classMaterial);

        academicsService.changeEnrollmentStatus(actor(Role.SUBJECT_MANAGER), classId, student.accountId(),
                EnrollmentStatus.REMOVED);
        assertThatThrownBy(() -> studentAssignmentService.loadAssignment(student, assignment.id(), classId))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> materialQueryService.listClassMaterials(student, classId, true, List.of(subjectMaterial)))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void docxImportKeepsTheEmbeddedImageAfterSavingTheAssignment() throws Exception {
        var teacher = actor(Role.TEACHER);
        var assignment = assignmentAuthoringService.createForClass(teacher, classId,
                new AssignmentCommands.Create(SubmissionMode.INDIVIDUAL, GradingMode.GRADED, false, "DOCX import"));
        byte[] png = createPagePng("Embedded image");
        var output = new ByteArrayOutputStream();
        try (var docx = new XWPFDocument()) {
            docx.createParagraph().createRun().addPicture(new ByteArrayInputStream(png), XWPFDocument.PICTURE_TYPE_PNG,
                    "embedded.png", Units.toEMU(320), Units.toEMU(160));
            docx.write(output);
        }
        var file = new MockMultipartFile("file", "import.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", output.toByteArray());
        var session = loginCookie(teacher);
        String csrf = UUID.randomUUID().toString();
        byte[] response = mvc.perform(multipart("/api/v1/assignments/{id}/skeleton:import-docx", assignment.id())
                        .file(file).cookie(session, new Cookie("XSRF-TOKEN", csrf)).header("X-XSRF-TOKEN", csrf))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        var preview = json.readValue(response, AssignmentViews.ImportPreview.class);
        assertThat(preview.warnings()).extracting(ValidationIssue::code).doesNotContain("IMAGE_SKIPPED");
        assertThat(preview.imageCount()).isEqualTo(1);
        DocumentBlock image = preview.document().blocks().get(0);
        assertThat(image.type()).isEqualTo(BlockType.IMAGE);
        assertThat(count("select count(*) from unattached_files")).isEqualTo(1);

        assignmentAuthoringService.saveSkeleton(teacher, assignment.id(),
                new AssignmentCommands.Skeleton(assignment.version(), preview.document(), null, true));
        assertThat(count("select count(*) from unattached_files")).isZero();
        String imageUrl = assignmentAuthoringService.resolveImageUrl(teacher, assignment.id(), image.id());
        var imageDownload = mvc.perform(get(imageUrl).cookie(session)).andReturn();
        mvc.perform(asyncDispatch(imageDownload)).andExpect(status().isOk()).andExpect(content().bytes(png));
    }

    @Test
    void unattachedUploadIsDeletedByScannerWhileAttachedUploadIsKept() throws Exception {
        var student = actor(Role.STUDENT);
        byte[] png = createPagePng("Diagram");
        var keptRef = artifactService.store(student, ArtifactPurpose.DOCUMENT_IMAGE,
                new FileUpload("kept.png", png.length, new ByteArrayInputStream(png)));
        artifactService.store(student, ArtifactPurpose.DOCUMENT_IMAGE,
                new FileUpload("abandoned.png", png.length, new ByteArrayInputStream(png)));
        assertThat(jdbc.queryForObject("select count(*) from unattached_files", Long.class)).isEqualTo(2);

        var kept = artifactService.attach(keptRef, student, ArtifactPurpose.DOCUMENT_IMAGE);
        var waiting = jdbc.queryForList("select file_id from unattached_files", String.class);
        assertThat(waiting).hasSize(1).doesNotContain(kept.fileId());
        String abandonedId = waiting.iterator().next();
        assertThat(storage.loadMetadata(abandonedId)).isPresent();

        unattachedFileScanner.scan(Instant.now());
        assertThat(storage.loadMetadata(abandonedId)).isPresent();

        redisTemplate.delete(UNATTACHED_KEY);
        unattachedFileScanner.scan(Instant.now().plus(Duration.ofDays(2)));
        assertThat(storage.loadMetadata(abandonedId)).isEmpty();
        assertThat(jdbc.queryForObject("select count(*) from unattached_files", Long.class)).isZero();
        assertThat(storage.loadMetadata(kept.fileId())).isPresent();
        try (var content = storage.open(kept.fileId())) {
            assertThat(content.readAllBytes()).isEqualTo(png);
        }
    }

    @Test
    void svgDownloadCarriesSandboxHeadersAndForeignAccountIsDeniedWithoutAudit() throws Exception {
        var student = actor(Role.STUDENT);
        byte[] svg = "<svg xmlns=\"http://www.w3.org/2000/svg\"><script>alert(1)</script><rect width=\"10\" height=\"10\"/></svg>"
                .getBytes(StandardCharsets.UTF_8);
        String svgId = putStoredFile(student.accountId(), "diagram.svg", "image/svg+xml", svg);
        String svgUrl = artifactService.issueDownloadToken(svgId, student.accountId()).url();

        mvc.perform(get(svgUrl).cookie(loginCookie(actor(Role.TEACHER))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("DOWNLOAD_TOKEN_NOT_FOUND"));
        assertThat(count("select count(*) from audit_logs where action='FILE_DOWNLOAD' and actor_id=?",
                actor(Role.TEACHER).accountId())).isZero();

        var started = mvc.perform(get(svgUrl).cookie(loginCookie(student))).andReturn();
        mvc.perform(asyncDispatch(started))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/svg+xml"))
                .andExpect(header().string("Content-Security-Policy", SVG_CSP))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Cache-Control", "private, no-store"))
                .andExpect(content().bytes(svg));

        byte[] png = createPagePng("Plain image");
        String pngId = putStoredFile(student.accountId(), "plain.png", "image/png", png);
        var pngStarted = mvc.perform(get(artifactService.issueDownloadToken(pngId, student.accountId()).url())
                .cookie(loginCookie(student))).andReturn();
        mvc.perform(asyncDispatch(pngStarted))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/png"))
                .andExpect(header().doesNotExist("Content-Security-Policy"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
    }


    @Test
    void concurrentFirstOpenOfANewSubjectCreatesNoModule() throws Exception {
        jdbc.update("delete from material_modules where subject_id = ?", subjectId);
        var manager = actor(Role.SUBJECT_MANAGER);
        var policy = mock(MaterialAccessPolicy.class);
        when(policy.canManageModules(manager, subjectId)).thenReturn(true);
        when(policy.requireSubject(subjectId)).thenReturn(
                new SubjectRef(subjectId, "TEST", "Synthetic subject",
                        SubjectStatus.ACTIVE, manager.accountId()));
        var repository = spy(new MaterialModuleRepositoryAdapter(
                JdbcClient.create(jdbc.getDataSource())));
        var firstReads = new CountDownLatch(2);
        var reads = new AtomicInteger();
        doAnswer(call -> {
            Object modules = call.callRealMethod();
            if (reads.getAndIncrement() < 2) {
                firstReads.countDown();
                assertThat(firstReads.await(10, TimeUnit.SECONDS)).isTrue();
            }
            return modules;
        }).when(repository).listModules(subjectId);
        var moduleService = new MaterialModuleService(
                repository,
                policy,
                mock(IQuizQuery.class),
                java.time.Clock.systemUTC());
        var transaction = new TransactionTemplate(
                new DataSourceTransactionManager(jdbc.getDataSource()));
        var pool = Executors.newFixedThreadPool(2);
        try {
            var first = pool.submit(() -> transaction.execute(status -> moduleService.listSubjectModules(manager, subjectId)));
            var second = pool.submit(() -> transaction.execute(status -> moduleService.listSubjectModules(manager, subjectId)));
            first.get(15, TimeUnit.SECONDS);
            second.get(15, TimeUnit.SECONDS);
        } finally {
            pool.shutdownNow();
        }
        // Môn mới không tự có module (yêu cầu 2026-11-26), kể cả khi hai người mở trang cùng lúc.
        assertThat(jdbc.queryForList("select title from material_modules where subject_id = ?", String.class, subjectId))
                .isEmpty();
    }

}
