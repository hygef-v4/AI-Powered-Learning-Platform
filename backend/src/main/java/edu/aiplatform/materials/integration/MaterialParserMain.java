package edu.aiplatform.materials.integration;

import java.io.*;
import java.nio.file.*;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.pdf.PDFParser;
import org.apache.tika.parser.pdf.PDFParserConfig;
import org.apache.tika.parser.microsoft.ooxml.OOXMLParser;
import org.apache.tika.extractor.EmbeddedDocumentExtractor;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

/** Runs in a disposable JVM with a fixed heap. Never boots Spring or executes macros/OCR. */
public final class MaterialParserMain {
    private MaterialParserMain() { }
    public static void main(String[] args) throws Exception {
        Metadata metadata=new Metadata();
        ParseContext context=new ParseContext();
        PDFParserConfig pdf=new PDFParserConfig();
        pdf.setOcrStrategy(PDFParserConfig.OCR_STRATEGY.NO_OCR);
        pdf.setExtractInlineImages(false);context.set(PDFParserConfig.class,pdf);
        context.set(EmbeddedDocumentExtractor.class,new EmbeddedDocumentExtractor() {
            @Override public boolean shouldParseEmbedded(Metadata ignored) {return false;}
            @Override public void parseEmbedded(InputStream stream,ContentHandler handler,Metadata ignored,boolean outputHtml) { }
        });
        TextCollector collector=new TextCollector();
        try(InputStream stream=Files.newInputStream(Path.of(args[0]))) {
            new AutoDetectParser(new PDFParser(),new OOXMLParser()).parse(stream,collector,metadata,context);
        }
        int pages=collector.pages;
        for(String key:new String[]{"xmpTPg:NPages","meta:page-count","meta:slide-count"}) {
            String count=metadata.get(key);
            if(count!=null)try{pages=Math.max(pages,Integer.parseInt(count));}catch(NumberFormatException ignored){ }
        }
        try(DataOutputStream out=new DataOutputStream(Files.newOutputStream(Path.of(args[1])))) {
            byte[] bytes=collector.text.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
            out.writeInt(Math.max(1,pages));out.writeInt(bytes.length);out.write(bytes);
        }
    }
    static final class TextCollector extends DefaultHandler {
        final StringBuilder text=new StringBuilder();int pages;
        @Override public void startElement(String uri,String local,String name,Attributes attributes) {
            String classes=attributes.getValue("class");
            if(classes!=null && (classes.equals("page")||classes.equals("slide")))pages++;
            append("\n");
        }
        @Override public void characters(char[] chars,int offset,int length) {
            int remaining=1_000_000-text.length();if(remaining>0)text.append(chars,offset,Math.min(length,remaining));
        }
        private void append(String value) {if(text.length()<1_000_000)text.append(value);}
    }
}
