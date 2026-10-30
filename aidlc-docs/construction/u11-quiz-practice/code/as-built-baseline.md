# U11 Quiz Practice — AS-BUILT 2026-10-30

Baseline `staging@794a405304bf02dd2593a9fa2c88662d7cd52845`; source-observed. Primary UC 49–52; FR-017, FR-018, FR-039.

quizpractice lưu attempt/snapshot/answers/result/activity; frontend QuizTakingPage/QuizResultPage. Quiz Practice không vào gradebook Assignment. Tác động retire/reopen phải đối chiếu publication callback và snapshot; source của state change không chứng minh mọi E2E case đã pass.

## Source và negative-test inventory

- [backend/src/main/java/edu/aiplatform/quizpractice/application/QuizActivityService.java](../../../../backend/src/main/java/edu/aiplatform/quizpractice/application/QuizActivityService.java)
- [backend/src/main/java/edu/aiplatform/quizpractice/application/QuizAttemptService.java](../../../../backend/src/main/java/edu/aiplatform/quizpractice/application/QuizAttemptService.java)
- [backend/src/main/java/edu/aiplatform/quizpractice/application/QuizPracticeQueryService.java](../../../../backend/src/main/java/edu/aiplatform/quizpractice/application/QuizPracticeQueryService.java)
- [backend/src/main/java/edu/aiplatform/quizpractice/application/TeacherQuizAttemptService.java](../../../../backend/src/main/java/edu/aiplatform/quizpractice/application/TeacherQuizAttemptService.java)
- [backend/src/main/java/edu/aiplatform/quizpractice/web/QuizPracticeController.java](../../../../backend/src/main/java/edu/aiplatform/quizpractice/web/QuizPracticeController.java)
- [backend/src/main/java/edu/aiplatform/quizpractice/web/TeacherQuizAttemptController.java](../../../../backend/src/main/java/edu/aiplatform/quizpractice/web/TeacherQuizAttemptController.java)

[Contracts/migrations/tests/UI đầy đủ](../../../inception/reverse-engineering/current-code-baseline.md); [Delta disposition](../../../inception/reverse-engineering/decision-register.md).

## Evidence và giới hạn

Không chạy lại application test/build trong đợt sửa tài liệu. Có source/test không đồng nghĩa test pass hoặc runtime/provider healthy. Negative cases cần kiểm trong test nguồn: actor sai role/scope, không có dữ liệu, stale version, unavailable provider; chi tiết thật theo assertions của từng file test.
