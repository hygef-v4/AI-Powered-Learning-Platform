package edu.aiplatform.audit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import edu.aiplatform.audit.contract.AuditEvent;
import edu.aiplatform.audit.contract.AuditResult;
import edu.aiplatform.audit.repository.AuditLogRepository;
import edu.aiplatform.audit.model.AuditRow;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;

class AuditStoreTest {

    static final Instant NOW = Instant.parse("2026-10-18T03:00:00Z");

    final AuditLogRepository repository = mock(AuditLogRepository.class);
    final PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
    final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();
    AuditStore store;

    @BeforeEach
    void setUp() {
        when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        store = new AuditStore(repository, objectMapper, transactionManager);
    }

    static AuditEvent event(AuditResult result, Map<String, Object> details) {
        return new AuditEvent(UUID.randomUUID(), UUID.randomUUID(), "ROLE_CHANGED", "ACCOUNT", "abc", result, null,
                details, NOW);
    }

    @Test
    void successIsWrittenInTheCallerTransaction() {
        store.record(event(AuditResult.SUCCESS, Map.of("before", "STUDENT", "after", "TEACHER")));

        verify(repository).insertIfAbsent(any());
        verify(transactionManager, never()).getTransaction(any());
    }

    @Test
    void deniedAndFailureUseASeparateTransaction() {
        store.record(event(AuditResult.DENIED, Map.of()));
        store.record(event(AuditResult.FAILURE, Map.of()));

        ArgumentCaptor<TransactionDefinition> definitions = ArgumentCaptor.forClass(TransactionDefinition.class);
        verify(transactionManager, times(2)).getTransaction(definitions.capture());
        assertThat(definitions.getAllValues()).extracting(TransactionDefinition::getPropagationBehavior)
                .containsOnly(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        verify(repository, times(2)).insertIfAbsent(any());
    }

    @Test
    void rowCarriesEveryFieldAndDetailsAsJson() {
        AuditEvent event = new AuditEvent(UUID.randomUUID(), null, "BACKUP_FAILED", "BACKUP", "bk-1",
                AuditResult.SUCCESS, "hết thời gian", Map.of("actorType", "WORKER"), NOW);

        store.record(event);

        ArgumentCaptor<AuditRow> row = ArgumentCaptor.forClass(AuditRow.class);
        verify(repository).insertIfAbsent(row.capture());
        assertThat(row.getValue().id()).isEqualTo(event.id());
        assertThat(row.getValue().actorId()).isNull();
        assertThat(row.getValue().result()).isEqualTo("SUCCESS");
        assertThat(row.getValue().reason()).isEqualTo("hết thời gian");
        assertThat(row.getValue().detailsJson()).isEqualTo("{\"actorType\":\"WORKER\"}");
        assertThat(row.getValue().occurredAt()).isEqualTo(NOW);
    }

    @Test
    void sameIdIsDelegatedToInsertIfAbsent() {
        AuditEvent event = event(AuditResult.SUCCESS, Map.of());

        store.record(event);
        store.record(event);

        verify(repository, times(2)).insertIfAbsent(any());
    }

    @Test
    void nullDetailsAreStoredAsEmptyObject() {
        store.record(event(AuditResult.SUCCESS, null));

        ArgumentCaptor<AuditRow> row = ArgumentCaptor.forClass(AuditRow.class);
        verify(repository).insertIfAbsent(row.capture());
        assertThat(row.getValue().detailsJson()).isEqualTo("{}");
    }

    @Test
    void forbiddenKeysAreRejectedEvenWhenNested() {
        assertThatThrownBy(() -> store.record(event(AuditResult.SUCCESS, Map.of("password", "x"))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> store.record(event(AuditResult.DENIED,
                Map.of("after", Map.of("items", List.of(Map.of("Phone", "0901")))))))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Phone");
        verify(repository, never()).insertIfAbsent(any());
    }

    @Test
    void missingRequiredFieldsAreRejected() {
        assertThatThrownBy(() -> store.record(new AuditEvent(null, null, "X", null, null, AuditResult.SUCCESS, null,
                Map.of(), NOW))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> store.record(new AuditEvent(UUID.randomUUID(), null, " ", null, null,
                AuditResult.SUCCESS, null, Map.of(), NOW))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> store.record(new AuditEvent(UUID.randomUUID(), null, "X", null, null, null, null,
                Map.of(), NOW))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> store.record(new AuditEvent(UUID.randomUUID(), null, "X", null, null,
                AuditResult.SUCCESS, null, Map.of(), null))).isInstanceOf(IllegalArgumentException.class);
        verify(repository, never()).insertIfAbsent(any());
    }

    @Test
    void databaseErrorsAreNotSwallowed() {
        when(repository.insertIfAbsent(any())).thenThrow(new DataAccessResourceFailureException("db down"));

        assertThatThrownBy(() -> store.record(event(AuditResult.SUCCESS, Map.of())))
                .isInstanceOf(DataAccessResourceFailureException.class);
        assertThatThrownBy(() -> store.record(event(AuditResult.FAILURE, Map.of())))
                .isInstanceOf(DataAccessResourceFailureException.class);
    }
}
