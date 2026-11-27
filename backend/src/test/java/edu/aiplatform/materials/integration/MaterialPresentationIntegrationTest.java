package edu.aiplatform.materials.integration;

import edu.aiplatform.shared.testsupport.SharedPostgres;
import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.materials.model.Material;
import edu.aiplatform.materials.model.MaterialVersion;
import edu.aiplatform.materials.repository.MaterialRepositoryAdapter;
import edu.aiplatform.shared.content.FormattedText;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class MaterialPresentationIntegrationTest {
    static final SharedPostgres.Database DATABASE = SharedPostgres.createDatabase();

    @Test
    void migrationKeepsLegacyMaterialsEnabledAndJdbcPersistsPresentationWithVersionGuards() {
        var dataSource = new DriverManagerDataSource(DATABASE.getJdbcUrl(), DATABASE.getUsername(), DATABASE.getPassword());
        var jdbc = new JdbcTemplate(dataSource);
        for (String table : new String[] {"accounts", "subjects", "course_classes", "quizzes"}) {
            jdbc.execute("create table " + table + " (id uuid primary key)");
        }
        UUID author = UUID.randomUUID(), subject = UUID.randomUUID(), id = UUID.randomUUID();
        Instant now = Instant.parse("2026-11-09T00:00:00Z");
        jdbc.update("insert into accounts values (?)", author);
        jdbc.update("insert into subjects values (?)", subject);
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration/materials")
                .baselineOnMigrate(true).baselineVersion("0").target("20260925.1900").load().migrate();
        jdbc.update("""
                insert into materials(id,scope,subject_id,kind,title,description,order_no,status,
                    current_version_no,created_by,created_at,updated_at,version)
                values (?,'SUBJECT',?,'FILE','Old','Mô tả cũ',0,'ACTIVE',1,?,?,?,0)
                """, id, subject, author, Timestamp.from(now), Timestamp.from(now));
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration/materials")
                .baselineOnMigrate(true).baselineVersion("0").load().migrate();
        var materialRepository =
                new MaterialRepositoryAdapter(JdbcClient.create(dataSource), new ObjectMapper());
        var legacy = materialRepository.findMaterial(id).orElseThrow();
        assertThat(legacy.enabled()).isTrue();
        assertThat(legacy.descriptionDocument()).isNull();
        assertThat(legacy.description()).isEqualTo("Mô tả cũ");
        var document = new FormattedText(List.of(new FormattedText.Block(FormattedText.Type.UL,
                FormattedText.Align.RIGHT, 1, List.of(new FormattedText.Run("Nội dung", true, null, null)))));
        var changed =
                legacy
                        .reviseMaterial("New", "", 0, 1, Material.Status.ACTIVE, now)
                        .changePresentation(false, document);
        assertThat(materialRepository.updateMaterial(changed, 0)).isTrue();
        assertThat(materialRepository.updateMaterial(changed, 0)).isFalse();
        assertThat(materialRepository.findMaterial(id)).contains(changed);
        assertThat(materialRepository.listMaterials(subject, null)).containsExactly(changed);
        var version = new MaterialVersion(id, 1, "file", "ref.pdf", "application/pdf", 10L, null, null,
                MaterialVersion.TextStatus.PENDING, null, author, now);
        materialRepository.insertVersion(version);
        assertThat(materialRepository.findByFileId("file")).containsExactly(changed);
        assertThat(materialRepository.findVersion(id, 1)).contains(version);
        assertThat(materialRepository.listStalePendingVersions(now.plusSeconds(1)))
                .containsExactly(version);
        assertThat(
                materialRepository.completeText(
                        id, 1, MaterialVersion.TextStatus.EXTRACTED, "source")).isTrue();
        assertThat(materialRepository.completeText(id, 1, MaterialVersion.TextStatus.FAILED, null))
                .isFalse();
        assertThat(materialRepository.listVersions(id))
                .singleElement()
                .satisfies(row -> assertThat(row.extractedText()).isEqualTo("source"));
        assertThat(materialRepository.listStalePendingVersions(now.plusSeconds(1))).isEmpty();

        var inserted = new Material(UUID.randomUUID(), Material.Scope.SUBJECT, subject, null, legacy.moduleId(),
                Material.Kind.FILE, "Created", "", 1, Material.Status.ACTIVE, 1, author, now, now, 0)
                .changePresentation(false, document);
        materialRepository.insertMaterial(
                inserted,
                new MaterialVersion(
                        inserted.id(),
                        1,
                        "second",
                        "second.pdf",
                        "application/pdf",
                        10L,
                        null,
                        null,
                        MaterialVersion.TextStatus.PENDING,
                        null,
                        author,
                        now));
        assertThat(materialRepository.findMaterial(inserted.id())).contains(inserted);
    }
}
