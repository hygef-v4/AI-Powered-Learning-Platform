package edu.aiplatform.materials.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import edu.aiplatform.files.contract.*;
import edu.aiplatform.identity.contract.*;
import edu.aiplatform.materials.model.*;
import edu.aiplatform.materials.repository.*;
import edu.aiplatform.shared.error.ApiException;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;

class MaterialQueryAndReferenceTest {

    IMaterialRepository materialRepository;
    IMaterialModuleRepository materialModuleRepository;
    MaterialAccessPolicy materialAccessPolicy;
    IArtifact artifact;
    MaterialQueryService materialQueryService;
    ReferenceMaterialService referenceMaterialService;
    final UUID id = UUID.randomUUID(), subject = UUID.randomUUID(), classId = UUID.randomUUID();
    final Actor actor = new Actor(UUID.randomUUID(), Role.STUDENT);
    final Instant now = Instant.now();
    Material material;

    @BeforeEach
    void setup() {
        materialRepository = mock(IMaterialRepository.class);
        materialAccessPolicy = mock(MaterialAccessPolicy.class);
        artifact = mock(IArtifact.class);
        materialQueryService =
                new MaterialQueryService(materialRepository, materialAccessPolicy, artifact);
        referenceMaterialService =
                new ReferenceMaterialService(materialQueryService, materialRepository);
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
        when(materialRepository.findVersion(id, 1)).thenReturn(Optional.of(version(id, "abcdef")));
        when(materialRepository.listVersions(id)).thenReturn(List.of(version(id, "abcdef")));
    }

    MaterialVersion version(UUID id, String text) {
        return new MaterialVersion(
                id,
                1,
                "private-storage",
                "doc.pdf",
                "application/pdf",
                100L,
                null,
                null,
                MaterialVersion.TextStatus.EXTRACTED,
                text,
                actor.accountId(),
                now);
    }

    @Test
    void extractedTextCarriesActualCurrentVersionProvenance() {
        Material current = material.reviseMaterial("Current title", "", 0, 2, Material.Status.ACTIVE, now);
        when(materialRepository.findMaterial(id)).thenReturn(Optional.of(current));
        when(materialRepository.findVersion(id, 2)).thenReturn(Optional.of(new MaterialVersion(
                id, 2, "current-file", "new.pdf", "application/pdf", 120L,
                null, null, MaterialVersion.TextStatus.EXTRACTED, "current evidence",
                actor.accountId(), now)));
        var result = referenceMaterialService.extractText(actor, List.of(id), 100).get(0);
        com.fasterxml.jackson.databind.JsonNode payload = new com.fasterxml.jackson.databind.ObjectMapper()
                .valueToTree(result);
        assertThat(payload.path("materialId").asText()).isEqualTo(id.toString());
        assertThat(payload.path("title").asText()).isEqualTo("Current title");
        assertThat(payload.path("versionNo").asInt(-1)).isEqualTo(2);
        assertThat(payload.path("text").asText()).isEqualTo("current evidence");
        verify(materialRepository, never()).findVersion(id, 1);
    }

    @Test
    void tokenIsIssuedOnlyAfterAuthorization() {
        materialQueryService.issueDownloadToken(actor, id, classId, null);
        var order = inOrder(materialAccessPolicy, artifact);
        order.verify(materialAccessPolicy).requireReadAccess(actor, material, classId);
        order.verify(artifact).issueDownloadToken("private-storage", actor.accountId());
    }

    @Test
    void selectableAndAttachmentListsFilterActualQueryResults() {
        UUID ownId = UUID.randomUUID(), archivedId = UUID.randomUUID();
        Material own = new Material(ownId, Material.Scope.CLASS, subject, classId,
                material.moduleId(), Material.Kind.FILE, "Own class", "", 1,
                Material.Status.ACTIVE, 1, actor.accountId(), now, now, 0);
        Material archived = new Material(archivedId, Material.Scope.SUBJECT, subject, null,
                material.moduleId(), Material.Kind.FILE, "Archived", "", 2,
                Material.Status.ARCHIVED, 1, actor.accountId(), now, now, 0);
        when(materialRepository.findVersion(ownId, 1)).thenReturn(Optional.of(new MaterialVersion(
                ownId, 1, "own-file", "own.pdf", "application/pdf", 100L,
                null, null, MaterialVersion.TextStatus.PENDING, null, actor.accountId(), now)));
        when(materialRepository.findVersion(archivedId, 1))
                .thenReturn(Optional.of(version(archivedId, "old")));
        when(materialRepository.listMaterials(subject, null))
                .thenReturn(List.of(material, archived));
        when(materialRepository.listMaterials(subject, classId))
                .thenReturn(List.of(material, own, archived));
        when(materialAccessPolicy.requireClassListAccess(actor, classId, true)).thenReturn(
                new edu.aiplatform.academics.contract.ClassRef(classId, subject, "C", "Class",
                        edu.aiplatform.academics.contract.ClassStatus.OPEN, null, false));
        Actor manager = new Actor(actor.accountId(), Role.SUBJECT_MANAGER);
        var subjectSources =
                referenceMaterialService.listSelectable(manager, ResourceRef.subject(subject));
        assertThat(subjectSources).extracting(s -> s.materialId()).containsExactly(id);
        assertThat(subjectSources.get(0).hasText()).isTrue();
        var classSources =
                referenceMaterialService.listSelectable(actor, ResourceRef.courseClass(classId));
        assertThat(classSources).extracting(s -> s.materialId()).containsExactly(id, ownId);
        assertThat(classSources.get(1).hasText()).isFalse();
        assertThat(
                materialQueryService.listClassMaterials(
                        actor, classId, true, List.of(ownId, archivedId)))
                .extracting(MaterialViews.Detail::id).containsExactly(ownId);
        verify(materialAccessPolicy).requireSubjectListAccess(manager, subject);
        verify(materialAccessPolicy, times(2)).requireClassListAccess(actor, classId, true);
    }

    @Test
    void existingButForbiddenSecondIdPreventsAllTextReads() {
        UUID second = UUID.randomUUID();
        Material forbidden = new Material(second, Material.Scope.SUBJECT, subject, null,
                material.moduleId(), material.kind(), "Hidden", "", 0, Material.Status.ACTIVE,
                1, actor.accountId(), now, now, 0);
        when(materialRepository.findMaterial(second)).thenReturn(Optional.of(forbidden));
        doThrow(ApiException.createNotFound("MATERIAL_NOT_FOUND"))
                .when(materialAccessPolicy)
                .requireReadAccess(actor, forbidden, null);
        assertThatThrownBy(
                () -> referenceMaterialService.extractText(actor, List.of(id, second), 100))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.readCode()).isEqualTo("MATERIAL_NOT_FOUND"));
        verify(materialRepository, never()).findVersion(any(), anyInt());
    }
}
