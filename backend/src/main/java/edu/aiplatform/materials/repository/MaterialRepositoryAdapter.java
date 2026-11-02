package edu.aiplatform.materials.repository;

import edu.aiplatform.materials.model.*;
import edu.aiplatform.materials.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly=true, timeout=10)
public class MaterialRepositoryAdapter implements IMaterialRepository {
    @PersistenceContext private EntityManager em;
    @Override public Optional<Material> find(UUID id) {
        MaterialRow row=em.find(MaterialRow.class,id);
        if(row==null) return Optional.empty();
        Material result=row.snapshot(); em.detach(row); return Optional.of(result);
    }
    @Override public List<Material> list(UUID subjectId, UUID classId) {
        var query=em.createQuery("select m from ReferenceMaterialRow m where m.subjectId=:subject and "
                + (classId==null ? "m.classId is null" : "(m.classId is null or m.classId=:classId)")
                + " order by m.scope desc,m.orderNo,m.id",MaterialRow.class).setParameter("subject",subjectId);
        if(classId!=null) query.setParameter("classId",classId);
        return query.getResultList().stream().map(row->{Material m=row.snapshot();em.detach(row);return m;}).toList();
    }
    @Override @Transactional(timeout=10) public void insert(Material material, MaterialVersion version) {
        var row=MaterialRow.of(material);em.persist(row);em.flush();em.detach(row);insertVersion(version);
    }
    @Override @Transactional(timeout=10) public boolean update(Material m,long expectedVersion) {
        return em.createQuery("update ReferenceMaterialRow m set m.moduleId=:module,m.title=:title,m.description=:description,"
                + "m.orderNo=:orderNo,m.status=:status,m.currentVersionNo=:contentVersion,m.updatedAt=:now,"
                + "m.version=:nextVersion where m.id=:id and m.version=:expected")
                .setParameter("module",m.moduleId()).setParameter("title",m.title()).setParameter("description",m.description())
                .setParameter("orderNo",m.orderNo()).setParameter("status",m.status())
                .setParameter("contentVersion",m.currentVersionNo()).setParameter("now",m.updatedAt())
                .setParameter("nextVersion",m.version()).setParameter("id",m.id())
                .setParameter("expected",expectedVersion).executeUpdate()==1;
    }
    @Override @Transactional(timeout=10) public void insertVersion(MaterialVersion version) {
        var row=MaterialVersionRow.of(version);em.persist(row);em.flush();em.detach(row);
    }
    @Override public List<MaterialVersion> versions(UUID id) {
        return em.createQuery("select v from ReferenceMaterialVersionRow v where v.materialId=:id order by v.versionNo desc",
                MaterialVersionRow.class).setParameter("id",id).getResultList().stream()
                .map(row->{var v=row.snapshot();em.detach(row);return v;}).toList();
    }
    @Override public Optional<MaterialVersion> version(UUID id,int versionNo) {
        var row=em.find(MaterialVersionRow.class,new MaterialVersionRow.Key(id,versionNo));
        if(row==null)return Optional.empty();var v=row.snapshot();em.detach(row);return Optional.of(v);
    }
    @Override @Transactional(timeout=10) public boolean completeText(UUID id,int versionNo,MaterialVersion.TextStatus status,String text) {
        return em.createQuery("update ReferenceMaterialVersionRow v set v.textStatus=:status,v.extractedText=:text "
                + "where v.materialId=:id and v.versionNo=:version and v.textStatus=:pending")
                .setParameter("status",status).setParameter("text",text).setParameter("id",id)
                .setParameter("version",versionNo).setParameter("pending",MaterialVersion.TextStatus.PENDING)
                .executeUpdate()==1;
    }
    @Override public List<MaterialVersion> stalePending(Instant olderThan) {
        return em.createQuery("select v from ReferenceMaterialVersionRow v, ReferenceMaterialRow m "
                + "where v.materialId=m.id and v.versionNo=m.currentVersionNo and v.textStatus=:pending "
                + "and v.createdAt<:before order by v.createdAt",MaterialVersionRow.class)
                .setParameter("pending",MaterialVersion.TextStatus.PENDING).setParameter("before",olderThan)
                .setMaxResults(100).getResultList().stream()
                .map(row->{var v=row.snapshot();em.detach(row);return v;}).toList();
    }
}
