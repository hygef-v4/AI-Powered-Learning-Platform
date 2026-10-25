package edu.aiplatform.quizzes.application;

import edu.aiplatform.settings.contract.ISettings;
import edu.aiplatform.settings.contract.SettingKey;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** U03 giả: giá trị theo khóa; {@code down} giả lập U03 không trả lời. */
class FakeSettings implements ISettings {

    final Map<String, Object> values = new HashMap<>();
    boolean down;

    @Override
    public <T> Optional<T> get(SettingKey<T> key) {
        if (down) {
            throw new IllegalStateException("U03 down");
        }
        return Optional.ofNullable(values.get(key.key())).map(key.valueType()::cast);
    }
}
