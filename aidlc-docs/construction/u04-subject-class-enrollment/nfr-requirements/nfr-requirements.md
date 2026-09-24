# U04 Subject, Class, Enrollment & Learning Access - NFR Requirements

## 1. Hiệu năng

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U04-01 | Kiểm phạm vi (`SubjectScopePort`, `ClassScopePort`, `ClassAccessPort`) là một query theo index, p95 ≤ 20 ms; **không cache** để thay đổi phân công và gỡ ghi danh có hiệu lực ngay. | Câu N1 |
| NFR-U04-02 | API danh sách môn/lớp/ghi danh p95 ≤ 300 ms, trang ≤ 100. | NFR-003 |
| NFR-U04-03 | Ghi danh danh sách 200 email xử lý đồng bộ ≤ 5 giây, tra U01 theo lô (không gọi từng email). | BR-U04-23 |
| NFR-U04-04 | Trang lớp của người học p95 ≤ 500 ms, chưa tính thời gian của `PublishedContentPort`. | NFR-003 |

## 2. Toàn vẹn dữ liệu

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U04-10 | Unique trong DB: `subjects.code`; `(subject_id, code)` của lớp; `invite_code`; `(class_id, learner_account_id)` của ghi danh. | BR-U04-02, 11, 21, 30 |
| NFR-U04-11 | Quy tắc "1 lớp chưa lưu trữ mỗi môn" (BR-U04-22) kiểm trong transaction có khóa hàng theo `(learner, subject)` để hai yêu cầu đồng thời không cùng qua. | BR-U04-22 |
| NFR-U04-12 | Đổi trạng thái lớp dùng khóa lạc quan (`version`); xung đột → "dữ liệu đã thay đổi, tải lại". | BR-U04-14 |
| NFR-U04-13 | Event `ENROLLMENT_ACTIVATED` gửi sau commit qua `EventPublisherPort` của U02. | BR-U04-26 |

## 3. Thông báo

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U04-20 | Mỗi event ghi danh sinh thông báo trong app ngay; email gửi qua job U02, **tối đa 300 email/ngày** cho email ghi danh, phần vượt dời sang ngày sau. Giới hạn này do U16 thực hiện, ghi ở đây để U16 nhận làm yêu cầu. | Câu N2, REL-005 |

## 4. Bảo mật

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U04-30 | Mọi endpoint kiểm quyền phía server qua U01; ngoài phạm vi trả `404`. | SEC-002, BR-U04-51 |
| NFR-U04-31 | Mã mời sinh bằng `SecureRandom`; rate limit 10 lần sai/giờ/tài khoản bằng Bucket4j + Redis; Redis lỗi → từ chối nhập mã (fail closed). | BR-U04-30, 34 |
| NFR-U04-32 | Kiểm đầu vào: độ dài, định dạng mã, email, CSV ≤ 200 dòng và ≤ 100 KB. | SEC-003 |
| NFR-U04-33 | Không log email người học trong log ứng dụng; audit ghi `accountId`. | SEC-005 |

## 5. Kiểm thử

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U04-40 | Unit test mọi `BR-U04-xx`; integration test hai yêu cầu ghi danh đồng thời vào hai lớp cùng môn chỉ một thành công. | NFR-004 |
| NFR-U04-41 | Test phân quyền cho 4 role trên mọi endpoint, gồm dùng ID lớp của người khác. | SEC-002 |

## 6. Compliance

| Rule | Trạng thái | Căn cứ |
|---|---|---|
| SECURITY-03 | Compliant | NFR-U04-33 |
| SECURITY-04 | N/A | Header dùng chung ở shared-infrastructure |
| SECURITY-05 | Compliant | NFR-U04-32 |
| SECURITY-08 | Compliant | NFR-U04-30 |
| SECURITY-09 | N/A | U04 không có secret riêng |
| SECURITY-12 | N/A | Xác thực thuộc U01 |
| SECURITY-15 | Compliant | Fail closed khi U01/Redis lỗi |
| RESILIENCY-04, 06 | N/A | Dùng chung backend |
| RESILIENCY-10 | N/A | U04 không gọi dịch vụ ngoài |
| Rule còn lại | N/A | Ngoài phạm vi đồ án |
