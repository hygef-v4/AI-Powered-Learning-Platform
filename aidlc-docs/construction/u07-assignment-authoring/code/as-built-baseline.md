# U07 Assignment Authoring — AS-BUILT source snapshot 2026-11-07

Source snapshot `f68a6ec5889074de80a1f1f0faf7bcb7e7054ec8`; current source paths are catalogued in [current-code-baseline](../../../inception/reverse-engineering/current-code-baseline.md). Dated deltas/test results below retain their original scope; this update is not runtime acceptance.

**Contract delta 2026-11-01:** `AssignmentSettingsSection` / `assignmentSettingsSections` provides U09 a separate assignment-class settings area with class context. Content authoring readOnly does not gate this section: a teacher assigned to the class can configure histogram visibility for a shared SUBJECT assignment while its content remains readOnly. `AssignmentFormSection` remains the existing U12 group slot. Verification for this delta belongs to the current change, not the older reports below.

assignments sở hữu document/rubric/authoring/AI draft/publication/copy. UI editor và service copy hiện có; không phân loại sample/general rubric/flat-page hoàn thành chỉ từ nhãn feat(ui). Nguồn AI qua U06, kết quả chỉ proposal/draft; version và scoped destination vẫn phải kiểm server. Hợp nhất delta AI authoring 24/10 và summary có ngày vào truy vết.

**Compatibility fix 2026-11-01:** RubricRepository reads both the current criteria array and the historical object envelope containing `criteria`, preserving decimal points and IDs. Reading never rewrites the stored envelope or its metadata, nor restores historical authoring features. Malformed criteria still fail instead of becoming an empty rubric. This restores U09 gradebook/progress reads for existing envelope rows. Editor tools now have distinct enabled hover/focus backgrounds and pointer cursors; the assignment sticky header stacks above the document toolbar so its dropdown remains accessible.

**Delta 2026-11-09/10 (`9cf5d502`, `594c79e5`, đối chiếu 2026-11-10):** `AiReviseChat` và `AssignmentDraftRevision` thêm chế độ `REVISE` cho `POST /assignments/{id}/ai-drafts`: AI trả thao tác `UPDATE`/`INSERT`/`DELETE` theo mã khối, server ghép vào bản đề đã gửi, áp dụng giữ id, ảnh, sơ đồ và định dạng của khối không đổi; `409 AI_DRAFT_STALE` khi đề đổi trong lúc AI xử lý. Chi tiết ở [F6b](../functional-design/business-logic-model.md) và BR-U07-54, 55. Tests nguồn: `AssignmentAiRevisionTest`, `AiReviseChat.test.tsx`; đợt đối chiếu này không chạy lại.

**Delta 2026-11-10 (PR #60: `bad8df16`, `16f2c8ce`, `cdaf0f46`, `211927b0`, `4ff7a0da`):** bài của môn là đề mẫu không phát hành và được là bài nhóm; copy nhận `title`; AI soạn nháp nhận `templateAssignmentId`; ba endpoint đọc mới (tài liệu của lớp/môn, đề mẫu); bài có chấm không còn số lượt; `SectionNumbering` đánh số câu hỏi bốn tầng. Migration `V20261109_2330__allow_group_subject_templates.sql`, `V20261110_1200__graded_without_attempt_limit.sql`. Rule BR-U07-80…86 và [delta frontend](../functional-design/frontend-components.md).

## Source và negative-test inventory

| Source | Trách nhiệm |
|---|---|
| [assignments/controller](../../../../backend/src/main/java/edu/aiplatform/assignments/controller) | HTTP endpoints, request/response và lỗi HTTP. |
| [assignments/service](../../../../backend/src/main/java/edu/aiplatform/assignments/service) | Use case, phân quyền, transaction/audit và contract implementation. |
| [assignments/repository](../../../../backend/src/main/java/edu/aiplatform/assignments/repository) | Repository interface/implementation, Spring JDBC/SQL và persistence mapping. |
| [assignments/model](../../../../backend/src/main/java/edu/aiplatform/assignments/model) | Đối tượng, enum và quy tắc nghiệp vụ. |
| [assignments/dto](../../../../backend/src/main/java/edu/aiplatform/assignments/dto) | Kiểu dữ liệu request/response riêng. |
| [assignments/contract](../../../../backend/src/main/java/edu/aiplatform/assignments/contract) | Public interface/DTO xuyên unit; giữ nguyên. |

| Feature | API | Model | Pages | Components | Hooks | Styles | Tests |
|---|---|---|---|---|---|---|---|
| [assignments](../../../../frontend/src/features/assignments) | [api](../../../../frontend/src/features/assignments/api) | [model](../../../../frontend/src/features/assignments/model) | [pages](../../../../frontend/src/features/assignments/pages) | [components](../../../../frontend/src/features/assignments/components) | [hooks](../../../../frontend/src/features/assignments/hooks) | — | [tests](../../../../frontend/tests/features/assignments) |

Tests: [assignments](../../../../backend/src/test/java/edu/aiplatform/assignments).

[Controller, migration and individual test inventory](../../../inception/reverse-engineering/current-code-baseline.md).

## Evidence và giới hạn

Kiểm tra của đợt refactor được ghi trong [refactor results](../../build-and-test/2026-10-30-package-moves-only-results.md). Những báo cáo có ngày trước đó là bằng chứng lịch sử. Có source/test không đồng nghĩa test pass hoặc runtime/provider healthy. Negative cases cần kiểm trong test nguồn: actor sai role/scope, không có dữ liệu, stale version, unavailable provider; chi tiết thật theo assertions của từng file test.
