package edu.aiplatform.groups.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.zaxxer.hikari.HikariDataSource;
import edu.aiplatform.groups.model.GroupFormationSettings;
import edu.aiplatform.groups.model.GroupProposal;
import edu.aiplatform.groups.model.ProposalMember;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * SQL của {@link GroupProposalRepository} chạy bằng role {@code app} trên PostgreSQL 16 dùng riêng cho test với toàn bộ
 * migration Flyway thật; kiểm chỉ mục một-đề-xuất-mỗi-sinh-viên, mời lại và CAS {@code version} của cài đặt.
 */
@Testcontainers
class GroupProposalRepositoryIntegrationTest {

    private static final String APP_PASSWORD = UUID.randomUUID().toString();
    private static final String MIGRATOR_PASSWORD = UUID.randomUUID().toString();
    private static final Instant NOW = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withEnv("POSTGRES_APP_PASSWORD", APP_PASSWORD)
            .withEnv("POSTGRES_MIGRATOR_PASSWORD", MIGRATOR_PASSWORD)
            .withCopyToContainer(rolesScript(), "/docker-entrypoint-initdb.d/01-roles.sh");

    private static HikariDataSource appDataSource;
    private static HikariDataSource ownerDataSource;
    private static JdbcClient owner;
    private static GroupProposalRepository proposals;
    private UUID classId;
    private UUID studentA;
    private UUID studentB;
    private UUID studentC;
    private UUID teacher;

    @BeforeAll
    static void setUp() {
        Flyway.configure().dataSource(POSTGRES.getJdbcUrl(), "migrator", MIGRATOR_PASSWORD)
                .locations("classpath:db/migration").load().migrate();
        appDataSource = dataSource("app", APP_PASSWORD);
        ownerDataSource = dataSource(POSTGRES.getUsername(), POSTGRES.getPassword());
        owner = JdbcClient.create(ownerDataSource);
        proposals = new GroupProposalRepository(JdbcClient.create(appDataSource));
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
        UUID subject = UUID.randomUUID();
        teacher = UUID.randomUUID();
        classId = UUID.randomUUID();
        studentA = UUID.randomUUID();
        studentB = UUID.randomUUID();
        studentC = UUID.randomUUID();
        for (UUID student : List.of(studentA, studentB, studentC)) {
            owner.sql("insert into accounts (id, school_email, role, status, display_name, password_hash) values "
                    + "(?, ?, 'STUDENT', 'ACTIVE', 'SV', 'x')").params(student, student + "@fpt.edu.vn").update();
        }
        owner.sql("insert into accounts (id, school_email, role, status, display_name, password_hash) values "
                + "(?, ?, 'TEACHER', 'ACTIVE', 'GV', 'x')").params(teacher, teacher + "@fpt.edu.vn").update();
        owner.sql("insert into subjects (id, code, name, created_at, updated_at) values (?, ?, 'Môn', now(), now())")
                .params(subject, "SWP391").update();
        owner.sql("insert into course_classes (id, subject_id, code, name, status, teacher_id, created_at, updated_at) "
                + "values (?, ?, 'SE1', 'SE1', 'OPEN', ?, now(), now())").params(classId, subject, teacher).update();
    }

    @Test
    void settingsAreCreatedOnceThenUpdatedByVersion() {
        GroupFormationSettings first = new GroupFormationSettings(classId, true, 2, 4, null, null, NOW, 1);

        assertThat(proposals.saveSettings(first, 0)).isTrue();
        assertThat(proposals.saveSettings(first, 0)).as("dòng đã có, version cũ").isFalse();
        GroupFormationSettings second = new GroupFormationSettings(classId, false, 3, 5, NOW.plusSeconds(3600), null,
                NOW, 2);
        assertThat(proposals.saveSettings(second, 1)).isTrue();

        assertThat(proposals.findSettings(classId)).get().satisfies(s -> {
            assertThat(s.enabled()).isFalse();
            assertThat(s.minSize()).isEqualTo(3);
            assertThat(s.deadline()).isEqualTo(NOW.plusSeconds(3600));
            assertThat(s.version()).isEqualTo(2);
        });
    }

