package edu.aiplatform.backup.application;

import java.nio.file.Path;

/** Chạy {@code pg_dump} ra tệp; tách ra để test không cần PostgreSQL. */
public interface IDumpRunner {

    void dump(Path target);
}
