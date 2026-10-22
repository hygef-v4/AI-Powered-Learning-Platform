package edu.aiplatform.backup.contract.stub;

import edu.aiplatform.backup.contract.BackupRunView;
import edu.aiplatform.backup.contract.BackupDashboardRunView;
import edu.aiplatform.backup.contract.BackupDashboardSummary;
import edu.aiplatform.backup.contract.IBackupDashboardStatus;
import edu.aiplatform.shared.error.ApiException;
import java.time.Instant;
import edu.aiplatform.backup.contract.IBackupStatus;
import edu.aiplatform.shared.stub.Stubs;
import java.util.Optional;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** Bản tạm của {@code IBackupStatus} (U03): chưa có lần backup nào. */
@AutoConfiguration
@ConditionalOnProperty(name = "platform.contract-stubs.enabled", havingValue = "true", matchIfMissing = true)
public class BackupContractStubs {

    @Bean
    @ConditionalOnMissingBean
    IBackupDashboardStatus backupDashboardStatusStub() {
        return Stubs.announce(IBackupDashboardStatus.class,
                new IBackupDashboardStatus() {
            private RuntimeException unavailable() {
                return ApiException.unavailable(
                        "BACKUP_STATUS_UNAVAILABLE", "Không đọc được trạng thái backup");
            }
            @Override public Optional<BackupDashboardRunView> latestRun() {
                throw unavailable();
            }
            @Override public Page<BackupDashboardRunView> history(Pageable page) {
                throw unavailable();
            }
            @Override public BackupDashboardSummary summarize(
                    Instant fromInclusive, Instant toExclusive) {
                throw unavailable();
            }
        });
    }

    @Bean
    @ConditionalOnMissingBean
    IBackupStatus backupStatusStub() {
        return Stubs.announce(IBackupStatus.class, new IBackupStatus() {
            @Override
            public Optional<BackupRunView> latest() {
                return Optional.empty();
            }

            @Override
            public Page<BackupRunView> list(Pageable page) {
                return Page.empty(page);
            }
        });
    }
}
