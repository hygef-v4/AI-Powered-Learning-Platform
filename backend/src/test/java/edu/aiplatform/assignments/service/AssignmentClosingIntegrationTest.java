package edu.aiplatform.assignments.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.assignments.repository.AssignmentRepository;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class AssignmentClosingIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> DATABASE = new PostgreSQLContainer<>("postgres:16-alpine");
    final UUID assignmentId = UUID.randomUUID();
    final Instant deadline = Instant.parse("2026-11-15T12:00:00Z");
    DriverManagerDataSource dataSource;
    AssignmentRepository repository;

    @BeforeEach
    void createAssignment() {
        dataSource = new DriverManagerDataSource(DATABASE.getJdbcUrl(), DATABASE.getUsername(), DATABASE.getPassword());
        var jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("create table if not exists assignments (id uuid primary key, status text, closes_at timestamptz, late_until timestamptz)");
        jdbc.update("delete from assignments");
        jdbc.update("insert into assignments values (?, 'OPEN', ?, ?)", assignmentId,
                Timestamp.from(deadline.minusSeconds(3600)), Timestamp.from(deadline));
        repository = new AssignmentRepository(JdbcClient.create(dataSource), new ObjectMapper());
    }

    @Test
    void closingWaitsForSharedSubmissionLockUntilTheWriteCommits() throws Exception {
        try (var student = dataSource.getConnection(); var teacher = dataSource.getConnection()) {
            student.setAutoCommit(false);
            teacher.setAutoCommit(false);
            new AssignmentRepository(JdbcClient.create(new org.springframework.jdbc.datasource.SingleConnectionDataSource(
                    student, true)), new ObjectMapper()).lockSubmission(assignmentId);
            var closing = new JdbcTemplate(new org.springframework.jdbc.datasource.SingleConnectionDataSource(teacher, true));
            closing.execute("set local lock_timeout = '100ms'");
            assertThatThrownBy(() -> closing.update("update assignments set status = 'CLOSED' where id = ?", assignmentId))
                    .isInstanceOfSatisfying(DataAccessException.class, error -> assertThat(
                            ((java.sql.SQLException) error.getMostSpecificCause()).getSQLState()).isEqualTo("55P03"));
            teacher.rollback();
            student.commit();
            assertThat(closing.update("update assignments set status = 'CLOSED' where id = ?", assignmentId)).isOne();
            teacher.commit();
        }
    }

    @Test
    void automaticCloseUsesLateDeadlineAndWaitsForTheWholeGraceWindow() {
        assertThat(repository.listDueToClose(deadline.minusSeconds(1), 200)).isEmpty();
        assertThat(repository.listDueToClose(deadline.plusSeconds(30), 200)).isEmpty();
        assertThat(repository.listDueToClose(deadline.plusSeconds(30).plusNanos(1000), 200))
                .containsExactly(assignmentId);
    }

    @Test
    void aFailedAutoSubmissionRollsBackTheClosedStatus() {
        var fixture = new AssignmentFixtures();
        var open = fixture.put(edu.aiplatform.assignments.contract.AssignmentScope.CLASS,
                edu.aiplatform.assignments.contract.AssignmentStatus.OPEN,
                new edu.aiplatform.assignments.contract.DocumentModel(java.util.List.of()), true);
        var jdbc = new JdbcTemplate(dataSource);
        jdbc.update("insert into assignments values (?, 'OPEN', ?, null)", open.id(), Timestamp.from(deadline));
        var assignments = org.mockito.Mockito.mock(AssignmentRepository.class);
        org.mockito.Mockito.when(assignments.findForUpdate(open.id())).thenReturn(java.util.Optional.of(open));
        org.mockito.Mockito.when(assignments.save(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> {
            edu.aiplatform.assignments.model.Assignment changed = invocation.getArgument(0);
            jdbc.update("update assignments set status = ? where id = ?", changed.status().name(), changed.id());
            return changed;
        });
        org.mockito.Mockito.doThrow(new IllegalStateException("auto-submit failed"))
                .when(fixture.listener).onClosed(open.id());
        var lifecycle = new AssignmentLifecycle(assignments, fixture.listeners, fixture.events);
        var transaction = new org.springframework.transaction.support.TransactionTemplate(
                new org.springframework.jdbc.datasource.DataSourceTransactionManager(dataSource));
        assertThatThrownBy(() -> transaction.execute(status -> lifecycle.closeAssignment(open.id(), deadline, false)))
                .hasMessage("auto-submit failed");
        assertThat(jdbc.queryForObject("select status from assignments where id = ?", String.class, open.id()))
                .isEqualTo("OPEN");
    }
}
