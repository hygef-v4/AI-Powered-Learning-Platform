package edu.aiplatform.audit.domain;

import static org.assertj.core.api.Assertions.assertThat;

import edu.aiplatform.audit.contract.AuditFilter;
import edu.aiplatform.audit.contract.AuditResult;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AuditLogRepositoryWhereTest {

    @Test
    void noFilterHasNoCondition() {
        assertThat(AuditLogRepository.Where.of(null).sql()).isEmpty();
        AuditLogRepository.Where where = AuditLogRepository.Where.of(
                new AuditFilter(null, null, null, null, null, null, null));
        assertThat(where.sql()).isEmpty();
        assertThat(where.params()).isEmpty();
    }

    @Test
    void partialActorMatchesBecomeAnInListAndNoMatchMatchesNothing() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        AuditLogRepository.Where some = AuditLogRepository.Where.of(
                new AuditFilter(null, null, null, null, null, null, null, "phong", List.of(a, b)));
        assertThat(some.sql()).isEqualTo(" where actor_id in (:actorIds)");
        assertThat(some.params()).containsEntry("actorIds", List.of(a, b));

        AuditLogRepository.Where none = AuditLogRepository.Where.of(
                new AuditFilter(null, null, null, null, null, null, null, "zzz", List.of()));
        assertThat(none.sql()).isEqualTo(" where 1 = 0");
    }

    @Test
    void everyFieldBecomesOneBoundCondition() {
        UUID actor = UUID.randomUUID();
        Instant from = Instant.parse("2026-10-07T00:00:00Z");
        Instant to = Instant.parse("2026-10-14T00:00:00Z");
        AuditLogRepository.Where where = AuditLogRepository.Where.of(
                new AuditFilter(actor, "ROLE_CHANGED", "ACCOUNT", "abc", AuditResult.DENIED, from, to));

        assertThat(where.sql()).isEqualTo(" where actor_id = :actorId and action = :action and object_type = :objectType"
                + " and object_id = :objectId and result = :result and occurred_at >= :from and occurred_at <= :to");
        assertThat(where.params()).containsEntry("actorId", actor).containsEntry("action", "ROLE_CHANGED")
                .containsEntry("objectType", "ACCOUNT").containsEntry("objectId", "abc")
                .containsEntry("result", "DENIED").containsEntry("from", Timestamp.from(from))
                .containsEntry("to", Timestamp.from(to));
    }

    @Test
    void valuesAreBoundNotConcatenated() {
        AuditLogRepository.Where where = AuditLogRepository.Where.of(
                new AuditFilter(null, "x' or '1'='1", null, null, null, null, null));

        assertThat(where.sql()).isEqualTo(" where action = :action");
        assertThat(where.params()).containsEntry("action", "x' or '1'='1");
    }
}
