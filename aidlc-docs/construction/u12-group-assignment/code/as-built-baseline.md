# U12 Group Assignment — AS-BUILT 2026-10-30

Baseline `staging@794a405304bf02dd2593a9fa2c88662d7cd52845`; source-observed. Primary UC 53–57; FR-025, FR-026, FR-039.

groups và groupdocs thuộc cùng U12. Leader request reason tùy chọn, null thành chuỗi rỗng, tối đa 1.000; reject reason tùy chọn, trống thành null. GroupRenameService cho đổi tên theo scope; editor hiển thị lỗi nhóm đã bị gỡ khỏi draft. Claims/locks/leader submit giữ theo SectionRules và services; không tự thêm peer assessment hay đổi AI Student GROUP.

## Source và negative-test inventory

- [backend/src/main/java/edu/aiplatform/groupdocs/application/GroupDocumentService.java](../../../../backend/src/main/java/edu/aiplatform/groupdocs/application/GroupDocumentService.java)
- [backend/src/main/java/edu/aiplatform/groupdocs/application/GroupSectionService.java](../../../../backend/src/main/java/edu/aiplatform/groupdocs/application/GroupSectionService.java)
- [backend/src/main/java/edu/aiplatform/groupdocs/application/GroupSubmissionService.java](../../../../backend/src/main/java/edu/aiplatform/groupdocs/application/GroupSubmissionService.java)
- [backend/src/main/java/edu/aiplatform/groupdocs/application/GroupWorkQueryService.java](../../../../backend/src/main/java/edu/aiplatform/groupdocs/application/GroupWorkQueryService.java)
- [backend/src/main/java/edu/aiplatform/groupdocs/application/GroupWorkspaceService.java](../../../../backend/src/main/java/edu/aiplatform/groupdocs/application/GroupWorkspaceService.java)
- [backend/src/main/java/edu/aiplatform/groupdocs/web/GroupDocController.java](../../../../backend/src/main/java/edu/aiplatform/groupdocs/web/GroupDocController.java)
- [backend/src/main/java/edu/aiplatform/groups/application/ClassGroupService.java](../../../../backend/src/main/java/edu/aiplatform/groups/application/ClassGroupService.java)
- [backend/src/main/java/edu/aiplatform/groups/application/GroupReadinessService.java](../../../../backend/src/main/java/edu/aiplatform/groups/application/GroupReadinessService.java)
- [backend/src/main/java/edu/aiplatform/groups/application/GroupRenameService.java](../../../../backend/src/main/java/edu/aiplatform/groups/application/GroupRenameService.java)
- [backend/src/main/java/edu/aiplatform/groups/application/LeaderRequestService.java](../../../../backend/src/main/java/edu/aiplatform/groups/application/LeaderRequestService.java)
- [backend/src/main/java/edu/aiplatform/groups/application/MyGroupService.java](../../../../backend/src/main/java/edu/aiplatform/groups/application/MyGroupService.java)
- [backend/src/main/java/edu/aiplatform/groups/web/GroupController.java](../../../../backend/src/main/java/edu/aiplatform/groups/web/GroupController.java)

[Contracts/migrations/tests/UI đầy đủ](../../../inception/reverse-engineering/current-code-baseline.md); [Delta disposition](../../../inception/reverse-engineering/decision-register.md).

## Evidence và giới hạn

Không chạy lại application test/build trong đợt sửa tài liệu. Có source/test không đồng nghĩa test pass hoặc runtime/provider healthy. Negative cases cần kiểm trong test nguồn: actor sai role/scope, không có dữ liệu, stale version, unavailable provider; chi tiết thật theo assertions của từng file test.

### Ghi nhận kiểm thử trước baseline

- [aidlc-docs/construction/build-and-test/e2e-u02-u06-u12-2026-10-24.md](../../build-and-test/e2e-u02-u06-u12-2026-10-24.md)
- [aidlc-docs/construction/build-and-test/u12-verification.md](../../build-and-test/u12-verification.md)
