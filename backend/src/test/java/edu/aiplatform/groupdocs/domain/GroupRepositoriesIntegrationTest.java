package edu.aiplatform.groupdocs.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.zaxxer.hikari.HikariDataSource;
import edu.aiplatform.assignments.contract.BlockOrigin;
import edu.aiplatform.assignments.contract.BlockType;
import edu.aiplatform.assignments.contract.DocumentBlock;
import edu.aiplatform.assignments.contract.DocumentModel;
import edu.aiplatform.groupdocs.domain.GroupDocument.SectionAuthor;
import edu.aiplatform.groupdocs.domain.GroupDocument.SubmitMode;
import edu.aiplatform.groups.domain.ClassGroup;
import edu.aiplatform.groups.domain.GroupRepository;
import edu.aiplatform.groups.domain.LeaderChangeRequest;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.flywaydb.core.Flyway;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * SQL của hai repository U12 chạy bằng role {@code app} trên PostgreSQL 16 dùng riêng cho test, với toàn bộ migration
 * Flyway thật. Dữ liệu sinh tại chỗ và container được hủy sau test; không kết nối database ngoài.
 */
@Testcontainers
class GroupRepositoriesIntegrationTest {

    private static final String APP_PASSWORD = UUID.randomUUID().toString();
    private static final String MIGRATOR_PASSWORD = UUID.randomUUID().toString();

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withEnv("POSTGRES_APP_PASSWORD", APP_PASSWORD)
            .withEnv("POSTGRES_MIGRATOR_PASSWORD", MIGRATOR_PASSWORD)
            .withCopyToContainer(rolesScript(), "/docker-entrypoint-initdb.d/01-roles.sh");

    private static HikariDataSource appDataSource;
    private static HikariDataSource ownerDataSource;
    private static JdbcClient owner;
    private static GroupRepository groups;
    private static GroupDocumentRepository documents;
    private static final Instant NOW = Instant.now().truncatedTo(ChronoUnit.MILLIS);
    private static UUID classId;
    private static UUID studentA;
    private static UUID studentB;
    private static UUID subject;
    private static UUID teacher;

