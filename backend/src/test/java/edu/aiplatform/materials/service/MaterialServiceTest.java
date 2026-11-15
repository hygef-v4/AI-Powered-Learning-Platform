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
    @Test
    void legacyTitleAndVisibilityUpdatePreservesFormattedTrailingBlankParagraph() {
        var document = new edu.aiplatform.shared.content.FormattedText(List.of(
                new edu.aiplatform.shared.content.FormattedText.Block(
                        edu.aiplatform.shared.content.FormattedText.Type.PARAGRAPH,
                        edu.aiplatform.shared.content.FormattedText.Align.CENTER, 0,
                        List.of(new edu.aiplatform.shared.content.FormattedText.Run("Mô tả", true, null, null))),
                new edu.aiplatform.shared.content.FormattedText.Block(
                        edu.aiplatform.shared.content.FormattedText.Type.PARAGRAPH,
                        edu.aiplatform.shared.content.FormattedText.Align.LEFT, 0,
                        List.of(new edu.aiplatform.shared.content.FormattedText.Run("", null, null, null)))));
        Material rich = material.changePresentation(false, document);
        when(materialRepository.findMaterial(id)).thenReturn(Optional.of(rich));
        when(materialRepository.updateMaterial(any(), eq(0L))).thenReturn(true);
        Material edited = materialService.updateMaterial(actor, id,
                new MaterialCommands.Update("New title", "Mô tả", 0, 0L, null, true, null));
        assertThat(edited.descriptionDocument()).isEqualTo(document);
        assertThat(edited.description()).isEqualTo("Mô tả\n");
        assertThat(edited.enabled()).isTrue();
    }

    @Test
    void presentationUpdatesDerivePlainTextAndPreserveDisabledAcrossRevisions() {
        var document = new edu.aiplatform.shared.content.FormattedText(List.of(
                new edu.aiplatform.shared.content.FormattedText.Block(
                        edu.aiplatform.shared.content.FormattedText.Type.UL,
                        edu.aiplatform.shared.content.FormattedText.Align.CENTER, 1,
                        List.of(new edu.aiplatform.shared.content.FormattedText.Run("Mô tả", true, null, null)))));
        when(materialRepository.updateMaterial(any(), eq(0L))).thenReturn(true);
        Material changed = materialService.updateMaterial(actor, id,
                new MaterialCommands.Update("Title", "Ignored plain", 0, 0L, null, false, document));
        assertThat(changed.enabled()).isFalse();
        assertThat(changed.description()).isEqualTo("Mô tả");
        assertThat(changed.descriptionDocument()).isEqualTo(document);
        assertThat(changed.revise("Title", changed.description(), 0, 2, Material.Status.ACTIVE, now).enabled()).isFalse();
        assertThat(changed.moveToModule(UUID.randomUUID()).descriptionDocument())
                .isEqualTo(document);
        when(materialRepository.findMaterial(id)).thenReturn(Optional.of(changed));
        when(materialRepository.updateMaterial(any(), eq(changed.version()))).thenReturn(true);
        Material enabled = materialService.updateMaterial(actor, id,
                new MaterialCommands.Update("Title", "Mô tả", 0, changed.version(), null, true, null));
        assertThat(enabled.enabled()).isTrue();
        assertThat(enabled.descriptionDocument()).isEqualTo(document);
    }

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
                        audit,
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
    void createAttachesTrustedFileAndEnqueuesVersionedJob() {
        var m =
                materialService.createMaterial(
                        actor,
                        subject,
                        false,
                        new MaterialCommands.Create(
                                Material.Kind.FILE,
                                "Title",
                                "",
                                0,
                                new MaterialCommands.Source("signed", null),
                                new UUID(0, 1)));
        verify(artifact).attach(new FileRef("signed"), actor, ArtifactPurpose.MATERIAL);
        verify(materialRepository)
                .insertMaterial(
                        eq(m),
                        argThat(
                                v ->
                                        v.fileId().equals("private-file")
                                                && v.textStatus()
                                                        == MaterialVersion.TextStatus.PENDING));
        verify(job)
                .enqueue(
                        eq(JobTypes.MATERIAL_TEXT_EXTRACT),
                        eq(Map.of("materialId", m.id().toString(), "versionNo", 1)),
                        eq("material:" + m.id() + ":1"));
        verify(audit).record(argThat(e -> e.action().equals("MATERIAL_CREATED")));
    }

    @Test
    void youtubeCreateDoesNotCallFilesOrJobs() {
        materialService.createMaterial(
                actor,
                subject,
                false,
                new MaterialCommands.Create(
                        Material.Kind.YOUTUBE_LINK,
                        "Video",
                        "",
                        0,
                        new MaterialCommands.Source(null, "https://youtu.be/abcdefghijk"),
                        new UUID(0, 1)));
        verifyNoInteractions(artifact, job);
        verify(materialRepository)
                .insertMaterial(
                        any(),
                        argThat(v -> v.videoId().equals("abcdefghijk") && v.textStatus() == null));
    }

    @Test
    void replaceAppendsWithoutChangingOldVersion() {
        var next =
                materialService.replaceMaterialVersion(
                        actor,
                        id,
                        new MaterialCommands.Replace(
                                0L, new MaterialCommands.Source("signed", null)));
        assertThat(next.currentVersionNo()).isEqualTo(2);
        assertThat(material.currentVersionNo()).isEqualTo(1);
        verify(materialRepository).insertVersion(argThat(v -> v.versionNo() == 2));
        verify(materialRepository, never()).completeText(any(), anyInt(), any(), any());
    }

    @Test
    void metadataAcceptsDocumentedBoundsAndRejectsOverflowWithoutWrites() {
        var next = materialService.updateMaterial(actor, id, new MaterialCommands.Update(
                "t".repeat(200), "d".repeat(2000), 0, 0L, null));
        assertThat(next.title()).hasSize(200);
        assertThat(next.description()).hasSize(2000);
        clearInvocations(materialRepository, audit);
        for (var input : List.of(
                new MaterialCommands.Update("t".repeat(201), "", 0, 0L, null),
                new MaterialCommands.Update("Title", "d".repeat(2001), 0, 0L, null),
                new MaterialCommands.Update("Title", "", -1, 0L, null))) {
            assertThatThrownBy(() -> materialService.updateMaterial(actor, id, input))
                    .isInstanceOf(IllegalArgumentException.class);
        }
        verify(materialRepository, never()).updateMaterial(any(), anyLong());
        verifyNoInteractions(audit, artifact, job);
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
        verify(audit, times(3)).record(events.capture());
        assertThat(events.getAllValues()).extracting(AuditEvent::action)
                .containsExactly("MATERIAL_UPDATED", "MATERIAL_VERSION_CREATED", "MATERIAL_ARCHIVED");
        assertThat(events.getAllValues()).allSatisfy(event -> {
            assertThat(event.actorId()).isEqualTo(actor.accountId());
            assertThat(event.objectId()).isEqualTo(id.toString());
            assertThat(event.result()).isEqualTo(AuditResult.SUCCESS);
            assertThat(event.occurredAt()).isEqualTo(now);
            assertThat(event.details()).containsEntry("version", 1L);
        });
        clearInvocations(audit);
        when(materialRepository.updateMaterial(any(), anyLong())).thenReturn(false);
        assertThatThrownBy(() -> materialService.updateMaterial(actor, id,
                new MaterialCommands.Update("Rejected", "", 0, 0L, null)))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("MATERIAL_VERSION_CONFLICT"));
        verifyNoInteractions(audit);
    }

    @Test
    void metadataUpdateDoesNotCreateContentVersionOrJob() {
        var next =
                materialService.updateMaterial(
                        actor, id, new MaterialCommands.Update("New", "Description", 3, 0L, null));
        assertThat(next.currentVersionNo()).isEqualTo(1);
        assertThat(next.version()).isEqualTo(1);
        verify(materialRepository, never()).insertVersion(any());
        verifyNoInteractions(artifact, job);
    }

    @Test
    void archiveKeepsHistoricalVersions() {
        assertThat(materialService.archiveMaterial(actor, id, 0).status())
                .isEqualTo(Material.Status.ARCHIVED);
        verify(materialRepository, never()).insertVersion(any());
        verifyNoInteractions(artifact, job);
    }

    @Test
    void staleVersionDoesNotAttachOrEnqueue() {
        assertThatThrownBy(
                        () ->
                                materialService.replaceMaterialVersion(
                                        actor,
                                        id,
                                        new MaterialCommands.Replace(
                                                8L, new MaterialCommands.Source("signed", null))))
                .isInstanceOfSatisfying(
                        ApiException.class, e -> assertThat(e.status().value()).isEqualTo(409));
        verifyNoInteractions(artifact, job, audit);
        verify(materialRepository, never()).updateMaterial(any(), anyLong());
    }

    @Test
    void lostCompareAndSwapDoesNotInsertVersionOrEnqueue() {
        when(materialRepository.updateMaterial(any(), anyLong())).thenReturn(false);
        assertThatThrownBy(
                        () ->
                                materialService.replaceMaterialVersion(
                                        actor,
                                        id,
                                        new MaterialCommands.Replace(
                                                0L, new MaterialCommands.Source("signed", null))))
                .isInstanceOf(ApiException.class);
        verify(materialRepository, never()).insertVersion(any());
        verifyNoInteractions(job, audit);
    }

    @Test
    void deniedMutationDoesNotTouchArtifactOrWrite() {
        doThrow(ApiException.notFound("MATERIAL_NOT_FOUND"))
                .when(materialAccessPolicy)
                .requireWriteAccess(actor, material);
        assertThatThrownBy(
                        () ->
                                materialService.replaceMaterialVersion(
                                        actor,
                                        id,
                                        new MaterialCommands.Replace(
                                                0L, new MaterialCommands.Source("signed", null))))
                .isInstanceOf(ApiException.class);
        verifyNoInteractions(artifact, job, audit);
        verify(materialRepository, never()).updateMaterial(any(), anyLong());
    }

    @Test
    void duplicateReorderRejectsBeforeAnyWrite() {
        var row = new MaterialCommands.Position(id, 0L);
        assertThatThrownBy(
                        () ->
                                materialService.reorderMaterials(
                                        actor,
                                        subject,
                                        false,
                                        new MaterialCommands.Reorder(List.of(row, row))))
                .isInstanceOf(ApiException.class);
        verify(materialRepository, never()).updateMaterial(any(), anyLong());
    }

    @Test
    void foreignScopeReorderRejectsBeforeAnyWrite() {
        assertThatThrownBy(
                        () ->
                                materialService.reorderMaterials(
                                        actor,
                                        UUID.randomUUID(),
                                        false,
                                        new MaterialCommands.Reorder(
                                                List.of(new MaterialCommands.Position(id, 0L)))))
                .isInstanceOf(ApiException.class);
        verify(materialRepository, never()).updateMaterial(any(), anyLong());
    }

    @Test
    void classMaterialCreateReplaceUpdateAndArchiveKeepScopeAndAppendContentHistory() {
        UUID classId = UUID.randomUUID();
        when(materialAccessPolicy.requireClassListAccess(actor, classId, false))
                .thenReturn(
                        new ClassRef(
                                classId,
                                subject,
                                "C",
                                "Class",
                                ClassStatus.OPEN,
                                actor.accountId(),
                                false));
        var created =
                materialService.createMaterial(
                        actor,
                        classId,
                        true,
                        new MaterialCommands.Create(
                                Material.Kind.FILE,
                                "Class file",
                                "",
                                0,
                                new MaterialCommands.Source("signed", null),
                                new UUID(0, 1)));
        assertThat(created.scope()).isEqualTo(Material.Scope.CLASS);
        assertThat(created.classId()).isEqualTo(classId);
        assertThat(created.subjectId()).isEqualTo(subject);
        verify(materialRepository)
                .insertMaterial(
                        eq(created),
                        argThat(v -> v.versionNo() == 1 && v.fileId().equals("private-file")));
        when(materialRepository.findMaterial(created.id())).thenReturn(Optional.of(created));

        var replaced =
                materialService.replaceMaterialVersion(
                        actor,
                        created.id(),
                        new MaterialCommands.Replace(
                                0L, new MaterialCommands.Source("signed", null)));
        verify(materialRepository)
                .insertVersion(
                        argThat(v -> v.materialId().equals(created.id()) && v.versionNo() == 2));
        assertThat(created.currentVersionNo()).isEqualTo(1);
        assertThat(replaced.currentVersionNo()).isEqualTo(2);
        when(materialRepository.findMaterial(created.id())).thenReturn(Optional.of(replaced));
        var updated =
                materialService.updateMaterial(
                        actor,
                        created.id(),
                        new MaterialCommands.Update("Updated", "Description", 1, 1L, null));
        assertThat(updated.currentVersionNo()).isEqualTo(2);
        assertThat(updated.classId()).isEqualTo(classId);
        when(materialRepository.findMaterial(created.id())).thenReturn(Optional.of(updated));
        var archived = materialService.archiveMaterial(actor, created.id(), 2L);
        assertThat(archived.status()).isEqualTo(Material.Status.ARCHIVED);
        assertThat(archived.currentVersionNo()).isEqualTo(2);
        verify(materialRepository, times(1)).insertVersion(any());
        verify(job)
                .enqueue(
                        eq(JobTypes.MATERIAL_TEXT_EXTRACT),
                        eq(Map.of("materialId", created.id().toString(), "versionNo", 2)),
                        eq("material:" + created.id() + ":2"));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void createAndMoveRejectModuleOfAnotherSubjectBeforeArtifactOrPersistence(boolean classScope) {
        UUID scopeId = classScope ? UUID.randomUUID() : subject, foreignModule = UUID.randomUUID();
        if (classScope)
            when(materialAccessPolicy.requireClassListAccess(actor, scopeId, false))
                    .thenReturn(
                            new ClassRef(
                                    scopeId,
                                    subject,
                                    "C",
                                    "Class",
                                    ClassStatus.OPEN,
                                    actor.accountId(),
                                    false));
        when(materialModuleRepository.findModule(foreignModule))
                .thenReturn(
                        Optional.of(
                                new MaterialModule(
                                        foreignModule,
                                        UUID.randomUUID(),
                                        "Foreign",
                                        0,
                                        actor.accountId(),
                                        now,
                                        now,
                                        0)));
        assertThatThrownBy(
                        () ->
                                materialService.createMaterial(
                                        actor,
                                        scopeId,
                                        classScope,
                                        new MaterialCommands.Create(
                                                Material.Kind.FILE,
                                                "Title",
                                                "",
                                                0,
                                                new MaterialCommands.Source("signed", null),
                                                foreignModule)))
                .isInstanceOfSatisfying(
                        ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("MATERIAL_MODULE_INVALID"));
        Material scoped =
                new Material(
                        id,
                        classScope ? Material.Scope.CLASS : Material.Scope.SUBJECT,
                        subject,
                        classScope ? scopeId : null,
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
        when(materialRepository.findMaterial(id)).thenReturn(Optional.of(scoped));
        assertThatThrownBy(
                        () ->
                                materialService.updateMaterial(
                                        actor,
                                        id,
                                        new MaterialCommands.Update(
                                                "Moved", "", 0, 0L, foreignModule)))
                .isInstanceOfSatisfying(
                        ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("MATERIAL_MODULE_INVALID"));
        verify(materialRepository, never()).insertMaterial(any(), any());
        verify(materialRepository, never()).updateMaterial(any(), anyLong());
        verifyNoInteractions(artifact, job, audit);
    }

    @Test
    void classReplacementStaleVersionAndLostCasCannotAppendHistoryOrEnqueue() {
        UUID classId = UUID.randomUUID();
        Material scoped =
                new Material(
                        id,
                        Material.Scope.CLASS,
                        subject,
                        classId,
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
        when(materialRepository.findMaterial(id)).thenReturn(Optional.of(scoped));
        assertThatThrownBy(
                        () ->
                                materialService.replaceMaterialVersion(
                                        actor,
                                        id,
                                        new MaterialCommands.Replace(
                                                1L, new MaterialCommands.Source("signed", null))))
                .isInstanceOfSatisfying(
                        ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("MATERIAL_VERSION_CONFLICT"));
        verifyNoInteractions(artifact, job, audit);
        when(materialRepository.updateMaterial(any(), anyLong())).thenReturn(false);
        assertThatThrownBy(
                        () ->
                                materialService.replaceMaterialVersion(
                                        actor,
                                        id,
                                        new MaterialCommands.Replace(
                                                0L, new MaterialCommands.Source("signed", null))))
                .isInstanceOfSatisfying(
                        ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("MATERIAL_VERSION_CONFLICT"));
        verify(materialRepository, never()).insertVersion(any());
        verifyNoInteractions(job, audit);
    }
}
