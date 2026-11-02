package edu.aiplatform.groupdocs.service;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.aiplatform.assignments.contract.AssignmentView;
import edu.aiplatform.assignments.contract.BlockOrigin;
import edu.aiplatform.assignments.contract.BlockType;
import edu.aiplatform.assignments.contract.DocumentBlock;
import edu.aiplatform.assignments.contract.DocumentFiles;
import edu.aiplatform.assignments.contract.DocumentModel;
import edu.aiplatform.assignments.contract.DocxImportPreview;
import edu.aiplatform.assignments.contract.IAssignmentQuery;
import edu.aiplatform.assignments.contract.IDocumentModel;
import edu.aiplatform.assignments.contract.IDocx;
import edu.aiplatform.assignments.contract.ValidationIssue;
import edu.aiplatform.assignments.contract.ValidationResult;
import edu.aiplatform.audit.contract.AuditEvent;
import edu.aiplatform.audit.contract.AuditResult;
import edu.aiplatform.audit.contract.IAudit;
import edu.aiplatform.files.contract.ArtifactPurpose;
import edu.aiplatform.files.contract.FileRef;
import edu.aiplatform.files.contract.IArtifact;
import edu.aiplatform.groupdocs.contract.ISseHub;
import edu.aiplatform.groupdocs.model.GroupSection;
import edu.aiplatform.groupdocs.model.SectionRevision;
import edu.aiplatform.groupdocs.model.SectionRules;
import edu.aiplatform.groupdocs.repository.GroupDocumentRepository;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.shared.error.ApiException;
import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Nhập file vào tài liệu nhóm (FB-U12-03), không dùng AI nên không tốn token:
 * <ul>
 * <li>Người giữ nhập DOCX vào mục đang sửa: xem trước rồi trình soạn chèn vào bản nháp.</li>
 * <li>Trưởng nhóm nhập DOCX và/hoặc tệp Draw.io cho cả tài liệu: heading trùng tên mục thì nội dung dưới heading vào
 * mục đó, trang Draw.io trùng tên mục thành sơ đồ của mục. Xem trước rồi áp dụng; mục đang có người giữ hoặc đã chốt
 * thì bỏ qua. Áp dụng ghi nội dung đã đăng và một bản lịch sử {@code IMPORT} cho từng mục.</li>
 * </ul>
 */
@Service
public class GroupDocImportService {

    static final int MAX_DRAWIO_BYTES = 10 * 1024 * 1024;

    private final GroupDocumentRepository documents;
    private final GroupDocAccess access;
    private final IAssignmentQuery assignments;
    private final IDocx docx;
    private final IDocumentModel documentModel;
    private final IArtifact artifacts;
    private final ISseHub sse;
    private final IAudit audit;
    private final Clock clock;

    public GroupDocImportService(GroupDocumentRepository documents, GroupDocAccess access, IAssignmentQuery assignments,
                                 IDocx docx, IDocumentModel documentModel, IArtifact artifacts, ISseHub sse, IAudit audit,
                                 Clock clock) {
        this.documents = documents;
        this.access = access;
        this.assignments = assignments;
        this.docx = docx;
        this.documentModel = documentModel;
        this.artifacts = artifacts;
        this.sse = sse;
        this.audit = audit;
        this.clock = clock;
    }

    /** Người giữ nhập DOCX vào mục: heading thành đoạn in đậm vì heading của mục là khung cố định. */
    @Transactional(readOnly = true)
    public SectionImport previewSection(Actor actor, UUID documentId, UUID sectionId, byte[] file) {
        GroupDocContext ctx = access.read(actor, documentId);
        GroupDocAccess.requireMember(ctx);
        access.requireEditable(ctx, false);
        GroupSection section = section(documentId, sectionId);
        if (SectionRules.canEdit(section, actor.accountId()) != null) {
            throw ApiException.conflict("SECTION_NOT_HELD", "Bạn không giữ mục này");
        }
        DocxImportPreview preview = docx.importPreview(actor, file, BlockOrigin.STUDENT, diagramEnabled(ctx),
                ref -> Optional.empty());
        List<DocumentBlock> blocks = preview.document().blocks().stream().map(GroupDocImportService::asBody).toList();
        return new SectionImport(new DocumentModel(blocks), preview.warnings(), count(blocks, BlockType.IMAGE),
                count(blocks, BlockType.DIAGRAM));
    }

