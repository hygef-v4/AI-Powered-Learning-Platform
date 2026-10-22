package edu.aiplatform.assignments.contract;

import java.util.UUID;

/** Khi xuất DOCX: mã ghi vào alt text của ảnh sơ đồ, ví dụ {@code [drawio:assignmentId/blockId]}, và link xem sơ đồ. */
public interface IDiagramLinker {

    String linkFor(UUID blockId);

    /** Link tuyệt đối tới trang xem sơ đồ (trang tự kiểm quyền); {@code null} thì ảnh không có hyperlink. */
    default String viewUrlFor(UUID blockId) {
        return null;
    }
}
