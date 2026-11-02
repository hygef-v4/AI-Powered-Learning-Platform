package edu.aiplatform.groups.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.aiplatform.academics.contract.ClassRef;
import edu.aiplatform.academics.contract.ClassStatus;
import edu.aiplatform.academics.contract.IClassAccess;
import edu.aiplatform.assignments.contract.AssignmentScope;
import edu.aiplatform.assignments.contract.AssignmentStatus;
import edu.aiplatform.assignments.contract.AssignmentView;
import edu.aiplatform.assignments.contract.GradingMode;
import edu.aiplatform.assignments.contract.IAssignmentQuery;
import edu.aiplatform.assignments.contract.SubmissionMode;
import edu.aiplatform.audit.contract.IAudit;
import edu.aiplatform.groupdocs.domain.GroupDocumentRepository;
import edu.aiplatform.groups.domain.ClassGroup;
import edu.aiplatform.groups.domain.GroupRepository;
import edu.aiplatform.groups.domain.LeaderChangeRequest;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.AuthorizationDecision;
import edu.aiplatform.identity.contract.IAccountLookup;
import edu.aiplatform.identity.contract.IAuthorization;
import edu.aiplatform.identity.contract.Role;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MyGroupServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-22T03:00:00Z");
    private final UUID classId = UUID.randomUUID();
    private final Actor student = new Actor(UUID.randomUUID(), Role.STUDENT);
    private final GroupRepository groups = mock(GroupRepository.class);
    private final GroupDocumentRepository documents = mock(GroupDocumentRepository.class);
    private final IAuthorization authorization = mock(IAuthorization.class);
    private final IClassAccess classes = mock(IClassAccess.class);
    private final IAssignmentQuery assignments = mock(IAssignmentQuery.class);
    private final MyGroupService service = new MyGroupService(groups, documents,
            new GroupAccess(authorization, classes, mock(IAudit.class), Clock.fixed(NOW, ZoneOffset.UTC)), assignments,
            mock(IAccountLookup.class));

    @BeforeEach
    void setUp() {
        when(authorization.authorize(any(), any(), any())).thenReturn(AuthorizationDecision.allow());
        when(classes.isActiveStudent(student.accountId(), classId)).thenReturn(true);
        when(classes.getClassRef(classId)).thenReturn(Optional.of(
                new ClassRef(classId, UUID.randomUUID(), "SE1801", "SWP391", ClassStatus.OPEN, UUID.randomUUID(), true)));
    }

    @Test
    void studentWithoutGroupSeesEmptyState() {
        when(groups.findGroupOf(classId, student.accountId())).thenReturn(Optional.empty());

        var view = service.myGroup(student, classId);

        assertThat(view.groupId()).isNull();
        assertThat(view.members()).isEmpty();
    }

    @Test
    void myGroupShowsOpenedGroupWorkAndOnlyMyOwnRequests() {
        ClassGroup group = new ClassGroup(UUID.randomUUID(), classId, "Nhóm 1", student.accountId(), null, NOW, NOW,
                List.of(student.accountId(), UUID.randomUUID()));
        when(groups.findGroupOf(classId, student.accountId())).thenReturn(Optional.of(group));
        when(assignments.listForClass(classId)).thenReturn(List.of(
                view(SubmissionMode.GROUP, AssignmentStatus.OPEN, "Bài nhóm 1"),
                view(SubmissionMode.GROUP, AssignmentStatus.SCHEDULED, "Bài chưa mở"),
                view(SubmissionMode.INDIVIDUAL, AssignmentStatus.OPEN, "Bài cá nhân")));
        when(documents.listByGroup(group.id())).thenReturn(List.of());
        when(groups.findPending(group.id())).thenReturn(Optional.of(new LeaderChangeRequest(UUID.randomUUID(), group.id(),
                UUID.randomUUID(), null, "Lý do của người khác", LeaderChangeRequest.Status.PENDING, null, null, null, null, NOW)));
        when(groups.latestOf(group.id(), student.accountId(), 2)).thenReturn(List.of());

        var view = service.myGroup(student, classId);

        assertThat(view.works()).extracting(MyGroupService.GroupWork::title).containsExactly("Bài nhóm 1");
        assertThat(view.pendingRequest()).isTrue();
        assertThat(view.myRequests()).isEmpty();
        assertThat(view.members()).hasSize(2);
        verify(groups).latestOf(eq(group.id()), eq(student.accountId()), eq(2));
    }

    private AssignmentView view(SubmissionMode mode, AssignmentStatus status, String title) {
        return new AssignmentView(UUID.randomUUID(), AssignmentScope.CLASS, classId, UUID.randomUUID(), 1, title, mode,
                GradingMode.GRADED, false, status, NOW, NOW.plusSeconds(3600), null, null, false, null, List.of());
    }
}
