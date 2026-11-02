package edu.aiplatform.announcements.domain;
import java.util.*;
public interface IAnnouncementRepository {
    Optional<Announcement> find(UUID id);
    List<Announcement> list(List<UUID> classes,int page,int size);
    void insert(Announcement announcement);
    boolean update(Announcement announcement,long expectedVersion);
}
