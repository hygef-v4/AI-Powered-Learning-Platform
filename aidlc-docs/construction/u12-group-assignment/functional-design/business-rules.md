# U12 Group Assignment — business-rules

> Revision 2026-10-15 theo [unit-of-work](../../../inception/application-design/unit-of-work.md): U12 Group Assignment thuộc Iter 3, người code: Người 2; UC 53–57. Gộp U12 cũ (nhóm và trưởng nhóm) và U14 cũ (tài liệu nhóm và nộp). Soạn đề, rubric dùng U07; chấm dùng U09. Chỉ sửa tài liệu; chưa triển khai hoặc kiểm runtime.

**Primary UC:** 53, 54, 55, 56, 57. **Primary stories:** US-GRP-001, US-GRP-002, US-GRP-003, US-GRP-004, US-GRP-005, US-GRP-006. **Requirements:** FR-025, FR-026, FR-018, FR-039.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [stories](../../../inception/user-stories/stories.md), [requirements](../../../inception/requirements/requirements.md).

## 1. Nhóm của lớp (UC 57)

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U12-01 | Nhóm thuộc lớp, quản lý trong tab Students của Teacher Class Detail; mọi bài nhóm của lớp dùng chung các nhóm này. Chỉ giảng viên chính của lớp (R3/R4) tạo, sửa. Admin và Chủ nhiệm môn không dạy lớp không xem, không sửa. | UC 57, FR-025 |
| BR-U12-02 | Chỉ sinh viên ghi danh `ACTIVE` của lớp được vào nhóm; mỗi sinh viên tối đa một nhóm trong lớp. Mỗi nhóm có tên (≤ 100, không trùng trong lớp), ít nhất một thành viên và đúng một trưởng nhóm là thành viên. | US-GRP-001 S1, S2 |
| BR-U12-03 | Tạo tay: tạo nhóm, thêm, chuyển, gỡ thành viên, chọn trưởng nhóm. Chia ngẫu nhiên: nhập sĩ số tối đa 2–20; chỉ chia sinh viên chưa có nhóm, giữ nguyên nhóm đã có; các nhóm mới chênh nhau tối đa một người; trưởng nhóm chọn ngẫu nhiên; kết quả là bản xem trước để sửa rồi mới lưu. | US-GRP-001 S1, S3 |
| BR-U12-04 | Lưu cả bộ nhóm của lớp một lần kèm `version`; có chỗ mâu thuẫn thì từ chối toàn bộ và báo từng lỗi. | US-GRP-001 S2 |
| BR-U12-05 | Chỉ xóa nhóm chưa có tài liệu nhóm có nội dung hoặc bản nộp; nhóm đã dùng chỉ chuyển thành viên. Gỡ thành viên: mục người đó đang giữ được nhả (bỏ bản nháp chưa Xong), nội dung đã Xong giữ nguyên kèm tên tác giả; bản nộp đã có không đổi. | FR-026 |
| BR-U12-06 | Sửa nhóm, gửi và duyệt yêu cầu đổi trưởng nhóm chỉ khi lớp `DRAFT` hoặc `OPEN`; lớp `ARCHIVED` chỉ xem. Sinh viên chỉ thao tác ở lớp `OPEN`. | BR-U05-16 |
| BR-U12-07 | Sinh viên bị gỡ khỏi lớp không còn là thành viên hiệu lực: không tính vào sẵn sàng, không vào được tài liệu nhóm; tab Students hiện "Đã rời lớp"; lần lưu sau xóa họ khỏi nhóm. Người đó là trưởng nhóm thì nhóm bị coi là thiếu trưởng nhóm tới khi giảng viên chọn người mới; yêu cầu đang chờ của họ tự hủy. | BR-U05-25 |

## 2. Trưởng nhóm (UC 55, 57)

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U12-10 | Thành viên (kể cả trưởng nhóm) gửi yêu cầu đổi trưởng nhóm kèm lý do 10–1.000 ký tự, có thể đề cử người thay là thành viên khác trưởng nhóm hiện tại. Mỗi nhóm tối đa một yêu cầu đang chờ; người gửi hủy được khi còn chờ. | UC 55, US-GRP-002 |
| BR-U12-11 | Chỉ giảng viên chính của lớp duyệt hoặc từ chối. Duyệt thì chọn trưởng nhóm mới (mặc định người được đề cử, bắt buộc chọn khi không có đề cử); từ chối bắt buộc lý do. | US-GRP-002 S1, S2 |
| BR-U12-12 | Giảng viên đổi trưởng nhóm trực tiếp thì yêu cầu đang chờ của nhóm tự hủy. Trưởng nhóm rời nhóm thì giảng viên phải chọn trưởng nhóm mới trong cùng thao tác. | FR-025 |
| BR-U12-13 | Đổi trưởng nhóm chuyển quyền giao mục và nộp bài sang người mới; không đổi mục ai đang giữ. | US-GRP-002 S1 |

