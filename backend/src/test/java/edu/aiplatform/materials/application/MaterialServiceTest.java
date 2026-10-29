package edu.aiplatform.materials.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import edu.aiplatform.audit.contract.*;
import edu.aiplatform.files.contract.*;
import edu.aiplatform.identity.contract.*;
import edu.aiplatform.jobs.contract.*;
import edu.aiplatform.materials.domain.*;
import edu.aiplatform.shared.error.ApiException;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;

class MaterialServiceTest {
    final UUID id=UUID.randomUUID(),subject=UUID.randomUUID();final Actor actor=new Actor(UUID.randomUUID(),Role.SUBJECT_MANAGER);
    final Instant now=Instant.parse("2026-10-21T00:00:00Z");
    IMaterialRepository repo;IMaterialModuleRepository modules;MaterialAccessPolicy access;IArtifact files;IJob jobs;IAudit audit;MaterialService service;
    Material material;
    @BeforeEach void setup() {
        repo=mock(IMaterialRepository.class);access=mock(MaterialAccessPolicy.class);files=mock(IArtifact.class);jobs=mock(IJob.class);audit=mock(IAudit.class);
        modules=mock(IMaterialModuleRepository.class);
        when(modules.find(new UUID(0,1))).thenReturn(Optional.of(new MaterialModule(new UUID(0,1),subject,"Module 1",0,actor.accountId(),now,now,0)));
        service=new MaterialService(repo,modules,access,files,jobs,audit,Clock.fixed(now,ZoneOffset.UTC));
        material=new Material(id,Material.Scope.SUBJECT,subject,null,new UUID(0,1),Material.Kind.FILE,"Title","",0,Material.Status.ACTIVE,1,actor.accountId(),now,now,0);
        when(repo.find(id)).thenReturn(Optional.of(material));when(repo.update(any(),anyLong())).thenReturn(true);
        when(files.attach(any(),eq(actor),eq(ArtifactPurpose.MATERIAL))).thenReturn(new StoredFileInfo("private-file",actor.accountId(),ArtifactPurpose.MATERIAL,"doc.pdf","application/pdf",100L,"sha"));
    }
    @Test void createAttachesTrustedFileAndEnqueuesVersionedJob() {
        var m=service.create(actor,subject,false,new MaterialCommands.Create(Material.Kind.FILE,"Title","",0,new MaterialCommands.Source("signed",null),new UUID(0,1)));
        verify(files).attach(new FileRef("signed"),actor,ArtifactPurpose.MATERIAL);
        verify(repo).insert(eq(m),argThat(v->v.fileId().equals("private-file")&&v.textStatus()==MaterialVersion.TextStatus.PENDING));
        verify(jobs).enqueue(eq(JobTypes.MATERIAL_TEXT_EXTRACT),eq(Map.of("materialId",m.id().toString(),"versionNo",1)),eq("material:"+m.id()+":1"));
        verify(audit).record(argThat(e->e.action().equals("MATERIAL_CREATED")));
    }
    @Test void youtubeCreateDoesNotCallFilesOrJobs() {
        service.create(actor,subject,false,new MaterialCommands.Create(Material.Kind.YOUTUBE_LINK,"Video","",0,new MaterialCommands.Source(null,"https://youtu.be/abcdefghijk"),new UUID(0,1)));
        verifyNoInteractions(files,jobs);verify(repo).insert(any(),argThat(v->v.videoId().equals("abcdefghijk")&&v.textStatus()==null));
    }
    @Test void replaceAppendsWithoutChangingOldVersion() {
        var next=service.replace(actor,id,new MaterialCommands.Replace(0L,new MaterialCommands.Source("signed",null)));
        assertThat(next.currentVersionNo()).isEqualTo(2);assertThat(material.currentVersionNo()).isEqualTo(1);
        verify(repo).insertVersion(argThat(v->v.versionNo()==2));verify(repo,never()).completeText(any(),anyInt(),any(),any());
    }
    @Test void metadataUpdateDoesNotCreateContentVersionOrJob() {
        var next=service.update(actor,id,new MaterialCommands.Update("New","Description",3,0L,null));
        assertThat(next.currentVersionNo()).isEqualTo(1);assertThat(next.version()).isEqualTo(1);
        verify(repo,never()).insertVersion(any());verifyNoInteractions(files,jobs);
    }
    @Test void archiveKeepsHistoricalVersions() {
        assertThat(service.archive(actor,id,0).status()).isEqualTo(Material.Status.ARCHIVED);
        verify(repo,never()).insertVersion(any());verifyNoInteractions(files,jobs);
    }
    @Test void staleVersionDoesNotAttachOrEnqueue() {
        assertThatThrownBy(()->service.replace(actor,id,new MaterialCommands.Replace(8L,new MaterialCommands.Source("signed",null))))
                .isInstanceOfSatisfying(ApiException.class,e->assertThat(e.status().value()).isEqualTo(409));
        verifyNoInteractions(files,jobs,audit);verify(repo,never()).update(any(),anyLong());
    }
    @Test void lostCompareAndSwapDoesNotInsertVersionOrEnqueue() {
        when(repo.update(any(),anyLong())).thenReturn(false);
        assertThatThrownBy(()->service.replace(actor,id,new MaterialCommands.Replace(0L,new MaterialCommands.Source("signed",null)))).isInstanceOf(ApiException.class);
        verify(repo,never()).insertVersion(any());verifyNoInteractions(jobs,audit);
    }
    @Test void deniedMutationDoesNotTouchArtifactOrWrite() {
        doThrow(ApiException.notFound("MATERIAL_NOT_FOUND")).when(access).write(actor,material);
        assertThatThrownBy(()->service.replace(actor,id,new MaterialCommands.Replace(0L,new MaterialCommands.Source("signed",null)))).isInstanceOf(ApiException.class);
        verifyNoInteractions(files,jobs,audit);verify(repo,never()).update(any(),anyLong());
    }
    @Test void duplicateReorderRejectsBeforeAnyWrite() {
        var row=new MaterialCommands.Position(id,0L);
        assertThatThrownBy(()->service.reorder(actor,subject,false,new MaterialCommands.Reorder(List.of(row,row)))).isInstanceOf(ApiException.class);
        verify(repo,never()).update(any(),anyLong());
    }
    @Test void foreignScopeReorderRejectsBeforeAnyWrite() {
        assertThatThrownBy(()->service.reorder(actor,UUID.randomUUID(),false,new MaterialCommands.Reorder(List.of(new MaterialCommands.Position(id,0L))))).isInstanceOf(ApiException.class);
        verify(repo,never()).update(any(),anyLong());
    }
}
