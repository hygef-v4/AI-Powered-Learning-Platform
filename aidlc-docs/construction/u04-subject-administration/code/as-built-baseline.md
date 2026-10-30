# U04 Subject Administration — AS-BUILT 2026-10-30

Baseline `staging@794a405304bf02dd2593a9fa2c88662d7cd52845`; source-observed. Primary UC 16–19; FR-003.

subjects có quản trị danh sách/chi tiết/create/update, manager assignment, archive/reopen và version guards. UI tại admin/subjects; Subject Detail của Admin không cấp quyền chấm hoặc sửa lớp. Các handoff raw paths bị mất được thay bằng baseline này, không tạo lại nội dung approval giả.

## Source và negative-test inventory

- [backend/src/main/java/edu/aiplatform/subjects/application/SubjectAdminService.java](../../../../backend/src/main/java/edu/aiplatform/subjects/application/SubjectAdminService.java)
- [backend/src/main/java/edu/aiplatform/subjects/application/SubjectCommandService.java](../../../../backend/src/main/java/edu/aiplatform/subjects/application/SubjectCommandService.java)
- [backend/src/main/java/edu/aiplatform/subjects/application/SubjectQueryService.java](../../../../backend/src/main/java/edu/aiplatform/subjects/application/SubjectQueryService.java)
- [backend/src/main/java/edu/aiplatform/subjects/application/SubjectScopeService.java](../../../../backend/src/main/java/edu/aiplatform/subjects/application/SubjectScopeService.java)
- [backend/src/main/java/edu/aiplatform/subjects/application/SubjectStatusService.java](../../../../backend/src/main/java/edu/aiplatform/subjects/application/SubjectStatusService.java)
- [backend/src/main/java/edu/aiplatform/subjects/web/SubjectAdminController.java](../../../../backend/src/main/java/edu/aiplatform/subjects/web/SubjectAdminController.java)

[Contracts/migrations/tests/UI đầy đủ](../../../inception/reverse-engineering/current-code-baseline.md); [Delta disposition](../../../inception/reverse-engineering/decision-register.md).

## Evidence và giới hạn

Không chạy lại application test/build trong đợt sửa tài liệu. Có source/test không đồng nghĩa test pass hoặc runtime/provider healthy. Negative cases cần kiểm trong test nguồn: actor sai role/scope, không có dữ liệu, stale version, unavailable provider; chi tiết thật theo assertions của từng file test.

### Ghi nhận kiểm thử trước baseline

- [aidlc-docs/construction/build-and-test/2026-10-25-b2-b8-u04-results.md](../../build-and-test/2026-10-25-b2-b8-u04-results.md)
