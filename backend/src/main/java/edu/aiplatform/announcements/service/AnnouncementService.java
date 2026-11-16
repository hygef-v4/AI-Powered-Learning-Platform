package edu.aiplatform.announcements.service;

import edu.aiplatform.announcements.dto.AnnouncementViews;
import edu.aiplatform.announcements.model.*;
import edu.aiplatform.announcements.repository.*;
import edu.aiplatform.academics.contract.*;
import edu.aiplatform.audit.contract.*;
import edu.aiplatform.identity.contract.*;
import edu.aiplatform.jobs.contract.*;
import edu.aiplatform.shared.error.ApiException;
import java.time.Clock;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(timeout = 10)
public class AnnouncementService {
    private final IAnnouncementRepository announcementRepository;
    private final AnnouncementAccessPolicy announcementAccessPolicy;
    private final IClassAccess classAccess;
    private final IAccountLookup accountLookup;
    private final IAudit audit;
    private final IEventPublisher eventPublisher;
    private final Clock clock;

    public AnnouncementService(
            IAnnouncementRepository announcementRepository,
            AnnouncementAccessPolicy announcementAccessPolicy,
            IClassAccess classAccess,
            IAccountLookup accountLookup,
            IAudit audit,
            IEventPublisher eventPublisher,
            Clock clock) {
        this.announcementRepository = announcementRepository;
        this.announcementAccessPolicy = announcementAccessPolicy;
        this.classAccess = classAccess;
        this.accountLookup = accountLookup;
        this.audit = audit;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Transactional(readOnly = true, timeout = 10)
    public AnnouncementViews.Feed listAnnouncements(Actor actor, UUID classId, int page, int size) {
        if (page < 1 || page > 100000 || size < 1 || size > 50)
            throw ApiException.createBadRequest("PAGE_INVALID", "Trang phải từ1, tối đa50 mục");
        var all = announcementAccessPolicy.listReadableClasses(actor, null);
        if (classId != null) announcementAccessPolicy.requireClassAccess(actor, classId, false);
        var allowed =
                all.stream().filter(c -> classId == null || c.classId().equals(classId)).toList();
        var byId = new HashMap<UUID, ClassRef>();
        allowed.forEach(c -> byId.put(c.classId(), c));
        var rows =
                announcementRepository.listAnnouncements(
                        allowed.stream().map(ClassRef::classId).toList(), page, size);
        boolean more =
                rows.size() == size
                        && !announcementRepository
                                .listAnnouncements(
                                        allowed.stream().map(ClassRef::classId).toList(),
                                        page + 1,
                                        size)
                                .isEmpty();
        var items =
                rows.stream()
                        .map(
                                announcement ->
                                        buildItemView(
                                                actor,
                                                announcement,
                                                byId.get(announcement.classId)))
                        .toList();
        var options =
                all.stream()
                        .map(
                                c ->
                                        new AnnouncementViews.ClassOption(
                                                c.classId(),
                                                c.code(),
                                                c.subjectCode(),
                                                actor.role() != Role.STUDENT
                                                        && Objects.equals(
                                                                c.teacherId(), actor.accountId()),
                                                actor.role() == Role.STUDENT
                                                        ? 0
                                                        : classAccess.listActiveStudents(
                                                                c.classId())
                                                                .size(),
                                                c.term()))
                        .toList();
        return new AnnouncementViews.Feed(items, options, page, size, more);
    }

    public AnnouncementViews.Item createAnnouncement(
            Actor actor, UUID classId, AnnouncementCommands.Create input) {
        var courseClass = announcementAccessPolicy.requireClassAccess(actor, classId, true);
        var announcement =
                Announcement.createAnnouncement(
                        classId,
                        actor.accountId(),
                        input.title(),
                        input.body(),
                        input.important(),
                        input.sendEmail(),
                        clock.instant());
        announcementRepository.insertAnnouncement(announcement);
        recordAudit(actor, "ANNOUNCEMENT_CREATED", announcement, buildAuditMetadata(announcement));
        eventPublisher.publish(
                new DomainEvent(
                        1,
                        UUID.randomUUID(),
                        EventTypes.CLASS_ANNOUNCEMENT_POSTED,
                        "U14",
                        clock.instant(),
                        announcement.id.toString(),
                        Map.of(
                                "announcementId",
                                announcement.id.toString(),
                                "classId",
                                classId.toString(),
                                "actorId",
                                actor.accountId().toString(),
                                "important",
                                announcement.important,
                                "sendEmail",
                                announcement.sendEmail)));
        return buildItemView(actor, announcement, courseClass);
    }

    public AnnouncementViews.Item editAnnouncement(
            Actor actor, UUID id, AnnouncementCommands.Edit input) {
        var announcement = loadAnnouncement(actor, id);
        var courseClass =
                announcementAccessPolicy.requireClassAccess(actor, announcement.classId, true);
        requireVersion(announcement, input.version());
        var before = buildAuditMetadata(announcement);
        announcement.edit(input.title(), input.body(), actor.accountId(), clock.instant());
        saveAnnouncement(announcement, input.version());
        recordAudit(
                actor,
                "ANNOUNCEMENT_UPDATED",
                announcement,
                Map.of("before", before, "after", buildAuditMetadata(announcement)));
        return buildItemView(actor, announcement, courseClass);
    }

    public void deleteAnnouncement(Actor actor, UUID id, long version) {
        var announcement = loadAnnouncement(actor, id);
        announcementAccessPolicy.requireClassAccess(actor, announcement.classId, true);
        requireVersion(announcement, version);
        announcement.markDeleted(actor.accountId(), clock.instant());
        saveAnnouncement(announcement, version);
        recordAudit(actor, "ANNOUNCEMENT_DELETED", announcement, buildAuditMetadata(announcement));
    }

    private Announcement loadAnnouncement(Actor actor, UUID id) {
        return announcementRepository
                .findAnnouncement(id)
                .filter(announcement -> announcement.status == Announcement.Status.ACTIVE)
                .orElseThrow(() -> announcementAccessPolicy.rejectAccess(actor, id));
    }

    private void requireVersion(Announcement announcement, long version) {
        if (version < 0 || announcement.version != version)
            throw ApiException.createConflict("ANNOUNCEMENT_STALE", "Thông báo đã thay đổi, hãy tải lại");
    }

    /** Ghi có điều kiện version; câu UPDATE tăng version trong database nên bản trả về cũng phải mang version mới. */
    private void saveAnnouncement(Announcement announcement, long version) {
        if (!announcementRepository.updateAnnouncement(announcement, version))
            throw ApiException.createConflict("ANNOUNCEMENT_STALE", "Thông báo đã thay đổi, hãy tải lại");
        announcement.version = version + 1;
    }

    private AnnouncementViews.Item buildItemView(
            Actor actor, Announcement announcement, ClassRef courseClass) {
        String author =
                accountLookup.getContact(announcement.createdBy)
                        .map(AccountContact::displayName)
                        .orElse("Giảng viên");
        return new AnnouncementViews.Item(
                announcement.id,
                announcement.classId,
                courseClass.code(),
                announcement.title,
                announcement.body,
                announcement.important,
                announcement.sendEmail,
                announcement.createdBy,
                author,
                announcement.createdAt,
                announcement.updatedAt,
                announcement.version,
                actor.role() != Role.STUDENT
                        && Objects.equals(courseClass.teacherId(), actor.accountId()));
    }

    private Map<String, Object> buildAuditMetadata(Announcement announcement) {
        return Map.of(
                "classId",
                announcement.classId.toString(),
                "titleLength",
                announcement.title.length(),
                "bodyLength",
                announcement.body.length(),
                "contentHash",
                hashText(announcement.title + "\n" + announcement.body),
                "important",
                announcement.important,
                "sendEmail",
                announcement.sendEmail,
                "version",
                announcement.version,
                "status",
                announcement.status.name());
    }

    private String hashText(String text) {
        try {
            return HexFormat.of()
                    .formatHex(
                            java.security.MessageDigest.getInstance("SHA-256")
                                    .digest(
                                            text.getBytes(
                                                    java.nio.charset.StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private void recordAudit(
            Actor actor, String action, Announcement announcement, Map<String, Object> details) {
        audit.record(
                new AuditEvent(
                        UUID.randomUUID(),
                        actor.accountId(),
                        action,
                        "ANNOUNCEMENT",
                        announcement.id.toString(),
                        AuditResult.SUCCESS,
                        null,
                        details,
                        clock.instant()));
    }
}
