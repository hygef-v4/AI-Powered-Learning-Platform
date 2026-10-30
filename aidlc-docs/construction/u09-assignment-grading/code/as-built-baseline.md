# U09 Assignment Grading — AS-BUILT 2026-10-30

Baseline `staging@794a405304bf02dd2593a9fa2c88662d7cd52845`; source-observed. Primary UC 42–45; FR-008, FR-009, FR-020, FR-024.

grading có submission review, AI proposal và structured feedback với evidence/recommendations, manual/finalize/publication/history/gradebook. Official publish là thao tác educator có scope và version; AI không tự công bố. Các reports structured feedback/result delivery/gradebook 25–26/10 là evidence lịch sử, không phải run mới ở HEAD.

## Source và negative-test inventory

- [backend/src/main/java/edu/aiplatform/grading/application/EvaluationLifecycleService.java](../../../../backend/src/main/java/edu/aiplatform/grading/application/EvaluationLifecycleService.java)
- [backend/src/main/java/edu/aiplatform/grading/application/GradebookService.java](../../../../backend/src/main/java/edu/aiplatform/grading/application/GradebookService.java)
- [backend/src/main/java/edu/aiplatform/grading/application/GradingBatchService.java](../../../../backend/src/main/java/edu/aiplatform/grading/application/GradingBatchService.java)
- [backend/src/main/java/edu/aiplatform/grading/application/GradingMcpBatchService.java](../../../../backend/src/main/java/edu/aiplatform/grading/application/GradingMcpBatchService.java)
- [backend/src/main/java/edu/aiplatform/grading/application/GradingMcpQueryService.java](../../../../backend/src/main/java/edu/aiplatform/grading/application/GradingMcpQueryService.java)
- [backend/src/main/java/edu/aiplatform/grading/application/GradingProposalService.java](../../../../backend/src/main/java/edu/aiplatform/grading/application/GradingProposalService.java)
- [backend/src/main/java/edu/aiplatform/grading/application/GradingService.java](../../../../backend/src/main/java/edu/aiplatform/grading/application/GradingService.java)
- [backend/src/main/java/edu/aiplatform/grading/application/SubmissionImageService.java](../../../../backend/src/main/java/edu/aiplatform/grading/application/SubmissionImageService.java)
- [backend/src/main/java/edu/aiplatform/grading/application/SubmissionReviewService.java](../../../../backend/src/main/java/edu/aiplatform/grading/application/SubmissionReviewService.java)
- [backend/src/main/java/edu/aiplatform/grading/web/EvaluationController.java](../../../../backend/src/main/java/edu/aiplatform/grading/web/EvaluationController.java)
- [backend/src/main/java/edu/aiplatform/grading/web/GradebookController.java](../../../../backend/src/main/java/edu/aiplatform/grading/web/GradebookController.java)
- [backend/src/main/java/edu/aiplatform/grading/web/GradingProposalController.java](../../../../backend/src/main/java/edu/aiplatform/grading/web/GradingProposalController.java)
- [backend/src/main/java/edu/aiplatform/grading/web/MyGradeController.java](../../../../backend/src/main/java/edu/aiplatform/grading/web/MyGradeController.java)
- [backend/src/main/java/edu/aiplatform/grading/web/SubmissionImageController.java](../../../../backend/src/main/java/edu/aiplatform/grading/web/SubmissionImageController.java)
- [backend/src/main/java/edu/aiplatform/grading/web/SubmissionReviewController.java](../../../../backend/src/main/java/edu/aiplatform/grading/web/SubmissionReviewController.java)

[Contracts/migrations/tests/UI đầy đủ](../../../inception/reverse-engineering/current-code-baseline.md); [Delta disposition](../../../inception/reverse-engineering/decision-register.md).

## Evidence và giới hạn

Không chạy lại application test/build trong đợt sửa tài liệu. Có source/test không đồng nghĩa test pass hoặc runtime/provider healthy. Negative cases cần kiểm trong test nguồn: actor sai role/scope, không có dữ liệu, stale version, unavailable provider; chi tiết thật theo assertions của từng file test.

### Ghi nhận kiểm thử trước baseline

- [aidlc-docs/construction/build-and-test/2026-10-25-u09-feedback-results.md](../../build-and-test/2026-10-25-u09-feedback-results.md)
- [aidlc-docs/construction/build-and-test/2026-10-25-u09-gradebook-class-layout-implementation-report.md](../../build-and-test/2026-10-25-u09-gradebook-class-layout-implementation-report.md)
- [aidlc-docs/construction/build-and-test/2026-10-25-u09-gradebook-class-layout-manual-test-guide.md](../../build-and-test/2026-10-25-u09-gradebook-class-layout-manual-test-guide.md)
- [aidlc-docs/construction/build-and-test/2026-10-25-u09-ui-bugfix-results.md](../../build-and-test/2026-10-25-u09-ui-bugfix-results.md)
- [aidlc-docs/construction/build-and-test/2026-10-25-u09-ui-polish-results.md](../../build-and-test/2026-10-25-u09-ui-polish-results.md)
- [aidlc-docs/construction/build-and-test/2026-10-26-u09-ai-result-delivery-report.md](../../build-and-test/2026-10-26-u09-ai-result-delivery-report.md)
- [aidlc-docs/construction/build-and-test/2026-10-26-u09-feedback-evidence-fix.md](../../build-and-test/2026-10-26-u09-feedback-evidence-fix.md)
- [aidlc-docs/construction/build-and-test/2026-10-26-u09-structured-feedback-manual-test-guide.md](../../build-and-test/2026-10-26-u09-structured-feedback-manual-test-guide.md)
- [aidlc-docs/construction/build-and-test/2026-10-26-u09-structured-feedback-report.md](../../build-and-test/2026-10-26-u09-structured-feedback-report.md)
