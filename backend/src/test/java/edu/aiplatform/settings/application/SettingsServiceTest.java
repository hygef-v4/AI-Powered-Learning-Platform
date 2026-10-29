package edu.aiplatform.settings.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import edu.aiplatform.audit.contract.AuditEvent;
import edu.aiplatform.audit.contract.AuditResult;
import edu.aiplatform.audit.contract.IAudit;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.AuthorizationDecision;
import edu.aiplatform.identity.contract.IAccountLookup;
import edu.aiplatform.identity.contract.IAuthorization;
import edu.aiplatform.identity.contract.Role;
import edu.aiplatform.settings.contract.SettingDefinition;
import edu.aiplatform.settings.contract.SettingGroup;
import edu.aiplatform.settings.contract.SettingKey;
import edu.aiplatform.settings.contract.SettingType;
import edu.aiplatform.settings.domain.SettingRow;
import edu.aiplatform.settings.domain.SettingValidator;
import edu.aiplatform.settings.domain.SystemSettingRepository;
import edu.aiplatform.shared.error.ApiException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;

class SettingsServiceTest {

    static final SettingDefinition MAX_MB = new SettingDefinition("files.material.maxSizeMb", SettingGroup.FILES,
            "Dung lượng", "d", SettingType.INTEGER, 50, Map.of(SettingValidator.MIN, 1, SettingValidator.MAX, 50));
    static final SettingDefinition DAILY_AT = new SettingDefinition("backup.dailyAt", SettingGroup.BACKUP, "Giờ", "d",
            SettingType.TIME_OF_DAY, null, Map.of());

    final SystemSettingRepository repository = mock(SystemSettingRepository.class);
    final IAuthorization authorization = mock(IAuthorization.class);
    final IAudit audit = mock(IAudit.class);
    final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();
    final MutableClock clock = new MutableClock(Instant.parse("2026-10-16T08:00:00Z"));
    final Actor admin = new Actor(UUID.randomUUID(), Role.ADMIN);
    SettingsService service;

    @BeforeEach
    void setUp() {
        when(authorization.authorize(any(), any(), any())).thenReturn(AuthorizationDecision.allow());
        SettingDefinitionRegistry registry = new SettingDefinitionRegistry(List.of(MAX_MB, DAILY_AT), repository,
                objectMapper, clock);
        service = new SettingsService(repository, registry, new SettingsSectionRegistry(List.of()), authorization,
                mock(IAccountLookup.class), audit, objectMapper, clock, Duration.ofSeconds(30));
    }

    static SettingRow row(String key, String json, long version) {
        return new SettingRow(key, "FILES", json, version, null, Instant.parse("2026-10-01T00:00:00Z"));
    }

    @Test
    void onlyAdminsSeeSettingsAndDenialsAreAudited() {
        Actor teacher = new Actor(UUID.randomUUID(), Role.TEACHER);

        assertThatThrownBy(() -> service.overview(teacher)).isInstanceOf(ApiException.class)
                .extracting("status").isEqualTo(HttpStatus.FORBIDDEN);
        ArgumentCaptor<AuditEvent> event = ArgumentCaptor.forClass(AuditEvent.class);
        verify(audit).record(event.capture());
        assertThat(event.getValue().result()).isEqualTo(AuditResult.DENIED);
    }

