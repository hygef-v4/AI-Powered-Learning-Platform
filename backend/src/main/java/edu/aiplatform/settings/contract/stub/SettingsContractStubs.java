package edu.aiplatform.settings.contract.stub;

import edu.aiplatform.settings.contract.ISettings;
import edu.aiplatform.settings.contract.SettingKey;
import edu.aiplatform.shared.stub.Stubs;
import java.util.Optional;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/** Bản tạm của {@code ISettings} (U03): mọi mục là "chưa cấu hình". */
@AutoConfiguration
@ConditionalOnProperty(name = "platform.contract-stubs.enabled", havingValue = "true", matchIfMissing = true)
public class SettingsContractStubs {

    @Bean
    @ConditionalOnMissingBean
    ISettings settingsStub() {
        return Stubs.announce(ISettings.class, new ISettings() {
            @Override
            public <T> Optional<T> get(SettingKey<T> key) {
                return Optional.empty();
            }
        });
    }
}
