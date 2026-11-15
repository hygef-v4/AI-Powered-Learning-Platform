package edu.aiplatform.announcements.repository;

import edu.aiplatform.announcements.model.*;
import jakarta.persistence.EntityManager;
import java.util.*;
import org.springframework.stereotype.Repository;

@Repository
public class AnnouncementRepositoryAdapter implements IAnnouncementRepository {
    private final EntityManager entityManager;

    public AnnouncementRepositoryAdapter(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Optional<Announcement> findAnnouncement(UUID id) {
        return Optional.ofNullable(entityManager.find(Announcement.class, id));
    }

    public List<Announcement> listAnnouncements(List<UUID> classIds, int page, int size) {
        if (classIds.isEmpty()) return List.of();
        return entityManager.createQuery(
                        "select a from Announcement a where a.classId in :ids and a.status=:status order by a.createdAt desc,a.id desc",
                        Announcement.class)
                .setParameter("ids", classIds)
                .setParameter("status", Announcement.Status.ACTIVE)
                .setFirstResult((page - 1) * size)
                .setMaxResults(size)
                .getResultList();
    }

    public void insertAnnouncement(Announcement announcement) {
        entityManager.persist(announcement);
    }

    public boolean updateAnnouncement(Announcement announcement, long expectedVersion) {
        // Detach before JPQL: a stale entity must not flush an unconditional update first.
        entityManager.detach(announcement);
        int changed =
                entityManager.createQuery(
                                "update Announcement a set a.title=:title,a.body=:body,a.status=:status,a.updatedBy=:actor,a.updatedAt=:now,a.deletedBy=:deletedBy,a.deletedAt=:deletedAt,a.version=a.version+1 where a.id=:id and a.version=:version and a.status=:active")
                        .setParameter("title", announcement.title)
                        .setParameter("body", announcement.body)
                        .setParameter("status", announcement.status)
                        .setParameter("actor", announcement.updatedBy)
                        .setParameter("now", announcement.updatedAt)
                        .setParameter("deletedBy", announcement.deletedBy)
                        .setParameter("deletedAt", announcement.deletedAt)
                        .setParameter("id", announcement.id)
                        .setParameter("version", expectedVersion)
                        .setParameter("active", Announcement.Status.ACTIVE)
                        .executeUpdate();
        if (changed == 1) announcement.version = expectedVersion + 1;
        return changed == 1;
    }
}
