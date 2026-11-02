package edu.aiplatform.assignments.service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Làm sạch SVG xem trước của sơ đồ (NFR-U07-30): bỏ phần tử chạy được (script, iframe, object, embed), thuộc tính
 * sự kiện {@code on*}, link không phải {@code #…} hay ảnh {@code data:image/…}, {@code url(…)} trỏ ra ngoài và
 * {@code <style>} có {@code @import} hoặc {@code url(}. SVG không parse được thì bỏ, không lưu.
 */
final class SvgSanitizer {

    static final int MAX_SVG_BYTES = 4 * 1024 * 1024;
    private static final Set<String> FORBIDDEN = Set.of("script", "iframe", "object", "embed", "frame", "frameset",
            "meta", "link", "base", "form", "input", "button", "textarea", "audio", "video", "handler", "listener");
    private static final Pattern EXTERNAL_URL = Pattern.compile("url\\(\\s*(?!['\"]?\\s*#)[^)]*\\)",
            Pattern.CASE_INSENSITIVE);

    private SvgSanitizer() {
    }

    static Optional<String> sanitize(String svg) {
        if (svg == null || svg.isBlank() || svg.getBytes(StandardCharsets.UTF_8).length > MAX_SVG_BYTES) {
            return Optional.empty();
        }
        try {
            Document document = SecureXml.parse(SecureXml.stripPrologue(svg));
            Element root = document.getDocumentElement();
            if (!"svg".equals(DiagramXml.name(root))) {
                return Optional.empty();
            }
            // Bản sao XML Draw.io gắn trong SVG (thuộc tính content) trùng với xml của block; bỏ cho nhẹ.
            root.removeAttribute("content");
            clean(root);
            return Optional.of(SecureXml.serialize(root));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private static void clean(Element element) {
        NamedNodeMap attributes = element.getAttributes();
        List<Attr> remove = new ArrayList<>();
        for (int i = 0; i < attributes.getLength(); i++) {
            Attr attribute = (Attr) attributes.item(i);
            String name = DiagramXml.name(attribute).toLowerCase(Locale.ROOT);
            String value = attribute.getValue() == null ? "" : attribute.getValue().strip();
            if (name.startsWith("on")) {
                remove.add(attribute);
            } else if (name.equals("href") || name.equals("src") || name.equals("action")
                    || name.equals("formaction")) {
                String lower = value.toLowerCase(Locale.ROOT);
                if (!(lower.startsWith("#") || lower.startsWith("data:image/png") || lower.startsWith("data:image/jpeg")
                        || lower.startsWith("data:image/gif") || lower.startsWith("data:image/webp"))) {
                    remove.add(attribute);
                }
            } else if (name.equals("style") && EXTERNAL_URL.matcher(value).find()) {
                attribute.setValue(EXTERNAL_URL.matcher(value).replaceAll("none"));
            }
        }
        remove.forEach(element::removeAttributeNode);

        NodeList children = element.getChildNodes();
        List<Node> drop = new ArrayList<>();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof Element nested) {
                String name = DiagramXml.name(nested).toLowerCase(Locale.ROOT);
                if (FORBIDDEN.contains(name)) {
                    drop.add(nested);
                } else if (name.equals("style")) {
                    String css = nested.getTextContent() == null ? "" : nested.getTextContent().toLowerCase(Locale.ROOT);
                    if (css.contains("@import") || EXTERNAL_URL.matcher(css).find() || css.contains("expression(")) {
                        drop.add(nested);
                    }
                } else {
                    clean(nested);
                }
            } else if (child.getNodeType() == Node.PROCESSING_INSTRUCTION_NODE) {
                drop.add(child);
            }
        }
        drop.forEach(element::removeChild);
    }
}
