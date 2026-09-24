# U08 Assessment Core & Publication - Business Rules

## 1. Quyền

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U08-01 | Chỉ giảng viên của lớp tạo/sửa/duyệt/phát hành/ngưng bài của lớp (Chủ nhiệm môn chỉ khi chính họ là giảng viên của lớp); Chủ nhiệm môn của môn và ADMIN chỉ xem. Không có đề chung cấp môn. | FR-007, U09 Câu 9 |
| BR-U08-02 | Phát hành chỉ tới lớp thuộc phạm vi người phát hành; sai lớp → từ chối, audit. | US-ASM-001 S2 |
| BR-U08-03 | Người học chỉ thấy publication `OPEN`/`CLOSED` của lớp mình đang ghi danh; `SCHEDULED`/`RETIRED` ẩn. | FR-007 |

## 2. Soạn bài

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U08-10 | Mỗi bài một loại (`QUIZ`, `ESSAY`, `DOCUMENT`, `CODE_LAB`, `GROUP`); thành phần phải khớp loại. | Câu 1, U09 |
| BR-U08-11 | Thành phần lấy từ ngân hàng (phiên bản `ACTIVE`, ghim) hoặc là câu riêng của bài; câu riêng kiểm theo quy tắc U06. | Câu 2 |
| BR-U08-12 | Điểm từng thành phần > 0; `totalPoints` = tổng, tự tính. | Thiết kế |
| BR-U08-13 | Bài 1-200 câu (`QUIZ`), 1-20 câu cho loại khác. | Thiết kế |
| BR-U08-14 | Chỉ sửa khi `DRAFT`; sửa bài `REVIEWED` đưa về `DRAFT`. | Câu 3 |
| BR-U08-15 | Bài `DRAFT` chưa từng phát hành được xóa. | Thiết kế |

## 3. Duyệt

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U08-20 | Chính người có quyền tự duyệt sau khi xem trước; hệ thống kiểm: ≥ 1 thành phần, thành phần hợp lệ, cấu hình loại bài đủ (`TypeConfigPort`). | Câu 3, US-ASM-001 |
| BR-U08-21 | Bản do AI tạo vào bài ở `DRAFT`, `origin = AI`, giữ tham chiếu nguồn; phải duyệt như bài thường. | FR-006 |
| BR-U08-22 | Chưa `REVIEWED` → không phát hành được. | US-ASM-001 S2 |

## 4. Phát hành

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U08-30 | Mỗi lần phát hành một lớp, lịch riêng; phát hành cùng bài cho lớp khác là lần phát hành khác. | Câu 8 |
| BR-U08-31 | `opensAt < closesAt`; `maxAttempts` 1-10; lớp phải `OPEN`. Bài `GROUP` chỉ phát hành khi U12 báo bộ nhóm sẵn sàng (`GroupReadinessPort`, `C`). | FR-007, U12 |
| BR-U08-32 | Tùy chọn nộp trễ: `allowLate` + `lateUntil` (≤ `closesAt` + 30 ngày). Nộp sau `closesAt` được đánh dấu trễ; sau `lateUntil` không nhận. | Câu 7 |
| BR-U08-33 | Phát hành lần đầu chuyển version sang `LOCKED`: nội dung, thành phần, điểm của version đó **không sửa được nữa**, kể cả khi chưa ai làm; thay đổi bằng version mới (BR-U08-43) hoặc nhân bản. | Câu 4, 6 |
| BR-U08-34 | Lịch của publication sửa được khi `SCHEDULED`; khi `OPEN` chỉ được kéo dài `closesAt`/`lateUntil`, không rút ngắn, không đổi `maxAttempts`. | Thiết kế |
| BR-U08-35 | Khi publication chuyển `OPEN`, phát event `ASSIGNMENT_OPENED`; U16 báo trong app và email cho người học của lớp. | Câu 8 |
| BR-U08-36 | Mở/đóng theo lịch tự động (job U02), sai lệch ≤ 1 phút. | Thiết kế |

## 5. Ngưng giao và nhân bản

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U08-40 | Ngưng giao: publication → `RETIRED`, lý do bắt buộc; người học không bắt đầu/nộp thêm; bài đã nộp giữ nguyên; phát event `ASSIGNMENT_RETIRED` (U11 xử lý lượt đang dở); audit. | Câu 4 |
| BR-U08-41 | Nhân bản: tạo bài `DRAFT` mới cùng phạm vi, sao chép thành phần (ghim cùng phiên bản ngân hàng, sao chép câu riêng), `origin = CLONE`, `sourceAssignmentId`; audit. | Câu 5 |
| BR-U08-42 | Lưu trữ bài `LOCKED` khi mọi publication đã `CLOSED`/`RETIRED`: ẩn khỏi danh sách mặc định. | Thiết kế |
| BR-U08-43 | Khi mọi publication của version mới nhất đã `CLOSED`/`RETIRED` (không còn `SCHEDULED`/`OPEN`), bấm "Sửa" tạo version kế tiếp (`versionNo + 1`, cùng `stableKey`, `DRAFT`, `origin = NEW_VERSION`) sao chép thành phần và cấu hình; version cũ vẫn `LOCKED` cho bài nộp cũ. Version mới duyệt và phát hành như bài mới. | U10 Câu 5, 6 |
| BR-U08-44 | Mỗi `stableKey` tối đa một version `DRAFT`. | Thiết kế |

## 6. Audit

| Mã | Quy tắc | Nguồn |
|---|---|---|
| BR-U08-50 | Audit: duyệt, phát hành (actor, lớp, lịch), sửa lịch, ngưng giao, nhân bản, tạo version mới, xóa nháp, phát hành bị từ chối vì sai phạm vi. | FR-014 |
