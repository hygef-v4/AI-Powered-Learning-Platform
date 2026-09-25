# U14 Group Document & Submission - Business Rules

## 1. Tài liệu nhóm và mục

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U14-01 | Bài nhóm là bài `DOCUMENT` làm nhóm; khi publication mở, mỗi nhóm có một tài liệu nhóm dựng từ khung: mỗi heading `workSection` thành một mục `TEACHER`, block còn lại là phần chung khóa. | Câu 4, 5 |
| BR-U14-02 | Nhóm thêm mục `GROUP` (tiêu đề + vị trí); chỉ trưởng nhóm xóa/đổi thứ tự mục `GROUP` khi mục đang `OPEN` và rỗng. Mục `TEACHER` không xóa/di chuyển. | Câu 5 |
| BR-U14-03 | Chỉ thành viên của nhóm (U12) và giảng viên lớp xem tài liệu nhóm; nhóm khác không thấy. | SEC-002 |
| BR-U14-04 | Mọi thao tác sửa chỉ khi publication còn nhận bài (U08) và nhóm chưa có bản nộp cuối sau hạn. | FR-007 |

## 2. Nhận, làm và xong mục

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U14-10 | Thành viên nhận mục `OPEN` hoặc `IN_REVIEW`; mục thành `CLAIMED` và khóa cho người đó; một người được nhận nhiều mục. | Câu 1, 6, 7 |
| BR-U14-11 | Người nhận làm mục trong trang riêng như bài `DOCUMENT` (chỉ block của mục); tự lưu vào `draftBlocks`, chỉ người nhận thấy bản nháp. | Câu 1 |
| BR-U14-12 | Bấm "Xong": kiểm block (`DocumentModelPort`), `publishedBlocks = draftBlocks`, tạo `SectionRevision`, mục → `IN_REVIEW`, bỏ khóa, đẩy cập nhật realtime cho mọi người đang mở tài liệu. | Câu 1, 3, 7 |
| BR-U14-13 | Nhả khóa: người nhận tự nhả, trưởng nhóm hoặc giảng viên nhả; người nhận rời nhóm (U12) thì tự nhả. Bản nháp chưa "Xong" của người đó giữ trong lịch sử nháp, không vào tài liệu chung. | US-GRP-003 S2 |
| BR-U14-14 | Mục `CLAIMED` quá 48 giờ không lưu gì → trưởng nhóm thấy gợi ý nhả khóa (không tự nhả). | Thiết kế |
| BR-U14-15 | Thành viên bình luận trên mục `IN_REVIEW` (≤ 2 000 ký tự, văn bản thuần); người nhận lại mục đánh dấu đã giải quyết. | Câu 7 |

## 3. Realtime

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U14-20 | Người đang mở tài liệu nhóm nhận sự kiện: mục được nhận/nhả/xong, mục mới, bình luận, bản nộp. Cập nhật ≤ 2 giây. | Câu 3 |
| BR-U14-21 | Realtime chỉ đẩy thay đổi đã "Xong" (không đồng bộ từng phím gõ); mỗi mục tại một thời điểm chỉ một người sửa nên không có xung đột soạn đồng thời. | Câu 1 |
| BR-U14-22 | Mất kết nối thì client tải lại trạng thái đầy đủ khi kết nối lại. | REL-003 |

## 4. Nộp bài nhóm

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U14-30 | Chỉ trưởng nhóm hiện tại (U12) nộp; cảnh báo nếu còn mục `OPEN` hoặc `CLAIMED` nhưng vẫn cho nộp. | Câu 8 |
| BR-U14-31 | Nộp: chụp tài liệu (phần chung + `publishedBlocks` các mục) và `sectionAuthors` thành bản bất biến; mục `CLAIMED` lấy `publishedBlocks` (không lấy nháp). Phát `u14.group.submitted`. | Câu 8 |
| BR-U14-32 | Trưởng nhóm nộp lại được trước hạn; bản nộp cuối được chấm; mọi bản giữ lại. | US-GRP-005 S2 |
| BR-U14-33 | Hạn chung của bài (và nộp trễ theo U08); tới hạn cuối nhận bài mà chưa nộp sau lần sửa cuối → tự nộp bản hiện tại (`AUTO_DEADLINE`), ghi cảnh báo mục chưa xong. Ngừng giao → `AUTO_RETIRED`. | Câu 2, 8 |
| BR-U14-34 | Sau khi nộp vẫn sửa được tới hạn (nộp lại); sau hạn tài liệu chỉ đọc. | FR-007 |
| BR-U14-35 | Thành viên và giảng viên tải DOCX bản hiện tại hoặc bản nộp (U09). | BR-U09-52 |

## 5. Hỗ trợ chấm (U15)

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U14-40 | Cung cấp cho U15: bản nộp cuối; theo từng thành viên các mục người đó là tác giả (theo `SectionRevision`) để chấm đóng góp; tài liệu chung chỉ chấm tay (không gửi AI). | US-GRP-006 S2 |

## 6. Audit

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U14-50 | Audit: nhả khóa do trưởng nhóm/giảng viên, thêm/xóa mục `GROUP`, nộp (tay/tự). Không audit từng lần tự lưu. | FR-014 |
