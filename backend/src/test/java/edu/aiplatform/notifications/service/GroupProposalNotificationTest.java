package edu.aiplatform.notifications.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import edu.aiplatform.academics.contract.ClassRef;
import edu.aiplatform.academics.contract.ClassStatus;
import edu.aiplatform.academics.contract.IClassAccess;
import edu.aiplatform.identity.contract.AccountContact;
import edu.aiplatform.identity.contract.AccountStatus;
import edu.aiplatform.identity.contract.IAccountLookup;
import edu.aiplatform.identity.contract.Role;
import edu.aiplatform.jobs.contract.DomainEvent;
import edu.aiplatform.jobs.contract.EventTypes;
import edu.aiplatform.notifications.model.NotificationType;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** Thông báo đề xuất nhóm (BR-U12-95): người nhận, đường dẫn, không email, không báo trùng khi duyệt. */
class GroupProposalNotificationTest {

    private final UUID classId = UUID.randomUUID();
    private final UUID proposalId = UUID.randomUUID();
    private final UUID student = UUID.randomUUID();
    private final UUID teacher = UUID.randomUUID();
    private final IClassAccess classes = mock(IClassAccess.class);
    private final IAccountLookup accounts = mock(IAccountLookup.class);
    private final RecipientResolver resolver = new RecipientResolver(classes, accounts);

    @BeforeEach
    void setUp() {
        when(classes.getClassRef(classId)).thenReturn(Optional.of(
                new ClassRef(classId, UUID.randomUUID(), "C", "Class", ClassStatus.OPEN, teacher, false)));
        when(classes.isActiveStudent(any(), eq(classId))).thenReturn(true);
        when(accounts.getContact(student)).thenReturn(Optional.of(
                new AccountContact(student, "s@example.test", "SV", Role.STUDENT, AccountStatus.ACTIVE)));
        when(accounts.getContact(teacher)).thenReturn(Optional.of(
                new AccountContact(teacher, "t@example.test", "GV", Role.TEACHER, AccountStatus.ACTIVE)));
    }

    @Test
    void studentEventsGoToRecipientsAndLinkToMyGroup() {
        for (String type : List.of(EventTypes.GROUP_PROPOSAL_INVITED, EventTypes.GROUP_PROPOSAL_APPROVED,
                EventTypes.GROUP_PROPOSAL_REJECTED)) {
            var resolved = resolver.resolve(event(type, List.of(student))).get(0);
            assertThat(resolved.recipients()).containsExactly(student);
            assertThat(resolved.link()).isEqualTo("/classes/" + classId + "/group");
            assertThat(resolved.emailType()).isNull();
        }
    }

    @Test
    void submittedProposalGoesToTheClassTeacher() {
        var resolved = resolver.resolve(event(EventTypes.GROUP_PROPOSAL_SUBMITTED, List.of(student))).get(0);

        assertThat(resolved.type()).isEqualTo(NotificationType.GROUP_PROPOSAL_SUBMITTED);
        assertThat(resolved.recipients()).containsExactly(teacher);
        assertThat(resolved.link()).isEqualTo("/teaching/classes/" + classId + "?tab=students");
    }

    @Test
    void membershipChangeFromAnApprovedProposalIsNotNotifiedTwice() {
        Map<String, Object> payload = Map.of("classId", classId.toString(), "groupId", UUID.randomUUID().toString(),
                "recipientIds", List.of(student.toString()), "reason", "PROPOSAL_APPROVED");
        DomainEvent membership = new DomainEvent(1, UUID.randomUUID(), EventTypes.GROUP_MEMBERSHIP_CHANGED, "U12",
                Instant.now(), null, payload);

        assertThat(resolver.resolve(membership)).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(value = NotificationType.class, names = "GROUP_PROPOSAL_.*", mode = EnumSource.Mode.MATCH_ALL)
    void everyProposalTypeHasTitleAndSummary(NotificationType type) {
        assertThat(NotificationEventService.title(type)).isNotBlank();
        assertThat(NotificationSummaries.forType(type)).isNotBlank();
    }

    private DomainEvent event(String type, List<UUID> recipients) {
        return new DomainEvent(1, UUID.randomUUID(), type, NotificationEventService.SOURCES.get(type), Instant.now(),
                null, Map.of("classId", classId.toString(), "proposalId", proposalId.toString(), "recipientIds",
                recipients.stream().map(UUID::toString).toList()));
    }
}
