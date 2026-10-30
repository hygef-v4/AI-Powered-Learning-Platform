package edu.aiplatform.settings.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.audit.contract.AuditEvent;
import edu.aiplatform.audit.contract.AuditResult;
import edu.aiplatform.audit.contract.IAudit;
import edu.aiplatform.identity.contract.AccountContact;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.IAccountLookup;
import edu.aiplatform.identity.contract.IAuthorization;
import edu.aiplatform.identity.contract.ResourceRef;
import edu.aiplatform.identity.contract.Role;
import edu.aiplatform.settings.contract.ISettings;
import edu.aiplatform.settings.contract.SettingDefinition;
import edu.aiplatform.settings.contract.SettingGroup;
import edu.aiplatform.settings.contract.SettingKey;
import edu.aiplatform.settings.contract.SettingType;
import edu.aiplatform.settings.model.SettingRow;
import edu.aiplatform.settings.model.SettingValidator;
import edu.aiplatform.settings.repository.SystemSettingRepository;
import edu.aiplatform.shared.error.ApiException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cài {@link ISettings} (S3, cache 30 giây) và nghiệp vụ UC 14, 15 (S1, S2): chỉ {@code ADMIN}, sửa có
 * {@code version}, ghi audit {@code SETTING_UPDATED} trong cùng transaction.
 */
@Service
public class SettingsService implements ISettings {

    private final SystemSettingRepository repository;
    private final SettingDefinitionRegistry definitions;
    private final SettingsSectionRegistry sections;
    private final IAuthorization authorization;
    private final IAccountLookup accounts;
    private final IAudit audit;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final Duration cacheTtl;
    private final Map<String, Cached> cache = new ConcurrentHashMap<>();

    public SettingsService(SystemSettingRepository repository, SettingDefinitionRegistry definitions,
                           SettingsSectionRegistry sections, IAuthorization authorization, IAccountLookup accounts,
                           IAudit audit, ObjectMapper objectMapper, Clock clock,
                           @Value("${platform.settings.cache-ttl:30s}") Duration cacheTtl) {
        this.repository = repository;
        this.definitions = definitions;
        this.sections = sections;
        this.authorization = authorization;
        this.accounts = accounts;
        this.audit = audit;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.cacheTtl = cacheTtl;
    }

    record Cached(JsonNode value, Instant expiresAt) {
    }

    public record SettingItemView(String key, SettingGroup group, String name, String description, SettingType type,
                                  JsonNode value, Object defaultValue, Map<String, Object> constraints, long version,
                                  String updatedBy, Instant updatedAt) {
    }

    public record GroupView(SettingGroup group, List<SettingItemView> items) {
    }

    public record SettingsOverview(List<GroupView> groups, List<SettingsSectionRegistry.SectionView> sections) {
    }

    // ---------- ISettings (S3) ----------

