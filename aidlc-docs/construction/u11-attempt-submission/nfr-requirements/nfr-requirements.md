# U11 Attempt & Submission - NFR Requirements

**Bản tài liệu 2026-10-09**: UC 18, 19, 20, 21, 22, 23, 24, 25, 26, 28, 29 theo [73 UC](../../../../docs/use-cases-73.md) và screen flow `docs/G21_Diagrams.drawio` (Page-2); danh sách quiz của học liệu trên Learning Material (UC 15, màn của U05); primary stories: US-ASM-003, US-ASM-012. Quyền và supporting flows theo [current SRS contract](../../current-srs-contract.md); đây là thiết kế/kế hoạch, không xác nhận implementation mới.

## 1. Tải và hiệu năng

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U11-01 | 100 người làm bài cùng lúc, tự lưu 10 giây/lần → ≈ 10 lần lưu/giây; lưu p95 ≤ 300 ms với nội dung ≤ 1 MB. | NFR-003 |
| NFR-U11-02 | Nội dung một lượt ≤ 10 MB (JSON, gồm XML sơ đồ); body request lưu gzip từ client, Nginx `client_max_body_size 12m` cho route lưu. | BR-U09-34 |
| NFR-U11-03 | Bắt đầu lượt p95 ≤ 500 ms; nộp p95 ≤ 1 s (gồm kiểm tài liệu). | NFR-003 |
| NFR-U11-04 | Tự nộp chạy trễ ≤ 1 phút sau `deadlineAt`; ngừng giao tự nộp mọi lượt dở ≤ 1 phút. | BR-U11-23 |
| NFR-U11-05 | Student Assignments, Quiz Practice History (gộp tới 10 lớp) và danh sách quiz của học liệu p95 ≤ 500 ms. | NFR-003, BR-U11-36, 50 |

## 2. Toàn vẹn dữ liệu

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U11-10 | Bắt đầu lượt: unique `(assignment_id, account_id, attempt_no)` + partial unique một `IN_PROGRESS` mỗi `(assignment_id, account_id)`; đếm lượt trong cùng transaction. | BR-U11-03, 04 |
| NFR-U11-11 | Lưu nháp dùng `contentVersion` (UPDATE có điều kiện). | BR-U11-11 |
| NFR-U11-12 | Nộp tay và tự nộp cùng lúc: UPDATE `status = 'SUBMITTED' WHERE status = 'IN_PROGRESS'`; chỉ một cái thắng. | BR-U11-21, 23 |
| NFR-U11-13 | Bài đã nộp bất biến: service chặn, user `app` không đổi được nội dung trong `attempts` khi `SUBMITTED` (trigger). | FR-018 |
| NFR-U11-14 | Giờ nộp, hạn, trễ tính theo giờ server. | NFR-U08-11 |

## 3. Bảo mật

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U11-20 | Mọi truy cập lượt kiểm chủ sở hữu; danh sách và chi tiết kiểm ghi danh `ACTIVE` lớp `OPEN`; người khác hoặc lớp ngoài phạm vi `404`; audit truy cập bị chặn. `ADMIN`, `TEACHER`, `SUBJECT_MANAGER` bị từ chối ở API người học. | US-ASM-003 S3, BR-U11-05 |
| NFR-U11-21 | Đề trả cho người học (Assignment Detail, workspace, Quiz Taking) không có đáp án, giải thích, test ẩn, `answerGuide`, lời giải mẫu; đáp án đúng chỉ trả trên Quiz Result của lượt đã nộp khi `showCorrectAnswers = AFTER_SUBMIT`. | SEC-002, BR-U11-54 |
| NFR-U11-22 | Nội dung tài liệu kiểm và làm sạch SVG (U09) trước khi lưu. | NFR-U09-13 |
| NFR-U11-23 | Rate limit lưu nháp 30 lần/phút/người (Bucket4j + Redis). | SEC-005 |
| NFR-U11-24 | Preview DOCX chỉ cho chủ lượt Diagram Essay đang làm; xác nhận dùng `contentVersion` và cùng kiểm cấu trúc như lưu nháp, không ghi đè block giảng viên. | BR-U09-45…48 |

## 4. Kiểm thử

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U11-30 | Unit test mọi `BR-U11-xx`; ranh giới giờ (đúng `deadlineAt`, trong 30 giây ân hạn, sau ân hạn; quiz không giới hạn giờ). | NFR-004 |
| NFR-U11-31 | Integration test (tester riêng, unit không viết): hai lần bắt đầu đồng thời chỉ tạo một lượt; nộp tay và tự nộp đồng thời; ngừng giao hoặc version mới của quiz tự nộp hàng loạt. | NFR-004; quyết định 2026-10-05 |
| NFR-U11-32 | Tải thử 100 người tự lưu đồng thời trong 10 phút (k6), ghi kết quả. | NFR-003 |

## 5. Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| SECURITY-03 | Compliant | Không log nội dung bài làm |
| SECURITY-05 | Compliant | Giới hạn kích thước, kiểm tài liệu |
| SECURITY-08 | Compliant | NFR-U11-20, 21 |
| SECURITY-15 | Compliant | NFR-U11-12, 13 |
| Rule còn lại | N/A | Dùng chung backend hoặc ngoài phạm vi đồ án |
