package edu.aiplatform.groupdocs.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
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
import edu.aiplatform.assignments.contract.IDocx;
import edu.aiplatform.assignments.contract.SubmissionMode;
import edu.aiplatform.assignments.contract.SubmissionWindow;
import edu.aiplatform.audit.contract.IAudit;
import edu.aiplatform.groupdocs.model.GroupDocument;
import edu.aiplatform.groupdocs.model.GroupSection;
import edu.aiplatform.groupdocs.model.GroupSection.Status;
import edu.aiplatform.groupdocs.repository.GroupDocumentRepository;
import edu.aiplatform.groups.model.ClassGroup;
import edu.aiplatform.groups.repository.GroupRepository;
import edu.aiplatform.groups.service.GroupAccess;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.AuthorizationDecision;
import edu.aiplatform.identity.contract.IAuthorization;
import edu.aiplatform.identity.contract.Role;
import edu.aiplatform.shared.error.ApiException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Người giữ mục nhập DOCX vào mục đang soạn (BR-U12-34, như trình soạn bài cá nhân). */
class GroupDocImportServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-08T03:00:00Z");
    private final UUID classId = UUID.randomUUID();
    private final UUID assignmentId = UUID.randomUUID();
    private final Actor leader = new Actor(UUID.randomUUID(), Role.STUDENT);
    private final Actor member = new Actor(UUID.randomUUID(), Role.STUDENT);
    private final Actor teacher = new Actor(UUID.randomUUID(), Role.TEACHER);
    private final GroupDocumentRepository documents = mock(GroupDocumentRepository.class);
    private final GroupRepository groups = mock(GroupRepository.class);
    private final IAuthorization authorization = mock(IAuthorization.class);
    private final IClassAccess classes = mock(IClassAccess.class);
    private final IAssignmentQuery assignments = mock(IAssignmentQuery.class);
    private final IDocx docx = mock(IDocx.class);
    private final IAudit audit = mock(IAudit.class);
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final GroupDocAccess access = new GroupDocAccess(documents, groups,
            new GroupAccess(authorization, classes, audit, clock), assignments, clock);
    private final GroupDocImportService imports = new GroupDocImportService(documents, access, assignments, docx);
    private final ClassGroup group = new ClassGroup(UUID.randomUUID(), classId, "Nhóm 1", leader.accountId(), null, NOW,
            NOW, List.of(leader.accountId(), member.accountId()));
    private final GroupDocument document = GroupDocument.create(UUID.randomUUID(), group.id(), assignmentId, classId,
            new DocumentModel(List.of()), false, NOW);
    private final GroupSection design = new GroupSection(UUID.randomUUID(), document.id(), "p2", null, List.of(), 2,
            "2. Sơ đồ lớp", Status.CLAIMED, member.accountId(), NOW, null, null, null, null, null, 3);

    @BeforeEach
    void setUp() {
        when(authorization.authorize(any(), any(), any())).thenReturn(AuthorizationDecision.allow());
        when(classes.isActiveStudent(any(), eq(classId))).thenReturn(true);
        when(classes.getClassRef(classId)).thenReturn(Optional.of(
                new ClassRef(classId, UUID.randomUUID(), "SE1801", "SWP391", ClassStatus.OPEN, teacher.accountId(), true)));
        when(documents.findById(document.id())).thenReturn(Optional.of(document));
        when(groups.findGroup(group.id())).thenReturn(Optional.of(group));
        when(assignments.submissionWindow(eq(assignmentId), any())).thenReturn(SubmissionWindow.ON_TIME);
        when(assignments.get(assignmentId)).thenReturn(Optional.of(new AssignmentView(assignmentId, AssignmentScope.CLASS,
                classId, UUID.randomUUID(), 1, "Bài nhóm", SubmissionMode.GROUP, GradingMode.GRADED, true,
                AssignmentStatus.OPEN, NOW.minus(Duration.ofDays(10)), NOW.plus(Duration.ofDays(1)), null, null, false,
                null, List.of())));
        when(documents.listSections(document.id())).thenReturn(List.of(design));
    }

    @Test
    void holderImportsDocxIntoTheSectionWithHeadingsTurnedIntoBoldParagraphs() {
        when(docx.importPreview(eq(member), any(), eq(BlockOrigin.STUDENT), eq(true), any())).thenReturn(
                new DocxImportPreview(new DocumentModel(List.of(heading("Tiêu đề trong file"), paragraph("x"))), List.of()));

        var preview = imports.previewSection(member, document.id(), design.id(), new byte[] {1});

        assertThat(preview.document().blocks()).extracting(DocumentBlock::type)
                .containsExactly(BlockType.PARAGRAPH, BlockType.PARAGRAPH);
        assertThat(preview.document().blocks().get(0).content().path("runs").get(0).path("bold").asBoolean()).isTrue();
    }

    @Test
    void onlyTheHolderMayImportIntoTheSection() {
        assertThatThrownBy(() -> imports.previewSection(leader, document.id(), design.id(), new byte[] {1}))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.code()).isEqualTo("SECTION_NOT_HELD"));
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
