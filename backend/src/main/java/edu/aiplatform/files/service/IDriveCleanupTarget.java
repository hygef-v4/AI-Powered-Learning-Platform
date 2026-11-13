package edu.aiplatform.files.service;

/** Nơi khác trong U03 cần dọn trên Drive (archive backup dở dang). */
public interface IDriveCleanupTarget {

    String area();

    void delete(String ref);
}
