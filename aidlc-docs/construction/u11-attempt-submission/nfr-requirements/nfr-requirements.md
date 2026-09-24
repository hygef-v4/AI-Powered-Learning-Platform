# U11 Attempt & Submission - NFR Requirements

## 1. Tải và hiệu năng

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U11-01 | 100 người làm bài cùng lúc, tự lưu 10 giây/lần → ≈ 10 lần lưu/giây; lưu p95 ≤ 300 ms với nội dung ≤ 1 MB. | NFR-003 |
| NFR-U11-02 | Nội dung một lượt ≤ 10 MB (JSON, gồm XML sơ đồ); body request lưu gzip từ client, Nginx `client_max_body_size 12m` cho route lưu. | BR-U09-34 |
| NFR-U11-03 | Bắt đầu lượt p95 ≤ 500 ms; nộp p95 ≤ 1 s (gồm kiểm tài liệu). | NFR-003 |
| NFR-U11-04 | Tự nộp chạy trễ ≤ 1 phút sau `deadlineAt`; ngừng giao tự nộp mọi lượt dở ≤ 1 phút. | BR-U11-23 |

## 2. Toàn vẹn dữ liệu

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U11-10 | Bắt đầu lượt: unique `(publication_id, learner_id, attempt_no)` + partial unique một `IN_PROGRESS` mỗi `(publication_id, learner_id)`; đếm lượt trong cùng transaction. | BR-U11-03, 04 |
| NFR-U11-11 | Lưu nháp dùng `contentVersion` (UPDATE có điều kiện). | BR-U11-11 |
| NFR-U11-12 | Nộp tay và tự nộp cùng lúc: UPDATE `status = 'SUBMITTED' WHERE status = 'IN_PROGRESS'`; chỉ một cái thắng. | BR-U11-21, 23 |
| NFR-U11-13 | Bài đã nộp bất biến: service chặn, user `app` không UPDATE được `submission_contents` khi `SUBMITTED` (trigger). | FR-018 |
| NFR-U11-14 | Giờ nộp, hạn, trễ tính theo giờ server. | NFR-U08-11 |

## 3. Bảo mật

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U11-20 | Mọi truy cập lượt kiểm chủ sở hữu; người khác `404`; audit truy cập bị chặn. | US-ASM-003 S3 |
| NFR-U11-21 | Đề trả cho người học không có đáp án, test ẩn, `answerGuide`. | SEC-002 |
| NFR-U11-22 | Nội dung tài liệu kiểm và làm sạch SVG (U09) trước khi lưu. | NFR-U09-13 |
| NFR-U11-23 | Rate limit lưu nháp 30 lần/phút/người (Bucket4j + Redis). | SEC-005 |

## 4. Kiểm thử

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U11-30 | Unit test mọi `BR-U11-xx`; ranh giới giờ (đúng `deadlineAt`, trong 30 giây ân hạn, sau ân hạn). | NFR-004 |
| NFR-U11-31 | Integration test: hai lần bắt đầu đồng thời chỉ tạo một lượt; nộp tay và tự nộp đồng thời; ngừng giao tự nộp hàng loạt. | NFR-004 |
| NFR-U11-32 | Tải thử 100 người tự lưu đồng thời trong 10 phút (k6), ghi kết quả. | NFR-003 |

## 5. Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| SECURITY-03 | Compliant | Không log nội dung bài làm |
| SECURITY-05 | Compliant | Giới hạn kích thước, kiểm tài liệu |
| SECURITY-08 | Compliant | NFR-U11-20, 21 |
| SECURITY-15 | Compliant | NFR-U11-12, 13 |
| Rule còn lại | N/A | Dùng chung backend hoặc ngoài phạm vi đồ án |
