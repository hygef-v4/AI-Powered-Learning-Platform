package edu.aiplatform.assignmentwork.contract;

import java.util.UUID;

/** U08 khai báo, U09 cài: tạo đánh giá chờ chấm trong cùng transaction nộp bài {@code GRADED}. */
public interface ISubmissionListener {

    void onSubmitted(UUID attemptId);
}
