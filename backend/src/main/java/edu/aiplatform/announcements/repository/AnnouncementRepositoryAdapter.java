package edu.aiplatform.announcements.repository;
import edu.aiplatform.announcements.model.*;
import edu.aiplatform.announcements.repository.*;
import jakarta.persistence.EntityManager;
import java.util.*;
import org.springframework.stereotype.Repository;

@Repository
public class AnnouncementRepositoryAdapter implements IAnnouncementRepository {
    private final EntityManager em;
    public AnnouncementRepositoryAdapter(EntityManager em) {this.em=em;}
    public Optional<Announcement> find(UUID id) {return Optional.ofNullable(em.find(Announcement.class,id));}
    public List<Announcement> list(List<UUID> classes,int page,int size) {
        if(classes.isEmpty())return List.of();
        return em.createQuery("select a from Announcement a where a.classId in :ids and a.status=:status order by a.createdAt desc,a.id desc",Announcement.class)
                .setParameter("ids",classes).setParameter("status",Announcement.Status.ACTIVE).setFirstResult((page-1)*size).setMaxResults(size).getResultList();
    }
    public void insert(Announcement a) {em.persist(a);}
    public boolean update(Announcement a,long version) {
        // Detach before JPQL: a stale entity must not flush an unconditional update first.
        em.detach(a);
        int changed=em.createQuery("update Announcement a set a.title=:title,a.body=:body,a.status=:status,a.updatedBy=:actor,a.updatedAt=:now,a.deletedBy=:deletedBy,a.deletedAt=:deletedAt,a.version=a.version+1 where a.id=:id and a.version=:version and a.status=:active")
                .setParameter("title",a.title).setParameter("body",a.body).setParameter("status",a.status).setParameter("actor",a.updatedBy)
                .setParameter("now",a.updatedAt).setParameter("deletedBy",a.deletedBy).setParameter("deletedAt",a.deletedAt)
                .setParameter("id",a.id).setParameter("version",version).setParameter("active",Announcement.Status.ACTIVE).executeUpdate();
        if(changed==1)a.version=version+1;return changed==1;
    }
}
