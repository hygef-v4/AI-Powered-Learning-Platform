package edu.aiplatform.materials.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import edu.aiplatform.jobs.contract.*;
import edu.aiplatform.materials.domain.*;
import edu.aiplatform.materials.infrastructure.MaterialTextExtractor;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;

class MaterialTextExtractJobHandlerTest {
    final UUID id=UUID.randomUUID(),subject=UUID.randomUUID(),actor=UUID.randomUUID();final Instant now=Instant.now();
    IMaterialRepository repo;MaterialTextExtractor extractor;MaterialTextExtractJobHandler handler;Material material;MaterialVersion version;
    @BeforeEach void setup() {
        repo=mock(IMaterialRepository.class);extractor=mock(MaterialTextExtractor.class);handler=new MaterialTextExtractJobHandler(repo,extractor);
        material=new Material(id,Material.Scope.SUBJECT,subject,null,new UUID(0,1),Material.Kind.FILE,"Title","",0,Material.Status.ACTIVE,1,actor,now,now,0);
        version=new MaterialVersion(id,1,"file","d.pdf","application/pdf",100L,null,null,MaterialVersion.TextStatus.PENDING,null,actor,now);
        when(repo.find(id)).thenReturn(Optional.of(material));when(repo.version(id,1)).thenReturn(Optional.of(version));
    }
    JobMessage message() {return new JobMessage(1,JobTypes.MATERIAL_TEXT_EXTRACT,"key",Map.of("materialId",id.toString(),"versionNo",1),0,"correlation");}
    @Test void extractsTextIntoExactVersion() {
        when(extractor.extract("file")).thenReturn(new MaterialTextExtractor.Result("a".repeat(100),1));handler.handle(message());
        verify(repo).completeText(id,1,MaterialVersion.TextStatus.EXTRACTED,"a".repeat(100));
    }
    @Test void sparsePagesProduceNoText() {
        when(extractor.extract("file")).thenReturn(new MaterialTextExtractor.Result("a".repeat(100),3));handler.handle(message());
        verify(repo).completeText(id,1,MaterialVersion.TextStatus.NO_TEXT,null);
    }
    @Test void supersededBeforeStartSkipsStorageAndWrite() {
        when(repo.find(id)).thenReturn(Optional.of(material.revise("Title","",0,2,material.status(),now)));
        handler.handle(message());verifyNoInteractions(extractor);verify(repo,never()).completeText(any(),anyInt(),any(),any());
    }
    @Test void replacementDuringParsingCannotOverwriteNewVersion() {
        when(extractor.extract("file")).thenAnswer(call->{when(repo.find(id)).thenReturn(Optional.of(material.revise("Title","",0,2,material.status(),now)));return new MaterialTextExtractor.Result("a".repeat(100),1);});
        handler.handle(message());verify(repo).completeText(id,1,MaterialVersion.TextStatus.EXTRACTED,"a".repeat(100));
        verify(repo,never()).update(any(),anyLong());verify(repo,never()).completeText(eq(id),eq(2),any(),any());
    }
    @Test void completedVersionIsIdempotent() {
        when(repo.version(id,1)).thenReturn(Optional.of(new MaterialVersion(id,1,"file","d.pdf","application/pdf",100L,null,null,MaterialVersion.TextStatus.EXTRACTED,"done",actor,now)));
        handler.handle(message());verifyNoInteractions(extractor);
    }
    @Test void transientParserFailureUsesU03RetryContract() {
        when(extractor.extract("file")).thenThrow(new IllegalStateException("private data"));
        assertThatThrownBy(()->handler.handle(message())).isInstanceOf(RetryableJobException.class).hasMessageNotContaining("private data");
        verify(repo,never()).completeText(any(),anyInt(),any(),any());
    }
    @Test void exhaustedRetriesMarkOnlyPendingExactVersionFailed() {
        handler.onFailed(message(),"private");verify(repo).completeText(id,1,MaterialVersion.TextStatus.FAILED,null);
    }
    @Test void sweeperReusesIdempotencyKeyAndPayload() {
        when(repo.stalePending(now)).thenReturn(List.of(version));var pending=handler.findStalePending(now).get(0);
        assertThat(pending.idempotencyKey()).isEqualTo("material:"+id+":1");assertThat(pending.payload()).containsEntry("versionNo",1);
    }
}
