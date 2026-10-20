package edu.aiplatform.ai.application;

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

import edu.aiplatform.ai.domain.AiUsageRepository;
import edu.aiplatform.ai.domain.QuotaPolicy;
import edu.aiplatform.ai.domain.QuotaPolicy.TeacherQuota;
import edu.aiplatform.ai.domain.QuotaPolicy.Window;
import edu.aiplatform.ai.domain.QuotaRepository;
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
        when(repository.policy()).thenReturn(new QuotaPolicy(10_000L, 50_000L, 1, null, NOW));
        when(repository.teacherQuota(any())).thenReturn(Optional.empty());
        when(repository.lockWindow(eq(QuotaRepository.GLOBAL), any())).thenReturn(new Window("GLOBAL", VN_DAY, 0, 1_000));
        when(repository.lockWindow(eq(teacher.toString()), any())).thenReturn(new Window(teacher.toString(), VN_DAY, 500, 2_000));
    }

    @Test
    void estimateIsHalfThePromptPlusMaxOutput() {
        assertThat(QuotaReservationService.estimate(1001, 500)).isEqualTo(501 + 500);
        assertThat(QuotaReservationService.estimate(0, 100)).isEqualTo(100);
    }

    @Test
    void dayFollowsVietnamTime() {
        assertThat(reservations.today()).isEqualTo(VN_DAY);
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
                    assertThat(e.code()).isEqualTo("AI_QUOTA_EXCEEDED");
                    assertThat(e.status()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
                    assertThat(e.getMessage()).contains("2026-10-23T00:00+07:00");
                });
        verify(repository, never()).adjust(any(), any(), anyLong(), anyLong());
    }

    @Test
    void globalCeilingIsEnforced() {
        when(repository.lockWindow(eq(QuotaRepository.GLOBAL), any())).thenReturn(new Window("GLOBAL", VN_DAY, 0, 49_900));
        assertThatThrownBy(() -> reservations.reserve(teacher, 200))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("AI_QUOTA_GLOBAL_EXCEEDED"));
    }

    @Test
    void notConfiguredOrBlockedTeacherCannotUseShared() {
        when(repository.teacherQuota(teacher)).thenReturn(Optional.of(new TeacherQuota(teacher, 0, 1, null, NOW)));
        assertThatThrownBy(() -> reservations.reserve(teacher, 10))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("AI_QUOTA_BLOCKED"));
        when(repository.policy()).thenReturn(QuotaPolicy.empty());
        assertThatThrownBy(() -> reservations.reserve(teacher, 10))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("AI_QUOTA_NOT_CONFIGURED"));
        assertThat(reservations.limitOf(teacher)).isNull();
    }

    @Test
    void teacherOverrideReplacesDefault() {
        when(repository.teacherQuota(teacher)).thenReturn(Optional.of(new TeacherQuota(teacher, 30_000, 1, null, NOW)));
        assertThat(reservations.limitOf(teacher)).isEqualTo(30_000);
        reservations.reserve(teacher, 20_000);
        verify(repository).adjust(teacher.toString(), VN_DAY, 20_000, 0);
    }

    @Test
    void settleReleaseAndChargeAdjustReservedAndUsed() {
        reservations.settle(teacher, VN_DAY, 1_000, 1_400);
        verify(repository).adjust(teacher.toString(), VN_DAY, -1_000, 1_400);
        reservations.release(teacher, VN_DAY, 800);
        verify(repository).adjust(teacher.toString(), VN_DAY, -800, 0);
        reservations.chargeReserved(teacher, VN_DAY, 600);
        verify(repository).adjust(teacher.toString(), VN_DAY, -600, 600);
    }

    @Test
    void remainingTakesTheSmallerOfOwnAndGlobal() {
        when(repository.findWindow(teacher.toString(), VN_DAY)).thenReturn(Optional.of(new Window(teacher.toString(), VN_DAY, 0, 4_000)));
        when(repository.findWindow(QuotaRepository.GLOBAL, VN_DAY)).thenReturn(Optional.of(new Window("GLOBAL", VN_DAY, 0, 48_000)));
        var remaining = reservations.remaining(teacher);
        assertThat(remaining.used()).isEqualTo(4_000);
        assertThat(remaining.left()).isEqualTo(2_000);
    }

    @Test
    void policyChangesAreAuditedWithBeforeAndAfterAndOnlyTeachersGetOverrides() {
        IAuthorization authorization = mock(IAuthorization.class);
        when(authorization.authorize(any(), any(), any())).thenReturn(AuthorizationDecision.allow());
        IAudit audit = mock(IAudit.class);
        IAccountLookup accounts = mock(IAccountLookup.class);
        QuotaPolicyService service = new QuotaPolicyService(repository, reservations, mock(AiUsageRepository.class),
                accounts, new AiAccess(authorization, audit, clock), audit, clock);
        Actor admin = new Actor(UUID.randomUUID(), Role.ADMIN);
        when(repository.savePolicy(20_000, 80_000, 1, admin.accountId(), NOW)).thenReturn(true);

        service.savePolicy(admin, 20_000L, 80_000L, 1L);

        ArgumentCaptor<AuditEvent> event = ArgumentCaptor.forClass(AuditEvent.class);
        verify(audit).record(event.capture());
        assertThat(event.getValue().action()).isEqualTo("QUOTA_CHANGED");
        assertThat(event.getValue().details()).containsEntry("defaultBefore", 10_000L).containsEntry("defaultAfter", 20_000L);

        UUID student = UUID.randomUUID();
        when(accounts.getContact(student)).thenReturn(Optional.of(new AccountContact(student, "sv@fpt.edu.vn", "SV",
                Role.STUDENT, AccountStatus.ACTIVE)));
        assertThatThrownBy(() -> service.setTeacherQuota(admin, student, 1000L))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("AI_QUOTA_NOT_TEACHER"));
        assertThatThrownBy(() -> service.savePolicy(admin, -1L, 10L, 1L))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("AI_QUOTA_INVALID"));
        when(repository.savePolicy(anyLong(), anyLong(), anyLong(), any(), any())).thenReturn(false);
        assertThatThrownBy(() -> service.savePolicy(admin, 1L, 10L, 0L))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.status()).isEqualTo(HttpStatus.CONFLICT));
    }
}
