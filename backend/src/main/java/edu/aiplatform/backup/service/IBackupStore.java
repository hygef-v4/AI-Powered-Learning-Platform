package edu.aiplatform.backup.service;

import java.nio.file.Path;
import java.util.UUID;

/** Thư mục backup riêng trên Google Drive (BR-U03-94): base backup theo lần chạy và thư mục {@code wal}. */
public interface IBackupStore {

    record StoredArchive(String ref, long byteSize, String md5) {
    }

    boolean isAvailable();

    /** Tải archive lên thư mục của lần chạy; trả kích thước và MD5 phía lưu trữ để đối chiếu. */
    StoredArchive putArchive(UUID runId, Path archive);

    /** Xóa thư mục của lần chạy; không còn thì coi như đã xóa. */
    void deleteArchive(String runId);

    /** Tải một tệp WAL đã nén (và mã hóa nếu có khóa) lên thư mục {@code wal}, tên {@code name}. */
    StoredArchive putWal(String name, Path file);

    /** Xóa tệp WAL theo {@code ref} trả về lúc tải lên; không còn thì coi như đã xóa. */
    void deleteWal(String ref);
}
