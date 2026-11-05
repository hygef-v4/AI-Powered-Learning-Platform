package edu.aiplatform.groupdocs.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import edu.aiplatform.assignments.contract.DocxImportPreview;
import edu.aiplatform.assignments.contract.GradingMode;
import edu.aiplatform.assignments.contract.IAssignmentQuery;
import edu.aiplatform.assignments.contract.IDocumentModel;
import edu.aiplatform.assignments.contract.IDocx;
import edu.aiplatform.assignments.contract.SubmissionMode;
import edu.aiplatform.assignments.contract.SubmissionWindow;
import edu.aiplatform.assignments.contract.ValidationResult;
import edu.aiplatform.audit.contract.IAudit;
import edu.aiplatform.files.contract.IArtifact;
import edu.aiplatform.groupdocs.contract.ISseHub;
import edu.aiplatform.groupdocs.model.GroupDocument;
import edu.aiplatform.groupdocs.model.GroupSection;
import edu.aiplatform.groupdocs.model.GroupSection.Status;
import edu.aiplatform.groupdocs.model.PeerReview;
import edu.aiplatform.groupdocs.model.SectionRevision;
import edu.aiplatform.groupdocs.repository.GroupDocumentRepository;
import edu.aiplatform.groupdocs.repository.GroupPeerReviewRepository;
import edu.aiplatform.groupdocs.service.GroupDocImportService.ApplyItem;
import edu.aiplatform.groupdocs.service.GroupPeerReviewService.Given;
import edu.aiplatform.groups.model.ClassGroup;
import edu.aiplatform.groups.repository.GroupRepository;
import edu.aiplatform.groups.service.GroupAccess;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.AuthorizationDecision;
import edu.aiplatform.identity.contract.IAuthorization;
import edu.aiplatform.identity.contract.Role;
import edu.aiplatform.shared.error.ApiException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Nhập file (BR-U12-46) và đánh giá chéo (BR-U12-47) của tài liệu nhóm. */
class GroupDocCollaborationServicesTest {

    private static final Instant NOW = Instant.parse("2026-11-02T03:00:00Z");
    private final UUID classId = UUID.randomUUID();
    private final UUID assignmentId = UUID.randomUUID();
    private final Actor leader = new Actor(UUID.randomUUID(), Role.STUDENT);
    private final Actor member = new Actor(UUID.randomUUID(), Role.STUDENT);
    private final Actor third = new Actor(UUID.randomUUID(), Role.STUDENT);
    private final Actor teacher = new Actor(UUID.randomUUID(), Role.TEACHER);
    private final GroupDocumentRepository documents = mock(GroupDocumentRepository.class);
    private final GroupPeerReviewRepository reviews = mock(GroupPeerReviewRepository.class);
    private final GroupRepository groups = mock(GroupRepository.class);
    private final IAuthorization authorization = mock(IAuthorization.class);
    private final IClassAccess classes = mock(IClassAccess.class);
    private final IAssignmentQuery assignments = mock(IAssignmentQuery.class);
    private final IDocumentModel documentModel = mock(IDocumentModel.class);
    private final IDocx docx = mock(IDocx.class);
    private final IArtifact artifacts = mock(IArtifact.class);
    private final ISseHub sse = mock(ISseHub.class);
    private final IAudit audit = mock(IAudit.class);
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final GroupDocAccess access = new GroupDocAccess(documents, groups,
            new GroupAccess(authorization, classes, audit, clock), assignments, clock);
    private final GroupDocImportService imports = new GroupDocImportService(documents, access, assignments, docx,
            documentModel, artifacts, sse, audit, clock);
    private final GroupPeerReviewService peer = new GroupPeerReviewService(reviews, access, assignments, audit, clock);
    private final ClassGroup group = new ClassGroup(UUID.randomUUID(), classId, "Nhóm 1", leader.accountId(), null, NOW,
            NOW, List.of(leader.accountId(), member.accountId(), third.accountId()));
    private final GroupDocument document = GroupDocument.create(UUID.randomUUID(), group.id(), assignmentId, classId,
            new DocumentModel(List.of()), false, NOW);
    private final GroupSection analysis = section("1. Phân tích yêu cầu", Status.OPEN, null, 0);
    private final GroupSection design = section("Thiết kế › 2.1 Sơ đồ lớp", Status.CLAIMED, member.accountId(), 3);

