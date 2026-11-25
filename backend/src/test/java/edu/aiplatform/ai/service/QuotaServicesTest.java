package edu.aiplatform.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.aiplatform.ai.repository.AiUsageRepository;
import edu.aiplatform.ai.model.QuotaPolicy;
import edu.aiplatform.ai.model.QuotaPolicy.TeacherQuota;
import edu.aiplatform.ai.model.QuotaPolicy.Window;
import edu.aiplatform.ai.repository.QuotaRepository;
import edu.aiplatform.audit.contract.AuditEvent;
import edu.aiplatform.audit.contract.IAudit;
import edu.aiplatform.identity.contract.AccountContact;
import edu.aiplatform.identity.contract.AccountStatus;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.AuthorizationDecision;
import edu.aiplatform.identity.contract.IAccountLookup;
import edu.aiplatform.identity.contract.IAuthorization;
import edu.aiplatform.identity.contract.Role;
import edu.aiplatform.shared.error.ApiException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.http.HttpStatus;

class QuotaServicesTest {

    /** 17:30 UTC là 00:30 ngày 22/10 theo giờ Việt Nam. */
    private static final Instant NOW = Instant.parse("2026-10-21T17:30:00Z");
    private static final LocalDate VN_DAY = LocalDate.of(2026, 10, 22);
    private final QuotaRepository repository = mock(QuotaRepository.class);
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final QuotaReservationService reservations = new QuotaReservationService(repository, clock);
    private final UUID teacher = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        when(repository.loadPolicy()).thenReturn(new QuotaPolicy(10_000L, 50_000L, 1, null, NOW));
        when(repository.findTeacherQuota(any())).thenReturn(Optional.empty());
        when(repository.lockWindow(eq(QuotaRepository.GLOBAL), any())).thenReturn(new Window("GLOBAL", VN_DAY, 0, 1_000));
        when(repository.lockWindow(eq(teacher.toString()), any())).thenReturn(new Window(teacher.toString(), VN_DAY, 500, 2_000));
    }

    @Test
    void reserveLocksGlobalFirstThenOwnAndAddsToBoth() {
        var reservation = reservations.reserve(teacher, 3_000);

        assertThat(reservation.day()).isEqualTo(VN_DAY);
        InOrder order = inOrder(repository);
        order.verify(repository).lockWindow(QuotaRepository.GLOBAL, VN_DAY);
        order.verify(repository).lockWindow(teacher.toString(), VN_DAY);
        verify(repository).adjust(QuotaRepository.GLOBAL, VN_DAY, 3_000, 0);
        verify(repository).adjust(teacher.toString(), VN_DAY, 3_000, 0);
    }

    @Test
    void ownDailyLimitIsEnforced() {
        assertThatThrownBy(() -> reservations.reserve(teacher, 7_600))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.readCode()).isEqualTo("AI_QUOTA_EXCEEDED");
                    assertThat(e.readStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
                    assertThat(e.getMessage()).contains("2026-10-23T00:00+07:00");
                });
        verify(repository, never()).adjust(any(), any(), anyLong(), anyLong());
    }

    @Test
    void globalCeilingIsEnforced() {
        when(repository.lockWindow(eq(QuotaRepository.GLOBAL), any())).thenReturn(new Window("GLOBAL", VN_DAY, 0, 49_900));
        assertThatThrownBy(() -> reservations.reserve(teacher, 200))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.readCode()).isEqualTo("AI_QUOTA_GLOBAL_EXCEEDED"));
    }

    @Test
    void notConfiguredOrBlockedTeacherCannotUseShared() {
        when(repository.findTeacherQuota(teacher)).thenReturn(Optional.of(new TeacherQuota(teacher, 0, 1, null, NOW)));
        assertThatThrownBy(() -> reservations.reserve(teacher, 10))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.readCode()).isEqualTo("AI_QUOTA_BLOCKED"));
        when(repository.loadPolicy()).thenReturn(QuotaPolicy.createEmptyPolicy());
        assertThatThrownBy(() -> reservations.reserve(teacher, 10))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.readCode()).isEqualTo("AI_QUOTA_NOT_CONFIGURED"));
        assertThat(reservations.limitOf(teacher)).isNull();
    }
}
