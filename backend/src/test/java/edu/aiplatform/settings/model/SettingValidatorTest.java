package edu.aiplatform.settings.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.settings.contract.SettingDefinition;
import edu.aiplatform.settings.contract.SettingGroup;
import edu.aiplatform.settings.contract.SettingType;
import edu.aiplatform.shared.error.ApiException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SettingValidatorTest {

    static final ObjectMapper JSON = new ObjectMapper();

    static final SettingDefinition MAX_MB = new SettingDefinition("files.material.maxSizeMb", SettingGroup.FILES, "n", "d",
            SettingType.INTEGER, 50, Map.of(SettingValidator.MIN, 1, SettingValidator.MAX, 50));
    static final SettingDefinition TYPES = new SettingDefinition("files.material.allowedTypes", SettingGroup.FILES, "n",
            "d", SettingType.ENUM_SET, List.of("PDF", "DOCX", "PPTX"),
            Map.of(SettingValidator.OPTIONS, List.of("PDF", "DOCX", "PPTX"), SettingValidator.NON_EMPTY, true));
    static final SettingDefinition ENABLED = new SettingDefinition("backup.enabled", SettingGroup.BACKUP, "n", "d",
            SettingType.BOOLEAN, false, Map.of(SettingValidator.REQUIRES_WHEN_TRUE, "backup.dailyAt"));
    static final SettingDefinition DAILY_AT = new SettingDefinition("backup.dailyAt", SettingGroup.BACKUP, "n", "d",
            SettingType.TIME_OF_DAY, null, Map.of());

    static JsonNode json(String text) throws Exception {
        return JSON.readTree(text);
    }

    @Test
    void integerMustBeWithinRange() throws Exception {
        assertThat(SettingValidator.validate(MAX_MB, json("20")).asInt()).isEqualTo(20);
        assertThatThrownBy(() -> SettingValidator.validate(MAX_MB, json("51"))).isInstanceOf(ApiException.class)
                .hasMessageContaining("1 đến 50");
        assertThatThrownBy(() -> SettingValidator.validate(MAX_MB, json("0"))).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> SettingValidator.validate(MAX_MB, json("\"20\""))).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> SettingValidator.validate(MAX_MB, json("2.5"))).isInstanceOf(ApiException.class);
    }

    @Test
    void itemWithDefaultCannotBeCleared() {
        assertThatThrownBy(() -> SettingValidator.validate(MAX_MB, null)).isInstanceOf(ApiException.class);
        assertThat(SettingValidator.validate(DAILY_AT, null).isNull()).isTrue();
    }

    @Test
    void enumSetIsNormalizedAndCannotBeEmpty() throws Exception {
        assertThat(SettingValidator.validate(TYPES, json("[\"PPTX\",\"PDF\",\"PDF\"]")).toString())
                .isEqualTo("[\"PDF\",\"PPTX\"]");
        assertThatThrownBy(() -> SettingValidator.validate(TYPES, json("[]"))).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> SettingValidator.validate(TYPES, json("[\"EXE\"]"))).isInstanceOf(ApiException.class);
    }

    @Test
    void timeOfDayAndBooleanFormats() throws Exception {
        assertThat(SettingValidator.validate(DAILY_AT, json("\"02:30\"")).asText()).isEqualTo("02:30");
        assertThatThrownBy(() -> SettingValidator.validate(DAILY_AT, json("\"24:00\""))).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> SettingValidator.validate(DAILY_AT, json("\"2:30\""))).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> SettingValidator.validate(ENABLED, json("\"true\""))).isInstanceOf(ApiException.class);
    }

    @Test
    void backupCanBeEnabledOnlyAfterTheTimeIsSet() throws Exception {
        List<SettingDefinition> all = List.of(ENABLED, DAILY_AT);
        Map<String, JsonNode> noTime = Map.of("backup.enabled", json("false"));
        assertThatThrownBy(() -> SettingValidator.validateCross(ENABLED, json("true"), all, noTime))
                .isInstanceOf(ApiException.class).extracting("code").isEqualTo("SETTING_DEPENDENCY");

        Map<String, JsonNode> withTime = Map.of("backup.enabled", json("false"), "backup.dailyAt", json("\"02:30\""));
        SettingValidator.validateCross(ENABLED, json("true"), all, withTime);
    }

    @Test
    void timeCannotBeClearedWhileBackupIsEnabled() throws Exception {
        List<SettingDefinition> all = List.of(ENABLED, DAILY_AT);
        Map<String, JsonNode> enabled = Map.of("backup.enabled", json("true"), "backup.dailyAt", json("\"02:30\""));

        assertThatThrownBy(() -> SettingValidator.validateCross(DAILY_AT, null, all, enabled))
                .isInstanceOf(ApiException.class);
    }
}
