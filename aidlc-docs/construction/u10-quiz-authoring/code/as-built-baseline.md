# U10 Quiz Authoring — AS-BUILT 2026-10-30

Baseline `staging@794a405304bf02dd2593a9fa2c88662d7cd52845`; source-observed. Primary UC 46–48; FR-006, FR-007, FR-016, FR-017, FR-028.

Quiz còn DRAFT/REVIEWED/OPEN/RETIRED. Quiz.edited giữ OPEN khi sửa quiz OPEN; sửa REVIEWED về DRAFT. Retire mọi trạng thái chưa RETIRED, lý do trống mặc định Lưu trữ. Reopen RETIRED về DRAFT nếu publishedAt null, ngược lại OPEN; điều kiện service vẫn áp dụng. Không suy ra bỏ review hoặc sinh đề theo tag từ góp ý chưa triển khai.

## Source và negative-test inventory

- [backend/src/main/java/edu/aiplatform/quizzes/application/QuestionBankService.java](../../../../backend/src/main/java/edu/aiplatform/quizzes/application/QuestionBankService.java)
- [backend/src/main/java/edu/aiplatform/quizzes/application/QuizAiDraftService.java](../../../../backend/src/main/java/edu/aiplatform/quizzes/application/QuizAiDraftService.java)
- [backend/src/main/java/edu/aiplatform/quizzes/application/QuizAuthoringService.java](../../../../backend/src/main/java/edu/aiplatform/quizzes/application/QuizAuthoringService.java)
- [backend/src/main/java/edu/aiplatform/quizzes/application/QuizCopyService.java](../../../../backend/src/main/java/edu/aiplatform/quizzes/application/QuizCopyService.java)
- [backend/src/main/java/edu/aiplatform/quizzes/application/QuizMcpAuthoringService.java](../../../../backend/src/main/java/edu/aiplatform/quizzes/application/QuizMcpAuthoringService.java)
- [backend/src/main/java/edu/aiplatform/quizzes/application/QuizPublicationService.java](../../../../backend/src/main/java/edu/aiplatform/quizzes/application/QuizPublicationService.java)
- [backend/src/main/java/edu/aiplatform/quizzes/application/QuizQueryService.java](../../../../backend/src/main/java/edu/aiplatform/quizzes/application/QuizQueryService.java)
- [backend/src/main/java/edu/aiplatform/quizzes/web/ClassQuizController.java](../../../../backend/src/main/java/edu/aiplatform/quizzes/web/ClassQuizController.java)
- [backend/src/main/java/edu/aiplatform/quizzes/web/QuestionController.java](../../../../backend/src/main/java/edu/aiplatform/quizzes/web/QuestionController.java)
- [backend/src/main/java/edu/aiplatform/quizzes/web/QuizController.java](../../../../backend/src/main/java/edu/aiplatform/quizzes/web/QuizController.java)
- [backend/src/main/java/edu/aiplatform/quizzes/web/SubjectQuizController.java](../../../../backend/src/main/java/edu/aiplatform/quizzes/web/SubjectQuizController.java)

[Contracts/migrations/tests/UI đầy đủ](../../../inception/reverse-engineering/current-code-baseline.md); [Delta disposition](../../../inception/reverse-engineering/decision-register.md).

## Evidence và giới hạn

Không chạy lại application test/build trong đợt sửa tài liệu. Có source/test không đồng nghĩa test pass hoặc runtime/provider healthy. Negative cases cần kiểm trong test nguồn: actor sai role/scope, không có dữ liệu, stale version, unavailable provider; chi tiết thật theo assertions của từng file test.
