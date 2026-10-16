package edu.aiplatform.assignments.contract;

/** Xuất, nhập DOCX cho bài làm cùng cách giữ sơ đồ như đề. Dùng bởi U08, U12. */
public interface IDocx {

    byte[] export(DocumentModel document, String title, IDiagramLinker diagramLinker);

    DocxImportPreview importPreview(byte[] docx, BlockOrigin origin, boolean diagramEnabled,
                                    IDiagramResolver diagramResolver);
}
