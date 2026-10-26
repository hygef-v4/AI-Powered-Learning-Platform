package edu.aiplatform.materials.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import edu.aiplatform.files.contract.*;
import edu.aiplatform.identity.contract.*;
import edu.aiplatform.materials.domain.*;
import edu.aiplatform.shared.error.ApiException;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;

class MaterialQueryAndReferenceTest {
    IMaterialRepository repo;MaterialAccessPolicy access;IArtifact files;MaterialQueryService queries;ReferenceMaterialService references;
    final UUID id=UUID.randomUUID(),subject=UUID.randomUUID(),classId=UUID.randomUUID();
    final Actor actor=new Actor(UUID.randomUUID(),Role.STUDENT);final Instant now=Instant.now();Material material;
    @BeforeEach void setup() {
        repo=mock(IMaterialRepository.class);access=mock(MaterialAccessPolicy.class);files=mock(IArtifact.class);
        queries=new MaterialQueryService(repo,access,files);references=new ReferenceMaterialService(queries,repo);
        material=new Material(id,Material.Scope.SUBJECT,subject,null,Material.Kind.FILE,"Title","",0,Material.Status.ACTIVE,1,actor.accountId(),now,now,0);
        when(repo.find(id)).thenReturn(Optional.of(material));when(repo.version(id,1)).thenReturn(Optional.of(version(id,"abcdef")));
        when(repo.versions(id)).thenReturn(List.of(version(id,"abcdef")));
    }
    MaterialVersion version(UUID id,String text) {return new MaterialVersion(id,1,"private-storage","doc.pdf","application/pdf",100L,null,null,MaterialVersion.TextStatus.EXTRACTED,text,actor.accountId(),now);}
    @Test void tokenIsIssuedOnlyAfterAuthorization() {
        queries.download(actor,id,classId,null);
        var order=inOrder(access,files);order.verify(access).read(actor,material,classId);order.verify(files).issueDownloadToken("private-storage",actor.accountId());
    }
    @Test void deniedDownloadNeverCallsStorage() {
        doThrow(ApiException.notFound("MATERIAL_NOT_FOUND")).when(access).read(actor,material,classId);
        assertThatThrownBy(()->queries.download(actor,id,classId,null)).isInstanceOf(ApiException.class);verifyNoInteractions(files);
    }
    @Test void studentsCannotDownloadOldVersions() {
        assertThatThrownBy(()->queries.download(actor,id,classId,2)).isInstanceOf(ApiException.class);verifyNoInteractions(files);
    }
    @Test void publicDtoDoesNotExposeStorageIdOrExtractedText() {
        var detail=queries.detail(actor,id,classId);
        assertThat(detail.toString()).doesNotContain("private-storage","abcdef");assertThat(detail.canManage()).isFalse();
    }
    @Test void extractionUsesTotalBudgetAcrossMaterials() {
        UUID second=UUID.randomUUID();Material next=new Material(second,material.scope(),subject,null,material.kind(),"Second","",1,material.status(),1,actor.accountId(),now,now,0);
        when(repo.find(second)).thenReturn(Optional.of(next));when(repo.version(second,1)).thenReturn(Optional.of(version(second,"123456")));
        var texts=references.extractText(actor,List.of(id,second),8);
        assertThat(texts.get(0).text()).isEqualTo("abcdef");assertThat(texts.get(0).truncated()).isFalse();
        assertThat(texts.get(1).text()).isEqualTo("12");assertThat(texts.get(1).truncated()).isTrue();
    }
    @Test void checksAllIdsBeforeReturningOrReadingAnyText() {
        UUID denied=UUID.randomUUID();when(repo.find(denied)).thenReturn(Optional.empty());
        assertThatThrownBy(()->references.extractText(actor,List.of(id,denied),100)).isInstanceOf(ApiException.class);
        verify(repo,never()).version(any(),anyInt());
    }
    @Test void nonExtractedFileProducesNoInventedText() {
        when(repo.version(id,1)).thenReturn(Optional.of(new MaterialVersion(id,1,"private","d.pdf","application/pdf",100L,null,null,MaterialVersion.TextStatus.NO_TEXT,null,actor.accountId(),now)));
        assertThat(references.extractText(actor,List.of(id),100).get(0).text()).isEmpty();
    }
    @Test void rejectsMoreThanTenIdsOrInvalidBudget() {
        assertThatThrownBy(()->references.extractText(actor,Collections.nCopies(11,id),100)).isInstanceOf(ApiException.class);
        assertThatThrownBy(()->references.extractText(actor,List.of(id),0)).isInstanceOf(ApiException.class);
        verifyNoInteractions(repo);
    }
    @Test void mcpRejectsUnauthorizedIdAndInvalidArguments() {
        var tools=new MaterialMcpTools(queries,repo);
        doThrow(ApiException.notFound("MATERIAL_NOT_FOUND")).when(access).read(actor,material,null);
        assertThat(tools.call(actor,"read_authorized_material",Map.of("materialId",id.toString())).error()).isTrue();
        assertThat(tools.call(actor,"read_authorized_material",Map.of("materialId","bad")).error()).isTrue();
        assertThat(tools.call(actor,"read_authorized_material",Map.of("materialId",id.toString(),"actorId","fake")).error()).isTrue();
        verify(repo,never()).version(any(),anyInt());
    }
    @Test void mcpReturnsActualVersionAndUntrustedMarker() {
        var result=new MaterialMcpTools(queries,repo).call(new Actor(actor.accountId(),Role.TEACHER),"read_authorized_material",Map.of("materialId",id.toString()));
        assertThat(result.error()).isFalse();Map<?,?> content=(Map<?,?>)result.content();
        assertThat(content.get("untrustedContent")).isEqualTo(true);assertThat(content.get("versionNo")).isEqualTo(1);
    }
    @Test void mcpPagesWithoutSplittingSurrogatePairsAndRejectsChangedVersions() {
        Actor teacher=new Actor(actor.accountId(),Role.TEACHER);
        when(repo.version(id,1)).thenReturn(Optional.of(version(id,"a😀bc")));
        var tools=new MaterialMcpTools(queries,repo);
        var first=tools.call(teacher,"read_authorized_material",Map.of("materialId",id.toString(),"limit",2));
        assertThat(first.error()).isFalse();
        var content=(Map<?,?>)first.content();
        assertThat(content.get("text")).isEqualTo("a");
        assertThat(content.get("nextOffset")).isEqualTo(1);
        var second=tools.call(teacher,"read_authorized_material",Map.of("materialId",id.toString(),"offset",1,"limit",2,"expectedVersion",1));
        assertThat(((Map<?,?>)second.content()).get("text")).isEqualTo("😀");
        assertThat(tools.call(teacher,"read_authorized_material",Map.of("materialId",id.toString(),"expectedVersion",2)).error()).isTrue();
    }
    @Test void staffMcpCanReadArchivedReferenceButNewAiSelectionCannotUseIt() {
        Material archived=material.revise(material.title(),"",0,1,Material.Status.ARCHIVED,now);
        when(repo.find(id)).thenReturn(Optional.of(archived));Actor teacher=new Actor(actor.accountId(),Role.TEACHER);
        var result=new MaterialMcpTools(queries,repo).call(teacher,"read_authorized_material",Map.of("materialId",id.toString()));
        assertThat(result.error()).isFalse();assertThat(((Map<?,?>)result.content()).get("text")).isEqualTo("abcdef");
        assertThatThrownBy(()->references.extractText(teacher,List.of(id),100)).isInstanceOf(ApiException.class);
    }
}
