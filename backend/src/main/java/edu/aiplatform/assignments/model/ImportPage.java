package edu.aiplatform.assignments.model;

/**
 * Một trang đề đã chuẩn hóa chờ AI đọc (BR-U07-89…91): ảnh JPEG cạnh dài tối đa 2.000px, chữ gợi ý trích từ PDF có lớp
 * chữ nếu có.
 */
public record ImportPage(int pageNo, String mediaType, int width, int height, byte[] bytes, String textHint) {

    @Override
    public String toString() {
        return "ImportPage[pageNo=" + pageNo + ", " + width + "x" + height + ", bytes=" + bytes.length + "]";
    }
}
