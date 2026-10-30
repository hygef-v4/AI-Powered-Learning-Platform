# U13 Reference Materials — AS-BUILT 2026-10-30

Baseline `staging@794a405304bf02dd2593a9fa2c88662d7cd52845`; source-observed. Primary UC 58–62; FR-004, FR-005.

Module cơ bản của môn đã triển khai. Học liệu môn/lớp bắt buộc moduleId cùng môn; dữ liệu cũ backfill Module 1. Manager quản lý module; class viewers đọc theo scope. Quiz nằm tối đa một module, attach khác module chuyển link; phải kiểm quyền với Quiz. Delete chặn active materials; archived materials chuyển sang module khác, không xóa module cuối nếu cần giữ chúng. Version/reorder kiểm server; module không đồng nghĩa Lesson/RAG.

## Source và negative-test inventory

- [backend/src/main/java/edu/aiplatform/materials/application/AssignmentSourceTextService.java](../../../../backend/src/main/java/edu/aiplatform/materials/application/AssignmentSourceTextService.java)
- [backend/src/main/java/edu/aiplatform/materials/application/MaterialModuleService.java](../../../../backend/src/main/java/edu/aiplatform/materials/application/MaterialModuleService.java)
- [backend/src/main/java/edu/aiplatform/materials/application/MaterialQueryService.java](../../../../backend/src/main/java/edu/aiplatform/materials/application/MaterialQueryService.java)
- [backend/src/main/java/edu/aiplatform/materials/application/MaterialService.java](../../../../backend/src/main/java/edu/aiplatform/materials/application/MaterialService.java)
- [backend/src/main/java/edu/aiplatform/materials/application/ReferenceMaterialService.java](../../../../backend/src/main/java/edu/aiplatform/materials/application/ReferenceMaterialService.java)
- [backend/src/main/java/edu/aiplatform/materials/web/ClassMaterialController.java](../../../../backend/src/main/java/edu/aiplatform/materials/web/ClassMaterialController.java)
- [backend/src/main/java/edu/aiplatform/materials/web/MaterialController.java](../../../../backend/src/main/java/edu/aiplatform/materials/web/MaterialController.java)
- [backend/src/main/java/edu/aiplatform/materials/web/MaterialModuleController.java](../../../../backend/src/main/java/edu/aiplatform/materials/web/MaterialModuleController.java)
- [backend/src/main/java/edu/aiplatform/materials/web/MyMaterialController.java](../../../../backend/src/main/java/edu/aiplatform/materials/web/MyMaterialController.java)
- [backend/src/main/java/edu/aiplatform/materials/web/SubjectMaterialController.java](../../../../backend/src/main/java/edu/aiplatform/materials/web/SubjectMaterialController.java)

[Contracts/migrations/tests/UI đầy đủ](../../../inception/reverse-engineering/current-code-baseline.md); [Delta disposition](../../../inception/reverse-engineering/decision-register.md).

## Evidence và giới hạn

Không chạy lại application test/build trong đợt sửa tài liệu. Có source/test không đồng nghĩa test pass hoặc runtime/provider healthy. Negative cases cần kiểm trong test nguồn: actor sai role/scope, không có dữ liệu, stale version, unavailable provider; chi tiết thật theo assertions của từng file test.

### Ghi nhận kiểm thử trước baseline

- [aidlc-docs/construction/build-and-test/u13-verification.md](../../build-and-test/u13-verification.md)