    @Test
    void studentCanBeActiveInOnlyOneProposal() {
        GroupProposal first = proposal("Nhóm A", studentA);
        GroupProposal second = proposal("Nhóm B", studentB);
        proposals.insertProposal(first);
        proposals.insertProposal(second);
        proposals.insertActiveMember(first.id(), classId, studentA, NOW);
        proposals.insertActiveMember(second.id(), classId, studentB, NOW);
        proposals.upsertInvitation(first.id(), classId, studentC, studentA, NOW);
        proposals.upsertInvitation(second.id(), classId, studentC, studentB, NOW);
        proposals.updateMember(first.id(), studentC, ProposalMember.Status.ACCEPTED, true, NOW);

        assertThatThrownBy(() -> proposals.updateMember(second.id(), studentC, ProposalMember.Status.ACCEPTED, true, NOW))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(proposals.findActiveProposalOf(classId, studentC)).map(GroupProposal::id).contains(first.id());
        assertThat(proposals.mapActiveMemberships(classId)).containsEntry(studentC, first.id());
    }

    @Test
    void reinvitingReusesTheRowAndRevokingSkipsTheKeptProposal() {
        GroupProposal first = proposal("Nhóm A", studentA);
        GroupProposal second = proposal("Nhóm B", studentB);
        proposals.insertProposal(first);
        proposals.insertProposal(second);
        proposals.upsertInvitation(first.id(), classId, studentC, studentA, NOW);
        proposals.updateMember(first.id(), studentC, ProposalMember.Status.DECLINED, false, NOW);
        proposals.upsertInvitation(first.id(), classId, studentC, studentA, NOW.plusSeconds(5));
        proposals.upsertInvitation(second.id(), classId, studentC, studentB, NOW);

        assertThat(proposals.listMembers(first.id())).singleElement()
                .satisfies(m -> assertThat(m.status()).isEqualTo(ProposalMember.Status.INVITED));
        assertThat(proposals.listInvitingProposals(classId, studentC)).hasSize(2);

        assertThat(proposals.revokeInvitationsOf(classId, List.of(studentC), first.id(), NOW)).isEqualTo(1);
        assertThat(proposals.listInvitingProposals(classId, studentC)).extracting(GroupProposal::id)
                .containsExactly(first.id());
        assertThat(proposals.revokeInvitationsOf(classId, List.of(studentC), null, NOW)).isEqualTo(1);
        assertThat(proposals.expireInvitations(classId, NOW)).isZero();
    }

    @Test
    void endedProposalFreesItsMembersAndKeepsHistory() {
        GroupProposal draft = proposal("Nhóm A", studentA);
        proposals.insertProposal(draft);
        proposals.insertActiveMember(draft.id(), classId, studentA, NOW);
        GroupProposal rejected = draft.submitted(NOW).rejected(teacher, "Thiếu người", NOW);
        proposals.lockProposal(draft.id()).orElseThrow();
        proposals.updateProposal(rejected);
        proposals.deactivateMembers(draft.id());

        assertThat(proposals.listActiveProposals(classId)).isEmpty();
        assertThat(proposals.findActiveProposalOf(classId, studentA)).isEmpty();
        assertThat(proposals.findLatestRejectedOf(classId, studentA)).get()
                .satisfies(p -> assertThat(p.decisionReason()).isEqualTo("Thiếu người"));
        GroupProposal again = proposal("Nhóm A", studentA);
        proposals.insertProposal(again);
        proposals.insertActiveMember(again.id(), classId, studentA, NOW);
        assertThat(proposals.listActiveMemberships(classId, List.of(studentA))).singleElement()
                .satisfies(m -> assertThat(m.proposalId()).isEqualTo(again.id()));
    }

    private GroupProposal proposal(String name, UUID leader) {
        return GroupProposal.draft(UUID.randomUUID(), classId, name, leader, NOW);
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
}
