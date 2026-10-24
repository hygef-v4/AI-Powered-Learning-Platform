package edu.aiplatform.ai.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import edu.aiplatform.ai.application.AiSourceService.SourceOption;
import edu.aiplatform.ai.contract.AiProvider;
import edu.aiplatform.ai.contract.AiSource;
import edu.aiplatform.ai.domain.AiConnection;
import edu.aiplatform.ai.domain.AiConnectionRepository;
import edu.aiplatform.audit.contract.IAudit;
import edu.aiplatform.identity.contract.Actions;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.AuthorizationDecision;
import edu.aiplatform.identity.contract.IAuthorization;
import edu.aiplatform.identity.contract.Role;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AiSourceServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-21T03:00:00Z");
    private final AiConnectionRepository connections = mock(AiConnectionRepository.class);
    private final QuotaReservationService quota = mock(QuotaReservationService.class);
    private final IAuthorization authorization = mock(IAuthorization.class);
    private final AiSourceService service = new AiSourceService(connections, quota,
            new AiAccess(authorization, mock(IAudit.class), Clock.fixed(NOW, ZoneOffset.UTC)));

    @Test
    void teacherSeesSharedQuotaAndSavedPersonalKeysOnly() {
        Actor teacher = new Actor(UUID.randomUUID(), Role.TEACHER);
        when(authorization.authorize(eq(teacher), any(), any())).thenReturn(AuthorizationDecision.allow());
        AiConnection shared = AiConnection.create(UUID.randomUUID(), AiConnection.Scope.INSTITUTIONAL, null,
                AiProvider.GEMINI, "gemini-2.5-flash", new AiConnection.EncryptedKey(new byte[] {1}, new byte[] {1}, 1),
                "1111", NOW);
        AiConnection mine = AiConnection.create(UUID.randomUUID(), AiConnection.Scope.PERSONAL, teacher.accountId(),
                AiProvider.CLAUDE, "claude-sonnet-5-5", new AiConnection.EncryptedKey(new byte[] {1}, new byte[] {1}, 1),
                "3kQf", NOW);
        when(connections.findInstitutional(any())).thenReturn(Optional.empty());
        when(connections.findInstitutional(AiProvider.GEMINI)).thenReturn(Optional.of(shared));
        when(connections.findPersonal(any(), any())).thenReturn(Optional.empty());
        when(connections.findPersonal(teacher.accountId(), AiProvider.CLAUDE)).thenReturn(Optional.of(mine));
        when(quota.remaining(teacher.accountId())).thenReturn(new QuotaReservationService.Remaining(4_000, 10_000, 6_000,
                ZonedDateTime.parse("2026-10-22T00:00+07:00")));

        List<SourceOption> options = service.sources(teacher);

        assertThat(options).hasSize(4);
        SourceOption first = options.get(0);
        assertThat(first.source()).isEqualTo(AiSource.SHARED);
        assertThat(first.available()).isTrue();
        assertThat(first.quota().left()).isEqualTo(6_000);
        assertThat(first.costBearer()).isEqualTo("SCHOOL");
        assertThat(options).filteredOn(SourceOption::available).extracting(SourceOption::provider)
                .containsExactly(AiProvider.GEMINI, AiProvider.CLAUDE);
        assertThat(options).filteredOn(o -> o.provider() == AiProvider.CLAUDE).singleElement()
                .satisfies(o -> assertThat(o.keyHint()).isEqualTo("3kQf"));
    }

    @Test
    void studentOnlyGetsPersonalSources() {
        Actor student = new Actor(UUID.randomUUID(), Role.STUDENT);
        when(authorization.authorize(eq(student), eq(Actions.AI_PERSONAL), any())).thenReturn(AuthorizationDecision.allow());
        when(authorization.authorize(eq(student), eq(Actions.AI_SHARED_USE), any()))
                .thenReturn(AuthorizationDecision.deny("ROLE"));
        when(connections.findPersonal(any(), any())).thenReturn(Optional.empty());

        List<SourceOption> options = service.sources(student);

        assertThat(options).extracting(SourceOption::source).containsOnly(AiSource.PERSONAL);
        assertThat(options).hasSize(AiProvider.values().length);
    }
}
