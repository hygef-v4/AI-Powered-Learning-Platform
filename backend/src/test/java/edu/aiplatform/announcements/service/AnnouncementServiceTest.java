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
    void createPublishesOnlyMetadataAndAudits() {
        var created =
                announcementService.createAnnouncement(
                        teacher,
                        classId,
                        new AnnouncementCommands.Create(" Mới ", "Nội dung", true, true));
        assertThat(created.title()).isEqualTo("Mới");
        verify(announcementRepository).insertAnnouncement(any());
        verify(eventPublisher)
                .publish(
                        argThat(
                                e ->
                                        e.eventType().equals(EventTypes.CLASS_ANNOUNCEMENT_POSTED)
                                                && !e.payload().containsKey("body")
                                                && e.payload().get("sendEmail").equals(true)));
    }

    @Test
    void editPreservesAuthorAndFlagsAndDoesNotRepublish() {
        var a = item();
        when(announcementRepository.updateAnnouncement(a, 0)).thenReturn(true);
        var edited =
                announcementService.editAnnouncement(
                        teacher, a.id, new AnnouncementCommands.Edit("Đổi", "Nội dung sửa", 0));
        assertThat(edited.createdBy()).isEqualTo(teacherId);
        assertThat(edited.createdAt()).isEqualTo(now.minusSeconds(60));
        assertThat(edited.important()).isTrue();
        assertThat(edited.sendEmail()).isTrue();
        assertThat(edited.version()).as("version mới sau UPDATE").isEqualTo(1);
        verifyNoInteractions(eventPublisher);
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

    @Test
    void staleVersionDoesNotWrite() {
        var a = item();
        assertThatThrownBy(
                        () ->
                                announcementService.editAnnouncement(
                                        teacher,
                                        a.id,
                                        new AnnouncementCommands.Edit("Đổi", "Sửa", 1)))
                .isInstanceOf(ApiException.class)
                .extracting("code")
                .isEqualTo("ANNOUNCEMENT_STALE");
        verify(announcementRepository, never()).updateAnnouncement(any(), anyLong());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void concurrentCasFailureDoesNotAuditSuccess() {
        var a = item();
        when(announcementRepository.updateAnnouncement(a, 0)).thenReturn(false);
        assertThatThrownBy(() -> announcementService.deleteAnnouncement(teacher, a.id, 0))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void deleteIsSoftAndDoesNotRepublish() {
        var a = item();
        when(announcementRepository.updateAnnouncement(a, 0)).thenReturn(true);
        announcementService.deleteAnnouncement(teacher, a.id, 0);
        assertThat(a.status).isEqualTo(Announcement.Status.DELETED);
        assertThat(a.deletedBy).isEqualTo(teacherId);
        assertThat(a.deletedAt).isEqualTo(now);
        verifyNoInteractions(eventPublisher);
    }

    @ParameterizedTest
    @EnumSource(
            value = Role.class,
            names = {"ADMIN", "STUDENT"})
    void cannotManage(Role role) {
        var actor = new Actor(UUID.randomUUID(), role);
        assertThatThrownBy(() -> announcementAccessPolicy.requireClassAccess(actor, classId, true))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.readStatus().value()).isEqualTo(404);
                    assertThat(e.readCode()).isEqualTo("ANNOUNCEMENT_NOT_FOUND");
                });
    }

    @Test
    void anotherTeacherCannotManage() {
        assertThatThrownBy(
                        () ->
                                announcementAccessPolicy.requireClassAccess(
                                        new Actor(UUID.randomUUID(), Role.TEACHER), classId, true))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.readStatus().value()).isEqualTo(404));
    }

    @Test
    void managerMustActuallyTeachTheClass() {
        assertThat(
                announcementAccessPolicy.requireClassAccess(
                        new Actor(teacherId, Role.SUBJECT_MANAGER), classId, true))
                .isEqualTo(course);
        assertThatThrownBy(
                        () ->
                                announcementAccessPolicy.requireClassAccess(
                                        new Actor(UUID.randomUUID(), Role.SUBJECT_MANAGER),
                                        classId,
                                        true))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void activeStudentReadsOnlyOpenEnrolledClass() {
        var student = new Actor(UUID.randomUUID(), Role.STUDENT);
        when(classAccess.isActiveStudent(student.accountId(), classId)).thenReturn(true);
        assertThat(announcementAccessPolicy.requireClassAccess(student, classId, false))
                .isEqualTo(course);
        when(classAccess.getClassRef(classId))
                .thenReturn(
                        Optional.of(
                                new ClassRef(
                                        classId,
                                        course.subjectId(),
                                        "C",
                                        "Closed",
                                        ClassStatus.ARCHIVED,
                                        teacherId,
                                        false)));
        assertThatThrownBy(
                () -> announcementAccessPolicy.requireClassAccess(student, classId, false))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void authorizationDenialEvenForTeacherIsAudited() {
        when(authorization.authorize(any(), any(), any()))
                .thenReturn(AuthorizationDecision.deny("DISABLED"));
        assertThatThrownBy(
                () -> announcementAccessPolicy.requireClassAccess(teacher, classId, true))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void invalidMarkdownAndEmailCombinationRejected() {
        assertThatThrownBy(
                        () -> Announcement.createAnnouncement(classId, teacherId, "t", "b", false, true, now))
                .isInstanceOf(ApiException.class);
        for (String body : List.of("", "<script>alert(1)</script>", "x".repeat(5001)))
            assertThatThrownBy(
                            () ->
                                    Announcement.createAnnouncement(
                                            classId, teacherId, "t", body, false, false, now))
                    .isInstanceOf(ApiException.class);
    }

    @Test
    void filteredFeedKeepsAllClassOptions() {
        when(classAccess.listOpenClassesOf(teacherId)).thenReturn(List.of(course));
        when(announcementRepository.listAnnouncements(anyList(), anyInt(), anyInt()))
                .thenReturn(List.of());
        var result = announcementService.listAnnouncements(teacher, classId, 1, 20);
        assertThat(result.classes()).hasSize(1);
        assertThat(result.items()).isEmpty();
    }

    @Test
    void badPaginationRejected() {
        assertThatThrownBy(() -> announcementService.listAnnouncements(teacher, null, 1, 51))
                .isInstanceOf(ApiException.class);
    }
}