    @Test
    void deniedWhenAuthorizationFailsEvenForAdminRole() {
        when(authorization.authorize(any(), any(), any())).thenReturn(AuthorizationDecision.deny("disabled"));

        assertThatThrownBy(() -> service.detail(admin, "files.material.maxSizeMb")).isInstanceOf(ApiException.class)
                .extracting("status").isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void overviewGroupsItemsAndShowsUnsetValuesAsEmpty() {
        when(repository.findAll()).thenReturn(List.of(row("files.material.maxSizeMb", "20", 3),
                row("backup.dailyAt", null, 1)));

        SettingsService.SettingsOverview overview = service.overview(admin);

        assertThat(overview.groups()).extracting(SettingsService.GroupView::group)
                .containsExactly(SettingGroup.FILES, SettingGroup.BACKUP);
        SettingsService.SettingItemView size = overview.groups().get(0).items().get(0);
        assertThat(size.value().asInt()).isEqualTo(20);
        assertThat(size.version()).isEqualTo(3);
        assertThat(overview.groups().get(1).items().get(0).value()).isNull();
    }

    @Test
    void updateWithStaleVersionIsAConflict() {
        when(repository.findAll()).thenReturn(List.of(row("files.material.maxSizeMb", "50", 4)));
        when(repository.update(eq("files.material.maxSizeMb"), eq("20"), eq(3L), any(), any())).thenReturn(0);

        assertThatThrownBy(() -> service.update(admin, "files.material.maxSizeMb", objectMapper.valueToTree(20), 3))
                .isInstanceOf(ApiException.class).extracting("status").isEqualTo(HttpStatus.CONFLICT);
        verify(audit, never()).record(any());
    }

    @Test
    void invalidValueIsRejectedWithoutWriting() {
        assertThatThrownBy(() -> service.update(admin, "files.material.maxSizeMb", objectMapper.valueToTree(80), 1))
                .isInstanceOf(ApiException.class).extracting("status").isEqualTo(HttpStatus.BAD_REQUEST);
        verify(repository, never()).update(anyString(), any(), anyLong(), any(), any());
    }

    @Test
    void unknownKeyIsNotFound() {
        assertThatThrownBy(() -> service.update(admin, "khong.co", objectMapper.valueToTree(1), 1))
                .isInstanceOf(ApiException.class).extracting("status").isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void successfulUpdateIsAuditedAndVisibleImmediatelyOnThisInstance() {
        SettingKey<Integer> key = new SettingKey<>("files.material.maxSizeMb", Integer.class);
        when(repository.find("files.material.maxSizeMb")).thenReturn(Optional.of(row("files.material.maxSizeMb", "50", 4)));
        assertThat(service.get(key)).contains(50);

        when(repository.findAll()).thenReturn(List.of(row("files.material.maxSizeMb", "50", 4)));
        when(repository.update(eq("files.material.maxSizeMb"), eq("20"), eq(4L), eq(admin.accountId()), any()))
                .thenReturn(1);
        when(repository.find("files.material.maxSizeMb")).thenReturn(Optional.of(row("files.material.maxSizeMb", "20", 5)));

        SettingsService.SettingItemView updated = service.update(admin, "files.material.maxSizeMb",
                objectMapper.valueToTree(20), 4);

        assertThat(updated.version()).isEqualTo(5);
        ArgumentCaptor<AuditEvent> event = ArgumentCaptor.forClass(AuditEvent.class);
        verify(audit).record(event.capture());
        assertThat(event.getValue().action()).isEqualTo("SETTING_UPDATED");
        assertThat(event.getValue().details()).containsEntry("before", "50").containsEntry("after", "20");
        assertThat(service.get(key)).contains(20);
    }

    @Test
    void valuesAreCachedForThirtySeconds() {
        SettingKey<Integer> key = new SettingKey<>("files.material.maxSizeMb", Integer.class);
        when(repository.find("files.material.maxSizeMb")).thenReturn(Optional.of(row("files.material.maxSizeMb", "50", 1)));

        service.get(key);
        clock.advance(Duration.ofSeconds(29));
        service.get(key);
        verify(repository, times(1)).find("files.material.maxSizeMb");

        clock.advance(Duration.ofSeconds(2));
        service.get(key);
        verify(repository, times(2)).find("files.material.maxSizeMb");
    }

    @Test
    void staleCacheIsUsedWhenTheDatabaseFailsAndNoCacheMeansUnavailable() {
        SettingKey<LocalTime> time = new SettingKey<>("backup.dailyAt", LocalTime.class);
        when(repository.find("backup.dailyAt")).thenReturn(Optional.of(row("backup.dailyAt", "\"02:30\"", 2)));
        assertThat(service.get(time)).contains(LocalTime.of(2, 30));

        clock.advance(Duration.ofMinutes(5));
        when(repository.find("backup.dailyAt")).thenThrow(new DataAccessResourceFailureException("db down"));
        assertThat(service.get(time)).contains(LocalTime.of(2, 30));

        when(repository.find("files.material.maxSizeMb")).thenThrow(new DataAccessResourceFailureException("db down"));
        assertThatThrownBy(() -> service.get(new SettingKey<>("files.material.maxSizeMb", Integer.class)))
                .isInstanceOf(ApiException.class).extracting("status").isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void startupInsertsMissingItemsWithDefaultsWithoutTouchingExistingOnes() {
        SettingDefinitionRegistry registry = new SettingDefinitionRegistry(List.of(MAX_MB, DAILY_AT), repository,
                objectMapper, clock);

        registry.initialize();

        verify(repository).insertIfAbsent(eq("files.material.maxSizeMb"), eq("FILES"), eq("50"), any());
        verify(repository).insertIfAbsent(eq("backup.dailyAt"), eq("BACKUP"), eq(null), any());
        verify(repository, never()).update(anyString(), any(), anyLong(), any(), any());
    }

    @Test
    void duplicateKeysAreRejected() {
        assertThatThrownBy(() -> new SettingDefinitionRegistry(List.of(MAX_MB, MAX_MB), repository, objectMapper, clock))
                .isInstanceOf(IllegalStateException.class);
    }

    static class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
