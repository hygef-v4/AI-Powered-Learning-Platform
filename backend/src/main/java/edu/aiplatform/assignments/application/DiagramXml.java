package edu.aiplatform.assignments.application;

import java.io.ByteArrayOutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * XML Draw.io (BR-U07-12): gốc {@code mxfile} hoặc {@code mxGraphModel}; trang nén (base64, deflate, URL-encode)
 * được giải có trần. Dùng để kiểm, đếm hình, rút gọn cho prompt AI ({@code compactDiagramXml}).
 */
final class DiagramXml {

    static final int MAX_XML_BYTES = 2 * 1024 * 1024;
    private static final int MAX_INFLATED_BYTES = 10 * 1024 * 1024;
    private static final int MAX_LABEL = 200;

    private DiagramXml() {
    }

    /** Mã lỗi khi XML không dùng được; rỗng là hợp lệ. */
    static Optional<String> validate(String xml) {
        if (xml == null || xml.isBlank()) {
            return Optional.of("DIAGRAM_XML_INVALID");
        }
        if (xml.getBytes(StandardCharsets.UTF_8).length > MAX_XML_BYTES) {
            return Optional.of("DIAGRAM_TOO_LARGE");
        }
        try {
            graphModels(xml);
            return Optional.empty();
        } catch (IllegalArgumentException e) {
            return Optional.of("DIAGRAM_XML_INVALID");
        }
    }

    /** Số hình và đường nối; 0 là sơ đồ rỗng. XML lỗi trả 0. */
    static int cellCount(String xml) {
        try {
            int count = 0;
            for (Element model : graphModels(xml)) {
                count += cells(model).size();
            }
            return count;
        } catch (IllegalArgumentException e) {
            return 0;
        }
    }

    /** Dạng chữ ngắn gọn: mỗi trang liệt kê hình (nhãn, kiểu) và đường nối (nguồn → đích, nhãn). */
    static String compact(String xml, int maxChars) {
        StringBuilder out = new StringBuilder();
        try {
            Document document = SecureXml.parse(xml);
            Element root = document.getDocumentElement();
            List<Element> pages = new ArrayList<>();
            if ("mxfile".equals(name(root))) {
                pages.addAll(children(root, "diagram"));
            }
            List<Element> models = graphModels(xml);
            for (int i = 0; i < models.size(); i++) {
                String page = i < pages.size() ? pages.get(i).getAttribute("name") : "";
                out.append("Trang ").append(i + 1);
                if (!page.isBlank()) {
                    out.append(" \"").append(clean(page)).append('"');
                }
                out.append(":\n");
                for (Cell cell : cells(models.get(i))) {
                    if (cell.edge()) {
                        out.append("- nối ").append(cell.source()).append(" -> ").append(cell.target());
                    } else {
                        out.append("- hình ").append(cell.id());
                        if (!cell.shape().isEmpty()) {
                            out.append(" (").append(cell.shape()).append(')');
                        }
                    }
                    if (!cell.label().isEmpty()) {
                        out.append(": ").append(cell.label());
                    }
                    out.append('\n');
                    if (out.length() > maxChars) {
                        out.setLength(maxChars);
                        return out.append("\n…(đã cắt)").toString();
                    }
                }
            }
        } catch (IllegalArgumentException e) {
            return "(sơ đồ không đọc được)";
        }
        return out.toString().strip();
    }

