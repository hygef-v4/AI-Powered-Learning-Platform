package edu.aiplatform.backup.application;

import static org.assertj.core.api.Assertions.*;
import edu.aiplatform.backup.contract.*;
import java.lang.reflect.Method;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class BackupDashboardContractTest {
    @Test void preservesLegacyContractAndExcludesPrivateMetadata() {
        assertThat(IBackupStatus.class.getDeclaredMethods()).extracting(Method::getName)
                .containsExactlyInAnyOrder("latest", "list");
        assertThat(new BackupRunView(null, null, null, null, null, null, null, null)).isNotNull();
        assertThat(Arrays.stream(BackupDashboardRunView.class.getRecordComponents()).map(c -> c.getName()))
                .contains("startedAt").doesNotContain("id", "archiveRef", "archiveSha256");
        assertThat(IBackupDashboardStatus.class.getDeclaredMethods()).extracting(Method::getName)
                .containsExactlyInAnyOrder("latestRun", "history", "summarize");
    }
}
