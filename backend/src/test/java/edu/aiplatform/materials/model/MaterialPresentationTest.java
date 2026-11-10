package edu.aiplatform.materials.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MaterialPresentationTest {
    @Test
    void oldMaterialsDefaultToEnabledAndPlainDescriptionsRemainAvailable() {
        Instant now = Instant.parse("2026-11-09T00:00:00Z");
        Material material = new Material(UUID.randomUUID(), Material.Scope.SUBJECT, UUID.randomUUID(), null,
                UUID.randomUUID(), Material.Kind.FILE, "Tài liệu", "Mô tả cũ", 0, Material.Status.ACTIVE,
                1, UUID.randomUUID(), now, now, 0);
        var node = new ObjectMapper().findAndRegisterModules().valueToTree(material);
        assertThat(node.path("enabled").asBoolean()).isTrue();
        assertThat(node.path("description").asText()).isEqualTo("Mô tả cũ");
    }
}
