package edu.aiplatform.groupdocs.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.aiplatform.academics.contract.ClassRef;
import edu.aiplatform.academics.contract.ClassStatus;
import edu.aiplatform.academics.contract.IClassAccess;
import edu.aiplatform.assignments.contract.DocumentModel;
import edu.aiplatform.assignments.contract.IAssignmentQuery;
import edu.aiplatform.assignmentwork.contract.ActivityEventInput;
import edu.aiplatform.assignmentwork.contract.ActivityKind;
import edu.aiplatform.assignmentwork.contract.IActivityWrite;
import edu.aiplatform.assignmentwork.contract.WorkKind;
import edu.aiplatform.assignmentwork.contract.WorkRef;
import edu.aiplatform.audit.contract.IAudit;
import edu.aiplatform.groupdocs.domain.GroupDocument;
import edu.aiplatform.groupdocs.domain.GroupDocument.SectionAuthor;
import edu.aiplatform.groupdocs.domain.GroupDocumentRepository;
import edu.aiplatform.groupdocs.domain.GroupSection;
import edu.aiplatform.groups.application.GroupAccess;
import edu.aiplatform.groups.domain.ClassGroup;
import edu.aiplatform.groups.domain.GroupRepository;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.AuthorizationDecision;
import edu.aiplatform.identity.contract.IAuthorization;
import edu.aiplatform.identity.contract.Role;
import edu.aiplatform.shared.error.ApiException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GroupWorkQueryServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-22T03:00:00Z");
    private final UUID classId = UUID.randomUUID();
    private final UUID assignmentId = UUID.randomUUID();
    private final Actor member = new Actor(UUID.randomUUID(), Role.STUDENT);
    private final Actor teacher = new Actor(UUID.randomUUID(), Role.TEACHER);
    private final GroupDocumentRepository documents = mock(GroupDocumentRepository.class);
    private final GroupRepository groups = mock(GroupRepository.class);
    private final IAuthorization authorization = mock(IAuthorization.class);
    private final IClassAccess classes = mock(IClassAccess.class);
    private final IActivityWrite activity = mock(IActivityWrite.class);
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final GroupAccess access = new GroupAccess(authorization, classes, mock(IAudit.class), clock);
    private final GroupWorkQueryService service = new GroupWorkQueryService(documents, groups,
            new GroupDocAccess(documents, groups, access, mock(IAssignmentQuery.class), clock), access, activity,
            mock(IAssignmentQuery.class));
    private final ClassGroup g1 = new ClassGroup(UUID.randomUUID(), classId, "Nhóm 1", member.accountId(), null, NOW, NOW,
            List.of(member.accountId()));
    private final ClassGroup g2 = new ClassGroup(UUID.randomUUID(), classId, "Nhóm 2", null, null, NOW, NOW, List.of());

    @BeforeEach
    void setUp() {
        when(authorization.authorize(any(), any(), any())).thenReturn(AuthorizationDecision.allow());
        when(classes.isActiveStudent(any(), eq(classId))).thenReturn(true);
        when(classes.getClassRef(classId)).thenReturn(Optional.of(
                new ClassRef(classId, UUID.randomUUID(), "SE1801", "SWP391", ClassStatus.OPEN, teacher.accountId(), true)));
        when(groups.findGroup(g1.id())).thenReturn(Optional.of(g1));
        when(groups.listGroups(classId)).thenReturn(List.of(g1, g2));
    }

    @Test
    void submissionForGradingOnlyAfterSubmitAndOnlyDoneAuthors() {
        GroupDocument draft = doc(false);
        when(documents.findById(draft.id())).thenReturn(Optional.of(draft));
        assertThat(service.getGroupSubmission(draft.id())).isEmpty();

        GroupDocument submitted = doc(true);
        when(documents.findById(submitted.id())).thenReturn(Optional.of(submitted));
        var view = service.getGroupSubmission(submitted.id()).orElseThrow();
        assertThat(view.submissionHash()).isEqualTo("sha256:x");
        assertThat(view.sectionAuthors()).extracting(a -> a.partId()).containsExactly("p1");
        assertThat(view.memberIds()).containsExactly(member.accountId());
    }

    @Test
    void groupListForGradingIncludesGroupsWithoutDocument() {
        GroupDocument submitted = doc(true);
        when(documents.listByAssignmentAndClass(assignmentId, classId)).thenReturn(List.of(submitted));

        var list = service.listGroupSubmissions(assignmentId, classId);

        assertThat(list).extracting(s -> s.groupName()).containsExactly("Nhóm 1", "Nhóm 2");
        assertThat(list.get(0).submitted()).isTrue();
        assertThat(list.get(1).groupDocumentId()).isNull();
    }

    @Test
    void statusForStudentAssignmentsUsesOwnGroups() {
        GroupDocument submitted = doc(true);
        when(groups.listGroupIdsOf(member.accountId())).thenReturn(List.of(g1.id()));
        when(documents.listByGroups(List.of(g1.id()), List.of(assignmentId))).thenReturn(List.of(submitted));

        var status = service.statusFor(member.accountId(), List.of(assignmentId));

        assertThat(status.get(assignmentId).groupDocumentId()).isEqualTo(submitted.id());
        assertThat(status.get(assignmentId).submitted()).isTrue();
    }

    @Test
    void progressCountsSectionsPerGroupForTeacher() {
        GroupDocument d = doc(false);
        when(documents.listByAssignmentAndClass(assignmentId, classId)).thenReturn(List.of(d));
        when(documents.listSections(d.id())).thenReturn(List.of(
                section(d, GroupSection.Status.DONE), section(d, GroupSection.Status.CLAIMED), section(d, GroupSection.Status.OPEN)));

        var progress = service.progress(teacher, classId, assignmentId);

        assertThat(progress.get(0).done()).isEqualTo(1);
        assertThat(progress.get(0).claimed()).isEqualTo(1);
        assertThat(progress.get(0).open()).isEqualTo(1);
        assertThat(progress.get(1).documentId()).isNull();
    }

    @Test
    void activityIsRecordedOnlyForMembersWhenMonitoringIsOn() {
        GroupDocument monitored = new GroupDocument(UUID.randomUUID(), g1.id(), assignmentId, classId,
                GroupDocument.Status.IN_PROGRESS, new DocumentModel(List.of()), true, null, null, List.of(), null, null, null,
                false, null, List.of(), NOW, 0, NOW);
        when(documents.findById(monitored.id())).thenReturn(Optional.of(monitored));
        List<ActivityEventInput> events = List.of(new ActivityEventInput(UUID.randomUUID(), "e1", 1,
                ActivityKind.values()[0], NOW));

        service.recordActivity(member, monitored.id(), events);
        verify(activity).record(member, new WorkRef(WorkKind.GROUP_DOCUMENT, monitored.id(), classId), events);

        GroupDocument off = doc(false);
        when(documents.findById(off.id())).thenReturn(Optional.of(off));
        assertThatThrownBy(() -> service.recordActivity(member, off.id(), events))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("GROUP_DOC_ACTIVITY_OFF"));
        assertThatThrownBy(() -> service.recordActivity(teacher, monitored.id(), events))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("GROUP_DOC_MEMBER_ONLY"));
        verify(activity).record(any(), any(), any());
    }

    private GroupDocument doc(boolean submitted) {
        GroupDocument d = GroupDocument.create(UUID.randomUUID(), g1.id(), assignmentId, classId, new DocumentModel(List.of()),
                false, NOW.minusSeconds(60));
        return submitted ? d.withSubmission(new DocumentModel(List.of()), List.of(
                new SectionAuthor("p1", "Mục 1", member.accountId(), true), new SectionAuthor("p2", "Mục 2", null, false)),
                member.accountId(), GroupDocument.SubmitMode.MANUAL, NOW, false, "sha256:x", List.of(),
                GroupDocument.Status.IN_PROGRESS) : d;
    }

    private static GroupSection section(GroupDocument d, GroupSection.Status status) {
        return new GroupSection(UUID.randomUUID(), d.id(), "p" + status, null, List.of(), 1, "Mục", status, null, null, null,
                null, null, null, null, 0);
    }
}
