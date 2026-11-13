package edu.aiplatform.grading.repository;

import static org.assertj.core.api.Assertions.*;

import java.util.UUID;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

@Testcontainers(disabledWithoutDocker = true)
class GradeDistributionVisibilityRepositoryTest {
    @Container static final PostgreSQLContainer<?> DB = new PostgreSQLContainer<>("postgres:16-alpine");

    @Test void migrationDefaultsOffAndConcurrentWritersCannotOverwriteAcrossClasses() throws Exception {
        var source = new DriverManagerDataSource(DB.getJdbcUrl(), DB.getUsername(), DB.getPassword());
        var jdbc = new JdbcTemplate(source);
        jdbc.execute("CREATE TABLE course_classes (id UUID PRIMARY KEY, show_grade_distribution BOOLEAN)");
        jdbc.execute("CREATE TABLE assignments (id UUID PRIMARY KEY)");
        var classId = UUID.randomUUID();
        var secondClass = UUID.randomUUID();
        var assignmentId = UUID.randomUUID();
        jdbc.update("INSERT INTO course_classes VALUES (?,true),(?,true)", classId, secondClass);
        jdbc.update("INSERT INTO assignments VALUES (?)", assignmentId);
        new ResourceDatabasePopulator(new ClassPathResource(
                "db/migration/grading/V20261101_0900__assignment_grade_distribution_visibility.sql")).execute(source);
        var repository = new GradeDistributionVisibilityRepository(jdbc);
        assertThat(repository.findVisibility(classId, assignmentId))
                .isEqualTo(new GradeDistributionVisibilityRepository.Visibility(false, 0));
        race(repository, classId, assignmentId, 0);
        assertThat(repository.findVisibility(classId, assignmentId).version()).isEqualTo(1);
        assertThat(repository.findVisibility(secondClass, assignmentId).enabled()).isFalse();
        assertThat(repository.updateVisibility(secondClass, assignmentId, false, 0)).isTrue();
        race(repository, classId, assignmentId, 1);
        assertThat(repository.findVisibility(classId, assignmentId).version()).isEqualTo(2);
        assertThat(repository.findVisibility(secondClass, assignmentId).version()).isEqualTo(1);
        assertThat(repository.updateVisibility(classId, assignmentId, false, 1)).isFalse();
    }

    void race(GradeDistributionVisibilityRepository repository, UUID classId, UUID assignmentId, long version)
            throws Exception {
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        var workers = Executors.newFixedThreadPool(2);
        try {
            Callable<Boolean> write = () -> {
                ready.countDown();
                if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Writer timed out");
                return repository.updateVisibility(classId, assignmentId, true, version);
            };
            var first = workers.submit(write);
            var second = workers.submit(write);
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            assertThat(java.util.List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(true, false);
        } finally {
            workers.shutdownNow();
        }
    }
}
