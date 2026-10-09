# U14 Group Document & Submission - Business Logic Model

**Bản tài liệu 2026-10-09**: UC 27 và phần bài nhóm của UC 16, 23, 28, 37 theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); primary stories: US-GRP-004, US-GRP-005. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

Luồng màn (screen flow Page-2): Class Dashboard → Student Assignments (U11) → Assignment Detail (U11, có `GroupAssignmentPanel` của U14) → Group Essay Workspace (U14, UC 27) hoặc Submission History (U11, có `GroupSubmissionView` của U14). Popup làm phần là popup che kín trang trong Group Essay Workspace. Giảng viên chính của lớp (R3/R4): Teacher Class Detail → tab Evals (danh sách bài nộp của U15, có `GroupDocsOverviewPanel` của U14) → Submission Detail → Grading Workspace (U15).

## F1 - Khởi tạo tài liệu nhóm
1. Bài nhóm (`GROUP_ASSIGNMENT`) chỉ có ở lớp và chỉ `GRADED` (U08). Khi bài mở, U08 gọi `AssignmentLifecyclePort.onOpened` (U14 cài) trong transaction mở bài → gửi việc `GROUP_DOC_CREATE` sau commit: với mỗi nhóm của lớp (U12) tạo một dòng `group_documents` cho bài; mỗi phần của khung (`config.parts`, heading nhỏ nhất của mỗi nhánh, U09) thành một mục khóa theo `partId`; heading cha và block ngoài phần là phần chung `sharedBlocks` (BR-U14-01).
2. Nhóm tạo sau khi bài đã mở: U12 gọi `GroupChangePort.onGroupCreated` (U14 cài) → việc `GROUP_DOC_CREATE` cho nhóm đó ở mọi bài nhóm đang mở của lớp.

## F2 - Vào Group Essay Workspace (UC 23, 27)
1. Assignment Detail (U11) của bài nhóm gắn `GroupAssignmentPanel`: gọi tài liệu nhóm của mình (nhóm, trưởng nhóm, thành viên, số mục theo trạng thái, trạng thái nộp, hạn). Chưa có nhóm hoặc chưa có tài liệu thì báo "Bạn chưa thuộc nhóm nào của lớp" hoặc "Tài liệu nhóm đang được tạo".
2. Bấm "Mở tài liệu nhóm" → Group Essay Workspace: kiểm R5 và thành viên nhóm (BR-U14-03); trả phần chung, các mục theo thứ tự khung, trạng thái mục, người giữ, trạng thái tài liệu; bản nháp chỉ trả cho người giữ.
3. Mở kênh realtime của tài liệu nhóm (`groupDocumentId`).

## F3 - Tài liệu bài nhóm trong My Group (UC 16)
1. `MyGroupCard` (U12) trong Student Class Detail gắn `GroupDocsOfGroupList`: mỗi bài nhóm đang mở hoặc đã đóng của lớp một dòng (tên bài, hạn, số mục xong/đang làm/trống, đã nộp hay chưa).
2. Bấm một dòng mở Assignment Detail của bài đó (đúng screen flow), từ đó vào Group Essay Workspace.

## F4 - Trưởng nhóm giao mục
1. Mục là các phần của khung; không ai thêm, xóa, đổi tên hay di chuyển mục (BR-U14-02).
2. Trưởng nhóm hiện tại giao mục cho một thành viên (BR-U14-05): trong dòng tài liệu đang khóa đặt `CLAIMED`, `claimedBy` = thành viên, `assignedBy` = trưởng nhóm; mục đang do người khác giữ thì nhả khóa người cũ trước (BR-U14-13). Đẩy `SECTION_ASSIGNED`; audit.

## F5 - Nhận mục
1. Thành viên nhận mục `OPEN` hoặc `DONE`: `CLAIMED`, `claimedBy`; `draftBlocks = publishedBlocks` (BR-U14-10).
2. Hai người nhận cùng lúc: một người thành công, người kia nhận `409` kèm tên người giữ. Đẩy `SECTION_CLAIMED`.

## F6 - Làm mục (popup che kín trang)
1. Người giữ mở mục trong popup che kín trang: hiện các heading trên nhánh của phần (`ancestorHeadingIds`) cùng nội dung của chúng và các block của mục, không hiện nhánh khác; `DocumentEditor` chế độ `STUDENT` (block khung khóa); tự lưu `draftBlocks` 10 giây sau lần sửa cuối (có `version`) (BR-U14-11).
2. Chỉ người đang giữ mục được lưu; lưu sau hạn cuối nhận bài bị từ chối, trừ lần lưu cuối trong 30 giây ân hạn (BR-U14-04).

