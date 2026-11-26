package edu.aiplatform.backup.service;

import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Public key mã hóa backup là tùy chọn (BR-U03-93): bỏ trống thì base backup và WAL lưu không mã hóa. */
public final class BackupKeys {

    private static final Logger log = LoggerFactory.getLogger(BackupKeys.class);
    /** Đẩy WAL chạy mỗi phút nên chỉ cảnh báo thiếu khóa một lần mỗi lần khởi động. */
    private static final AtomicBoolean WARNED = new AtomicBoolean();

    private BackupKeys() {
    }

    /** Trả {@code null} khi chưa đặt khóa; khóa sai định dạng thì báo lỗi vĩnh viễn. */
    public static PublicKey loadOptionalPublicKey(String pemOrBase64) {
        if (pemOrBase64 == null || pemOrBase64.isBlank()) {
            if (WARNED.compareAndSet(false, true)) {
                log.warn("Chưa đặt U03_BACKUP_PUBLIC_KEY: base backup và WAL sẽ không được mã hóa");
            }
            return null;
        }
        try {
            return BackupEncryptor.parsePublicKey(pemOrBase64);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new BackupFailure("U03_BACKUP_PUBLIC_KEY không hợp lệ");
        }
    }
}
