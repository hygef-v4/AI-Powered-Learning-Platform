package edu.aiplatform.materials.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import edu.aiplatform.jobs.contract.*;
import edu.aiplatform.materials.model.*;
import edu.aiplatform.materials.repository.*;
import edu.aiplatform.materials.integration.MaterialTextExtractor;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;

class MaterialTextExtractJobHandlerTest {
    final UUID id = UUID.randomUUID(), subject = UUID.randomUUID(), actor = UUID.randomUUID();
    final Instant now = Instant.now();
    IMaterialRepository materialRepository;
    MaterialTextExtractor materialTextExtractor;
    MaterialTextExtractJobHandler materialTextExtractJobHandler;
    Material material;
    MaterialVersion version;

    @BeforeEach
    void setup() {
        materialRepository = mock(IMaterialRepository.class);
        materialTextExtractor = mock(MaterialTextExtractor.class);
        materialTextExtractJobHandler =
                new MaterialTextExtractJobHandler(materialRepository, materialTextExtractor);
        material =
                new Material(
                        id,
                        Material.Scope.SUBJECT,
                        subject,
                        null,
                        new UUID(0, 1),
                        Material.Kind.FILE,
                        "Title",
                        "",
                        0,
                        Material.Status.ACTIVE,
                        1,
                        actor,
                        now,
                        now,
                        0);
        version =
                new MaterialVersion(
                        id,
                        1,
                        "file",
                        "d.pdf",
                        "application/pdf",
                        100L,
                        null,
                        null,
                        MaterialVersion.TextStatus.PENDING,
                        null,
                        actor,
                        now);
        when(materialRepository.findMaterial(id)).thenReturn(Optional.of(material));
        when(materialRepository.findVersion(id, 1)).thenReturn(Optional.of(version));
    }

    JobMessage message() {
        return new JobMessage(
                1,
                JobTypes.MATERIAL_TEXT_EXTRACT,
                "key",
                Map.of("materialId", id.toString(), "versionNo", 1),
                0,
                "correlation");
    }

    @Test
    void textDensityBoundaryRequiresFiftyCharactersPerPage() {
        when(materialTextExtractor.extractText("file"))
                .thenReturn(new MaterialTextExtractor.Result("a".repeat(98), 2));
        materialTextExtractJobHandler.handle(message());
        verify(materialRepository).completeText(id, 1, MaterialVersion.TextStatus.NO_TEXT, null);
        clearInvocations(materialRepository);
        when(materialTextExtractor.extractText("file"))
                .thenReturn(new MaterialTextExtractor.Result("a".repeat(100), 2));
        materialTextExtractJobHandler.handle(message());
        verify(materialRepository)
                .completeText(id, 1, MaterialVersion.TextStatus.EXTRACTED, "a".repeat(100));
    }

    @Test
    void extractedTextIsCappedAtOneMillionCharacters() {
        when(materialTextExtractor.extractText("file"))
                .thenReturn(new MaterialTextExtractor.Result("a".repeat(1_000_001), 1));
        materialTextExtractJobHandler.handle(message());
        verify(materialRepository)
                .completeText(id, 1, MaterialVersion.TextStatus.EXTRACTED, "a".repeat(1_000_000));
    }
}
