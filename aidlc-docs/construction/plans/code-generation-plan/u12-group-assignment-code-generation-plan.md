# U12 Group Assignment — Code Generation Plan

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U12 Group Assignment thuộc Iter 3, người code: Người 2; UC 53–57. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 53, 54, 55, 56, 57. **Primary stories:** US-GRP-001, US-GRP-002, US-GRP-003, US-GRP-004, US-GRP-005, US-GRP-006. **Requirements:** FR-025, FR-026, FR-018, FR-039.

Thiết kế: [business-logic-model](../../u12-group-assignment/functional-design/business-logic-model.md), [business-rules](../../u12-group-assignment/functional-design/business-rules.md), [domain-entities](../../u12-group-assignment/functional-design/domain-entities.md), [frontend-components](../../u12-group-assignment/functional-design/frontend-components.md), [NFR](../../u12-group-assignment/nfr-requirements/nfr-requirements.md), [NFR design](../../u12-group-assignment/nfr-design/nfr-design-patterns.md), [infrastructure](../../u12-group-assignment/infrastructure-design/infrastructure-design.md).

## Trạng thái

Plan theo unit 2026-10-15, chưa được duyệt và chưa code. Khung code tạo lại ngày 2026-10-16 (code cũ đã xóa, không dùng lại); unit chưa có nghiệp vụ, migration hay test riêng.

## Code hiện có

| Path | Hiện trạng |
|---|---|
| `backend/src/main/java/edu/aiplatform/groups/` | Đủ 5 tầng `contract`, `application`, `domain`, `infrastructure`, `web`; tầng chưa có code giữ chỗ bằng `.gitkeep` |
| `backend/.../groups/contract/` | Trống: unit không cung cấp interface mới, chỉ cài interface của unit trước |
| `frontend/src/features/groups/` | Chỉ có `index.ts` trống |
| `backend/src/main/java/edu/aiplatform/groupdocs/` | Đủ 5 tầng `contract`, `application`, `domain`, `infrastructure`, `web`; tầng chưa có code giữ chỗ bằng `.gitkeep` |
| `backend/.../groupdocs/contract/` | interface `ISseHub` |
| `backend/.../groupdocs/contract/stub/GroupDocsContractStubs` | Bản tạm trả dữ liệu mẫu cho `ISseHub` (danh sách rỗng, quyền `true`); tự tắt khi unit cài thật |
| `frontend/src/features/groupdocs/` | Chỉ có `index.ts` trống |

## Checklist

- [ ] **Bước 1 — Migration:** `db/migration/groups/V20260925_1800__create_group_tables.sql` theo infrastructure-design.
- [ ] **Bước 2 — Domain:** `ClassGroup`, `GroupMember`, `LeaderChangeRequest`, `GroupDocument`, `GroupSection`, `SectionRevision` và enum; repository chỉ dùng trong package.
- [ ] **Bước 3 — Nhóm:** `ClassGroupService`, `RandomGroupSplitter` (F1, F2, F14, P2).
- [ ] **Bước 4 — Trưởng nhóm:** `LeaderRequestService` (F3, F5).
- [ ] **Bước 5 — Bài nhóm:** `GroupReadinessService`, `GroupAssignmentLifecycle` (F6, F7, F13).
- [ ] **Bước 6 — Tài liệu nhóm:** `GroupDocumentService`, `GroupDocCreateJobHandler`, `GroupSectionService` (F7–F11, P1, P3).
- [ ] **Bước 7 — Nộp:** `GroupSubmissionService`, `GroupAutoSubmitScanner` (F12, F13, P5).
- [ ] **Bước 8 — Realtime:** `ISseHub`, `RabbitSseHub`, exchange `platform.realtime` (P4).
- [ ] **Bước 9 — Interface cho unit khác:** `GroupWorkQueryService` (cài `IGroupWorkStatus`, `IGroupSubmissionSource`), `GroupMcpTools` (tool `read_authorized_group_submission`), ghi dữ liệu hoạt động qua U08 (F15).
- [ ] **Bước 10 — API:** controller theo domain-entities mục 5; cập nhật contract; ngoài phạm vi trả `404`.
- [ ] **Bước 11 — Frontend:** `ClassGroupsPanel` (đăng ký `TeacherClassTab`), `StudentGroupInfo` (`StudentDetailSection`), `MyGroupSection` (`StudentClassSection`), `GroupAssignmentPanel` (`AssignmentFormSection`), `OpenGroupWorkButton` (`GroupWorkEntry`), `GroupWorkspacePage` và các component.
- [ ] **Bước 12 — Unit test (người code viết):** mỗi sinh viên một nhóm, nhóm phải có trưởng nhóm; chia ngẫu nhiên chênh tối đa một người; lưu bộ nhóm mâu thuẫn bị từ chối cả bộ; xóa nhóm đã dùng bị từ chối; yêu cầu đổi trưởng nhóm (lý do, đề cử, một yêu cầu chờ, hủy khi đổi trực tiếp); sẵn sàng phát hành; dựng mục từ phần; nhận, giao, nhả, Xong và trạng thái mục; lưu nháp lệch `version`; chỉ trưởng nhóm nộp; tự nộp chốt bản nháp; người bị gỡ khỏi lớp mất quyền; component frontend. Integration test do tester viết.
- [ ] **Bước 13 — Review:** đối chiếu UC 53–57, story, BR-U12 với code; người dùng duyệt trước khi chuyển unit.

## Integration test cases (tester viết)

- Hai thành viên nhận cùng một mục cùng lúc: chỉ một người được.
- Hết hạn khi có mục đang làm: bản nháp được chốt vào bản nộp, U09 nhận `onGroupSubmitted`.
- Gỡ thành viên đang giữ mục khi bài đang mở: mục được nhả, nội dung đã Xong giữ tác giả.
- SSE: thay đổi trên instance A tới người mở tài liệu ở instance B trong 2 giây.
- Phân quyền 4 role trên mọi endpoint, gồm sinh viên nhóm khác mở tài liệu và giảng viên lớp khác nhả khóa.

## DoD

Người viết requirement, người viết test case và người code là ba người khác nhau. Không đánh dấu bước hoàn thành khi chưa có code và test chạy được; code nằm ở thư mục gốc repo, không nằm trong `aidlc-docs`.
