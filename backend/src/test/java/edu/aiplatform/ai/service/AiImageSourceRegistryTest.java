package edu.aiplatform.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import edu.aiplatform.ai.contract.AiImage;
import edu.aiplatform.ai.contract.IAiImageSource;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AiImageSourceRegistryTest {

    private static IAiImageSource source(String name) {
        return new IAiImageSource() {
            @Override
            public String source() {
                return name;
            }

            @Override
            public Optional<AiImage> load(String ref) {
                return Optional.of(new AiImage("image/jpeg", new byte[]{1}));
            }
        };
    }

    @Test
    void findsRegisteredSourceByName() {
        AiImageSourceRegistry registry = AiImageSourceRegistry.createRegistry(List.of(source("ASSIGNMENT_IMPORT")));

        assertThat(registry.findSource("ASSIGNMENT_IMPORT")).isPresent();
        assertThat(registry.findSource("OTHER")).isEmpty();
    }

    @Test
    void duplicateSourceNamesAreRejected() {
        AiImageSourceRegistry registry = AiImageSourceRegistry.createRegistry(List.of(source("A"), source("A")));

        assertThatThrownBy(() -> registry.findSource("A")).isInstanceOf(IllegalStateException.class);
    }
}
