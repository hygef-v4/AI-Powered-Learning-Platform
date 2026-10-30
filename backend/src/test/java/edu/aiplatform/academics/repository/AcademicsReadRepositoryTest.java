package edu.aiplatform.academics.repository;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import edu.aiplatform.academics.contract.ClassStatus;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@SuppressWarnings({"unchecked", "rawtypes"})
class AcademicsReadRepositoryTest {
    final NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);
    final JdbcAcademicsReadRepository repository = new JdbcAcademicsReadRepository(jdbc);

    @Test void emptyManagerScopeCannotBecomeUnscopedQuery() {
        assertThat(repository.listForManager(List.of(), null, null, 20, 0)).isEmpty();
        verifyNoInteractions(jdbc);
        assertThatThrownBy(() -> repository.listForManager(List.of(), null, null, 101, 0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test void managerQueryBindsScopeAndEscapesSearchWildcards() {
        UUID subject = UUID.randomUUID();
        when(jdbc.query(anyString(), anyMap(), any(RowMapper.class))).thenReturn(List.of());
        repository.listForManager(List.of(subject), "50%_!'; drop table x", ClassStatus.OPEN, 25, 50);
        var sql = ArgumentCaptor.forClass(String.class);
        var args = ArgumentCaptor.forClass(Map.class);
        verify(jdbc).query(sql.capture(), args.capture(), any(RowMapper.class));
        assertThat(sql.getValue()).contains("subject_id in (:subjectIds)", "c.status = :status", "limit :limit offset :offset", "escape '!'");
        assertThat(sql.getValue()).doesNotContain("drop table");
        assertThat(args.getValue()).containsEntry("subjectIds", List.of(subject)).containsEntry("limit", 25).containsEntry("offset", 50)
                .containsEntry("search", "%50!%!_!!'; drop table x%");
    }

    @Test void studentQueryAlwaysRequiresActiveEnrollmentAndVisibleStatus() {
        UUID student = UUID.randomUUID();
        when(jdbc.query(anyString(), anyMap(), any(RowMapper.class))).thenReturn(List.of());
        repository.listForStudent(student, null, null, 20, 0);
        var sql = ArgumentCaptor.forClass(String.class);
        var args = ArgumentCaptor.forClass(Map.class);
        verify(jdbc).query(sql.capture(), args.capture(), any(RowMapper.class));
        assertThat(sql.getValue()).contains("exists", "e.status = 'ACTIVE'", "c.status in ('OPEN', 'ARCHIVED')", "e.account_id = :accountId");
        assertThat(args.getValue()).containsEntry("accountId", student);
    }

    @Test void teacherScopeAndPaginationAreBoundAndValidated() {
        UUID teacher = UUID.randomUUID();
        when(jdbc.query(anyString(), anyMap(), any(RowMapper.class))).thenReturn(List.of());
        repository.listForTeacher(teacher, "  ", null, 100, 0);
        verify(jdbc).query(contains("c.teacher_id = :accountId"), argThat((Map<String,Object> args) -> teacher.equals(args.get("accountId"))), any(RowMapper.class));
        reset(jdbc);
        assertThatThrownBy(() -> repository.listForStudent(teacher, null, null, 20, -1)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(jdbc);
    }

    @Test void emptyAggregateInputsDoNotGenerateInvalidInClause() {
        assertThat(repository.countClassesOfSubjects(List.of())).isEmpty();
        assertThat(repository.enrollmentCounts(List.of())).isEmpty();
        verifyNoInteractions(jdbc);
    }
}
