package edu.aiplatform.notifications.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import edu.aiplatform.academics.contract.*;
import edu.aiplatform.identity.contract.*;
import edu.aiplatform.jobs.contract.*;
import java.time.Instant;
import java.util.*;
import java.util.stream.IntStream;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class RecipientResolverTest {
    IClassAccess classAccess;
    IAccountLookup accountLookup;
    RecipientResolver recipientResolver;
    UUID classId = UUID.randomUUID(),
            subjectId = UUID.randomUUID(),
            student = UUID.randomUUID(),
            teacher = UUID.randomUUID();

    @BeforeEach
    void setup() {
        classAccess = mock(IClassAccess.class);
        accountLookup = mock(IAccountLookup.class);
        recipientResolver = new RecipientResolver(classAccess, accountLookup);
        when(classAccess.getClassRef(classId))
                .thenReturn(
                        Optional.of(
                                new ClassRef(
                                        classId,
                                        subjectId,
                                        "C",
                                        "Class",
                                        ClassStatus.OPEN,
                                        teacher,
                                        false)));
        when(classAccess.listActiveStudents(classId)).thenReturn(List.of(student));
        when(classAccess.isActiveStudent(any(), eq(classId))).thenReturn(true);
        when(accountLookup.getContact(any()))
                .thenAnswer(
                        i ->
                                Optional.of(
                                        new AccountContact(
                                                i.getArgument(0),
                                                "test@example.test",
                                                "Student",
                                                Role.STUDENT,
                                                AccountStatus.ACTIVE)));
    }

    DomainEvent event(String type, Map<String, Object> payload) {
        return new DomainEvent(
                1,
                UUID.randomUUID(),
                type,
                NotificationEventService.SOURCES.get(type),
                Instant.now(),
                null,
                payload);
    }

    @Test
    void enrollmentUsesAccountId() {
        var r =
                recipientResolver.resolveRecipients(
                                event(
                                        EventTypes.ENROLLMENT_ACTIVATED,
                                        Map.of(
                                                "accountId",
                                                student.toString(),
                                                "classId",
                                                classId.toString())))
                        .get(0);
        assertThat(r.recipients()).containsExactly(student);
        assertThat(r.emailType()).isEqualTo("ENROLLED");
    }

    @Test
    void gradeUsesStudentIdsAndNeverScore() {
        var r =
                recipientResolver.resolveRecipients(
                                event(
                                        EventTypes.GRADE_PUBLISHED,
                                        Map.of(
                                                "classId",
                                                classId.toString(),
                                                "studentIds",
                                                List.of(student.toString()),
                                                "score",
                                                9.5)))
                        .get(0);
        assertThat(r.recipients()).containsExactly(student);
        assertThat(r.link()).doesNotContain("9.5");
    }

    @Test
    void subjectAssignmentFansOutAllOpenClassesAndDeduplicates() {
        var second = UUID.randomUUID();
        when(classAccess.listOpenClassesOfSubject(subjectId))
                .thenReturn(
                        List.of(
                                new ClassRef(
                                        classId,
                                        subjectId,
                                        "C",
                                        "Class",
                                        ClassStatus.OPEN,
                                        teacher,
                                        false),
                                new ClassRef(
                                        second,
                                        subjectId,
                                        "D",
                                        "Other",
                                        ClassStatus.OPEN,
                                        teacher,
                                        false)));
        when(classAccess.listActiveStudents(second)).thenReturn(List.of(student));
        var r =
                recipientResolver.resolveRecipients(
                                event(
                                        EventTypes.ASSIGNMENT_OPENED,
                                        Map.of(
                                                "scope",
                                                "SUBJECT",
                                                "subjectId",
                                                subjectId.toString(),
                                                "assignmentId",
                                                UUID.randomUUID().toString())))
                        .get(0);
        assertThat(r.recipients()).containsExactly(student);
    }

    @Test
    void quizHasNoEmail() {
        var r =
                recipientResolver.resolveRecipients(
                                event(
                                        EventTypes.QUIZ_PUBLISHED,
                                        Map.of(
                                                "classId",
                                                classId.toString(),
                                                "quizId",
                                                UUID.randomUUID().toString())))
                        .get(0);
        assertThat(r.emailType()).isNull();
    }

    @Test
    void questionProposalNotifiesManagerAndReviewNotifiesProposer() {
        UUID manager = UUID.randomUUID();
        when(accountLookup.getContact(manager))
                .thenReturn(
                        Optional.of(
                                new AccountContact(
                                        manager,
                                        "m@example.test",
                                        "M",
                                        Role.SUBJECT_MANAGER,
                                        AccountStatus.ACTIVE)));
        when(accountLookup.getContact(teacher))
                .thenReturn(
                        Optional.of(
                                new AccountContact(
                                        teacher,
                                        "t@example.test",
                                        "T",
                                        Role.TEACHER,
                                        AccountStatus.ACTIVE)));
        var proposed =
                recipientResolver.resolveRecipients(
                                event(
                                        EventTypes.QUESTION_PROPOSED,
                                        Map.of(
                                                "questionId",
                                                UUID.randomUUID().toString(),
                                                "subjectId",
                                                subjectId.toString(),
                                                "recipientIds",
                                                List.of(manager.toString()))))
                        .get(0);
        assertThat(proposed.recipients()).containsExactly(manager);
        assertThat(proposed.link())
                .isEqualTo("/manager/questions?subjectId=" + subjectId + "&status=PENDING");
        assertThat(proposed.emailType()).isEqualTo("QUESTION_PROPOSED");
        var reviewed =
                recipientResolver.resolveRecipients(
                                event(
                                        EventTypes.QUESTION_REVIEWED,
                                        Map.of(
                                                "questionId",
                                                UUID.randomUUID().toString(),
                                                "classId",
                                                classId.toString(),
                                                "subjectId",
                                                subjectId.toString(),
                                                "decision",
                                                "REJECTED",
                                                "recipientIds",
                                                List.of(teacher.toString()))))
                        .get(0);
        assertThat(reviewed.recipients()).containsExactly(teacher);
        assertThat(reviewed.link()).isEqualTo("/teaching/classes/" + classId + "?tab=materials");
        assertThat(reviewed.emailType()).isEqualTo("QUESTION_REVIEWED");
    }

    @Test
    void announcementOpensTheClassAnnouncementsPage() {
        var r =
                recipientResolver.resolveRecipients(
                                event(
                                        EventTypes.CLASS_ANNOUNCEMENT_POSTED,
                                        Map.of(
                                                "classId",
                                                classId.toString(),
                                                "announcementId",
                                                UUID.randomUUID().toString(),
                                                "actorId",
                                                teacher.toString())))
                        .get(0);
        assertThat(r.link()).isEqualTo("/classes/" + classId + "/announcements");
    }

    @Test
    void announcementEmailRequiresBothFlagsAndExcludesAuthor() {
        var p =
                new HashMap<String, Object>(
                        Map.of(
                                "classId",
                                classId.toString(),
                                "announcementId",
                                UUID.randomUUID().toString(),
                                "actorId",
                                student.toString(),
                                "important",
                                true,
                                "sendEmail",
                                true));
        assertThat(
                        recipientResolver.resolveRecipients(
                                event(EventTypes.CLASS_ANNOUNCEMENT_POSTED, p))
                                .get(0)
                                .recipients())
                .isEmpty();
        p.put("actorId", teacher.toString());
        p.put("important", false);
        assertThat(
                        recipientResolver.resolveRecipients(
                                event(EventTypes.CLASS_ANNOUNCEMENT_POSTED, p))
                                .get(0)
                                .emailType())
                .isNull();
    }

    @ParameterizedTest
    @CsvSource({"false,false", "false,true", "true,false", "true,true"})
    void announcementEmailFlagMatrixKeepsInAppRecipients(boolean important, boolean sendEmail) {
        var resolved =
                recipientResolver.resolveRecipients(
                                event(
                                        EventTypes.CLASS_ANNOUNCEMENT_POSTED,
                                        Map.of(
                                                "classId",
                                                classId.toString(),
                                                "announcementId",
                                                UUID.randomUUID().toString(),
                                                "actorId",
                                                teacher.toString(),
                                                "important",
                                                important,
                                                "sendEmail",
                                                sendEmail)))
                        .get(0);

        assertThat(resolved.recipients()).containsExactly(student);
        assertThat(resolved.emailType())
                .isEqualTo(important && sendEmail ? "ANNOUNCEMENT_IMPORTANT" : null);
        assertThat(resolved.link()).isEqualTo("/classes/" + classId + "/announcements");
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                EventTypes.GROUP_LEADER_CHANGED,
                EventTypes.GROUP_LEADER_REQUEST_REJECTED,
                EventTypes.GROUP_MEMBERSHIP_CHANGED,
                EventTypes.GROUP_SUBMITTED
            })
    void currentGroupProducerRecipientIdsAreDeduplicatedAndRechecked(String type) {
        UUID removed = UUID.randomUUID(), disabled = UUID.randomUUID();
        when(classAccess.isActiveStudent(removed, classId)).thenReturn(false);
        when(accountLookup.getContact(disabled))
                .thenReturn(
                        Optional.of(
                                new AccountContact(
                                        disabled,
                                        "disabled@example.test",
                                        "Disabled",
                                        Role.STUDENT,
                                        AccountStatus.DISABLED)));

        var resolved =
                recipientResolver.resolveRecipients(
                                event(
                                        type,
                                        Map.of(
                                                "classId",
                                                classId.toString(),
                                                "groupId",
                                                UUID.randomUUID().toString(),
                                                "recipientIds",
                                                List.of(
                                                        student.toString(),
                                                        student.toString(),
                                                        removed.toString(),
                                                        disabled.toString()))))
                        .get(0);

        assertThat(resolved.recipients()).containsExactly(student);
        assertThat(resolved.emailType()).isNull();
        assertThat(resolved.link()).isEqualTo("/classes/" + classId + "/group");
    }

    @Test
    void backupIncludesMoreThan20AdminsAndMultiplePages() {
        var admins =
                IntStream.range(0, 125)
                        .mapToObj(
                                i ->
                                        new AccountContact(
                                                new UUID(0, i + 1),
                                                "a@example.test",
                                                "A",
                                                Role.ADMIN,
                                                AccountStatus.ACTIVE))
                        .toList();
        when(accountLookup.listActiveAdmins(null, 100)).thenReturn(admins.subList(0, 100));
        when(accountLookup.listActiveAdmins(admins.get(99).accountId(), 100))
                .thenReturn(admins.subList(100, 125));
        admins.forEach(
                a -> when(accountLookup.getContact(a.accountId())).thenReturn(Optional.of(a)));
        var r =
                recipientResolver.resolveRecipients(
                                event(
                                        EventTypes.BACKUP_FAILED,
                                        Map.of("runId", UUID.randomUUID().toString())))
                        .get(0);
        assertThat(r.recipients()).hasSize(125);
    }

    @Test
    void inactiveAccountNotNotified() {
        when(accountLookup.getContact(student))
                .thenReturn(
                        Optional.of(
                                new AccountContact(
                                        student,
                                        "t@example.test",
                                        "x",
                                        Role.STUDENT,
                                        AccountStatus.DISABLED)));
        assertThat(
                        recipientResolver.resolveRecipients(
                                        event(
                                                EventTypes.ENROLLMENT_ACTIVATED,
                                                Map.of(
                                                        "accountId",
                                                        student.toString(),
                                                        "classId",
                                                        classId.toString())))
                                .get(0)
                                .recipients())
                .isEmpty();
    }

    @Test
    void leaderRequestGoesToTeacherOnly() {
        when(accountLookup.getContact(teacher))
                .thenReturn(
                        Optional.of(
                                new AccountContact(
                                        teacher,
                                        "teacher@example.test",
                                        "T",
                                        Role.TEACHER,
                                        AccountStatus.ACTIVE)));
        var group = UUID.randomUUID();
        var r =
                recipientResolver.resolveRecipients(
                                event(
                                        EventTypes.GROUP_LEADER_CHANGE_REQUESTED,
                                        Map.of(
                                                "classId",
                                                classId.toString(),
                                                "groupId",
                                                group.toString())))
                        .get(0);
        assertThat(r.recipients()).containsExactly(teacher);
        assertThat(r.emailType()).isEqualTo("LEADER_CHANGE_REQUESTED");
        assertThat(r.link())
                .isEqualTo("/teaching/classes/" + classId + "?tab=students&group=" + group);
    }

    @Test
    void groupMembersUseExplicitRecipientList() {
        var r =
                recipientResolver.resolveRecipients(
                                event(
                                        EventTypes.GROUP_SUBMITTED,
                                        Map.of(
                                                "classId",
                                                classId.toString(),
                                                "groupId",
                                                UUID.randomUUID().toString(),
                                                "memberIds",
                                                List.of(student.toString()))))
                        .get(0);
        assertThat(r.recipients()).containsExactly(student);
        assertThat(r.link()).isEqualTo("/classes/" + classId + "/group");
    }

    @Test
    void gradeOpensTheGradedAssignment() {
        var assignment = UUID.randomUUID();
        var r =
                recipientResolver.resolveRecipients(
                                event(
                                        EventTypes.GRADE_PUBLISHED,
                                        Map.of(
                                                "classId",
                                                classId.toString(),
                                                "assignmentId",
                                                assignment.toString(),
                                                "studentIds",
                                                List.of(student.toString()))))
                        .get(0);
        assertThat(r.link()).isEqualTo("/my-assignments/" + assignment + "?classId=" + classId);
    }

    @Test
    void serviceIgnoresUnknownVersionAndRejectsWrongSource() {
        var notificationEventService =
                new NotificationEventService(recipientResolver, mock(NotificationService.class));
        notificationEventService.handleEvent(
                new DomainEvent(
                        2,
                        UUID.randomUUID(),
                        EventTypes.BACKUP_FAILED,
                        "U03",
                        Instant.now(),
                        null,
                        Map.of()));
        assertThatThrownBy(
                        () ->
                                notificationEventService.handleEvent(
                                        new DomainEvent(
                                                1,
                                                UUID.randomUUID(),
                                                EventTypes.BACKUP_FAILED,
                                                "U05",
                                                Instant.now(),
                                                null,
                                                Map.of())))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
