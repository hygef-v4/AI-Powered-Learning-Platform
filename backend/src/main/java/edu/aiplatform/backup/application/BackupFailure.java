package edu.aiplatform.backup.application;

/** Lỗi vĩnh viễn của lần backup (thiếu dung lượng, thiếu khóa): không thử lại, chuyển {@code FAILED} ngay. */
public class BackupFailure extends RuntimeException {

    public BackupFailure(String message) {
        super(message);
    }
}
