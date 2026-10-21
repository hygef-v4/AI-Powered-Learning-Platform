package edu.aiplatform.groupdocs.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.aiplatform.academics.contract.IClassAccess;
import edu.aiplatform.assignments.contract.AssignmentContent;
import edu.aiplatform.assignments.contract.AssignmentScope;
import edu.aiplatform.assignments.contract.AssignmentStatus;
import edu.aiplatform.assignments.contract.AssignmentView;
import edu.aiplatform.assignments.contract.DocumentModel;
import edu.aiplatform.assignments.contract.DocumentPart;
import edu.aiplatform.assignments.contract.GradingMode;
import edu.aiplatform.assignments.contract.IAssignmentQuery;
import edu.aiplatform.assignments.contract.SubmissionMode;
import edu.aiplatform.assignments.contract.ReadinessResult;
import edu.aiplatform.groupdocs.domain.GroupDocument;
import edu.aiplatform.groupdocs.domain.GroupDocumentRepository;
import edu.aiplatform.groupdocs.domain.GroupSection;
import edu.aiplatform.groups.application.GroupReadinessService;
import edu.aiplatform.groups.domain.ClassGroup;
import edu.aiplatform.groups.domain.GroupRepository;
import edu.aiplatform.jobs.contract.IJob;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class GroupDocumentServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-22T03:00:00Z");
    private final UUID classId = UUID.randomUUID();
    private final UUID assignmentId = UUID.randomUUID();
    private final GroupDocumentRepository documents = mock(GroupDocumentRepository.class);
    private final GroupRepository groups = mock(GroupRepository.class);
    private final IAssignmentQuery assignments = mock(IAssignmentQuery.class);
    private final IClassAccess classes = mock(IClassAccess.class);
    private final GroupDocumentService service = new GroupDocumentService(documents, groups, assignments, classes,
            Clock.fixed(NOW, ZoneOffset.UTC));
    private final UUID a1 = UUID.randomUUID();
    private final UUID a2 = UUID.randomUUID();
    private final UUID left = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        when(assignments.get(assignmentId)).thenReturn(Optional.of(view(SubmissionMode.GROUP, AssignmentStatus.OPEN)));
        UUID heading = UUID.randomUUID();
        when(assignments.getContent(assignmentId)).thenReturn(Optional.of(new AssignmentContent(assignmentId, 1, "Đề",
                new DocumentModel(List.of()), List.of(new DocumentPart("p1", heading, List.of(), "1. Sơ đồ lớp"),
                new DocumentPart("p2", UUID.randomUUID(), List.of(heading), "2. Giải thích")), Map.of(), List.of())));
        when(classes.listActiveStudents(classId)).thenReturn(List.of(a1, a2));
    }

    @Test
    void eachGroupWithActiveMembersGetsOneDocumentWithOneSectionPerPart() {
        ClassGroup g1 = group(a1);
        ClassGroup gone = group(left);
        when(groups.listGroups(classId)).thenReturn(List.of(g1, gone));
        when(documents.insertIfAbsent(any())).thenReturn(true);

        assertThat(service.ensureDocuments(assignmentId)).isEqualTo(1);

        ArgumentCaptor<GroupDocument> doc = ArgumentCaptor.forClass(GroupDocument.class);
        verify(documents).insertIfAbsent(doc.capture());
        assertThat(doc.getValue().groupId()).isEqualTo(g1.id());
        assertThat(doc.getValue().status()).isEqualTo(GroupDocument.Status.IN_PROGRESS);
        assertThat(doc.getValue().activityMonitoring()).isTrue();
        ArgumentCaptor<GroupSection> sections = ArgumentCaptor.forClass(GroupSection.class);
        verify(documents, times(2)).insertSection(sections.capture());
        assertThat(sections.getAllValues()).extracting(GroupSection::partId).containsExactly("p1", "p2");
        assertThat(sections.getAllValues()).extracting(GroupSection::orderNo).containsExactly(1, 2);
        assertThat(sections.getAllValues()).allMatch(s -> s.status() == GroupSection.Status.OPEN);
    }

    @Test
    void runningAgainCreatesNothingNew() {
        when(groups.listGroups(classId)).thenReturn(List.of(group(a1)));
        when(documents.insertIfAbsent(any())).thenReturn(false);

        assertThat(service.ensureDocuments(assignmentId)).isZero();
        verify(documents, never()).insertSection(any());
    }

    @Test
    void individualOrClosedAssignmentGetsNoDocument() {
        when(assignments.get(assignmentId)).thenReturn(Optional.of(view(SubmissionMode.INDIVIDUAL, AssignmentStatus.OPEN)));
        assertThat(service.ensureDocuments(assignmentId)).isZero();
        when(assignments.get(assignmentId)).thenReturn(Optional.of(view(SubmissionMode.GROUP, AssignmentStatus.CLOSED)));
        assertThat(service.ensureDocuments(assignmentId)).isZero();
        verify(documents, never()).insertIfAbsent(any());
    }

    @Test
    void lifecycleQueuesCreationForGroupAssignmentsAndMarksRetired() {
        IJob jobs = mock(IJob.class);
        GroupAssignmentLifecycle lifecycle = new GroupAssignmentLifecycle(assignments, documents, jobs);

        lifecycle.onOpened(assignmentId);
        lifecycle.onRetired(assignmentId);

        verify(jobs).enqueue(eq("GROUP_DOC_CREATE"), eq(Map.of("assignmentId", assignmentId.toString())),
                eq("group-doc-create:" + assignmentId));
        verify(documents).markRetired(assignmentId);
    }

    @Test
    void readinessCountsOnlyActiveMembers() {
        GroupReadinessService readiness = new GroupReadinessService(groups, classes);
        when(groups.listGroups(classId)).thenReturn(List.of());
        assertThat(readiness.checkReadiness(classId)).isEqualTo(
                new ReadinessResult(List.of("CLASS_WITHOUT_GROUP"), List.of("STUDENT_WITHOUT_GROUP")));

        ClassGroup leaderLeft = new ClassGroup(UUID.randomUUID(), classId, "Nhóm 1", left, null, NOW, NOW, List.of(left, a1));
        ClassGroup empty = new ClassGroup(UUID.randomUUID(), classId, "Nhóm 2", left, null, NOW, NOW, List.of(left));
        when(groups.listGroups(classId)).thenReturn(List.of(leaderLeft, empty));
        assertThat(readiness.checkReadiness(classId)).isEqualTo(new ReadinessResult(
                List.of("GROUP_WITHOUT_LEADER", "GROUP_WITHOUT_MEMBER"), List.of("STUDENT_WITHOUT_GROUP")));

        ClassGroup ok = new ClassGroup(UUID.randomUUID(), classId, "Nhóm 1", a1, null, NOW, NOW, List.of(a1, a2));
        when(groups.listGroups(classId)).thenReturn(List.of(ok));
        assertThat(readiness.checkReadiness(classId)).isEqualTo(new ReadinessResult(List.of(), List.of()));
    }

    private ClassGroup group(UUID member) {
        return new ClassGroup(UUID.randomUUID(), classId, "Nhóm " + member, member, null, NOW, NOW, List.of(member));
    }

    private AssignmentView view(SubmissionMode mode, AssignmentStatus status) {
        return new AssignmentView(assignmentId, AssignmentScope.CLASS, classId, UUID.randomUUID(), 1, "Sơ đồ lớp", mode,
                GradingMode.GRADED, true, status, NOW, NOW.plusSeconds(86400), null, null, true, null, List.of());
    }
}
