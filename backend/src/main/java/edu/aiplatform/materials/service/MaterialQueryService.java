package edu.aiplatform.materials.service;

import edu.aiplatform.files.contract.*;
import edu.aiplatform.identity.contract.*;
import edu.aiplatform.materials.model.*;
import edu.aiplatform.materials.repository.*;
import edu.aiplatform.shared.error.ApiException;
import edu.aiplatform.subjects.contract.SubjectStatus;
import edu.aiplatform.academics.contract.ClassStatus;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly=true,timeout=10,noRollbackFor=ApiException.class)
public class MaterialQueryService {
    private final IMaterialRepository repository;
    private final MaterialAccessPolicy access;
    private final IArtifact artifacts;
    public MaterialQueryService(IMaterialRepository repository,MaterialAccessPolicy access,IArtifact artifacts) {
        this.repository=repository;this.access=access;this.artifacts=artifacts;
    }
    public List<MaterialViews.Detail> subjectList(Actor actor,UUID subjectId) {
        access.subjectList(actor,subjectId);
        return repository.list(subjectId,null).stream().map(m->view(actor,m,false,null)).toList();
    }
    public List<MaterialViews.Detail> classList(Actor actor,UUID classId,boolean student,List<UUID> ids) {
        if(ids!=null&&ids.size()>100)throw ApiException.badRequest("MATERIAL_IDS_LIMIT","Tối đa 100 học liệu");
        var c=access.classList(actor,classId,student);
        return repository.list(c.subjectId(),classId).stream()
                .filter(m->!student||m.status()==Material.Status.ACTIVE)
                .filter(m->ids==null||ids.contains(m.id())).map(m->view(actor,m,false,classId)).toList();
    }
    public Material authorized(Actor actor,UUID id,UUID classId) {
        Material m=repository.find(id).orElseThrow(()->ApiException.notFound("MATERIAL_NOT_FOUND"));
        access.read(actor,m,classId);return m;
    }
    public MaterialViews.Detail detail(Actor actor,UUID id,UUID classId) {
        return view(actor,authorized(actor,id,classId),true,classId);
    }
    public DownloadToken download(Actor actor,UUID id,UUID classId,Integer versionNo) {
        Material m=authorized(actor,id,classId);
        int number=versionNo==null?m.currentVersionNo():versionNo;
        if(m.kind()!=Material.Kind.FILE || (actor.role()==Role.STUDENT && number!=m.currentVersionNo()))
            throw ApiException.notFound("MATERIAL_NOT_FOUND");
        MaterialVersion v=repository.version(id,number).orElseThrow(()->ApiException.notFound("MATERIAL_NOT_FOUND"));
        return artifacts.issueDownloadToken(v.fileId(),actor.accountId());
    }
    private MaterialViews.Detail view(Actor actor,Material m,boolean history,UUID contextClassId) {
        MaterialVersion current=repository.version(m.id(),m.currentVersionNo()).orElseThrow(()->ApiException.notFound("MATERIAL_NOT_FOUND"));
        boolean editable=actor.role()!=Role.STUDENT && !(contextClassId!=null&&m.scope()==Material.Scope.SUBJECT)
                && m.status()==Material.Status.ACTIVE && access.canManage(actor,m)
                && access.subject(m.subjectId()).status()==SubjectStatus.ACTIVE
                && (m.classId()==null||access.courseClass(m.classId()).status()!=ClassStatus.ARCHIVED);
        List<MaterialViews.Version> versions=!history?List.of():actor.role()==Role.STUDENT?List.of(MaterialViews.Version.of(current))
                :repository.versions(m.id()).stream().map(MaterialViews.Version::of).toList();
        return new MaterialViews.Detail(m.id(),m.scope(),m.subjectId(),m.classId(),m.moduleId(),m.kind(),m.title(),m.description(),m.orderNo(),
                m.status(),m.currentVersionNo(),m.version(),m.updatedAt(),editable,MaterialViews.Version.of(current),versions);
    }
}
