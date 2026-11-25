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
    final NamedParameterJdbcTemplate namedParameterJdbcTemplate = mock(NamedParameterJdbcTemplate.class);
    final AcademicsReadRepository academicsReadRepository = new AcademicsReadRepository(namedParameterJdbcTemplate);

    @Test void emptyManagerScopeCannotBecomeUnscopedQuery() {
        assertThat(academicsReadRepository.listForManager(List.of(), null, null, null, 20, 0)).isEmpty();
        verifyNoInteractions(namedParameterJdbcTemplate);
        assertThatThrownBy(() -> academicsReadRepository.listForManager(List.of(), null, null, null, 101, 0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test void emptyAggregateInputsDoNotGenerateInvalidInClause() {
        assertThat(academicsReadRepository.countClassesOfSubjects(List.of())).isEmpty();
        assertThat(academicsReadRepository.countEnrollments(List.of())).isEmpty();
        verifyNoInteractions(namedParameterJdbcTemplate);
    }
}
