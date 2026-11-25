package edu.aiplatform.materials.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import edu.aiplatform.audit.contract.*;
import edu.aiplatform.files.contract.*;
import edu.aiplatform.identity.contract.*;
import edu.aiplatform.jobs.contract.*;
import edu.aiplatform.materials.model.*;
import edu.aiplatform.materials.repository.*;
import edu.aiplatform.shared.error.ApiException;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import edu.aiplatform.academics.contract.ClassRef;
import edu.aiplatform.academics.contract.ClassStatus;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class MaterialServiceTest {

    final UUID id = UUID.randomUUID(), subject = UUID.randomUUID();
    final Actor actor = new Actor(UUID.randomUUID(), Role.SUBJECT_MANAGER);
    final Instant now = Instant.parse("2026-10-21T00:00:00Z");
    IMaterialRepository materialRepository;
    IMaterialModuleRepository materialModuleRepository;
    MaterialAccessPolicy materialAccessPolicy;
    IArtifact artifact;
    IJob job;
    IAudit audit;
    MaterialService materialService;
    Material material;

    @BeforeEach
    void setup() {
        materialRepository = mock(IMaterialRepository.class);
        materialAccessPolicy = mock(MaterialAccessPolicy.class);
        artifact = mock(IArtifact.class);
        job = mock(IJob.class);
        audit = mock(IAudit.class);
        materialModuleRepository = mock(IMaterialModuleRepository.class);
        when(materialModuleRepository.findModule(new UUID(0, 1)))
                .thenReturn(
                        Optional.of(
                                new MaterialModule(
                                        new UUID(0, 1),
                                        subject,
                                        "Module 1",
                                        0,
                                        actor.accountId(),
                                        now,
                                        now,
                                        0)));
        materialService =
                new MaterialService(
                        materialRepository,
                        materialModuleRepository,
                        materialAccessPolicy,
                        artifact,
                        job,
                        Clock.fixed(now, ZoneOffset.UTC));
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
                        actor.accountId(),
                        now,
                        now,
                        0);
        when(materialRepository.findMaterial(id)).thenReturn(Optional.of(material));
        when(materialRepository.updateMaterial(any(), anyLong())).thenReturn(true);
        when(artifact.attach(any(), eq(actor), eq(ArtifactPurpose.MATERIAL)))
                .thenReturn(
                        new StoredFileInfo(
                                "private-file",
                                actor.accountId(),
                                ArtifactPurpose.MATERIAL,
                                "doc.pdf",
                                "application/pdf",
                                100L,
                                "sha"));
    }

    @Test
    void mutationsAuditExactActionActorObjectTimeAndVersion() {
        materialService.updateMaterial(
                actor, id, new MaterialCommands.Update("New", "", 0, 0L, null));
        materialService.replaceMaterialVersion(
                actor,
                id,
                new MaterialCommands.Replace(0L, new MaterialCommands.Source("signed", null)));
        materialService.archiveMaterial(actor, id, 0L);
        var events = ArgumentCaptor.forClass(AuditEvent.class);
        assertThat(events.getAllValues()).allSatisfy(event -> {
            assertThat(event.actorId()).isEqualTo(actor.accountId());
            assertThat(event.objectId()).isEqualTo(id.toString());
            assertThat(event.result()).isEqualTo(AuditResult.SUCCESS);
            assertThat(event.occurredAt()).isEqualTo(now);
            assertThat(event.details()).containsEntry("version", 1L);
        });
        when(materialRepository.updateMaterial(any(), anyLong())).thenReturn(false);
        assertThatThrownBy(() -> materialService.updateMaterial(actor, id,
                new MaterialCommands.Update("Rejected", "", 0, 0L, null)))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.readCode()).isEqualTo("MATERIAL_VERSION_CONFLICT"));
    }
}
