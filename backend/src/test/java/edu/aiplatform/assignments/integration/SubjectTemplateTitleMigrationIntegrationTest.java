package edu.aiplatform.assignments.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.assignments.contract.AssignmentScope;
import edu.aiplatform.assignments.contract.AssignmentStatus;
import edu.aiplatform.assignments.contract.DocumentModel;
import edu.aiplatform.assignments.contract.GradingMode;
import edu.aiplatform.assignments.contract.SubmissionMode;
import edu.aiplatform.assignments.model.Assignment;
import edu.aiplatform.assignments.model.AssignmentOrigin;
import edu.aiplatform.assignments.repository.AssignmentRepository;
import edu.aiplatform.shared.error.ApiException;
import edu.aiplatform.shared.testsupport.SharedPostgres;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Migration BR-U07-102 trên PostgreSQL thật: đổi tên đề mẫu đã trùng rồi unique index chặn tên trùng mới. */
@Testcontainers(disabledWithoutDocker = true)
class SubjectTemplateTitleMigrationIntegrationTest {

    static final SharedPostgres.Database DATABASE = SharedPostgres.createDatabase();

    private static final Instant T0 = Instant.parse("2026-10-01T00:00:00Z");

    private JdbcTemplate jdbc;
    private final UUID actor = UUID.randomUUID();
    private final UUID subject = UUID.randomUUID();
    private final UUID otherSubject = UUID.randomUUID();
    private final UUID courseClass = UUID.randomUUID();

    @Test
    void renamesExistingDuplicatesThenBlocksNewOnes() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(DATABASE.getJdbcUrl(),
                DATABASE.getUsername(), DATABASE.getPassword());
        jdbc = new JdbcTemplate(dataSource);
        for (String table : new String[] {"accounts", "subjects", "course_classes"}) {
            jdbc.execute("create table " + table + " (id uuid primary key)");
        }
        jdbc.update("insert into accounts(id) values (?)", actor);
        jdbc.update("insert into subjects(id) values (?), (?)", subject, otherSubject);
        jdbc.update("insert into course_classes(id) values (?)", courseClass);
        migrate(dataSource, "20261111.1300");

        UUID kept = insert(subject, "SUBJECT", "MANUAL", null, "DRAFT", "Đề cuối kỳ", 1);
        UUID numbered = insert(subject, "SUBJECT", "MANUAL", null, "DRAFT", "Đề cuối kỳ (2)", 2);
        UUID duplicate = insert(subject, "SUBJECT", "CLONE", null, "REVIEWED", "  đề   cuối KỲ ", 3);
        UUID retired = insert(subject, "SUBJECT", "MANUAL", null, "RETIRED", "Đề cuối kỳ", 4);
        UUID version = insert(subject, "SUBJECT", "NEW_VERSION", kept, "DRAFT", "Đề cuối kỳ", 5);
        UUID ofClass = insert(subject, "CLASS", "MANUAL", null, "DRAFT", "Đề cuối kỳ", 6);
        UUID elsewhere = insert(otherSubject, "SUBJECT", "MANUAL", null, "DRAFT", "Đề cuối kỳ", 7);
        String longTitle = "A".repeat(200);
        insert(subject, "SUBJECT", "MANUAL", null, "DRAFT", longTitle, 8);
        UUID longDuplicate = insert(subject, "SUBJECT", "MANUAL", null, "DRAFT", longTitle, 9);

        migrate(dataSource, null);

        assertThat(titleOf(kept)).isEqualTo("Đề cuối kỳ");
        assertThat(titleOf(numbered)).isEqualTo("Đề cuối kỳ (2)");
        assertThat(titleOf(duplicate)).as("bỏ qua số đã có").isEqualTo("đề   cuối KỲ (3)");
        assertThat(versionOf(duplicate)).as("tăng version để màn hình đang mở phải tải lại").isEqualTo(1L);
        assertThat(versionOf(kept)).isZero();
        assertThat(titleOf(longDuplicate)).hasSize(200).endsWith(" (2)");
        for (UUID untouched : List.of(retired, version, ofClass, elsewhere)) {
            assertThat(titleOf(untouched)).isEqualTo("Đề cuối kỳ");
        }

        assertThatThrownBy(() -> insert(subject, "SUBJECT", "MANUAL", null, "DRAFT", "ĐỀ CUỐI  KỲ", 10))
                .isInstanceOf(DuplicateKeyException.class).hasMessageContaining("uq_assignments_subject_title");
        insert(subject, "SUBJECT", "MANUAL", null, "RETIRED", "Đề cuối kỳ", 11);
        insert(subject, "SUBJECT", "NEW_VERSION", numbered, "DRAFT", "Đề cuối kỳ (2)", 12);

        AssignmentRepository repository = new AssignmentRepository(JdbcClient.create(dataSource),
                new ObjectMapper().findAndRegisterModules());
        Assignment draft = repository.findAssignment(numbered).orElseThrow();
        assertThatThrownBy(() -> repository.saveAssignment(draft.updateAuthoring("đề cuối kỳ", draft.instructions(),
                draft.skeleton(), draft.parts(), T0)))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.readCode()).isEqualTo("TEMPLATE_TITLE_DUPLICATE"));
        Assignment copy = new Assignment(UUID.randomUUID(), AssignmentScope.SUBJECT, null, subject, null,
                AssignmentOrigin.MANUAL, 1, SubmissionMode.INDIVIDUAL, true, GradingMode.GRADED, "Đề cuối kỳ", "",
                new DocumentModel(List.of()), List.of(), Map.of(), List.of(), false, null, AssignmentStatus.DRAFT,
                null, null, null, null, null, actor, T0, T0, 0);
        assertThatThrownBy(() -> repository.insert(copy)).isInstanceOfSatisfying(ApiException.class,
                e -> assertThat(e.readCode()).isEqualTo("TEMPLATE_TITLE_DUPLICATE"));
    }

    private static void migrate(DriverManagerDataSource dataSource, String target) {
        var flyway = Flyway.configure().dataSource(dataSource).locations("classpath:db/migration/assignments")
                .baselineOnMigrate(true).baselineVersion("0");
        if (target != null) {
            flyway = flyway.target(target);
        }
        flyway.load().migrate();
    }

    private UUID insert(UUID subjectId, String scope, String origin, UUID source, String status, String title,
                        int minutes) {
        UUID id = UUID.randomUUID();
        boolean published = !"DRAFT".equals(status) && !"REVIEWED".equals(status);
        Timestamp at = Timestamp.from(T0.plusSeconds(60L * minutes));
        jdbc.update("""
                insert into assignments(id, scope, class_id, subject_id, source_assignment_id, origin, submission_mode,
                    grading_mode, title, skeleton, parts, status, opens_at, closes_at, created_by, created_at, updated_at)
                values (?, ?, ?, ?, ?, ?, 'INDIVIDUAL', 'GRADED', ?, '{"blocks":[]}'::jsonb, '[]'::jsonb, ?, ?, ?, ?, ?, ?)
                """, id, scope, "CLASS".equals(scope) ? courseClass : null, subjectId, source, origin, title, status,
                published ? at : null, published ? Timestamp.from(T0.plusSeconds(86_400)) : null, actor, at, at);
        return id;
    }

    private String titleOf(UUID id) {
        return jdbc.queryForObject("select title from assignments where id = ?", String.class, id);
    }

    private Long versionOf(UUID id) {
        return jdbc.queryForObject("select version from assignments where id = ?", Long.class, id);
    }
}