    @BeforeAll
    static void setUp() {
        Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), "migrator", MIGRATOR_PASSWORD)
                .locations("classpath:db/migration").load().migrate();
        appDataSource = dataSource("app", APP_PASSWORD);
        ownerDataSource = dataSource(POSTGRES.getUsername(), POSTGRES.getPassword());
        owner = JdbcClient.create(ownerDataSource);
        JdbcClient jdbc = JdbcClient.create(appDataSource);
        var json = JsonMapper.builder().findAndAddModules().build();
        groups = new GroupRepository(jdbc);
        documents = new GroupDocumentRepository(jdbc, json);
    }

    @AfterAll
    static void close() {
        if (appDataSource != null) {
            appDataSource.close();
        }
        if (ownerDataSource != null) {
            ownerDataSource.close();
        }
    }

    @BeforeEach
    void seed() {
        owner.sql("truncate accounts, subjects cascade").update();
        subject = UUID.randomUUID();
        classId = UUID.randomUUID();
        studentA = UUID.randomUUID();
        studentB = UUID.randomUUID();
        teacher = UUID.randomUUID();
        for (UUID student : List.of(studentA, studentB)) {
            owner.sql("insert into accounts (id, school_email, role, status, display_name, password_hash) values "
                    + "(?, ?, 'STUDENT', 'ACTIVE', 'SV', 'x')").params(student, student + "@fpt.edu.vn").update();
        }
        owner.sql("insert into subjects (id, code, name, created_at, updated_at) values (?, ?, 'Môn', now(), now())")
                .params(subject, "SWP391").update();
        owner.sql("insert into accounts (id, school_email, role, status, display_name, password_hash) values "
                + "(?, ?, 'TEACHER', 'ACTIVE', 'GV', 'x')").params(teacher, teacher + "@fpt.edu.vn").update();
        owner.sql("insert into course_classes (id, subject_id, code, name, status, teacher_id, created_at, updated_at) values "
                + "(?, ?, 'SE1', 'SE1', 'OPEN', ?, now(), now())").params(classId, subject, teacher).update();
    }

    private static Transferable rolesScript() {
        try {
            String script = Files.readString(Path.of("..", "infra", "postgres", "init", "01-roles.sh"));
            return Transferable.of(script.replace("\r", ""), 0755);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static HikariDataSource dataSource(String user, String password) {
        HikariDataSource source = new HikariDataSource();
        source.setJdbcUrl(POSTGRES.getJdbcUrl());
        source.setUsername(user);
        source.setPassword(password);
        source.setMaximumPoolSize(4);
        return source;
    }

    @Test
    void groupsMembersVersionAndLeaderRequests() {
        assertThat(groups.setVersion(classId)).isZero();
        assertThat(groups.bumpSetVersion(classId, 0, NOW)).isTrue();
        assertThat(groups.bumpSetVersion(classId, 0, NOW)).as("version cũ bị từ chối").isFalse();
        assertThat(groups.setVersion(classId)).isEqualTo(1);

        UUID groupId = UUID.randomUUID();
        groups.insertGroup(new ClassGroup(groupId, classId, "Nhóm 1", studentA, null, NOW, NOW, List.of()));
        groups.addMember(groupId, classId, studentA, NOW);
        groups.addMember(groupId, classId, studentB, NOW);
        assertThat(groups.listGroups(classId)).singleElement()
                .satisfies(g -> assertThat(g.memberIds()).containsExactlyInAnyOrder(studentA, studentB));
        assertThat(groups.findGroupOf(classId, studentB)).map(ClassGroup::id).contains(groupId);

        LeaderChangeRequest request = new LeaderChangeRequest(UUID.randomUUID(), groupId, studentB, studentB,
                "Trưởng nhóm bận việc riêng", LeaderChangeRequest.Status.PENDING, null, null, null, null, NOW);
        groups.insertRequest(request);
        assertThat(groups.findPending(groupId)).isPresent();
        assertThat(groups.listPendingOfClass(classId)).hasSize(1);
        groups.updateRequest(request.decide(LeaderChangeRequest.Status.APPROVED, studentB, studentA, null, NOW));
        assertThat(groups.findPending(groupId)).isEmpty();
        assertThat(groups.latestOf(groupId, studentB, 2)).singleElement()
                .satisfies(r -> assertThat(r.status()).isEqualTo(LeaderChangeRequest.Status.APPROVED));

        groups.removeMembers(classId, List.of(studentB));
        assertThat(groups.findGroupOf(classId, studentB)).isEmpty();
    }

    @Test
    void documentsSectionsLocksAndSubmission() {
        UUID groupId = UUID.randomUUID();
        groups.insertGroup(new ClassGroup(groupId, classId, "Nhóm D " + groupId, studentA, null, NOW, NOW, List.of()));
        UUID assignment = UUID.randomUUID();
        owner.sql("insert into assignments (id, scope, class_id, subject_id, origin, submission_mode, grading_mode, title, "
                + "skeleton, parts, status, opens_at, closes_at, max_attempts, created_by, created_at, updated_at) values "
                + "(?, 'CLASS', ?, ?, 'MANUAL', 'GROUP', 'GRADED', 'Bài nhóm', '{\"blocks\":[]}'::jsonb, '[]'::jsonb, "
                + "'OPEN', now() - interval '1 hour', now() + interval '1 hour', 1, ?, now(), now())")
                .params(assignment, classId, subject, teacher).update();
        GroupDocument doc = GroupDocument.create(UUID.randomUUID(), groupId, assignment, classId,
                new DocumentModel(List.of()), false, NOW);
        assertThat(documents.insertIfAbsent(doc)).isTrue();
        assertThat(documents.insertIfAbsent(GroupDocument.create(UUID.randomUUID(), groupId, assignment, classId,
                new DocumentModel(List.of()), false, NOW))).as("không tạo trùng").isFalse();
        GroupSection section = new GroupSection(UUID.randomUUID(), doc.id(), "p1", null, List.of(UUID.randomUUID()), 1,
                "1. Tổng quan", GroupSection.Status.OPEN, null, null, null, null, null, null, null, 0);
        documents.insertSection(section);
        assertThat(documents.groupHasContent(groupId)).isFalse();

        GroupSection locked = documents.lockSection(doc.id(), section.id()).orElseThrow();
        assertThat(documents.updateSection(locked.claimedBy(studentA, studentA, NOW))).isTrue();
        assertThat(documents.updateSection(locked.claimedBy(studentB, studentB, NOW))).as("version cũ").isFalse();
        List<DocumentBlock> blocks = List.of(new DocumentBlock(UUID.randomUUID(), BlockOrigin.STUDENT, BlockType.PARAGRAPH,
                JsonNodeFactory.instance.objectNode().put("text", "Xin chào")));
        GroupSection claimed = documents.lockSection(doc.id(), section.id()).orElseThrow();
        assertThat(documents.updateSection(claimed.withDraft(blocks, NOW))).isTrue();
        assertThat(documents.lockSectionsHeldBy(groupId, studentA)).hasSize(1);
        GroupSection drafted = documents.lockSection(doc.id(), section.id()).orElseThrow();
        assertThat(drafted.draftBlocks()).isEqualTo(blocks);
        assertThat(documents.updateSection(drafted.done(NOW))).isTrue();
        documents.insertRevision(new SectionRevision(UUID.randomUUID(), section.id(), studentA, blocks, NOW));
        assertThat(documents.groupHasContent(groupId)).isTrue();

        GroupDocument current = documents.lockDocument(doc.id()).orElseThrow();
        GroupDocument submitted = current.withSubmission(new DocumentModel(blocks),
                List.of(new SectionAuthor("p1", "1. Tổng quan", studentA, true)), studentA, SubmitMode.MANUAL, NOW, false,
                "abc", List.of(), GroupDocument.Status.IN_PROGRESS);
        assertThat(documents.update(submitted)).isTrue();
        GroupDocument read = documents.findById(doc.id()).orElseThrow();
        assertThat(read.submittedDocument().blocks()).hasSize(1);
        assertThat(read.sectionAuthors()).singleElement().satisfies(a -> assertThat(a.accountId()).isEqualTo(studentA));
        assertThat(read.receiptHash()).isEqualTo("abc");

        assertThat(documents.markRetired(assignment)).isEqualTo(1);
        assertThat(documents.listDueForAutoSubmit(List.of())).contains(doc.id());
        assertThat(documents.listDueForAutoSubmit(List.of(assignment))).contains(doc.id());
        assertThat(documents.listInProgressAssignments()).contains(assignment);
        assertThat(documents.listByGroups(List.of(groupId), List.of(assignment))).hasSize(1);
    }
}
