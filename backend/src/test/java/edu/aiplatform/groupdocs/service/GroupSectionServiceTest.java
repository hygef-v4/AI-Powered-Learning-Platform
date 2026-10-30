package edu.aiplatform.groupdocs.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import edu.aiplatform.academics.contract.ClassRef;
import edu.aiplatform.academics.contract.ClassStatus;
import edu.aiplatform.academics.contract.IClassAccess;
import edu.aiplatform.assignments.contract.BlockOrigin;
import edu.aiplatform.assignments.contract.BlockType;
import edu.aiplatform.assignments.contract.DocumentBlock;
import edu.aiplatform.assignments.contract.DocumentModel;
import edu.aiplatform.assignments.contract.IAssignmentQuery;
import edu.aiplatform.assignments.contract.IDocumentModel;
import edu.aiplatform.assignments.contract.SubmissionWindow;
import edu.aiplatform.assignments.contract.ValidationResult;
import edu.aiplatform.audit.contract.AuditEvent;
import edu.aiplatform.audit.contract.IAudit;
import edu.aiplatform.groupdocs.contract.ISseHub;
import edu.aiplatform.groupdocs.model.GroupDocument;
import edu.aiplatform.groupdocs.repository.GroupDocumentRepository;
import edu.aiplatform.groupdocs.model.GroupSection;
import edu.aiplatform.groupdocs.model.GroupSection.Status;
import edu.aiplatform.groupdocs.model.SectionRevision;
import edu.aiplatform.groupdocs.model.SectionRules;
import edu.aiplatform.groups.service.GroupAccess;
import edu.aiplatform.groups.model.ClassGroup;
import edu.aiplatform.groups.repository.GroupRepository;
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
import org.mockito.ArgumentCaptor;

class GroupSectionServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-22T03:00:00Z");
    private final UUID classId = UUID.randomUUID();
    private final UUID assignmentId = UUID.randomUUID();
    private final Actor leader = new Actor(UUID.randomUUID(), Role.STUDENT);
    private final Actor member = new Actor(UUID.randomUUID(), Role.STUDENT);
    private final Actor outsider = new Actor(UUID.randomUUID(), Role.STUDENT);
    private final Actor teacher = new Actor(UUID.randomUUID(), Role.TEACHER);
    private final GroupDocumentRepository documents = mock(GroupDocumentRepository.class);
    private final GroupRepository groups = mock(GroupRepository.class);
    private final IAuthorization authorization = mock(IAuthorization.class);
    private final IClassAccess classes = mock(IClassAccess.class);
    private final IAssignmentQuery assignments = mock(IAssignmentQuery.class);
    private final IDocumentModel documentModel = mock(IDocumentModel.class);
    private final ISseHub sse = mock(ISseHub.class);
    private final IAudit audit = mock(IAudit.class);
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final GroupSectionService service = new GroupSectionService(documents,
            new GroupDocAccess(documents, groups, new GroupAccess(authorization, classes, audit, clock), assignments, clock),
            documentModel, sse, audit, new ObjectMapper(), clock);
    private final ClassGroup group = new ClassGroup(UUID.randomUUID(), classId, "Nhóm 1", leader.accountId(), null, NOW,
            NOW, List.of(leader.accountId(), member.accountId()));
    private final GroupDocument document = GroupDocument.create(UUID.randomUUID(), group.id(), assignmentId, classId,
            new DocumentModel(List.of()), false, NOW);
    private final UUID sectionId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        when(authorization.authorize(any(), any(), any())).thenReturn(AuthorizationDecision.allow());
        when(authorization.authorize(eq(outsider), any(), any())).thenReturn(AuthorizationDecision.allow());
        when(classes.isActiveStudent(any(), eq(classId))).thenReturn(true);
        when(classes.getClassRef(classId)).thenReturn(Optional.of(
                new ClassRef(classId, UUID.randomUUID(), "SE1801", "SWP391", ClassStatus.OPEN, teacher.accountId(), true)));
        when(documents.findById(document.id())).thenReturn(Optional.of(document));
        when(groups.findGroup(group.id())).thenReturn(Optional.of(group));
        when(assignments.submissionWindow(eq(assignmentId), any())).thenReturn(SubmissionWindow.ON_TIME);
        when(documents.updateSection(any())).thenReturn(true);
        when(documentModel.validateForSave(any(), any())).thenReturn(new ValidationResult(List.of(), List.of()));
    }

    @Test
    void claimingSectionHeldByAnotherMemberIsRefused() {
        section(Status.CLAIMED, leader.accountId(), null, 2);

        assertThatThrownBy(() -> service.claim(member, document.id(), sectionId))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("SECTION_CLAIMED_BY_OTHER"));
        verify(documents, never()).updateSection(any());
    }

    @Test
    void memberClaimsOpenSectionAndEveryoneIsNotified() {
        section(Status.OPEN, null, null, 0);

        var view = service.claim(member, document.id(), sectionId);

        assertThat(view.status()).isEqualTo(Status.CLAIMED);
        assertThat(view.holderId()).isEqualTo(member.accountId());
        assertThat(view.version()).isEqualTo(1);
        verify(sse).publish(eq("group-doc:" + document.id()), any());
    }

    @Test
    void leaderMustReleaseBeforeReassigningAndAssignmentIsAudited() {
        section(Status.CLAIMED, leader.accountId(), null, 2);
        assertThatThrownBy(() -> service.assign(leader, document.id(), sectionId, member.accountId(), false))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("SECTION_CLAIMED_BY_OTHER"));

        var view = service.assign(leader, document.id(), sectionId, member.accountId(), true);

        assertThat(view.holderId()).isEqualTo(member.accountId());
        assertThat(view.assignedBy()).isEqualTo(leader.accountId());
        ArgumentCaptor<AuditEvent> events = ArgumentCaptor.forClass(AuditEvent.class);
        verify(audit, org.mockito.Mockito.times(2)).record(events.capture());
        assertThat(events.getAllValues()).extracting(AuditEvent::action)
                .containsExactly("GROUP_SECTION_RELEASED", "GROUP_SECTION_ASSIGNED");
        assertThatThrownBy(() -> service.assign(member, document.id(), sectionId, member.accountId(), true))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("GROUP_DOC_LEADER_ONLY"));
    }

    @Test
    void releaseGoesBackToDoneWhenPublishedOrOpenOtherwise() {
        section(Status.CLAIMED, member.accountId(), List.of(block()), 3);
        assertThat(service.release(teacher, document.id(), sectionId).status()).isEqualTo(Status.DONE);
        verify(audit).record(any());

        section(Status.CLAIMED, member.accountId(), null, 3);
        assertThat(service.release(member, document.id(), sectionId).status()).isEqualTo(Status.OPEN);
    }

    @Test
    void draftWithStaleVersionIsRejectedAndOnlyHolderCanSave() {
        section(Status.CLAIMED, member.accountId(), null, 5);

        assertThatThrownBy(() -> service.saveDraft(member, document.id(), sectionId, 4L, List.of(block())))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("SECTION_VERSION_CONFLICT"));
        assertThatThrownBy(() -> service.saveDraft(leader, document.id(), sectionId, 5L, List.of(block())))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("SECTION_NOT_HELD"));
        var saved = service.saveDraft(member, document.id(), sectionId, 5L, List.of(block()));
        assertThat(saved.draftBlocks()).hasSize(1);
        verify(documents).touch(document.id(), NOW);
    }

    @Test
    void doneMovesDraftToPublishedAndKeepsRevision() {
        section(Status.CLAIMED, member.accountId(), null, 1);
        GroupSection held = documents.lockSection(document.id(), sectionId).orElseThrow().withDraft(List.of(block()), NOW);
        when(documents.lockSection(document.id(), sectionId)).thenReturn(Optional.of(held));

        var done = service.done(member, document.id(), sectionId);

        assertThat(done.status()).isEqualTo(Status.DONE);
        assertThat(done.publishedBlocks()).hasSize(1);
        assertThat(done.lastAuthorId()).isEqualTo(member.accountId());
        ArgumentCaptor<SectionRevision> revision = ArgumentCaptor.forClass(SectionRevision.class);
        verify(documents).insertRevision(revision.capture());
        assertThat(revision.getValue().authorId()).isEqualTo(member.accountId());
    }

    @Test
    void outsiderGetsNotFoundTeacherCannotEditAndDeadlineStopsEdits() {
        section(Status.OPEN, null, null, 0);
        assertThatThrownBy(() -> service.claim(outsider, document.id(), sectionId))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo(GroupAccess.NOT_FOUND));
        assertThatThrownBy(() -> service.claim(teacher, document.id(), sectionId))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("GROUP_DOC_MEMBER_ONLY"));
        when(assignments.submissionWindow(eq(assignmentId), any())).thenReturn(SubmissionWindow.CLOSED);
        assertThatThrownBy(() -> service.claim(member, document.id(), sectionId))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("GROUP_DOC_DEADLINE_PASSED"));
    }

    @Test
    void draftIsCheckedInsideTheSharedSkeletonByTheRealValidator() {
        DocumentBlock first = heading("1. Phân tích");
        DocumentBlock second = heading("2. Thiết kế");
        GroupDocument withSkeleton = GroupDocument.create(document.id(), group.id(), assignmentId, classId,
                new DocumentModel(List.of(first, second)), false, NOW);
        when(documents.findById(document.id())).thenReturn(Optional.of(withSkeleton));
        GroupSection held = new GroupSection(sectionId, document.id(), "p1", first.id(), List.of(), 1, "1. Phân tích",
                Status.CLAIMED, member.accountId(), NOW, null, null, null, null, null, 2);
        when(documents.lockSection(document.id(), sectionId)).thenReturn(Optional.of(held));
        GroupSectionService real = new GroupSectionService(documents,
                new GroupDocAccess(documents, groups, new GroupAccess(authorization, classes, audit, clock), assignments, clock),
                new edu.aiplatform.assignments.service.DocumentModelService(new ObjectMapper()), sse, audit,
                new ObjectMapper(), clock);

        var saved = real.saveDraft(member, document.id(), sectionId, 2L, List.of(paragraph("Bài làm của nhóm")));

        assertThat(saved.draftBlocks()).hasSize(1);
        assertThatThrownBy(() -> real.saveDraft(member, document.id(), sectionId, 2L, List.of(first)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("SECTION_CONTENT_INVALID"));
    }

    @Test
    void graceOnlyExtendsTheDeadlineNotTheOpening() {
        section(Status.CLAIMED, member.accountId(), null, 1);
        // Vừa mở bài: lùi 30 giây thì trước giờ mở (CLOSED), nhưng hiện tại vẫn đang nhận bài.
        when(assignments.submissionWindow(eq(assignmentId), eq(NOW))).thenReturn(SubmissionWindow.ON_TIME);
        when(assignments.submissionWindow(eq(assignmentId), eq(NOW.minus(SectionRules.GRACE)))).thenReturn(SubmissionWindow.CLOSED);
        assertThat(service.saveDraft(member, document.id(), sectionId, 1L, List.of(block())).draftBlocks()).hasSize(1);

        // Vừa quá hạn: lần lưu nháp cuối trong ân hạn vẫn nhận, nhận mục mới thì không.
        section(Status.CLAIMED, member.accountId(), null, 1);
        when(assignments.submissionWindow(eq(assignmentId), eq(NOW))).thenReturn(SubmissionWindow.CLOSED);
        when(assignments.submissionWindow(eq(assignmentId), eq(NOW.minus(SectionRules.GRACE)))).thenReturn(SubmissionWindow.ON_TIME);
        assertThat(service.saveDraft(member, document.id(), sectionId, 1L, List.of(block())).draftBlocks()).hasSize(1);
        section(Status.OPEN, null, null, 0);
        assertThatThrownBy(() -> service.claim(member, document.id(), sectionId))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("GROUP_DOC_DEADLINE_PASSED"));
    }

    private static DocumentBlock heading(String text) {
        return new DocumentBlock(UUID.randomUUID(), BlockOrigin.TEACHER, BlockType.HEADING,
                JsonNodeFactory.instance.objectNode().put("style", "H1").put("text", text));
    }

    private static DocumentBlock paragraph(String text) {
        var content = JsonNodeFactory.instance.objectNode();
        content.putArray("runs").addObject().put("text", text);
        return new DocumentBlock(UUID.randomUUID(), BlockOrigin.STUDENT, BlockType.PARAGRAPH, content);
    }

    private void section(Status status, UUID holder, List<DocumentBlock> published, long version) {
        GroupSection section = new GroupSection(sectionId, document.id(), "p1", null, List.of(), 1, "1. Sơ đồ", status, holder,
                holder == null ? null : NOW, null, published, null, null, null, version);
        when(documents.lockSection(document.id(), sectionId)).thenReturn(Optional.of(section));
    }

    private static DocumentBlock block() {
        return new DocumentBlock(UUID.randomUUID(), BlockOrigin.STUDENT, BlockType.values()[0],
                JsonNodeFactory.instance.objectNode().put("text", "Nội dung"));
    }
}
