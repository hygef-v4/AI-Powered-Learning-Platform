package edu.aiplatform.materials.application;

import edu.aiplatform.audit.contract.*;
import edu.aiplatform.files.contract.*;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.jobs.contract.*;
import edu.aiplatform.materials.domain.*;
import edu.aiplatform.shared.error.ApiException;
import java.time.Clock;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(timeout=10)
public class MaterialService {
    private final IMaterialRepository repository;
    private final MaterialAccessPolicy access;
    private final IArtifact artifacts;
    private final IJob jobs;
    private final IAudit audit;
    private final Clock clock;
    public MaterialService(IMaterialRepository repository,MaterialAccessPolicy access,IArtifact artifacts,IJob jobs,IAudit audit,Clock clock) {
        this.repository=repository;this.access=access;this.artifacts=artifacts;this.jobs=jobs;this.audit=audit;this.clock=clock;
    }
    public Material create(Actor actor,UUID scopeId,boolean classScope,MaterialCommands.Create input) {
        UUID subjectId=classScope?access.classList(actor,scopeId,false).subjectId():scopeId;
        if(!classScope)access.subjectList(actor,subjectId);
        Material material=new Material(UUID.randomUUID(),classScope?Material.Scope.CLASS:Material.Scope.SUBJECT,
                subjectId,classScope?scopeId:null,input.kind(),input.title(),input.description(),input.orderNo(),
                Material.Status.ACTIVE,1,actor.accountId(),clock.instant(),clock.instant(),0);
        access.write(actor,material);
        MaterialVersion version=source(actor,material,1,input.source());
        repository.insert(material,version); enqueue(version);record(actor,"MATERIAL_CREATED",material);return material;
    }
    public Material update(Actor actor,UUID id,MaterialCommands.Update input) {
        Material m=writable(actor,id,input.version());
        Material next=m.revise(input.title(),input.description(),input.orderNo(),m.currentVersionNo(),m.status(),clock.instant());
        save(next,m.version());record(actor,"MATERIAL_UPDATED",next);return next;
    }
    public Material replace(Actor actor,UUID id,MaterialCommands.Replace input) {
        Material m=writable(actor,id,input.version());
        MaterialVersion version=source(actor,m,m.currentVersionNo()+1,input.source());
        Material next=m.revise(m.title(),m.description(),m.orderNo(),version.versionNo(),m.status(),clock.instant());
        save(next,m.version());repository.insertVersion(version);enqueue(version);record(actor,"MATERIAL_VERSION_CREATED",next);return next;
    }
    public Material archive(Actor actor,UUID id,long expected) {
        Material m=writable(actor,id,expected);
        Material next=m.revise(m.title(),m.description(),m.orderNo(),m.currentVersionNo(),Material.Status.ARCHIVED,clock.instant());
        save(next,m.version());record(actor,"MATERIAL_ARCHIVED",next);return next;
    }
    public void reorder(Actor actor,UUID scopeId,boolean classScope,MaterialCommands.Reorder input) {
        if(classScope)access.classList(actor,scopeId,false);else access.subjectList(actor,scopeId);
        Set<UUID> seen=new HashSet<>();List<Material> rows=new ArrayList<>();
        for(var position:input.items()) {
            if(!seen.add(position.id()))throw ApiException.badRequest("MATERIAL_REORDER_INVALID","Danh sách có học liệu trùng");
            Material m=writable(actor,position.id(),position.version());
            if(classScope ? !scopeId.equals(m.classId()) : m.classId()!=null||!scopeId.equals(m.subjectId()))
                throw ApiException.notFound("MATERIAL_NOT_FOUND");
            rows.add(m);
        }
        // Lock/update in UUID order to avoid deadlocks between two reorder transactions.
        Map<UUID,Integer> positions=new HashMap<>();for(int i=0;i<rows.size();i++)positions.put(rows.get(i).id(),i);
        rows.sort(Comparator.comparing(Material::id));
        for(Material m:rows) {
            Material next=m.revise(m.title(),m.description(),positions.get(m.id()),m.currentVersionNo(),m.status(),clock.instant());
            save(next,m.version());record(actor,"MATERIAL_UPDATED",next);
        }
    }
    private Material writable(Actor actor,UUID id,long expected) {
        Material m=repository.find(id).orElseThrow(()->ApiException.notFound("MATERIAL_NOT_FOUND"));
        access.write(actor,m);if(expected!=m.version())throw conflict();return m;
    }
    private void save(Material m,long expected) { if(!repository.update(m,expected))throw conflict(); }
    private ApiException conflict() { return ApiException.conflict("MATERIAL_VERSION_CONFLICT","Học liệu đã thay đổi, vui lòng tải lại"); }
    private MaterialVersion source(Actor actor,Material m,int number,MaterialCommands.Source input) {
        if(input==null)throw ApiException.badRequest("MATERIAL_SOURCE_REQUIRED","Chọn tệp hoặc đường dẫn video");
        if(m.kind()==Material.Kind.YOUTUBE_LINK) {
            if(input.fileRef()!=null)throw ApiException.badRequest("MATERIAL_SOURCE_INVALID","Nguồn không phù hợp loại học liệu");
            YoutubeVideo video;
            try{video=YoutubeVideo.parse(input.videoUrl());}catch(IllegalArgumentException e){throw ApiException.badRequest("MATERIAL_YOUTUBE_INVALID",e.getMessage());}
            return new MaterialVersion(m.id(),number,null,null,null,null,video.url(),video.id(),null,null,actor.accountId(),clock.instant());
        }
        if(input.videoUrl()!=null||input.fileRef()==null||input.fileRef().isBlank())
            throw ApiException.badRequest("MATERIAL_SOURCE_INVALID","Chọn tệp đã tải lên");
        StoredFileInfo file=artifacts.attach(new FileRef(input.fileRef()),actor,ArtifactPurpose.MATERIAL);
        if(file==null||file.purpose()!=ArtifactPurpose.MATERIAL||!actor.accountId().equals(file.ownerAccountId()))
            throw ApiException.badRequest("MATERIAL_FILE_INVALID","Tệp không phù hợp");
        return new MaterialVersion(m.id(),number,file.fileId(),file.originalFileName(),file.mediaType(),file.byteSize(),
                null,null,MaterialVersion.TextStatus.PENDING,null,actor.accountId(),clock.instant());
    }
    private void enqueue(MaterialVersion v) {
        if(v.fileId()!=null)jobs.enqueue(JobTypes.MATERIAL_TEXT_EXTRACT,
                Map.of("materialId",v.materialId().toString(),"versionNo",v.versionNo()),"material:"+v.materialId()+":"+v.versionNo());
    }
    private void record(Actor actor,String action,Material m) {
        audit.record(new AuditEvent(UUID.randomUUID(),actor.accountId(),action,"MATERIAL",m.id().toString(),
                AuditResult.SUCCESS,null,Map.of("versionNo",m.currentVersionNo(),"version",m.version()),clock.instant()));
    }
}
