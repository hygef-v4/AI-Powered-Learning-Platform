package edu.aiplatform.backup.service;

import java.nio.file.Path;

/** Chạy {@code pg_dump} ra tệp; tách ra để test không cần PostgreSQL. */
public interface IDumpRunner {

    void dump(Path target);
}
