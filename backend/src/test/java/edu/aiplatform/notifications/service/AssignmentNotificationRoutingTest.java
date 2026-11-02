package edu.aiplatform.notifications.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import edu.aiplatform.academics.contract.*;
import edu.aiplatform.identity.contract.*;
import edu.aiplatform.jobs.contract.*;
import edu.aiplatform.notifications.model.NotificationType;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;

class AssignmentNotificationRoutingTest {
    final IClassAccess classes = mock(IClassAccess.class);
    final IAccountLookup accounts = mock(IAccountLookup.class);
    final NotificationService notifications = mock(NotificationService.class);
    final NotificationEventService service = new NotificationEventService(new RecipientResolver(classes, accounts), notifications);
    final UUID subject = UUID.randomUUID(), assignment = UUID.randomUUID();
    final UUID first = new UUID(0, 1), second = new UUID(0, 2);
    final UUID studentA = UUID.randomUUID(), studentB = UUID.randomUUID();

    ClassRef course(UUID id, ClassStatus status) {
        return new ClassRef(id, subject, "C", "Class", status, UUID.randomUUID(), false);
    }

    void member(UUID student, UUID classId) {
        when(classes.isActiveStudent(student, classId)).thenReturn(true);
        when(accounts.getContact(student)).thenReturn(Optional.of(
                new AccountContact(student, "fixture@example.test", "Student", Role.STUDENT, AccountStatus.ACTIVE)));
    }

    DomainEvent event(Map<String, Object> scope) {
        var payload = new HashMap<>(scope);
        payload.put("assignmentId", assignment.toString());
        payload.put("subjectId", subject.toString());
        return new DomainEvent(1, UUID.randomUUID(), EventTypes.ASSIGNMENT_OPENED, "U07", Instant.now(), null, payload);
    }

    void delivery(DomainEvent event, UUID classId, UUID... students) {
        verify(notifications).create(eq(event.eventId()), eq(NotificationType.ASSIGNMENT_OPENED),
                eq(List.of(students)), anyString(), anyString(),
                eq("/my-assignments/" + assignment + "?classId=" + classId), eq("ASSIGNMENT_OPENED"), isNull());
    }

    @Test void classAssignmentKeepsStudentClassInLinkAndFiltersStaleMembershipAndDisabledAccounts() {
        UUID removed = UUID.randomUUID(), disabled = UUID.randomUUID(), teacher = UUID.randomUUID();
        when(classes.getClassRef(first)).thenReturn(Optional.of(course(first, ClassStatus.OPEN)));
        when(classes.listActiveStudents(first)).thenReturn(List.of(studentA, removed, disabled, teacher));
        member(studentA, first);
        member(removed, first);
        when(classes.isActiveStudent(removed, first)).thenReturn(false);
        when(accounts.getContact(disabled)).thenReturn(Optional.of(
                new AccountContact(disabled, "fixture@example.test", "Disabled", Role.STUDENT, AccountStatus.DISABLED)));
        when(accounts.getContact(teacher)).thenReturn(Optional.of(
                new AccountContact(teacher, "fixture@example.test", "Teacher", Role.TEACHER, AccountStatus.ACTIVE)));
        var event = event(Map.of("scope", "CLASS", "classId", first.toString()));
        service.accept(event);
        delivery(event, first, studentA);
        verifyNoMoreInteractions(notifications);
    }

    @Test void subjectAssignmentUsesPerClassLinksAndStableDedupRegardlessOfProviderOrder() {
        var a = course(first, ClassStatus.OPEN);
        var b = course(second, ClassStatus.OPEN);
        var closed = course(UUID.randomUUID(), ClassStatus.ARCHIVED);
        when(classes.getClassRef(first)).thenReturn(Optional.of(a));
        when(classes.getClassRef(second)).thenReturn(Optional.of(b));
        when(classes.listOpenClassesOfSubject(subject)).thenReturn(List.of(b, closed, a), List.of(a, b, closed));
        when(classes.listActiveStudents(first)).thenReturn(List.of(studentA, studentA));
        when(classes.listActiveStudents(second)).thenReturn(List.of(studentA, studentB));
        member(studentA, first);
        member(studentA, second);
        member(studentB, second);
        for (int attempt = 0; attempt < 2; attempt++) {
            clearInvocations(notifications);
            var event = event(Map.of("scope", "SUBJECT"));
            service.accept(event);
            delivery(event, first, studentA);
            delivery(event, second, studentB);
            verifyNoMoreInteractions(notifications);
        }
        verify(classes, never()).listActiveStudents(closed.classId());
    }

    @Test void closedClassDoesNotNotifyStudents() {
        when(classes.getClassRef(first)).thenReturn(Optional.of(course(first, ClassStatus.ARCHIVED)));
        service.accept(event(Map.of("scope", "CLASS", "classId", first.toString())));
        verifyNoInteractions(notifications);
    }
}
