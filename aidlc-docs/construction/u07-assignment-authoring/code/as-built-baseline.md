# U07 Assignment Authoring — AS-BUILT 2026-10-30

Baseline `staging@794a405304bf02dd2593a9fa2c88662d7cd52845`; source-observed. Primary UC 34–36; FR-006, FR-007, FR-016, FR-017, FR-027, FR-028.

assignments sở hữu document/rubric/authoring/AI draft/publication/copy. UI editor và service copy hiện có; không phân loại sample/general rubric/flat-page hoàn thành chỉ từ nhãn feat(ui). Nguồn AI qua U06, kết quả chỉ proposal/draft; version và scoped destination vẫn phải kiểm server. Hợp nhất delta AI authoring 24/10 và summary có ngày vào truy vết.

## Source và negative-test inventory

- [backend/src/main/java/edu/aiplatform/assignments/application/AssignmentAiDraftService.java](../../../../backend/src/main/java/edu/aiplatform/assignments/application/AssignmentAiDraftService.java)
- [backend/src/main/java/edu/aiplatform/assignments/application/AssignmentAuthoringService.java](../../../../backend/src/main/java/edu/aiplatform/assignments/application/AssignmentAuthoringService.java)
- [backend/src/main/java/edu/aiplatform/assignments/application/AssignmentCopyService.java](../../../../backend/src/main/java/edu/aiplatform/assignments/application/AssignmentCopyService.java)
- [backend/src/main/java/edu/aiplatform/assignments/application/AssignmentMcpAuthoringService.java](../../../../backend/src/main/java/edu/aiplatform/assignments/application/AssignmentMcpAuthoringService.java)
- [backend/src/main/java/edu/aiplatform/assignments/application/AssignmentPublicationService.java](../../../../backend/src/main/java/edu/aiplatform/assignments/application/AssignmentPublicationService.java)
- [backend/src/main/java/edu/aiplatform/assignments/application/AssignmentQueryService.java](../../../../backend/src/main/java/edu/aiplatform/assignments/application/AssignmentQueryService.java)
- [backend/src/main/java/edu/aiplatform/assignments/application/DocumentModelService.java](../../../../backend/src/main/java/edu/aiplatform/assignments/application/DocumentModelService.java)
- [backend/src/main/java/edu/aiplatform/assignments/application/DocxService.java](../../../../backend/src/main/java/edu/aiplatform/assignments/application/DocxService.java)
- [backend/src/main/java/edu/aiplatform/assignments/application/RubricService.java](../../../../backend/src/main/java/edu/aiplatform/assignments/application/RubricService.java)
- [backend/src/main/java/edu/aiplatform/assignments/web/AssignmentAiDraftController.java](../../../../backend/src/main/java/edu/aiplatform/assignments/web/AssignmentAiDraftController.java)
- [backend/src/main/java/edu/aiplatform/assignments/web/AssignmentController.java](../../../../backend/src/main/java/edu/aiplatform/assignments/web/AssignmentController.java)
- [backend/src/main/java/edu/aiplatform/assignments/web/ClassAssignmentController.java](../../../../backend/src/main/java/edu/aiplatform/assignments/web/ClassAssignmentController.java)
- [backend/src/main/java/edu/aiplatform/assignments/web/RubricController.java](../../../../backend/src/main/java/edu/aiplatform/assignments/web/RubricController.java)
- [backend/src/main/java/edu/aiplatform/assignments/web/SubjectAssignmentController.java](../../../../backend/src/main/java/edu/aiplatform/assignments/web/SubjectAssignmentController.java)

[Contracts/migrations/tests/UI đầy đủ](../../../inception/reverse-engineering/current-code-baseline.md); [Delta disposition](../../../inception/reverse-engineering/decision-register.md).

## Evidence và giới hạn

Không chạy lại application test/build trong đợt sửa tài liệu. Có source/test không đồng nghĩa test pass hoặc runtime/provider healthy. Negative cases cần kiểm trong test nguồn: actor sai role/scope, không có dữ liệu, stale version, unavailable provider; chi tiết thật theo assertions của từng file test.
