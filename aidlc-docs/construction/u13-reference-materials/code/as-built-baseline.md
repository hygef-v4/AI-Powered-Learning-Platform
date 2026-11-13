# U13 Reference Materials — AS-BUILT source snapshot 2026-11-07

Source snapshot `f68a6ec5889074de80a1f1f0faf7bcb7e7054ec8`; current source paths are catalogued in [current-code-baseline](../../../inception/reverse-engineering/current-code-baseline.md). Dated deltas/test results below retain their original scope; this update is not runtime acceptance.

Module cơ bản của môn đã triển khai. Học liệu môn/lớp bắt buộc moduleId cùng môn; dữ liệu cũ backfill Module 1. Manager quản lý module; class viewers đọc theo scope. Quiz nằm tối đa một module, attach khác module chuyển link; phải kiểm quyền với Quiz. Delete chặn active materials; archived materials chuyển sang module khác, không xóa module cuối nếu cần giữ chúng. Version/reorder kiểm server; module không đồng nghĩa Lesson/RAG.

## Source và negative-test inventory

| Source | Trách nhiệm |
|---|---|
| [materials/controller](../../../../backend/src/main/java/edu/aiplatform/materials/controller) | HTTP endpoints, request/response và lỗi HTTP. |
| [materials/service](../../../../backend/src/main/java/edu/aiplatform/materials/service) | Use case, phân quyền, transaction/audit và contract implementation. |
| [materials/repository](../../../../backend/src/main/java/edu/aiplatform/materials/repository) | Repository interface/implementation, SQL/JPA và persistence mapping. |
| [materials/model](../../../../backend/src/main/java/edu/aiplatform/materials/model) | Đối tượng, enum và quy tắc nghiệp vụ. |
| [materials/integration](../../../../backend/src/main/java/edu/aiplatform/materials/integration) | Adapter hạ tầng/provider, listener và sender. |

| Feature | API | Model | Pages | Components | Hooks | Styles | Tests |
|---|---|---|---|---|---|---|---|
| [materials](../../../../frontend/src/features/materials) | [api](../../../../frontend/src/features/materials/api) | [model](../../../../frontend/src/features/materials/model) | [pages](../../../../frontend/src/features/materials/pages) | [components](../../../../frontend/src/features/materials/components) | [hooks](../../../../frontend/src/features/materials/hooks) | [styles](../../../../frontend/src/features/materials/styles) | [tests](../../../../frontend/tests/features/materials) |

Tests: [materials](../../../../backend/src/test/java/edu/aiplatform/materials).

[Controller, migration and individual test inventory](../../../inception/reverse-engineering/current-code-baseline.md).

## Evidence và giới hạn

Kiểm tra của đợt refactor được ghi trong [refactor results](../../build-and-test/2026-10-30-package-moves-only-results.md). Những báo cáo có ngày trước đó là bằng chứng lịch sử. Có source/test không đồng nghĩa test pass hoặc runtime/provider healthy. Negative cases cần kiểm trong test nguồn: actor sai role/scope, không có dữ liệu, stale version, unavailable provider; chi tiết thật theo assertions của từng file test.

### Ghi nhận kiểm thử trước baseline

- [aidlc-docs/construction/build-and-test/u13-verification.md](../../build-and-test/u13-verification.md)

## Delta working tree — 2026-11-09

Thêm enabled độc lập status, descriptionDocument và kiểm lại quyền đổi token tải; sinh viên/nguồn AI bị chặn khi tắt. Material/version adapter đã JDBC, module adapter còn JPA. Hai row JPA của material/version đã xóa. UI nhãn Bật/Tắt, rich description và sửa gutter bullet/số.

Delta này bổ sung snapshot commit có ngày phía trên, chưa tạo commit mới và không sửa lại evidence lịch sử. [Kết quả và giới hạn](../../build-and-test/2026-11-09-material-quiz-ui-results.md); [đối chiếu tài liệu](../../build-and-test/2026-11-09-aidlc-doc-sync.md).