    @BeforeEach
    void setUp() {
        when(authorization.authorize(any(), any(), any())).thenReturn(AuthorizationDecision.allow());
        when(classes.isActiveStudent(any(), eq(classId))).thenReturn(true);
        when(classes.getClassRef(classId)).thenReturn(Optional.of(
                new ClassRef(classId, UUID.randomUUID(), "SE1801", "SWP391", ClassStatus.OPEN, teacher.accountId(), true)));
        when(documents.findById(document.id())).thenReturn(Optional.of(document));
        when(groups.findGroup(group.id())).thenReturn(Optional.of(group));
        when(assignments.submissionWindow(eq(assignmentId), any())).thenReturn(SubmissionWindow.ON_TIME);
        when(assignments.get(assignmentId)).thenReturn(Optional.of(assignment(NOW.plus(Duration.ofDays(1)))));
        when(documents.listSections(document.id())).thenReturn(List.of(analysis, design));
        when(documents.updateSection(any())).thenReturn(true);
        when(documentModel.validateForSave(any(), any())).thenReturn(new ValidationResult(List.of(), List.of()));
        when(documentModel.sanitize(any())).thenAnswer(call -> call.getArgument(0));
    }

    // ---- nhập file ---------------------------------------------------------------------------------------------

    @Test
    void headingsMatchingSectionTitlesSplitTheContentAndOthersStayInside() {
        Map<UUID, List<DocumentBlock>> content = new LinkedHashMap<>();
        List<String> unmatched = new ArrayList<>();

        GroupDocImportService.splitByHeadings(List.of(paragraph("Lời mở đầu"), heading("PHÂN TÍCH YÊU CẦU"),
                paragraph("Yêu cầu 1"), heading("Ghi chú"), paragraph("Ghi chú 1"), heading("2.1. Sơ đồ lớp"),
                paragraph("Lớp A")), List.of(analysis, design), content, unmatched);

        assertThat(content.get(analysis.id())).hasSize(3);
        assertThat(content.get(analysis.id()).get(1).type()).isEqualTo(BlockType.PARAGRAPH);
        assertThat(content.get(design.id())).hasSize(1);
        assertThat(unmatched).containsExactly("Nội dung trước tiêu đề mục đầu tiên");
        assertThat(GroupDocImportService.normalize("Phần 2: Thiết kế")).isEqualTo("thiết kế");
        assertThat(GroupDocImportService.normalize("vi dụ mẫu")).isEqualTo("vi dụ mẫu");
    }

