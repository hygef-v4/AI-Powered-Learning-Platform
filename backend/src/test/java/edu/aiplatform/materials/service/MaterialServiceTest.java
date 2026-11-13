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
        Material rich = material.withPresentation(false, document);
        when(repo.find(id)).thenReturn(Optional.of(rich));
        when(repo.update(any(), eq(0L))).thenReturn(true);
        Material edited = service.update(actor, id,
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
        when(repo.update(any(), eq(0L))).thenReturn(true);
        Material changed = service.update(actor, id,
                new MaterialCommands.Update("Title", "Ignored plain", 0, 0L, null, false, document));
        assertThat(changed.enabled()).isFalse();
        assertThat(changed.description()).isEqualTo("Mô tả");
        assertThat(changed.descriptionDocument()).isEqualTo(document);
        assertThat(changed.revise("Title", changed.description(), 0, 2, Material.Status.ACTIVE, now).enabled()).isFalse();
        assertThat(changed.inModule(UUID.randomUUID()).descriptionDocument()).isEqualTo(document);
        when(repo.find(id)).thenReturn(Optional.of(changed));
        when(repo.update(any(), eq(changed.version()))).thenReturn(true);
        Material enabled = service.update(actor, id,
                new MaterialCommands.Update("Title", "Mô tả", 0, changed.version(), null, true, null));
        assertThat(enabled.enabled()).isTrue();
        assertThat(enabled.descriptionDocument()).isEqualTo(document);
    }

    final UUID id = UUID.randomUUID(), subject = UUID.randomUUID();
    final Actor actor = new Actor(UUID.randomUUID(), Role.SUBJECT_MANAGER);
    final Instant now = Instant.parse("2026-10-21T00:00:00Z");
    IMaterialRepository repo;
    IMaterialModuleRepository modules;
    MaterialAccessPolicy access;
    IArtifact files;
    IJob jobs;
    IAudit audit;
    MaterialService service;
    Material material;

    @BeforeEach
    void setup() {
        repo = mock(IMaterialRepository.class);
        access = mock(MaterialAccessPolicy.class);
        files = mock(IArtifact.class);
        jobs = mock(IJob.class);
        audit = mock(IAudit.class);
        modules = mock(IMaterialModuleRepository.class);
        when(modules.find(new UUID(0, 1)))
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
        service =
                new MaterialService(
                        repo,
                        modules,
                        access,
                        files,
                        jobs,
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
        when(repo.find(id)).thenReturn(Optional.of(material));
        when(repo.update(any(), anyLong())).thenReturn(true);
        when(files.attach(any(), eq(actor), eq(ArtifactPurpose.MATERIAL)))
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
                service.create(
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
        verify(files).attach(new FileRef("signed"), actor, ArtifactPurpose.MATERIAL);
        verify(repo)
                .insert(
                        eq(m),
                        argThat(
                                v ->
                                        v.fileId().equals("private-file")
                                                && v.textStatus()
                                                        == MaterialVersion.TextStatus.PENDING));
        verify(jobs)
                .enqueue(
                        eq(JobTypes.MATERIAL_TEXT_EXTRACT),
                        eq(Map.of("materialId", m.id().toString(), "versionNo", 1)),
                        eq("material:" + m.id() + ":1"));
        verify(audit).record(argThat(e -> e.action().equals("MATERIAL_CREATED")));
    }

    @Test
    void youtubeCreateDoesNotCallFilesOrJobs() {
        service.create(
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
        verifyNoInteractions(files, jobs);
        verify(repo)
                .insert(
                        any(),
                        argThat(v -> v.videoId().equals("abcdefghijk") && v.textStatus() == null));
    }

    @Test
    void replaceAppendsWithoutChangingOldVersion() {
        var next =
                service.replace(
                        actor,
                        id,
                        new MaterialCommands.Replace(
                                0L, new MaterialCommands.Source("signed", null)));
        assertThat(next.currentVersionNo()).isEqualTo(2);
        assertThat(material.currentVersionNo()).isEqualTo(1);
        verify(repo).insertVersion(argThat(v -> v.versionNo() == 2));
        verify(repo, never()).completeText(any(), anyInt(), any(), any());
    }

    @Test
    void metadataAcceptsDocumentedBoundsAndRejectsOverflowWithoutWrites() {
        var next = service.update(actor, id, new MaterialCommands.Update(
                "t".repeat(200), "d".repeat(2000), 0, 0L, null));
        assertThat(next.title()).hasSize(200);
        assertThat(next.description()).hasSize(2000);
        clearInvocations(repo, audit);
        for (var input : List.of(
                new MaterialCommands.Update("t".repeat(201), "", 0, 0L, null),
                new MaterialCommands.Update("Title", "d".repeat(2001), 0, 0L, null),
                new MaterialCommands.Update("Title", "", -1, 0L, null))) {
            assertThatThrownBy(() -> service.update(actor, id, input))
                    .isInstanceOf(IllegalArgumentException.class);
        }
        verify(repo, never()).update(any(), anyLong());
        verifyNoInteractions(audit, files, jobs);
    }

    @Test
    void mutationsAuditExactActionActorObjectTimeAndVersion() {
        service.update(actor, id, new MaterialCommands.Update("New", "", 0, 0L, null));
        service.replace(actor, id, new MaterialCommands.Replace(0L, new MaterialCommands.Source("signed", null)));
        service.archive(actor, id, 0L);
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
        when(repo.update(any(), anyLong())).thenReturn(false);
        assertThatThrownBy(() -> service.update(actor, id,
                new MaterialCommands.Update("Rejected", "", 0, 0L, null)))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("MATERIAL_VERSION_CONFLICT"));
        verifyNoInteractions(audit);
    }

    @Test
    void metadataUpdateDoesNotCreateContentVersionOrJob() {
        var next =
                service.update(
                        actor, id, new MaterialCommands.Update("New", "Description", 3, 0L, null));
        assertThat(next.currentVersionNo()).isEqualTo(1);
        assertThat(next.version()).isEqualTo(1);
        verify(repo, never()).insertVersion(any());
        verifyNoInteractions(files, jobs);
    }

    @Test
    void archiveKeepsHistoricalVersions() {
        assertThat(service.archive(actor, id, 0).status()).isEqualTo(Material.Status.ARCHIVED);
        verify(repo, never()).insertVersion(any());
        verifyNoInteractions(files, jobs);
    }

    @Test
    void staleVersionDoesNotAttachOrEnqueue() {
        assertThatThrownBy(
                        () ->
                                service.replace(
                                        actor,
                                        id,
                                        new MaterialCommands.Replace(
                                                8L, new MaterialCommands.Source("signed", null))))
                .isInstanceOfSatisfying(
                        ApiException.class, e -> assertThat(e.status().value()).isEqualTo(409));
        verifyNoInteractions(files, jobs, audit);
        verify(repo, never()).update(any(), anyLong());
    }

    @Test
    void lostCompareAndSwapDoesNotInsertVersionOrEnqueue() {
        when(repo.update(any(), anyLong())).thenReturn(false);
        assertThatThrownBy(
                        () ->
                                service.replace(
                                        actor,
                                        id,
                                        new MaterialCommands.Replace(
                                                0L, new MaterialCommands.Source("signed", null))))
                .isInstanceOf(ApiException.class);
        verify(repo, never()).insertVersion(any());
        verifyNoInteractions(jobs, audit);
    }

    @Test
    void deniedMutationDoesNotTouchArtifactOrWrite() {
        doThrow(ApiException.notFound("MATERIAL_NOT_FOUND")).when(access).write(actor, material);
        assertThatThrownBy(
                        () ->
                                service.replace(
                                        actor,
                                        id,
                                        new MaterialCommands.Replace(
                                                0L, new MaterialCommands.Source("signed", null))))
                .isInstanceOf(ApiException.class);
        verifyNoInteractions(files, jobs, audit);
        verify(repo, never()).update(any(), anyLong());
    }

    @Test
    void duplicateReorderRejectsBeforeAnyWrite() {
        var row = new MaterialCommands.Position(id, 0L);
        assertThatThrownBy(
                        () ->
                                service.reorder(
                                        actor,
                                        subject,
                                        false,
                                        new MaterialCommands.Reorder(List.of(row, row))))
                .isInstanceOf(ApiException.class);
        verify(repo, never()).update(any(), anyLong());
    }

    @Test
    void foreignScopeReorderRejectsBeforeAnyWrite() {
        assertThatThrownBy(
                        () ->
                                service.reorder(
                                        actor,
                                        UUID.randomUUID(),
                                        false,
                                        new MaterialCommands.Reorder(
                                                List.of(new MaterialCommands.Position(id, 0L)))))
                .isInstanceOf(ApiException.class);
        verify(repo, never()).update(any(), anyLong());
    }

    @Test
    void classMaterialCreateReplaceUpdateAndArchiveKeepScopeAndAppendContentHistory() {
        UUID classId = UUID.randomUUID();
        when(access.classList(actor, classId, false))
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
                service.create(
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
        verify(repo)
                .insert(
                        eq(created),
                        argThat(v -> v.versionNo() == 1 && v.fileId().equals("private-file")));
        when(repo.find(created.id())).thenReturn(Optional.of(created));

        var replaced =
                service.replace(
                        actor,
                        created.id(),
                        new MaterialCommands.Replace(
                                0L, new MaterialCommands.Source("signed", null)));
        verify(repo)
                .insertVersion(
                        argThat(v -> v.materialId().equals(created.id()) && v.versionNo() == 2));
        assertThat(created.currentVersionNo()).isEqualTo(1);
        assertThat(replaced.currentVersionNo()).isEqualTo(2);
        when(repo.find(created.id())).thenReturn(Optional.of(replaced));
        var updated =
                service.update(
                        actor,
                        created.id(),
                        new MaterialCommands.Update("Updated", "Description", 1, 1L, null));
        assertThat(updated.currentVersionNo()).isEqualTo(2);
        assertThat(updated.classId()).isEqualTo(classId);
        when(repo.find(created.id())).thenReturn(Optional.of(updated));
        var archived = service.archive(actor, created.id(), 2L);
        assertThat(archived.status()).isEqualTo(Material.Status.ARCHIVED);
        assertThat(archived.currentVersionNo()).isEqualTo(2);
        verify(repo, times(1)).insertVersion(any());
        verify(jobs)
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
            when(access.classList(actor, scopeId, false))
                    .thenReturn(
                            new ClassRef(
                                    scopeId,
                                    subject,
                                    "C",
                                    "Class",
                                    ClassStatus.OPEN,
                                    actor.accountId(),
                                    false));
        when(modules.find(foreignModule))
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
                                service.create(
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
        when(repo.find(id)).thenReturn(Optional.of(scoped));
        assertThatThrownBy(
                        () ->
                                service.update(
                                        actor,
                                        id,
                                        new MaterialCommands.Update(
                                                "Moved", "", 0, 0L, foreignModule)))
                .isInstanceOfSatisfying(
                        ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("MATERIAL_MODULE_INVALID"));
        verify(repo, never()).insert(any(), any());
        verify(repo, never()).update(any(), anyLong());
        verifyNoInteractions(files, jobs, audit);
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
        when(repo.find(id)).thenReturn(Optional.of(scoped));
        assertThatThrownBy(
                        () ->
                                service.replace(
                                        actor,
                                        id,
                                        new MaterialCommands.Replace(
                                                1L, new MaterialCommands.Source("signed", null))))
                .isInstanceOfSatisfying(
                        ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("MATERIAL_VERSION_CONFLICT"));
        verifyNoInteractions(files, jobs, audit);
        when(repo.update(any(), anyLong())).thenReturn(false);
        assertThatThrownBy(
                        () ->
                                service.replace(
                                        actor,
                                        id,
                                        new MaterialCommands.Replace(
                                                0L, new MaterialCommands.Source("signed", null))))
                .isInstanceOfSatisfying(
                        ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("MATERIAL_VERSION_CONFLICT"));
        verify(repo, never()).insertVersion(any());
        verifyNoInteractions(jobs, audit);
    }
}
