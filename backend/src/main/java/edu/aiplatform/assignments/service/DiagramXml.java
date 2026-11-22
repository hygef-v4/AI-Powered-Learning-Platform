package edu.aiplatform.assignments.service;

import edu.aiplatform.assignments.contract.DiagramType;
import java.io.ByteArrayOutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
    private static final int MAX_PAGES = 100;
    private static final int MAX_LABEL = 200;
    private static final int MAX_CONTAINMENT_SHAPES = 500;

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
            listGraphModels(xml);
            return Optional.empty();
        } catch (IllegalArgumentException e) {
            return Optional.of("DIAGRAM_XML_INVALID");
        }
    }

    /** Số hình và đường nối; 0 là sơ đồ rỗng. XML lỗi trả 0. */
    static int countCells(String xml) {
        try {
            int count = 0;
            for (Element model : listGraphModels(xml)) {
                count += listCells(model).size();
            }
            return count;
        } catch (IllegalArgumentException e) {
            return 0;
        }
    }

    /**
     * Loại sơ đồ đoán từ kiểu hình Draw.io (yêu cầu 2026-10-22: chèn sơ đồ không chọn loại): lifeline là tuần tự;
     * component, module, giao diện là thành phần; bảng, mũi tên ER hoặc thoi nối hình elip không mũi tên (Chen) là ER;
     * actor là use case; nút đầu, cuối kèm thoi, thanh fork hoặc làn là hoạt động, chỉ nút đầu, cuối là trạng thái; lớp
     * UML (swimlane xếp ô) là lớp; còn thoi là hoạt động. Không nhận ra hoặc XML lỗi: {@code OTHER}.
     */
    static DiagramType detectType(String xml) {
        Set<String> shapes = new HashSet<>();
        Set<String> tokens = new HashSet<>();
        boolean arrows = false;
        try {
            for (Element model : listGraphModels(xml)) {
                NodeList all = model.getElementsByTagNameNS("*", "*");
                for (int i = 0; i < all.getLength(); i++) {
                    Element element = (Element) all.item(i);
                    boolean edge = "1".equals(element.getAttribute("edge"));
                    if (!"mxCell".equals(readNodeName(element)) || (!edge && !"1".equals(element.getAttribute("vertex")))) {
                        continue;
                    }
                    String style = element.getAttribute("style");
                    arrows |= edge && !style.contains("endArrow=none");
                    for (String raw : style.split(";")) {
                        String token = raw.strip();
                        if (token.startsWith("shape=")) {
                            shapes.add(token.substring(6));
                        } else if (!token.isEmpty() && !token.contains("=")) {
                            shapes.add(token);
                        } else if (!token.isEmpty()) {
                            tokens.add(token);
                        }
                    }
                }
            }
        } catch (IllegalArgumentException e) {
            return DiagramType.OTHER;
        }
        boolean stacked = shapes.contains("swimlane") && tokens.contains("childLayout=stackLayout");
        if (shapes.contains("umlLifeline") || shapes.contains("umlDestroy")) {
            return DiagramType.SEQUENCE;
        }
        if (shapes.contains("component") || shapes.contains("module") || shapes.contains("providedRequiredInterface")
                || shapes.contains("requiredInterface") || shapes.contains("lollipop")) {
            return DiagramType.COMPONENT;
        }
        if (shapes.contains("table") || shapes.contains("associativeEntity") || tokens.contains("childLayout=tableLayout")
                || tokens.contains("edgeStyle=entityRelationEdgeStyle")
                || tokens.stream().anyMatch(t -> t.startsWith("endArrow=ER") || t.startsWith("startArrow=ER"))) {
            return DiagramType.ER;
        }
        if (shapes.contains("umlActor")) {
            return DiagramType.USE_CASE;
        }
        if (shapes.contains("startState") || shapes.contains("endState")) {
            return shapes.contains("rhombus") || shapes.contains("line") || (shapes.contains("swimlane") && !stacked)
                    ? DiagramType.ACTIVITY : DiagramType.STATE;
        }
        if (stacked) {
            return DiagramType.CLASS;
        }
        if (shapes.contains("rhombus")) {
            return shapes.contains("ellipse") && !arrows ? DiagramType.ER : DiagramType.ACTIVITY;
        }
        return DiagramType.OTHER;
    }

    /** Dạng chữ ngắn gọn: mỗi trang liệt kê hình (nhãn, kiểu) và đường nối (nguồn → đích, nét đứt, nhãn). */
    static String compactXml(String xml, int maxChars) {
        StringBuilder out = new StringBuilder();
        try {
            List<Element> models = listGraphModels(xml);
            Document document = SecureXml.parse(xml);
            Element root = document.getDocumentElement();
            List<Element> pages = new ArrayList<>();
            if ("mxfile".equals(readNodeName(root))) {
                pages.addAll(listChildren(root, "diagram"));
            }
            for (int i = 0; i < models.size(); i++) {
                String page = i < pages.size() ? pages.get(i).getAttribute("name") : "";
                out.append("Trang ").append(i + 1);
                if (!page.isBlank()) {
                    out.append(" \"").append(clean(page)).append('"');
                }
                out.append(":\n");
                List<Cell> cells = listCells(models.get(i));
                Map<String, String> containers = mapContainers(cells);
                Map<String, String> labels = new HashMap<>();
                for (Cell cell : cells) {
                    if (!cell.edge() && !cell.label().isBlank()) {
                        labels.putIfAbsent(cell.id(), cell.label());
                    }
                }
                for (Cell cell : cells) {
                    if (cell.edge()) {
                        out.append("- nối ").append(formatEndpoint(cell.source(), labels))
                                .append(" -> ").append(formatEndpoint(cell.target(), labels));
                        if (cell.dashed()) {
                            out.append(" (nét đứt)");
                        } else if (cell.label().contains("<<")) {
                            // Stereotype như <<include>>/<<extend>> phải vẽ nét đứt: ghi rõ nét liền để AI không mặc định là đúng (G2).
                            out.append(" (nét liền)");
                        }
                    } else {
                        // Hình không có shape trong style là hình chữ nhật mặc định của Draw.io (thường là system boundary).
                        out.append("- hình ").append(cell.id())
                                .append(" (").append(cell.shape().isEmpty() ? "chữ nhật" : cell.shape()).append(')');
                        String container = containers.get(cell.id());
                        if (container != null) {
                            out.append(" trong ").append(container);
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

    private static String formatEndpoint(String id, Map<String, String> labels) {
        String label = labels.get(id);
        return label == null ? id : '"' + label.replace("\"", "\\\"") + "\" [" + id + "]";
    }

    /** Các {@code mxGraphModel}: gốc là model, hoặc mỗi {@code diagram} của {@code mxfile} (giải nén nếu cần). */
    static List<Element> listGraphModels(String xml) {
        return listGraphModels(xml, MAX_INFLATED_BYTES);
    }

    static List<Element> listGraphModels(String xml, int maxExpandedBytes) {
        if (xml == null) {
            throw new IllegalArgumentException("Sơ đồ không hợp lệ");
        }
        int xmlBytes = xml.getBytes(StandardCharsets.UTF_8).length;
        if (xmlBytes > MAX_XML_BYTES || xmlBytes > maxExpandedBytes) {
            throw new IllegalArgumentException("Sơ đồ quá lớn");
        }
        int[] remainingBytes = {maxExpandedBytes - xmlBytes};
        Document document = SecureXml.parse(xml);
        Element root = document.getDocumentElement();
        String rootName = readNodeName(root);
        if ("mxGraphModel".equals(rootName)) {
            return List.of(root);
        }
        if (!"mxfile".equals(rootName)) {
            throw new IllegalArgumentException("Gốc phải là mxfile hoặc mxGraphModel");
        }
        List<Element> models = new ArrayList<>();
        List<Element> diagrams = listChildren(root, "diagram");
        if (diagrams.size() > MAX_PAGES) {
            throw new IllegalArgumentException("Sơ đồ có quá nhiều trang");
        }
        for (Element diagram : diagrams) {
            List<Element> inline = listChildren(diagram, "mxGraphModel");
            if (!inline.isEmpty()) {
                models.add(inline.get(0));
                continue;
            }
            String packed = diagram.getTextContent() == null ? "" : diagram.getTextContent().strip();
            if (packed.isEmpty()) {
                continue;
            }
            Element model = SecureXml.parse(inflateXml(packed, remainingBytes)).getDocumentElement();
            if (!"mxGraphModel".equals(readNodeName(model))) {
                throw new IllegalArgumentException("Trang sơ đồ không hợp lệ");
            }
            models.add(model);
        }
        return models;
    }

    /** Trang nén của Draw.io: base64 → raw deflate → URL-decode. */
    private static String inflateXml(String packed, int[] remainingBytes) {
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
                if (n > remainingBytes[0]) {
                    throw new IllegalArgumentException("Trang sơ đồ quá lớn");
                }
                remainingBytes[0] -= n;
                out.write(buffer, 0, n);
            }
            return URLDecoder.decode(out.toString(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
        } catch (DataFormatException e) {
            throw new IllegalArgumentException("Trang sơ đồ không giải được", e);
        } finally {
            inflater.end();
        }
    }

    private static List<Cell> listCells(Element model) {
        List<Cell> cells = new ArrayList<>();
        NodeList all = model.getElementsByTagNameNS("*", "*");
        for (int i = 0; i < all.getLength(); i++) {
            Element element = (Element) all.item(i);
            if (!"mxCell".equals(readNodeName(element))) {
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
            if (parent instanceof Element wrapper && ("UserObject".equals(readNodeName(wrapper)) || "object".equals(readNodeName(wrapper)))) {
                id = wrapper.getAttribute("id");
                label = wrapper.getAttribute("label");
            }
            String style = element.getAttribute("style");
            cells.add(new Cell(id, edge, element.getAttribute("source"), element.getAttribute("target"),
                    clean(label), resolveShape(style), style.contains("dashed=1"), element.getAttribute("parent"),
                    vertex ? readBounds(element) : null));
        }
        return cells;
    }

    /**
     * Hình chứa mỗi hình: cha trong XML nếu cha là một hình; nếu không thì hình nhỏ nhất bao trọn nó theo tọa độ,
     * vì khung system boundary thường chỉ nằm đè lên use case chứ không làm cha.
     */
    private static Map<String, String> mapContainers(List<Cell> cells) {
        Map<String, Cell> vertices = new HashMap<>();
        cells.stream().filter(c -> !c.edge()).forEach(c -> vertices.put(c.id(), c));
        List<Cell> topLevel = vertices.values().stream()
                .filter(c -> !vertices.containsKey(c.parent()) && c.bounds() != null).toList();
        Map<String, String> containers = new HashMap<>();
        for (Cell cell : vertices.values()) {
            if (vertices.containsKey(cell.parent())) {
                containers.put(cell.id(), cell.parent());
                continue;
            }
            // shortcut: so từng cặp O(n²); sơ đồ quá MAX_CONTAINMENT_SHAPES hình thì bỏ qua, nâng cấp nếu gặp sơ đồ lớn hơn.
            if (cell.bounds() == null || topLevel.size() > MAX_CONTAINMENT_SHAPES) {
                continue;
            }
            Cell best = null;
            for (Cell other : topLevel) {
                if (other != cell && isInside(cell.bounds(), other.bounds())
                        && (best == null || area(other.bounds()) < area(best.bounds()))) {
                    best = other;
                }
            }
            if (best != null) {
                containers.put(cell.id(), best.id());
            }
        }
        return containers;
    }

    private static boolean isInside(double[] inner, double[] outer) {
        return inner[0] >= outer[0] && inner[1] >= outer[1] && inner[0] + inner[2] <= outer[0] + outer[2]
                && inner[1] + inner[3] <= outer[1] + outer[3] && area(inner) < area(outer);
    }

    private static double area(double[] bounds) {
        return bounds[2] * bounds[3];
    }

    /** {x, y, rộng, cao} từ mxGeometry; thiếu rộng/cao hoặc không đọc được thì null. */
    private static double[] readBounds(Element cell) {
        for (Element geometry : listChildren(cell, "mxGeometry")) {
            try {
                double width = Double.parseDouble(geometry.getAttribute("width"));
                double height = Double.parseDouble(geometry.getAttribute("height"));
                return width > 0 && height > 0
                        ? new double[] {readCoordinate(geometry, "x"), readCoordinate(geometry, "y"), width, height}
                        : null;
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private static double readCoordinate(Element geometry, String name) {
        String value = geometry.getAttribute(name);
        return value.isBlank() ? 0 : Double.parseDouble(value);
    }

    private static String resolveShape(String style) {
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

    /** Bỏ thẻ HTML trong nhãn, giữ stereotype {@code <<include>>}, gộp khoảng trắng, cắt ngắn. */
    static String clean(String label) {
        if (label == null) {
            return "";
        }
        // Nhãn không HTML được trình phân tích XML giải sẵn thành <<include>>; tạm đổi sang « » để không bị xóa như thẻ.
        String text = label.replaceAll("<<([^<>]{1,80})>>", "«$1»")
                .replaceAll("(?i)<br\\s*/?>", " ").replaceAll("<[^>]*>", " ")
                .replace("&nbsp;", " ").replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&")
                .replace("«", "<<").replace("»", ">>")
                .replaceAll("\\s+", " ").strip();
        return text.length() > MAX_LABEL ? text.substring(0, MAX_LABEL) + "…" : text;
    }

    static String readNodeName(Node node) {
        return node.getLocalName() != null ? node.getLocalName() : node.getNodeName();
    }

    private static List<Element> listChildren(Element parent, String localName) {
        List<Element> result = new ArrayList<>();
        NodeList nodes = parent.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            if (nodes.item(i) instanceof Element child && localName.equals(readNodeName(child))) {
                result.add(child);
            }
        }
        return result;
    }

    private record Cell(String id, boolean edge, String source, String target, String label, String shape,
                boolean dashed, String parent, double[] bounds) {
    }
}
