package edu.aiplatform.academics.integration;

import edu.aiplatform.academics.repository.CourseClassRepository;
import edu.aiplatform.academics.repository.EnrollmentRepository;

import edu.aiplatform.academics.model.CourseClass;
import edu.aiplatform.academics.model.AcademicRuleException;
import edu.aiplatform.academics.model.Enrollment;
import edu.aiplatform.academics.model.EnrollmentSource;
import edu.aiplatform.academics.model.EnrollmentStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AcademicsPersistenceTest {
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate = mock(NamedParameterJdbcTemplate.class);
    private final Instant now = Instant.parse("2026-10-17T00:00:00Z");
    private final LocalDate start = LocalDate.of(2027, 1, 10);

    @AfterEach void clearTransaction() { TransactionSynchronizationManager.clear(); }

    @Test void updatingStaleVersionRaisesConflictAndNeverChangesIdentity() {
        TransactionSynchronizationManager.setActualTransactionActive(true);
        var repository = new CourseClassRepository(namedParameterJdbcTemplate);
        var original = CourseClass.createClass(UUID.randomUUID(), UUID.randomUUID(), "C1", "Class", null, start, null, now);
        when(namedParameterJdbcTemplate.update(anyString(), anyMap())).thenReturn(0);
        assertThatThrownBy(() -> repository.updateClass(original.updateInfo("New", null, start, now), 0))
                .isInstanceOf(AcademicRuleException.class).hasMessageContaining("tải lại");
        verify(namedParameterJdbcTemplate).update(argThat(sql -> sql.contains("where id = :id and version = :expectedVersion") &&
                !sql.contains("set subject_id") && !sql.contains("set code")),
                argThat((Map<String, ?> params) -> params.get("expectedVersion").equals(0L)));
    }

    @Test void rejectsUncoordinatedWritesAndInvalidVersion() {
        var repository = new CourseClassRepository(namedParameterJdbcTemplate);
        var original = CourseClass.createClass(UUID.randomUUID(), UUID.randomUUID(), "C1", "Class", null, start, null, now);
        assertThatThrownBy(() -> repository.insert(original)).isInstanceOf(IllegalStateException.class);
        TransactionSynchronizationManager.setActualTransactionActive(true);
        assertThatThrownBy(() -> repository.updateClass(original, 5)).isInstanceOf(IllegalArgumentException.class);
        TransactionSynchronizationManager.setCurrentTransactionReadOnly(true);
        assertThatThrownBy(() -> repository.insert(original)).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(namedParameterJdbcTemplate);
    }

    @Test void enforcesPaginationBoundsBeforeQueryingDatabase() {
        var repository = new CourseClassRepository(namedParameterJdbcTemplate);
        assertThatThrownBy(() -> repository.listBySubject(UUID.randomUUID(), 101, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> repository.listBySubject(UUID.randomUUID(), 0, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> repository.listBySubject(UUID.randomUUID(), 20, -1)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(namedParameterJdbcTemplate);
    }

    @Test void persistenceBeansAreRegistered() {
        var runner = new ApplicationContextRunner().withBean(NamedParameterJdbcTemplate.class, () -> namedParameterJdbcTemplate)
                .withUserConfiguration(CourseClassRepository.class, EnrollmentRepository.class);
        runner.run(context -> {
            assertThat(context).hasSingleBean(CourseClassRepository.class);
            assertThat(context).hasSingleBean(EnrollmentRepository.class);
        });
    }

    @Test void duplicateEnrollmentAndConcurrentStatusChangeAreReported() {
        TransactionSynchronizationManager.setActualTransactionActive(true);
        var repository = new EnrollmentRepository(namedParameterJdbcTemplate);
        var value = Enrollment.createEnrollment(UUID.randomUUID(), UUID.randomUUID(), EnrollmentSource.MANUAL, now);
        when(namedParameterJdbcTemplate.update(anyString(), anyMap())).thenThrow(new DuplicateKeyException("SQL details"));
        assertThatThrownBy(() -> repository.insert(value)).isInstanceOf(AcademicRuleException.class)
                .hasMessageNotContaining("SQL details");
        reset(namedParameterJdbcTemplate);
        when(namedParameterJdbcTemplate.update(anyString(), anyMap())).thenReturn(0);
        assertThatThrownBy(() -> repository.updateEnrollment(value.removeEnrollment(now), EnrollmentStatus.ACTIVE))
                .isInstanceOf(AcademicRuleException.class).hasMessageContaining("tải lại");
    }

    @Test void enrollmentWritesRequireTransactionAndReadQueriesAreNotCached() {
        var repository = new EnrollmentRepository(namedParameterJdbcTemplate);
        var accountId = UUID.randomUUID();
        var subjectId = UUID.randomUUID();
        var classId = UUID.randomUUID();
        var value = Enrollment.createEnrollment(classId, accountId, EnrollmentSource.MANUAL, now);
        assertThatThrownBy(() -> repository.insert(value)).isInstanceOf(IllegalStateException.class);
        when(namedParameterJdbcTemplate.queryForObject(anyString(), anyMap(), eq(Boolean.class))).thenReturn(true, false);
        assertThat(repository.hasActiveEnrollmentElsewhere(accountId, subjectId, classId)).isTrue();
        assertThat(repository.hasActiveEnrollmentElsewhere(accountId, subjectId, classId)).isFalse();
        verify(namedParameterJdbcTemplate, times(2)).queryForObject(anyString(), anyMap(), eq(Boolean.class));
    }

    @Test void writesStartDateAndMapsDuplicateCodeInTermToClassExists() {
        TransactionSynchronizationManager.setActualTransactionActive(true);
        var repository = new CourseClassRepository(namedParameterJdbcTemplate);
        var original = CourseClass.createClass(UUID.randomUUID(), UUID.randomUUID(), "C1", "Class", null, start, null, now);
        when(namedParameterJdbcTemplate.update(anyString(), anyMap())).thenThrow(new DuplicateKeyException("SQL details"));
        assertThatThrownBy(() -> repository.insert(original))
                .isInstanceOfSatisfying(AcademicRuleException.class, error -> {
                    assertThat(error.readCode()).isEqualTo("CLASS_EXISTS");
                    assertThat(error.getMessage()).isEqualTo("Mã lớp đã tồn tại trong môn ở học kỳ này");
                });
        var moved = original.updateInfo("Class", null, LocalDate.of(2027, 5, 15), now.plusSeconds(1));
        assertThatThrownBy(() -> repository.updateClass(moved, 0))
                .isInstanceOfSatisfying(AcademicRuleException.class,
                        error -> assertThat(error.readCode()).isEqualTo("CLASS_EXISTS"));
        verify(namedParameterJdbcTemplate).update(argThat(sql -> sql.contains("insert") && sql.contains("starts_on")
                && !sql.contains(" term")), argThat((Map<String, ?> params) ->
                params.get("startsOn").equals(java.sql.Date.valueOf(start))));
        verify(namedParameterJdbcTemplate).update(argThat(sql -> sql.contains("starts_on = :startsOn")
                && !sql.contains("term =")), anyMap());
    }
}
