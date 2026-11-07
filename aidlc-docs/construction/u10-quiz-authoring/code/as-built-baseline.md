# U10 Quiz Authoring — AS-BUILT source snapshot 2026-11-07

Source snapshot `f68a6ec5889074de80a1f1f0faf7bcb7e7054ec8`; current source paths are catalogued in [current-code-baseline](../../../inception/reverse-engineering/current-code-baseline.md). Dated deltas/test results below retain their original scope; this update is not runtime acceptance.

Quiz có DRAFT/OPEN/RETIRED (bỏ REVIEWED 2026-11-01). Quiz.edited giữ OPEN khi sửa quiz OPEN chưa có lượt làm; OPEN đã có lượt làm thì không sửa câu (409 QUIZ_HAS_ATTEMPTS, tạo version mới), thông tin chung vẫn sửa được; Retire mọi trạng thái chưa RETIRED, lý do trống mặc định Lưu trữ. Reopen RETIRED về DRAFT nếu publishedAt null, ngược lại OPEN; đã phát hành mà chuỗi version có bản khác đang OPEN thì 409 QUIZ_OTHER_VERSION_OPEN; điều kiện service vẫn áp dụng. Sinh đề theo tag: BR-U10-17.

## Source và negative-test inventory

| Source | Trách nhiệm |
|---|---|
| [quizzes/controller](../../../../backend/src/main/java/edu/aiplatform/quizzes/controller) | HTTP endpoints, request/response và lỗi HTTP. |
| [quizzes/service](../../../../backend/src/main/java/edu/aiplatform/quizzes/service) | Use case, phân quyền, transaction/audit và contract implementation. |
| [quizzes/repository](../../../../backend/src/main/java/edu/aiplatform/quizzes/repository) | Repository interface/implementation, Spring JDBC/SQL và persistence mapping. |
| [quizzes/model](../../../../backend/src/main/java/edu/aiplatform/quizzes/model) | Đối tượng, enum và quy tắc nghiệp vụ. |
| [quizzes/contract](../../../../backend/src/main/java/edu/aiplatform/quizzes/contract) | Public interface/DTO xuyên unit; giữ nguyên. |

| Feature | API | Model | Pages | Components | Hooks | Styles | Tests |
|---|---|---|---|---|---|---|---|
| [quizzes](../../../../frontend/src/features/quizzes) | [api](../../../../frontend/src/features/quizzes/api) | [model](../../../../frontend/src/features/quizzes/model) | [pages](../../../../frontend/src/features/quizzes/pages) | [components](../../../../frontend/src/features/quizzes/components) | — | — | [tests](../../../../frontend/tests/features/quizzes) |

Tests: [quizzes](../../../../backend/src/test/java/edu/aiplatform/quizzes).

[Controller, migration and individual test inventory](../../../inception/reverse-engineering/current-code-baseline.md).

## Evidence và giới hạn

Kiểm tra của đợt refactor được ghi trong [refactor results](../../build-and-test/2026-10-30-package-moves-only-results.md). Những báo cáo có ngày trước đó là bằng chứng lịch sử. Có source/test không đồng nghĩa test pass hoặc runtime/provider healthy. Negative cases cần kiểm trong test nguồn: actor sai role/scope, không có dữ liệu, stale version, unavailable provider; chi tiết thật theo assertions của từng file test.

## Delta 2026-11-01

Source trên nhánh `feat/quiz-rework-2026-11-01`: [roadmap](../../plans/code-generation-plan/2026-11-01-quiz-rework.md), [kết quả và giới hạn](../../build-and-test/2026-11-01-quiz-rework-results.md). Migration `quizzes/V20261101_1200__quiz_rework.sql`.
