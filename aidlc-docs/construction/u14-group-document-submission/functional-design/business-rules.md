# U14 Group Document & Submission - Business Rules

**Bản tài liệu 2026-10-09**: UC 27 và phần bài nhóm của UC 16, 23, 28, 37 theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); primary stories: US-GRP-004, US-GRP-005. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

## 1. Tài liệu nhóm và mục

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U14-01 | Bài nhóm (`GROUP_ASSIGNMENT`) là bài `DOCUMENT` làm nhóm, chỉ có ở lớp và chỉ `GRADED` (U08). Khi bài mở, mỗi nhóm của lớp (U12) có một tài liệu nhóm cho bài đó, dựng từ khung: mỗi phần của khung (`config.parts`, heading nhỏ nhất của mỗi nhánh, BR-U09-25) thành một mục khóa theo `partId`; heading cha và block không thuộc phần nào là phần chung (`sharedBlocks`), khóa. | UC 27, 45; người dùng chốt 2026-10-04, 2026-10-09 |
| BR-U14-02 | Không ai trong nhóm, kể cả trưởng nhóm, được thêm, xóa, đổi tên hay di chuyển mục (phần của khung); phần chung của người soạn chỉ đọc. Không có mục chi tiết. | UC 27; người dùng chốt 2026-10-04 |
| BR-U14-03 | Thành viên hiện tại của nhóm (U12) là sinh viên ghi danh `ACTIVE` của lớp `OPEN` (R5) mới mở Group Essay Workspace, nhận/làm/xong/nhả mục của mình; trưởng nhóm hiện tại thêm quyền giao mục và nộp. Giảng viên chính của lớp (R3/R4) chỉ xem tiến độ, bản nộp và nhả khóa mục; không sửa nội dung, không giao mục, không nộp thay. Nhóm khác, tài khoản ngoài lớp và `ADMIN` nhận `404`. | SEC-002; current SRS contract |
| BR-U14-04 | Mọi thao tác sửa (giao, nhận, lưu nháp, xong, nhả của thành viên) chỉ khi tài liệu `IN_PROGRESS` và bài còn nhận bài nộp (`isSubmissionOpen` của U08 là `ON_TIME` hoặc `LATE`); lần lưu nháp cuối gửi trong 30 giây ân hạn sau hạn cuối nhận bài vẫn được nhận (như BR-U11-14). Bài ngưng giao thì đóng ngay, không có ân hạn. | FR-007 |
| BR-U14-05 | Trưởng nhóm giao một mục cho một thành viên của nhóm: mục thành `CLAIMED` cho người đó, ghi `assignedBy`. Mục đang do người khác giữ thì nhả khóa người cũ trước (BR-U14-13). Thành viên vẫn tự nhận được mục còn `OPEN` hoặc `DONE`. | UC 27; US-GRP-004 S1b |

## 2. Nhận, làm và xong mục

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U14-10 | Thành viên nhận mục `OPEN` hoặc `DONE`; mục thành `CLAIMED` và khóa cho người đó; một người được giữ nhiều mục. | UC 27; US-GRP-004 S1 |
| BR-U14-11 | Người giữ mở mục trong popup che kín trang; popup hiện các heading trên nhánh của phần cùng nội dung của chúng và nội dung phần, không hiện nhánh khác (ví dụ phần H3.1 hiện H1, H2.1, H3.1, không hiện H2.2, H3.2); trình soạn như bài `DOCUMENT` (block khung khóa). Cơ chế này chỉ có trong popup làm phần; Group Essay Workspace, Submission History và Submission Detail hiện toàn bộ tài liệu; tự lưu vào `draftBlocks`, chỉ người giữ thấy bản nháp. | Người dùng chốt 2026-10-04; BR-U09-25 |
| BR-U14-12 | Bấm "Xong": kiểm block (`DocumentModelPort`), `publishedBlocks = draftBlocks`, thêm một phần tử `revisions`, mục → `DONE`, bỏ khóa, đóng popup và đẩy cập nhật realtime để Group Essay Workspace của mọi người đang mở tài liệu hiện nội dung mới. | US-GRP-004 S2 |
| BR-U14-13 | Nhả khóa: người giữ tự nhả, trưởng nhóm hoặc giảng viên chính của lớp nhả; người giữ rời nhóm (U12) thì tự nhả. Trước khi nhả bằng tay, hệ thống cảnh báo "bản nháp chưa Xong sẽ bị bỏ". Khi nhả, `draftBlocks` bị xóa, không vào tài liệu chung; mục giữ `publishedBlocks` và về `DONE` nếu đã có nội dung, không thì `OPEN`. | FR-026; người dùng chốt 2026-10-04 |

