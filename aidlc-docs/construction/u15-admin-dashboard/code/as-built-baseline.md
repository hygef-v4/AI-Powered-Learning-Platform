# U15 Admin Dashboard — AS-BUILT 2026-10-30

Current structure: parent HEAD `5a76e428` + working-tree refactor; unit ownership and public contracts unchanged.

Admin Dashboard có counts, backup metadata/history và GET /activity. Activity gồm biến động ACTIVE accounts hôm nay, số Assignment 7 ngày (individual/group/practice), tối đa 50 security events trong 30 ngày; ngày Asia/Ho_Chi_Minh. Các nguồn lỗi/timeout trả từng Section UNAVAILABLE/data null; no-store, chỉ ADMIN, query lạ bị từ chối. Không lộ archive/secrets hoặc cấp quyền học vụ.

## Source và negative-test inventory

| Source | Trách nhiệm |
|---|---|
| [admindashboard/controller](../../../../backend/src/main/java/edu/aiplatform/admindashboard/controller) | HTTP endpoints, request/response và lỗi HTTP. |
| [admindashboard/service](../../../../backend/src/main/java/edu/aiplatform/admindashboard/service) | Use case, phân quyền, transaction/audit và contract implementation. |
| [admindashboard/dto](../../../../backend/src/main/java/edu/aiplatform/admindashboard/dto) | Kiểu dữ liệu request/response riêng. |
| [admindashboard/integration](../../../../backend/src/main/java/edu/aiplatform/admindashboard/integration) | Adapter hạ tầng/provider, listener và sender. |

| Feature | API | Model | Pages | Components | Hooks | Styles | Tests |
|---|---|---|---|---|---|---|---|
| [admindashboard](../../../../frontend/src/features/admindashboard) | [api](../../../../frontend/src/features/admindashboard/api) | [model](../../../../frontend/src/features/admindashboard/model) | [pages](../../../../frontend/src/features/admindashboard/pages) | [components](../../../../frontend/src/features/admindashboard/components) | [hooks](../../../../frontend/src/features/admindashboard/hooks) | — | [tests](../../../../frontend/tests/features/admindashboard) |

Tests: [admindashboard](../../../../backend/src/test/java/edu/aiplatform/admindashboard).

[Controller, migration and individual test inventory](../../../inception/reverse-engineering/current-code-baseline.md).

## Evidence và giới hạn

Kiểm tra của đợt refactor được ghi trong [refactor results](../../build-and-test/2026-10-30-package-moves-only-results.md). Những báo cáo có ngày trước đó là bằng chứng lịch sử. Có source/test không đồng nghĩa test pass hoặc runtime/provider healthy. Negative cases cần kiểm trong test nguồn: actor sai role/scope, không có dữ liệu, stale version, unavailable provider; chi tiết thật theo assertions của từng file test.

### Ghi nhận kiểm thử trước baseline

- [aidlc-docs/construction/build-and-test/2026-10-25-u05-u15-audit-report.md](../../build-and-test/2026-10-25-u05-u15-audit-report.md)
- [aidlc-docs/construction/build-and-test/2026-10-25-u05-u15-manual-test-guide.md](../../build-and-test/2026-10-25-u05-u15-manual-test-guide.md)
- [aidlc-docs/construction/build-and-test/2026-10-26-u05-u15-followup-report.md](../../build-and-test/2026-10-26-u05-u15-followup-report.md)
- [aidlc-docs/construction/build-and-test/u15-verification.md](../../build-and-test/u15-verification.md)
