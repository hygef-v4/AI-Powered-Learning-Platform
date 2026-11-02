package edu.aiplatform.assignments.application;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.aiplatform.assignments.contract.BlockOrigin;
import edu.aiplatform.assignments.contract.BlockType;
import edu.aiplatform.assignments.contract.DocumentBlock;
import edu.aiplatform.assignments.contract.DocumentFiles;
import edu.aiplatform.assignments.contract.DocumentModel;
import edu.aiplatform.assignments.contract.DocxImportPreview;
import edu.aiplatform.assignments.contract.IDiagramResolver;
import edu.aiplatform.assignments.contract.ValidationIssue;
import edu.aiplatform.files.contract.ArtifactPurpose;
import edu.aiplatform.files.contract.FileRef;
import edu.aiplatform.files.contract.FileUpload;
import edu.aiplatform.files.contract.IArtifact;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.shared.error.ApiException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.openxml4j.opc.PackageRelationship;
import org.apache.poi.xwpf.usermodel.BodyElementType;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.UnderlinePatterns;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFHeaderFooter;
import org.apache.poi.xwpf.usermodel.XWPFHyperlink;
import org.apache.poi.xwpf.usermodel.XWPFHyperlinkRun;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFPicture;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFStyle;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.openxmlformats.schemas.drawingml.x2006.main.CTNonVisualDrawingProps;
import org.openxmlformats.schemas.drawingml.x2006.wordprocessingDrawing.CTAnchor;
import org.openxmlformats.schemas.drawingml.x2006.wordprocessingDrawing.CTInline;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTDrawing;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Nhập DOCX thành bản xem trước (BR-U07-61, 62; NFR-U07-31): đọc trong bộ nhớ, chặn file nén bất thường; style Word
 * thành heading; đoạn, danh sách, bảng, ảnh thành block. Mỗi ảnh tìm sơ đồ theo mã trong alt text, hyperlink, chunk
 * {@code mxfile} của PNG rồi thuộc tính {@code content} của SVG; không có thì giữ là ảnh (lưu tạm qua U03).
 */
final class DocxImporter {

    static final int MAX_DOCX_BYTES = 20 * 1024 * 1024;
    static final long MAX_UNPACKED_BYTES = 100L * 1024 * 1024;
    static final int MAX_ENTRIES = 2_000;
    private static final Pattern CODE = Pattern.compile("\\[drawio:([^\\]\\s]+)]");
    private static final Pattern LINK = Pattern.compile("/(assignments|attempts)/([0-9a-fA-F-]{36})/diagrams/([0-9a-fA-F-]{36})");
    private static final Set<String> MONO = Set.of("consolas", "courier new", "courier", "monaco", "menlo",
            "source code pro", "roboto mono", "jetbrains mono", "fira code");
    private static final Map<String, String> UNSUPPORTED = Map.of("txbxContent", "hộp văn bản", "oMath", "công thức",
            "oMathPara", "công thức", "smartArt", "SmartArt", "chart", "biểu đồ");

    private final IArtifact artifacts;

    DocxImporter(IArtifact artifacts) {
        this.artifacts = artifacts;
    }

