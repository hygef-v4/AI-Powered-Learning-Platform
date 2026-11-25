package edu.aiplatform.notifications.service;

import static org.mockito.Mockito.*;

import edu.aiplatform.academics.contract.*;
import edu.aiplatform.identity.contract.*;
import edu.aiplatform.jobs.contract.*;
import edu.aiplatform.notifications.model.NotificationType;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;

class AssignmentNotificationRoutingTest {
    final IClassAccess classAccess = mock(IClassAccess.class);
    final IAccountLookup accountLookup = mock(IAccountLookup.class);
    final NotificationService notificationService = mock(NotificationService.class);
    final NotificationEventService notificationEventService =
            new NotificationEventService(
                    new RecipientResolver(classAccess, accountLookup), notificationService);
    final UUID subject = UUID.randomUUID(), assignment = UUID.randomUUID();
    final UUID first = new UUID(0, 1), second = new UUID(0, 2);
    final UUID studentA = UUID.randomUUID(), studentB = UUID.randomUUID();

    ClassRef course(UUID id, ClassStatus status) {
        return new ClassRef(id, subject, "C", "Class", status, UUID.randomUUID(), false);
    }

    void member(UUID student, UUID classId) {
        when(classAccess.isActiveStudent(student, classId)).thenReturn(true);
        when(accountLookup.getContact(student))
                .thenReturn(
                        Optional.of(
                                new AccountContact(
                                        student,
                                        "fixture@example.test",
                                        "Student",
                                        Role.STUDENT,
                                        AccountStatus.ACTIVE)));
    }

    DomainEvent event(Map<String, Object> scope) {
        var payload = new HashMap<>(scope);
        payload.put("assignmentId", assignment.toString());
        payload.put("subjectId", subject.toString());
        return new DomainEvent(
                1,
                UUID.randomUUID(),
                EventTypes.ASSIGNMENT_OPENED,
                "U07",
                Instant.now(),
                null,
                payload);
    }

    void delivery(DomainEvent event, UUID classId, UUID... students) {
        verify(notificationService)
                .createNotification(
                        eq(event.eventId()),
                        eq(NotificationType.ASSIGNMENT_OPENED),
                        eq(List.of(students)),
                        anyString(),
                        anyString(),
                        eq("/my-assignments/" + assignment + "?classId=" + classId),
                        eq("ASSIGNMENT_OPENED"),
                        isNull());
    }

    @Test
    void subjectAssignmentUsesPerClassLinksAndStableDedupRegardlessOfProviderOrder() {
        var a = course(first, ClassStatus.OPEN);
        var b = course(second, ClassStatus.OPEN);
        var closed = course(UUID.randomUUID(), ClassStatus.ARCHIVED);
        when(classAccess.getClassRef(first)).thenReturn(Optional.of(a));
        when(classAccess.getClassRef(second)).thenReturn(Optional.of(b));
        when(classAccess.listOpenClassesOfSubject(subject))
                .thenReturn(List.of(b, closed, a), List.of(a, b, closed));
        when(classAccess.listActiveStudents(first)).thenReturn(List.of(studentA, studentA));
        when(classAccess.listActiveStudents(second)).thenReturn(List.of(studentA, studentB));
        member(studentA, first);
        member(studentA, second);
        member(studentB, second);
        for (int attempt = 0; attempt < 2; attempt++) {
            clearInvocations(notificationService);
            var event = event(Map.of("scope", "SUBJECT"));
            notificationEventService.handleEvent(event);
            delivery(event, first, studentA);
            delivery(event, second, studentB);
            verifyNoMoreInteractions(notificationService);
        }
        verify(classAccess, never()).listActiveStudents(closed.classId());
    }

    @Test
    void closedClassDoesNotNotifyStudents() {
        when(classAccess.getClassRef(first))
                .thenReturn(Optional.of(course(first, ClassStatus.ARCHIVED)));
        notificationEventService.handleEvent(
                event(Map.of("scope", "CLASS", "classId", first.toString())));
        verifyNoInteractions(notificationService);
    }
}
