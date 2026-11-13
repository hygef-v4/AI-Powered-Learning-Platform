# U01 Account & Access — AS-BUILT source snapshot 2026-11-07

Source snapshot `f68a6ec5889074de80a1f1f0faf7bcb7e7054ec8`; current source paths are catalogued in [current-code-baseline](../../../inception/reverse-engineering/current-code-baseline.md). Dated deltas/test results below retain their original scope; this update is not runtime acceptance.

Tài khoản trường cấp, OTP/kích hoạt/khôi phục, JWT/session, hồ sơ và Admin CRUD/status có controller/service. Quyền current actor/session không thay bằng hard-code UI; xem identity contract và tests. Mail transport SMTP hoặc Brevo HTTPS API thực tế vẫn cần cấu hình, không kết luận email đã đến inbox.

## Source và negative-test inventory

| Source | Trách nhiệm |
|---|---|
| [identity/controller](../../../../backend/src/main/java/edu/aiplatform/identity/controller) | HTTP endpoints, request/response và lỗi HTTP. |
| [identity/service](../../../../backend/src/main/java/edu/aiplatform/identity/service) | Use case, phân quyền, transaction/audit và contract implementation. |
| [identity/repository](../../../../backend/src/main/java/edu/aiplatform/identity/repository) | Repository interface/implementation, Spring JDBC/SQL và persistence mapping. |
| [identity/model](../../../../backend/src/main/java/edu/aiplatform/identity/model) | Đối tượng, enum và quy tắc nghiệp vụ. |
| [identity/integration](../../../../backend/src/main/java/edu/aiplatform/identity/integration) | Adapter hạ tầng/provider, listener và sender. |
| [identity/config](../../../../backend/src/main/java/edu/aiplatform/identity/config) | Properties, cấu hình bean và topology. |
| [identity/contract](../../../../backend/src/main/java/edu/aiplatform/identity/contract) | Public interface/DTO xuyên unit; giữ nguyên. |

| Feature | API | Model | Pages | Components | Hooks | Styles | Tests |
|---|---|---|---|---|---|---|---|
| [identity](../../../../frontend/src/features/identity) | [api](../../../../frontend/src/features/identity/api) | [model](../../../../frontend/src/features/identity/model) | [pages](../../../../frontend/src/features/identity/pages) | [components](../../../../frontend/src/features/identity/components) | [hooks](../../../../frontend/src/features/identity/hooks) | — | [tests](../../../../frontend/tests/features/identity) |

Tests: [identity](../../../../backend/src/test/java/edu/aiplatform/identity).

[Controller, migration and individual test inventory](../../../inception/reverse-engineering/current-code-baseline.md).

## Evidence và giới hạn

Kiểm tra của đợt refactor được ghi trong [refactor results](../../build-and-test/2026-10-30-package-moves-only-results.md). Những báo cáo có ngày trước đó là bằng chứng lịch sử. Có source/test không đồng nghĩa test pass hoặc runtime/provider healthy. Negative cases cần kiểm trong test nguồn: actor sai role/scope, không có dữ liệu, stale version, unavailable provider; chi tiết thật theo assertions của từng file test.

## Delta working tree — 2026-11-09

UI Admin nhập Excel .xlsx và tải account-import.xlsx; backend đọc sheet đầu, giữ validate/commit và CSV API cũ. Giới hạn 1 MB/1.000 dòng, không nhận công thức hoặc macro. Không đổi rule tạo tài khoản/phân quyền.

Delta này bổ sung snapshot commit có ngày phía trên, chưa tạo commit mới và không sửa lại evidence lịch sử. [Kết quả và giới hạn](../../build-and-test/2026-11-09-excel-import-results.md); [đối chiếu tài liệu](../../build-and-test/2026-11-09-aidlc-doc-sync.md).
