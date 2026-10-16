package edu.aiplatform.files.contract;

import java.io.InputStream;

/** Tệp cần lưu; loại tệp được xác định lại từ nội dung, không tin tên hay header. */
public record FileUpload(String originalFileName, long byteSize, InputStream content) {
}
