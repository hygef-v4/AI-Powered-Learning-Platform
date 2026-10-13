# U12 Group & Allocation — frontend-components

> Current Construction revision 2026-10-13: 66 active UC, 50 MVP stories + 1 extension, 15 logical units. Đồng bộ theo Inception, chỉ sửa tài liệu; chưa triển khai/kiểm runtime hoặc duyệt stage/code mới. Approval/checklist trước revision thuộc lịch sử.

**Primary UC:** 16, 17. **Primary stories:** US-GRP-001, US-GRP-002. **Requirements:** FR-025.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [decisions](../../../inception/requirements/revision-decisions-2026-10-13.md). Baseline cũ: [snapshot](../../historical/baseline-before-66-uc-2026-10-13/README.md). Folder legacy `u12-group-allocation` không quyết định chức năng hiện hành.

## 1. Màn hình theo screen flow

Screen flow không có màn nhóm riêng: U12 gắn component vào màn của U04 và U08.

| Màn | Component | UC | Role | Mở từ |
|---|---|---|---|---|
| Tab Students (Teacher Class Detail) | `ClassStudentsGroupsPanel` (gắn vào `StudentsTab` của U04) | 32 (luồng phụ nhóm, duyệt yêu cầu) | Teacher, Subject Manager được giao dạy | Class Dashboard → Teacher Class Detail |
| Popup chia ngẫu nhiên | `RandomSplitDialog` | 32 | Teacher, Subject Manager được giao dạy | Tab Students |
| Popup Student Detail (U04) | Dòng Nhóm đọc dữ liệu của `ClassStudentsGroupsPanel` | 32 | Teacher, Subject Manager được giao dạy | Tab Students |
| Hộp phát hành của Assignment Form (U08) | `GroupReadinessPanel` | 45 | Teacher, Subject Manager được giao dạy | Tab Evals → Assignment Form |
| Student Class Detail (U04) | `MyGroupPanel` | 16 | Student | Class Dashboard → Student Class Detail |
| Popup Request Leader Change | `LeaderChangeRequestDialog` | 17 | Student (thành viên nhóm) | Student Class Detail |

## 2. Cây component

```
app/classes/[id]/teaching/                StudentsTab (U04)        tab Students của Teacher Class Detail
  ClassStudentsGroupsPanel                nhóm của lớp, bản nháp phía client, nút Lưu
    GroupsToolbar                         Tạo nhóm, Chia ngẫu nhiên, Lưu
    GroupCard                             tên, thành viên (nhãn "Đã rời lớp"), chọn trưởng nhóm, thêm/bớt, xóa nhóm chưa dùng
    UngroupedStudentsPanel                sinh viên chưa có nhóm
    RandomSplitDialog                     popup: sĩ số tối đa, xem trước
    LeaderRequestsPanel                   yêu cầu đổi trưởng nhóm: Duyệt / Từ chối
  StudentDetail (U04)                     popup Student Detail: dòng Nhóm lấy từ ClassStudentsGroupsPanel
app/classes/[id]/teaching/assignments/[assignmentId]/  AssignmentFormPage (U08)
  PublishDialog (U08)
    GroupReadinessPanel                   lỗi chặn, cảnh báo cần xác nhận (bài nhóm)
app/classes/[id]/                         StudentClassPage (U04)   màn Student Class Detail
  MyGroupPanel                            phần Nhóm của tôi (UC 16)
    MyGroupCard                           tên nhóm, thành viên, trưởng nhóm; gắn GroupDocsOfGroupList (U14) cho tài liệu bài nhóm
    LeaderRequestStatus                   yêu cầu gần nhất của mình, nút Hủy khi còn chờ
    LeaderChangeRequestDialog             popup Request Leader Change (UC 17)
```

Route theo `RoleGuard` của U01 (`/classes/*` cho Student, Teacher, Subject Manager; Admin không vào). Backend vẫn kiểm R3/R4, R5.

## 3. Component và API

