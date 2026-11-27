package edu.aiplatform.backup.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.settings.contract.SettingDefinition;
import edu.aiplatform.settings.model.SettingValidator;
import edu.aiplatform.shared.error.ApiException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** BR-U03-88, KAN-255: sao lưu chỉ bật được khi đã đặt giờ chạy, theo đúng khai báo của nhóm Backup. */
class BackupSettingDefinitionsTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final BackupSettingDefinitions backupSettingDefinitions = new BackupSettingDefinitions();
    private final SettingDefinition enabled = backupSettingDefinitions.backupEnabled();
    private final SettingDefinition dailyAt = backupSettingDefinitions.backupDailyAt();
    private final List<SettingDefinition> all = List.of(enabled, dailyAt);

    @Test
    void backupCannotBeTurnedOnWithoutADailyRunTime() throws Exception {
        Map<String, JsonNode> noTime = Map.of("backup.enabled", JSON.readTree("false"));

        assertThatThrownBy(() -> SettingValidator.validateCross(enabled, JSON.readTree("true"), all, noTime))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("SETTING_DEPENDENCY");
    }

    @Test
    void backupCanBeTurnedOnOnceTheTimeIsSetAndOffAtAnyTime() throws Exception {
        Map<String, JsonNode> withTime = Map.of("backup.enabled", JSON.readTree("false"),
                "backup.dailyAt", JSON.readTree("\"02:30\""));

        SettingValidator.validateCross(enabled, JSON.readTree("true"), all, withTime);
        SettingValidator.validateCross(enabled, JSON.readTree("false"), all, Map.of());
    }

    @Test
    void dailyRunTimeCannotBeClearedWhileBackupIsOn() throws Exception {
        Map<String, JsonNode> on = Map.of("backup.enabled", JSON.readTree("true"),
                "backup.dailyAt", JSON.readTree("\"02:30\""));

        assertThatThrownBy(() -> SettingValidator.validateCross(dailyAt, null, all, on))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("SETTING_DEPENDENCY");
        assertThat(enabled.constraints()).containsEntry(SettingValidator.REQUIRES_WHEN_TRUE, "backup.dailyAt");
    }
}