    /** Trưởng nhóm xem trước cách chia file vào các mục; chưa ghi gì. */
    @Transactional(readOnly = true)
    public DocumentImport previewDocument(Actor actor, UUID documentId, byte[] docxFile, byte[] drawioFile) {
        GroupDocContext ctx = access.read(actor, documentId);
        requireLeader(ctx);
        access.requireEditable(ctx, false);
        if (docxFile == null && drawioFile == null) {
            throw ApiException.badRequest("IMPORT_EMPTY", "Chọn tệp DOCX hoặc tệp Draw.io");
        }
        List<GroupSection> sections = documents.listSections(documentId);
        Map<UUID, List<DocumentBlock>> content = new LinkedHashMap<>();
        List<String> unmatched = new ArrayList<>();
        List<ValidationIssue> warnings = new ArrayList<>();
        boolean diagrams = diagramEnabled(ctx);
        if (docxFile != null) {
            DocxImportPreview preview = docx.importPreview(actor, docxFile, BlockOrigin.STUDENT, diagrams,
                    ref -> Optional.empty());
            warnings.addAll(preview.warnings());
            splitByHeadings(preview.document().blocks(), sections, content, unmatched);
        }
        if (drawioFile != null) {
            if (!diagrams) {
                warnings.add(new ValidationIssue("DIAGRAM_DISABLED", null, "Bài chưa bật sơ đồ, bỏ qua tệp Draw.io"));
            } else {
                for (DrawioPage page : drawioPages(drawioFile)) {
                    Optional<GroupSection> target = match(page.name(), sections);
                    if (target.isEmpty()) {
                        unmatched.add("Trang Draw.io “" + page.name() + "”");
                        continue;
                    }
                    content.computeIfAbsent(target.get().id(), k -> new ArrayList<>()).add(diagram(page));
                }
            }
        }
        List<SectionPlan> plans = new ArrayList<>();
        for (GroupSection section : sections) {
            List<DocumentBlock> blocks = content.get(section.id());
            if (blocks == null) {
                continue;
            }
            String skip = section.locked() ? "Mục đã chốt" : section.status() == GroupSection.Status.CLAIMED
                    ? "Mục đang có người giữ" : null;
            plans.add(new SectionPlan(section.id(), section.title(), section.version(), blocks, skip,
                    section.hasPublished()));
        }
        return new DocumentImport(plans, unmatched, warnings);
    }

    /**
     * Áp dụng bản xem trước đã duyệt: từng mục khóa dòng, kiểm {@code version} và trạng thái lại, làm sạch, kiểm khung,
     * gắn ảnh rồi ghi nội dung đã đăng. Mục lỗi bỏ qua và báo lại, mục khác vẫn ghi.
     */
    @Transactional
    public ApplyResult applyDocument(Actor actor, UUID documentId, List<ApplyItem> items) {
        GroupDocContext ctx = access.read(actor, documentId);
        requireLeader(ctx);
        access.requireEditable(ctx, false);
        List<UUID> applied = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        Instant now = clock.instant();
        DocumentModel shared = ctx.document().sharedBlocks();
        for (ApplyItem item : items == null ? List.<ApplyItem>of() : items) {
            Optional<GroupSection> locked = item.sectionId() == null ? Optional.empty()
                    : documents.lockSection(documentId, item.sectionId());
            if (locked.isEmpty()) {
                skipped.add("Không tìm thấy mục");
                continue;
            }
            GroupSection section = locked.get();
            String reason = section.locked() ? "đã chốt" : section.status() == GroupSection.Status.CLAIMED
                    ? "đang có người giữ" : item.version() == null || item.version() != section.version()
                    ? "vừa được thay đổi" : null;
            if (reason != null) {
                skipped.add(section.title() + ": " + reason);
                continue;
            }
            List<DocumentBlock> previous = section.publishedBlocks() == null ? List.of() : section.publishedBlocks();
            DocumentModel incoming = documentModel.sanitize(new DocumentModel(item.blocks() == null ? List.of()
                    : item.blocks().stream().map(GroupDocImportService::asBody).toList()));
            DocumentModel pending = DocumentFiles.resolveFiles(new DocumentModel(previous), incoming, null);
            ValidationResult result = documentModel.validateForSave(shared,
                    GroupSnapshot.withSection(shared, section.headingBlockId(), pending.blocks()));
            if (result != null && !result.ok()) {
                skipped.add(section.title() + ": " + result.errors().get(0).message());
                continue;
            }
            List<DocumentBlock> stored = DocumentFiles.resolveFiles(new DocumentModel(previous), pending,
                    ref -> artifacts.attach(new FileRef(ref), actor, ArtifactPurpose.DOCUMENT_IMAGE).fileId()).blocks();
            GroupSection published = section.published(stored, actor.accountId(), now);
            if (!documents.updateSection(published)) {
                skipped.add(section.title() + ": vừa được thay đổi");
                continue;
            }
            documents.insertRevision(new SectionRevision(UUID.randomUUID(), section.id(), actor.accountId(), stored, now,
                    SectionRevision.Kind.IMPORT));
            applied.add(section.id());
        }
        if (!applied.isEmpty()) {
            documents.touch(documentId, now);
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("sections", applied.size());
            details.put("skipped", skipped.size());
            audit.record(new AuditEvent(UUID.randomUUID(), actor.accountId(), "GROUP_DOC_IMPORTED", "GROUP_DOCUMENT",
                    documentId.toString(), AuditResult.SUCCESS, null, details, now));
            Map<String, Object> event = new LinkedHashMap<>();
            event.put("type", "document.imported");
            event.put("documentId", documentId.toString());
            sse.publish(GroupSectionService.channel(documentId), event);
        }
        return new ApplyResult(applied.size(), skipped);
    }

