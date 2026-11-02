package edu.aiplatform.backup.application;

import java.nio.file.Path;
import java.util.UUID;

/** Thư mục backup riêng (BR-U03-94): Drive hoặc thư mục local khi chạy không có Drive. */
public interface IBackupStore {

    record StoredArchive(String ref, long byteSize, String md5) {
    }

    boolean available();

    /** Tải archive lên thư mục của lần chạy; trả kích thước và MD5 phía lưu trữ để đối chiếu. */
    StoredArchive put(UUID runId, Path archive);

    /** Xóa thư mục của lần chạy; không còn thì coi như đã xóa. */
    void delete(String runId);
}
