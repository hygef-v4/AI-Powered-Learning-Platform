# U14 Group Document & Submission - Business Rules

## 1. Tài liệu nhóm và mục

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U14-01 | Bài nhóm là bài `DOCUMENT` làm nhóm; khi publication mở, mỗi nhóm của lớp (U12) có một tài liệu nhóm cho publication đó, dựng từ khung: mỗi heading `workSection` thành một mục chính `TEACHER`, block còn lại là phần chung khóa. | Câu 4, 5; UC 28 |
| BR-U14-02 | Chỉ trưởng nhóm thêm mục chi tiết `GROUP` (tiêu đề + vị trí) dưới một mục chính, sửa tiêu đề, đổi thứ tự trong cùng mục chính, và xóa mục `GROUP` khi mục đang `OPEN` và rỗng. Mục `TEACHER` không xóa, đổi tên hay di chuyển. | UC 16, thay đổi 2026-10-01 |
| BR-U14-03 | Chỉ thành viên của nhóm (U12) và giảng viên lớp xem tài liệu nhóm; nhóm khác không thấy. | SEC-002 |
| BR-U14-04 | Mọi thao tác sửa chỉ khi publication còn nhận bài (U08) và chưa qua hạn cuối nhận bài. | FR-007 |
| BR-U14-05 | Trưởng nhóm giao một mục lá cho một thành viên của nhóm: mục thành `CLAIMED` cho người đó, ghi `assignedBy`. Mục đang do người khác giữ thì nhả khóa người cũ trước (BR-U14-13). Thành viên vẫn tự nhận được mục lá còn `OPEN` hoặc `DONE`. | UC 16, thay đổi 2026-10-01 |
| BR-U14-06 | Mục chính đã có mục chi tiết là mục cha: không nhận/giao trực tiếp, trạng thái suy ra từ các mục chi tiết. Mục lá (mục chính không có mục chi tiết, hoặc mục chi tiết) là đơn vị nhận, làm và "Xong". | Thiết kế |

## 2. Nhận, làm và xong mục

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U14-10 | Thành viên nhận mục lá `OPEN` hoặc `DONE`; mục thành `CLAIMED` và khóa cho người đó; một người được giữ nhiều mục. | Câu 1, 6, 7 |
| BR-U14-11 | Người giữ làm mục trong trang riêng như bài `DOCUMENT` (chỉ block của mục); tự lưu vào `draftBlocks`, chỉ người giữ thấy bản nháp. | Câu 1 |
| BR-U14-12 | Bấm "Xong": kiểm block (`DocumentModelPort`), `publishedBlocks = draftBlocks`, tạo `SectionRevision`, mục → `DONE`, bỏ khóa, đẩy cập nhật realtime cho mọi người đang mở tài liệu. | Câu 1, 3, 7 |
| BR-U14-13 | Nhả khóa: người giữ tự nhả, trưởng nhóm hoặc giảng viên nhả; người giữ rời nhóm (U12) thì tự nhả. Bản nháp chưa "Xong" của người đó giữ trong lịch sử nháp, không vào tài liệu chung. | US-GRP-003 S2 |
| BR-U14-14 | Mục `CLAIMED` quá 48 giờ không lưu gì → trưởng nhóm thấy gợi ý nhả khóa hoặc giao lại (không tự nhả). | Thiết kế |
| BR-U14-15 | Thành viên bình luận trên mục `DONE` (≤ 2 000 ký tự, văn bản thuần); người nhận lại mục đánh dấu đã giải quyết. | Câu 7 |

## 3. Review và realtime

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U14-20 | Người đang mở tài liệu nhóm nhận sự kiện: mục được thêm/sửa/giao/nhận/nhả/xong, đổi trạng thái tài liệu, bình luận, bản nộp. Cập nhật ≤ 2 giây. | Câu 3 |
| BR-U14-21 | Realtime chỉ đẩy thay đổi đã "Xong" (không đồng bộ từng phím gõ); mỗi mục tại một thời điểm chỉ một người sửa nên không có xung đột soạn đồng thời. | Câu 1 |
| BR-U14-22 | Mất kết nối thì client tải lại trạng thái đầy đủ khi kết nối lại. | REL-003 |
| BR-U14-24 | Khi mọi mục lá đều `DONE`, tài liệu tự chuyển `IN_PROGRESS` → `REVIEW` và mọi thành viên được báo để cùng xem lại, bình luận. | UC 16, thay đổi 2026-10-01 |
| BR-U14-25 | Trong `REVIEW`, nhận lại, giao lại hoặc thêm mục làm tài liệu về `IN_PROGRESS`; tài liệu chỉ quay lại `REVIEW` khi mọi mục lá lại `DONE`. | UC 16, thay đổi 2026-10-01 |

## 4. Nộp bài nhóm

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U14-30 | Chỉ trưởng nhóm hiện tại (U12) nộp, và chỉ khi tài liệu ở `REVIEW`. Còn bình luận chưa giải quyết thì cảnh báo nhưng vẫn cho nộp. | Câu 8, thay đổi 2026-10-01 |
| BR-U14-31 | Nộp: chụp tài liệu (phần chung + `publishedBlocks` các mục) và `sectionAuthors` thành bản bất biến. Phát `group.submitted`. | Câu 8 |
| BR-U14-32 | Trưởng nhóm nộp lại được trước hạn (tài liệu phải về lại `REVIEW`); bản nộp cuối được chấm; mọi bản giữ lại. | US-GRP-005 S2 |
| BR-U14-33 | Hạn chung của bài (và nộp trễ theo U08); tới hạn cuối nhận bài mà chưa nộp sau lần sửa cuối → tự nộp bản hiện tại (`AUTO_DEADLINE`) bất kể trạng thái tài liệu, mục `CLAIMED` lấy `publishedBlocks` (không lấy nháp), ghi cảnh báo mục chưa xong hoặc tài liệu chưa review. Ngừng giao → `AUTO_RETIRED`. | Câu 2, 8 |
| BR-U14-34 | Sau khi nộp vẫn sửa được tới hạn (nộp lại); sau hạn tài liệu chỉ đọc. | FR-007 |
| BR-U14-35 | Thành viên và giảng viên tải DOCX bản hiện tại hoặc bản nộp (U09). | BR-U09-52 |

## 5. Hỗ trợ chấm (U15)

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U14-40 | Cung cấp cho U15: bản nộp cuối; theo từng thành viên các mục người đó là tác giả (theo `SectionRevision`) để chấm đóng góp; tài liệu chung chỉ chấm tay (không gửi AI). | US-GRP-006 S2 |

## 6. Audit

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U14-50 | Audit: thêm/sửa/xóa mục `GROUP`, giao mục, nhả khóa do trưởng nhóm/giảng viên, nộp (tay/tự). Không audit từng lần tự lưu. | FR-014 |