    // ---- chia nội dung ----------------------------------------------------------------------------------------

    /**
     * Heading trùng tên một mục mở vùng của mục đó; heading khác nằm trong vùng hiện tại thành đoạn in đậm. Nội dung
     * trước heading trùng đầu tiên không thuộc mục nào và được báo lại.
     */
    static void splitByHeadings(List<DocumentBlock> blocks, List<GroupSection> sections,
                                Map<UUID, List<DocumentBlock>> content, List<String> unmatched) {
        List<DocumentBlock> current = null;
        boolean leading = false;
        for (DocumentBlock block : blocks) {
            if (block.type() == BlockType.HEADING) {
                Optional<GroupSection> target = match(block.content().path("text").asText(""), sections);
                if (target.isPresent()) {
                    current = content.computeIfAbsent(target.get().id(), k -> new ArrayList<>());
                    continue;
                }
                if (current == null) {
                    unmatched.add("Tiêu đề “" + block.content().path("text").asText("") + "”");
                    continue;
                }
            }
            if (current == null) {
                leading = true;
            } else {
                current.add(asBody(block));
            }
        }
        if (leading) {
            unmatched.add("Nội dung trước tiêu đề mục đầu tiên");
        }
    }

    /** So tên đã chuẩn hóa: bỏ dấu cách thừa, số thứ tự đầu dòng, chữ hoa; so cả tên đầy đủ và tên phần cuối. */
    static Optional<GroupSection> match(String text, List<GroupSection> sections) {
        String key = normalize(text);
        if (key.isEmpty()) {
            return Optional.empty();
        }
        for (GroupSection section : sections) {
            String title = section.title() == null ? "" : section.title();
            String last = title.contains("›") ? title.substring(title.lastIndexOf('›') + 1) : title;
            if (key.equals(normalize(title)) || key.equals(normalize(last))) {
                return Optional.of(section);
            }
        }
        return Optional.empty();
    }

    static String normalize(String value) {
        String s = Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFC).toLowerCase(Locale.ROOT).trim();
        s = s.replaceAll("^(phần|chương|mục|part|section)\\s+([0-9]+|[ivxlc]+)[.):\\-]?\\s*", "");
        s = s.replaceAll("^[0-9]+([.][0-9]+)*([.):]\\s*|\\s+)", "");
        s = s.replaceAll("^[ivxlc]+[.)]\\s+", "");
        return s.replaceAll("[\\s\\u00a0]+", " ").replaceAll("[.:]+$", "").trim();
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

    // ---- Draw.io ---------------------------------------------------------------------------------------------

    record DrawioPage(String name, String xml) {
    }

