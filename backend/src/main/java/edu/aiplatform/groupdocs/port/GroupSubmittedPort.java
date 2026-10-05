package edu.aiplatform.groupdocs.port;

import java.util.List;
import java.util.UUID;

/** U14 khai báo, U15 cài (`C`): gọi trong transaction nộp bài nhóm. */
public interface GroupSubmittedPort {
    void onGroupSubmitted(UUID groupDocumentId, List<UUID> memberIds);
}