    @Override
    public <T> Optional<T> get(SettingKey<T> key) {
        JsonNode value = currentValue(key.key());
        if (value == null || value.isNull()) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(objectMapper.treeToValue(value, key.valueType()));
        } catch (JsonProcessingException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    JsonNode currentValue(String key) {
        Instant now = clock.instant();
        Cached cached = cache.get(key);
        if (cached != null && now.isBefore(cached.expiresAt())) {
            return cached.value();
        }
        try {
            JsonNode value = repository.find(key).map(row -> parse(row.valueJson())).orElse(null);
            cache.put(key, new Cached(value, now.plus(cacheTtl)));
            return value;
        } catch (DataAccessException e) {
            if (cached != null) {
                return cached.value();
            }
            throw ApiException.unavailable("SETTINGS_UNAVAILABLE", "Không đọc được cài đặt, thử lại sau");
        }
    }

    // ---------- UC 14 (S1) ----------

    public SettingsOverview overview(Actor actor) {
        requireAdmin(actor, "SETTINGS_VIEW");
        Map<String, SettingRow> rows = rowsByKey();
        Map<SettingGroup, List<SettingItemView>> grouped = new LinkedHashMap<>();
        for (SettingGroup group : SettingGroup.values()) {
            grouped.put(group, new ArrayList<>());
        }
        for (SettingDefinition definition : definitions.all()) {
            grouped.get(definition.group()).add(view(definition, rows.get(definition.key())));
        }
        List<GroupView> groups = grouped.entrySet().stream().filter(entry -> !entry.getValue().isEmpty())
                .map(entry -> new GroupView(entry.getKey(), entry.getValue())).toList();
        return new SettingsOverview(groups, sections.summaries());
    }

    public SettingItemView detail(Actor actor, String key) {
        requireAdmin(actor, "SETTINGS_VIEW");
        SettingDefinition definition = definition(key);
        return view(definition, repository.find(key).orElse(null));
    }

    // ---------- UC 15 (S2) ----------

    @Transactional
    public SettingItemView update(Actor actor, String key, JsonNode value, long version) {
        requireAdmin(actor, "SETTINGS_EDIT");
        SettingDefinition definition = definition(key);
        JsonNode normalized = SettingValidator.validate(definition, value);
        Map<String, SettingRow> rows = rowsByKey();
        Map<String, JsonNode> current = new HashMap<>();
        rows.forEach((rowKey, row) -> current.put(rowKey, parse(row.valueJson())));
        SettingValidator.validateCross(definition, normalized, definitions.all(), current);
        SettingRow before = rows.get(key);
        Instant now = clock.instant();
        String json = normalized.isNull() ? null : normalized.toString();
        // Mục chưa có dòng hiện version 0: lưu với version 0 thì tạo dòng thay vì báo xung đột.
        int written = before != null ? repository.update(key, json, version, actor.accountId(), now)
                : version == 0 ? repository.insert(key, definition.group().name(), json, actor.accountId(), now) : 0;
        if (written == 0) {
            throw ApiException.conflict("SETTING_VERSION_CONFLICT", "Cài đặt đã được người khác sửa, tải lại để xem");
        }
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("key", key);
        details.put("before", before == null ? null : before.valueJson());
        details.put("after", json);
        audit.record(new AuditEvent(UUID.randomUUID(), actor.accountId(), "SETTING_UPDATED", "SETTING", key,
                AuditResult.SUCCESS, null, details, now));
        cache.remove(key);
        return view(definition, repository.find(key).orElseThrow());
    }

    // ---------- helpers ----------

    private void requireAdmin(Actor actor, String action) {
        boolean allowed = actor != null && actor.role() == Role.ADMIN
                && authorization.authorize(actor, action, ResourceRef.none()).allowed();
        if (!allowed) {
            audit.record(new AuditEvent(UUID.randomUUID(), actor == null ? null : actor.accountId(), "ACCESS_DENIED",
                    "SETTINGS", null, AuditResult.DENIED, action, Map.of(), clock.instant()));
            throw ApiException.forbidden("SETTINGS_FORBIDDEN");
        }
    }

    private SettingDefinition definition(String key) {
        return definitions.find(key).orElseThrow(() -> ApiException.notFound("SETTING_NOT_FOUND"));
    }

    private Map<String, SettingRow> rowsByKey() {
        return repository.findAll().stream().collect(Collectors.toMap(SettingRow::key, Function.identity()));
    }

    private SettingItemView view(SettingDefinition definition, SettingRow row) {
        String updatedBy = row == null || row.updatedBy() == null ? null
                : accounts.getContact(row.updatedBy()).map(AccountContact::displayName).orElse(null);
        return new SettingItemView(definition.key(), definition.group(), definition.name(), definition.description(),
                definition.type(), row == null ? null : parse(row.valueJson()), definition.defaultValue(),
                definition.constraints(), row == null ? 0 : row.version(), updatedBy,
                row == null ? null : row.updatedAt());
    }

    private JsonNode parse(String json) {
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