## F7 - Xong mục
1. Kiểm block (`DocumentModelPort.validateForSave`) → `publishedBlocks = draftBlocks`, thêm phần tử `revisions`, mục → `DONE`, bỏ khóa; popup đóng. Đẩy `SECTION_DONE` để Group Essay Workspace của mọi người cập nhật (BR-U14-12).
2. Mọi mục `DONE` không đổi trạng thái tài liệu, không gửi thông báo (BR-U14-24).

## F8 - Nhả khóa
- Người giữ, trưởng nhóm, hoặc giảng viên chính của lớp (từ `GroupDocsOverviewPanel`) bấm Nhả: hiện cảnh báo bản nháp chưa Xong sẽ bị bỏ; xác nhận thì xóa `draftBlocks`, mục về `DONE` (đã có nội dung) hoặc `OPEN`. Người giữ rời nhóm (U12 gọi `GroupChangePort.onMemberRemoved`) thì nhả tự động như vậy và đóng kênh của người đó (BR-U14-13). Đẩy `SECTION_RELEASED`; audit khi trưởng nhóm/giảng viên nhả.

## F9 - Nộp
1. Trưởng nhóm hiện tại nộp bất kỳ lúc nào khi bài còn nhận bài nộp (BR-U14-30…32); còn mục chưa xong thì cảnh báo nhưng vẫn cho nộp.
2. `GroupAutoSubmitScanner` (mỗi phút) tự nộp tài liệu `IN_PROGRESS` khi bài quá hạn cuối nhận bài 30 giây (`AUTO_DEADLINE`) hoặc bài đã ngưng giao (`AUTO_RETIRED`) (BR-U14-33): chốt các mục `CLAIMED` bằng bản nháp đã lưu gần nhất (thêm `revisions` theo người giữ, mục `DONE`), nộp nếu chưa có bản nộp sau lần sửa cuối; tài liệu → `CLOSED`.
3. Ghi `submitted_snapshot` (bản chụp bất biến, biên nhận), `submit_mode`, `submitted_at`; trong cùng transaction gọi `GroupSubmittedPort` (U15 cài: tạo/đặt lại đánh giá chờ chấm); sau commit phát `group.submitted` cho thông báo U16, đẩy `GROUP_SUBMITTED`.
4. Mọi client đang mở Group Essay Workspace khi tự nộp (đang sửa mục hoặc chỉ xem): khóa trang, người đang sửa gửi lần lưu cuối (chỉ khi hết hạn), hiện vòng chờ; nhận `GROUP_SUBMITTED` thì chuyển sang Submission History, không quay về Group Essay Workspace (BR-U14-36).

## F10 - Ngưng giao bài nhóm
1. U08 ngưng giao gọi `AssignmentLifecyclePort.onRetired` (U14 cài) trong transaction ngưng giao; U14 không ghi DB, sau commit đẩy `ASSIGNMENT_RETIRED` tới kênh của mọi tài liệu nhóm của bài.
2. Client khóa trang và hiện vòng chờ; bài đã đóng ngay khi ngưng giao nên không nhận thêm lần lưu; `GroupAutoSubmitScanner` tự nộp ở lượt quét kế tiếp (F9.2, `AUTO_RETIRED`).

## F11 - Xem bản nộp (UC 28, 37)
1. Submission History (U11) của bài nhóm hiện `GroupSubmissionView`: bản nộp cuối (bản được chấm), thời điểm, người nộp, cách nộp, trễ, biên nhận, tác giả từng mục, tải DOCX; điểm và phản hồi đã công bố do U15 hiện.
2. Submission Detail (U15) của nhóm đọc cùng bản nộp qua API/port của U14.

## F12 - Luồng phụ của giảng viên chính của lớp (UC 37)
1. `GroupDocsOverviewPanel` trong danh sách bài nộp của bài nhóm (tab Evals, U15): mỗi nhóm một dòng (số mục xong/đang làm/trống, người giữ từng mục, đã nộp, trễ, cách nộp); mở rộng dòng để thấy các mục.
2. Nhả khóa một mục (F8); mở Submission Detail của nhóm (U15) khi đã nộp. Giảng viên không sửa nội dung, không giao mục, không nộp thay.

## F13 - Cho unit khác
- `GroupSubmissionQueryPort`: U11 (trạng thái nhóm mình trên Student Assignments), U13 (nội dung bản nộp cho AI đề xuất), U15 (bản nộp cuối, tác giả từng mục), U16 (tiến độ, nhóm chưa nộp) (BR-U14-40).
