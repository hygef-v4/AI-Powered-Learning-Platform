package edu.aiplatform.backup.service;

import java.nio.file.Path;

/** Chụp base backup vật lý ra một tệp tar nén; tách ra để test không cần PostgreSQL. */
public interface IBaseBackupRunner {

    void runBaseBackup(Path target);
}