## 3. Realtime

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U14-20 | Thành viên đang mở Group Essay Workspace nhận sự kiện: mục được giao/nhận/nhả/xong, bản nộp, bài ngưng giao. Cập nhật ≤ 2 giây. | US-GRP-005 S1 |
| BR-U14-21 | Realtime chỉ đẩy thay đổi đã "Xong" (không đồng bộ từng phím gõ); mỗi mục tại một thời điểm chỉ một người sửa nên không có xung đột soạn đồng thời. | US-GRP-004 |
| BR-U14-22 | Mất kết nối thì client tải lại trạng thái đầy đủ khi kết nối lại. | REL-003 |
| BR-U14-24 | Không có bước review: khi mọi mục đều `DONE`, tài liệu không đổi trạng thái và không gửi thông báo đọc lại. | US-GRP-004 S2 |

## 4. Nộp bài nhóm

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U14-30 | Chỉ trưởng nhóm hiện tại (U12) nộp, bất kỳ lúc nào khi bài còn nhận bài nộp. Còn mục chưa xong thì cảnh báo nhưng vẫn cho nộp; phần chưa "Xong" không vào bản nộp tay. | UC 27; US-GRP-005 S2 |
| BR-U14-31 | Nộp: chụp tài liệu (phần chung + `publishedBlocks` các mục) và `sectionAuthors` thành bản bất biến kèm biên nhận (`receiptHash`), `late` nếu sau `closesAt`. Phát `group.submitted`. | US-GRP-005 S2 |
| BR-U14-32 | Trưởng nhóm nộp lại được trước hạn; bản nộp cuối ghi đè `submitted_snapshot` và được chấm; người nộp, thời điểm, `receiptHash` của mọi lần nộp nằm trong audit. | US-GRP-005 S2 |
| BR-U14-33 | Hạn chung của bài (và nộp trễ theo U08). Quá hạn cuối nhận bài 30 giây (để nhận lần lưu cuối của client), hoặc khi bài bị ngưng giao, hệ thống tự nộp bản hiện tại (`AUTO_DEADLINE`, `AUTO_RETIRED`) nếu chưa có bản nộp sau lần sửa cuối. Mục đang `CLAIMED` được chốt bằng bản nháp đã lưu gần nhất của người giữ (thêm `revisions`, mục `DONE`), nên phần đang làm của thành viên được nộp theo. Tài liệu chuyển `CLOSED`. | US-GRP-005 S3; người dùng chốt 2026-10-04 |
| BR-U14-34 | Sau khi trưởng nhóm nộp vẫn sửa được tới hạn (nộp lại); tài liệu `CLOSED` chỉ đọc. | FR-026 |
| BR-U14-35 | Thành viên tải DOCX bản hiện tại hoặc bản nộp; giảng viên chính của lớp tải DOCX bản nộp (U09). | BR-U09-52 |
| BR-U14-36 | Khi tự nộp (hết hạn hoặc ngưng giao), mọi thành viên đang mở Group Essay Workspace, dù đang sửa mục trong popup hay chỉ xem: client khóa trang (khi hết hạn người đang sửa gửi lần lưu cuối) và hiện vòng chờ "Đang nộp…"; tự nộp xong (sự kiện `GROUP_SUBMITTED`) chuyển sang Submission History, không quay về Group Essay Workspace. | US-GRP-005 S3 |
| BR-U14-37 | Submission History (UC 28) của bài nhóm hiện bản nộp cuối của nhóm (bản được chấm) với thời điểm, người nộp, cách nộp, trễ, biên nhận và tác giả từng mục; các lần nộp trước chỉ còn trong audit. | UC 28; BR-U14-32 |

## 5. Hỗ trợ màn khác

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U14-40 | Cung cấp cho U15: bản nộp cuối (tài liệu chung, chấm như bài `DOCUMENT` theo rubric từng phần, có thể nhờ AI đề xuất) và tác giả từng mục (khóa theo `partId`) để tham khảo. | US-GRP-006; người dùng chốt 2026-10-04 |
| BR-U14-41 | Assignment Detail (UC 23) của bài nhóm hiện nhóm của mình, trưởng nhóm, số mục theo trạng thái, trạng thái nộp và nút mở Group Essay Workspace; Student Assignments (UC 22) hiện trạng thái nộp của nhóm mình cho mỗi bài nhóm. | UC 22, 23; screen flow Page-2 |
| BR-U14-42 | My Group (UC 16) liệt kê tài liệu của các bài nhóm của lớp với tiến độ và trạng thái nộp; mỗi dòng mở Assignment Detail của bài đó. Chỉ thành viên nhóm thấy. | UC 16 |
| BR-U14-43 | Giảng viên chính của lớp xem tiến độ các nhóm của bài nhóm trong danh sách bài nộp (UC 37): số mục xong/đang làm/trống, người giữ, đã nộp, trễ, cách nộp; nhả khóa mục; mở Submission Detail của nhóm đã nộp. | UC 37; FR-026 |

## 6. Audit

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U14-50 | Audit: giao mục, nhả khóa do trưởng nhóm/giảng viên, nộp (tay/tự), truy cập tài liệu nhóm khác bị chặn. Không audit từng lần tự lưu. | FR-014 |