## 3. Bài nhóm (UC 53)

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U12-20 | Bài nhóm là Assignment `submissionMode = GROUP` của U07: chỉ bài của lớp, chỉ `GRADED` (D02), sơ đồ tùy chọn; soạn đề, khung, rubric bằng Assignment Form của U07. U12 thêm phần nhóm trên form (slot `AssignmentFormSection`): số nhóm, trạng thái sẵn sàng, lối vào tab Students. | UC 53, US-GRP-003 |
| BR-U12-21 | Giảng viên không giao phần cho từng sinh viên; mỗi phần của khung (heading nhỏ nhất của mỗi nhánh, U07) thành một mục, trưởng nhóm giao mục trong tài liệu nhóm. | Người dùng chốt 2026-10-04 |
| BR-U12-22 | Sẵn sàng phát hành (`GroupAssignmentSupport.checkReadiness`): lỗi chặn `CLASS_WITHOUT_GROUP`, `GROUP_WITHOUT_LEADER`, `GROUP_WITHOUT_MEMBER` (tính theo thành viên hiệu lực); cảnh báo `STUDENT_WITHOUT_GROUP`, giảng viên xác nhận thì vẫn phát hành. | FR-025 |

## 4. Tài liệu nhóm (UC 56)

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U12-30 | Khi bài mở, mỗi nhóm của lớp có một tài liệu nhóm dựng từ khung: mỗi phần thành một mục khóa theo `partId`; heading cha và block ngoài phần là phần chung, chỉ đọc. Nhóm tạo sau khi bài mở cũng có tài liệu. Không ai thêm, xóa, đổi tên hay di chuyển mục. | UC 56 |
| BR-U12-31 | Thành viên hiệu lực mở workspace, nhận, làm, xong, nhả mục của mình; trưởng nhóm thêm quyền giao mục và nộp. Giảng viên chính của lớp xem tiến độ, bản nộp và nhả khóa mục; không sửa nội dung, không giao, không nộp thay. Nhóm khác, tài khoản ngoài lớp, Admin nhận `404`. | SEC-002 |
| BR-U12-32 | Mọi thao tác sửa chỉ khi tài liệu `IN_PROGRESS` và bài còn nhận bài nộp (`submissionWindow` là `ON_TIME` hoặc `LATE`); lần lưu nháp cuối trong 30 giây ân hạn vẫn nhận; bài bị ngưng giao thì đóng ngay. | FR-007 |
| BR-U12-33 | Trưởng nhóm giao một mục cho một thành viên: mục `CLAIMED` cho người đó, ghi người giao; mục đang do người khác giữ thì nhả trước (có cảnh báo). Thành viên tự nhận mục `OPEN` hoặc `DONE`; một người giữ được nhiều mục; mỗi mục một người sửa tại một thời điểm. | FR-026, US-GRP-004 S1 |
| BR-U12-34 | Người giữ mở mục trong popup che kín trang: hiện các heading trên nhánh của phần kèm nội dung của chúng và nội dung phần, không hiện nhánh khác; trình soạn như bài cá nhân (block khung khóa). Chỉ popup này hiện theo nhánh; workspace và các màn khác hiện toàn bộ tài liệu. Tự lưu bản nháp 10 giây sau lần sửa cuối, kèm `version` của mục; chỉ người giữ thấy bản nháp. | Người dùng chốt 2026-10-04 |
| BR-U12-35 | Bấm Xong: kiểm block qua `DocumentModelPort`; nội dung nháp thành nội dung đã đăng; thêm một bản lịch sử (tác giả, thời điểm); mục `DONE`, bỏ khóa; đẩy cập nhật realtime. | US-GRP-004 S2 |
| BR-U12-36 | Nhả khóa: người giữ, trưởng nhóm hoặc giảng viên chính của lớp; người giữ rời nhóm thì tự nhả. Cảnh báo "bản nháp chưa Xong sẽ bị bỏ"; bản nháp bị xóa; mục về `DONE` nếu đã có nội dung đăng, không thì `OPEN`. | FR-026 |
| BR-U12-37 | Realtime qua SSE: thành viên đang mở workspace nhận sự kiện giao, nhận, nhả, xong, nộp, ngưng giao trong ≤ 2 giây; chỉ đẩy thay đổi đã Xong, không đồng bộ từng phím gõ; mất kết nối thì tải lại toàn bộ khi kết nối lại. | US-GRP-005 |
| BR-U12-38 | Bài bật theo dõi: workspace của từng thành viên gửi sự kiện qua `useActivityTracker`; U12 kiểm thành viên, tài liệu `IN_PROGRESS`, chính sách rồi gọi `ActivityWritePort` (loại `GROUP_DOCUMENT`, theo từng thành viên). | FR-039 |

