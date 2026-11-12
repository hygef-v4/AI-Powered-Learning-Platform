package edu.aiplatform.ai.service;

import edu.aiplatform.ai.contract.IAiImageSource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Nguồn ảnh theo {@code source} do unit gọi cài; lấy lười như {@link AiTaskResultHandlerRegistry} để không tạo vòng phụ
 * thuộc lúc khởi động (BR-U06-55).
 */
@Component
public class AiImageSourceRegistry {

    private final Supplier<List<IAiImageSource>> sourceSupplier;
    private volatile Map<String, IAiImageSource> sources;

    @Autowired
    public AiImageSourceRegistry(ObjectProvider<IAiImageSource> sources) {
        this(() -> sources.orderedStream().toList());
    }

    AiImageSourceRegistry(Supplier<List<IAiImageSource>> sourceSupplier) {
        this.sourceSupplier = sourceSupplier;
    }

    /** Dùng trong test với danh sách cố định. */
    static AiImageSourceRegistry of(List<IAiImageSource> sources) {
        return new AiImageSourceRegistry(() -> sources);
    }

    public Optional<IAiImageSource> find(String source) {
        return Optional.ofNullable(loadSources().get(source));
    }

    private Map<String, IAiImageSource> loadSources() {
        Map<String, IAiImageSource> current = sources;
        if (current == null) {
            synchronized (this) {
                if (sources == null) {
                    Map<String, IAiImageSource> built = new HashMap<>();
                    for (IAiImageSource source : sourceSupplier.get()) {
                        if (built.putIfAbsent(source.source(), source) != null) {
                            throw new IllegalStateException("Nguồn ảnh AI bị trùng: " + source.source());
                        }
                    }
                    sources = Map.copyOf(built);
                }
                current = sources;
            }
        }
        return current;
    }
}
