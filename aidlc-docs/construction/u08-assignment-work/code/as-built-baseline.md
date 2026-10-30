# U08 Assignment Work — AS-BUILT 2026-10-30

Baseline `staging@794a405304bf02dd2593a9fa2c88662d7cd52845`; source-observed. Primary UC 37–41; FR-018, FR-030, FR-039.

assignmentwork có start/save/submit/history, late/auto-submit, practice feedback và activity. ActivityService.RETENTION = 1 ngày; scanner xóa theo hằng này, không Settings activity.retentionDays 1–365. Theo dõi hoạt động không tự trừ điểm. Feedback Practice gắn submission/version và không vào gradebook.

## Source và negative-test inventory

- [backend/src/main/java/edu/aiplatform/assignmentwork/application/ActivityService.java](../../../../backend/src/main/java/edu/aiplatform/assignmentwork/application/ActivityService.java)
- [backend/src/main/java/edu/aiplatform/assignmentwork/application/AttemptService.java](../../../../backend/src/main/java/edu/aiplatform/assignmentwork/application/AttemptService.java)
- [backend/src/main/java/edu/aiplatform/assignmentwork/application/PracticeFeedbackService.java](../../../../backend/src/main/java/edu/aiplatform/assignmentwork/application/PracticeFeedbackService.java)
- [backend/src/main/java/edu/aiplatform/assignmentwork/application/StudentAssignmentService.java](../../../../backend/src/main/java/edu/aiplatform/assignmentwork/application/StudentAssignmentService.java)
- [backend/src/main/java/edu/aiplatform/assignmentwork/application/SubmissionQueryService.java](../../../../backend/src/main/java/edu/aiplatform/assignmentwork/application/SubmissionQueryService.java)
- [backend/src/main/java/edu/aiplatform/assignmentwork/web/AttemptController.java](../../../../backend/src/main/java/edu/aiplatform/assignmentwork/web/AttemptController.java)
- [backend/src/main/java/edu/aiplatform/assignmentwork/web/StudentAssignmentController.java](../../../../backend/src/main/java/edu/aiplatform/assignmentwork/web/StudentAssignmentController.java)

[Contracts/migrations/tests/UI đầy đủ](../../../inception/reverse-engineering/current-code-baseline.md); [Delta disposition](../../../inception/reverse-engineering/decision-register.md).

## Evidence và giới hạn

Không chạy lại application test/build trong đợt sửa tài liệu. Có source/test không đồng nghĩa test pass hoặc runtime/provider healthy. Negative cases cần kiểm trong test nguồn: actor sai role/scope, không có dữ liệu, stale version, unavailable provider; chi tiết thật theo assertions của từng file test.
