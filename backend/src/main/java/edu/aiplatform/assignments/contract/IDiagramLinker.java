package edu.aiplatform.assignments.contract;

import java.util.UUID;

/** Khi xuất DOCX: chuỗi ghi vào alt text của ảnh sơ đồ, ví dụ {@code [drawio:assignmentId/blockId]}. */
public interface IDiagramLinker {

    String linkFor(UUID blockId);
}
