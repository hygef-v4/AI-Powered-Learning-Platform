# U11 Quiz Practice — AS-BUILT 2026-10-30

Current structure: parent HEAD `5a76e428` + working-tree refactor; unit ownership and public contracts unchanged.

quizpractice lưu attempt/snapshot/answers/result/activity; frontend QuizTakingPage/QuizResultPage. Quiz Practice không vào gradebook Assignment. Tác động retire/reopen phải đối chiếu publication callback và snapshot; source của state change không chứng minh mọi E2E case đã pass.

## Source và negative-test inventory

| Source | Trách nhiệm |
|---|---|
| [quizpractice/controller](../../../../backend/src/main/java/edu/aiplatform/quizpractice/controller) | HTTP endpoints, request/response và lỗi HTTP. |
| [quizpractice/service](../../../../backend/src/main/java/edu/aiplatform/quizpractice/service) | Use case, phân quyền, transaction/audit và contract implementation. |
| [quizpractice/repository](../../../../backend/src/main/java/edu/aiplatform/quizpractice/repository) | Repository interface/implementation, SQL/JPA và persistence mapping. |
| [quizpractice/model](../../../../backend/src/main/java/edu/aiplatform/quizpractice/model) | Đối tượng, enum và quy tắc nghiệp vụ. |
| [quizpractice/dto](../../../../backend/src/main/java/edu/aiplatform/quizpractice/dto) | Kiểu dữ liệu request/response riêng. |

| Feature | API | Model | Pages | Components | Hooks | Styles | Tests |
|---|---|---|---|---|---|---|---|
| [quizpractice](../../../../frontend/src/features/quizpractice) | [api](../../../../frontend/src/features/quizpractice/api) | [model](../../../../frontend/src/features/quizpractice/model) | [pages](../../../../frontend/src/features/quizpractice/pages) | [components](../../../../frontend/src/features/quizpractice/components) | — | — | [tests](../../../../frontend/tests/features/quizpractice) |

Tests: [quizpractice](../../../../backend/src/test/java/edu/aiplatform/quizpractice).

[Controller, migration and individual test inventory](../../../inception/reverse-engineering/current-code-baseline.md).

## Evidence và giới hạn

Kiểm tra của đợt refactor được ghi trong [refactor results](../../build-and-test/2026-10-30-package-moves-only-results.md). Những báo cáo có ngày trước đó là bằng chứng lịch sử. Có source/test không đồng nghĩa test pass hoặc runtime/provider healthy. Negative cases cần kiểm trong test nguồn: actor sai role/scope, không có dữ liệu, stale version, unavailable provider; chi tiết thật theo assertions của từng file test.
