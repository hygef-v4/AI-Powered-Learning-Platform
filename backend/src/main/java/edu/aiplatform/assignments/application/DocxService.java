package edu.aiplatform.assignments.application;

import edu.aiplatform.assignments.contract.BlockOrigin;
import edu.aiplatform.assignments.contract.BlockType;
import edu.aiplatform.assignments.contract.DocumentBlock;
import edu.aiplatform.assignments.contract.DocumentModel;
import edu.aiplatform.assignments.contract.DocumentPart;
import edu.aiplatform.assignments.contract.DocxImportPreview;
import edu.aiplatform.assignments.contract.IDiagramLinker;
import edu.aiplatform.assignments.contract.IDiagramResolver;
import edu.aiplatform.assignments.contract.IDocx;
import edu.aiplatform.assignments.domain.Assignment;
import edu.aiplatform.assignments.domain.AssignmentRepository;
import edu.aiplatform.assignments.domain.AssignmentRubric;
import edu.aiplatform.shared.error.ApiException;
import edu.aiplatform.files.contract.IArtifact;
import edu.aiplatform.identity.contract.Actor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Cài {@link IDocx} cho U08, U12 và xuất, nhập DOCX của đề (F7; BR-U07-60…62). */
@Service
public class DocxService implements IDocx {

    private final DocxExporter exporter;
    private final DocxImporter importer;
    private final AssignmentSupport support;
    private final AssignmentAccess access;
    private final RubricService rubrics;
    private final DocumentModelService documents;
    private final AssignmentRepository assignments;
    private final String publicUrl;

    public DocxService(IArtifact artifacts, AssignmentSupport support, AssignmentAccess access, RubricService rubrics,
                       DocumentModelService documents,
                       AssignmentRepository assignments,
                       @Value("${platform.assignments.public-url:http://localhost:8088}") String publicUrl) {
        this.exporter = new DocxExporter(artifacts);
        this.importer = new DocxImporter(artifacts);
        this.support = support;
        this.access = access;
        this.rubrics = rubrics;
        this.documents = documents;
        this.assignments = assignments;
        this.publicUrl = publicUrl.endsWith("/") ? publicUrl.substring(0, publicUrl.length() - 1) : publicUrl;
    }

    @Override
    public byte[] export(DocumentModel document, String title, IDiagramLinker diagramLinker) {
        return exporter.export(title, null, document, diagramLinker, List.of());
    }

    @Override
    public DocxImportPreview importPreview(Actor actor, byte[] docx, BlockOrigin origin, boolean diagramEnabled,
                                           IDiagramResolver diagramResolver) {
        DocxImportPreview preview = importer.preview(actor, docx, origin, diagramEnabled, diagramResolver);
        return new DocxImportPreview(documents.sanitize(preview.document()), preview.warnings());
    }

    /** F7 xuất: người đọc được bài; tùy chọn kèm rubric; không lưu file. */
    @Transactional(readOnly = true)
    public Export exportAssignment(Actor actor, UUID id, boolean includeRubric) {
        Assignment assignment = support.readable(actor, id, "ASSIGNMENT_DOCX_EXPORTED");
        List<DocxExporter.RubricSection> sections = new ArrayList<>();
        if (includeRubric) {
            Map<String, AssignmentRubric> byPart = rubrics.all(assignment.id()).stream()
                    .collect(Collectors.toMap(AssignmentRubric::partId, r -> r, (a, b) -> a));
            for (DocumentPart part : assignment.parts()) {
                AssignmentRubric rubric = byPart.get(part.partId());
                if (rubric != null && !rubric.criteria().isEmpty()) {
                    sections.add(new DocxExporter.RubricSection(part.title(), rubric.criteria(), rubric.totalPoints()));
                }
            }
        }
        byte[] bytes = exporter.export(assignment.title(), assignment.instructions(), assignment.skeleton(),
                linker(assignment.id()), sections);
        return new Export(fileName(assignment.title()), bytes);
    }

    /** F7 nhập: bài nháp, đọc trong bộ nhớ; sơ đồ theo mã chỉ lấy khi người nhập đọc được bài nguồn. */
    @Transactional(readOnly = true)
    public AssignmentViews.ImportPreview importAssignment(Actor actor, UUID id, byte[] docx) {
        Assignment assignment = support.manageable(actor, id, "ASSIGNMENT_DOCX_IMPORTED");
        access.requireOwnerWritable(assignment);
        if (!assignment.editable()) {
            throw ApiException.conflict("ASSIGNMENT_NOT_EDITABLE",
                    "Bài đã phát hành, không nhập DOCX được");
        }
        DocxImportPreview preview = importPreview(actor, docx, BlockOrigin.TEACHER, assignment.diagramEnabled(),
                ref -> resolve(actor, ref));
        List<DocumentBlock> blocks = preview.document().blocks();
        int headings = (int) blocks.stream().filter(b -> b.type() == BlockType.HEADING).count();
        int images = (int) blocks.stream().filter(b -> b.type() == BlockType.IMAGE).count();
        int diagrams = (int) blocks.stream().filter(b -> b.type() == BlockType.DIAGRAM).count();
        return new AssignmentViews.ImportPreview(preview.document(), preview.warnings(), headings, images, diagrams);
    }

    /** Mã {@code assignmentId/blockId}: chỉ khi người nhập đọc được bài nguồn (BR-U07-61). */
    Optional<String> resolve(Actor actor, String ref) {
        String[] parts = ref.split("/");
        if (parts.length != 2) {
            return Optional.empty();
        }
        try {
            UUID assignmentId = UUID.fromString(parts[0]);
            UUID blockId = UUID.fromString(parts[1]);
            return assignments.find(assignmentId).filter(a -> access.canRead(actor, a))
                    .flatMap(a -> a.skeleton().blocks().stream()
                            .filter(b -> blockId.equals(b.id()) && b.type() == BlockType.DIAGRAM).findFirst())
                    .map(b -> b.content().path("xml").asText(null));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    IDiagramLinker linker(UUID assignmentId) {
        return new IDiagramLinker() {
            @Override
            public String linkFor(UUID blockId) {
                return assignmentId + "/" + blockId;
            }

            @Override
            public String viewUrlFor(UUID blockId) {
                return publicUrl + "/assignments/" + assignmentId + "/diagrams/" + blockId;
            }
        };
    }

    static String fileName(String title) {
        String base = title == null ? "bai-tap" : title.replaceAll("[\\\\/:*?\"<>|\\r\\n]+", " ").strip();
        if (base.isEmpty()) {
            base = "bai-tap";
        }
        return (base.length() > 120 ? base.substring(0, 120) : base) + ".docx";
    }

    public record Export(String fileName, byte[] bytes) {
    }
}
