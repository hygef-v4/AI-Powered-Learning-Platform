package edu.aiplatform.requirements;

import edu.aiplatform.shared.testsupport.SharedPostgres;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import edu.aiplatform.backup.service.IBackupStore;
import edu.aiplatform.files.config.FilesProperties;
import edu.aiplatform.files.contract.ArtifactPurpose;
import edu.aiplatform.files.contract.FileUpload;
import edu.aiplatform.files.integration.DriveClientFactory;
import edu.aiplatform.files.integration.DriveFolders;
import edu.aiplatform.files.integration.DriveHealthIndicator;
import edu.aiplatform.files.service.ArtifactService;
import edu.aiplatform.files.service.IStorage;
import edu.aiplatform.files.service.UnattachedFileScanner;
import edu.aiplatform.identity.contract.*;
import edu.aiplatform.identity.integration.JwtService;
import edu.aiplatform.identity.integration.SessionStore;
import edu.aiplatform.jobs.contract.IJob;
import edu.aiplatform.jobs.contract.JobTypes;
import edu.aiplatform.jobs.integration.JobListener;
import edu.aiplatform.jobs.service.JobDispatcher;
import edu.aiplatform.shared.error.ApiException;
import jakarta.servlet.http.Cookie;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import javax.imageio.ImageIO;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.*;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.utility.DockerImageName;

/**
 * Google Drive storage with personal-Drive production settings. The real Google Drive
 * client library, {@link edu.aiplatform.files.integration.GoogleDriveStorageAdapter}, {@link DriveFolders},
 * {@code DriveBackupStore}, upload/download services, Redis, RabbitMQ and PostgreSQL are used; only the Drive client
 * points at a local HTTP server that implements the Drive v3 REST calls the platform makes (resumable upload,
 * metadata, media download, list, folder create, delete). The OAuth token exchange is not exercised.
 */
@SpringBootTest(classes = {PlatformWorkflowIntegrationTest.Application.class, DriveStorageIntegrationTest.FakeDriveConfig.class},
        properties = {
                "platform.identity.bcrypt-cost=4", "platform.identity.mail-from=integration@example.test",
                "platform.identity.bootstrap-admin-email=", "platform.identity.mail-transport=smtp",
                "platform.files.drive-service-account-key=",
                "platform.files.shared-drive-id=", "platform.files.drive-oauth-client-id=integration-client",
                "platform.files.drive-oauth-client-secret=integration-secret",
                "platform.files.drive-oauth-refresh-token=integration-refresh-token",
                "platform.files.drive-folder-name=", "platform.files.drive-folder-id=",
                "platform.backup.folder-id=",
                "spring.rabbitmq.listener.simple.auto-startup=false",
                "spring.rabbitmq.listener.direct.auto-startup=false",
                "spring.main.allow-bean-definition-overriding=false"
        })
