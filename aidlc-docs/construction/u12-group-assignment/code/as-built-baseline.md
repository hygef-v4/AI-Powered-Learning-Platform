# U12 Group Assignment — AS-BUILT 2026-10-30

Current structure: parent HEAD `5a76e428` + working-tree refactor; unit ownership and public contracts unchanged.

groups và groupdocs thuộc cùng U12. Leader request reason tùy chọn, null thành chuỗi rỗng, tối đa 1.000; reject reason tùy chọn, trống thành null. GroupRenameService cho đổi tên theo scope; editor hiển thị lỗi nhóm đã bị gỡ khỏi draft. Claims/locks/leader submit giữ theo SectionRules và services; không tự thêm peer assessment hay đổi AI Student GROUP.

## Source và negative-test inventory

| Source | Trách nhiệm |
|---|---|
| [groups/controller](../../../../backend/src/main/java/edu/aiplatform/groups/controller) | HTTP endpoints, request/response và lỗi HTTP. |
| [groups/service](../../../../backend/src/main/java/edu/aiplatform/groups/service) | Use case, phân quyền, transaction/audit và contract implementation. |
| [groups/repository](../../../../backend/src/main/java/edu/aiplatform/groups/repository) | Repository interface/implementation, SQL/JPA và persistence mapping. |
| [groups/model](../../../../backend/src/main/java/edu/aiplatform/groups/model) | Đối tượng, enum và quy tắc nghiệp vụ. |
| [groupdocs/controller](../../../../backend/src/main/java/edu/aiplatform/groupdocs/controller) | HTTP endpoints, request/response và lỗi HTTP. |
| [groupdocs/service](../../../../backend/src/main/java/edu/aiplatform/groupdocs/service) | Use case, phân quyền, transaction/audit và contract implementation. |
| [groupdocs/repository](../../../../backend/src/main/java/edu/aiplatform/groupdocs/repository) | Repository interface/implementation, SQL/JPA và persistence mapping. |
| [groupdocs/model](../../../../backend/src/main/java/edu/aiplatform/groupdocs/model) | Đối tượng, enum và quy tắc nghiệp vụ. |
| [groupdocs/integration](../../../../backend/src/main/java/edu/aiplatform/groupdocs/integration) | Adapter hạ tầng/provider, listener và sender. |
| [groupdocs/contract](../../../../backend/src/main/java/edu/aiplatform/groupdocs/contract) | Public interface/DTO xuyên unit; giữ nguyên. |

| Feature | API | Model | Pages | Components | Hooks |
|---|---|---|---|---|---|
| [groups](../../../../frontend/src/features/groups) | [api](../../../../frontend/src/features/groups/api) | [model](../../../../frontend/src/features/groups/model) | — | [components](../../../../frontend/src/features/groups/components) | — |
| [groupdocs](../../../../frontend/src/features/groupdocs) | [api](../../../../frontend/src/features/groupdocs/api) | [model](../../../../frontend/src/features/groupdocs/model) | [pages](../../../../frontend/src/features/groupdocs/pages) | [components](../../../../frontend/src/features/groupdocs/components) | [hooks](../../../../frontend/src/features/groupdocs/hooks) |

Tests: [groups](../../../../backend/src/test/java/edu/aiplatform/groups), [groupdocs](../../../../backend/src/test/java/edu/aiplatform/groupdocs).

[Controller, migration and individual test inventory](../../../inception/reverse-engineering/current-code-baseline.md).

## Evidence và giới hạn

Kiểm tra của đợt refactor được ghi trong [refactor results](../../build-and-test/2026-10-30-package-moves-only-results.md). Những báo cáo có ngày trước đó là bằng chứng lịch sử. Có source/test không đồng nghĩa test pass hoặc runtime/provider healthy. Negative cases cần kiểm trong test nguồn: actor sai role/scope, không có dữ liệu, stale version, unavailable provider; chi tiết thật theo assertions của từng file test.

### Ghi nhận kiểm thử trước baseline

- [aidlc-docs/construction/build-and-test/e2e-u02-u06-u12-2026-10-24.md](../../build-and-test/e2e-u02-u06-u12-2026-10-24.md)
- [aidlc-docs/construction/build-and-test/u12-verification.md](../../build-and-test/u12-verification.md)
