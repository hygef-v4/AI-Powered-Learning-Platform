# U09 Assignment Grading — AS-BUILT 2026-10-30

Current structure: parent HEAD `5a76e428` + working-tree refactor; unit ownership and public contracts unchanged.

grading có submission review, AI proposal và structured feedback với evidence/recommendations, manual/finalize/publication/history/gradebook. Official publish là thao tác educator có scope và version; AI không tự công bố. Các reports structured feedback/result delivery/gradebook 25–26/10 là evidence lịch sử, không phải run mới ở HEAD.

## Source và negative-test inventory

| Source | Trách nhiệm |
|---|---|
| [grading/controller](../../../../backend/src/main/java/edu/aiplatform/grading/controller) | HTTP endpoints, request/response và lỗi HTTP. |
| [grading/service](../../../../backend/src/main/java/edu/aiplatform/grading/service) | Use case, phân quyền, transaction/audit và contract implementation. |
| [grading/repository](../../../../backend/src/main/java/edu/aiplatform/grading/repository) | Repository interface/implementation, SQL/JPA và persistence mapping. |
| [grading/model](../../../../backend/src/main/java/edu/aiplatform/grading/model) | Đối tượng, enum và quy tắc nghiệp vụ. |
| [grading/dto](../../../../backend/src/main/java/edu/aiplatform/grading/dto) | Kiểu dữ liệu request/response riêng. |
| [grading/contract](../../../../backend/src/main/java/edu/aiplatform/grading/contract) | Public interface/DTO xuyên unit; giữ nguyên. |

| Feature | API | Model | Pages | Components | Hooks |
|---|---|---|---|---|---|
| [grading](../../../../frontend/src/features/grading) | [api](../../../../frontend/src/features/grading/api) | [model](../../../../frontend/src/features/grading/model) | [pages](../../../../frontend/src/features/grading/pages) | [components](../../../../frontend/src/features/grading/components) | [hooks](../../../../frontend/src/features/grading/hooks) |

Tests: [grading](../../../../backend/src/test/java/edu/aiplatform/grading).

[Controller, migration and individual test inventory](../../../inception/reverse-engineering/current-code-baseline.md).

## Evidence và giới hạn

Kiểm tra của đợt refactor được ghi trong [refactor results](../../build-and-test/2026-10-30-package-moves-only-results.md). Những báo cáo có ngày trước đó là bằng chứng lịch sử. Có source/test không đồng nghĩa test pass hoặc runtime/provider healthy. Negative cases cần kiểm trong test nguồn: actor sai role/scope, không có dữ liệu, stale version, unavailable provider; chi tiết thật theo assertions của từng file test.

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
