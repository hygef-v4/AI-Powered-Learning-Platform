package edu.aiplatform.backup.contract.stub;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;
import edu.aiplatform.backup.contract.IBackupDashboardStatus;
import edu.aiplatform.shared.error.ApiException;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.domain.PageRequest;

class BackupDashboardUnavailableTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(BackupContractStubs.class));

    @Test void fallbackFailsClosedForEveryRead() {
        runner.run(context -> {
            var port = context.getBean(IBackupDashboardStatus.class);
            assertThatThrownBy(port::latestRun).isInstanceOf(ApiException.class);
            assertThatThrownBy(() -> port.history(PageRequest.of(0, 3))).isInstanceOf(ApiException.class);
            assertThatThrownBy(() -> port.summarize(Instant.EPOCH, Instant.now()))
                    .isInstanceOfSatisfying(ApiException.class,
                            e -> assertThat(e.code()).isEqualTo("BACKUP_STATUS_UNAVAILABLE"));
        });
    }

    @Test void realProviderReplacesFallback() {
        var provider = mock(IBackupDashboardStatus.class);
        runner.withBean(IBackupDashboardStatus.class, () -> provider).run(context -> {
            assertThat(context).hasSingleBean(IBackupDashboardStatus.class);
            assertThat(context.getBean(IBackupDashboardStatus.class)).isSameAs(provider);
        });
    }

    @Test void disabledStubsDoNotRegisterCapability() {
        runner.withPropertyValues("platform.contract-stubs.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(IBackupDashboardStatus.class));
    }
}
