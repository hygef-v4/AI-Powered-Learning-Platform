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
    @Test
    void disabledMaterialsDisappearFromStudentListsAndNewAiSources() {
        var disabled = material.changePresentation(false, null);
        when(materialRepository.findMaterial(id)).thenReturn(Optional.of(disabled));
        when(materialRepository.listMaterials(subject, classId)).thenReturn(List.of(disabled));
        when(materialRepository.listMaterials(subject, null)).thenReturn(List.of(disabled));
        when(materialAccessPolicy.requireClassListAccess(actor, classId, true))
                .thenReturn(
                        new edu.aiplatform.academics.contract.ClassRef(
                                classId,
                                subject,
                                "C",
                                "Class",
                                edu.aiplatform.academics.contract.ClassStatus.OPEN,
                                null,
                                false));
        assertThat(materialQueryService.listClassMaterials(actor, classId, true, null)).isEmpty();
        assertThat(
                materialQueryService.listSubjectMaterials(
                        new Actor(actor.accountId(), Role.SUBJECT_MANAGER), subject))
                .singleElement().satisfies(view -> assertThat(view.enabled()).isFalse());
        assertThat(
                referenceMaterialService.listSelectable(
                        new Actor(actor.accountId(), Role.SUBJECT_MANAGER),
                        ResourceRef.subject(subject))).isEmpty();
        assertThatThrownBy(() -> referenceMaterialService.extractText(actor, List.of(id), 100))
                .isInstanceOf(ApiException.class);
    }

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
        Material current = material.revise("Current title", "", 0, 2, Material.Status.ACTIVE, now);
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
    void deniedDownloadNeverCallsStorage() {
        doThrow(ApiException.notFound("MATERIAL_NOT_FOUND"))
                .when(materialAccessPolicy)
                .requireReadAccess(actor, material, classId);
        assertThatThrownBy(() -> materialQueryService.issueDownloadToken(actor, id, classId, null))
                .isInstanceOf(ApiException.class);
        verifyNoInteractions(artifact);
    }

    @Test
    void studentsCannotDownloadOldVersions() {
        material = material.revise("Title", "", 0, 2, Material.Status.ACTIVE, now);
        when(materialRepository.findMaterial(id)).thenReturn(Optional.of(material));
        var currentVersion = new MaterialVersion(
                id, 2, "current-file", "new.pdf", "application/pdf", 120L,
                null, null, MaterialVersion.TextStatus.EXTRACTED, "new text", actor.accountId(), now);
        when(materialRepository.findVersion(id, 2)).thenReturn(Optional.of(currentVersion));
        when(materialRepository.listVersions(id))
                .thenReturn(List.of(currentVersion, version(id, "old")));
        clearInvocations(materialRepository);
        assertThatThrownBy(() -> materialQueryService.issueDownloadToken(actor, id, classId, 1))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("MATERIAL_NOT_FOUND"));
        verifyNoInteractions(artifact);
        materialQueryService.issueDownloadToken(actor, id, classId, null);
        verify(artifact).issueDownloadToken("current-file", actor.accountId());
        assertThat(materialQueryService.loadMaterial(actor, id, classId).versions())
                .extracting(MaterialViews.Version::versionNo).containsExactly(2);
        Actor teacher = new Actor(actor.accountId(), Role.TEACHER);
        materialQueryService.issueDownloadToken(teacher, id, classId, 1);
        verify(artifact).issueDownloadToken("private-storage", teacher.accountId());
        assertThat(materialQueryService.loadMaterial(teacher, id, classId).versions())
                .extracting(MaterialViews.Version::versionNo).containsExactly(2, 1);
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
        doThrow(ApiException.notFound("MATERIAL_NOT_FOUND"))
                .when(materialAccessPolicy)
                .requireReadAccess(actor, forbidden, null);
        assertThatThrownBy(
                () -> referenceMaterialService.extractText(actor, List.of(id, second), 100))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("MATERIAL_NOT_FOUND"));
        verify(materialRepository, never()).findVersion(any(), anyInt());
    }

    @Test
    void failedExtractionDoesNotBlockAuthorizedViewOrDownload() {
        when(materialRepository.findVersion(id, 1)).thenReturn(Optional.of(new MaterialVersion(
                id, 1, "private-storage", "doc.pdf", "application/pdf", 100L,
                null, null, MaterialVersion.TextStatus.FAILED, null, actor.accountId(), now)));
        assertThat(materialQueryService.loadMaterial(actor, id, classId).current().textStatus())
                .isEqualTo(MaterialVersion.TextStatus.FAILED);
        materialQueryService.issueDownloadToken(actor, id, classId, null);
        verify(artifact).issueDownloadToken("private-storage", actor.accountId());
        assertThat(referenceMaterialService.extractText(actor, List.of(id), 100).get(0).text())
                .isEmpty();
    }

    @Test
    void publicDtoDoesNotExposeStorageIdOrExtractedText() {
        var detail = materialQueryService.loadMaterial(actor, id, classId);
        assertThat(detail.toString()).doesNotContain("private-storage", "abcdef");
        assertThat(detail.canManage()).isFalse();
    }

    @Test
    void extractionUsesTotalBudgetAcrossMaterials() {
        UUID second = UUID.randomUUID();
        Material next =
                new Material(
                        second,
                        material.scope(),
                        subject,
                        null,
                        material.moduleId(),
                        material.kind(),
                        "Second",
                        "",
                        1,
                        material.status(),
                        1,
                        actor.accountId(),
                        now,
                        now,
                        0);
        when(materialRepository.findMaterial(second)).thenReturn(Optional.of(next));
        when(materialRepository.findVersion(second, 1))
                .thenReturn(Optional.of(version(second, "123456")));
        var texts = referenceMaterialService.extractText(actor, List.of(id, second), 8);
        assertThat(texts.get(0).text()).isEqualTo("abcdef");
        assertThat(texts.get(0).truncated()).isFalse();
        assertThat(texts.get(1).text()).isEqualTo("12");
        assertThat(texts.get(1).truncated()).isTrue();
    }

    @Test
    void checksAllIdsBeforeReturningOrReadingAnyText() {
        UUID denied = UUID.randomUUID();
        when(materialRepository.findMaterial(denied)).thenReturn(Optional.empty());
        assertThatThrownBy(
                () -> referenceMaterialService.extractText(actor, List.of(id, denied), 100))
                .isInstanceOf(ApiException.class);
        verify(materialRepository, never()).findVersion(any(), anyInt());
    }

    @Test
    void nonExtractedFileProducesNoInventedText() {
        when(materialRepository.findVersion(id, 1))
                .thenReturn(
                        Optional.of(
                                new MaterialVersion(
                                        id,
                                        1,
                                        "private",
                                        "d.pdf",
                                        "application/pdf",
                                        100L,
                                        null,
                                        null,
                                        MaterialVersion.TextStatus.NO_TEXT,
                                        null,
                                        actor.accountId(),
                                        now)));
        assertThat(referenceMaterialService.extractText(actor, List.of(id), 100).get(0).text())
                .isEmpty();
    }

    @Test
    void rejectsMoreThanTenIdsOrInvalidBudget() {
        assertThatThrownBy(
                () -> referenceMaterialService.extractText(actor, Collections.nCopies(11, id), 100))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> referenceMaterialService.extractText(actor, List.of(id), 0))
                .isInstanceOf(ApiException.class);
        verifyNoInteractions(materialRepository);
    }

    @Test
    void mcpRejectsUnauthorizedIdAndInvalidArguments() {
        var tools = new MaterialMcpTools(materialQueryService, materialRepository);
        doThrow(ApiException.notFound("MATERIAL_NOT_FOUND"))
                .when(materialAccessPolicy)
                .requireReadAccess(actor, material, null);
        assertThat(
                        tools.call(
                                        actor,
                                        "read_authorized_material",
                                        Map.of("materialId", id.toString()))
                                .error())
                .isTrue();
        assertThat(
                        tools.call(actor, "read_authorized_material", Map.of("materialId", "bad"))
                                .error())
                .isTrue();
        assertThat(
                        tools.call(
                                        actor,
                                        "read_authorized_material",
                                        Map.of("materialId", id.toString(), "actorId", "fake"))
                                .error())
                .isTrue();
        verify(materialRepository, never()).findVersion(any(), anyInt());
    }

    @Test
    void mcpReturnsActualVersionAndUntrustedMarker() {
        var result =
                new MaterialMcpTools(materialQueryService, materialRepository)
                        .call(
                                new Actor(actor.accountId(), Role.TEACHER),
                                "read_authorized_material",
                                Map.of("materialId", id.toString()));
        assertThat(result.error()).isFalse();
        Map<?, ?> content = (Map<?, ?>) result.content();
        assertThat(content.get("untrustedContent")).isEqualTo(true);
        assertThat(content.get("versionNo")).isEqualTo(1);
    }

    @Test
    void mcpPagesWithoutSplittingSurrogatePairsAndRejectsChangedVersions() {
        Actor teacher = new Actor(actor.accountId(), Role.TEACHER);
        when(materialRepository.findVersion(id, 1)).thenReturn(Optional.of(version(id, "a😀bc")));
        var tools = new MaterialMcpTools(materialQueryService, materialRepository);
        var first =
                tools.call(
                        teacher,
                        "read_authorized_material",
                        Map.of("materialId", id.toString(), "limit", 2));
        assertThat(first.error()).isFalse();
        var content = (Map<?, ?>) first.content();
        assertThat(content.get("text")).isEqualTo("a");
        assertThat(content.get("nextOffset")).isEqualTo(1);
        var second =
                tools.call(
                        teacher,
                        "read_authorized_material",
                        Map.of(
                                "materialId",
                                id.toString(),
                                "offset",
                                1,
                                "limit",
                                2,
                                "expectedVersion",
                                1));
        assertThat(((Map<?, ?>) second.content()).get("text")).isEqualTo("😀");
        assertThat(
                        tools.call(
                                        teacher,
                                        "read_authorized_material",
                                        Map.of("materialId", id.toString(), "expectedVersion", 2))
                                .error())
                .isTrue();
    }

    @Test
    void staffMcpCanReadArchivedReferenceButNewAiSelectionCannotUseIt() {
        Material archived =
                material.revise(material.title(), "", 0, 1, Material.Status.ARCHIVED, now);
        when(materialRepository.findMaterial(id)).thenReturn(Optional.of(archived));
        Actor teacher = new Actor(actor.accountId(), Role.TEACHER);
        var result =
                new MaterialMcpTools(materialQueryService, materialRepository)
                        .call(
                                teacher,
                                "read_authorized_material",
                                Map.of("materialId", id.toString()));
        assertThat(result.error()).isFalse();
        assertThat(((Map<?, ?>) result.content()).get("text")).isEqualTo("abcdef");
        assertThatThrownBy(() -> referenceMaterialService.extractText(teacher, List.of(id), 100))
                .isInstanceOf(ApiException.class);
    }
}
