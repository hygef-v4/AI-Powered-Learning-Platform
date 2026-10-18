package edu.aiplatform.academics.infrastructure;

import edu.aiplatform.academics.domain.CourseClass;
import edu.aiplatform.academics.domain.AcademicRuleException;
import edu.aiplatform.academics.domain.Enrollment;
import edu.aiplatform.academics.domain.EnrollmentSource;
import edu.aiplatform.academics.domain.EnrollmentStatus;
import java.time.Instant;
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
    private final NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);
    private final Instant now = Instant.parse("2026-10-17T00:00:00Z");

    @AfterEach void clearTransaction() { TransactionSynchronizationManager.clear(); }

    @Test void updatingStaleVersionRaisesConflictAndNeverChangesIdentity() {
        TransactionSynchronizationManager.setActualTransactionActive(true);
        var repository = new JdbcCourseClassRepository(jdbc);
        var original = CourseClass.create(UUID.randomUUID(), UUID.randomUUID(), "C1", "Class", null, null, null, now);
        when(jdbc.update(anyString(), anyMap())).thenReturn(0);
        assertThatThrownBy(() -> repository.update(original.updateInfo("New", null, null, now), 0))
                .isInstanceOf(AcademicRuleException.class).hasMessageContaining("tải lại");
        verify(jdbc).update(argThat(sql -> sql.contains("where id = :id and version = :expectedVersion") &&
                !sql.contains("set subject_id") && !sql.contains("set code")),
                argThat((Map<String, ?> params) -> params.get("expectedVersion").equals(0L)));
    }

    @Test void rejectsUncoordinatedWritesAndInvalidVersion() {
        var repository = new JdbcCourseClassRepository(jdbc);
        var original = CourseClass.create(UUID.randomUUID(), UUID.randomUUID(), "C1", "Class", null, null, null, now);
        assertThatThrownBy(() -> repository.insert(original)).isInstanceOf(IllegalStateException.class);
        TransactionSynchronizationManager.setActualTransactionActive(true);
        assertThatThrownBy(() -> repository.update(original, 5)).isInstanceOf(IllegalArgumentException.class);
        TransactionSynchronizationManager.setCurrentTransactionReadOnly(true);
        assertThatThrownBy(() -> repository.insert(original)).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(jdbc);
    }

    @Test void enforcesPaginationBoundsBeforeQueryingDatabase() {
        var repository = new JdbcCourseClassRepository(jdbc);
        assertThatThrownBy(() -> repository.listBySubject(UUID.randomUUID(), 101, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> repository.listBySubject(UUID.randomUUID(), 0, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> repository.listBySubject(UUID.randomUUID(), 20, -1)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(jdbc);
    }

    @Test void persistenceBeansAreDisabledUntilExplicitlyEnabled() {
        var runner = new ApplicationContextRunner().withBean(NamedParameterJdbcTemplate.class, () -> jdbc)
                .withUserConfiguration(JdbcCourseClassRepository.class, JdbcEnrollmentRepository.class, SubjectMutationLock.class);
        runner.run(context -> {
            assertThat(context).doesNotHaveBean(JdbcCourseClassRepository.class);
            assertThat(context).doesNotHaveBean(JdbcEnrollmentRepository.class);
            assertThat(context).doesNotHaveBean(SubjectMutationLock.class);
        });
        runner.withPropertyValues("platform.academics.persistence-enabled=true").run(context -> {
            assertThat(context).hasSingleBean(JdbcCourseClassRepository.class);
            assertThat(context).hasSingleBean(JdbcEnrollmentRepository.class);
            assertThat(context).hasSingleBean(SubjectMutationLock.class);
        });
    }

    @Test void subjectLockCannotEscapeTransaction() {
        assertThatThrownBy(() -> new SubjectMutationLock(jdbc).acquire(UUID.randomUUID())).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(jdbc);
    }

    @Test void duplicateEnrollmentAndConcurrentStatusChangeAreReported() {
        TransactionSynchronizationManager.setActualTransactionActive(true);
        var repository = new JdbcEnrollmentRepository(jdbc);
        var value = Enrollment.create(UUID.randomUUID(), UUID.randomUUID(), EnrollmentSource.MANUAL, now);
        when(jdbc.update(anyString(), anyMap())).thenThrow(new DuplicateKeyException("SQL details"));
        assertThatThrownBy(() -> repository.insert(value)).isInstanceOf(AcademicRuleException.class)
                .hasMessageNotContaining("SQL details");
        reset(jdbc);
        when(jdbc.update(anyString(), anyMap())).thenReturn(0);
        assertThatThrownBy(() -> repository.update(value.remove(now), EnrollmentStatus.ACTIVE))
                .isInstanceOf(AcademicRuleException.class).hasMessageContaining("tải lại");
    }

    @Test void enrollmentWritesRequireTransactionAndReadQueriesAreNotCached() {
        var repository = new JdbcEnrollmentRepository(jdbc);
        var accountId = UUID.randomUUID();
        var subjectId = UUID.randomUUID();
        var classId = UUID.randomUUID();
        var value = Enrollment.create(classId, accountId, EnrollmentSource.MANUAL, now);
        assertThatThrownBy(() -> repository.insert(value)).isInstanceOf(IllegalStateException.class);
        when(jdbc.queryForObject(anyString(), anyMap(), eq(Boolean.class))).thenReturn(true, false);
        assertThat(repository.activeInOtherClass(accountId, subjectId, classId)).isTrue();
        assertThat(repository.activeInOtherClass(accountId, subjectId, classId)).isFalse();
        verify(jdbc, times(2)).queryForObject(anyString(), anyMap(), eq(Boolean.class));
    }
}
