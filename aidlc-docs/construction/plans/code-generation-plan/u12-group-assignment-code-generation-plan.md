# U12 Group Assignment — Code Generation Plan

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U12 Group Assignment thuộc Iter 3, người code: Người 2; UC 53–57. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 53, 54, 55, 56, 57. **Primary stories:** US-GRP-001, US-GRP-002, US-GRP-003, US-GRP-004, US-GRP-005, US-GRP-006. **Requirements:** FR-025, FR-026, FR-018, FR-039.

Thiết kế: [business-logic-model](../../u12-group-assignment/functional-design/business-logic-model.md), [business-rules](../../u12-group-assignment/functional-design/business-rules.md), [domain-entities](../../u12-group-assignment/functional-design/domain-entities.md), [frontend-components](../../u12-group-assignment/functional-design/frontend-components.md), [NFR](../../u12-group-assignment/nfr-requirements/nfr-requirements.md), [NFR design](../../u12-group-assignment/nfr-design/nfr-design-patterns.md), [infrastructure](../../u12-group-assignment/infrastructure-design/infrastructure-design.md).

## Trạng thái

Plan mới, chưa được duyệt và chưa code. Thay plan U12 cũ và plan U14 cũ.

## Code hiện có

| Path | Hiện trạng | Việc trong plan này |
|---|---|---|
| `backend/.../groups/` (`GroupReadinessService`, `MembershipQueryService`, `GroupEvents`, port `GroupChangePort`, `GroupMembershipPort`, `GroupView`) | Khung cũ, chưa có test | Giữ package `groups`; `GroupReadinessService` cài `GroupAssignmentSupport` của U07; `GroupChangePort` thành gọi nội bộ |
| `backend/.../groupdocs/` (`AssignmentLifecycleAdapter`, `GroupChangeAdapter`, `GroupSubmissionQueryService`, `GroupSubmitted`, port `GroupSubmissionQueryPort`, `GroupSubmissionView`, `GroupSubmittedPort`, `SseHub`, `RabbitSseHub`) | Khung cũ | Giữ package `groupdocs` và `SseHub`; adapter cài `AssignmentLifecycleListener`, `GroupWorkStatusPort`, `GroupSubmissionSource`; bỏ `GroupSubmittedPort` (dùng `GroupEvaluationPort` của U09) |
| `contracts/openapi/groups.yaml`, `group-docs.yaml`, `contracts/messages/group-events.json`, `group-submission-events.json` | Contract cũ | Cập nhật theo API và sự kiện mới |

## Checklist

- [ ] **Bước 1 — Dọn code cũ:** theo bảng trên; code biên dịch được.
- [ ] **Bước 2 — Migration:** `db/migration/groups/V20260925_1800__create_group_tables.sql` theo infrastructure-design.
- [ ] **Bước 3 — Domain:** `ClassGroup`, `GroupMember`, `LeaderChangeRequest`, `GroupDocument`, `GroupSection`, `SectionRevision` và enum; repository chỉ dùng trong package.
- [ ] **Bước 4 — Nhóm:** `ClassGroupService`, `RandomGroupSplitter` (F1, F2, F14, P2).
- [ ] **Bước 5 — Trưởng nhóm:** `LeaderRequestService` (F3, F5).
- [ ] **Bước 6 — Bài nhóm:** `GroupReadinessService`, `GroupAssignmentLifecycle` (F6, F7, F13).
- [ ] **Bước 7 — Tài liệu nhóm:** `GroupDocumentService`, `GroupDocCreateJobHandler`, `GroupSectionService` (F7–F11, P1, P3).
- [ ] **Bước 8 — Nộp:** `GroupSubmissionService`, `GroupAutoSubmitScanner` (F12, F13, P5).
- [ ] **Bước 9 — Realtime:** `SseHub`, `RabbitSseHub`, exchange `platform.realtime` (P4).
- [ ] **Bước 10 — Port cho unit khác:** `GroupWorkQueryService` (cài `GroupWorkStatusPort`, `GroupSubmissionSource`), `GroupMcpTools` (tool `read_authorized_group_submission`), ghi dữ liệu hoạt động qua U08 (F15).
- [ ] **Bước 11 — API:** controller theo domain-entities mục 5; cập nhật contract; ngoài phạm vi trả `404`.
- [ ] **Bước 12 — Frontend:** `ClassGroupsPanel` (đăng ký `TeacherClassTab`), `StudentGroupInfo` (`StudentDetailSection`), `MyGroupSection` (`StudentClassSection`), `GroupAssignmentPanel` (`AssignmentFormSection`), `OpenGroupWorkButton` (`GroupWorkEntry`), `GroupWorkspacePage` và các component.
- [ ] **Bước 13 — Unit test (người code viết):** mỗi sinh viên một nhóm, nhóm phải có trưởng nhóm; chia ngẫu nhiên chênh tối đa một người; lưu bộ nhóm mâu thuẫn bị từ chối cả bộ; xóa nhóm đã dùng bị từ chối; yêu cầu đổi trưởng nhóm (lý do, đề cử, một yêu cầu chờ, hủy khi đổi trực tiếp); sẵn sàng phát hành; dựng mục từ phần; nhận, giao, nhả, Xong và trạng thái mục; lưu nháp lệch `version`; chỉ trưởng nhóm nộp; tự nộp chốt bản nháp; người bị gỡ khỏi lớp mất quyền; component frontend. Integration test do tester viết.
- [ ] **Bước 14 — Review:** đối chiếu UC 53–57, story, BR-U12 với code; người dùng duyệt trước khi chuyển unit.

## Integration test cases (tester viết)

- Hai thành viên nhận cùng một mục cùng lúc: chỉ một người được.
- Hết hạn khi có mục đang làm: bản nháp được chốt vào bản nộp, U09 nhận `onGroupSubmitted`.
- Gỡ thành viên đang giữ mục khi bài đang mở: mục được nhả, nội dung đã Xong giữ tác giả.
- SSE: thay đổi trên instance A tới người mở tài liệu ở instance B trong 2 giây.
- Phân quyền 4 role trên mọi endpoint, gồm sinh viên nhóm khác mở tài liệu và giảng viên lớp khác nhả khóa.

## DoD

Người viết requirement, người viết test case và người code là ba người khác nhau. Không đánh dấu bước hoàn thành khi chưa có code và test chạy được; code nằm ở thư mục gốc repo, không nằm trong `aidlc-docs`.
