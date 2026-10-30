package edu.aiplatform.backup.service;

import edu.aiplatform.files.service.IDriveCleanupTarget;
import org.springframework.stereotype.Component;

/** {@code DRIVE_CLEANUP} với {@code storage = BACKUP}: xóa thư mục của lần backup lỗi (B4). */
@Component
public class BackupCleanupTarget implements IDriveCleanupTarget {

    public static final String AREA = "BACKUP";

    private final IBackupStore store;

    public BackupCleanupTarget(IBackupStore store) {
        this.store = store;
    }

    @Override
    public String area() {
        return AREA;
    }

    @Override
    public void delete(String ref) {
        store.delete(ref);
    }
}
