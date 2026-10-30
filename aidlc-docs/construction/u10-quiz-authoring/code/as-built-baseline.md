# U10 Quiz Authoring — AS-BUILT 2026-10-30

Current structure: parent HEAD `5a76e428` + working-tree refactor; unit ownership and public contracts unchanged.

Quiz còn DRAFT/REVIEWED/OPEN/RETIRED. Quiz.edited giữ OPEN khi sửa quiz OPEN; sửa REVIEWED về DRAFT. Retire mọi trạng thái chưa RETIRED, lý do trống mặc định Lưu trữ. Reopen RETIRED về DRAFT nếu publishedAt null, ngược lại OPEN; điều kiện service vẫn áp dụng. Không suy ra bỏ review hoặc sinh đề theo tag từ góp ý chưa triển khai.

## Source và negative-test inventory

| Source | Trách nhiệm |
|---|---|
| [quizzes/controller](../../../../backend/src/main/java/edu/aiplatform/quizzes/controller) | HTTP endpoints, request/response và lỗi HTTP. |
| [quizzes/service](../../../../backend/src/main/java/edu/aiplatform/quizzes/service) | Use case, phân quyền, transaction/audit và contract implementation. |
| [quizzes/repository](../../../../backend/src/main/java/edu/aiplatform/quizzes/repository) | Repository interface/implementation, SQL/JPA và persistence mapping. |
| [quizzes/model](../../../../backend/src/main/java/edu/aiplatform/quizzes/model) | Đối tượng, enum và quy tắc nghiệp vụ. |
| [quizzes/contract](../../../../backend/src/main/java/edu/aiplatform/quizzes/contract) | Public interface/DTO xuyên unit; giữ nguyên. |

| Feature | API | Model | Pages | Components | Hooks | Styles | Tests |
|---|---|---|---|---|---|---|---|
| [quizzes](../../../../frontend/src/features/quizzes) | [api](../../../../frontend/src/features/quizzes/api) | [model](../../../../frontend/src/features/quizzes/model) | [pages](../../../../frontend/src/features/quizzes/pages) | [components](../../../../frontend/src/features/quizzes/components) | — | — | [tests](../../../../frontend/tests/features/quizzes) |

Tests: [quizzes](../../../../backend/src/test/java/edu/aiplatform/quizzes).

[Controller, migration and individual test inventory](../../../inception/reverse-engineering/current-code-baseline.md).

## Evidence và giới hạn

Kiểm tra của đợt refactor được ghi trong [refactor results](../../build-and-test/2026-10-30-package-moves-only-results.md). Những báo cáo có ngày trước đó là bằng chứng lịch sử. Có source/test không đồng nghĩa test pass hoặc runtime/provider healthy. Negative cases cần kiểm trong test nguồn: actor sai role/scope, không có dữ liệu, stale version, unavailable provider; chi tiết thật theo assertions của từng file test.
