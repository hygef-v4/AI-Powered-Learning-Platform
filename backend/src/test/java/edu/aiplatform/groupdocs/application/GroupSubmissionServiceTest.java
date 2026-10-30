package edu.aiplatform.groupdocs.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import edu.aiplatform.academics.contract.ClassRef;
import edu.aiplatform.academics.contract.ClassStatus;
import edu.aiplatform.academics.contract.IClassAccess;
import edu.aiplatform.assignments.contract.AssignmentScope;
import edu.aiplatform.assignments.contract.AssignmentStatus;
import edu.aiplatform.assignments.contract.AssignmentView;
import edu.aiplatform.assignments.contract.BlockOrigin;
import edu.aiplatform.assignments.contract.BlockType;
import edu.aiplatform.assignments.contract.DocumentBlock;
import edu.aiplatform.assignments.contract.DocumentModel;
import edu.aiplatform.assignments.contract.GradingMode;
import edu.aiplatform.assignments.contract.IAssignmentQuery;
import edu.aiplatform.assignments.contract.IDocumentModel;
import edu.aiplatform.assignments.contract.IDocx;
import edu.aiplatform.assignments.contract.SubmissionMode;
import edu.aiplatform.assignments.contract.SubmissionWindow;
import edu.aiplatform.assignments.contract.ValidationIssue;
import edu.aiplatform.assignments.contract.ValidationResult;
import edu.aiplatform.audit.contract.IAudit;
import edu.aiplatform.grading.contract.IGroupEvaluation;
import edu.aiplatform.groupdocs.contract.ISseHub;
import edu.aiplatform.groupdocs.domain.GroupDocument;
import edu.aiplatform.groupdocs.domain.GroupDocumentRepository;
import edu.aiplatform.groupdocs.domain.GroupSection;
import edu.aiplatform.groupdocs.domain.GroupSection.Status;
import edu.aiplatform.groupdocs.domain.SectionRevision;
import edu.aiplatform.groups.application.GroupAccess;
import edu.aiplatform.groups.domain.ClassGroup;
import edu.aiplatform.groups.domain.GroupRepository;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.AuthorizationDecision;
import edu.aiplatform.identity.contract.IAuthorization;
import edu.aiplatform.identity.contract.Role;
import edu.aiplatform.jobs.contract.IEventPublisher;
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

class GroupSubmissionServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-22T03:00:00Z");
    private final UUID classId = UUID.randomUUID();
    private final UUID assignmentId = UUID.randomUUID();
    private final Actor leader = new Actor(UUID.randomUUID(), Role.STUDENT);
    private final Actor member = new Actor(UUID.randomUUID(), Role.STUDENT);
    private final GroupDocumentRepository documents = mock(GroupDocumentRepository.class);
    private final GroupRepository groups = mock(GroupRepository.class);
    private final IAuthorization authorization = mock(IAuthorization.class);
    private final IClassAccess classes = mock(IClassAccess.class);
    private final IAssignmentQuery assignments = mock(IAssignmentQuery.class);
    private final IDocumentModel documentModel = mock(IDocumentModel.class);
    private final IGroupEvaluation evaluation = mock(IGroupEvaluation.class);
    private final IAudit audit = mock(IAudit.class);
    private final ISseHub sse = mock(ISseHub.class);
    private final IEventPublisher events = mock(IEventPublisher.class);
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final GroupSubmissionService service = new GroupSubmissionService(documents, groups,
            new GroupDocAccess(documents, groups, new GroupAccess(authorization, classes, audit, clock), assignments, clock),
            assignments, documentModel, mock(IDocx.class), evaluation, sse, audit,
            events, new ObjectMapper().findAndRegisterModules(), clock);
    private final ClassGroup group = new ClassGroup(UUID.randomUUID(), classId, "Nhóm 1", leader.accountId(), null, NOW,
            NOW, List.of(leader.accountId(), member.accountId()));
    private final UUID heading1 = UUID.randomUUID();
    private final UUID heading2 = UUID.randomUUID();
    private final DocumentModel skeleton = new DocumentModel(List.of(block(heading1, BlockType.HEADING, "1"),
            block(UUID.randomUUID(), BlockType.PARAGRAPH, "hướng dẫn 1"), block(heading2, BlockType.HEADING, "2")));
    private GroupDocument document;

    @BeforeEach
    void setUp() {
        document = GroupDocument.create(UUID.randomUUID(), group.id(), assignmentId, classId, skeleton, false,
                NOW.minusSeconds(3600));
        when(authorization.authorize(any(), any(), any())).thenReturn(AuthorizationDecision.allow());
        when(classes.isActiveStudent(any(), eq(classId))).thenReturn(true);
        when(classes.getClassRef(classId)).thenReturn(Optional.of(
                new ClassRef(classId, UUID.randomUUID(), "SE1801", "SWP391", ClassStatus.OPEN, UUID.randomUUID(), true)));
        when(documents.lockDocument(document.id())).thenReturn(Optional.of(document));
        when(groups.findGroup(group.id())).thenReturn(Optional.of(group));
        when(assignments.submissionWindow(eq(assignmentId), any())).thenReturn(SubmissionWindow.ON_TIME);
        when(assignments.get(assignmentId)).thenReturn(Optional.of(new AssignmentView(assignmentId, AssignmentScope.CLASS,
                classId, UUID.randomUUID(), 1, "Sơ đồ lớp", SubmissionMode.GROUP, GradingMode.GRADED, true,
                AssignmentStatus.OPEN, NOW.minusSeconds(86400), NOW.plusSeconds(3600), null, null, false, null, List.of())));
        when(assignments.getContent(assignmentId)).thenReturn(Optional.empty());
        when(documentModel.validateForSubmit(any(), any(), any())).thenReturn(new ValidationResult(List.of(), List.of()));
        when(documents.update(any())).thenReturn(true);
        when(documents.updateSection(any())).thenReturn(true);
    }

    @Test
    void leaderSubmitsDoneSectionsOnlyAndGradingIsToldInTheSameCall() {
        DocumentBlock answer = block(UUID.randomUUID(), BlockType.PARAGRAPH, "bài làm 1");
        when(documents.listSections(document.id())).thenReturn(List.of(
                section("p1", heading1, 1, Status.DONE, null, List.of(answer), member.accountId()),
                section("p2", heading2, 2, Status.CLAIMED, leader.accountId(), null, null)));

        var receipt = service.submit(leader, document.id(), 0L);

        assertThat(receipt.mode()).isEqualTo("MANUAL");
        assertThat(receipt.late()).isFalse();
        assertThat(receipt.receiptHash()).startsWith("sha256:");
        assertThat(receipt.warnings()).anyMatch(w -> w.contains("chưa Xong"));
        ArgumentCaptor<GroupDocument> saved = ArgumentCaptor.forClass(GroupDocument.class);
        verify(documents).update(saved.capture());
        assertThat(saved.getValue().status()).isEqualTo(GroupDocument.Status.IN_PROGRESS);
        assertThat(saved.getValue().submittedDocument().blocks()).extracting(DocumentBlock::id)
                .containsExactly(heading1, skeleton.blocks().get(1).id(), answer.id(), heading2);
        assertThat(saved.getValue().sectionAuthors()).extracting(GroupDocument.SectionAuthor::accountId)
                .containsExactly(member.accountId(), null);
        verify(evaluation).onGroupSubmitted(document.id(), receipt.receiptHash(), assignmentId, classId, group.memberIds());
    }

    @Test
    void memberCannotSubmitAndInvalidDocumentIsRejectedWithIssues() {
        when(documents.listSections(document.id())).thenReturn(List.of());
        assertThatThrownBy(() -> service.submit(member, document.id(), 0L))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("GROUP_DOC_LEADER_ONLY"));

        when(documentModel.validateForSubmit(any(), any(), any())).thenReturn(new ValidationResult(
                List.of(new ValidationIssue("DIAGRAM_REQUIRED", null, "Thiếu sơ đồ lớp")), List.of()));
        assertThatThrownBy(() -> service.submit(leader, document.id(), 0L))
                .isInstanceOfSatisfying(GroupSubmissionService.SubmitInvalidException.class,
                        e -> assertThat(e.issues()).extracting(ValidationIssue::code).containsExactly("DIAGRAM_REQUIRED"));
        verify(evaluation, never()).onGroupSubmitted(any(), anyString(), any(), any(), any());
    }

    @Test
    void autoSubmitFinalizesHeldDraftAndClosesTheDocument() {
        DocumentBlock draft = block(UUID.randomUUID(), BlockType.PARAGRAPH, "nháp chưa Xong");
        GroupSection held = section("p1", heading1, 1, Status.CLAIMED, member.accountId(), null, null)
                .withDraft(List.of(draft), NOW.minusSeconds(40));
        when(documents.lockSections(document.id())).thenReturn(List.of(held));
        when(documents.listSections(document.id())).thenReturn(List.of(held.done(NOW)));

        var receipt = service.autoSubmit(document.id()).orElseThrow();

        assertThat(receipt.mode()).isEqualTo("AUTO_DEADLINE");
        assertThat(receipt.submittedBy()).isNull();
        ArgumentCaptor<SectionRevision> revision = ArgumentCaptor.forClass(SectionRevision.class);
        verify(documents).insertRevision(revision.capture());
        assertThat(revision.getValue().authorId()).isEqualTo(member.accountId());
        ArgumentCaptor<GroupDocument> saved = ArgumentCaptor.forClass(GroupDocument.class);
        verify(documents).update(saved.capture());
        assertThat(saved.getValue().status()).isEqualTo(GroupDocument.Status.CLOSED);
        verify(evaluation).onGroupSubmitted(eq(document.id()), anyString(), eq(assignmentId), eq(classId), any());
    }

    @Test
    void autoSubmitOnClosedDocumentDoesNothing() {
        when(documents.lockDocument(document.id())).thenReturn(Optional.of(document.closed()));

        assertThat(service.autoSubmit(document.id())).isEmpty();
        verify(documents, never()).update(any());
        verify(evaluation, never()).onGroupSubmitted(any(), anyString(), any(), any(), any());
    }

    @Test
    void retiredAssignmentUsesRetiredMode() {
        document = document.withAutoSubmitReason(GroupDocument.SubmitMode.AUTO_RETIRED);
        when(documents.lockDocument(document.id())).thenReturn(Optional.of(document));
        when(documents.lockSections(document.id())).thenReturn(List.of());
        when(documents.listSections(document.id())).thenReturn(List.of());

        assertThat(service.autoSubmit(document.id()).orElseThrow().mode()).isEqualTo("AUTO_RETIRED");
    }

    @Test
    void staleSubmissionVersionHasNoPersistenceOrDeliverySideEffects() {
        assertThatThrownBy(() -> service.submit(leader, document.id(), 1L))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("GROUP_DOC_VERSION_CONFLICT"));
        verify(documents, never()).update(any());
        verify(documents, never()).listSections(any());
        verifyNoInteractions(documentModel, evaluation, events, sse, audit);
    }

    @Test
    void lostDocumentCasDoesNotPublishAReceiptOrTriggerGrading() {
        when(documents.listSections(document.id())).thenReturn(List.of());
        when(documents.update(any())).thenReturn(false);

        assertThatThrownBy(() -> service.submit(leader, document.id(), 0L))
                .isInstanceOfSatisfying(ApiException.class,
                        e -> assertThat(e.code()).isEqualTo("GROUP_DOC_VERSION_CONFLICT"));

        verifyNoInteractions(evaluation, events, sse, audit);
    }

    @Test
    void resubmitBuildsANewSnapshotWithoutChangingThePreviousReceipt() {
        DocumentBlock first = block(UUID.randomUUID(), BlockType.PARAGRAPH, "first answer");
        DocumentBlock second = block(UUID.randomUUID(), BlockType.PARAGRAPH, "replacement answer");
        when(documents.listSections(document.id())).thenReturn(List.of(
                section("p1", heading1, 1, Status.DONE, null, List.of(first), member.accountId())));
        var firstReceipt = service.submit(leader, document.id(), 0L);
        ArgumentCaptor<GroupDocument> saved = ArgumentCaptor.forClass(GroupDocument.class);
        verify(documents).update(saved.capture());
        GroupDocument originalSnapshot = saved.getValue();
        when(documents.lockDocument(document.id())).thenReturn(Optional.of(persistedVersion(originalSnapshot, 1)));
        when(documents.listSections(document.id())).thenReturn(List.of(
                section("p1", heading1, 1, Status.DONE, null, List.of(second), leader.accountId())));

        var secondReceipt = service.submit(leader, document.id(), 1L);

        verify(documents, times(2)).update(saved.capture());
        GroupDocument newSnapshot = saved.getValue();
        assertThat(newSnapshot.version()).isEqualTo(1);
        assertThat(originalSnapshot.submittedDocument().blocks()).extracting(DocumentBlock::id)
                .contains(first.id()).doesNotContain(second.id());
        assertThat(originalSnapshot.receiptHash()).isEqualTo(firstReceipt.receiptHash());
        assertThat(newSnapshot.submittedDocument().blocks()).extracting(DocumentBlock::id)
                .contains(second.id()).doesNotContain(first.id());
        assertThat(newSnapshot.sectionAuthors()).extracting(GroupDocument.SectionAuthor::accountId)
                .containsExactly(leader.accountId());
        assertThat(secondReceipt.receiptHash()).isNotEqualTo(firstReceipt.receiptHash());
        verify(evaluation).onGroupSubmitted(document.id(), secondReceipt.receiptHash(), assignmentId, classId, group.memberIds());
    }

    @Test
    void unchangedSinceManualSubmitOnlyClosesAndPreservesSnapshot() {
        when(documents.listSections(document.id())).thenReturn(List.of());
        var receipt = service.submit(leader, document.id(), 0L);
        ArgumentCaptor<GroupDocument> saved = ArgumentCaptor.forClass(GroupDocument.class);
        verify(documents).update(saved.capture());
        GroupDocument submitted = saved.getValue();
        when(documents.lockDocument(document.id())).thenReturn(Optional.of(submitted));
        when(documents.lockSections(document.id())).thenReturn(List.of());
        clearInvocations(documents, evaluation, events, audit, sse);

        assertThat(service.autoSubmit(document.id())).isEmpty();

        verify(documents).update(saved.capture());
        assertThat(saved.getValue().status()).isEqualTo(GroupDocument.Status.CLOSED);
        assertThat(saved.getValue().submittedDocument()).isEqualTo(submitted.submittedDocument());
        assertThat(saved.getValue().receiptHash()).isEqualTo(receipt.receiptHash());
        assertThat(saved.getValue().submitMode()).isEqualTo(GroupDocument.SubmitMode.MANUAL);
        verifyNoInteractions(evaluation, events, audit);
        verify(sse).publish(eq(GroupSectionService.channel(document.id())),
                eq(java.util.Map.of("type", "document.closed", "documentId", document.id().toString())));
    }

    @Test
    void lostCasWhileClosingUnchangedSubmissionHasNoDeliverySideEffects() {
        GroupDocument submitted = document.withSubmission(skeleton, List.of(), leader.accountId(),
                GroupDocument.SubmitMode.MANUAL, NOW, false, "existing-receipt", List.of(),
                GroupDocument.Status.IN_PROGRESS);
        when(documents.lockDocument(document.id())).thenReturn(Optional.of(submitted));
        when(documents.lockSections(document.id())).thenReturn(List.of());
        when(documents.update(any())).thenReturn(false);

        assertThat(service.autoSubmit(document.id())).isEmpty();

        verifyNoInteractions(evaluation, events, audit, sse);
    }

    private GroupSection section(String partId, UUID heading, int order, Status status, UUID holder,
                                 List<DocumentBlock> published, UUID author) {
        return new GroupSection(UUID.randomUUID(), document.id(), partId, heading, List.of(), order, "Mục " + order, status,
                holder, holder == null ? null : NOW, null, published, holder == null ? null : published, null, author, 1);
    }

    private static DocumentBlock block(UUID id, BlockType type, String text) {
        return new DocumentBlock(id, BlockOrigin.TEACHER, type, JsonNodeFactory.instance.objectNode().put("text", text));
    }

    /** The repository increments the persisted version after a successful conditional update. */
    private static GroupDocument persistedVersion(GroupDocument d, long version) {
        return new GroupDocument(d.id(), d.groupId(), d.assignmentId(), d.classId(), d.status(), d.sharedBlocks(),
                d.activityMonitoring(), d.autoSubmitReason(), d.submittedDocument(), d.sectionAuthors(), d.submittedBy(),
                d.submitMode(), d.submittedAt(), d.late(), d.receiptHash(), d.warnings(), d.lastChangedAt(), version,
                d.createdAt());
    }
}
