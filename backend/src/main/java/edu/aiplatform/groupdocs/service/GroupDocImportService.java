package edu.aiplatform.groupdocs.service;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.aiplatform.assignments.contract.AssignmentView;
import edu.aiplatform.assignments.contract.BlockOrigin;
import edu.aiplatform.assignments.contract.BlockType;
import edu.aiplatform.assignments.contract.DocumentBlock;
import edu.aiplatform.assignments.contract.DocumentModel;
import edu.aiplatform.assignments.contract.DocxImportPreview;
import edu.aiplatform.assignments.contract.IAssignmentQuery;
import edu.aiplatform.assignments.contract.IDocx;
import edu.aiplatform.assignments.contract.ValidationIssue;
import edu.aiplatform.groupdocs.model.GroupSection;
import edu.aiplatform.groupdocs.model.SectionRules;
import edu.aiplatform.groupdocs.repository.GroupDocumentRepository;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.shared.error.ApiException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Người giữ mục nhập DOCX vào mục đang soạn, như trình soạn bài cá nhân (BR-U12-34): chỉ xem trước, trình soạn chèn
 * vào bản nháp; không dùng AI.
 */
@Service
public class GroupDocImportService {

    private final GroupDocumentRepository documents;
    private final GroupDocAccess access;
    private final IAssignmentQuery assignments;
    private final IDocx docx;

    public GroupDocImportService(GroupDocumentRepository documents, GroupDocAccess access, IAssignmentQuery assignments,
                                 IDocx docx) {
        this.documents = documents;
        this.access = access;
        this.assignments = assignments;
        this.docx = docx;
    }

    /** Xem trước DOCX của người giữ mục: heading thành đoạn in đậm vì heading của mục là khung cố định. */
    @Transactional(readOnly = true)
    public SectionImport previewSection(Actor actor, UUID documentId, UUID sectionId, byte[] file) {
        GroupDocContext ctx = access.read(actor, documentId);
        GroupDocAccess.requireMember(ctx);
        access.requireEditable(ctx, false);
        GroupSection section = documents.listSections(documentId).stream().filter(s -> s.id().equals(sectionId))
                .findFirst().orElseThrow(() -> ApiException.notFound("SECTION_NOT_FOUND"));
        if (SectionRules.canEdit(section, actor.accountId()) != null) {
            throw ApiException.conflict("SECTION_NOT_HELD", "Bạn không giữ mục này");
        }
        boolean diagramEnabled = assignments.get(ctx.document().assignmentId())
                .map(AssignmentView::diagramEnabled).orElse(false);
        DocxImportPreview preview = docx.importPreview(actor, file, BlockOrigin.STUDENT, diagramEnabled,
                ref -> Optional.empty());
        List<DocumentBlock> blocks = preview.document().blocks().stream().map(GroupDocImportService::asBody).toList();
        return new SectionImport(new DocumentModel(blocks), preview.warnings(), countBlocks(blocks, BlockType.IMAGE),
                countBlocks(blocks, BlockType.DIAGRAM));
    }

    /** Heading trong nội dung mục thành đoạn in đậm như bài cá nhân; block nào cũng là của nhóm. */
    static DocumentBlock asBody(DocumentBlock block) {
        if (block.type() == BlockType.HEADING) {
            ObjectNode content = JsonNodeFactory.instance.objectNode();
            content.putArray("runs").addObject().put("text", block.content().path("text").asText("")).put("bold", true);
            return new DocumentBlock(block.id(), BlockOrigin.STUDENT, BlockType.PARAGRAPH, content);
        }
        return block.origin() == BlockOrigin.STUDENT ? block
                : new DocumentBlock(block.id(), BlockOrigin.STUDENT, block.type(), block.content());
    }

    private static int countBlocks(List<DocumentBlock> blocks, BlockType type) {
        return (int) blocks.stream().filter(b -> b.type() == type).count();
    }

    public record SectionImport(DocumentModel document, List<ValidationIssue> warnings, int images, int diagrams) {
    }
}
