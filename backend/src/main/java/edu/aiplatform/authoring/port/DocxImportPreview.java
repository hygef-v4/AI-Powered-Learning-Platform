package edu.aiplatform.authoring.port;

import java.util.List;

public record DocxImportPreview(DocumentModel blocks, int diagramCount, int imageCount, List<String> droppedParts) {}
