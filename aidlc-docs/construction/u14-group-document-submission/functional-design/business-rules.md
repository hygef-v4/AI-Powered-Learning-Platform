# U14 Group Document & Submission - Business Rules

**Bản tài liệu 2026-10-08**: UC 23; primary stories: US-GRP-004, US-GRP-005. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

## 1. Tài liệu nhóm và mục

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U14-01 | Bài nhóm là bài `DOCUMENT` làm nhóm; khi bài mở, mỗi nhóm của lớp (U12) có một tài liệu nhóm cho bài đó, dựng từ khung: mỗi phần của khung (`config.parts`, BR-U09-25) thành một mục; block không thuộc phần nào là phần chung (`sharedBlocks`), khóa. | Câu 4, 5; UC 43; người dùng chốt 2026-10-04 |
| BR-U14-02 | Không ai trong nhóm, kể cả trưởng nhóm, được thêm, xóa, đổi tên hay di chuyển mục (phần của khung); phần chung của giảng viên chỉ đọc. Không có mục chi tiết. | UC 23; người dùng chốt 2026-10-04 |
| BR-U14-03 | Chỉ thành viên của nhóm (U12) và giảng viên lớp xem tài liệu nhóm; nhóm khác không thấy. | SEC-002 |
| BR-U14-04 | Mọi thao tác sửa chỉ khi bài còn nhận bài nộp (U08) và chưa qua hạn cuối nhận bài. | FR-007 |
| BR-U14-05 | Trưởng nhóm giao một mục cho một thành viên của nhóm: mục thành `CLAIMED` cho người đó, ghi `assignedBy`. Mục đang do người khác giữ thì nhả khóa người cũ trước (BR-U14-13). Thành viên vẫn tự nhận được mục còn `OPEN` hoặc `DONE`. | UC 23, thay đổi 2026-10-01 |

## 2. Nhận, làm và xong mục

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U14-10 | Thành viên nhận mục `OPEN` hoặc `DONE`; mục thành `CLAIMED` và khóa cho người đó; một người được giữ nhiều mục. | Câu 1, 6, 7 |
| BR-U14-11 | Người giữ mở mục trong popup che kín trang; popup hiện các heading trên nhánh của phần cùng nội dung của chúng và nội dung phần, không hiện nhánh khác (ví dụ phần H3.1 hiện H1, H2.1, H3.1, không hiện H2.2, H3.2); trình soạn như bài `DOCUMENT`. Cơ chế này chỉ có trong popup làm phần được giao; Assignment Workspace, trang bài đã nộp và trang chấm hiện toàn bộ tài liệu; tự lưu vào `draftBlocks`, chỉ người giữ thấy bản nháp. | Người dùng chốt 2026-10-04 |
| BR-U14-12 | Bấm "Xong": kiểm block (`DocumentModelPort`), `publishedBlocks = draftBlocks`, thêm một phần tử `revisions`, mục → `DONE`, bỏ khóa, đóng popup và đẩy cập nhật realtime để Assignment Workspace của mọi người đang mở tài liệu hiện nội dung mới. | Câu 1, 3, 7; người dùng chốt 2026-10-04 |
| BR-U14-13 | Nhả khóa: người giữ tự nhả, trưởng nhóm hoặc giảng viên nhả; người giữ rời nhóm (U12) thì tự nhả. Trước khi nhả bằng tay, hệ thống cảnh báo "bản nháp chưa Xong sẽ bị bỏ". Khi nhả, `draftBlocks` bị xóa, không vào tài liệu chung; mục giữ `publishedBlocks` và về `DONE` nếu đã có nội dung, không thì `OPEN`. | US-GRP-003 S2; người dùng chốt 2026-10-04 |

## 3. Realtime

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U14-20 | Người đang mở tài liệu nhóm nhận sự kiện: mục được giao/nhận/nhả/xong, đổi trạng thái tài liệu, bản nộp. Cập nhật ≤ 2 giây. | Câu 3 |
| BR-U14-21 | Realtime chỉ đẩy thay đổi đã "Xong" (không đồng bộ từng phím gõ); mỗi mục tại một thời điểm chỉ một người sửa nên không có xung đột soạn đồng thời. | Câu 1 |
| BR-U14-22 | Mất kết nối thì client tải lại trạng thái đầy đủ khi kết nối lại. | REL-003 |
| BR-U14-24 | Không có bước review: khi mọi mục đều `DONE`, tài liệu không đổi trạng thái và không gửi thông báo đọc lại. | Người dùng chốt 2026-10-04 |

## 4. Nộp bài nhóm

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U14-30 | Chỉ trưởng nhóm hiện tại (U12) nộp, bất kỳ lúc nào khi bài còn nhận bài nộp. Còn mục chưa xong thì cảnh báo nhưng vẫn cho nộp; phần chưa "Xong" không vào bản nộp tay. | Người dùng chốt 2026-10-04 |
| BR-U14-31 | Nộp: chụp tài liệu (phần chung + `publishedBlocks` các mục) và `sectionAuthors` thành bản bất biến. Phát `group.submitted`. | Câu 8 |
| BR-U14-32 | Trưởng nhóm nộp lại được trước hạn; bản nộp cuối ghi đè `submitted_snapshot` và được chấm; người nộp, thời điểm, `receiptHash` của mọi lần nộp nằm trong audit. | US-GRP-005 S2 |
| BR-U14-33 | Hạn chung của bài (và nộp trễ theo U08). Quá hạn cuối nhận bài 30 giây (để nhận lần lưu cuối của client), hoặc khi bài bị ngưng giao, hệ thống tự nộp bản hiện tại (`AUTO_DEADLINE`, `AUTO_RETIRED`) nếu chưa có bản nộp sau lần sửa cuối. Mục đang `CLAIMED` được chốt bằng bản nháp đã lưu gần nhất của người giữ (thêm `revisions`, mục `DONE`), nên phần đang làm của thành viên được nộp theo. Tài liệu chuyển `CLOSED`. | Câu 2, 8; người dùng chốt 2026-10-04 |
| BR-U14-34 | Sau khi trưởng nhóm nộp vẫn sửa được tới hạn (nộp lại); tài liệu `CLOSED` chỉ đọc. | FR-007 |
| BR-U14-35 | Thành viên và giảng viên tải DOCX bản hiện tại hoặc bản nộp (U09). | BR-U09-52 |
| BR-U14-36 | Khi tự nộp (hết hạn hoặc ngưng giao), mọi thành viên đang mở tài liệu nhóm, dù đang sửa mục trong popup hay chỉ xem Assignment Workspace: client khóa trang (người đang sửa thì gửi lần lưu cuối) và hiện vòng chờ "Đang nộp…"; tự nộp xong (sự kiện `GROUP_SUBMITTED`) chuyển sang trang bài đã nộp (Submission History), không quay về Assignment Workspace. | Người dùng chốt 2026-10-04 |

## 5. Hỗ trợ chấm (U15)

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U14-40 | Cung cấp cho U15: bản nộp cuối (tài liệu chung, chấm như bài `DOCUMENT`, có thể nhờ AI đề xuất) và tác giả từng mục để tham khảo. | US-GRP-006; người dùng chốt 2026-10-04 |

## 6. Audit

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U14-50 | Audit: giao mục, nhả khóa do trưởng nhóm/giảng viên, nộp (tay/tự). Không audit từng lần tự lưu. | FR-014 |
