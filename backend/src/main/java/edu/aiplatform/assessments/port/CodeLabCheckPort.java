package edu.aiplatform.assessments.port;

import java.util.List;
import java.util.UUID;

/** U08 khai báo, U13 cài (`C`): lời giải mẫu đạt mọi test với `contentHash` hiện tại (BR-U13-33). */
public interface CodeLabCheckPort {
    List<ReviewIssue> check(UUID assignmentId);
}