## 5. Nộp (UC 56)

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U12-40 | Chỉ trưởng nhóm hiện tại nộp, bất kỳ lúc nào bài còn nhận bài nộp; thành viên khác không nộp thay. Còn mục chưa Xong thì cảnh báo nhưng vẫn cho nộp, phần chưa Xong không vào bản nộp; tài liệu phải qua `validateForSubmit` (đủ sơ đồ bắt buộc, sơ đồ không rỗng). | US-GRP-005 S1 |
| BR-U12-41 | Nộp chụp phần chung, nội dung đã đăng của các mục và tác giả từng mục thành bản bất biến kèm `receiptHash`, `late` nếu sau `closesAt`. Trong cùng transaction gọi `GroupEvaluationPort.onGroupSubmitted` của U09; sau commit phát `group.submitted`. | US-GRP-005 S1, BR-U03-64 |
| BR-U12-42 | Trưởng nhóm nộp lại được trước hạn; bản nộp cuối thay bản trước và được chấm; người nộp, thời điểm, mã băm của mọi lần nộp nằm trong audit. Sau khi nộp vẫn sửa được tới hạn. | FR-026 |
| BR-U12-43 | Tự nộp khi quá hạn cuối nhận bài 30 giây hoặc khi bài bị ngưng giao, nếu có thay đổi sau lần nộp cuối: mục đang `CLAIMED` được chốt bằng bản nháp đã lưu gần nhất (thêm lịch sử, `DONE`); chụp như BR-U12-41 với `AUTO_DEADLINE` hoặc `AUTO_RETIRED`; tài liệu `CLOSED`, chỉ đọc. Không phụ thuộc AI. Người đang mở workspace thấy trang khóa "Đang nộp…" rồi chuyển sang bản nộp. | US-GRP-005 S2 |
| BR-U12-44 | Workspace có thẻ Bản nộp: bản nộp cuối với thời điểm, người nộp, cách nộp, trễ, biên nhận và tác giả từng mục. Thành viên tải DOCX bản hiện tại hoặc bản nộp; giảng viên chính của lớp tải DOCX bản nộp. | UC 56 |

## 6. Xem nhóm và hỗ trợ màn khác (UC 54)

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U12-50 | My Group (phần trên Student Class Detail): tên nhóm, thành viên, trưởng nhóm, tài liệu nhóm của các bài nhóm đã mở kèm tiến độ và trạng thái nộp, yêu cầu đổi trưởng nhóm gần nhất của mình (trạng thái, lý do từ chối). Không thấy nhóm khác hay yêu cầu của người khác, chỉ biết nhóm đang có yêu cầu chờ. | UC 54, US-GRP-001 S4 |
| BR-U12-51 | Student Assignments (U08) hiện trạng thái bài nhóm qua `GroupWorkStatusPort`; Assignment Detail có nút mở workspace (slot `GroupWorkEntry`) kèm nhóm, trưởng nhóm, số mục theo trạng thái. Popup Student Detail hiện nhóm của sinh viên (slot `StudentDetailSection`). | UC 37, 38, 23 |
| BR-U12-52 | Giảng viên xem tiến độ từng nhóm theo bài nhóm trong tab Students: số mục xong, đang làm, trống, người giữ, đã nộp, trễ, cách nộp; nhả khóa mục; U09 lấy cùng dữ liệu qua `GroupSubmissionSource` cho Submission Detail và chấm. | UC 42, FR-026 |

## 7. AI, audit và lỗi

| Mã | Rule | Nguồn |
|---|---|---|
| BR-U12-60 | Bài nhóm không có tính năng AI cho người học (bài nhóm chỉ `GRADED` theo D02; D03 bỏ): không phản hồi AI, không tool đọc tài liệu nhóm cho người học. Agent của giảng viên đọc bản nộp nhóm qua tool `read_authorized_group_submission` để đề xuất chấm ở U09 (US-GRP-006). | US-GRP-004 S2, D02 |
| BR-U12-61 | Audit: tạo, sửa, xóa nhóm; chia ngẫu nhiên (khi lưu); đổi trưởng nhóm; gửi, hủy, duyệt, từ chối yêu cầu; giao mục; nhả khóa do trưởng nhóm hoặc giảng viên; nộp (tay, tự). Không audit từng lần tự lưu. | FR-014 |
| BR-U12-62 | Mọi API kiểm quyền phía server, ngoài phạm vi trả `404` và ghi audit `ACCESS_DENIED`. U01, U05, U07 không trả lời thì từ chối (fail closed). Sự kiện phát sau commit. | SEC-002 |
