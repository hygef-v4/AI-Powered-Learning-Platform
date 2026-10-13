# U12 Group & Allocation — business-logic-model

> Current Construction revision 2026-10-13: 66 active UC, 50 MVP stories + 1 extension, 15 logical units. Đồng bộ theo Inception, chỉ sửa tài liệu; chưa triển khai/kiểm runtime hoặc duyệt stage/code mới. Approval/checklist trước revision thuộc lịch sử.

**Primary UC:** 16, 17. **Primary stories:** US-GRP-001, US-GRP-002. **Requirements:** FR-025.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [decisions](../../../inception/requirements/revision-decisions-2026-10-13.md). Baseline cũ: [snapshot](../../historical/baseline-before-66-uc-2026-10-13/README.md). Folder legacy `u12-group-allocation` không quyết định chức năng hiện hành.

Actor: "giảng viên lớp" = Teacher hoặc Subject Manager là giảng viên chính của lớp (R3/R4); "sinh viên" = người ghi danh `ACTIVE` lớp `OPEN` (R5). Admin không có chức năng nhóm. Chủ nhiệm môn (R2) không quản lý nhóm nếu không phải giảng viên chính của lớp.

## F1 - Quản lý nhóm của lớp (luồng phụ UC 32, tab Students)
1. Giảng viên lớp mở Teacher Class Detail (U04) → tab Students: thấy các nhóm của lớp (mọi `StudentGroup` cùng `classId`, rỗng nếu chưa có), sinh viên chưa có nhóm và thành viên đã rời lớp (BR-U12-15).
2. Chọn cách: tạo/sửa tay (BR-U12-04) hoặc chia ngẫu nhiên (BR-U12-05); đổi trưởng nhóm trực tiếp trên thẻ nhóm (BR-U12-12).
3. Lưu nguyên khối, kiểm BR-U12-02, 03, 07, 08, 09; audit.
4. Bấm một sinh viên → popup Student Detail (U04) hiện nhóm của người đó lấy từ dữ liệu nhóm đã tải ở tab Students (BR-U12-33).

## F2 - Chia ngẫu nhiên
1. Lấy sinh viên đang ghi danh `ACTIVE` chưa có nhóm trong lớp, xáo trộn.
2. Số nhóm mới = trần(số người / sĩ số tối đa); chia vòng tròn để chênh ≤ 1.
3. Đặt tên "Nhóm N" tiếp theo; chọn trưởng nhóm ngẫu nhiên; trả bản xem trước để giảng viên sửa rồi lưu (F1 bước 3).

## F3 - (Đã bỏ) Phân công phần của giảng viên
- Khung bài nhóm tự chia thành các phần theo heading (U09). Trưởng nhóm giao phần cho thành viên trong tài liệu nhóm (U14, UC 27).

## F4 - Sẵn sàng phát hành (`GroupReadinessPort`, UC 42)
- Bài nhóm (`ASSIGNMENT` với `submissionMode=GROUP`) chỉ có ở lớp. U08 gọi khi giảng viên lớp phát hành bài nhóm: kiểm BR-U12-21 trên nhóm của lớp, trả lỗi chặn (lớp chưa có nhóm, nhóm thiếu trưởng nhóm, nhóm không còn thành viên) và cảnh báo (sinh viên chưa có nhóm) để giảng viên xác nhận.
- Hộp phát hành của U08 nhúng `GroupReadinessPanel` để hiện trước danh sách này.

## F5 - Đổi nhóm khi lớp đã có bài nhóm đang mở
1. Thêm/bớt thành viên, đổi nhóm, tạo nhóm mới (BR-U12-22) trong một transaction.
2. Nhóm mới: gọi `GroupChangePort.onGroupCreated(groupId)` → U14 tạo tài liệu nhóm cho mọi bài nhóm đang mở của lớp. Thành viên rời nhóm: gọi `GroupChangePort.onMemberRemoved(groupId, studentId)` → U14 nhả khóa mục của người đó ở mọi tài liệu nhóm đang mở. Cả hai trong cùng transaction.
3. Audit; sau commit phát event `group.membership-changed` cho thông báo.

## F6 - Yêu cầu đổi trưởng nhóm (UC 17) và quyết định của giảng viên
1. Sinh viên ở phần Nhóm của tôi bấm "Yêu cầu đổi trưởng nhóm" → popup: nhập lý do, có thể đề cử một thành viên khác (BR-U12-10). Gửi → `PENDING`; sau commit phát `group.leader-requested` để U16 báo giảng viên lớp. Người gửi hủy được khi còn `PENDING` (BR-U12-11).
2. Giảng viên lớp xem yêu cầu ở tab Students (luồng phụ UC 32): duyệt (chọn trưởng nhóm mới, mặc định người được đề cử; bắt buộc chọn khi không có đề cử), từ chối (bắt buộc lý do), hoặc đổi trực tiếp (BR-U12-11, 12). Sau commit: duyệt hoặc đổi trực tiếp phát `group.leader-changed`; từ chối phát `group.leader-request-rejected` để U16 báo người gửi.

## F7 - Sinh viên xem nhóm (UC 16)
1. Sinh viên mở Class Dashboard → Student Class Detail (U04); phần Nhóm của tôi (U12 gắn vào) gọi `my-group`.
2. Hiện tên nhóm, thành viên, trưởng nhóm, tài liệu nhóm của từng bài nhóm (`GroupDocsOfGroupList` của U14 gọi `GET /api/v1/groups/{groupId}/group-docs`) và yêu cầu đổi trưởng nhóm gần nhất của mình (BR-U12-30). Chưa có nhóm → thông báo "Bạn chưa có nhóm trong lớp này".
3. Bấm một tài liệu → Assignment Detail của bài nhóm (U11) → Group Essay Workspace (U14), theo screen flow.

## F8 - Sinh viên bị gỡ khỏi lớp
- Chủ nhiệm môn gỡ sinh viên (UC 51, U04) không gọi U12. U12 xử lý khi đọc theo BR-U12-15: bỏ người đó khỏi `GroupMembershipPort` và Nhóm của tôi, đánh dấu "Đã rời lớp" ở tab Students; lần lưu sau của giảng viên xóa dòng và gọi `onMemberRemoved`.

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
