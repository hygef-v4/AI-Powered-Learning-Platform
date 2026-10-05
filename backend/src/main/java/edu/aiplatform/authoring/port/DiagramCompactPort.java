package edu.aiplatform.authoring.port;

/** U13: XML Draw.io rút gọn theo allowlist, chỉ trong bộ nhớ (BR-U09-60). */
public interface DiagramCompactPort {
    String compact(String fullXml);
}
