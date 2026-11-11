# U12 — Sinh viên tự chia nhóm — kế hoạch Functional Design

Dựa trên [yêu cầu SG-01…09](../../../inception/requirements/2026-11-11-student-self-grouping.md), US-GRP-007…010 trong [stories.md](../../../inception/user-stories/stories.md) và [kế hoạch thực hiện](../../../inception/plans/2026-11-11-student-self-grouping-execution-plan.md), đã duyệt 2026-11-11.

## Bối cảnh từ mã hiện tại

- `ClassGroupService.save` kiểm cả bộ bằng `GroupSetRules`, tăng `version` bằng `bumpSetVersion`, rồi `apply`: thêm/sửa/xóa nhóm, nhả mục của người chuyển nhóm, tạo tài liệu cho nhóm mới khi lớp có bài nhóm đang mở (`ensureForGroups`), audit `GROUP_CREATED`, phát `group.membership-changed` (U14 báo cho người vào nhóm).
- Sinh viên rời lớp không có listener riêng: U12 lọc theo ghi danh `ACTIVE` khi đọc và bỏ họ ở lần lưu sau (BR-U12-07).
- Thông báo đi theo sự kiện `EventTypes` → `NotificationEventService` → `RecipientResolver`.

## Câu hỏi

Phương án đầu của mỗi câu là đề xuất của AI.

## Question 1
Duyệt đề xuất thành nhóm chính thức bằng đường nào?

A) Dùng lại phần lõi của `ClassGroupService.save`: bộ nhóm hiện tại cộng nhóm mới từ đề xuất, cùng kiểm `GroupSetRules`, cùng `apply` (tạo tài liệu cho bài nhóm đang mở, audit, sự kiện); server tự đọc và tăng `version`, giảng viên không gửi `version` khi duyệt (đề xuất)

B) Đường riêng chỉ chèn nhóm và thành viên, tự kiểm quy tắc

C) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 2
Duyệt nhiều đề xuất một lần chạy thế nào?

A) Một transaction, một lần tăng `version`; kiểm lần lượt từng đề xuất trên bộ nhóm đã cộng các đề xuất hợp lệ trước đó; đề xuất lỗi bị bỏ qua, trả về danh sách lỗi theo đề xuất, vẫn ở chờ duyệt (đề xuất)

B) Mỗi đề xuất một transaction riêng

C) Có một đề xuất lỗi thì không duyệt đề xuất nào

D) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 3
Thành viên đề xuất đã rời lớp, hoặc đã vào nhóm chính thức, khi giảng viên duyệt?

A) Như BR-U12-07: không cần listener; khi đọc và khi duyệt chỉ tính thành viên `ACTIVE` chưa có nhóm. Lúc duyệt, nếu trưởng nhóm không còn hiệu lực hoặc số thành viên hiệu lực dưới tối thiểu thì đề xuất đó lỗi (`PROPOSAL_INCOMPLETE`) và tự về nháp, các thành viên không hiệu lực bị bỏ khỏi đề xuất (đề xuất)

B) Như A nhưng vẫn duyệt dù dưới tối thiểu, chỉ bỏ người không hiệu lực

C) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 4
Hạn chót và lời mời hết hạn xử lý thế nào?

A) Tính khi đọc/ghi, không cần job: đợt mở khi bật, lớp `OPEN` và `now < deadline`; lời mời `PENDING` khi đợt đóng được coi và hiển thị là hết hạn, chuyển hẳn sang `EXPIRED` ở lần ghi kế tiếp chạm tới đề xuất hoặc khi đợt mở lại (đề xuất)

B) Job định kỳ (`IScheduledScanner`) đóng đợt và hủy lời mời đúng giờ

C) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 5
Giữ lịch sử đề xuất?

A) Giữ bản ghi đề xuất ở mọi trạng thái cuối (`APPROVED`, `REJECTED`, `CANCELLED`) và lời mời kèm trạng thái; sinh viên thấy đề xuất bị từ chối gần nhất kèm lý do cho tới khi tạo hoặc vào đề xuất khác; giảng viên chỉ thấy đề xuất đang hoạt động (đề xuất)

