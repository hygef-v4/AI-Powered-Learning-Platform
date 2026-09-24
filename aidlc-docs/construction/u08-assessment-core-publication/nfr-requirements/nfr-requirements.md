# U08 Assessment Core & Publication - NFR Requirements

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
| NFR-U08-20 | Bài `LOCKED`: service chặn mọi sửa thành phần/điểm; test bảo đảm. | BR-U08-33 |
| NFR-U08-21 | Khóa lạc quan (`version`) cho bài và publication. | Thiết kế |
| NFR-U08-22 | Partial unique: một publication chưa `RETIRED` mỗi `(assignment, class)`. | BR-U08-30 |
| NFR-U08-23 | Chuyển trạng thái publication bằng UPDATE có điều kiện trạng thái cũ (job chạy lại không đổi sai). | BR-U08-36 |
| NFR-U08-24 | Event `ASSIGNMENT_OPENED/CLOSED/RETIRED` gửi sau commit qua U02. | BR-U08-35, 40 |

## 4. Bảo mật

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U08-30 | API người học không bao giờ trả đáp án, `answerGuide`, test ẩn (dùng `QuestionLearnerView` của U06, câu riêng lọc tương tự). | SEC-002 |
| NFR-U08-31 | Kiểm phạm vi lớp mọi endpoint; ngoài phạm vi `404`; phát hành sai lớp audit. | SEC-002, BR-U08-02 |
| NFR-U08-32 | Markdown hướng dẫn và câu hỏi hiển thị đã làm sạch. | SEC-003 |

## 5. Kiểm thử

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U08-40 | Unit test mọi `BR-U08-xx`; test ranh giới giờ (đúng `closesAt`, `lateUntil`). | NFR-004 |
| NFR-U08-41 | Integration test job mở/đóng chạy lặp không đổi sai trạng thái; đổi lịch sau khi đã tạo job. | NFR-004 |

## 6. Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| SECURITY-03 | Compliant | Không log nội dung đáp án |
| SECURITY-05 | Compliant | NFR-U08-32, kiểm lịch/đầu vào |
| SECURITY-08 | Compliant | NFR-U08-30, 31 |
| SECURITY-15 | Compliant | NFR-U08-23 |
| Rule còn lại | N/A | Dùng chung backend hoặc ngoài phạm vi đồ án |
