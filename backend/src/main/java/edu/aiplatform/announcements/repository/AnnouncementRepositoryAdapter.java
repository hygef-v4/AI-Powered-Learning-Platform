package edu.aiplatform.announcements.repository;

import edu.aiplatform.announcements.model.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class AnnouncementRepositoryAdapter implements IAnnouncementRepository {
    private static final String COLUMNS =
            "id,class_id,title,body,important,send_email,status,created_by,created_at,"
                    + "updated_by,updated_at,deleted_by,deleted_at,version";
    private final JdbcClient jdbcClient;

    public AnnouncementRepositoryAdapter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public Optional<Announcement> findAnnouncement(UUID id) {
        return jdbcClient
                .sql("select " + COLUMNS + " from announcements where id = :id")
                .param("id", id)
                .query(AnnouncementRepositoryAdapter::mapAnnouncement)
                .optional();
    }

    public List<Announcement> listAnnouncements(List<UUID> classIds, int page, int size) {
        if (classIds.isEmpty()) return List.of();
        return jdbcClient
                .sql("select " + COLUMNS + " from announcements "
                        + "where class_id in (:classIds) and status = :status "
                        + "order by created_at desc,id desc offset :offset limit :size")
                .param("classIds", classIds)
                .param("status", Announcement.Status.ACTIVE.name())
                .param("offset", (page - 1) * size)
                .param("size", size)
                .query(AnnouncementRepositoryAdapter::mapAnnouncement)
                .list();
    }

    public void insertAnnouncement(Announcement announcement) {
        jdbcClient
                .sql("insert into announcements(" + COLUMNS + ") values (:id,:classId,:title,"
                        + ":body,:important,:sendEmail,:status,:createdBy,:createdAt,:updatedBy,"
                        + ":updatedAt,:deletedBy,:deletedAt,:version)")
                .param("id", announcement.id)
                .param("classId", announcement.classId)
                .param("title", announcement.title)
                .param("body", announcement.body)
                .param("important", announcement.important)
                .param("sendEmail", announcement.sendEmail)
                .param("status", announcement.status.name())
                .param("createdBy", announcement.createdBy)
                .param("createdAt", toTimestamp(announcement.createdAt))
                .param("updatedBy", announcement.updatedBy)
                .param("updatedAt", toTimestamp(announcement.updatedAt))
                .param("deletedBy", announcement.deletedBy)
                .param("deletedAt", toTimestamp(announcement.deletedAt))
                .param("version", announcement.version)
                .update();
    }

    public boolean updateAnnouncement(Announcement announcement, long expectedVersion) {
        int changed =
                jdbcClient
                        .sql("update announcements set title = :title, body = :body, "
                                + "status = :status, updated_by = :actor, updated_at = :now, "
                                + "deleted_by = :deletedBy, deleted_at = :deletedAt, "
                                + "version = version + 1 "
                                + "where id = :id and version = :version and status = :active")
                        .param("title", announcement.title)
                        .param("body", announcement.body)
                        .param("status", announcement.status.name())
                        .param("actor", announcement.updatedBy)
                        .param("now", toTimestamp(announcement.updatedAt))
                        .param("deletedBy", announcement.deletedBy)
                        .param("deletedAt", toTimestamp(announcement.deletedAt))
                        .param("id", announcement.id)
                        .param("version", expectedVersion)
                        .param("active", Announcement.Status.ACTIVE.name())
                        .update();
        if (changed == 1) announcement.version = expectedVersion + 1;
        return changed == 1;
    }

    private static Timestamp toTimestamp(Instant time) {
        return time == null ? null : Timestamp.from(time);
    }

    private static Instant readInstant(ResultSet row, String column) throws SQLException {
        Timestamp time = row.getTimestamp(column);
        return time == null ? null : time.toInstant();
    }

    private static Announcement mapAnnouncement(ResultSet row, int rowNumber)
            throws SQLException {
        var announcement = new Announcement();
        announcement.id = row.getObject("id", UUID.class);
        announcement.classId = row.getObject("class_id", UUID.class);
        announcement.title = row.getString("title");
        announcement.body = row.getString("body");
        announcement.important = row.getBoolean("important");
        announcement.sendEmail = row.getBoolean("send_email");
        announcement.status = Announcement.Status.valueOf(row.getString("status"));
        announcement.createdBy = row.getObject("created_by", UUID.class);
        announcement.createdAt = readInstant(row, "created_at");
        announcement.updatedBy = row.getObject("updated_by", UUID.class);
        announcement.updatedAt = readInstant(row, "updated_at");
        announcement.deletedBy = row.getObject("deleted_by", UUID.class);
        announcement.deletedAt = readInstant(row, "deleted_at");
        announcement.version = row.getLong("version");
        return announcement;
    }
}
