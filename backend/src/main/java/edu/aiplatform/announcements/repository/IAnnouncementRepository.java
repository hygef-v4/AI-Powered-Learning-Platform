package edu.aiplatform.announcements.repository;

import edu.aiplatform.announcements.model.Announcement;
import java.util.*;

public interface IAnnouncementRepository {
    Optional<Announcement> findAnnouncement(UUID id);

    List<Announcement> listAnnouncements(List<UUID> classIds, int page, int size);

    void insertAnnouncement(Announcement announcement);

    boolean updateAnnouncement(Announcement announcement, long expectedVersion);
}
