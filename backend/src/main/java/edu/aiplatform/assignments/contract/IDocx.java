package edu.aiplatform.assignments.contract;

import edu.aiplatform.identity.contract.Actor;

/** Xuất, nhập DOCX cho bài làm cùng cách giữ sơ đồ như đề. Dùng bởi U08, U12. */
public interface IDocx {

    byte[] export(DocumentModel document, String title, IDiagramLinker diagramLinker);

    /**
     * Đọc DOCX trong bộ nhớ thành bản xem trước; ảnh được lưu tạm qua U03 dưới tên {@code actor} (chưa gắn),
     * block ảnh mang {@code fileRef} để unit gọi gắn khi lưu. Không lưu file DOCX.
     */
    DocxImportPreview importPreview(Actor actor, byte[] docx, BlockOrigin origin, boolean diagramEnabled,
                                    IDiagramResolver diagramResolver);
}
