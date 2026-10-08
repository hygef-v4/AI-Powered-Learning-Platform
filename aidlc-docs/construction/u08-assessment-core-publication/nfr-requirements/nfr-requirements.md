# U08 Assessment Core & Publication - NFR Requirements

**Bản tài liệu 2026-10-08**: UC 38; primary stories: US-ASM-001. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

## 1. Hiệu năng

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U08-01 | Danh sách bài của lớp p95 ≤ 300 ms; trình soạn bài 200 câu tải p95 ≤ 800 ms. | NFR-003 |
| NFR-U08-02 | `isSubmissionOpen` p95 ≤ 20 ms (U11 gọi mỗi lần nộp). | NFR-003 |
| NFR-U08-03 | Mở/đóng theo lịch trễ ≤ 1 phút so với giờ đặt. | BR-U08-36 |

## 2. Thời gian

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U08-10 | Lưu mọi thời điểm `timestamptz` (UTC); giao diện nhập/hiển thị giờ `Asia/Ho_Chi_Minh`. | Thiết kế |
| NFR-U08-11 | Quyết định đúng hạn/trễ/đóng dựa trên giờ server khi nhận request, không tin giờ client. | FR-007 |

## 3. Toàn vẹn dữ liệu

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U08-20 | Bài từ `SCHEDULED` trở đi: service chặn mọi sửa câu/điểm; test bảo đảm. | BR-U08-33 |
| NFR-U08-21 | Mọi thao tác sửa bài khóa dòng `assignments` (`SELECT ... FOR UPDATE`) rồi kiểm trạng thái trong cùng transaction; hai người sửa cùng lúc chạy lần lượt; bài không còn `DRAFT` → `409`. Cột `version` là số version của bài, không dùng làm khóa lạc quan. | Thiết kế |
| NFR-U08-22 | Mỗi bài thuộc đúng một lớp (bài của lớp) hoặc một môn (template): CHECK đúng một trong `class_id`, `subject_id` khác NULL. | BR-U08-30 |
| NFR-U08-23 | Chuyển trạng thái theo lịch bằng UPDATE có điều kiện trạng thái cũ và mốc giờ (scanner chạy lại không đổi sai). | BR-U08-36 |
| NFR-U08-24 | Mở bài/ngưng giao gọi `AssignmentLifecyclePort` trong transaction (U11, U14 ghi dòng hoặc gửi việc của mình); event `assignment.opened` gửi sau commit qua U03 chỉ cho thông báo. | BR-U08-35, 40 |

## 4. Bảo mật

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U08-30 | API người học không bao giờ trả đáp án, `answerGuide`, test ẩn (dùng `QuestionStudentView` của U06, câu riêng lọc tương tự). | SEC-002 |
| NFR-U08-31 | Kiểm phạm vi lớp mọi endpoint; ngoài phạm vi `404`; phát hành sai lớp audit. | SEC-002, BR-U08-02 |
| NFR-U08-32 | Markdown hướng dẫn và câu hỏi hiển thị đã làm sạch. | SEC-003 |

## 5. Kiểm thử

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U08-40 | Unit test mọi `BR-U08-xx`; test ranh giới giờ (đúng `closes_at`, `late_until`). | NFR-004 |
| NFR-U08-41 | Integration test scanner mở/đóng chạy lặp không đổi sai trạng thái; đổi lịch rồi chạy scanner. | NFR-004 |

## 6. Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| SECURITY-03 | Compliant | Không log nội dung đáp án |
| SECURITY-05 | Compliant | NFR-U08-32, kiểm lịch/đầu vào |
| SECURITY-08 | Compliant | NFR-U08-30, 31 |
| SECURITY-15 | Compliant | NFR-U08-23 |
| Rule còn lại | N/A | Dùng chung backend hoặc ngoài phạm vi đồ án |