| Component | Hành vi | API |
|---|---|---|
| `ClassStudentsGroupsPanel`, `GroupCard`, `UngroupedStudentsPanel` | Sửa trên bản nháp phía client (kéo thả thành viên), bấm Lưu gửi nguyên khối; `409` báo tải lại; lớp `ARCHIVED` chỉ xem; thành viên đã rời lớp có nhãn và bị bỏ khi lưu | `GET`, `PUT /api/v1/classes/{classId}/groups` |
| `RandomSplitDialog` | Sĩ số 2-20; trả xem trước, gộp vào bản nháp, chưa ghi | `POST /api/v1/classes/{classId}/groups:random-split` |
| `StudentDetail` (U04) | Dòng Nhóm: tên nhóm, có là trưởng nhóm không; "Chưa có nhóm" nếu không thấy | Dữ liệu đã tải từ `GET /api/v1/classes/{classId}/groups` |
| `LeaderRequestsPanel` | Danh sách `PENDING` trước; duyệt chọn trưởng nhóm mới (mặc định người được đề cử, bắt buộc chọn khi không có đề cử); từ chối bắt buộc lý do; đổi trực tiếp trong `GroupCard` | `GET /api/v1/classes/{classId}/leader-requests`, `POST /api/v1/leader-requests/{requestId}/approve`, `.../reject` |
| `GroupReadinessPanel` | Lỗi chặn phát hành; cảnh báo sinh viên chưa có nhóm cần giảng viên tick xác nhận trước khi U08 gửi phát hành | `GET /api/v1/classes/{classId}/groups/readiness` |
| `MyGroupPanel`, `MyGroupCard` | Chỉ nhóm của mình; chưa có nhóm thì hiện thông báo; mỗi tài liệu mở Assignment Detail của bài nhóm (U11) rồi Group Essay Workspace (U14) | `GET /api/v1/classes/{classId}/my-group` |
| `LeaderRequestStatus` | Trạng thái, lý do từ chối; Hủy khi còn `PENDING` | `POST /api/v1/leader-requests/{requestId}/cancel` |
| `LeaderChangeRequestDialog` | Lý do 10-1000 ký tự, đề cử tùy chọn một thành viên khác trưởng nhóm; nút bị khóa khi nhóm đang có yêu cầu chờ | `POST /api/v1/groups/{groupId}/leader-requests` |

## Quy tắc tích hợp revision hiện hành

- Groups belong class, not assessment. Teacher assigned R3/R4 creates/allocates; Admin/manager without teaching assignment denied.
- Members ACTIVE enrolled, one effective group/class; exactly one leader in members. Random assignment only ungrouped students, preserve existing groups, preview balanced difference <=1 before save.
- Leader-change request PENDING/APPROVED/REJECTED/CANCELLED; max one pending/group; reason/proposed member validated. Only assigned teacher decides, requester may cancel pending.
- Changing/removing leader must select replacement atomically. Removal current membership revokes edit/submit next call but preserves revision authorship.
- Publication readiness for ASSIGNMENT submissionMode GROUP through GroupReadinessPort; không separate Group product type.
- Authorization current actor/resource on every web/API/MCP call, fail closed; CSRF protections for cookie-session state-changing routes; no user-supplied actor trusted.
- expectedVersion on edits and idempotency on repeatable commands/events; conflicts do not overwrite current state, retry bounded, reference returned not secrets.
- Unit repository private; cross-unit read/write through public ports. DB ownership is logical within one Spring Boot monolith; no 15 service deploys.
- No fabricated survey/implementation evidence. D01 Quiz Practice, D02 Group Graded, D03 own-editable group AI context retained provisionally; numeric quota D04 and backup/activity retention require settings confirmation.

## Kiểm tra revision bắt buộc

- Balanced preview preserve grouped students; concurrent version conflict; exactly leader; nonteacher denied.
- Member removal denies U14/AI context next call while historical author retained; group readiness missing leader blocks publish.