    @Test
    void drawioPagesBecomeSinglePageFilesAndUnsafeXmlIsRejected() {
        String file = "<mxfile><diagram name=\"Phân tích yêu cầu\" id=\"a\"><mxGraphModel><root/></mxGraphModel></diagram>"
                + "<diagram name=\"Trang khác\" id=\"b\">abc</diagram></mxfile>";

        var pages = GroupDocImportService.drawioPages(file.getBytes(StandardCharsets.UTF_8));

        assertThat(pages).extracting(GroupDocImportService.DrawioPage::name)
                .containsExactly("Phân tích yêu cầu", "Trang khác");
        assertThat(pages.get(0).xml()).startsWith("<mxfile").contains("id=\"a\"").doesNotContain("id=\"b\"");
        String evil = "<!DOCTYPE x [<!ENTITY e SYSTEM \"file:///etc/passwd\">]><mxfile><diagram name=\"&e;\"/></mxfile>";
        assertThatThrownBy(() -> GroupDocImportService.drawioPages(evil.getBytes(StandardCharsets.UTF_8)))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("DRAWIO_INVALID"));
    }

    @Test
    void leaderPreviewMapsDocxAndDrawioAndMarksHeldSectionsAsSkipped() {
        when(docx.importPreview(eq(leader), any(), eq(BlockOrigin.STUDENT), eq(true), any())).thenReturn(
                new DocxImportPreview(new DocumentModel(List.of(heading("Sơ đồ lớp"), paragraph("Lớp A"))), List.of()));
        String drawio = "<mxfile><diagram name=\"1. Phân tích yêu cầu\">x</diagram><diagram name=\"Lạ\">y</diagram></mxfile>";

        var preview = imports.previewDocument(leader, document.id(), new byte[] {1},
                drawio.getBytes(StandardCharsets.UTF_8));

        assertThat(preview.sections()).hasSize(2);
        assertThat(preview.sections().get(0).blocks()).singleElement()
                .satisfies(b -> assertThat(b.type()).isEqualTo(BlockType.DIAGRAM));
        assertThat(preview.sections().get(1).skipReason()).isEqualTo("Mục đang có người soạn");
        assertThat(preview.unmatched()).containsExactly("Trang Draw.io “Lạ”");
        assertThatThrownBy(() -> imports.previewDocument(member, document.id(), new byte[] {1}, null))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("GROUP_DOC_LEADER_ONLY"));
    }

    @Test
    void applyPublishesFreeSectionsWithImportRevisionAndSkipsHeldOrStaleOnes() {
        when(documents.lockSection(document.id(), analysis.id())).thenReturn(Optional.of(analysis));
        when(documents.lockSection(document.id(), design.id())).thenReturn(Optional.of(design));

        var result = imports.applyDocument(leader, document.id(), List.of(
                new ApplyItem(analysis.id(), 0L, List.of(paragraph("Yêu cầu 1"))),
                new ApplyItem(design.id(), 3L, List.of(paragraph("Lớp A")))));

        assertThat(result.applied()).isEqualTo(1);
        assertThat(result.skipped()).containsExactly("Thiết kế › 2.1 Sơ đồ lớp: đang có người soạn");
        ArgumentCaptor<GroupSection> saved = ArgumentCaptor.forClass(GroupSection.class);
        verify(documents).updateSection(saved.capture());
        assertThat(saved.getValue().status()).isEqualTo(Status.DONE);
        assertThat(saved.getValue().lastAuthorId()).isEqualTo(leader.accountId());
        ArgumentCaptor<SectionRevision> revision = ArgumentCaptor.forClass(SectionRevision.class);
        verify(documents).insertRevision(revision.capture());
        assertThat(revision.getValue().kind()).isEqualTo(SectionRevision.Kind.IMPORT);
        verify(sse).publish(eq("group-doc:" + document.id()), any());

        var stale = imports.applyDocument(leader, document.id(),
                List.of(new ApplyItem(analysis.id(), 7L, List.of(paragraph("x")))));
        assertThat(stale.applied()).isZero();
        assertThat(stale.skipped()).singleElement().asString().endsWith("vừa được thay đổi");
    }

    @Test
    void sectionImportRequiresHoldingTheSectionAndTurnsHeadingsIntoBoldParagraphs() {
        when(documents.assignmentLock(document.id()))
                .thenReturn(Optional.of(new edu.aiplatform.groupdocs.model.AssignmentLock(leader.accountId(), NOW)));
        when(docx.importPreview(eq(member), any(), eq(BlockOrigin.STUDENT), eq(true), any())).thenReturn(
                new DocxImportPreview(new DocumentModel(List.of(heading("Tiêu đề trong file"), paragraph("x"))), List.of()));

        var preview = imports.previewSection(member, document.id(), design.id(), new byte[] {1});

        assertThat(preview.document().blocks()).extracting(DocumentBlock::type)
                .containsExactly(BlockType.PARAGRAPH, BlockType.PARAGRAPH);
        assertThat(preview.document().blocks().get(0).content().path("runs").get(0).path("bold").asBoolean()).isTrue();
        assertThatThrownBy(() -> imports.previewSection(leader, document.id(), design.id(), new byte[] {1}))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("SECTION_NOT_HELD"));
    }

    // ---- đánh giá chéo -----------------------------------------------------------------------------------------

    @Test
    void peerReviewOpensOnlyAfterTheGroupSubmits() {
        assertThat(peer.view(member, document.id()).open()).isFalse();
        assertThatThrownBy(() -> peer.save(member, document.id(), List.of(new Given(leader.accountId(), 4, null))))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("PEER_REVIEW_CLOSED"));
        verify(reviews, never()).upsert(any());
    }

    @Test
    void memberReviewsOthersButNotThemselvesAndScoresStayInRange() {
        submitted();

        assertThatThrownBy(() -> peer.save(member, document.id(), List.of(new Given(member.accountId(), 4, null))))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("PEER_REVIEW_INVALID"));
        assertThatThrownBy(() -> peer.save(member, document.id(), List.of(new Given(leader.accountId(), 6, null))))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("PEER_REVIEW_INVALID"));
        assertThatThrownBy(() -> peer.save(member, document.id(), List.of(new Given(UUID.randomUUID(), 3, null))))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("PEER_REVIEW_INVALID"));

        peer.save(member, document.id(), List.of(new Given(leader.accountId(), 5, "  Làm phần khó  "),
                new Given(third.accountId(), 2, " ")));

        ArgumentCaptor<PeerReview> saved = ArgumentCaptor.forClass(PeerReview.class);
        verify(reviews, org.mockito.Mockito.times(2)).upsert(saved.capture());
        assertThat(saved.getAllValues()).extracting(PeerReview::comment).containsExactly("Làm phần khó", null);
        assertThat(saved.getAllValues()).allSatisfy(r -> assertThat(r.reviewerId()).isEqualTo(member.accountId()));
    }

    @Test
    void teacherSeesAveragesOnTenPointScaleMembersOnlySeeTheirOwn() {
        submitted();
        when(reviews.listByDocument(document.id())).thenReturn(List.of(
                review(member, leader, 5), review(third, leader, 4), review(leader, member, 3)));

        var summary = peer.view(teacher, document.id());
        var mine = peer.view(member, document.id());

        assertThat(summary.reviewers()).isEqualTo(3);
        assertThat(summary.summary()).filteredOn(s -> s.accountId().equals(leader.accountId())).singleElement()
                .satisfies(s -> {
                    assertThat(s.average()).isEqualTo(4.5);
                    assertThat(s.outOfTen()).isEqualTo(9.0);
                    assertThat(s.count()).isEqualTo(2);
                });
        assertThat(summary.summary()).filteredOn(s -> s.accountId().equals(third.accountId())).singleElement()
                .satisfies(s -> assertThat(s.average()).isNull());
        assertThat(mine.summary()).isEmpty();
        assertThat(mine.mine()).singleElement().satisfies(g -> assertThat(g.revieweeId()).isEqualTo(leader.accountId()));
    }

    @Test
    void peerReviewClosesSevenDaysAfterTheFinalDeadline() {
        submitted();
        when(assignments.get(assignmentId)).thenReturn(Optional.of(assignment(NOW.minus(Duration.ofDays(8)))));

        var view = peer.view(member, document.id());

        assertThat(view.open()).isFalse();
        assertThat(view.closedReason()).isEqualTo("Đã hết thời gian đánh giá");
    }

    // ---- dữ liệu ---------------------------------------------------------------------------------------------

    private void submitted() {
        when(documents.findById(document.id())).thenReturn(Optional.of(document.withSubmission(new DocumentModel(List.of()),
                List.of(), leader.accountId(), GroupDocument.SubmitMode.MANUAL, NOW.minusSeconds(60), false, "hash",
                List.of(), GroupDocument.Status.IN_PROGRESS)));
    }

    private PeerReview review(Actor from, Actor to, int score) {
        return new PeerReview(UUID.randomUUID(), document.id(), from.accountId(), to.accountId(), score, null, NOW, NOW);
    }

    private AssignmentView assignment(Instant closesAt) {
        return new AssignmentView(assignmentId, AssignmentScope.CLASS, classId, UUID.randomUUID(), 1, "Bài nhóm",
                SubmissionMode.GROUP, GradingMode.GRADED, true, AssignmentStatus.OPEN, NOW.minus(Duration.ofDays(10)),
                closesAt, null, null, false, null, List.of());
    }

    private GroupSection section(String title, Status status, UUID holder, long version) {
        return new GroupSection(UUID.randomUUID(), document.id(), UUID.randomUUID().toString(), null, List.of(), 1, title,
                status, holder, holder == null ? null : NOW, null, null, null, null, null, version);
    }

    private static DocumentBlock heading(String text) {
        return new DocumentBlock(UUID.randomUUID(), BlockOrigin.STUDENT, BlockType.HEADING,
                JsonNodeFactory.instance.objectNode().put("style", "H1").put("text", text));
    }

    private static DocumentBlock paragraph(String text) {
        var content = JsonNodeFactory.instance.objectNode();
        content.putArray("runs").addObject().put("text", text);
        return new DocumentBlock(UUID.randomUUID(), BlockOrigin.STUDENT, BlockType.PARAGRAPH, content);
    }
}
