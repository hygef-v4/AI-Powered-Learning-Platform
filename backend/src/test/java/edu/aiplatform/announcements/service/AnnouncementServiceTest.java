package edu.aiplatform.announcements.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import edu.aiplatform.announcements.model.*;
import edu.aiplatform.announcements.repository.*;
import edu.aiplatform.academics.contract.*;
import edu.aiplatform.audit.contract.*;
import edu.aiplatform.identity.contract.*;
import edu.aiplatform.jobs.contract.*;
import edu.aiplatform.shared.error.ApiException;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class AnnouncementServiceTest {
    UUID classId = UUID.randomUUID(), teacherId = UUID.randomUUID();
    Actor teacher = new Actor(teacherId, Role.TEACHER);
    Instant now = Instant.parse("2026-10-21T08:00:00Z");
    Clock clock = Clock.fixed(now, ZoneOffset.UTC);
    IAnnouncementRepository announcementRepository;
    IAuthorization authorization;
    IClassAccess classAccess;
    IAudit audit;
    IEventPublisher eventPublisher;
    IAccountLookup accountLookup;
    AnnouncementAccessPolicy announcementAccessPolicy;
    AnnouncementService announcementService;
    ClassRef course;

    @BeforeEach
    void setup() {
        announcementRepository = mock(IAnnouncementRepository.class);
        authorization = mock(IAuthorization.class);
        classAccess = mock(IClassAccess.class);
        audit = mock(IAudit.class);
        eventPublisher = mock(IEventPublisher.class);
        accountLookup = mock(IAccountLookup.class);
        course =
                new ClassRef(
                        classId,
                        UUID.randomUUID(),
                        "SWP391",
                        "Lớp thử",
                        ClassStatus.OPEN,
                        teacherId,
                        false);
        when(classAccess.getClassRef(classId)).thenReturn(Optional.of(course));
        when(authorization.authorize(any(), any(), any()))
                .thenReturn(AuthorizationDecision.allow());
        when(accountLookup.getContact(any())).thenReturn(Optional.empty());
        announcementAccessPolicy =
                new AnnouncementAccessPolicy(authorization, classAccess);
        announcementService =
                new AnnouncementService(
                        announcementRepository,
                        announcementAccessPolicy,
                        classAccess,
                        accountLookup,
                        eventPublisher,
                        clock);
    }

    Announcement item() {
        var a =
                Announcement.createAnnouncement(
                        classId,
                        teacherId,
                        "Tiêu đề",
                        "Nội dung **Markdown**",
                        true,
                        true,
                        now.minusSeconds(60));
        when(announcementRepository.findAnnouncement(a.id)).thenReturn(Optional.of(a));
        return a;
    }

    @Test
    void consecutiveEditsUseReturnedVersion() {
        var announcement = item();
        when(announcementRepository.updateAnnouncement(eq(announcement), anyLong()))
                .thenAnswer(call -> {
            announcement.version = (Long) call.getArgument(1) + 1;
            return true;
        });
        var first = announcementService.editAnnouncement(teacher, announcement.id,
                new AnnouncementCommands.Edit("First", "private-body-sentinel", 0));
        var second = announcementService.editAnnouncement(teacher, announcement.id,
                new AnnouncementCommands.Edit("Second", "Second body", first.version()));
        assertThat(second.version()).isEqualTo(2);
        assertThat(second.createdBy()).isEqualTo(teacherId);
        assertThat(second.createdAt()).isEqualTo(now.minusSeconds(60));
        assertThat(announcement.updatedBy).isEqualTo(teacherId);
        assertThat(announcement.updatedAt).isEqualTo(now);
        verify(announcementRepository).updateAnnouncement(announcement, 0);
        verify(announcementRepository).updateAnnouncement(announcement, 1);
        verifyNoInteractions(eventPublisher);
    }
}