    /** Các {@code mxGraphModel}: gốc là model, hoặc mỗi {@code diagram} của {@code mxfile} (giải nén nếu cần). */
    static List<Element> graphModels(String xml) {
        Document document = SecureXml.parse(xml);
        Element root = document.getDocumentElement();
        String rootName = name(root);
        if ("mxGraphModel".equals(rootName)) {
            return List.of(root);
        }
        if (!"mxfile".equals(rootName)) {
            throw new IllegalArgumentException("Gốc phải là mxfile hoặc mxGraphModel");
        }
        List<Element> models = new ArrayList<>();
        for (Element diagram : children(root, "diagram")) {
            List<Element> inline = children(diagram, "mxGraphModel");
            if (!inline.isEmpty()) {
                models.add(inline.get(0));
                continue;
            }
            String packed = diagram.getTextContent() == null ? "" : diagram.getTextContent().strip();
            if (packed.isEmpty()) {
                continue;
            }
            Element model = SecureXml.parse(inflate(packed)).getDocumentElement();
            if (!"mxGraphModel".equals(name(model))) {
                throw new IllegalArgumentException("Trang sơ đồ không hợp lệ");
            }
            models.add(model);
        }
        return models;
    }

    /** Trang nén của Draw.io: base64 → raw deflate → URL-decode. */
    static String inflate(String packed) {
        byte[] compressed;
        try {
            compressed = Base64.getDecoder().decode(packed.replaceAll("\\s", ""));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Trang sơ đồ không giải được", e);
        }
        Inflater inflater = new Inflater(true);
        try {
            inflater.setInput(compressed);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            while (!inflater.finished()) {
                int n = inflater.inflate(buffer);
                if (n == 0 && (inflater.needsInput() || inflater.needsDictionary())) {
                    break;
                }
                out.write(buffer, 0, n);
                if (out.size() > MAX_INFLATED_BYTES) {
                    throw new IllegalArgumentException("Trang sơ đồ quá lớn");
                }
            }
            return URLDecoder.decode(out.toString(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
        } catch (DataFormatException e) {
            throw new IllegalArgumentException("Trang sơ đồ không giải được", e);
        } finally {
            inflater.end();
        }
    }

    private static List<Cell> cells(Element model) {
        List<Cell> cells = new ArrayList<>();
        NodeList all = model.getElementsByTagNameNS("*", "*");
        for (int i = 0; i < all.getLength(); i++) {
            Element element = (Element) all.item(i);
            if (!"mxCell".equals(name(element))) {
                continue;
            }
            boolean vertex = "1".equals(element.getAttribute("vertex"));
            boolean edge = "1".equals(element.getAttribute("edge"));
            if (!vertex && !edge) {
                continue;
            }
            String id = element.getAttribute("id");
            String label = element.getAttribute("value");
            Node parent = element.getParentNode();
            if (parent instanceof Element wrapper && ("UserObject".equals(name(wrapper)) || "object".equals(name(wrapper)))) {
                id = wrapper.getAttribute("id");
                label = wrapper.getAttribute("label");
            }
            cells.add(new Cell(id, edge, element.getAttribute("source"), element.getAttribute("target"),
                    clean(label), shape(element.getAttribute("style"))));
        }
        return cells;
    }

    private static String shape(String style) {
        if (style == null || style.isBlank()) {
            return "";
        }
        for (String token : style.split(";")) {
            if (token.startsWith("shape=")) {
                return token.substring(6);
            }
        }
        String first = style.split(";")[0];
        return first.contains("=") ? "" : first;
    }

    /** Bỏ thẻ HTML trong nhãn, gộp khoảng trắng, cắt ngắn. */
    static String clean(String label) {
        if (label == null) {
            return "";
        }
        String text = label.replaceAll("(?i)<br\\s*/?>", " ").replaceAll("<[^>]*>", " ")
                .replace("&nbsp;", " ").replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&")
                .replaceAll("\\s+", " ").strip();
        return text.length() > MAX_LABEL ? text.substring(0, MAX_LABEL) + "…" : text;
    }

    static String name(Node node) {
        return node.getLocalName() != null ? node.getLocalName() : node.getNodeName();
    }

    private static List<Element> children(Element parent, String localName) {
        List<Element> result = new ArrayList<>();
        NodeList nodes = parent.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            if (nodes.item(i) instanceof Element child && localName.equals(name(child))) {
                result.add(child);
            }
        }
        return result;
    }

    private record Cell(String id, boolean edge, String source, String target, String label, String shape) {
    }
}
