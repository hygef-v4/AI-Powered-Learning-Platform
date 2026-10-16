package edu.aiplatform.assignments.contract;

import java.util.Optional;

/** Khi nhập DOCX: khôi phục XML sơ đồ từ alt text nếu người nhập có quyền đọc nguồn. */
public interface IDiagramResolver {

    Optional<String> resolveXml(String diagramRef);
}