// Không in request/response: bản in đọc header trong khi luồng async tải file còn ghi, máy bận thì lỗi
// "End size ... is less than fixed size ...".
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DriveStorageIntegrationTest {
    static final String JWT_SECRET = UUID.randomUUID().toString() + UUID.randomUUID();
    static final String FILE_SECRET = UUID.randomUUID().toString() + UUID.randomUUID();
    static final String ENCRYPTION_KEY = Base64.getEncoder().encodeToString(
            UUID.randomUUID().toString().replace("-", "").getBytes(StandardCharsets.UTF_8));
    static final String UNATTACHED_KEY = "file:unattached";
    static final String APP_FOLDER = FilesProperties.DEFAULT_FOLDER_NAME;
    static final FakeDrive DRIVE = FakeDrive.start();

    static final SharedPostgres.Database DATABASE = SharedPostgres.createDatabase();
    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7.4-alpine"))
            .withExposedPorts(6379);
    @Container
    static final GenericContainer<?> BROKER = new GenericContainer<>(
            DockerImageName.parse("rabbitmq:4.1-management-alpine"))
            // Các fork test song song cùng bật RabbitMQ thì có thể mở cổng chậm hơn mức chờ mặc định 60 s.
            .withStartupTimeout(Duration.ofMinutes(3))
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

    /** Replaces only how the Drive client is built: same library and calls, local Drive endpoint, fixed token. */
    @Configuration(proxyBeanMethods = false)
    static class FakeDriveConfig {
        @Bean @Primary
        DriveClientFactory fakeDriveClientFactory(FilesProperties properties) {
            return new FakeDriveClientFactory(properties);
        }
    }

    static class FakeDriveClientFactory extends DriveClientFactory {
        FakeDriveClientFactory(FilesProperties properties) {
            super(properties);
        }

        @Override
        public Drive createDrive() {
            return new Drive.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance(), request -> {
                request.getHeaders().setAuthorization("Bearer integration-drive-access-token");
                request.setConnectTimeout(2_000);
                request.setReadTimeout(10_000);
            }).setRootUrl(DRIVE.baseUrl() + "/").setApplicationName("alap-platform").build();
        }
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;
    @Autowired MockMvc mvc;
    @Autowired SessionStore sessionStore;
    @Autowired JwtService jwtService;
    @Autowired RabbitTemplate rabbitTemplate;
    @Autowired StringRedisTemplate redisTemplate;
    @Autowired JobDispatcher jobDispatcher;
    @Autowired IJob job;
    @Autowired ArtifactService artifactService;
    @Autowired IStorage storage;
    @Autowired IBackupStore backupStore;
    @Autowired UnattachedFileScanner unattachedFileScanner;
    @Autowired DriveClientFactory driveClientFactory;
    @Autowired FilesProperties filesProperties;
    final Map<Role, Actor> actors = new EnumMap<>(Role.class);

    @BeforeEach
    void seedIndependentFixture() {
        String tables = jdbc.queryForObject("select string_agg(quote_ident(tablename), ',') from pg_tables "
                + "where schemaname='public' and tablename not in ('flyway_schema_history','system_settings')",
                String.class);
        jdbc.execute("truncate " + tables + " cascade");
        redisTemplate.delete(UNATTACHED_KEY);
        DRIVE.resetRequestsAndFaults();
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
        while (rabbitTemplate.receive("jobs.drive") != null) {
            // No worker consumes this disposable queue; discard the previous scenario's messages.
        }
    }

    Actor actor(Role role) {
        return actors.get(role);
    }

    Cookie loginCookie(Actor actor) {
        var session = sessionStore.createSession(actor.accountId(), null);
        return new Cookie("access_token", jwtService.issue(actor.accountId(), actor.role(), session.sid()));
    }

    static byte[] createPng(Color color) throws IOException {
        var image = new BufferedImage(320, 200, BufferedImage.TYPE_INT_RGB);
        var graphics = image.createGraphics();
        try {
            graphics.setColor(color);
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        } finally {
            graphics.dispose();
        }
        var output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }

    static String hash(String algorithm, byte[] content) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance(algorithm).digest(content));
    }

    String uploadPng(Actor owner, String fileName, byte[] png) {
        var ref = artifactService.store(owner, ArtifactPurpose.DOCUMENT_IMAGE,
                new FileUpload(fileName, png.length, new ByteArrayInputStream(png)));
        return artifactService.attach(ref, owner, ArtifactPurpose.DOCUMENT_IMAGE).fileId();
    }

    @Test
    void uploadIsStoredInPurposeFolderOnDriveAndDownloadStreamsTheSameBytes() throws Exception {
        var student = actor(Role.STUDENT);
        byte[] png = createPng(Color.ORANGE);
        String fileId = uploadPng(student, "diagram.png", png);

        var stored = DRIVE.item(fileId).orElseThrow();
        var purposeFolder = DRIVE.item(stored.parents().get(0)).orElseThrow();
        var appFolder = DRIVE.item(purposeFolder.parents().get(0)).orElseThrow();
        assertThat(stored.name()).isEqualTo("diagram.png");
        assertThat(stored.content()).isEqualTo(png);
        assertThat(stored.appProperties()).containsEntry("ownerAccountId", student.accountId().toString())
                .containsEntry("purpose", "DOCUMENT_IMAGE").containsEntry("mediaType", "image/png")
                .containsEntry("sha256", hash("SHA-256", png));
        assertThat(purposeFolder.name()).isEqualTo("DOCUMENT_IMAGE");
        assertThat(purposeFolder.mimeType()).isEqualTo(DriveFolders.FOLDER_MIME);
        assertThat(appFolder.name()).isEqualTo(APP_FOLDER);
        assertThat(appFolder.parents()).containsExactly("root");
        assertThat(DRIVE.requests()).anyMatch(request -> request.startsWith("POST /upload/drive/v3/files uploadType=resumable"));
        assertThat(DRIVE.authorizations()).containsOnly("Bearer integration-drive-access-token");

        uploadPng(student, "second.png", createPng(Color.BLUE));
        assertThat(DRIVE.items()).filteredOn(item -> item.name().equals(APP_FOLDER)).hasSize(1);
        assertThat(DRIVE.items()).filteredOn(item -> item.name().equals("DOCUMENT_IMAGE")).hasSize(1);

        String url = artifactService.issueDownloadToken(fileId, student.accountId()).url();
        var started = mvc.perform(get(url).cookie(loginCookie(student))).andReturn();
        mvc.perform(asyncDispatch(started))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/png"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(content().bytes(png));
        assertThat(DRIVE.requests()).anyMatch(request -> request.equals("GET /download/drive/v3/files/" + fileId + " alt=media"));
    }

    @Test
    void transientDriveErrorsAreRetriedButPermissionErrorFailsUploadWithoutStoringAnything() throws Exception {
        var student = actor(Role.STUDENT);
        DRIVE.failNext("POST /upload/drive/v3/files", 503, "backendError", 2);
        String fileId = uploadPng(student, "retried.png", createPng(Color.GREEN));
        assertThat(DRIVE.item(fileId)).isPresent();
        assertThat(DRIVE.requests()).filteredOn(request -> request.startsWith("POST /upload/drive/v3/files")).hasSize(3);
        assertThat(DRIVE.items()).filteredOn(item -> item.name().equals("retried.png")).hasSize(1);

        DRIVE.resetRequestsAndFaults();
        jdbc.update("delete from unattached_files");
        DRIVE.failNext("POST /upload/drive/v3/files", 403, "insufficientFilePermissions", 1);
        byte[] png = createPng(Color.RED);
        assertThatThrownBy(() -> artifactService.store(student, ArtifactPurpose.DOCUMENT_IMAGE,
                new FileUpload("denied.png", png.length, new ByteArrayInputStream(png))))
                .isInstanceOfSatisfying(ApiException.class, error -> assertThat(error.readCode()).isEqualTo("STORAGE_UNAVAILABLE"));
        assertThat(DRIVE.requests()).filteredOn(request -> request.startsWith("POST /upload/drive/v3/files")).hasSize(1);
        assertThat(DRIVE.items()).noneMatch(item -> item.name().equals("denied.png"));
        assertThat(jdbc.queryForObject("select count(*) from unattached_files", Long.class)).isZero();
    }

    @Test
    void abusiveDriveFileIsReportedAsUnavailableWithoutAudit() throws Exception {
        var student = actor(Role.STUDENT);
        String fileId = uploadPng(student, "flagged.png", createPng(Color.MAGENTA));
        DRIVE.failNext("GET /download/drive/v3/files/" + fileId + " alt=media", 403, "cannotDownloadAbusiveFile", 1);
        String url = artifactService.issueDownloadToken(fileId, student.accountId()).url();

        mvc.perform(get(url).cookie(loginCookie(student)))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value("FILE_UNAVAILABLE"));
        assertThat(jdbc.queryForObject("select count(*) from audit_logs where action='FILE_DOWNLOAD' and actor_id=?",
                Long.class, student.accountId())).isZero();
        assertThat(DRIVE.item(fileId)).isPresent();
    }

    @Test
    void unattachedDriveFileIsDeletedByScannerAndFailedDeleteIsRetriedOnNextScan() throws Exception {
        var student = actor(Role.STUDENT);
        byte[] png = createPng(Color.CYAN);
        artifactService.store(student, ArtifactPurpose.DOCUMENT_IMAGE,
                new FileUpload("abandoned.png", png.length, new ByteArrayInputStream(png)));
        String fileId = jdbc.queryForObject("select file_id from unattached_files", String.class);
        assertThat(DRIVE.item(fileId)).isPresent();

        DRIVE.failNext("DELETE /drive/v3/files/" + fileId, 403, "insufficientFilePermissions", 1);
        unattachedFileScanner.scan(Instant.now().plus(Duration.ofDays(2)));
        assertThat(DRIVE.item(fileId)).isPresent();
        assertThat(jdbc.queryForObject("select count(*) from unattached_files", Long.class)).isEqualTo(1);

        unattachedFileScanner.scan(Instant.now().plus(Duration.ofDays(2)));
        assertThat(DRIVE.item(fileId)).isEmpty();
        assertThat(jdbc.queryForObject("select count(*) from unattached_files", Long.class)).isZero();
        assertThat(DRIVE.requests()).filteredOn(request -> request.equals("DELETE /drive/v3/files/" + fileId)).hasSize(2);
    }

    @Test
    void driveCleanupJobFromQueueDeletesFileAndRepeatedJobIsAcknowledged() throws Exception {
        String fileId = uploadPng(actor(Role.TEACHER), "orphan.png", createPng(Color.PINK));
        var listener = new JobListener(jobDispatcher, json);
        for (int round = 0; round < 2; round++) {
            job.enqueue(JobTypes.DRIVE_CLEANUP, Map.of("storage", "FILES", "ref", fileId), "drive-cleanup:" + UUID.randomUUID());
            assertThat(deliverQueuedMessages("jobs.drive", listener::receiveDrive)).isEqualTo(1);
            assertThat(DRIVE.item(fileId)).isEmpty();
        }
        assertThat(DRIVE.requests()).filteredOn(request -> request.equals("DELETE /drive/v3/files/" + fileId)).hasSize(2);
        assertThat(rabbitTemplate.receive("jobs.drive", 300)).isNull();
        assertThat(rabbitTemplate.receive("jobs.retry.30s", 300)).isNull();
    }

    @Test
    void backupArchiveIsStoredInRunFolderUnderBackupsAndDeletedWithItsFolder() throws Exception {
        UUID runId = UUID.randomUUID();
        byte[] archive = new byte[4096];
        new Random(42).nextBytes(archive);
        Path file = Files.createTempFile(FILES, "archive-", ".alapbk");
        Files.write(file, archive);

        var stored = backupStore.putArchive(runId, file);
        var item = DRIVE.item(stored.ref()).orElseThrow();
        var runFolder = DRIVE.item(item.parents().get(0)).orElseThrow();
        var backupsFolder = DRIVE.item(runFolder.parents().get(0)).orElseThrow();
        var appFolder = DRIVE.item(backupsFolder.parents().get(0)).orElseThrow();
        assertThat(stored.byteSize()).isEqualTo(archive.length);
        assertThat(stored.md5()).isEqualTo(hash("MD5", archive));
        assertThat(item.name()).isEqualTo("archive.alapbk");
        assertThat(item.content()).isEqualTo(archive);
        assertThat(runFolder.name()).isEqualTo(runId.toString());
        assertThat(backupsFolder.name()).isEqualTo("backups");
        assertThat(appFolder.name()).isEqualTo(APP_FOLDER);

        backupStore.deleteArchive(runId.toString());
        assertThat(DRIVE.item(runFolder.id())).isEmpty();
        assertThat(DRIVE.item(stored.ref())).isEmpty();
        assertThatCode(() -> backupStore.deleteArchive(runId.toString())).doesNotThrowAnyException();
        assertThat(DRIVE.item(backupsFolder.id())).isPresent();
    }

    @Test
    void configuredRootFolderIsUsedOrFallsBackByNameAndHealthReportsDriveState() {
        var namedRoot = new DriveFolders(driveClientFactory, withFolder("Lab's \\ files", null)).resolveRoot();
        assertThat(DRIVE.item(namedRoot).orElseThrow().name()).isEqualTo("Lab's \\ files");
        assertThat(new DriveFolders(driveClientFactory, withFolder("Lab's \\ files", null)).resolveRoot())
                .isEqualTo(namedRoot);
        assertThat(DRIVE.items()).filteredOn(item -> item.name().equals("Lab's \\ files")).hasSize(1);

        assertThat(new DriveFolders(driveClientFactory, withFolder("Ignored name", namedRoot)).resolveRoot())
                .isEqualTo(namedRoot);
        assertThat(new DriveFolders(driveClientFactory, withFolder("Lab's \\ files", "deleted-folder-id")).resolveRoot())
                .isEqualTo(namedRoot);
        assertThat(DRIVE.items()).noneMatch(item -> item.name().equals("Ignored name"));

        var properties = withFolder("Lab's \\ files", namedRoot);
        var folders = new DriveFolders(driveClientFactory, properties);
        var healthy = new DriveHealthIndicator(properties, driveClientFactory, folders, Clock.systemUTC());
        try {
            Awaitility.await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                    assertThat(healthy.health().getDetails()).containsEntry("drive", "UP"));
        } finally {
            healthy.closeProbe();
        }
        DRIVE.failNext("GET /drive/v3/files/" + namedRoot, 403, "insufficientFilePermissions", 2);
        var degraded = new DriveHealthIndicator(properties, driveClientFactory, folders, Clock.systemUTC());
        try {
            Awaitility.await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                    assertThat(degraded.health().getDetails()).containsEntry("drive", "DEGRADED"));
        } finally {
            degraded.closeProbe();
        }
    }

    FilesProperties withFolder(String folderName, String folderId) {
        var p = filesProperties;
        return new FilesProperties(p.filerefSecret(), p.localStorageDir(), p.uploadTmpDir(),
                p.driveServiceAccountKey(), p.sharedDriveId(), p.driveOauthClientId(), p.driveOauthClientSecret(),
                p.driveOauthRefreshToken(), folderName, folderId);
    }

    @FunctionalInterface
    interface QueueConsumer {
        void accept(Message message, com.rabbitmq.client.Channel channel) throws IOException;
    }

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
                    delivery.getProps().getHeaders().forEach(properties::setHeader);
                }
                consumer.accept(new Message(delivery.getBody(), properties), channel);
                delivered++;
            }
            return delivered;
        });
    }

    /** Minimal Drive v3 REST endpoint for the calls the platform makes; state lives in memory. */
    static final class FakeDrive {
        record Item(String id, String name, String mimeType, List<String> parents, Map<String, String> appProperties,
                    byte[] content) {
        }

        record Fault(String target, int status, String reason, AtomicInteger remaining) {
        }

        private static final Pattern PARENT_AND_NAME = Pattern.compile(
                "'([^']*)' in parents and name = '((?:\\\\.|[^'\\\\])*)'");
        private final ObjectMapper mapper = new ObjectMapper();
        private final HttpServer server;
        private final Map<String, Item> items = new ConcurrentHashMap<>();
        private final Map<String, ObjectNode> uploads = new ConcurrentHashMap<>();
        private final List<String> requests = new CopyOnWriteArrayList<>();
        private final List<String> authorizations = new CopyOnWriteArrayList<>();
        private final List<Fault> faults = new CopyOnWriteArrayList<>();
        private final AtomicInteger sequence = new AtomicInteger();

        private FakeDrive(HttpServer server) {
            this.server = server;
        }

        static FakeDrive start() {
            try {
                var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
                var drive = new FakeDrive(server);
                server.createContext("/", drive::handleExchange);
                server.setExecutor(Executors.newFixedThreadPool(4));
                server.start();
                return drive;
            } catch (IOException exception) {
                throw new java.io.UncheckedIOException(exception);
            }
        }

        String baseUrl() {
            return "http://127.0.0.1:" + server.getAddress().getPort();
        }

        void resetRequestsAndFaults() {
            requests.clear();
            authorizations.clear();
            faults.clear();
        }

        /** The next {@code times} requests whose "METHOD path [alt=media]" starts with {@code target} fail. */
        void failNext(String target, int status, String reason, int times) {
            faults.add(new Fault(target, status, reason, new AtomicInteger(times)));
        }

        Optional<Item> item(String id) {
            return Optional.ofNullable(items.get(id));
        }

        Collection<Item> items() {
            return List.copyOf(items.values());
        }

        List<String> requests() {
            return List.copyOf(requests);
        }

        List<String> authorizations() {
            return List.copyOf(authorizations);
        }

        private void handleExchange(HttpExchange exchange) throws IOException {
            try (exchange) {
                String method = exchange.getRequestMethod();
                String path = exchange.getRequestURI().getPath();
                Map<String, String> query = parseQuery(exchange.getRequestURI().getRawQuery());
                byte[] body = readBody(exchange);
                String marker = query.containsKey("uploadType") ? " uploadType=" + query.get("uploadType")
                        : "media".equals(query.get("alt")) ? " alt=media" : "";
                String request = method + " " + path + marker;
                requests.add(request);
                authorizations.add(String.valueOf(exchange.getRequestHeaders().getFirst("Authorization")));
                for (Fault fault : faults) {
                    if (request.startsWith(fault.target()) && fault.remaining().getAndUpdate(n -> Math.max(0, n - 1)) > 0) {
                        sendError(exchange, fault.status(), fault.reason());
                        return;
                    }
                }
                route(exchange, method, path, query, body);
            } catch (RuntimeException exception) {
                sendError(exchange, 500, "internalError");
            }
        }

        private void route(HttpExchange exchange, String method, String path, Map<String, String> query, byte[] body)
                throws IOException {
            if (path.equals("/upload/drive/v3/files") && method.equals("POST") && "resumable".equals(query.get("uploadType"))) {
                String uploadId = UUID.randomUUID().toString();
                uploads.put(uploadId, (ObjectNode) mapper.readTree(body));
                exchange.getResponseHeaders().add("Location",
                        baseUrl() + "/upload/drive/v3/files?uploadType=resumable&upload_id=" + uploadId);
                exchange.sendResponseHeaders(200, -1);
            } else if (path.equals("/upload/drive/v3/files") && method.equals("PUT") && query.containsKey("upload_id")) {
                ObjectNode metadata = uploads.remove(query.get("upload_id"));
                sendJson(exchange, 200, toJson(create(metadata, body)));
            } else if (path.equals("/drive/v3/files") && method.equals("POST")) {
                sendJson(exchange, 200, toJson(create((ObjectNode) mapper.readTree(body), new byte[0])));
            } else if (path.equals("/drive/v3/files") && method.equals("GET")) {
                sendJson(exchange, 200, list(query.getOrDefault("q", "")));
            } else if (path.startsWith("/drive/v3/files/") || path.startsWith("/download/drive/v3/files/")) {
                // Media downloads use the download/ prefix of the Drive v3 client library.
                String id = path.substring(path.indexOf("/files/") + "/files/".length());
                Item item = items.get(id);
                if (item == null) {
                    sendError(exchange, 404, "notFound");
                } else if (method.equals("DELETE")) {
                    delete(id);
                    exchange.sendResponseHeaders(204, -1);
                } else if ("media".equals(query.get("alt"))) {
                    exchange.getResponseHeaders().add("Content-Type", "application/octet-stream");
                    exchange.sendResponseHeaders(200, item.content().length);
                    exchange.getResponseBody().write(item.content());
                } else {
                    sendJson(exchange, 200, toJson(item));
                }
            } else if (path.startsWith("/drive/v3/drives/")) {
                sendJson(exchange, 200, mapper.createObjectNode().put("id", path.substring("/drive/v3/drives/".length())));
            } else {
                sendError(exchange, 404, "notFound");
            }
        }

        private Item create(ObjectNode metadata, byte[] content) {
            List<String> parents = new ArrayList<>();
            if (metadata.has("parents")) metadata.get("parents").forEach(parent -> parents.add(parent.asText()));
            Map<String, String> properties = new LinkedHashMap<>();
            if (metadata.has("appProperties")) metadata.get("appProperties").fields()
                    .forEachRemaining(entry -> properties.put(entry.getKey(), entry.getValue().asText()));
            String id = "drive-" + sequence.incrementAndGet();
            Item item = new Item(id, metadata.path("name").asText(), metadata.path("mimeType").asText(null),
                    List.copyOf(parents), Map.copyOf(properties), content);
            items.put(id, item);
            return item;
        }

        private void delete(String id) {
            items.remove(id);
            items.values().stream().filter(child -> child.parents().contains(id)).map(Item::id).toList()
                    .forEach(this::delete);
        }

        private ObjectNode list(String q) {
            var matcher = PARENT_AND_NAME.matcher(q);
            ObjectNode result = mapper.createObjectNode();
            ArrayNode files = result.putArray("files");
            if (matcher.find()) {
                String parent = matcher.group(1);
                String name = matcher.group(2).replace("\\'", "'").replace("\\\\", "\\");
                items.values().stream()
                        .filter(item -> item.parents().contains(parent) && item.name().equals(name)
                                && DriveFolders.FOLDER_MIME.equals(item.mimeType()))
                        .sorted(Comparator.comparing(Item::id))
                        .forEach(item -> files.addObject().put("id", item.id()));
            }
            return result;
        }

        private ObjectNode toJson(Item item) {
            ObjectNode node = mapper.createObjectNode().put("id", item.id()).put("name", item.name())
                    .put("size", String.valueOf(item.content().length));
            if (item.mimeType() != null) node.put("mimeType", item.mimeType());
            try {
                node.put("md5Checksum", hash("MD5", item.content()));
            } catch (Exception exception) {
                throw new IllegalStateException(exception);
            }
            ArrayNode parents = node.putArray("parents");
            item.parents().forEach(parents::add);
            ObjectNode properties = node.putObject("appProperties");
            item.appProperties().forEach(properties::put);
            return node;
        }

        private void sendJson(HttpExchange exchange, int status, ObjectNode body) throws IOException {
            byte[] bytes = mapper.writeValueAsBytes(body);
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
        }

        private void sendError(HttpExchange exchange, int status, String reason) throws IOException {
            ObjectNode body = mapper.createObjectNode();
            ObjectNode error = body.putObject("error").put("code", status).put("message", reason);
            error.putArray("errors").addObject().put("domain", "global").put("reason", reason).put("message", reason);
            sendJson(exchange, status, body);
        }

        private static byte[] readBody(HttpExchange exchange) throws IOException {
            byte[] raw = exchange.getRequestBody().readAllBytes();
            String encoding = exchange.getRequestHeaders().getFirst("Content-Encoding");
            if (encoding != null && encoding.contains("gzip") && raw.length > 0) {
                try (var gzip = new GZIPInputStream(new ByteArrayInputStream(raw))) {
                    return gzip.readAllBytes();
                }
            }
            return raw;
        }

        private static Map<String, String> parseQuery(String rawQuery) {
            Map<String, String> query = new HashMap<>();
            if (rawQuery == null) return query;
            for (String pair : rawQuery.split("&")) {
                int split = pair.indexOf('=');
                String key = URLDecoder.decode(split < 0 ? pair : pair.substring(0, split), StandardCharsets.UTF_8);
                String value = split < 0 ? "" : URLDecoder.decode(pair.substring(split + 1), StandardCharsets.UTF_8);
                query.put(key, value);
            }
            return query;
        }
    }
}
