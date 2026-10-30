package edu.aiplatform.materials.service;

import edu.aiplatform.jobs.contract.*;
import edu.aiplatform.materials.model.*;
import edu.aiplatform.materials.repository.*;
import edu.aiplatform.materials.integration.MaterialTextExtractor;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class MaterialTextExtractJobHandler implements IJobHandler,IPendingSweeper {
    private final IMaterialRepository repository;
    private final MaterialTextExtractor extractor;
    public MaterialTextExtractJobHandler(IMaterialRepository repository,MaterialTextExtractor extractor) {this.repository=repository;this.extractor=extractor;}
    @Override public String jobType() {return JobTypes.MATERIAL_TEXT_EXTRACT;}
    @Override public JobQueue queue() {return JobQueue.TRIGGERED;}
    @Override public Duration timeout() {return Duration.ofSeconds(130);}
    @Override public void handle(JobMessage message) {
        UUID id=id(message);int number=number(message);
        var material=repository.find(id);var version=repository.version(id,number);
        if(material.isEmpty()||version.isEmpty()||material.get().currentVersionNo()!=number
                ||version.get().textStatus()!=MaterialVersion.TextStatus.PENDING)return;
        MaterialTextExtractor.Result result;
        try {result=extractor.extract(version.get().fileId());}
        catch(RuntimeException e) {throw new RetryableJobException("Material extraction temporarily unavailable");}
        String text=result.text().substring(0,Math.min(1_000_000,result.text().length()));
        boolean hasText=(double)text.strip().length()/Math.max(1,result.pages())>=50;
        repository.completeText(id,number,hasText?MaterialVersion.TextStatus.EXTRACTED:MaterialVersion.TextStatus.NO_TEXT,hasText?text:null);
    }
    @Override public void onFailed(JobMessage message,String ignoredError) {
        repository.completeText(id(message),number(message),MaterialVersion.TextStatus.FAILED,null);
    }
    @Override public List<PendingJob> findStalePending(Instant olderThan) {
        return repository.stalePending(olderThan).stream().map(v->new PendingJob("material:"+v.materialId()+":"+v.versionNo(),
                Map.of("materialId",v.materialId().toString(),"versionNo",v.versionNo()))).toList();
    }
    private UUID id(JobMessage message) {return UUID.fromString(Objects.toString(message.payload().get("materialId")));}
    private int number(JobMessage message) {return Integer.parseInt(Objects.toString(message.payload().get("versionNo")));}
}
