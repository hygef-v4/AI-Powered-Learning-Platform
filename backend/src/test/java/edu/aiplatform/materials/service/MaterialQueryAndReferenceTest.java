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
        var disabled = material.withPresentation(false, null);
        when(repo.find(id)).thenReturn(Optional.of(disabled));
        when(repo.list(subject, classId)).thenReturn(List.of(disabled));
        when(repo.list(subject, null)).thenReturn(List.of(disabled));
        when(access.classList(actor, classId, true)).thenReturn(new edu.aiplatform.academics.contract.ClassRef(
                classId, subject, "C", "Class", edu.aiplatform.academics.contract.ClassStatus.OPEN, null, false));
        assertThat(queries.classList(actor, classId, true, null)).isEmpty();
        assertThat(queries.subjectList(new Actor(actor.accountId(), Role.SUBJECT_MANAGER), subject))
                .singleElement().satisfies(view -> assertThat(view.enabled()).isFalse());
        assertThat(references.listSelectable(new Actor(actor.accountId(), Role.SUBJECT_MANAGER),
                ResourceRef.subject(subject))).isEmpty();
        assertThatThrownBy(() -> references.extractText(actor, List.of(id), 100)).isInstanceOf(ApiException.class);
    }

    IMaterialRepository repo;
    IMaterialModuleRepository modules;
    MaterialAccessPolicy access;
    IArtifact files;
    MaterialQueryService queries;
    ReferenceMaterialService references;
    final UUID id = UUID.randomUUID(), subject = UUID.randomUUID(), classId = UUID.randomUUID();
    final Actor actor = new Actor(UUID.randomUUID(), Role.STUDENT);
    final Instant now = Instant.now();
    Material material;

    @BeforeEach
    void setup() {
        repo = mock(IMaterialRepository.class);
        access = mock(MaterialAccessPolicy.class);
        files = mock(IArtifact.class);
        queries = new MaterialQueryService(repo, access, files);
        references = new ReferenceMaterialService(queries, repo);
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
        when(repo.find(id)).thenReturn(Optional.of(material));
        when(repo.version(id, 1)).thenReturn(Optional.of(version(id, "abcdef")));
        when(repo.versions(id)).thenReturn(List.of(version(id, "abcdef")));
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
        when(repo.find(id)).thenReturn(Optional.of(current));
        when(repo.version(id, 2)).thenReturn(Optional.of(new MaterialVersion(
                id, 2, "current-file", "new.pdf", "application/pdf", 120L,
                null, null, MaterialVersion.TextStatus.EXTRACTED, "current evidence",
                actor.accountId(), now)));
        var result = references.extractText(actor, List.of(id), 100).get(0);
        com.fasterxml.jackson.databind.JsonNode payload = new com.fasterxml.jackson.databind.ObjectMapper()
                .valueToTree(result);
        assertThat(payload.path("materialId").asText()).isEqualTo(id.toString());
        assertThat(payload.path("title").asText()).isEqualTo("Current title");
        assertThat(payload.path("versionNo").asInt(-1)).isEqualTo(2);
        assertThat(payload.path("text").asText()).isEqualTo("current evidence");
        verify(repo, never()).version(id, 1);
    }

    @Test
    void tokenIsIssuedOnlyAfterAuthorization() {
        queries.download(actor, id, classId, null);
        var order = inOrder(access, files);
        order.verify(access).read(actor, material, classId);
        order.verify(files).issueDownloadToken("private-storage", actor.accountId());
    }

    @Test
    void deniedDownloadNeverCallsStorage() {
        doThrow(ApiException.notFound("MATERIAL_NOT_FOUND"))
                .when(access)
                .read(actor, material, classId);
        assertThatThrownBy(() -> queries.download(actor, id, classId, null))
                .isInstanceOf(ApiException.class);
        verifyNoInteractions(files);
    }

    @Test
    void studentsCannotDownloadOldVersions() {
        material = material.revise("Title", "", 0, 2, Material.Status.ACTIVE, now);
        when(repo.find(id)).thenReturn(Optional.of(material));
        var currentVersion = new MaterialVersion(
                id, 2, "current-file", "new.pdf", "application/pdf", 120L,
                null, null, MaterialVersion.TextStatus.EXTRACTED, "new text", actor.accountId(), now);
        when(repo.version(id, 2)).thenReturn(Optional.of(currentVersion));
        when(repo.versions(id)).thenReturn(List.of(currentVersion, version(id, "old")));
        clearInvocations(repo);
        assertThatThrownBy(() -> queries.download(actor, id, classId, 1))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("MATERIAL_NOT_FOUND"));
        verifyNoInteractions(files);
        queries.download(actor, id, classId, null);
        verify(files).issueDownloadToken("current-file", actor.accountId());
        assertThat(queries.detail(actor, id, classId).versions())
                .extracting(MaterialViews.Version::versionNo).containsExactly(2);
        Actor teacher = new Actor(actor.accountId(), Role.TEACHER);
        queries.download(teacher, id, classId, 1);
        verify(files).issueDownloadToken("private-storage", teacher.accountId());
        assertThat(queries.detail(teacher, id, classId).versions())
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
        when(repo.version(ownId, 1)).thenReturn(Optional.of(new MaterialVersion(
                ownId, 1, "own-file", "own.pdf", "application/pdf", 100L,
                null, null, MaterialVersion.TextStatus.PENDING, null, actor.accountId(), now)));
        when(repo.version(archivedId, 1)).thenReturn(Optional.of(version(archivedId, "old")));
        when(repo.list(subject, null)).thenReturn(List.of(material, archived));
        when(repo.list(subject, classId)).thenReturn(List.of(material, own, archived));
        when(access.classList(actor, classId, true)).thenReturn(
                new edu.aiplatform.academics.contract.ClassRef(classId, subject, "C", "Class",
                        edu.aiplatform.academics.contract.ClassStatus.OPEN, null, false));
        Actor manager = new Actor(actor.accountId(), Role.SUBJECT_MANAGER);
        var subjectSources = references.listSelectable(manager, ResourceRef.subject(subject));
        assertThat(subjectSources).extracting(s -> s.materialId()).containsExactly(id);
        assertThat(subjectSources.get(0).hasText()).isTrue();
        var classSources = references.listSelectable(actor, ResourceRef.courseClass(classId));
        assertThat(classSources).extracting(s -> s.materialId()).containsExactly(id, ownId);
        assertThat(classSources.get(1).hasText()).isFalse();
        assertThat(queries.classList(actor, classId, true, List.of(ownId, archivedId)))
                .extracting(MaterialViews.Detail::id).containsExactly(ownId);
        verify(access).subjectList(manager, subject);
        verify(access, times(2)).classList(actor, classId, true);
    }

    @Test
    void existingButForbiddenSecondIdPreventsAllTextReads() {
        UUID second = UUID.randomUUID();
        Material forbidden = new Material(second, Material.Scope.SUBJECT, subject, null,
                material.moduleId(), material.kind(), "Hidden", "", 0, Material.Status.ACTIVE,
                1, actor.accountId(), now, now, 0);
        when(repo.find(second)).thenReturn(Optional.of(forbidden));
        doThrow(ApiException.notFound("MATERIAL_NOT_FOUND")).when(access).read(actor, forbidden, null);
        assertThatThrownBy(() -> references.extractText(actor, List.of(id, second), 100))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("MATERIAL_NOT_FOUND"));
        verify(repo, never()).version(any(), anyInt());
    }

    @Test
    void failedExtractionDoesNotBlockAuthorizedViewOrDownload() {
        when(repo.version(id, 1)).thenReturn(Optional.of(new MaterialVersion(
                id, 1, "private-storage", "doc.pdf", "application/pdf", 100L,
                null, null, MaterialVersion.TextStatus.FAILED, null, actor.accountId(), now)));
        assertThat(queries.detail(actor, id, classId).current().textStatus())
                .isEqualTo(MaterialVersion.TextStatus.FAILED);
        queries.download(actor, id, classId, null);
        verify(files).issueDownloadToken("private-storage", actor.accountId());
        assertThat(references.extractText(actor, List.of(id), 100).get(0).text()).isEmpty();
    }

    @Test
    void publicDtoDoesNotExposeStorageIdOrExtractedText() {
        var detail = queries.detail(actor, id, classId);
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
        when(repo.find(second)).thenReturn(Optional.of(next));
        when(repo.version(second, 1)).thenReturn(Optional.of(version(second, "123456")));
        var texts = references.extractText(actor, List.of(id, second), 8);
        assertThat(texts.get(0).text()).isEqualTo("abcdef");
        assertThat(texts.get(0).truncated()).isFalse();
        assertThat(texts.get(1).text()).isEqualTo("12");
        assertThat(texts.get(1).truncated()).isTrue();
    }

    @Test
    void checksAllIdsBeforeReturningOrReadingAnyText() {
        UUID denied = UUID.randomUUID();
        when(repo.find(denied)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> references.extractText(actor, List.of(id, denied), 100))
                .isInstanceOf(ApiException.class);
        verify(repo, never()).version(any(), anyInt());
    }

    @Test
    void nonExtractedFileProducesNoInventedText() {
        when(repo.version(id, 1))
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
        assertThat(references.extractText(actor, List.of(id), 100).get(0).text()).isEmpty();
    }

    @Test
    void rejectsMoreThanTenIdsOrInvalidBudget() {
        assertThatThrownBy(() -> references.extractText(actor, Collections.nCopies(11, id), 100))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> references.extractText(actor, List.of(id), 0))
                .isInstanceOf(ApiException.class);
        verifyNoInteractions(repo);
    }

    @Test
    void mcpRejectsUnauthorizedIdAndInvalidArguments() {
        var tools = new MaterialMcpTools(queries, repo);
        doThrow(ApiException.notFound("MATERIAL_NOT_FOUND"))
                .when(access)
                .read(actor, material, null);
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
        verify(repo, never()).version(any(), anyInt());
    }

    @Test
    void mcpReturnsActualVersionAndUntrustedMarker() {
        var result =
                new MaterialMcpTools(queries, repo)
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
        when(repo.version(id, 1)).thenReturn(Optional.of(version(id, "a😀bc")));
        var tools = new MaterialMcpTools(queries, repo);
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
        when(repo.find(id)).thenReturn(Optional.of(archived));
        Actor teacher = new Actor(actor.accountId(), Role.TEACHER);
        var result =
                new MaterialMcpTools(queries, repo)
                        .call(
                                teacher,
                                "read_authorized_material",
                                Map.of("materialId", id.toString()));
        assertThat(result.error()).isFalse();
        assertThat(((Map<?, ?>) result.content()).get("text")).isEqualTo("abcdef");
        assertThatThrownBy(() -> references.extractText(teacher, List.of(id), 100))
                .isInstanceOf(ApiException.class);
    }
}
