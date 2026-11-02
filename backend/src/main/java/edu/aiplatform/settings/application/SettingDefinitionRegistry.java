package edu.aiplatform.settings.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.settings.contract.SettingDefinition;
import edu.aiplatform.settings.domain.SystemSettingRepository;
import java.time.Clock;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Gom các {@link SettingDefinition} do unit khai báo và khởi tạo mục còn thiếu khi khởi động (S4). */
@Component
public class SettingDefinitionRegistry {

    private final Map<String, SettingDefinition> definitions = new LinkedHashMap<>();
    private final SystemSettingRepository repository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public SettingDefinitionRegistry(List<SettingDefinition> definitions, SystemSettingRepository repository,
                                     ObjectMapper objectMapper, Clock clock) {
        for (SettingDefinition definition : definitions) {
            if (this.definitions.putIfAbsent(definition.key(), definition) != null) {
                throw new IllegalStateException("Khóa cài đặt khai báo hai lần: " + definition.key());
            }
        }
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public Optional<SettingDefinition> find(String key) {
        return Optional.ofNullable(definitions.get(key));
    }

    public Collection<SettingDefinition> all() {
        return definitions.values();
    }

    /** Mục chưa có thì thêm với giá trị mặc định (hoặc rỗng), mục đã có giữ nguyên (BR-U03-84). */
    @EventListener(ApplicationReadyEvent.class)
    public void initialize() {
        for (SettingDefinition definition : definitions.values()) {
            repository.insertIfAbsent(definition.key(), definition.group().name(), json(definition.defaultValue()),
                    clock.instant());
        }
    }

    private String json(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}
