package edu.aiplatform.assignments.application;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Parser của JDK đã khóa (NFR-U07-30): cấm DOCTYPE, entity ngoài, XInclude. SVG của Draw.io có khai báo DOCTYPE
 * chuẩn không kèm subset; {@link #stripPrologue} bỏ phần đó trước khi parse, DOCTYPE có subset ({@code [}) bị từ chối.
 */
final class SecureXml {

    private static final Pattern PROLOGUE = Pattern.compile(
            "^\\s*(<\\?xml[^>]*\\?>)?\\s*(<!DOCTYPE[^>\\[]*>)?", Pattern.CASE_INSENSITIVE);

    private SecureXml() {
    }

    static Document parse(String xml) {
        try {
            DocumentBuilder builder = factory().newDocumentBuilder();
            builder.setErrorHandler(new DefaultHandler());
            builder.setEntityResolver((publicId, systemId) -> {
                throw new SAXException("Không cho tham chiếu ngoài");
            });
            return builder.parse(new InputSource(new StringReader(xml)));
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new IllegalArgumentException("XML không hợp lệ", e);
        }
    }

    /** Bỏ khai báo XML và DOCTYPE không có subset; còn DOCTYPE khác thì parse sẽ từ chối. */
    static String stripPrologue(String xml) {
        Matcher matcher = PROLOGUE.matcher(xml);
        return matcher.lookingAt() ? xml.substring(matcher.end()) : xml;
    }

    static String serialize(Node node) {
        try {
            TransformerFactory factory = TransformerFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
            Transformer transformer = factory.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            StringWriter out = new StringWriter();
            transformer.transform(new DOMSource(node), new StreamResult(out));
            return out.toString();
        } catch (TransformerException e) {
            throw new IllegalArgumentException("Không ghi được XML", e);
        }
    }

    private static DocumentBuilderFactory factory() throws ParserConfigurationException {
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
        factory.setNamespaceAware(true);
        return factory;
    }
}