    /** Mỗi trang ({@code <diagram>}) của tệp {@code .drawio} thành một tệp một trang để mở lại trong Draw.io. */
    static List<DrawioPage> drawioPages(byte[] file) {
        if (file.length > MAX_DRAWIO_BYTES) {
            throw ApiException.payloadTooLarge("DRAWIO_TOO_LARGE", "Tệp Draw.io tối đa 10 MB");
        }
        try {
            Document doc = factory().newDocumentBuilder().parse(new ByteArrayInputStream(file));
            Element root = doc.getDocumentElement();
            List<DrawioPage> pages = new ArrayList<>();
            if ("mxGraphModel".equals(root.getLocalName() == null ? root.getNodeName() : root.getLocalName())) {
                pages.add(new DrawioPage("Trang 1", wrap(doc, root, "Trang 1")));
                return pages;
            }
            if (!"mxfile".equals(root.getNodeName())) {
                throw invalidDrawio();
            }
            NodeList diagrams = root.getElementsByTagName("diagram");
            for (int i = 0; i < diagrams.getLength(); i++) {
                Element diagram = (Element) diagrams.item(i);
                String name = diagram.getAttribute("name").isBlank() ? "Trang " + (i + 1) : diagram.getAttribute("name");
                Element single = doc.createElement("mxfile");
                single.setAttribute("host", "app.diagrams.net");
                single.appendChild(diagram.cloneNode(true));
                pages.add(new DrawioPage(name.trim(), write(single)));
            }
            if (pages.isEmpty()) {
                throw invalidDrawio();
            }
            return pages;
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw invalidDrawio();
        }
    }

    private static String wrap(Document doc, Element model, String name) throws Exception {
        Element file = doc.createElement("mxfile");
        file.setAttribute("host", "app.diagrams.net");
        Element diagram = doc.createElement("diagram");
        diagram.setAttribute("name", name);
        diagram.appendChild(model.cloneNode(true));
        file.appendChild(diagram);
        return write(file);
    }

    private static DocumentBlock diagram(DrawioPage page) {
        ObjectNode content = JsonNodeFactory.instance.objectNode();
        content.put("diagramType", "OTHER");
        content.put("xml", page.xml());
        content.put("alt", page.name());
        return new DocumentBlock(UUID.randomUUID(), BlockOrigin.STUDENT, BlockType.DIAGRAM, content);
    }

    private static ApiException invalidDrawio() {
        return ApiException.badRequest("DRAWIO_INVALID", "Không đọc được tệp Draw.io (.drawio, .xml)");
    }

    private static DocumentBuilderFactory factory() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        return factory;
    }

    private static String write(Element element) throws Exception {
        TransformerFactory factory = TransformerFactory.newInstance();
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
        Transformer transformer = factory.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, StandardCharsets.UTF_8.name());
        StringWriter out = new StringWriter();
        transformer.transform(new DOMSource(element), new StreamResult(out));
        return out.toString();
    }

    // ---- tiện ích -----------------------------------------------------------------------------------------------

    private GroupSection section(UUID documentId, UUID sectionId) {
        return documents.listSections(documentId).stream().filter(s -> s.id().equals(sectionId)).findFirst()
                .orElseThrow(() -> ApiException.notFound("SECTION_NOT_FOUND"));
    }

    private boolean diagramEnabled(GroupDocContext ctx) {
        return assignments.get(ctx.document().assignmentId()).map(AssignmentView::diagramEnabled).orElse(false);
    }

    private static void requireLeader(GroupDocContext ctx) {
        if (!ctx.leader()) {
            throw ApiException.forbidden("GROUP_DOC_LEADER_ONLY");
        }
    }

    private static int count(List<DocumentBlock> blocks, BlockType type) {
        return (int) blocks.stream().filter(b -> b.type() == type).count();
    }

    public record SectionImport(DocumentModel document, List<ValidationIssue> warnings, int images, int diagrams) {
    }

    /** {@code skipReason}: mục không nhận nội dung nhập; {@code replaces}: mục đã có nội dung, nhập sẽ thay. */
    public record SectionPlan(UUID sectionId, String title, long version, List<DocumentBlock> blocks, String skipReason,
                              boolean replaces) {
    }

    public record DocumentImport(List<SectionPlan> sections, List<String> unmatched, List<ValidationIssue> warnings) {
    }

    public record ApplyItem(UUID sectionId, Long version, List<DocumentBlock> blocks) {
    }

    public record ApplyResult(int applied, List<String> skipped) {
    }
}
