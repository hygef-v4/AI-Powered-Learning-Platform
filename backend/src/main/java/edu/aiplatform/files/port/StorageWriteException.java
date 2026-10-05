package edu.aiplatform.files.port;

/** Tệp đã được tạo trên kho nhưng ghi chưa trọn; `orphanFileId` cần được dọn (BR-U03-06). */
public class StorageWriteException extends StorageUnavailableException {

    private final String orphanFileId;

    public StorageWriteException(String message, String orphanFileId, Throwable cause) {
        super(message, cause);
        this.orphanFileId = orphanFileId;
    }

    public String orphanFileId() {
        return orphanFileId;
    }
}
