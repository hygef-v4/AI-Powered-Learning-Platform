package edu.aiplatform.ai.application;

import edu.aiplatform.ai.contract.IMcpToolProvider;
import edu.aiplatform.ai.contract.McpToolDefinition;
import edu.aiplatform.ai.contract.McpToolKind;
import edu.aiplatform.ai.domain.McpCredential.Access;
import edu.aiplatform.identity.contract.Role;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Gom tool của các unit đăng ký qua {@link IMcpToolProvider} (BR-U06-34). Tên tool là duy nhất; trùng thì báo lỗi
 * ngay lần đầu dùng, không chọn ngẫu nhiên một bên.
 */
@Component
public class McpToolRegistry {

    private final ObjectProvider<IMcpToolProvider> providers;
    private volatile Map<String, Entry> entries;

    public McpToolRegistry(ObjectProvider<IMcpToolProvider> providers) {
        this.providers = providers;
    }

    /** Tool người gọi thấy: role được dùng; credential {@code READ_ONLY} chỉ thấy tool đọc (F6 bước 4). */
    public List<McpToolDefinition> visible(Role role, Access access) {
        return entries().values().stream().map(Entry::definition).filter(d -> visible(d, role, access)).toList();
    }

    public Optional<Entry> find(String name, Role role, Access access) {
        return Optional.ofNullable(name == null ? null : entries().get(name))
                .filter(e -> visible(e.definition(), role, access));
    }

    static boolean visible(McpToolDefinition d, Role role, Access access) {
        return d.allowedRoles() != null && d.allowedRoles().contains(role)
                && (d.kind() == McpToolKind.READ || access == Access.READ_WRITE);
    }

    private Map<String, Entry> entries() {
        Map<String, Entry> current = entries;
        if (current == null) {
            synchronized (this) {
                if (entries == null) {
                    Map<String, Entry> built = new LinkedHashMap<>();
                    providers.orderedStream().forEach(p -> p.tools().forEach(d -> {
                        if (built.putIfAbsent(d.name(), new Entry(d, p)) != null) {
                            throw new IllegalStateException("Tool MCP trùng tên: " + d.name());
                        }
                    }));
                    entries = Collections.unmodifiableMap(built);
                }
                current = entries;
            }
        }
        return current;
    }

    public record Entry(McpToolDefinition definition, IMcpToolProvider provider) {
    }
}
