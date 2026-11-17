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
    void extractsTextIntoExactVersion() {
        when(materialTextExtractor.extractText("file"))
                .thenReturn(new MaterialTextExtractor.Result("a".repeat(100), 1));
        materialTextExtractJobHandler.handle(message());
        verify(materialRepository)
                .completeText(id, 1, MaterialVersion.TextStatus.EXTRACTED, "a".repeat(100));
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

    @Test
    void sparsePagesProduceNoText() {
        when(materialTextExtractor.extractText("file"))
                .thenReturn(new MaterialTextExtractor.Result("a".repeat(100), 3));
        materialTextExtractJobHandler.handle(message());
        verify(materialRepository).completeText(id, 1, MaterialVersion.TextStatus.NO_TEXT, null);
    }

    @Test
    void supersededBeforeStartSkipsStorageAndWrite() {
        when(materialRepository.findMaterial(id))
                .thenReturn(
                        Optional.of(material.reviseMaterial("Title", "", 0, 2, material.status(), now)));
        materialTextExtractJobHandler.handle(message());
        verifyNoInteractions(materialTextExtractor);
        verify(materialRepository, never()).completeText(any(), anyInt(), any(), any());
    }

    @Test
    void replacementDuringParsingCannotOverwriteNewVersion() {
        when(materialTextExtractor.extractText("file"))
                .thenAnswer(
                        call -> {
                            when(materialRepository.findMaterial(id))
                                    .thenReturn(
                                            Optional.of(
                                                    material.reviseMaterial(
                                                            "Title",
                                                            "",
                                                            0,
                                                            2,
                                                            material.status(),
                                                            now)));
                            return new MaterialTextExtractor.Result("a".repeat(100), 1);
                        });
        materialTextExtractJobHandler.handle(message());
        verify(materialRepository)
                .completeText(id, 1, MaterialVersion.TextStatus.EXTRACTED, "a".repeat(100));
        verify(materialRepository, never()).updateMaterial(any(), anyLong());
        verify(materialRepository, never()).completeText(eq(id), eq(2), any(), any());
    }

    @Test
    void completedVersionIsIdempotent() {
        when(materialRepository.findVersion(id, 1))
                .thenReturn(
                        Optional.of(
                                new MaterialVersion(
                                        id,
                                        1,
                                        "file",
                                        "d.pdf",
                                        "application/pdf",
                                        100L,
                                        null,
                                        null,
                                        MaterialVersion.TextStatus.EXTRACTED,
                                        "done",
                                        actor,
                                        now)));
        materialTextExtractJobHandler.handle(message());
        verifyNoInteractions(materialTextExtractor);
    }

    @Test
    void transientParserFailureUsesU03RetryContract() {
        when(materialTextExtractor.extractText("file"))
                .thenThrow(new IllegalStateException("private data"));
        assertThatThrownBy(() -> materialTextExtractJobHandler.handle(message()))
                .isInstanceOf(RetryableJobException.class)
                .hasMessageNotContaining("private data");
        verify(materialRepository, never()).completeText(any(), anyInt(), any(), any());
    }

    @Test
    void exhaustedRetriesMarkOnlyPendingExactVersionFailed() {
        materialTextExtractJobHandler.onFailed(message(), "private");
        verify(materialRepository).completeText(id, 1, MaterialVersion.TextStatus.FAILED, null);
    }

    @Test
    void sweeperReusesIdempotencyKeyAndPayload() {
        when(materialRepository.listStalePendingVersions(now)).thenReturn(List.of(version));
        var pending = materialTextExtractJobHandler.findStalePending(now).get(0);
        assertThat(pending.idempotencyKey()).isEqualTo("material:" + id + ":1");
        assertThat(pending.payload()).containsEntry("versionNo", 1);
    }
}
