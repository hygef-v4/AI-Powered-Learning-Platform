package edu.aiplatform.authoring.port;

import java.io.InputStream;

public interface DocxExportPort {
    InputStream export(DocumentModel document, String title);
}
