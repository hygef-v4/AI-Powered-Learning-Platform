package edu.aiplatform.groupdocs.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import edu.aiplatform.academics.contract.ClassRef;
import edu.aiplatform.academics.contract.ClassStatus;
import edu.aiplatform.academics.contract.IClassAccess;
import edu.aiplatform.assignments.contract.AssignmentScope;
import edu.aiplatform.assignments.contract.AssignmentStatus;
import edu.aiplatform.assignments.contract.AssignmentView;
import edu.aiplatform.assignments.contract.DocumentModel;
import edu.aiplatform.assignments.contract.GradingMode;
import edu.aiplatform.assignments.contract.IAssignmentQuery;
import edu.aiplatform.assignments.contract.SubmissionMode;
import edu.aiplatform.assignments.contract.SubmissionWindow;
import edu.aiplatform.audit.contract.IAudit;
import edu.aiplatform.groupdocs.model.GroupDocument;
import edu.aiplatform.groupdocs.repository.GroupDocumentRepository;
import edu.aiplatform.groups.service.GroupAccess;
import edu.aiplatform.groups.model.ClassGroup;
import edu.aiplatform.groups.repository.GroupRepository;
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

class GroupWorkspaceServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-25T03:00:00Z");
    private final UUID classId = UUID.randomUUID();
    private final UUID assignmentId = UUID.randomUUID();
    private final Actor member = new Actor(UUID.randomUUID(), Role.STUDENT);
    private final GroupDocumentRepository documents = mock(GroupDocumentRepository.class);
    private final GroupRepository groups = mock(GroupRepository.class);
    private final IAuthorization authorization = mock(IAuthorization.class);
    private final IClassAccess classes = mock(IClassAccess.class);
    private final IAssignmentQuery assignments = mock(IAssignmentQuery.class);
    private final IAudit audit = mock(IAudit.class);
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final GroupAccess groupAccess = new GroupAccess(authorization, classes, audit, clock);
    private final GroupWorkspaceService service = new GroupWorkspaceService(documents, groups,
            new GroupDocAccess(documents, groups, groupAccess, assignments, clock), groupAccess,
            mock(GroupDocumentService.class), assignments, mock(IAccountLookup.class));
    private final ClassGroup group = new ClassGroup(UUID.randomUUID(), classId, "Nhóm 1", member.accountId(), null, NOW,
            NOW, List.of(member.accountId()));
    private final GroupDocument document = GroupDocument.create(UUID.randomUUID(), group.id(), assignmentId, classId,
            new DocumentModel(List.of()), true, NOW);

    @BeforeEach
    void setUp() {
        when(authorization.authorize(any(), any(), any())).thenReturn(AuthorizationDecision.allow());
        when(classes.isActiveStudent(any(), eq(classId))).thenReturn(true);
        when(classes.getClassRef(classId)).thenReturn(Optional.of(
                new ClassRef(classId, UUID.randomUUID(), "SE1801", "SWP391", ClassStatus.OPEN, UUID.randomUUID(), true)));
        when(documents.findById(document.id())).thenReturn(Optional.of(document));
        when(documents.listSections(document.id())).thenReturn(List.of());
        when(groups.findGroup(group.id())).thenReturn(Optional.of(group));
        when(assignments.submissionWindow(eq(assignmentId), any())).thenReturn(SubmissionWindow.ON_TIME);
    }

    @Test
    void workspaceCarriesDiagramFlagFromTheAssignment() {
        when(assignments.get(assignmentId)).thenReturn(Optional.of(assignment(true)));
        var view = service.workspace(member, document.id());
        assertThat(view.diagramEnabled()).isTrue();
        assertThat(view.activityMonitoring()).isTrue();

        when(assignments.get(assignmentId)).thenReturn(Optional.of(assignment(false)));
        assertThat(service.workspace(member, document.id()).diagramEnabled()).isFalse();
    }

    private AssignmentView assignment(boolean diagramEnabled) {
        return new AssignmentView(assignmentId, AssignmentScope.CLASS, classId, UUID.randomUUID(), 1, "Bài nhóm",
                SubmissionMode.GROUP, GradingMode.GRADED, diagramEnabled, AssignmentStatus.OPEN, NOW, NOW.plusSeconds(3600),
                null, 1, true, null, List.of());
    }
}
