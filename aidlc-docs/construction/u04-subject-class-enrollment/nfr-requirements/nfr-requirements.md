# U04 Subject, Class & Enrollment — nfr-requirements

> Current Construction revision 2026-10-13: 66 active UC, 50 MVP stories + 1 extension, 15 logical units. Đồng bộ theo Inception, chỉ sửa tài liệu; chưa triển khai/kiểm runtime hoặc duyệt stage/code mới. Approval/checklist trước revision thuộc lịch sử.

**Primary UC:** 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 16, 17, 18, 19. **Primary stories:** US-CAT-001, US-CAT-002, US-CAT-003, US-LRN-001. **Requirements:** FR-002/003/005/027.

Nguồn: [shared contract](../../current-srs-contract.md), [Inception catalog](../../../inception/requirements/use-cases.md), [decisions](../../../inception/requirements/revision-decisions-2026-10-13.md). Baseline cũ: [snapshot](../../historical/baseline-before-66-uc-2026-10-13/README.md). Folder legacy `u04-subject-class-enrollment` không quyết định chức năng hiện hành.

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
| NFR-U04-10 | Unique trong DB: `subjects.code`; `(subject_id, code)` của lớp; khóa chính `(class_id, account_id)` của ghi danh. | BR-U04-02, 11, 21 |
| NFR-U04-11 | Quy tắc "1 lớp chưa lưu trữ mỗi môn" (BR-U04-22) kiểm trong transaction có khóa hàng theo `(student, subject)` để hai yêu cầu đồng thời không cùng qua. | BR-U04-22 |
| NFR-U04-12 | Đổi trạng thái lớp dùng khóa lạc quan (`version`); xung đột → "dữ liệu đã thay đổi, tải lại". | BR-U04-14 |
| NFR-U04-13 | Event `enrollment.activated` gửi sau commit qua `EventPublisherPort` của U03. | BR-U04-26 |

## 3. Thông báo

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U04-20 | Mỗi event ghi danh sinh thông báo trong app ngay; email gửi qua việc nền U03 trong trần chung **300 email/ngày** của mọi email thông báo, phần vượt dời sang ngày sau. Giới hạn này do U16 thực hiện, ghi ở đây để U16 nhận làm yêu cầu. | Câu N2, REL-005 |

## 4. Bảo mật

| Mã | Yêu cầu | Nguồn |
|---|---|---|
| NFR-U04-30 | Mọi endpoint kiểm quyền phía server qua U01; ngoài phạm vi trả `404`. | SEC-002, BR-U04-51 |
| NFR-U04-31 | (Bỏ 2026-10-09 cùng mã mời.) | - |
| NFR-U04-32 | Kiểm đầu vào: độ dài, định dạng mã môn/lớp, email, CSV ≤ 200 dòng và ≤ 100 KB. | SEC-003 |
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
| SECURITY-15 | Compliant | Fail closed khi U01 lỗi |
| RESILIENCY-04, 06 | N/A | Dùng chung backend |
| RESILIENCY-10 | N/A | U04 không gọi dịch vụ ngoài |
| Rule còn lại | N/A | Ngoài phạm vi đồ án |

## Quy tắc tích hợp revision hiện hành

- Admin CRUD/archive subjects và gán Subject Manager ACTIVE, chỉ xem class list metadata; không teacher hoặc manager assignment cho Admin.
- Subject Manager của môn R2 tạo/lifecycle lớp, phân Teacher/Subject Manager ACTIVE và thêm/gỡ Student; Teacher chỉ xem lớp được giao, không enrollment/lifecycle.
- Enrollment ACTIVE cấp R5, khi gỡ không xóa lịch sử bài/điểm nhưng deny thao tác tiếp theo. Mỗi sinh viên tối đa một lớp hiệu lực của một môn theo baseline.
- Scope reader cho API/MCP/activity cùng current authority; group membership và teacher assignment kiểm mỗi call, không role-only.
- Archived resource chỉ đọc lịch sử theo quyền; không tiếp tục edit/publish/enroll.
- Authorization current actor/resource on every web/API/MCP call, fail closed; CSRF protections for cookie-session state-changing routes; no user-supplied actor trusted.
- expectedVersion on edits and idempotency on repeatable commands/events; conflicts do not overwrite current state, retry bounded, reference returned not secrets.
- Unit repository private; cross-unit read/write through public ports. DB ownership is logical within one Spring Boot monolith; no 15 service deploys.
- No fabricated survey/implementation evidence. D01 Quiz Practice, D02 Group Graded, D03 own-editable group AI context retained provisionally; numeric quota D04 and backup/activity retention require settings confirmation.

## Kiểm tra revision bắt buộc

- Admin không dạy/chấm, manager không assigned teacher không được chấm, Teacher không enrollment edit.
- Concurrent enrollment/assignment update có version; revoke enrollment deny next API/MCP/activity call; giữ lịch sử.