    DocxImportPreview preview(Actor actor, byte[] bytes, BlockOrigin origin, boolean diagramEnabled,
                              IDiagramResolver resolver) {
        if (bytes == null || bytes.length == 0) {
            throw ApiException.badRequest("DOCX_INVALID", "Chưa chọn tệp DOCX");
        }
        if (bytes.length > MAX_DOCX_BYTES) {
            throw ApiException.payloadTooLarge("DOCX_TOO_LARGE", "Tệp DOCX tối đa 20 MB");
        }
        checkZip(bytes);
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(bytes))) {
            Reader reader = new Reader(actor, doc, origin, diagramEnabled, resolver);
            reader.read();
            return new DocxImportPreview(new DocumentModel(List.copyOf(reader.blocks)), reader.warnings());
        } catch (ApiException e) {
            throw e;
        } catch (IOException | RuntimeException e) {
            throw ApiException.badRequest("DOCX_INVALID", "Không đọc được tệp DOCX");
        }
    }

    /** Chặn file nén bất thường: số mục, tổng dung lượng giải nén (đếm byte thật). */
    static void checkZip(byte[] bytes) {
        int entries = 0;
        long total = 0;
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            byte[] buffer = new byte[8192];
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (++entries > MAX_ENTRIES) {
                    throw ApiException.badRequest("DOCX_INVALID", "Tệp DOCX có quá nhiều thành phần");
                }
                if (entry.getName().contains("..")) {
                    throw ApiException.badRequest("DOCX_INVALID", "Tệp DOCX không hợp lệ");
                }
                int n;
                while ((n = zip.read(buffer)) > 0) {
                    total += n;
                    if (total > MAX_UNPACKED_BYTES) {
                        throw ApiException.badRequest("DOCX_INVALID", "Tệp DOCX giải nén vượt 100 MB");
                    }
                }
            }
        } catch (IOException e) {
            throw ApiException.badRequest("DOCX_INVALID", "Không đọc được tệp DOCX");
        }
        if (entries == 0) {
            throw ApiException.badRequest("DOCX_INVALID", "Không đọc được tệp DOCX");
        }
    }

    private final class Reader {
        private final Actor actor;
        private final XWPFDocument doc;
        private final BlockOrigin origin;
        private final boolean diagramEnabled;
        private final IDiagramResolver resolver;
        private final List<DocumentBlock> blocks = new ArrayList<>();
        private final List<ValidationIssue> issues = new ArrayList<>();
        private final Map<String, Integer> unsupported = new LinkedHashMap<>();
        private ObjectNode list;
        private BigInteger listId;
        private int skippedImages;
        private boolean truncated;

        Reader(Actor actor, XWPFDocument doc, BlockOrigin origin, boolean diagramEnabled, IDiagramResolver resolver) {
            this.actor = actor;
            this.doc = doc;
            this.origin = origin;
            this.diagramEnabled = diagramEnabled;
            this.resolver = resolver;
        }

        void read() {
            for (IBodyElement element : doc.getBodyElements()) {
                if (element.getElementType() == BodyElementType.PARAGRAPH) {
                    paragraph((XWPFParagraph) element);
                } else if (element.getElementType() == BodyElementType.TABLE) {
                    endList();
                    table((XWPFTable) element);
                } else {
                    endList();
                    unsupported.merge("khối điều khiển nội dung (ví dụ mục lục)", 1, Integer::sum);
                }
            }
            endList();
            boolean headerText = false;
            for (XWPFHeaderFooter part : doc.getHeaderList()) {
                headerText |= part.getText() != null && !part.getText().isBlank();
            }
            for (XWPFHeaderFooter part : doc.getFooterList()) {
                headerText |= part.getText() != null && !part.getText().isBlank();
            }
            if (headerText) {
                unsupported.merge("đầu trang, chân trang", 1, Integer::sum);
            }
        }

        List<ValidationIssue> warnings() {
            List<ValidationIssue> all = new ArrayList<>(issues);
            if (!unsupported.isEmpty()) {
                List<String> parts = new ArrayList<>();
                unsupported.forEach((name, count) -> parts.add(count + " " + name));
                all.add(new ValidationIssue("UNSUPPORTED_CONTENT", null, "Đã bỏ: " + String.join(", ", parts)));
            }
            if (skippedImages > 0) {
                all.add(new ValidationIssue("IMAGE_SKIPPED", null,
                        "Bỏ " + skippedImages + " ảnh không đúng loại hoặc quá lớn"));
            }
            if (truncated) {
                all.add(new ValidationIssue("TOO_MANY_BLOCKS", null, "Tài liệu dài quá 2.000 block, phần sau bị bỏ"));
            }
            return List.copyOf(all);
        }

        /** Căn lề của đoạn đang đọc. */
        private String align;

        private void paragraph(XWPFParagraph p) {
            countUnsupported(p.getCTP().getDomNode());
            String heading = headingStyle(p);
            if (heading != null) {
                endList();
                String text = p.getText() == null ? "" : p.getText().replaceAll("\\s+", " ").strip();
                if (!text.isEmpty()) {
                    ObjectNode content = JsonNodeFactory.instance.objectNode();
                    content.put("style", heading);
                    content.put("text", text.length() > DocumentModelService.MAX_HEADING
                            ? text.substring(0, DocumentModelService.MAX_HEADING) : text);
                    add(BlockType.HEADING, content);
                }
                pictures(p);
                return;
            }
            if (p.getNumID() != null) {
                listItem(p);
                pictures(p);
                return;
            }
            endList();
            align = switch (p.getAlignment()) {
                case CENTER -> "CENTER";
                case RIGHT -> "RIGHT";
                case BOTH -> "JUSTIFY";
                default -> null;
            };
            ArrayNode runs = JsonNodeFactory.instance.arrayNode();
            for (XWPFRun run : p.getRuns()) {
                if (!run.getEmbeddedPictures().isEmpty()) {
                    flushParagraph(runs);
                    runs = JsonNodeFactory.instance.arrayNode();
                    pictures(run);
                }
                appendRun(runs, run);
            }
            flushParagraph(runs);
        }

        private void flushParagraph(ArrayNode runs) {
            boolean hasText = false;
            for (var run : runs) {
                hasText |= !run.path("text").asText("").isBlank();
            }
            if (hasText) {
                ObjectNode content = JsonNodeFactory.instance.objectNode();
                content.set("runs", runs);
                if (align != null) {
                    content.put("align", align);
                }
                add(BlockType.PARAGRAPH, content);
            }
        }

        private void listItem(XWPFParagraph p) {
            if (list == null || !p.getNumID().equals(listId)) {
                endList();
                list = JsonNodeFactory.instance.objectNode();
                String format = p.getNumFmt();
                list.put("ordered", format != null && !format.equalsIgnoreCase("bullet") && !format.equalsIgnoreCase("none"));
                list.putArray("items");
                listId = p.getNumID();
            }
            ArrayNode items = (ArrayNode) list.get("items");
            if (items.size() >= DocumentModelService.MAX_ITEMS) {
                return;
            }
            ObjectNode item = items.addObject();
            item.put("level", p.getNumIlvl() == null ? 0 : Math.max(0, Math.min(4, p.getNumIlvl().intValue())));
            ArrayNode runs = item.putArray("runs");
            for (XWPFRun run : p.getRuns()) {
                appendRun(runs, run);
            }
        }

        private void endList() {
            if (list != null) {
                add(BlockType.LIST, list);
                list = null;
                listId = null;
            }
        }

        private void table(XWPFTable table) {
            ObjectNode content = JsonNodeFactory.instance.objectNode();
            List<XWPFTableRow> rows = table.getRows();
            content.put("headerRow", !rows.isEmpty() && rows.get(0).isRepeatHeader());
            ArrayNode outRows = content.putArray("rows");
            for (XWPFTableRow row : rows.subList(0, Math.min(rows.size(), DocumentModelService.MAX_ROWS))) {
                ArrayNode outRow = outRows.addArray();
                List<XWPFTableCell> cells = row.getTableCells();
                for (XWPFTableCell cell : cells.subList(0, Math.min(cells.size(), DocumentModelService.MAX_CELLS))) {
                    ArrayNode runs = outRow.addObject().putArray("runs");
                    List<XWPFParagraph> paragraphs = cell.getParagraphs();
                    for (int i = 0; i < paragraphs.size(); i++) {
                        if (i > 0) {
                            runs.addObject().put("text", "\n");
                        }
                        for (XWPFRun run : paragraphs.get(i).getRuns()) {
                            if (!run.getEmbeddedPictures().isEmpty()) {
                                unsupported.merge("ảnh trong bảng", run.getEmbeddedPictures().size(), Integer::sum);
                            }
                            appendRun(runs, run);
                        }
                    }
                    if (!cell.getTables().isEmpty()) {
                        unsupported.merge("bảng lồng trong bảng", cell.getTables().size(), Integer::sum);
                    }
                }
                if (outRow.isEmpty()) {
                    outRow.addObject().putArray("runs");
                }
            }
            if (outRows.isEmpty()) {
                return;
            }
            if (rows.size() > DocumentModelService.MAX_ROWS) {
                unsupported.merge("hàng bảng vượt " + DocumentModelService.MAX_ROWS, 1, Integer::sum);
            }
            add(BlockType.TABLE, content);
        }

        private void appendRun(ArrayNode runs, XWPFRun run) {
            String text = run.text();
            if (text == null || text.isEmpty()) {
                return;
            }
            boolean bold = run.isBold();
            boolean italic = run.isItalic();
            boolean underline = run.getUnderline() != null && run.getUnderline() != UnderlinePatterns.NONE;
            String font = run.getFontFamily();
            boolean code = font != null && MONO.contains(font.toLowerCase(Locale.ROOT));
            boolean strike = run.isStrikeThrough();
            String rawColor = run.getColor();
            String color = rawColor != null && rawColor.matches("[0-9a-fA-F]{6}") && !rawColor.equals("000000")
                    ? "#" + rawColor.toUpperCase(Locale.ROOT) : null;
            if (!runs.isEmpty()) {
                ObjectNode last = (ObjectNode) runs.get(runs.size() - 1);
                if (last.path("bold").asBoolean(false) == bold && last.path("italic").asBoolean(false) == italic
                        && last.path("underline").asBoolean(false) == underline
                        && last.path("code").asBoolean(false) == code
                        && last.path("strike").asBoolean(false) == strike
                        && java.util.Objects.equals(last.path("color").asText(null), color)) {
                    last.put("text", last.path("text").asText("") + text);
                    return;
                }
            }
            ObjectNode next = runs.addObject();
            next.put("text", text);
            if (bold) {
                next.put("bold", true);
            }
            if (italic) {
                next.put("italic", true);
            }
            if (underline) {
                next.put("underline", true);
            }
            if (code) {
                next.put("code", true);
            }
            if (strike) {
                next.put("strike", true);
            }
            if (color != null) {
                next.put("color", color);
            }
        }

        private void pictures(XWPFParagraph p) {
            for (XWPFRun run : p.getRuns()) {
                if (!run.getEmbeddedPictures().isEmpty()) {
                    pictures(run);
                }
            }
        }

        private void pictures(XWPFRun run) {
            List<DrawingInfo> drawings = drawings(run);
            String runLink = run instanceof XWPFHyperlinkRun hyperlinkRun ? url(hyperlinkRun) : null;
            List<XWPFPicture> pictures = run.getEmbeddedPictures();
            for (int i = 0; i < pictures.size(); i++) {
                XWPFPicture picture = pictures.get(i);
                DrawingInfo info = i < drawings.size() ? drawings.get(i) : new DrawingInfo(null, null);
                String alt = info.description() != null && !info.description().isBlank() ? info.description()
                        : picture.getDescription();
                picture(picture, alt == null ? "" : alt, info.link() != null ? info.link() : runLink);
            }
        }

        private void picture(XWPFPicture picture, String alt, String link) {
            byte[] data = picture.getPictureData() == null ? null : picture.getPictureData().getData();
            String cleanAlt = CODE.matcher(alt).replaceAll("").strip();
            if (cleanAlt.length() > DocumentModelService.MAX_ALT) {
                cleanAlt = cleanAlt.substring(0, DocumentModelService.MAX_ALT);
            }
            Matcher code = CODE.matcher(alt);
            String xml = null;
            String how = null;
            boolean looked = false;
            if (code.find()) {
                looked = true;
                xml = resolve(code.group(1));
                how = "DIAGRAM_RESTORED_BY_CODE";
            }
            if (xml == null && link != null) {
                Matcher matcher = LINK.matcher(link);
                if (matcher.find()) {
                    looked = true;
                    xml = resolve((matcher.group(1).equals("attempts") ? "attempt/" : "") + matcher.group(2) + "/"
                            + matcher.group(3));
                    how = "DIAGRAM_RESTORED_BY_LINK";
                }
            }
            if (xml == null && data != null) {
                xml = PngChunks.readMxfile(data).filter(x -> DiagramXml.validate(x).isEmpty()).orElse(null);
                how = "DIAGRAM_RESTORED_BY_PNG";
            }
            String svg = svg(picture, data);
            if (xml == null && svg != null) {
                xml = svgContent(svg);
                how = "DIAGRAM_RESTORED_BY_SVG";
            }
            UUID id = UUID.randomUUID();
            if (xml != null && diagramEnabled) {
                ObjectNode content = JsonNodeFactory.instance.objectNode();
                content.put("diagramType", "OTHER");
                content.put("xml", xml);
                content.put("alt", cleanAlt);
                if (svg != null) {
                    SvgSanitizer.sanitize(svg).ifPresent(clean -> content.put("svgPreview", clean));
                }
                if (add(id, BlockType.DIAGRAM, content)) {
                    issues.add(new ValidationIssue(how, id, "Khôi phục sơ đồ"));
                }
                return;
            }
            if (xml != null || looked) {
                issues.add(new ValidationIssue("DIAGRAM_KEPT_AS_IMAGE", id, xml != null
                        ? "Bài chưa bật sơ đồ, sơ đồ được nhập thành ảnh" : "Không đọc được sơ đồ gốc, giữ là ảnh"));
            }
            store(id, picture, data, cleanAlt);
        }

        private void store(UUID id, XWPFPicture picture, byte[] data, String alt) {
            if (data == null || data.length == 0) {
                skippedImages++;
                return;
            }
            String name = picture.getPictureData().getFileName();
            FileRef ref;
            try (InputStream in = new ByteArrayInputStream(data)) {
                ref = artifacts.store(actor, ArtifactPurpose.DOCUMENT_IMAGE,
                        new FileUpload(name == null ? "image" : name, data.length, in));
            } catch (IOException | RuntimeException e) {
                skippedImages++;
                return;
            }
            ObjectNode content = JsonNodeFactory.instance.objectNode();
            content.put(DocumentFiles.FILE_REF, ref.value());
            content.put("alt", alt);
            PngChunks.size(data).ifPresent(size -> content.put("width", Math.max(16, Math.min(600, size[0]))));
            add(id, BlockType.IMAGE, content);
        }

        private String resolve(String ref) {
            try {
                return resolver == null ? null : resolver.resolveXml(ref)
                        .filter(x -> DiagramXml.validate(x).isEmpty()).orElse(null);
            } catch (RuntimeException e) {
                return null;
            }
        }

        /** SVG của ảnh: chính ảnh nếu là SVG, hoặc phần {@code svgBlip} Word lưu kèm ảnh PNG dự phòng. */
        private String svg(XWPFPicture picture, byte[] data) {
            if (data != null && looksLikeSvg(data)) {
                return new String(data, java.nio.charset.StandardCharsets.UTF_8);
            }
            Element blip = find(picture.getCTPicture().getDomNode(), "svgBlip");
            if (blip == null) {
                return null;
            }
            String rid = attribute(blip, "embed");
            if (rid == null) {
                return null;
            }
            try {
                PackagePart part = doc.getPackagePart();
                PackageRelationship relationship = part.getRelationship(rid);
                if (relationship == null) {
                    return null;
                }
                try (InputStream in = part.getRelatedPart(relationship).getInputStream()) {
                    byte[] bytes = in.readNBytes(SvgSanitizer.MAX_SVG_BYTES + 1);
                    return bytes.length > SvgSanitizer.MAX_SVG_BYTES ? null
                            : new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
                }
            } catch (Exception e) {
                return null;
            }
        }

        private String svgContent(String svg) {
            try {
                Element root = SecureXml.parse(SecureXml.stripPrologue(svg)).getDocumentElement();
                String content = root.getAttribute("content");
                return content != null && !content.isBlank() && DiagramXml.validate(content).isEmpty() ? content : null;
            } catch (IllegalArgumentException e) {
                return null;
            }
        }

        private List<DrawingInfo> drawings(XWPFRun run) {
            List<DrawingInfo> result = new ArrayList<>();
            for (CTDrawing drawing : run.getCTR().getDrawingList()) {
                for (CTInline inline : drawing.getInlineList()) {
                    result.add(info(inline.getDocPr()));
                }
                for (CTAnchor anchor : drawing.getAnchorList()) {
                    result.add(info(anchor.getDocPr()));
                }
            }
            return result;
        }

        private DrawingInfo info(CTNonVisualDrawingProps props) {
            if (props == null) {
                return new DrawingInfo(null, null);
            }
            String link = null;
            if (props.isSetHlinkClick() && props.getHlinkClick().getId() != null) {
                try {
                    PackageRelationship relationship = doc.getPackagePart().getRelationship(props.getHlinkClick().getId());
                    link = relationship == null ? null : relationship.getTargetURI().toString();
                } catch (RuntimeException e) {
                    link = null;
                }
            }
            return new DrawingInfo(props.getDescr(), link);
        }

        private String url(XWPFHyperlinkRun run) {
            try {
                XWPFHyperlink hyperlink = run.getHyperlink(doc);
                return hyperlink == null ? null : hyperlink.getURL();
            } catch (RuntimeException e) {
                return null;
            }
        }

        private String headingStyle(XWPFParagraph p) {
            String id = p.getStyleID();
            if (id == null) {
                return null;
            }
            String name = id;
            XWPFStyle style = doc.getStyles() == null ? null : doc.getStyles().getStyle(id);
            if (style != null && style.getName() != null) {
                name = style.getName();
            }
            String key = name.toLowerCase(Locale.ROOT).replace(" ", "");
            String idKey = id.toLowerCase(Locale.ROOT).replace(" ", "");
            for (String candidate : List.of(key, idKey)) {
                if (candidate.equals("title")) {
                    return "TITLE";
                }
                if (candidate.equals("subtitle")) {
                    return "SUBTITLE";
                }
                if (candidate.length() == 8 && candidate.startsWith("heading") && candidate.charAt(7) >= '1'
                        && candidate.charAt(7) <= '6') {
                    return "H" + candidate.charAt(7);
                }
            }
            return null;
        }

        private void countUnsupported(Node node) {
            NodeList children = node.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                if (child instanceof Element element) {
                    String name = DiagramXml.name(element);
                    if (UNSUPPORTED.containsKey(name)) {
                        unsupported.merge(UNSUPPORTED.get(name), 1, Integer::sum);
                        continue;
                    }
                    if (name.equals("graphicData")) {
                        String uri = element.getAttribute("uri");
                        if (uri.endsWith("/diagram")) {
                            unsupported.merge("SmartArt", 1, Integer::sum);
                        } else if (uri.endsWith("/chart")) {
                            unsupported.merge("biểu đồ", 1, Integer::sum);
                        }
                    }
                    countUnsupported(element);
                }
            }
        }

        private void add(BlockType type, ObjectNode content) {
            add(UUID.randomUUID(), type, content);
        }

        private boolean add(UUID id, BlockType type, ObjectNode content) {
            if (blocks.size() >= DocumentModelService.MAX_BLOCKS) {
                truncated = true;
                return false;
            }
            blocks.add(new DocumentBlock(id, origin, type, content));
            return true;
        }
    }

    private record DrawingInfo(String description, String link) {
    }

    private static boolean looksLikeSvg(byte[] data) {
        int length = Math.min(data.length, 512);
        String head = new String(data, 0, length, java.nio.charset.StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
        return head.contains("<svg");
    }

    private static Element find(Node node, String localName) {
        if (node instanceof Element element && localName.equals(DiagramXml.name(element))) {
            return element;
        }
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Element found = find(children.item(i), localName);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private static String attribute(Element element, String localName) {
        NamedNodeMap attributes = element.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node attribute = attributes.item(i);
            if (localName.equals(DiagramXml.name(attribute))) {
                return attribute.getNodeValue();
            }
        }
        return null;
    }

    static Optional<String> code(String alt) {
        Matcher matcher = CODE.matcher(alt == null ? "" : alt);
        return matcher.find() ? Optional.of(matcher.group(1)) : Optional.empty();
    }
}
