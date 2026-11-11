package edu.aiplatform.files.contract;

/**
 * {@code MATERIAL}: học liệu U13. {@code DOCUMENT_IMAGE}: ảnh tài liệu. {@code ASSIGNMENT_SOURCE}: file nguồn riêng cho bản
 * nháp U07. {@code ASSIGNMENT_IMPORT}: ảnh/PDF đề cũ U07 nhập thành khung đề, chỉ kiểm, không lưu (BR-U03-99).
 */
public enum ArtifactPurpose {
    MATERIAL, DOCUMENT_IMAGE, ASSIGNMENT_SOURCE, ASSIGNMENT_IMPORT;

    /** Mục đích chỉ để kiểm tệp, không lưu lên kho. */
    public boolean storable() {
        return this != ASSIGNMENT_IMPORT;
    }
}