B) Xóa đề xuất và lời mời khi kết thúc, chỉ còn audit

C) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 6
Trưởng nhóm có đổi được tên đề xuất nháp không?

A) Có, khi nháp và đợt mở; tên kiểm không trùng lúc tạo/đổi và kiểm lại khi duyệt (tên nhóm chính thức có thể đã bị dùng: đề xuất lỗi `GROUP_NAME_DUPLICATE`, vẫn chờ duyệt, giảng viên từ chối hoặc trưởng nhóm rút về đổi tên) (đề xuất)

B) Không; tên cố định khi tạo

C) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 7
Khi duyệt, thành viên nhận mấy thông báo?

A) Một: thông báo "Đề xuất nhóm được duyệt" thay cho thông báo "được thêm vào nhóm" của `group.membership-changed` với những người này; sự kiện membership vẫn phát cho SSE/các phần khác nhưng không tạo thông báo trùng (đề xuất)

B) Cả hai thông báo

C) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 8
Thay đổi do hệ thống gây ra (giảng viên xếp tay làm đề xuất bị hủy hoặc về nháp, duyệt lỗi tự về nháp) có thông báo không?

A) Không; sinh viên thấy trạng thái mới trên trang Nhóm, đúng bốn loại thông báo của SG-08 (đề xuất)

B) Có, thêm thông báo cho trưởng nhóm khi đề xuất bị hủy hoặc về nháp do hệ thống

C) Other (please describe after [Answer]: tag below)

[Answer]: A

## Question 9
Đặt giao diện ở đâu?

A) Sinh viên: thêm `GroupProposalSection` vào trang Nhóm của tôi và thẻ Nhóm của tôi khi chưa có nhóm. Giảng viên: trong `ClassGroupsPanel` thêm nút và popup `SelfGroupingSettingsDialog`, khu `ProposalReviewList` (cạnh `LeaderRequestList`), cột "Đề xuất" cho sinh viên chưa có nhóm (đề xuất)

B) Trang riêng "Đăng ký nhóm" cho sinh viên và trang riêng duyệt đề xuất cho giảng viên

C) Other (please describe after [Answer]: tag below)

[Answer]: A

## Các bước thực hiện (sau khi trả lời)

- [x] `domain-entities.md`: thêm `GroupFormationSettings`, `GroupProposal`, `ProposalMember`; máy trạng thái đề xuất và lời mời (Mermaid kèm bản chữ); sự kiện mới; API mới
- [x] `business-rules.md`: thêm mục quy tắc tự chia nhóm (BR-U12-70 trở đi): đợt, đề xuất, lời mời, gửi duyệt, duyệt, tương tác với lưu bộ nhóm, quyền, audit, thông báo
- [x] `business-logic-model.md`: luồng bật đợt, tạo/mời/chấp nhận, gửi/rút, duyệt một và nhiều, dọn đề xuất khi lưu bộ nhóm; xử lý đồng thời
- [x] `frontend-components.md`: component mới, trạng thái theo vai trò, form và kiểm hợp lệ, API mỗi component dùng
- [x] Cập nhật dòng Primary stories/Requirements ở đầu bốn tệp (US-GRP-007…010, SG-01…09)
- [x] Kiểm Security (SECURITY-03/04/05/08/09/12/15) và Resiliency (RESILIENCY-04/06/10) trong phạm vi; kiểm Mermaid và liên kết

## Kết quả

Người dùng chọn A cho 9 câu (2026-11-11). Đã cập nhật bốn tệp `construction/u12-group-assignment/functional-design/`: entity, máy trạng thái, sự kiện, API (domain-entities); BR-U12-70…96 (business-rules); F16–F24 (business-logic-model); component và trạng thái màn (frontend-components).

Extension: SECURITY-03, 05, 08, 09, 15 tuân thủ (BR-U12-91…96); SECURITY-04, 12 N/A (không đổi header HTTP hay xác thực). RESILIENCY-10 tuân thủ (BR-U12-95); RESILIENCY-04, 06 N/A ở thiết kế chức năng (pipeline và health check hiện có, migration chỉ thêm).
